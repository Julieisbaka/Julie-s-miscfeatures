package com.example.miscfeatures.command;

import com.example.miscfeatures.config.MiscFeaturesConfig;
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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ConfigCommand {

    private static final List<String> SETTING_KEYS = List.of(
            "maxSearchRadius",
            "maxItemSearchRadius",
            "maxEntitySearchRadius",
            "maxSearchResults",
            "maxFindItemResults",
            "maxFindEntityResults",
            "searchUnloadedChunks",
            "searchItemsInUnloadedChunks",
            "searchEntitiesInUnloadedChunks",
            "developerMode",
            "verboseLogging",
            "anvilAutoInsertArmor",
            "anvilAutoInsertWeapons",
            "anvilAutoInsertNameTags",
            "allowNegativeEnchants",
            "allowHighLevelEnchants",
            "requireConfirmForUnsafeEnchants",
            "requireConfirmForNegativeEnchants",
            "negativeEnchantConfirmWindowSeconds",
            "requirePermissionForEnchant",
            "enchantPermissionLevel",
            "requirePermissionForAnvil",
            "anvilPermissionLevel",
            "preventCreativePacketCrashOnUnsafeEnchants",
            "preventCreativePacketCrashOnNegativeEnchants",
            "fixHighLevelEnchantText",
            "highLevelEnchantStyleRoman"
    );

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
        String setting = StringArgumentType.getString(context, "setting").toLowerCase(Locale.ROOT);
        return switch (setting) {
            case "searchunloadedchunks", "searchitemsinunloadedchunks", "searchentitiesinunloadedchunks",
                 "developermode", "verboselogging", "anvilautoinsertarmor", "anvilautoinsertweapons",
                 "anvilautoinsertnametags", "allownegativeenchants", "allowhighlevelenchants", "requireconfirmforunsafeenchants", "requireconfirmfornegativeenchants",
                "requirepermissionforenchant", "requirepermissionforanvil", "preventcreativepacketcrashonunsafeenchants", "preventcreativepacketcrashonnegativeenchants", "fixhighlevelenchanttext",
                 "highlevelenchantstyleroman" -> SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
            case "enchantpermissionlevel", "anvilpermissionlevel" -> SharedSuggestionProvider.suggest(List.of("0", "1", "2", "3", "4"), builder);
            case "negativeenchantconfirmwindowseconds" -> SharedSuggestionProvider.suggest(List.of("5", "10", "15"), builder);
            default -> Suggestions.empty();
        };
    }

    private static int show(CommandContext<CommandSourceStack> context) {
        MiscFeaturesConfig config = MiscFeaturesConfig.getInstance();
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6misc-features config"), false);

        sendConfigLine(source, "maxSearchRadius", "§f" + config.getMaxSearchRadius(),
            "default = " + MiscFeaturesConfig.DEFAULT_MAX_SEARCH_RADIUS + ", min = 1, max = " + MiscFeaturesConfig.ABSOLUTE_MAX_SEARCH_RADIUS,
            "Maximum radius allowed for /find block searches.");
        sendConfigLine(source, "maxItemSearchRadius", "§f" + config.getMaxItemSearchRadius(),
            "default = " + MiscFeaturesConfig.DEFAULT_MAX_ITEM_SEARCH_RADIUS + ", min = 1, max = " + MiscFeaturesConfig.ABSOLUTE_MAX_SEARCH_RADIUS,
            "Maximum radius allowed for /find item searches.");
        sendConfigLine(source, "maxEntitySearchRadius", "§f" + config.getMaxEntitySearchRadius(),
            "default = " + MiscFeaturesConfig.DEFAULT_MAX_ENTITY_SEARCH_RADIUS + ", min = 1, max = " + MiscFeaturesConfig.ABSOLUTE_MAX_SEARCH_RADIUS,
            "Maximum radius allowed for /find entity searches.");
        sendConfigLine(source, "maxSearchResults", "§f" + config.getMaxSearchResults(),
            "default = " + MiscFeaturesConfig.DEFAULT_MAX_SEARCH_RESULTS + ", min = 1",
            "Maximum printed /find block results per target before truncation.");
        sendConfigLine(source, "maxFindItemResults", "§f" + config.getMaxFindItemResults(),
            "default = " + MiscFeaturesConfig.DEFAULT_MAX_FIND_ITEM_RESULTS + ", min = 1",
            "Maximum printed /find item container results per target before truncation.");
        sendConfigLine(source, "maxFindEntityResults", "§f" + config.getMaxFindEntityResults(),
            "default = " + MiscFeaturesConfig.DEFAULT_MAX_FIND_ENTITY_RESULTS + ", min = 1",
            "Maximum printed /find entity results per target before truncation.");

        sendConfigLine(source, "searchUnloadedChunks", formatBoolean(config.shouldSearchUnloadedChunks()),
            "default = " + MiscFeaturesConfig.DEFAULT_SEARCH_UNLOADED_CHUNKS,
            "Controls unloaded-chunk behavior for /find block.");
        sendConfigLine(source, "searchItemsInUnloadedChunks", formatBoolean(config.shouldSearchItemsInUnloadedChunks()),
            "default = " + MiscFeaturesConfig.DEFAULT_SEARCH_ITEMS_IN_UNLOADED_CHUNKS,
            "Controls unloaded-chunk behavior for /find item.");
        sendConfigLine(source, "searchEntitiesInUnloadedChunks", formatBoolean(config.shouldSearchEntitiesInUnloadedChunks()),
            "default = " + MiscFeaturesConfig.DEFAULT_SEARCH_ENTITIES_IN_UNLOADED_CHUNKS,
            "Controls unloaded-chunk behavior for /find entity.");

        sendConfigLine(source, "developerMode", formatBoolean(config.isDeveloperMode()),
            "default = " + MiscFeaturesConfig.DEFAULT_DEVELOPER_MODE,
            "Unlocks developer-only toggles and relaxed caps.");
        sendConfigLine(source, "verboseLogging", formatBoolean(config.isVerboseLogging()),
            "default = " + MiscFeaturesConfig.DEFAULT_VERBOSE_LOGGING,
            "Logs extra command diagnostics when developer mode is enabled.");

        sendConfigLine(source, "anvilAutoInsertArmor", formatBoolean(config.shouldAnvilAutoInsertArmor()),
            "default = " + MiscFeaturesConfig.DEFAULT_ANVIL_AUTO_INSERT_ARMOR,
            "Auto-inserts held armor-like item into /anvil input slot.");
        sendConfigLine(source, "anvilAutoInsertWeapons", formatBoolean(config.shouldAnvilAutoInsertWeapons()),
            "default = " + MiscFeaturesConfig.DEFAULT_ANVIL_AUTO_INSERT_WEAPONS,
            "Auto-inserts held weapon/tool into /anvil input slot.");
        sendConfigLine(source, "anvilAutoInsertNameTags", formatBoolean(config.shouldAnvilAutoInsertNameTags()),
            "default = " + MiscFeaturesConfig.DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS,
            "Auto-inserts held name tag into /anvil input slot.");

        sendConfigLine(source, "allowNegativeEnchants", formatBoolean(config.shouldAllowNegativeEnchants()),
            "default = " + MiscFeaturesConfig.DEFAULT_ALLOW_NEGATIVE_ENCHANTS,
            "Allows /mf enchant levels below 0 in developer mode.");
        sendConfigLine(source, "allowHighLevelEnchants", formatBoolean(config.shouldAllowHighLevelEnchants()),
            "default = " + MiscFeaturesConfig.DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS + ", dev-only, allows >255",
            "Allows /mf enchant levels above 255 in developer mode.");
        sendConfigLine(source, "requireConfirmForUnsafeEnchants", formatBoolean(config.shouldRequireConfirmForUnsafeEnchants()),
            "default = " + MiscFeaturesConfig.DEFAULT_REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS + ", covers <0 and >255",
            "Requires repeating unsafe enchant command within confirmation window.");
        sendConfigLine(source, "negativeEnchantConfirmWindowSeconds", "§f" + config.getNegativeEnchantConfirmWindowSeconds(),
            "default = " + MiscFeaturesConfig.DEFAULT_NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS + ", min = 1, max = 30",
            "Seconds allowed to repeat unsafe enchant command for confirmation.");

        sendConfigLine(source, "requirePermissionForEnchant", formatBoolean(config.shouldRequirePermissionForEnchant()),
            "default = " + MiscFeaturesConfig.DEFAULT_REQUIRE_PERMISSION_FOR_ENCHANT,
            "Requires configured permission level for /mf enchant.");
        sendConfigLine(source, "enchantPermissionLevel", "§f" + config.getEnchantPermissionLevel(),
            "default = " + MiscFeaturesConfig.DEFAULT_ENCHANT_PERMISSION_LEVEL + ", min = 0, max = 4",
            "Permission level required when enchant permission gate is enabled.");
        sendConfigLine(source, "requirePermissionForAnvil", formatBoolean(config.shouldRequirePermissionForAnvil()),
            "default = " + MiscFeaturesConfig.DEFAULT_REQUIRE_PERMISSION_FOR_ANVIL,
            "Requires configured permission level for /anvil.");
        sendConfigLine(source, "anvilPermissionLevel", "§f" + config.getAnvilPermissionLevel(),
            "default = " + MiscFeaturesConfig.DEFAULT_ANVIL_PERMISSION_LEVEL + ", min = 0, max = 4",
            "Permission level required when anvil permission gate is enabled.");

        sendConfigLine(source, "preventCreativePacketCrashOnUnsafeEnchants", formatBoolean(config.shouldPreventCreativePacketCrashOnUnsafeEnchants()),
            "default = " + MiscFeaturesConfig.DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS + ", covers <0 and >255",
            "Blocks unsafe enchants on creative targets to reduce known packet crash risk.");
        sendConfigLine(source, "fixHighLevelEnchantText", formatBoolean(config.shouldFixHighLevelEnchantText()),
            "default = " + MiscFeaturesConfig.DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT,
            "Fixes tooltip text display for enchant levels above 10.");
        sendConfigLine(source, "highLevelEnchantStyleRoman", formatBoolean(config.shouldUseRomanForHighLevelEnchantText()),
            "default = " + MiscFeaturesConfig.DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN,
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
        String setting = rawSetting.toLowerCase(Locale.ROOT);
        String value = StringArgumentType.getString(context, "value");
        MiscFeaturesConfig config = MiscFeaturesConfig.getInstance();

        switch (setting) {
            case "maxsearchradius" -> config.setMaxSearchRadius(parseInt(value, rawSetting));
            case "maxitemsearchradius" -> config.setMaxItemSearchRadius(parseInt(value, rawSetting));
            case "maxentitysearchradius" -> config.setMaxEntitySearchRadius(parseInt(value, rawSetting));
            case "maxsearchresults" -> config.setMaxSearchResults(parseInt(value, rawSetting));
            case "maxfinditemresults" -> config.setMaxFindItemResults(parseInt(value, rawSetting));
            case "maxfindentityresults" -> config.setMaxFindEntityResults(parseInt(value, rawSetting));
            case "searchunloadedchunks" -> config.setSearchUnloadedChunks(parseBoolean(value, rawSetting));
            case "searchitemsinunloadedchunks" -> config.setSearchItemsInUnloadedChunks(parseBoolean(value, rawSetting));
            case "searchentitiesinunloadedchunks" -> config.setSearchEntitiesInUnloadedChunks(parseBoolean(value, rawSetting));
            case "developermode" -> config.setDeveloperMode(parseBoolean(value, rawSetting));
            case "verboselogging" -> {
                boolean parsed = parseBoolean(value, rawSetting);
                if (parsed && !config.isDeveloperMode()) {
                    throw new CommandSyntaxException(null, Component.literal("misc-features: enable developerMode before turning on verboseLogging."));
                }
                config.setVerboseLogging(parsed);
            }
            case "anvilautoinsertarmor" -> config.setAnvilAutoInsertArmor(parseBoolean(value, rawSetting));
            case "anvilautoinsertweapons" -> config.setAnvilAutoInsertWeapons(parseBoolean(value, rawSetting));
            case "anvilautoinsertnametags" -> config.setAnvilAutoInsertNameTags(parseBoolean(value, rawSetting));
            case "allownegativeenchants" -> {
                boolean parsed = parseBoolean(value, rawSetting);
                if (parsed && !config.isDeveloperMode()) {
                    throw new CommandSyntaxException(null, Component.literal("misc-features: enable developerMode before turning on allowNegativeEnchants."));
                }
                config.setAllowNegativeEnchants(parsed);
            }
            case "allowhighlevelenchants" -> {
                boolean parsed = parseBoolean(value, rawSetting);
                if (parsed && !config.isDeveloperMode()) {
                    throw new CommandSyntaxException(null, Component.literal("misc-features: enable developerMode before turning on allowHighLevelEnchants."));
                }
                config.setAllowHighLevelEnchants(parsed);
            }
            case "requireconfirmforunsafeenchants" -> config.setRequireConfirmForUnsafeEnchants(parseBoolean(value, rawSetting));
            case "requireconfirmfornegativeenchants" -> config.setRequireConfirmForNegativeEnchants(parseBoolean(value, rawSetting));
            case "negativeenchantconfirmwindowseconds" -> config.setNegativeEnchantConfirmWindowSeconds(parseInt(value, rawSetting));
            case "requirepermissionforenchant" -> config.setRequirePermissionForEnchant(parseBoolean(value, rawSetting));
            case "enchantpermissionlevel" -> config.setEnchantPermissionLevel(parseInt(value, rawSetting));
            case "requirepermissionforanvil" -> config.setRequirePermissionForAnvil(parseBoolean(value, rawSetting));
            case "anvilpermissionlevel" -> config.setAnvilPermissionLevel(parseInt(value, rawSetting));
            case "preventcreativepacketcrashonunsafeenchants" -> config.setPreventCreativePacketCrashOnUnsafeEnchants(parseBoolean(value, rawSetting));
            case "preventcreativepacketcrashonnegativeenchants" -> config.setPreventCreativePacketCrashOnUnsafeEnchants(parseBoolean(value, rawSetting));
            case "fixhighlevelenchanttext" -> config.setFixHighLevelEnchantText(parseBoolean(value, rawSetting));
            case "highlevelenchantstyleroman" -> config.setHighLevelEnchantStyleRoman(parseBoolean(value, rawSetting));
            default -> {
                context.getSource().sendFailure(Component.literal(
                        "Unknown config setting: " + rawSetting + ". Use /mf config show to list valid settings."
                ));
                return 0;
            }
        }

        config.save();

        if (setting.equals("developermode")
                || setting.equals("allownegativeenchants")
                || setting.equals("allowhighlevelenchants")
                || setting.equals("requirepermissionforenchant")
                || setting.equals("enchantpermissionlevel")
                || setting.equals("requirepermissionforanvil")
                || setting.equals("anvilpermissionlevel")
                || setting.equals("preventcreativepacketcrashonunsafeenchants")
                || setting.equals("preventcreativepacketcrashonnegativeenchants")) {
            refreshCommands(context.getSource());
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "misc-features: " + rawSetting + " set to " + getCurrentValue(config, setting)
        ), true);

        if (setting.equals("allownegativeenchants") && config.shouldAllowNegativeEnchants()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "§6Warning: unsafe enchant levels are unstable developer behavior and can crash server/client or corrupt inventory data."
            ), false);
        }

        if (setting.equals("allowhighlevelenchants") && config.shouldAllowHighLevelEnchants()) {
            context.getSource().sendSuccess(() -> Component.literal(
                "§6Warning: unsafe enchant levels are unstable developer behavior and can crash server/client or corrupt inventory data."
            ), false);
        }

        return 1;
    }

    private static String getCurrentValue(MiscFeaturesConfig config, String normalizedSetting) {
        return switch (normalizedSetting) {
            case "maxsearchradius" -> String.valueOf(config.getMaxSearchRadius());
            case "maxitemsearchradius" -> String.valueOf(config.getMaxItemSearchRadius());
            case "maxentitysearchradius" -> String.valueOf(config.getMaxEntitySearchRadius());
            case "maxsearchresults" -> String.valueOf(config.getMaxSearchResults());
            case "maxfinditemresults" -> String.valueOf(config.getMaxFindItemResults());
            case "maxfindentityresults" -> String.valueOf(config.getMaxFindEntityResults());
            case "searchunloadedchunks" -> String.valueOf(config.shouldSearchUnloadedChunks());
            case "searchitemsinunloadedchunks" -> String.valueOf(config.shouldSearchItemsInUnloadedChunks());
            case "searchentitiesinunloadedchunks" -> String.valueOf(config.shouldSearchEntitiesInUnloadedChunks());
            case "developermode" -> String.valueOf(config.isDeveloperMode());
            case "verboselogging" -> String.valueOf(config.isVerboseLogging());
            case "anvilautoinsertarmor" -> String.valueOf(config.shouldAnvilAutoInsertArmor());
            case "anvilautoinsertweapons" -> String.valueOf(config.shouldAnvilAutoInsertWeapons());
            case "anvilautoinsertnametags" -> String.valueOf(config.shouldAnvilAutoInsertNameTags());
            case "allownegativeenchants" -> String.valueOf(config.shouldAllowNegativeEnchants());
            case "allowhighlevelenchants" -> String.valueOf(config.shouldAllowHighLevelEnchants());
            case "requireconfirmforunsafeenchants", "requireconfirmfornegativeenchants" -> String.valueOf(config.shouldRequireConfirmForUnsafeEnchants());
            case "negativeenchantconfirmwindowseconds" -> String.valueOf(config.getNegativeEnchantConfirmWindowSeconds());
            case "requirepermissionforenchant" -> String.valueOf(config.shouldRequirePermissionForEnchant());
            case "enchantpermissionlevel" -> String.valueOf(config.getEnchantPermissionLevel());
            case "requirepermissionforanvil" -> String.valueOf(config.shouldRequirePermissionForAnvil());
            case "anvilpermissionlevel" -> String.valueOf(config.getAnvilPermissionLevel());
            case "preventcreativepacketcrashonunsafeenchants", "preventcreativepacketcrashonnegativeenchants" -> String.valueOf(config.shouldPreventCreativePacketCrashOnUnsafeEnchants());
            case "fixhighlevelenchanttext" -> String.valueOf(config.shouldFixHighLevelEnchantText());
            case "highlevelenchantstyleroman" -> String.valueOf(config.shouldUseRomanForHighLevelEnchantText());
            default -> "?";
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
}
