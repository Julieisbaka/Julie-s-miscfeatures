package com.example.miscfeatures.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class AreaCommandSupport {

    private AreaCommandSupport() {
    }

    public static List<String> tokenizeTargets(String input) {
        List<String> tokens = new ArrayList<>();
        String trimmed = input.trim();
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == ',' || Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
            } else {
                currentToken.append(c);
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

    public static CompletableFuture<Suggestions> suggestTokenList(
            SuggestionsBuilder builder,
            Stream<String> suggestions
    ) {
        String remaining = builder.getRemaining();
        int lastSeparator = Math.max(remaining.lastIndexOf(' '), remaining.lastIndexOf(','));
        int tokenStartOffset = lastSeparator >= 0 ? lastSeparator + 1 : 0;
        String completedInput = tokenStartOffset > 0 ? remaining.substring(0, tokenStartOffset) : "";
        String currentToken = remaining.substring(tokenStartOffset).trim();
        Set<String> alreadySelected = new HashSet<>(tokenizeTargets(completedInput));
        SuggestionsBuilder tokenBuilder = builder.createOffset(builder.getStart() + tokenStartOffset);

        List<String> filteredSuggestions = new ArrayList<>();
        Set<String> deduplicated = new HashSet<>();
        suggestions.forEach(candidate -> {
            // Inline expansion logic
            List<String> expanded = new ArrayList<>();
            expanded.add(candidate);
            if (candidate.startsWith("minecraft:")) {
                expanded.add(candidate.substring("minecraft:".length()));
            } else if (candidate.startsWith("#minecraft:")) {
                expanded.add("#" + candidate.substring("#minecraft:".length()));
            }
            
            for (String normalizedCandidate : expanded) {
                if (!alreadySelected.contains(normalizedCandidate)
                        && matchesToken(currentToken, normalizedCandidate)
                        && deduplicated.add(normalizedCandidate)) {
                    filteredSuggestions.add(normalizedCandidate);
                }
            }
        });

        return SharedSuggestionProvider.suggest(filteredSuggestions.stream(), tokenBuilder);
    }

    public static boolean validateRadius(
            CommandSourceStack source,
            int radius,
            int maxRadius,
            String configKey
    ) {
        if (radius <= maxRadius) {
            return true;
        }

        source.sendFailure(Component.literal(
                "Radius §e" + radius + "§c exceeds the configured maximum of §e" + maxRadius
                        + "§c for §b" + configKey + "§c. Change it in Mod Menu or with §e/miscfeatures config§c."
        ));
        return false;
    }

    public static int cubeWidth(int radius) {
        return radius * 2 + 1;
    }

    public static void forEachBlockInCube(
            ServerLevel level,
            BlockPos center,
            int radius,
            boolean includeUnloadedChunks,
            BlockPosVisitor visitor
    ) {
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                if (!includeUnloadedChunks && !level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                    continue;
                }

                for (int y = min.getY(); y <= max.getY(); y++) {
                    mutablePos.set(x, y, z);
                    visitor.visit(mutablePos);
                }
            }
        }
    }

    public static void forEachChunkInCube(
            BlockPos center,
            int radius,
            ChunkPosVisitor visitor
    ) {
        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        for (int chunkX = min.getX() >> 4; chunkX <= max.getX() >> 4; chunkX++) {
            for (int chunkZ = min.getZ() >> 4; chunkZ <= max.getZ() >> 4; chunkZ++) {
                visitor.visit(chunkX, chunkZ);
            }
        }
    }

    public static Component createCoordinateComponent(BlockPos pos) {
        String commandCoordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        String displayCoordinates = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        return Component.literal("[" + displayCoordinates + "]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + commandCoordinates))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                                "Click to suggest /tp @s " + commandCoordinates
                        ))));
    }

    @FunctionalInterface
    public interface BlockPosVisitor {
        void visit(BlockPos.MutableBlockPos pos);
    }

    @FunctionalInterface
    public interface ChunkPosVisitor {
        void visit(int chunkX, int chunkZ);
    }

    private static boolean matchesToken(String inputToken, String candidate) {
        if (inputToken.isEmpty()) {
            return true;
        }

        String normalizedInput = inputToken.toLowerCase(Locale.ROOT);
        String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
        if (normalizedCandidate.startsWith(normalizedInput)) {
            return true;
        }

        int namespaceSeparator = normalizedCandidate.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalizedCandidate.length()) {
            return normalizedCandidate.substring(namespaceSeparator + 1).startsWith(normalizedInput);
        }

        return false;
    }

    /**
     * Get standardized error message for unknown block/tag
     */
    public static Component getUnknownBlockError(String unknown) {
        return Component.literal(
                "Unknown block or tag: §e" + unknown
                        + "§c. Use ids like §eminecraft:stone§c, §estone§c, tags like §e#minecraft:logs§c, or with properties §eminecraft:stone[axis=y]§c."
        );
    }

    /**
     * Get standardized error message for unknown item/tag
     */
    public static Component getUnknownItemError(String unknown) {
        return Component.literal(
                "Unknown item or tag: §e" + unknown
                        + "§c. Use ids like §eminecraft:diamond§c, §ediamond§c, or tags like §e#minecraft:planks§c."
        );
    }

    /**
     * Get standardized error message for unknown entity/tag
     */
    public static Component getUnknownEntityError(String unknown) {
        return Component.literal(
                "Unknown entity or tag: §e" + unknown
                        + "§c. Use ids like §eminecraft:villager§c, §evillager§c, or tags like §e#minecraft:undead§c."
        );
    }

    public static Integer parsePositiveIntFlag(String token, String prefix) {
        if (!token.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            return null;
        }

        String value = token.substring(prefix.length());
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}