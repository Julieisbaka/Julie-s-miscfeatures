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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.Comparator;

public final class FindEntity {

    private static final String FIND_ENTITY_USAGE = "/find entity <radius> <entity|#tag> [more entities or tags...] [--sort=distance|alphabetic|chunk] [--page=1] [--limit=25] [--loaded-only]";
    private static final List<String> FLAG_SUGGESTIONS = List.of(
            "--sort=distance",
            "--sort=alphabetic",
            "--sort=chunk",
            "--page=1",
            "--limit=25",
            "--loaded-only",
            "--include-unloaded"
    );

    private FindEntity() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("find")
                .then(Commands.literal("entity")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .then(Commands.argument("entities", StringArgumentType.greedyString())
                            .suggests(FindEntity::suggestEntities)
                            .executes(FindEntity::execute))))
        );
    }

    private static CompletableFuture<Suggestions> suggestEntities(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        List<String> suggestions = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString).forEach(suggestions::add);
        BuiltInRegistries.ENTITY_TYPE.getTags().map(named -> "#" + named.key().location()).forEach(suggestions::add);
        suggestions.addAll(FLAG_SUGGESTIONS);
        return AreaSupport.suggestTokenList(builder, suggestions.stream());
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Config config = Config.getInstance();
        int maxRadius = config.getMaxEntitySearchRadius();
        int maxResults = config.getMaxFindEntityResults();
        boolean searchUnloadedChunks = config.shouldSearchEntitiesInUnloadedChunks();
        SortMode sortMode = SortMode.DISTANCE;
        int page = 1;
        int requestedLimit = maxResults;
        String input = StringArgumentType.getString(context, "entities");

        if (!AreaSupport.validateRadius(source, radius, maxRadius, "maxEntitySearchRadius")) {
            return 0;
        }

        MiscFeatures.verbose(
            "Running /find entity with radius={} searchUnloadedChunks={} rawTargets='{}'",
                radius,
            searchUnloadedChunks,
                input
        );

        Map<String, EntitySearchTarget> targets = new HashMap<>();
        Map<EntityType<?>, Set<EntitySearchTarget>> targetIndex = new HashMap<>();
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
            source.sendFailure(AreaSupport.getUnknownEntityError(unknown));
        }

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No valid entities or tags specified. Usage: §e" + FIND_ENTITY_USAGE
            ));
            return 0;
        }

        MiscFeatures.verbose(
                "Indexed {} unique entity targets across {} unique entity types",
                targets.size(),
                targetIndex.size()
        );

        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        final SortMode sortModeFinal = sortMode;
        final boolean searchUnloadedChunksFinal = searchUnloadedChunks;
        source.sendSuccess(() -> Component.literal(
            "§7Searching loaded entities in radius §f" + radius + "§7 around ["
                        + center.getX() + ", " + center.getY() + ", " + center.getZ() + "]…"
        ), false);

        if (searchUnloadedChunksFinal) {
            source.sendSuccess(() -> Component.literal(
                    "§eNote: entity search only returns currently loaded entities; unloaded chunks are not force-loaded."
            ), false);
        }

        AABB bounds = new AABB(
                center.getX() - radius,
                center.getY() - radius,
                center.getZ() - radius,
                center.getX() + radius + 1,
                center.getY() + radius + 1,
                center.getZ() + radius + 1
        );

        Map<EntitySearchTarget, List<EntityMatch>> results = new HashMap<>();
        Map<EntitySearchTarget, Long> totalCounts = new HashMap<>();
        for (EntitySearchTarget target : targets.values()) {
            results.put(target, new ArrayList<>());
            totalCounts.put(target, 0L);
        }

        for (Entity entity : level.getEntities((Entity) null, bounds)) {
            Set<EntitySearchTarget> matches = targetIndex.get(entity.getType());
            if (matches == null || matches.isEmpty()) {
                continue;
            }

            BlockPos pos = BlockPos.containing(entity.position());
            String entityName = entity.getDisplayName() != null ? entity.getDisplayName().getString() : "Unknown";
            for (EntitySearchTarget target : matches) {
                totalCounts.merge(target, 1L, Long::sum);
                List<EntityMatch> bucket = results.get(target);
                if (bucket.size() < maxResults) {
                    bucket.add(new EntityMatch(entityName, pos));
                }
            }
        }

        long grandTotal = 0L;
        for (Map.Entry<EntitySearchTarget, List<EntityMatch>> entry : results.entrySet()) {
            EntitySearchTarget target = entry.getKey();
            List<EntityMatch> matches = entry.getValue();
            long total = totalCounts.get(target);
            grandTotal += Math.min(total, Integer.MAX_VALUE);

            if (total == 0) {
                source.sendSuccess(() -> Component.literal(
                        "§cNo §e" + target.displayName() + "§c found within radius " + radius + "."
                ), false);
                continue;
            }

            sortMatches(matches, sortModeFinal, center);
            int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) pageLimit));
            int clampedPage = Math.max(1, Math.min(page, totalPages));
            int pageStart = (clampedPage - 1) * pageLimit;
            int pageEnd = Math.min(matches.size(), pageStart + pageLimit);
            List<EntityMatch> pagedMatches = pageStart < pageEnd
                    ? matches.subList(pageStart, pageEnd)
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

            for (EntityMatch match : pagedMatches) {
                source.sendSuccess(() -> Component.literal("  " + match.entityName() + " at ")
                        .append(AreaSupport.createCoordinateComponent(match.pos())), false);
            }
        }

        int finalTotal = grandTotal;
        MiscFeatures.verbose(
                "Find entity complete with {} total matches across {} targets",
                finalTotal,
                targets.size()
        );
        source.sendSuccess(() -> Component.literal(
                "§7Find entity complete — §f" + finalTotal + " §7entity(s) total."
        ), false);

        return finalTotal;
    }

    private static boolean indexTarget(
            String token,
            Map<String, EntitySearchTarget> targets,
            Map<EntityType<?>, Set<EntitySearchTarget>> targetIndex
    ) {
        if (token.startsWith("#")) {
            String rawTag = token.substring(1);
            String fullId = rawTag.contains(":") ? rawTag : "minecraft:" + rawTag;
            Identifier identifier = Identifier.tryParse(fullId);
            if (identifier == null) {
                return false;
            }

            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, identifier);
            boolean exists = BuiltInRegistries.ENTITY_TYPE.getTags().anyMatch(named -> named.key().equals(tagKey));
            if (!exists) {
                return false;
            }

            EntitySearchTarget target = targets.computeIfAbsent("#" + identifier, EntitySearchTarget::new);
            for (var holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tagKey)) {
                indexEntityTarget(targetIndex, holder.value(), target);
            }
            return true;
        }

        String fullId = token.contains(":") ? token : "minecraft:" + token;
        Identifier identifier = Identifier.tryParse(fullId);
        if (identifier == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
            return false;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        EntitySearchTarget target = targets.computeIfAbsent(identifier.toString(), EntitySearchTarget::new);
        indexEntityTarget(targetIndex, entityType, target);
        return true;
    }

    private static void indexEntityTarget(
            Map<EntityType<?>, Set<EntitySearchTarget>> targetIndex,
            EntityType<?> entityType,
            EntitySearchTarget target
    ) {
        targetIndex.computeIfAbsent(entityType, ignored -> new HashSet<>()).add(target);
    }

    private record EntitySearchTarget(String displayName) {
    }

    private record EntityMatch(String entityName, BlockPos pos) {
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

    private static void sortMatches(List<EntityMatch> matches, SortMode sortMode, BlockPos center) {
        Comparator<EntityMatch> comparator = switch (sortMode) {
            case DISTANCE -> Comparator
                    .comparingDouble((EntityMatch match) -> match.pos().distSqr(center))
                    .thenComparing(EntityMatch::entityName, String.CASE_INSENSITIVE_ORDER);
            case ALPHABETIC -> Comparator
                    .comparing(EntityMatch::entityName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(match -> match.pos().getX())
                    .thenComparing(match -> match.pos().getY())
                    .thenComparing(match -> match.pos().getZ());
            case CHUNK -> Comparator
                    .comparingInt((EntityMatch match) -> match.pos().getX() >> 4)
                    .thenComparingInt(match -> match.pos().getZ() >> 4)
                    .thenComparingInt(match -> match.pos().getY());
        };

        matches.sort(comparator);
    }
}