package com.example.miscfeatures.command;

import com.example.miscfeatures.config.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ConfigCommand {

    private static final List<String> SETTING_KEYS = Arrays.stream(SettingKey.values())
        .map(SettingKey::key)
        .toList();

    private ConfigCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRootLiteral("miscfeatures"));
        dispatcher.register(buildRootLiteral("mf"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRootLiteral(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .then(Commands.literal("config")
                        .executes(ConfigCommand::show)
                        .then(Commands.literal("show").executes(ConfigCommand::show))
                        .then(Commands.literal("set")
                                .then(Commands.argument("setting", StringArgumentType.word())
                                        .suggests(ConfigCommand::suggestSettingKeys)
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .suggests(ConfigCommand::suggestSettingValues)
                                                .executes(ConfigCommand::setSetting)))));
    }

    private static CompletableFuture<Suggestions> suggestSettingKeys(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(SETTING_KEYS.stream(), builder);
    }

    private static CompletableFuture<Suggestions> suggestSettingValues(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        SettingKey setting = SettingKey.fromInput(StringArgumentType.getString(context, "setting"));
        if (setting == null) {
            return Suggestions.empty();
        }

        return switch (setting) {
            case SEARCH_UNLOADED_CHUNKS,
                 SEARCH_ITEMS_IN_UNLOADED_CHUNKS,
                 SEARCH_ENTITIES_IN_UNLOADED_CHUNKS,
                 DEVELOPER_MODE,
                 VERBOSE_LOGGING,
                 ANVIL_AUTO_INSERT_ARMOR,
                 ANVIL_AUTO_INSERT_WEAPONS,
                 ANVIL_AUTO_INSERT_NAME_TAGS,
                 ALLOW_NEGATIVE_ENCHANTS,
                 ALLOW_HIGH_LEVEL_ENCHANTS,
                 REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS,
                 REQUIRE_CONFIRM_FOR_NEGATIVE_ENCHANTS,
                 REQUIRE_PERMISSION_FOR_ENCHANT,
                 REQUIRE_PERMISSION_FOR_ANVIL,
                 PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS,
                 PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS,
                 FIX_HIGH_LEVEL_ENCHANT_TEXT,
                 HIGH_LEVEL_ENCHANT_STYLE_ROMAN -> SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
            case ENCHANT_PERMISSION_LEVEL,
                 ANVIL_PERMISSION_LEVEL -> SharedSuggestionProvider.suggest(List.of("0", "1", "2", "3", "4"), builder);
            case NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS -> SharedSuggestionProvider.suggest(List.of("5", "10", "15"), builder);
            default -> Suggestions.empty();
        };
    }

    private static int show(CommandContext<CommandSourceStack> context) {
        Config config = Config.getInstance();
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6misc-features config"), false);

        sendConfigLine(source, "maxSearchRadius", "§f" + config.getMaxSearchRadius(),
            "default = " + Config.DEFAULT_MAX_SEARCH_RADIUS + ", min = 1, max = " + Config.ABSOLUTE_MAX_SEARCH_RADIUS,
            "Maximum radius allowed for /find block searches.");
        sendConfigLine(source, "maxItemSearchRadius", "§f" + config.getMaxItemSearchRadius(),
            "default = " + Config.DEFAULT_MAX_ITEM_SEARCH_RADIUS + ", min = 1, max = " + Config.ABSOLUTE_MAX_SEARCH_RADIUS,
            "Maximum radius allowed for /find item searches.");
        sendConfigLine(source, "maxEntitySearchRadius", "§f" + config.getMaxEntitySearchRadius(),
            "default = " + Config.DEFAULT_MAX_ENTITY_SEARCH_RADIUS + ", min = 1, max = " + Config.ABSOLUTE_MAX_SEARCH_RADIUS,
            "Maximum radius allowed for /find entity searches.");
        sendConfigLine(source, "maxSearchResults", "§f" + config.getMaxSearchResults(),
            "default = " + Config.DEFAULT_MAX_SEARCH_RESULTS + ", min = 1",
            "Maximum printed /find block results per target before truncation.");
        sendConfigLine(source, "maxFindItemResults", "§f" + config.getMaxFindItemResults(),
            "default = " + Config.DEFAULT_MAX_FIND_ITEM_RESULTS + ", min = 1",
            "Maximum printed /find item container results per target before truncation.");
        sendConfigLine(source, "maxFindEntityResults", "§f" + config.getMaxFindEntityResults(),
            "default = " + Config.DEFAULT_MAX_FIND_ENTITY_RESULTS + ", min = 1",
            "Maximum printed /find entity results per target before truncation.");

        sendConfigLine(source, "searchUnloadedChunks", formatBoolean(config.shouldSearchUnloadedChunks()),
            "default = " + Config.DEFAULT_SEARCH_UNLOADED_CHUNKS,
            "Controls unloaded-chunk behavior for /find block.");
        sendConfigLine(source, "searchItemsInUnloadedChunks", formatBoolean(config.shouldSearchItemsInUnloadedChunks()),
            "default = " + Config.DEFAULT_SEARCH_ITEMS_IN_UNLOADED_CHUNKS,
            "Controls unloaded-chunk behavior for /find item.");
        sendConfigLine(source, "searchEntitiesInUnloadedChunks", formatBoolean(config.shouldSearchEntitiesInUnloadedChunks()),
            "default = " + Config.DEFAULT_SEARCH_ENTITIES_IN_UNLOADED_CHUNKS,
            "Controls unloaded-chunk behavior for /find entity.");

        sendConfigLine(source, "developerMode", formatBoolean(config.isDeveloperMode()),
            "default = " + Config.DEFAULT_DEVELOPER_MODE,
            "Unlocks developer-only toggles and relaxed caps.");
        sendConfigLine(source, "verboseLogging", formatBoolean(config.isVerboseLogging()),
            "default = " + Config.DEFAULT_VERBOSE_LOGGING,
            "Logs extra command diagnostics when developer mode is enabled.");

        sendConfigLine(source, "anvilAutoInsertArmor", formatBoolean(config.shouldAnvilAutoInsertArmor()),
            "default = " + Config.DEFAULT_ANVIL_AUTO_INSERT_ARMOR,
            "Auto-inserts held armor-like item into /anvil input slot.");
        sendConfigLine(source, "anvilAutoInsertWeapons", formatBoolean(config.shouldAnvilAutoInsertWeapons()),
            "default = " + Config.DEFAULT_ANVIL_AUTO_INSERT_WEAPONS,
            "Auto-inserts held weapon/tool into /anvil input slot.");
        sendConfigLine(source, "anvilAutoInsertNameTags", formatBoolean(config.shouldAnvilAutoInsertNameTags()),
            "default = " + Config.DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS,
            "Auto-inserts held name tag into /anvil input slot.");

        sendConfigLine(source, "allowNegativeEnchants", formatBoolean(config.shouldAllowNegativeEnchants()),
            "default = " + Config.DEFAULT_ALLOW_NEGATIVE_ENCHANTS,
            "Allows /mf enchant levels below 0 in developer mode.");
        sendConfigLine(source, "allowHighLevelEnchants", formatBoolean(config.shouldAllowHighLevelEnchants()),
            "default = " + Config.DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS + ", dev-only, allows >255",
            "Allows /mf enchant levels above 255 in developer mode.");
        sendConfigLine(source, "requireConfirmForUnsafeEnchants", formatBoolean(config.shouldRequireConfirmForUnsafeEnchants()),
            "default = " + Config.DEFAULT_REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS + ", covers <0 and >255",
            "Requires repeating unsafe enchant command within confirmation window.");
        sendConfigLine(source, "negativeEnchantConfirmWindowSeconds", "§f" + config.getNegativeEnchantConfirmWindowSeconds(),
            "default = " + Config.DEFAULT_NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS + ", min = 1, max = 30",
            "Seconds allowed to repeat unsafe enchant command for confirmation.");

        sendConfigLine(source, "requirePermissionForEnchant", formatBoolean(config.shouldRequirePermissionForEnchant()),
            "default = " + Config.DEFAULT_REQUIRE_PERMISSION_FOR_ENCHANT,
            "Requires configured permission level for /mf enchant.");
        sendConfigLine(source, "enchantPermissionLevel", "§f" + config.getEnchantPermissionLevel(),
            "default = " + Config.DEFAULT_ENCHANT_PERMISSION_LEVEL + ", min = 0, max = 4",
            "Permission level required when enchant permission gate is enabled.");
        sendConfigLine(source, "requirePermissionForAnvil", formatBoolean(config.shouldRequirePermissionForAnvil()),
            "default = " + Config.DEFAULT_REQUIRE_PERMISSION_FOR_ANVIL,
            "Requires configured permission level for /anvil.");
        sendConfigLine(source, "anvilPermissionLevel", "§f" + config.getAnvilPermissionLevel(),
            "default = " + Config.DEFAULT_ANVIL_PERMISSION_LEVEL + ", min = 0, max = 4",
            "Permission level required when anvil permission gate is enabled.");

        sendConfigLine(source, "preventCreativePacketCrashOnUnsafeEnchants", formatBoolean(config.shouldPreventCreativePacketCrashOnUnsafeEnchants()),
            "default = " + Config.DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS + ", covers <0 and >255",
            "Blocks unsafe enchants on creative targets to reduce known packet crash risk.");
        sendConfigLine(source, "fixHighLevelEnchantText", formatBoolean(config.shouldFixHighLevelEnchantText()),
            "default = " + Config.DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT,
            "Fixes tooltip text display for enchant levels above 10.");
        sendConfigLine(source, "highLevelEnchantStyleRoman", formatBoolean(config.shouldUseRomanForHighLevelEnchantText()),
            "default = " + Config.DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN,
            "Uses Roman numerals for high enchant level tooltip formatting.");

        return 1;
    }

        private static void sendConfigLine(CommandSourceStack source, String key, String value, String details, String description) {
        Component keyWithHover = Component.literal("§b" + key)
            .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(description))));

        source.sendSuccess(() -> Component.literal("§7- ")
            .append(keyWithHover)
            .append(Component.literal("§7: "))
            .append(Component.literal(value))
            .append(Component.literal(" §8[" + details + "]")), false);
        }

    private static int setSetting(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String rawSetting = StringArgumentType.getString(context, "setting");
        SettingKey setting = SettingKey.fromInput(rawSetting);
        String value = StringArgumentType.getString(context, "value");
        Config config = Config.getInstance();

        if (setting == null) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown config setting: " + rawSetting + ". Use /mf config show to list valid settings."
            ));
            return 0;
        }

        switch (setting) {
            case MAX_SEARCH_RADIUS -> config.setMaxSearchRadius(parseInt(value, rawSetting));
            case MAX_ITEM_SEARCH_RADIUS -> config.setMaxItemSearchRadius(parseInt(value, rawSetting));
            case MAX_ENTITY_SEARCH_RADIUS -> config.setMaxEntitySearchRadius(parseInt(value, rawSetting));
            case MAX_SEARCH_RESULTS -> config.setMaxSearchResults(parseInt(value, rawSetting));
            case MAX_FIND_ITEM_RESULTS -> config.setMaxFindItemResults(parseInt(value, rawSetting));
            case MAX_FIND_ENTITY_RESULTS -> config.setMaxFindEntityResults(parseInt(value, rawSetting));
            case SEARCH_UNLOADED_CHUNKS -> config.setSearchUnloadedChunks(parseBoolean(value, rawSetting));
            case SEARCH_ITEMS_IN_UNLOADED_CHUNKS -> config.setSearchItemsInUnloadedChunks(parseBoolean(value, rawSetting));
            case SEARCH_ENTITIES_IN_UNLOADED_CHUNKS -> config.setSearchEntitiesInUnloadedChunks(parseBoolean(value, rawSetting));
            case DEVELOPER_MODE -> config.setDeveloperMode(parseBoolean(value, rawSetting));
            case VERBOSE_LOGGING -> {
                boolean parsed = parseBoolean(value, rawSetting);
                if (parsed && !config.isDeveloperMode()) {
                    throw new CommandSyntaxException(null, Component.literal("misc-features: enable developerMode before turning on verboseLogging."));
                }
                config.setVerboseLogging(parsed);
            }
            case ANVIL_AUTO_INSERT_ARMOR -> config.setAnvilAutoInsertArmor(parseBoolean(value, rawSetting));
            case ANVIL_AUTO_INSERT_WEAPONS -> config.setAnvilAutoInsertWeapons(parseBoolean(value, rawSetting));
            case ANVIL_AUTO_INSERT_NAME_TAGS -> config.setAnvilAutoInsertNameTags(parseBoolean(value, rawSetting));
            case ALLOW_NEGATIVE_ENCHANTS -> {
                boolean parsed = parseBoolean(value, rawSetting);
                if (parsed && !config.isDeveloperMode()) {
                    throw new CommandSyntaxException(null, Component.literal("misc-features: enable developerMode before turning on allowNegativeEnchants."));
                }
                config.setAllowNegativeEnchants(parsed);
            }
            case ALLOW_HIGH_LEVEL_ENCHANTS -> {
                boolean parsed = parseBoolean(value, rawSetting);
                if (parsed && !config.isDeveloperMode()) {
                    throw new CommandSyntaxException(null, Component.literal("misc-features: enable developerMode before turning on allowHighLevelEnchants."));
                }
                config.setAllowHighLevelEnchants(parsed);
            }
            case REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS -> config.setRequireConfirmForUnsafeEnchants(parseBoolean(value, rawSetting));
            case REQUIRE_CONFIRM_FOR_NEGATIVE_ENCHANTS -> config.setRequireConfirmForNegativeEnchants(parseBoolean(value, rawSetting));
            case NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS -> config.setNegativeEnchantConfirmWindowSeconds(parseInt(value, rawSetting));
            case REQUIRE_PERMISSION_FOR_ENCHANT -> config.setRequirePermissionForEnchant(parseBoolean(value, rawSetting));
            case ENCHANT_PERMISSION_LEVEL -> config.setEnchantPermissionLevel(parseInt(value, rawSetting));
            case REQUIRE_PERMISSION_FOR_ANVIL -> config.setRequirePermissionForAnvil(parseBoolean(value, rawSetting));
            case ANVIL_PERMISSION_LEVEL -> config.setAnvilPermissionLevel(parseInt(value, rawSetting));
            case PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS,
                 PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS -> config.setPreventCreativePacketCrashOnUnsafeEnchants(parseBoolean(value, rawSetting));
            case FIX_HIGH_LEVEL_ENCHANT_TEXT -> config.setFixHighLevelEnchantText(parseBoolean(value, rawSetting));
            case HIGH_LEVEL_ENCHANT_STYLE_ROMAN -> config.setHighLevelEnchantStyleRoman(parseBoolean(value, rawSetting));
        }

        config.save();

        if (requiresCommandRefresh(setting)) {
            refreshCommands(context.getSource());
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "misc-features: " + rawSetting + " set to " + getCurrentValue(config, setting)
        ), true);

        if (setting == SettingKey.ALLOW_NEGATIVE_ENCHANTS && config.shouldAllowNegativeEnchants()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "§6Warning: unsafe enchant levels are unstable developer behavior and can crash server/client or corrupt inventory data."
            ), false);
        }

        if (setting == SettingKey.ALLOW_HIGH_LEVEL_ENCHANTS && config.shouldAllowHighLevelEnchants()) {
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Warning: unsafe enchant levels are unstable developer behavior and can crash server/client or corrupt inventory data."
            ), false);
        }

        return 1;
    }

    private static String getCurrentValue(Config config, SettingKey setting) {
        return switch (setting) {
            case MAX_SEARCH_RADIUS -> String.valueOf(config.getMaxSearchRadius());
            case MAX_ITEM_SEARCH_RADIUS -> String.valueOf(config.getMaxItemSearchRadius());
            case MAX_ENTITY_SEARCH_RADIUS -> String.valueOf(config.getMaxEntitySearchRadius());
            case MAX_SEARCH_RESULTS -> String.valueOf(config.getMaxSearchResults());
            case MAX_FIND_ITEM_RESULTS -> String.valueOf(config.getMaxFindItemResults());
            case MAX_FIND_ENTITY_RESULTS -> String.valueOf(config.getMaxFindEntityResults());
            case SEARCH_UNLOADED_CHUNKS -> String.valueOf(config.shouldSearchUnloadedChunks());
            case SEARCH_ITEMS_IN_UNLOADED_CHUNKS -> String.valueOf(config.shouldSearchItemsInUnloadedChunks());
            case SEARCH_ENTITIES_IN_UNLOADED_CHUNKS -> String.valueOf(config.shouldSearchEntitiesInUnloadedChunks());
            case DEVELOPER_MODE -> String.valueOf(config.isDeveloperMode());
            case VERBOSE_LOGGING -> String.valueOf(config.isVerboseLogging());
            case ANVIL_AUTO_INSERT_ARMOR -> String.valueOf(config.shouldAnvilAutoInsertArmor());
            case ANVIL_AUTO_INSERT_WEAPONS -> String.valueOf(config.shouldAnvilAutoInsertWeapons());
            case ANVIL_AUTO_INSERT_NAME_TAGS -> String.valueOf(config.shouldAnvilAutoInsertNameTags());
            case ALLOW_NEGATIVE_ENCHANTS -> String.valueOf(config.shouldAllowNegativeEnchants());
            case ALLOW_HIGH_LEVEL_ENCHANTS -> String.valueOf(config.shouldAllowHighLevelEnchants());
            case REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS,
                 REQUIRE_CONFIRM_FOR_NEGATIVE_ENCHANTS -> String.valueOf(config.shouldRequireConfirmForUnsafeEnchants());
            case NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS -> String.valueOf(config.getNegativeEnchantConfirmWindowSeconds());
            case REQUIRE_PERMISSION_FOR_ENCHANT -> String.valueOf(config.shouldRequirePermissionForEnchant());
            case ENCHANT_PERMISSION_LEVEL -> String.valueOf(config.getEnchantPermissionLevel());
            case REQUIRE_PERMISSION_FOR_ANVIL -> String.valueOf(config.shouldRequirePermissionForAnvil());
            case ANVIL_PERMISSION_LEVEL -> String.valueOf(config.getAnvilPermissionLevel());
            case PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS,
                 PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS -> String.valueOf(config.shouldPreventCreativePacketCrashOnUnsafeEnchants());
            case FIX_HIGH_LEVEL_ENCHANT_TEXT -> String.valueOf(config.shouldFixHighLevelEnchantText());
            case HIGH_LEVEL_ENCHANT_STYLE_ROMAN -> String.valueOf(config.shouldUseRomanForHighLevelEnchantText());
        };
    }

    private static boolean requiresCommandRefresh(SettingKey setting) {
        return switch (setting) {
            case DEVELOPER_MODE,
                 ALLOW_NEGATIVE_ENCHANTS,
                 ALLOW_HIGH_LEVEL_ENCHANTS,
                 REQUIRE_PERMISSION_FOR_ENCHANT,
                 ENCHANT_PERMISSION_LEVEL,
                 REQUIRE_PERMISSION_FOR_ANVIL,
                 ANVIL_PERMISSION_LEVEL,
                 PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS,
                 PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS -> true;
            default -> false;
        };
    }

    private static int parseInt(String value, String setting) throws CommandSyntaxException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CommandSyntaxException(null, Component.literal(
                    "Invalid integer for " + setting + ": " + value
            ));
        }
    }

    private static boolean parseBoolean(String value, String setting) throws CommandSyntaxException {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new CommandSyntaxException(null, Component.literal(
                "Invalid boolean for " + setting + ": " + value + " (expected true/false)"
        ));
    }

    private static String formatBoolean(boolean value) {
        return value ? "§atrue" : "§cfalse";
    }

    private static void refreshCommands(CommandSourceStack source) {
        if (source.getServer() == null) {
            return;
        }

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            source.getServer().getCommands().sendCommands(player);
        }
    }

    private enum SettingKey {
        MAX_SEARCH_RADIUS("maxSearchRadius"),
        MAX_ITEM_SEARCH_RADIUS("maxItemSearchRadius"),
        MAX_ENTITY_SEARCH_RADIUS("maxEntitySearchRadius"),
        MAX_SEARCH_RESULTS("maxSearchResults"),
        MAX_FIND_ITEM_RESULTS("maxFindItemResults"),
        MAX_FIND_ENTITY_RESULTS("maxFindEntityResults"),
        SEARCH_UNLOADED_CHUNKS("searchUnloadedChunks"),
        SEARCH_ITEMS_IN_UNLOADED_CHUNKS("searchItemsInUnloadedChunks"),
        SEARCH_ENTITIES_IN_UNLOADED_CHUNKS("searchEntitiesInUnloadedChunks"),
        DEVELOPER_MODE("developerMode"),
        VERBOSE_LOGGING("verboseLogging"),
        ANVIL_AUTO_INSERT_ARMOR("anvilAutoInsertArmor"),
        ANVIL_AUTO_INSERT_WEAPONS("anvilAutoInsertWeapons"),
        ANVIL_AUTO_INSERT_NAME_TAGS("anvilAutoInsertNameTags"),
        ALLOW_NEGATIVE_ENCHANTS("allowNegativeEnchants"),
        ALLOW_HIGH_LEVEL_ENCHANTS("allowHighLevelEnchants"),
        REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS("requireConfirmForUnsafeEnchants"),
        REQUIRE_CONFIRM_FOR_NEGATIVE_ENCHANTS("requireConfirmForNegativeEnchants"),
        NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS("negativeEnchantConfirmWindowSeconds"),
        REQUIRE_PERMISSION_FOR_ENCHANT("requirePermissionForEnchant"),
        ENCHANT_PERMISSION_LEVEL("enchantPermissionLevel"),
        REQUIRE_PERMISSION_FOR_ANVIL("requirePermissionForAnvil"),
        ANVIL_PERMISSION_LEVEL("anvilPermissionLevel"),
        PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS("preventCreativePacketCrashOnUnsafeEnchants"),
        PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS("preventCreativePacketCrashOnNegativeEnchants"),
        FIX_HIGH_LEVEL_ENCHANT_TEXT("fixHighLevelEnchantText"),
        HIGH_LEVEL_ENCHANT_STYLE_ROMAN("highLevelEnchantStyleRoman");

        private final String key;
        private final String normalized;

        SettingKey(String key) {
            this.key = key;
            this.normalized = key.toLowerCase(Locale.ROOT);
        }

        private String key() {
            return key;
        }

        private static SettingKey fromInput(String key) {
            String normalizedInput = key.toLowerCase(Locale.ROOT);
            for (SettingKey setting : values()) {
                if (setting.normalized.equals(normalizedInput)) {
                    return setting;
                }
            }
            return null;
        }
    }
}
