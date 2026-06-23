package com.example.miscfeatures.command;

import com.example.miscfeatures.MiscFeaturesMod;
import com.example.miscfeatures.config.MiscFeaturesConfig;
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
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.Comparator;
import java.util.stream.Stream;

public final class FindItemCommand {

    private static final String FIND_ITEM_USAGE = "/find item <radius> <item|#tag> [more items/tags] [--sort=distance|alphabetic|chunk] [--page=1] [--limit=25] [--loaded-only] [--include-shulker|--exclude-shulker]";
    private static final List<String> FLAG_SUGGESTIONS = List.of(
            "--sort=distance",
            "--sort=alphabetic",
            "--sort=chunk",
            "--page=1",
            "--limit=25",
            "--loaded-only",
            "--include-shulker",
            "--exclude-shulker"
    );

    private FindItemCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("find")
                .then(Commands.literal("item")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .then(Commands.argument("items", StringArgumentType.greedyString())
                            .suggests(FindItemCommand::suggestItems)
                            .executes(FindItemCommand::execute))))
        );
    }

    private static CompletableFuture<Suggestions> suggestItems(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        List<String> suggestions = new ArrayList<>();
        BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).forEach(suggestions::add);
        BuiltInRegistries.ITEM.getTags().map(named -> "#" + named.key().location()).forEach(suggestions::add);
        suggestions.addAll(FLAG_SUGGESTIONS);
        return AreaCommandSupport.suggestTokenList(builder, suggestions.stream());
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        MiscFeaturesConfig config = MiscFeaturesConfig.getInstance();
        int maxRadius = config.getMaxItemSearchRadius();
        int maxResults = config.getMaxFindItemResults();
        boolean searchUnloadedChunks = config.shouldSearchItemsInUnloadedChunks();
        boolean includeShulker = true;
        SortMode sortMode = SortMode.DISTANCE;
        int page = 1;
        int requestedLimit = maxResults;
        String input = StringArgumentType.getString(context, "items");

        if (!AreaCommandSupport.validateRadius(source, radius, maxRadius, "maxItemSearchRadius")) {
            return 0;
        }

        MiscFeaturesMod.verbose(
            "Running /find item with radius={} searchUnloadedChunks={} rawTargets='{}'",
                radius,
                searchUnloadedChunks,
                input
        );

        Map<String, ItemSearchTarget> targets = new HashMap<>();
        Map<Item, Set<ItemSearchTarget>> targetIndex = new HashMap<>();
        List<String> unknownTokens = new ArrayList<>();

        for (String token : AreaCommandSupport.tokenizeTargets(input)) {
            if (token.isEmpty()) {
                continue;
            }

            if (token.equalsIgnoreCase("--loaded-only")) {
                searchUnloadedChunks = false;
                continue;
            }

            if (token.equalsIgnoreCase("--include-shulker")) {
                includeShulker = true;
                continue;
            }

            if (token.equalsIgnoreCase("--exclude-shulker")) {
                includeShulker = false;
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

            Integer parsedPage = AreaCommandSupport.parsePositiveIntFlag(token, "--page=");
            if (token.toLowerCase().startsWith("--page=")) {
                if (parsedPage == null) {
                    unknownTokens.add(token);
                } else {
                    page = parsedPage;
                }
                continue;
            }

            Integer parsedLimit = AreaCommandSupport.parsePositiveIntFlag(token, "--limit=");
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
            source.sendFailure(AreaCommandSupport.getUnknownItemError(unknown));
        }

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No valid items or tags specified. Usage: §e" + FIND_ITEM_USAGE
            ));
            return 0;
        }

        MiscFeaturesMod.verbose(
                "Indexed {} unique item targets across {} unique items",
                targets.size(),
                targetIndex.size()
        );

        final boolean searchUnloadedChunksFinal = searchUnloadedChunks;
        final boolean includeShulkerFinal = includeShulker;
        final SortMode sortModeFinal = sortMode;

        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        source.sendSuccess(() -> Component.literal(
                "§7Searching containers in " + AreaCommandSupport.cubeWidth(radius) + "³ blocks around ["
                        + center.getX() + ", " + center.getY() + ", " + center.getZ() + "]…"
        ), false);

        Map<ItemSearchTarget, List<ContainerMatch>> results = new HashMap<>();
        Map<ItemSearchTarget, Long> totalCounts = new HashMap<>();
        Map<ItemSearchTarget, Long> containerCounts = new HashMap<>();
        for (ItemSearchTarget target : targets.values()) {
            results.put(target, new ArrayList<>());
            totalCounts.put(target, 0L);
            containerCounts.put(target, 0L);
        }

        AreaCommandSupport.forEachBlockInCube(level, center, radius, searchUnloadedChunksFinal, pos -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null || !(blockEntity instanceof Container container)) {
                return;
            }

            if (!includeShulkerFinal && blockEntity instanceof ShulkerBoxBlockEntity) {
                return;
            }

            Map<ItemSearchTarget, Long> containerMatches = new HashMap<>();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                Set<ItemSearchTarget> matches = targetIndex.get(stack.getItem());
                if (matches == null || matches.isEmpty()) {
                    continue;
                }

                for (ItemSearchTarget target : matches) {
                    containerMatches.merge(target, (long) stack.getCount(), Long::sum);
                }
            }

            if (containerMatches.isEmpty()) {
                return;
            }

            BlockPos immutablePos = pos.immutable();
            net.minecraft.network.chat.Component blockNameComponent = level.getBlockState(pos).getBlock().getName();
            String containerName = blockNameComponent != null ? blockNameComponent.getString() : "Unknown Container";
            for (Map.Entry<ItemSearchTarget, Long> entry : containerMatches.entrySet()) {
                ItemSearchTarget target = entry.getKey();
                long count = Math.min(entry.getValue(), Integer.MAX_VALUE);
                totalCounts.merge(target, count, Long::sum);
                containerCounts.merge(target, 1L, Long::sum);

                List<ContainerMatch> bucket = results.get(target);
                if (bucket.size() < maxResults) {
                    bucket.add(new ContainerMatch(containerName, immutablePos, count));
                }
            }
        });

        int grandTotal = 0;
        for (Map.Entry<ItemSearchTarget, List<ContainerMatch>> entry : results.entrySet()) {
            ItemSearchTarget target = entry.getKey();
            List<ContainerMatch> matches = entry.getValue();
            sortMatches(matches, sortModeFinal, center);
            long totalItems = totalCounts.get(target);
            long totalContainers = containerCounts.get(target);
            grandTotal += Math.min(totalItems, Integer.MAX_VALUE);

            if (totalItems == 0) {
                source.sendSuccess(() -> Component.literal(
                        "§cNo §e" + target.displayName() + "§c found in nearby containers within radius " + radius + "."
                ), false);
                continue;
            }

                int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) pageLimit));
                int clampedPage = Math.max(1, Math.min(page, totalPages));
                int pageStart = (clampedPage - 1) * pageLimit;
                int pageEnd = Math.min(matches.size(), pageStart + pageLimit);
                List<ContainerMatch> pagedMatches = pageStart < pageEnd
                    ? matches.subList(pageStart, pageEnd)
                    : List.of();

                boolean truncated = totalContainers > maxResults;
            source.sendSuccess(() -> Component.literal(
                    "§a" + totalItems + " §e" + target.displayName() + "§a found across §e" + totalContainers + "§a container(s):"
                    + (truncated ? " §7(capped at " + maxResults + " by config)" : "")
                    + " §8[sort=" + sortModeFinal.token
                    + ", page=" + clampedPage + "/" + totalPages
                    + ", limit=" + pageLimit
                    + (searchUnloadedChunksFinal ? "" : ", loaded-only")
                    + (includeShulkerFinal ? "" : ", exclude-shulker") + "]"
            ), false);

                for (ContainerMatch match : pagedMatches) {
                source.sendSuccess(() -> Component.literal("  " + match.containerName() + " x" + match.itemCount() + " at ")
                        .append(AreaCommandSupport.createCoordinateComponent(match.pos())), false);
            }
        }

        int finalTotal = grandTotal;
        MiscFeaturesMod.verbose(
                "Find item complete with {} total item matches across {} targets",
                finalTotal,
                targets.size()
        );
        source.sendSuccess(() -> Component.literal(
            "§7Find item complete — §f" + finalTotal + " §7item(s) total."
        ), false);

        return finalTotal;
    }

    private static boolean indexTarget(
            String token,
            Map<String, ItemSearchTarget> targets,
            Map<Item, Set<ItemSearchTarget>> targetIndex
    ) {
        if (token.startsWith("#")) {
            String rawTag = token.substring(1);
            String fullId = rawTag.contains(":") ? rawTag : "minecraft:" + rawTag;
            Identifier identifier = Identifier.tryParse(fullId);
            if (identifier == null) {
                return false;
            }

            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, identifier);
            boolean exists = BuiltInRegistries.ITEM.getTags().anyMatch(named -> named.key().equals(tagKey));
            if (!exists) {
                return false;
            }

            ItemSearchTarget target = targets.computeIfAbsent("#" + identifier, ItemSearchTarget::new);
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                indexItemTarget(targetIndex, holder.value(), target);
            }
            return true;
        }

        String fullId = token.contains(":") ? token : "minecraft:" + token;
        Identifier identifier = Identifier.tryParse(fullId);
        if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) {
            return false;
        }

        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        ItemSearchTarget target = targets.computeIfAbsent(identifier.toString(), ItemSearchTarget::new);
        indexItemTarget(targetIndex, item, target);
        return true;
    }

    private static void indexItemTarget(
            Map<Item, Set<ItemSearchTarget>> targetIndex,
            Item item,
            ItemSearchTarget target
    ) {
        targetIndex.computeIfAbsent(item, ignored -> new HashSet<>()).add(target);
    }

    private record ItemSearchTarget(String displayName) {
    }

    private record ContainerMatch(String containerName, BlockPos pos, long itemCount) {
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

    private static void sortMatches(List<ContainerMatch> matches, SortMode sortMode, BlockPos center) {
        Comparator<ContainerMatch> comparator = switch (sortMode) {
            case DISTANCE -> Comparator
                    .comparingDouble((ContainerMatch match) -> match.pos().distSqr(center))
                    .thenComparing(ContainerMatch::containerName);
            case ALPHABETIC -> Comparator
                    .comparing(ContainerMatch::containerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(match -> match.pos().getX())
                    .thenComparing(match -> match.pos().getY())
                    .thenComparing(match -> match.pos().getZ());
            case CHUNK -> Comparator
                    .comparingInt((ContainerMatch match) -> match.pos().getX() >> 4)
                    .thenComparingInt(match -> match.pos().getZ() >> 4)
                    .thenComparingInt(match -> match.pos().getY());
        };
        matches.sort(comparator);
    }
}