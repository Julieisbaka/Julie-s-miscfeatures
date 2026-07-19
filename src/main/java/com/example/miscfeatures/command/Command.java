package com.example.miscfeatures.command;

import com.example.miscfeatures.MiscFeatures;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Command {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(?<!%)%(?:\\d+\\$)?[sSdD]");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private Command() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRootLiteral("miscfeatures"));
        dispatcher.register(buildRootLiteral("mf"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildRootLiteral(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .executes(Command::showHelp)
                .then(Commands.literal("help")
                .executes(Command::showHelp)
                .then(Commands.literal("enchant").executes(Command::showHelpEnchant))
                .then(Commands.literal("anvil").executes(Command::showHelpAnvil))
                .then(Commands.literal("config").executes(Command::showHelpConfig)))
            .then(Commands.literal("locale")
                .then(Commands.literal("coverage")
                    .executes(Command::showLocaleCoverage)
                    .then(Commands.argument("locale", StringArgumentType.word())
                        .suggests(Command::suggestLocales)
                        .executes(Command::showLocaleCoverageForLocale)
                        .then(Commands.literal("verbose")
                            .executes(Command::showLocaleCoverageForLocaleVerbose)))))
            .then(Commands.literal("lc")
                .executes(Command::showLocaleCoverage)
                .then(Commands.argument("locale", StringArgumentType.word())
                    .suggests(Command::suggestLocales)
                    .executes(Command::showLocaleCoverageForLocale)
                    .then(Commands.literal("verbose")
                        .executes(Command::showLocaleCoverageForLocaleVerbose))));
    }

    private static CompletableFuture<Suggestions> suggestLocales(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        Path langDir = FabricLoader.getInstance()
                .getModContainer(MiscFeatures.MOD_ID)
                .flatMap(container -> container.findPath("assets/misc-features/lang"))
                .orElse(null);

        if (langDir == null || !Files.exists(langDir)) {
            return Suggestions.empty();
        }

        try (Stream<Path> files = Files.list(langDir)) {
            List<String> locales = files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(Command::normalizeLocaleIdentifier)
                    .distinct()
                    .sorted(String::compareToIgnoreCase)
                    .toList();

            String remainingLower = builder.getRemainingLowerCase();
            for (String locale : locales) {
                if (locale.startsWith(remainingLower)) {
                    builder.suggest(locale);
                }
            }
            return builder.buildFuture();
        } catch (IOException exception) {
            return Suggestions.empty();
        }
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6JulieISBaka Misc Features Help\n"
                    + "§7- §b/mf version§7 or §b/miscfeatures version§7: show the mod version\n"
                        + "§7- §b/mf help enchant§7: enchant usage, examples, and warnings\n"
                        + "§7- §b/mf help anvil§7: anvil usage and permission notes\n"
                        + "§7- §b/mf help config§7: config examples and key autocomplete usage\n"
                        + "§7- §b/mf locale coverage§7: show missing keys and format mismatches per language\n"
                    + "§7- §b/mf lc§7: alias for locale coverage\n"
                        + "§7- §b/mf locale coverage <locale> verbose§7: show full key-level locale mismatch details\n"
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
                    + "§7Syntax: §b/anvil rename <name>\n"
                    + "§7Syntax: §b/anvil clearname\n"
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
        LocaleCoverageData coverageData = loadLocaleCoverageData(context);
        if (coverageData == null) {
            return 0;
        }

        Set<String> base = coverageData.localeKeys().get("en_us");
        Map<String, String> baseValues = coverageData.localeValues().get("en_us");

        context.getSource().sendSuccess(() -> Component.literal("§6Locale coverage report (baseline: en_us)"), false);
        int localesWithIssues = 0;
        for (Map.Entry<String, Set<String>> entry : coverageData.localeKeys().entrySet()) {
            String locale = entry.getKey();
            if ("en_us".equals(locale)) {
                continue;
            }

            LocaleIssue issue = computeLocaleIssue(
                    base,
                    baseValues,
                    entry.getValue(),
                    coverageData.localeValues().getOrDefault(locale, Map.of())
            );
            List<String> missing = issue.missingKeys();
            List<String> formatMismatches = issue.formatMismatchKeys();

            if (missing.isEmpty() && formatMismatches.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§a- " + locale + ": complete"), false);
            } else {
                localesWithIssues++;
                int missingPreviewCount = Math.min(3, missing.size());
                String missingPreview = missingPreviewCount > 0
                        ? String.join(", ", missing.subList(0, missingPreviewCount))
                        : "";
                int mismatchPreviewCount = Math.min(3, formatMismatches.size());
                String mismatchPreview = mismatchPreviewCount > 0
                        ? String.join(", ", formatMismatches.subList(0, mismatchPreviewCount))
                        : "";

                String missingText = "missing " + missing.size() + " key(s)"
                        + (missingPreviewCount > 0 ? " (" + missingPreview + (missing.size() > missingPreviewCount ? ", ..." : "") + ")" : "");
                String mismatchText = "format mismatches " + formatMismatches.size()
                        + (mismatchPreviewCount > 0 ? " (" + mismatchPreview + (formatMismatches.size() > mismatchPreviewCount ? ", ..." : "") + ")" : "");

                context.getSource().sendSuccess(() -> Component.literal(
                        "§e- " + locale + ": " + missingText + "; " + mismatchText
                ), false);
            }
        }

        int finalLocalesWithIssues = localesWithIssues;
        context.getSource().sendSuccess(() -> Component.literal(
                finalLocalesWithIssues == 0
                        ? "§aAll locales are fully covered."
                        : "§6Locales with missing keys or format mismatches: " + finalLocalesWithIssues
        ), false);
        return 1;
    }

    private static int showLocaleCoverageForLocale(CommandContext<CommandSourceStack> context) {
        return showLocaleCoverageForLocale(context, false);
    }

    private static int showLocaleCoverageForLocaleVerbose(CommandContext<CommandSourceStack> context) {
        return showLocaleCoverageForLocale(context, true);
    }

    private static int showLocaleCoverageForLocale(CommandContext<CommandSourceStack> context, boolean verbose) {
        LocaleCoverageData coverageData = loadLocaleCoverageData(context);
        if (coverageData == null) {
            return 0;
        }

        String locale = StringArgumentType.getString(context, "locale").toLowerCase(Locale.ROOT);
        if (!coverageData.localeKeys().containsKey(locale)) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown locale: " + locale + ". Use /mf locale coverage to list known locales."
            ));
            return 0;
        }

        if ("en_us".equals(locale)) {
            context.getSource().sendSuccess(() -> Component.literal("§aen_us is the baseline locale."), false);
            return 1;
        }

        Set<String> baseKeys = coverageData.localeKeys().get("en_us");
        Map<String, String> baseValues = coverageData.localeValues().get("en_us");
        Set<String> localeKeys = coverageData.localeKeys().get(locale);
        Map<String, String> localeValues = coverageData.localeValues().getOrDefault(locale, Map.of());

        LocaleIssue issue = computeLocaleIssue(baseKeys, baseValues, localeKeys, localeValues);
        List<String> missing = issue.missingKeys();
        List<String> formatMismatches = issue.formatMismatchKeys();

        context.getSource().sendSuccess(() -> Component.literal(
                "§6Locale coverage detail for §e" + locale + "§6 (baseline: en_us)"
        ), false);

        if (missing.isEmpty() && formatMismatches.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§aNo missing keys or format mismatches."), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "§eMissing keys: " + missing.size() + " | Format mismatches: " + formatMismatches.size()
        ), false);

        if (!verbose) {
            int missingPreviewCount = Math.min(3, missing.size());
            int mismatchPreviewCount = Math.min(3, formatMismatches.size());
            if (missingPreviewCount > 0) {
                String preview = String.join(", ", missing.subList(0, missingPreviewCount));
                context.getSource().sendSuccess(() -> Component.literal(
                        "§7Missing preview: " + preview + (missing.size() > missingPreviewCount ? ", ..." : "")
                ), false);
            }
            if (mismatchPreviewCount > 0) {
                String preview = String.join(", ", formatMismatches.subList(0, mismatchPreviewCount));
                context.getSource().sendSuccess(() -> Component.literal(
                        "§7Mismatch preview: " + preview + (formatMismatches.size() > mismatchPreviewCount ? ", ..." : "")
                ), false);
            }
            context.getSource().sendSuccess(() -> Component.literal(
                    "§7Use /mf locale coverage " + locale + " verbose for full details."
            ), false);
            return 1;
        }

        for (String key : missing) {
            context.getSource().sendSuccess(() -> Component.literal("§cMissing: §7" + key), false);
        }

        for (String key : formatMismatches) {
            String baseText = baseValues.getOrDefault(key, "");
            String localeText = localeValues.getOrDefault(key, "");
            FormatSignature baseSignature = buildFormatSignature(baseText);
            FormatSignature localeSignature = buildFormatSignature(localeText);
            context.getSource().sendSuccess(() -> Component.literal(
                    "§eFormat mismatch: §7" + key
                            + " §8[base placeholders=" + baseSignature.placeholders()
                            + ", locale placeholders=" + localeSignature.placeholders()
                            + ", base colors=" + baseSignature.colorCodes()
                            + ", locale colors=" + localeSignature.colorCodes() + "]"
            ), false);
        }

        return 1;
    }

    private static LocaleCoverageData loadLocaleCoverageData(CommandContext<CommandSourceStack> context) {
        Path langDir = FabricLoader.getInstance()
                .getModContainer(MiscFeatures.MOD_ID)
                .flatMap(container -> container.findPath("assets/misc-features/lang"))
                .orElse(null);

        if (langDir == null || !Files.exists(langDir)) {
            context.getSource().sendFailure(Component.literal("Could not locate language files for locale coverage report."));
            return null;
        }

        Map<String, Set<String>> localeKeys = new TreeMap<>();
        Map<String, Map<String, String>> localeValues = new TreeMap<>();
        try (Stream<Path> files = Files.list(langDir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        String locale = normalizeLocaleIdentifier(path.getFileName().toString());
                        try {
                            JsonObject object = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                            Set<String> keys = new HashSet<>();
                            Map<String, String> values = new LinkedHashMap<>();
                            object.keySet().forEach(keys::add);
                            object.entrySet().forEach(entry -> {
                                if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                                    values.put(entry.getKey(), entry.getValue().getAsString());
                                }
                            });
                            localeKeys.put(locale, keys);
                            localeValues.put(locale, values);
                        } catch (Exception exception) {
                            context.getSource().sendFailure(Component.literal(
                                    "Failed reading locale " + locale + ": " + exception.getMessage()
                            ));
                        }
                    });
        } catch (IOException exception) {
            context.getSource().sendFailure(Component.literal("Failed to scan lang directory: " + exception.getMessage()));
            return null;
        }

        if (!localeKeys.containsKey("en_us")) {
            context.getSource().sendFailure(Component.literal("en_us.json is required for locale coverage baseline."));
            return null;
        }

        if (!localeValues.containsKey("en_us")) {
            context.getSource().sendFailure(Component.literal("en_us.json values are required for format checks."));
            return null;
        }

        return new LocaleCoverageData(localeKeys, localeValues);
    }

    private static String normalizeLocaleIdentifier(String fileName) {
        String locale = fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
        return locale.toLowerCase(Locale.ROOT);
    }

    private static LocaleIssue computeLocaleIssue(
            Set<String> baseKeys,
            Map<String, String> baseValues,
            Set<String> localeKeys,
            Map<String, String> localeValues
    ) {
        List<String> missing = new ArrayList<>();
        for (String key : baseKeys) {
            if (!localeKeys.contains(key)) {
                missing.add(key);
            }
        }

        List<String> formatMismatches = new ArrayList<>();
        for (Map.Entry<String, String> baseEntry : baseValues.entrySet()) {
            String key = baseEntry.getKey();
            if (!localeKeys.contains(key)) {
                continue;
            }

            String localized = localeValues.get(key);
            if (localized == null) {
                continue;
            }

            FormatSignature baseSignature = buildFormatSignature(baseEntry.getValue());
            FormatSignature localeSignature = buildFormatSignature(localized);
            if (!baseSignature.equals(localeSignature)) {
                formatMismatches.add(key);
            }
        }

        return new LocaleIssue(missing, formatMismatches);
    }

    private static FormatSignature buildFormatSignature(String value) {
        return new FormatSignature(extractMatches(value, PLACEHOLDER_PATTERN), extractMatches(value, COLOR_CODE_PATTERN));
    }

    private static List<String> extractMatches(String value, Pattern pattern) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            matches.add(matcher.group().toLowerCase());
        }
        return matches;
    }

    private record FormatSignature(List<String> placeholders, List<String> colorCodes) {
    }

    private record LocaleCoverageData(Map<String, Set<String>> localeKeys, Map<String, Map<String, String>> localeValues) {
    }

    private record LocaleIssue(List<String> missingKeys, List<String> formatMismatchKeys) {
    }
}