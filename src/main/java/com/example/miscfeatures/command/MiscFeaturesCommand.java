package com.example.miscfeatures.command;

import com.example.miscfeatures.MiscFeaturesMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class MiscFeaturesCommand {

    private MiscFeaturesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRootLiteral("miscfeatures"));
        dispatcher.register(buildRootLiteral("mf"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildRootLiteral(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .executes(MiscFeaturesCommand::showHelp)
                .then(Commands.literal("help")
                .executes(MiscFeaturesCommand::showHelp)
                .then(Commands.literal("enchant").executes(MiscFeaturesCommand::showHelpEnchant))
                .then(Commands.literal("anvil").executes(MiscFeaturesCommand::showHelpAnvil))
                .then(Commands.literal("config").executes(MiscFeaturesCommand::showHelpConfig)))
            .then(Commands.literal("locale")
                .then(Commands.literal("coverage")
                    .executes(MiscFeaturesCommand::showLocaleCoverage)));
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6JulieISBaka Misc Features Help\n"
                        + "§7- §b/mf help enchant§7: enchant usage, examples, and warnings\n"
                        + "§7- §b/mf help anvil§7: anvil usage and permission notes\n"
                        + "§7- §b/mf help config§7: config examples and key autocomplete usage\n"
                        + "§7- §b/mf locale coverage§7: show missing translation keys per language\n"
                        + "§7- §b/mf config show§7: view config with defaults/ranges\n"
                        + "§7- §b/mf config set <setting> <value>§7: change config values (with suggestions)\n"
                        + "§7- §b/mf enchant <target> <enchantment> <level>§7: unsafe enchant target main hand\n"
                        + "§7- §b/mf enchant dry-run <target> <enchantment> <level>§7: preview unsafe enchant checks\n"
                        + "§7- §b/anvil§7: open creative virtual anvil\n"
                        + "§7- §b/wear <head|chest|legs|feet>§7: move main-hand item into chosen armor slot\\n"
                        + "§7- §b/find block|item|entity ...§7: search helpers with --sort/--page/--limit\n"
                        + "§6Note: Negative and >255 enchant levels are developer-only and unstable; they can crash servers/clients."
        ), false);
        return 1;
    }

    private static int showHelpEnchant(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6/mf enchant help\n"
                        + "§7Syntax: §b/mf enchant <target> <enchantment> <level>\n"
                    + "§7Dry-run: §b/mf enchant dry-run <target> <enchantment> <level>\n"
                        + "§7Example: §b/mf enchant @s minecraft:aqua_affinity 1\n"
                        + "§7Example: §b/mf enchant @s sharpness 10\n"
                        + "§7Negative levels require developerMode + allowNegativeEnchants.\n"
                        + "§7Levels above 255 require developerMode + allowHighLevelEnchants.\n"
                        + "§6Warning: Unsafe levels are unstable and may crash server/client or corrupt data."
        ), false);
        return 1;
    }

    private static int showHelpAnvil(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6/anvil help\n"
                        + "§7Syntax: §b/anvil\n"
                        + "§7Requires creative mode.\n"
                        + "§7Optional permission gate is controlled by §brequirePermissionForAnvil§7 + §banvilPermissionLevel§7.\n"
                        + "§7Auto-insert behavior is controlled by anvilAutoInsert* config toggles."
        ), false);
        return 1;
    }

    private static int showHelpConfig(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6/mf config help\n"
                        + "§7Show all settings: §b/mf config show\n"
                        + "§7Set by path: §b/mf config set <setting> <value>\n"
                        + "§7Example: §b/mf config set developerMode true\n"
                        + "§7Example: §b/mf config set allowNegativeEnchants true\n"
                        + "§7Example: §b/mf config set allowHighLevelEnchants true\n"
                        + "§7Example: §b/mf config set enchantPermissionLevel 2\n"
                        + "§7Tip: setting and boolean values have autocomplete suggestions."
        ), false);
        return 1;
    }

    private static int showLocaleCoverage(CommandContext<CommandSourceStack> context) {
        Path langDir = FabricLoader.getInstance()
                .getModContainer(MiscFeaturesMod.MOD_ID)
                .flatMap(container -> container.findPath("assets/misc-features/lang"))
                .orElse(null);

        if (langDir == null || !Files.exists(langDir)) {
            context.getSource().sendFailure(Component.literal("Could not locate language files for locale coverage report."));
            return 0;
        }

        Map<String, Set<String>> localeKeys = new TreeMap<>();
        try (Stream<Path> files = Files.list(langDir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        String locale = path.getFileName().toString().replace(".json", "");
                        try {
                            JsonObject object = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                            Set<String> keys = new HashSet<>();
                            object.keySet().forEach(keys::add);
                            localeKeys.put(locale, keys);
                        } catch (Exception exception) {
                            context.getSource().sendFailure(Component.literal(
                                    "Failed reading locale " + locale + ": " + exception.getMessage()
                            ));
                        }
                    });
        } catch (IOException exception) {
            context.getSource().sendFailure(Component.literal("Failed to scan lang directory: " + exception.getMessage()));
            return 0;
        }

        Set<String> base = localeKeys.get("en_us");
        if (base == null) {
            context.getSource().sendFailure(Component.literal("en_us.json is required for locale coverage baseline."));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6Locale coverage report (baseline: en_us)"), false);
        int localesWithMissing = 0;
        for (Map.Entry<String, Set<String>> entry : localeKeys.entrySet()) {
            String locale = entry.getKey();
            if ("en_us".equals(locale)) {
                continue;
            }

            List<String> missing = new ArrayList<>();
            for (String key : base) {
                if (!entry.getValue().contains(key)) {
                    missing.add(key);
                }
            }

            if (missing.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§a- " + locale + ": complete"), false);
            } else {
                localesWithMissing++;
                int previewCount = Math.min(3, missing.size());
                String preview = String.join(", ", missing.subList(0, previewCount));
                context.getSource().sendSuccess(() -> Component.literal(
                        "§e- " + locale + ": missing " + missing.size() + " key(s) §7(" + preview + (missing.size() > previewCount ? ", ..." : "") + ")"
                ), false);
            }
        }

        int finalLocalesWithMissing = localesWithMissing;
        context.getSource().sendSuccess(() -> Component.literal(
                finalLocalesWithMissing == 0
                        ? "§aAll locales are fully covered."
                        : "§6Locales with missing keys: " + finalLocalesWithMissing
        ), false);
        return 1;
    }
}