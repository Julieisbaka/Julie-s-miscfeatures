package com.example.miscfeatures.command;

import com.example.miscfeatures.MiscFeatures;
import com.example.miscfeatures.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

public class FindBlock {

    private static final String FIND_BLOCK_USAGE = "/find block <radius> <block|#tag>[property=value] [more blocks or tags...] [--sort=distance|alphabetic|chunk] [--page=1] [--limit=25] [--loaded-only]";
    private static final List<String> FLAG_SUGGESTIONS = List.of(
            "--sort=distance",
            "--sort=alphabetic",
            "--sort=chunk",
            "--page=1",
            "--limit=25",
            "--loaded-only",
            "--include-unloaded"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("find")
                .then(Commands.literal("block")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .then(Commands.argument("blocks", StringArgumentType.greedyString())
                            .suggests(FindBlock::suggestBlocks)
                            .executes(FindBlock::execute))))
        );
    }

    private static CompletableFuture<Suggestions> suggestBlocks(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        List<String> suggestions = new ArrayList<>();
        BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString).forEach(suggestions::add);
        BuiltInRegistries.BLOCK.getTags().map(named -> "#" + named.key().location()).forEach(suggestions::add);
        suggestions.addAll(FLAG_SUGGESTIONS);
        return AreaSupport.suggestTokenList(builder, suggestions.stream());
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Config config = Config.getInstance();
        int maxRadius = config.getMaxSearchRadius();
        int maxResults = config.getMaxSearchResults();
        boolean searchUnloadedChunks = config.shouldSearchUnloadedChunks();
        SortMode sortMode = SortMode.DISTANCE;
        int page = 1;
        int requestedLimit = maxResults;
        String input = StringArgumentType.getString(context, "blocks");

        if (!AreaSupport.validateRadius(source, radius, maxRadius, "maxSearchRadius")) {
            return 0;
        }

        MiscFeatures.verbose(
            "Running /find block with radius={} searchUnloadedChunks={} rawTargets='{}'",
            radius,
            searchUnloadedChunks,
            input
        );

        Map<String, SearchTarget> targets = new HashMap<>();
        Map<Block, Set<SearchTarget>> targetIndex = new HashMap<>();
        List<String> unknownTokens = new ArrayList<>();

        for (String token : AreaSupport.tokenizeTargets(input)) {
            if (token.isEmpty()) {
                continue;
            }

            if (token.equalsIgnoreCase("--loaded-only")) {
                searchUnloadedChunks = false;
                continue;
            }

            if (token.equalsIgnoreCase("--include-unloaded")) {
                searchUnloadedChunks = true;
                continue;
            }

            if (token.toLowerCase().startsWith("--sort=")) {
                SortMode parsed = SortMode.fromToken(token.substring("--sort=".length()));
                if (parsed == null) {
                    unknownTokens.add(token);
                } else {
                    sortMode = parsed;
                }
                continue;
            }

            Integer parsedPage = AreaSupport.parsePositiveIntFlag(token, "--page=");
            if (token.toLowerCase().startsWith("--page=")) {
                if (parsedPage == null) {
                    unknownTokens.add(token);
                } else {
                    page = parsedPage;
                }
                continue;
            }

            Integer parsedLimit = AreaSupport.parsePositiveIntFlag(token, "--limit=");
            if (token.toLowerCase().startsWith("--limit=")) {
                if (parsedLimit == null) {
                    unknownTokens.add(token);
                } else {
                    requestedLimit = parsedLimit;
                }
                continue;
            }

            if (!indexTarget(token, targets, targetIndex)) {
                unknownTokens.add(token);
            }
        }

        int pageLimit = Math.max(1, Math.min(maxResults, requestedLimit));

        for (String unknown : unknownTokens) {
            source.sendFailure(AreaSupport.getUnknownBlockError(unknown));
        }

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No valid blocks or tags specified. Usage: §e" + FIND_BLOCK_USAGE
            ));
            return 0;
        }

        MiscFeatures.verbose(
            "Indexed {} unique targets across {} unique blocks",
            targets.size(),
            targetIndex.size()
        );

        ServerLevel level = source.getLevel();
        if (level == null) {
            source.sendFailure(Component.literal("Cannot search: not in a world context."));
            return 0;
        }

        Vec3 posVec = source.getPosition();
        BlockPos center = BlockPos.containing(posVec);
        final SortMode sortModeFinal = sortMode;
        final boolean searchUnloadedChunksFinal = searchUnloadedChunks;

        source.sendSuccess(() -> Component.literal(
                "§7Searching " + AreaSupport.cubeWidth(radius) + "³ blocks around ["
                        + center.getX() + ", " + center.getY() + ", " + center.getZ() + "]…"
        ), false);

        Map<SearchTarget, List<BlockPos>> results = new HashMap<>();
        Map<SearchTarget, Long> totalCounts = new HashMap<>();
        for (SearchTarget target : targets.values()) {
            results.put(target, new ArrayList<>());
            totalCounts.put(target, 0L);
        }

        AreaSupport.forEachBlockInCube(level, center, radius, searchUnloadedChunksFinal, mutablePos -> {
            BlockState state = level.getBlockState(mutablePos);
            Set<SearchTarget> matches = targetIndex.get(state.getBlock());
            if (matches == null || matches.isEmpty()) {
                return;
            }

            for (SearchTarget target : matches) {
                if (target.stateFilter.matches(state)) {
                    totalCounts.merge(target, 1L, Long::sum);
                    List<BlockPos> bucket = results.get(target);
                    if (bucket.size() < maxResults) {
                        bucket.add(mutablePos.immutable());
                    }
                }
            }
        });

        int grandTotal = 0;
        for (Map.Entry<SearchTarget, List<BlockPos>> entry : results.entrySet()) {
            SearchTarget target = entry.getKey();
            List<BlockPos> positions = entry.getValue();
            long total = totalCounts.get(target);
            grandTotal += Math.min(total, Integer.MAX_VALUE);

            if (total == 0) {
                source.sendSuccess(() -> Component.literal(
                        "§cNo §e" + target.displayName() + "§c found within radius " + radius + "."
                ), false);
                continue;
            }

            sortMatches(positions, sortModeFinal, center);
            int totalPages = Math.max(1, (int) Math.ceil(positions.size() / (double) pageLimit));
            int clampedPage = Math.max(1, Math.min(page, totalPages));
            int pageStart = (clampedPage - 1) * pageLimit;
            int pageEnd = Math.min(positions.size(), pageStart + pageLimit);
            List<BlockPos> pagedPositions = pageStart < pageEnd
                    ? positions.subList(pageStart, pageEnd)
                    : List.of();

            boolean truncated = total > maxResults;
            source.sendSuccess(() -> Component.literal(
                    "§a" + total + " §e" + target.displayName() + "§a found:"
                        + (truncated ? " §7(capped at " + maxResults + " by config)" : "")
                        + " §8[sort=" + sortModeFinal.token
                        + ", page=" + clampedPage + "/" + totalPages
                        + ", limit=" + pageLimit
                        + (searchUnloadedChunksFinal ? "" : ", loaded-only") + "]"
            ), false);

            for (BlockPos pos : pagedPositions) {
                source.sendSuccess(() -> Component.literal("  ").append(AreaSupport.createCoordinateComponent(pos)), false);
            }
        }

        int finalTotal = grandTotal;
        MiscFeatures.verbose(
            "Search complete with {} total matches across {} targets",
            finalTotal,
            targets.size()
        );
        source.sendSuccess(() -> Component.literal(
            "§7Find block complete — §f" + finalTotal + " §7block(s) total."
        ), false);

        return finalTotal;
    }

    private static boolean indexTarget(
            String token,
            Map<String, SearchTarget> targets,
            Map<Block, Set<SearchTarget>> targetIndex
    ) {
        String blockId = BlockStateFilter.extractBlockId(token);
        BlockStateFilter stateFilter = new BlockStateFilter(token);

        if (blockId.startsWith("#")) {
            String rawTag = blockId.substring(1);
            String fullId = rawTag.contains(":") ? rawTag : "minecraft:" + rawTag;
            Identifier identifier = Identifier.tryParse(fullId);
            if (identifier == null) {
                return false;
            }

            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, identifier);
            boolean exists = BuiltInRegistries.BLOCK.getTags().anyMatch(named -> named.key().equals(tagKey));
            if (!exists) {
                return false;
            }

            String displayName = "#" + identifier + stateFilter.toString();
            SearchTarget target = targets.computeIfAbsent(displayName, name -> new SearchTarget(name, stateFilter));
            for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey)) {
                indexBlockTarget(targetIndex, holder.value(), target);
            }
            return true;
        }

        String fullId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
        Identifier identifier = Identifier.tryParse(fullId);
        if (identifier == null || !BuiltInRegistries.BLOCK.containsKey(identifier)) {
            return false;
        }

        Block block = BuiltInRegistries.BLOCK.getValue(identifier);
        boolean isAirBlock = block == Blocks.AIR;
        if (isAirBlock && !identifier.getPath().equals("air")) {
            return false;
        }

        String displayName = identifier.toString() + stateFilter.toString();
        SearchTarget target = targets.computeIfAbsent(displayName, name -> new SearchTarget(name, stateFilter));
        indexBlockTarget(targetIndex, block, target);
        return true;
    }

    private static void indexBlockTarget(
            Map<Block, Set<SearchTarget>> targetIndex,
            Block block,
            SearchTarget target
    ) {
        targetIndex.computeIfAbsent(block, ignored -> new HashSet<>()).add(target);
    }

    private record SearchTarget(String displayName, BlockStateFilter stateFilter) {
    }

    private enum SortMode {
        DISTANCE("distance"),
        ALPHABETIC("alphabetic"),
        CHUNK("chunk");

        private final String token;

        SortMode(String token) {
            this.token = token;
        }

        private static SortMode fromToken(String token) {
            return switch (token.toLowerCase()) {
                case "distance" -> DISTANCE;
                case "alphabetic", "alphabetical" -> ALPHABETIC;
                case "chunk" -> CHUNK;
                default -> null;
            };
        }
    }

    private static void sortMatches(List<BlockPos> positions, SortMode sortMode, BlockPos center) {
        Comparator<BlockPos> comparator = switch (sortMode) {
            case DISTANCE -> Comparator
                    .comparingDouble((BlockPos pos) -> pos.distSqr(center))
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getY)
                    .thenComparingInt(BlockPos::getZ);
            case ALPHABETIC -> Comparator
                    .comparingInt((BlockPos pos) -> pos.getX())
                    .thenComparingInt(pos -> pos.getY())
                    .thenComparingInt(pos -> pos.getZ());
            case CHUNK -> Comparator
                    .comparingInt((BlockPos pos) -> pos.getX() >> 4)
                    .thenComparingInt(pos -> pos.getZ() >> 4)
                    .thenComparingInt(BlockPos::getY);
        };

        positions.sort(comparator);
    }
}