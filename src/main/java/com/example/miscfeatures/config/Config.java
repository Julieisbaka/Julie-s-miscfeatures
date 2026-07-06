package com.example.miscfeatures.config;

import com.example.miscfeatures.MiscFeatures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {

    public static final int DEFAULT_MAX_SEARCH_RADIUS = 64;
    public static final int DEFAULT_MAX_ITEM_SEARCH_RADIUS = 64;
    public static final int DEFAULT_MAX_ENTITY_SEARCH_RADIUS = 64;
    public static final int DEFAULT_MAX_SEARCH_RESULTS = 50;
    public static final int DEFAULT_MAX_FIND_ITEM_RESULTS = 50;
    public static final int DEFAULT_MAX_FIND_ENTITY_RESULTS = 50;
    public static final int ABSOLUTE_MAX_SEARCH_RADIUS = 512;
    public static final boolean DEFAULT_SEARCH_UNLOADED_CHUNKS = false;
    public static final boolean DEFAULT_SEARCH_ITEMS_IN_UNLOADED_CHUNKS = false;
    public static final boolean DEFAULT_SEARCH_ENTITIES_IN_UNLOADED_CHUNKS = false;
    public static final boolean DEFAULT_DEVELOPER_MODE = false;
    public static final boolean DEFAULT_VERBOSE_LOGGING = false;
    public static final boolean DEFAULT_ANVIL_AUTO_INSERT_ARMOR = true;
    public static final boolean DEFAULT_ANVIL_AUTO_INSERT_WEAPONS = true;
    public static final boolean DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS = true;
    public static final boolean DEFAULT_ALLOW_NEGATIVE_ENCHANTS = false;
    public static final boolean DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS = false;
    public static final boolean DEFAULT_REQUIRE_CONFIRM_FOR_NEGATIVE_ENCHANTS = true;
    public static final boolean DEFAULT_REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS = DEFAULT_REQUIRE_CONFIRM_FOR_NEGATIVE_ENCHANTS;
    public static final int DEFAULT_NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS = 5;
    public static final boolean DEFAULT_REQUIRE_PERMISSION_FOR_ENCHANT = false;
    public static final int DEFAULT_ENCHANT_PERMISSION_LEVEL = 2;
    public static final boolean DEFAULT_REQUIRE_PERMISSION_FOR_ANVIL = false;
    public static final int DEFAULT_ANVIL_PERMISSION_LEVEL = 2;
    public static final boolean DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS = false;
    public static final boolean DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS = DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_NEGATIVE_ENCHANTS;
    public static final boolean DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT = false;
    public static final boolean DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Config INSTANCE = new Config();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("misc-features.json");
    private int maxSearchRadius = DEFAULT_MAX_SEARCH_RADIUS;
    private int maxItemSearchRadius = DEFAULT_MAX_ITEM_SEARCH_RADIUS;
    private int maxEntitySearchRadius = DEFAULT_MAX_ENTITY_SEARCH_RADIUS;
    private int maxSearchResults = DEFAULT_MAX_SEARCH_RESULTS;
    private int maxFindItemResults = DEFAULT_MAX_FIND_ITEM_RESULTS;
    private int maxFindEntityResults = DEFAULT_MAX_FIND_ENTITY_RESULTS;
    private boolean searchUnloadedChunks = DEFAULT_SEARCH_UNLOADED_CHUNKS;
    private boolean searchItemsInUnloadedChunks = DEFAULT_SEARCH_ITEMS_IN_UNLOADED_CHUNKS;
    private boolean searchEntitiesInUnloadedChunks = DEFAULT_SEARCH_ENTITIES_IN_UNLOADED_CHUNKS;
    private boolean developerMode = DEFAULT_DEVELOPER_MODE;
    private boolean verboseLogging = DEFAULT_VERBOSE_LOGGING;
    private boolean anvilAutoInsertArmor = DEFAULT_ANVIL_AUTO_INSERT_ARMOR;
    private boolean anvilAutoInsertWeapons = DEFAULT_ANVIL_AUTO_INSERT_WEAPONS;
    private boolean anvilAutoInsertNameTags = DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS;
    private boolean allowNegativeEnchants = DEFAULT_ALLOW_NEGATIVE_ENCHANTS;
    private boolean allowHighLevelEnchants = DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS;
    private boolean requireConfirmForUnsafeEnchants = DEFAULT_REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS;
    private int negativeEnchantConfirmWindowSeconds = DEFAULT_NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS;
    private boolean requirePermissionForEnchant = DEFAULT_REQUIRE_PERMISSION_FOR_ENCHANT;
    private int enchantPermissionLevel = DEFAULT_ENCHANT_PERMISSION_LEVEL;
    private boolean requirePermissionForAnvil = DEFAULT_REQUIRE_PERMISSION_FOR_ANVIL;
    private int anvilPermissionLevel = DEFAULT_ANVIL_PERMISSION_LEVEL;
    private boolean preventCreativePacketCrashOnUnsafeEnchants = DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS;
    private boolean fixHighLevelEnchantText = DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT;
    private boolean highLevelEnchantStyleRoman = DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN;

    private Config() {
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            StoredConfig stored = GSON.fromJson(reader, StoredConfig.class);
            if (stored != null) {
                setDeveloperMode(stored.developerMode);
                setMaxSearchRadius(stored.maxSearchRadius);
                setMaxItemSearchRadius(stored.maxItemSearchRadius);
                setMaxEntitySearchRadius(stored.maxEntitySearchRadius);
                setMaxSearchResults(stored.maxSearchResults);
                setMaxFindItemResults(stored.maxFindItemResults);
                setMaxFindEntityResults(stored.maxFindEntityResults);
                setSearchUnloadedChunks(stored.searchUnloadedChunks);
                setSearchItemsInUnloadedChunks(stored.searchItemsInUnloadedChunks);
                setSearchEntitiesInUnloadedChunks(stored.searchEntitiesInUnloadedChunks);
                setVerboseLogging(stored.verboseLogging);
                setAnvilAutoInsertArmor(stored.anvilAutoInsertArmor != null
                    ? stored.anvilAutoInsertArmor
                    : DEFAULT_ANVIL_AUTO_INSERT_ARMOR);
                setAnvilAutoInsertWeapons(stored.anvilAutoInsertWeapons != null
                    ? stored.anvilAutoInsertWeapons
                    : DEFAULT_ANVIL_AUTO_INSERT_WEAPONS);
                setAnvilAutoInsertNameTags(stored.anvilAutoInsertNameTags != null
                    ? stored.anvilAutoInsertNameTags
                    : DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS);
                setAllowNegativeEnchants(stored.allowNegativeEnchants != null
                    ? stored.allowNegativeEnchants
                    : DEFAULT_ALLOW_NEGATIVE_ENCHANTS);
                setAllowHighLevelEnchants(stored.allowHighLevelEnchants != null
                    ? stored.allowHighLevelEnchants
                    : DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS);
                setRequireConfirmForUnsafeEnchants(stored.requireConfirmForUnsafeEnchants != null
                    ? stored.requireConfirmForUnsafeEnchants
                    : stored.requireConfirmForNegativeEnchants != null
                        ? stored.requireConfirmForNegativeEnchants
                        : DEFAULT_REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS);
                setNegativeEnchantConfirmWindowSeconds(stored.negativeEnchantConfirmWindowSeconds != null
                    ? stored.negativeEnchantConfirmWindowSeconds
                    : DEFAULT_NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS);
                setRequirePermissionForEnchant(stored.requirePermissionForEnchant != null
                    ? stored.requirePermissionForEnchant
                    : DEFAULT_REQUIRE_PERMISSION_FOR_ENCHANT);
                setEnchantPermissionLevel(stored.enchantPermissionLevel != null
                    ? stored.enchantPermissionLevel
                    : DEFAULT_ENCHANT_PERMISSION_LEVEL);
                setRequirePermissionForAnvil(stored.requirePermissionForAnvil != null
                    ? stored.requirePermissionForAnvil
                    : DEFAULT_REQUIRE_PERMISSION_FOR_ANVIL);
                setAnvilPermissionLevel(stored.anvilPermissionLevel != null
                    ? stored.anvilPermissionLevel
                    : DEFAULT_ANVIL_PERMISSION_LEVEL);
                setPreventCreativePacketCrashOnUnsafeEnchants(stored.preventCreativePacketCrashOnUnsafeEnchants != null
                    ? stored.preventCreativePacketCrashOnUnsafeEnchants
                    : stored.preventCreativePacketCrashOnNegativeEnchants != null
                        ? stored.preventCreativePacketCrashOnNegativeEnchants
                        : DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS);
                setFixHighLevelEnchantText(stored.fixHighLevelEnchantText != null
                    ? stored.fixHighLevelEnchantText
                    : DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT);
                setHighLevelEnchantStyleRoman(stored.highLevelEnchantStyleRoman != null
                    ? stored.highLevelEnchantStyleRoman
                    : DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN);
            }
        } catch (IOException | JsonSyntaxException exception) {
            MiscFeatures.LOGGER.warn(MiscFeatures.LOG_PREFIX + "Failed to load config from {}. Using defaults.", configPath, exception);
            maxSearchRadius = DEFAULT_MAX_SEARCH_RADIUS;
            maxItemSearchRadius = DEFAULT_MAX_ITEM_SEARCH_RADIUS;
            maxEntitySearchRadius = DEFAULT_MAX_ENTITY_SEARCH_RADIUS;
            maxSearchResults = DEFAULT_MAX_SEARCH_RESULTS;
            maxFindItemResults = DEFAULT_MAX_FIND_ITEM_RESULTS;
            maxFindEntityResults = DEFAULT_MAX_FIND_ENTITY_RESULTS;
            searchUnloadedChunks = DEFAULT_SEARCH_UNLOADED_CHUNKS;
            searchItemsInUnloadedChunks = DEFAULT_SEARCH_ITEMS_IN_UNLOADED_CHUNKS;
            searchEntitiesInUnloadedChunks = DEFAULT_SEARCH_ENTITIES_IN_UNLOADED_CHUNKS;
            developerMode = DEFAULT_DEVELOPER_MODE;
            verboseLogging = DEFAULT_VERBOSE_LOGGING;
            anvilAutoInsertArmor = DEFAULT_ANVIL_AUTO_INSERT_ARMOR;
            anvilAutoInsertWeapons = DEFAULT_ANVIL_AUTO_INSERT_WEAPONS;
            anvilAutoInsertNameTags = DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS;
            allowNegativeEnchants = DEFAULT_ALLOW_NEGATIVE_ENCHANTS;
            allowHighLevelEnchants = DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS;
            requireConfirmForUnsafeEnchants = DEFAULT_REQUIRE_CONFIRM_FOR_UNSAFE_ENCHANTS;
            negativeEnchantConfirmWindowSeconds = DEFAULT_NEGATIVE_ENCHANT_CONFIRM_WINDOW_SECONDS;
            requirePermissionForEnchant = DEFAULT_REQUIRE_PERMISSION_FOR_ENCHANT;
            enchantPermissionLevel = DEFAULT_ENCHANT_PERMISSION_LEVEL;
            requirePermissionForAnvil = DEFAULT_REQUIRE_PERMISSION_FOR_ANVIL;
            anvilPermissionLevel = DEFAULT_ANVIL_PERMISSION_LEVEL;
            preventCreativePacketCrashOnUnsafeEnchants = DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS;
            fixHighLevelEnchantText = DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT;
            highLevelEnchantStyleRoman = DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN;
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(new StoredConfig(
                        maxSearchRadius,
                        maxItemSearchRadius,
                        maxEntitySearchRadius,
                        maxSearchResults,
                        maxFindItemResults,
                        maxFindEntityResults,
                        searchUnloadedChunks,
                        searchItemsInUnloadedChunks,
                        searchEntitiesInUnloadedChunks,
                        developerMode,
                        verboseLogging,
                        anvilAutoInsertArmor,
                        anvilAutoInsertWeapons,
                        anvilAutoInsertNameTags,
                        allowNegativeEnchants,
                        allowHighLevelEnchants,
                        requireConfirmForUnsafeEnchants,
                        requireConfirmForUnsafeEnchants,
                        negativeEnchantConfirmWindowSeconds,
                        requirePermissionForEnchant,
                        enchantPermissionLevel,
                        requirePermissionForAnvil,
                        anvilPermissionLevel,
                        preventCreativePacketCrashOnUnsafeEnchants,
                        preventCreativePacketCrashOnUnsafeEnchants,
                        fixHighLevelEnchantText,
                        highLevelEnchantStyleRoman
                ), writer);
            }
        } catch (IOException exception) {
            MiscFeatures.LOGGER.error(MiscFeatures.LOG_PREFIX + "Failed to save config to {}.", configPath, exception);
        }
    }

    public int getMaxSearchRadius() {
        return maxSearchRadius;
    }

    public void setMaxSearchRadius(int maxSearchRadius) {
        this.maxSearchRadius = clampRadius(maxSearchRadius);
    }

    public int getMaxItemSearchRadius() {
        return maxItemSearchRadius;
    }

    public void setMaxItemSearchRadius(int maxItemSearchRadius) {
        this.maxItemSearchRadius = clampRadius(maxItemSearchRadius);
    }

    public int getMaxEntitySearchRadius() {
        return maxEntitySearchRadius;
    }

    public void setMaxEntitySearchRadius(int maxEntitySearchRadius) {
        this.maxEntitySearchRadius = clampRadius(maxEntitySearchRadius);
    }

    public int getMaxSearchResults() {
        return maxSearchResults;
    }

    public void setMaxSearchResults(int maxSearchResults) {
        this.maxSearchResults = Math.max(1, maxSearchResults);
    }

    public int getMaxFindItemResults() {
        return maxFindItemResults;
    }

    public void setMaxFindItemResults(int maxFindItemResults) {
        this.maxFindItemResults = Math.max(1, maxFindItemResults);
    }

    public int getMaxFindEntityResults() {
        return maxFindEntityResults;
    }

    public void setMaxFindEntityResults(int maxFindEntityResults) {
        this.maxFindEntityResults = Math.max(1, maxFindEntityResults);
    }

    public boolean shouldSearchUnloadedChunks() {
        return searchUnloadedChunks;
    }

    public void setSearchUnloadedChunks(boolean searchUnloadedChunks) {
        this.searchUnloadedChunks = searchUnloadedChunks;
    }

    public boolean shouldSearchItemsInUnloadedChunks() {
        return searchItemsInUnloadedChunks;
    }

    public void setSearchItemsInUnloadedChunks(boolean searchItemsInUnloadedChunks) {
        this.searchItemsInUnloadedChunks = searchItemsInUnloadedChunks;
    }

    public boolean shouldSearchEntitiesInUnloadedChunks() {
        return searchEntitiesInUnloadedChunks;
    }

    public void setSearchEntitiesInUnloadedChunks(boolean searchEntitiesInUnloadedChunks) {
        this.searchEntitiesInUnloadedChunks = searchEntitiesInUnloadedChunks;
    }

    public boolean isDeveloperMode() {
        return developerMode;
    }

    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode;
        if (!developerMode) {
            verboseLogging = false;
            setMaxSearchRadius(maxSearchRadius);
            setMaxItemSearchRadius(maxItemSearchRadius);
            setMaxEntitySearchRadius(maxEntitySearchRadius);
        }
    }

    public boolean isVerboseLogging() {
        return developerMode && verboseLogging;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    public boolean shouldAnvilAutoInsertArmor() {
        return anvilAutoInsertArmor;
    }

    public void setAnvilAutoInsertArmor(boolean anvilAutoInsertArmor) {
        this.anvilAutoInsertArmor = anvilAutoInsertArmor;
    }

    public boolean shouldAnvilAutoInsertWeapons() {
        return anvilAutoInsertWeapons;
    }

    public void setAnvilAutoInsertWeapons(boolean anvilAutoInsertWeapons) {
        this.anvilAutoInsertWeapons = anvilAutoInsertWeapons;
    }

    public boolean shouldAnvilAutoInsertNameTags() {
        return anvilAutoInsertNameTags;
    }

    public void setAnvilAutoInsertNameTags(boolean anvilAutoInsertNameTags) {
        this.anvilAutoInsertNameTags = anvilAutoInsertNameTags;
    }

    public boolean shouldAllowNegativeEnchants() {
        return allowNegativeEnchants;
    }

    public void setAllowNegativeEnchants(boolean allowNegativeEnchants) {
        this.allowNegativeEnchants = allowNegativeEnchants;
    }

    public boolean shouldAllowHighLevelEnchants() {
        return allowHighLevelEnchants;
    }

    public void setAllowHighLevelEnchants(boolean allowHighLevelEnchants) {
        this.allowHighLevelEnchants = allowHighLevelEnchants;
    }

    public boolean shouldRequireConfirmForNegativeEnchants() {
        return shouldRequireConfirmForUnsafeEnchants();
    }

    public void setRequireConfirmForNegativeEnchants(boolean requireConfirmForNegativeEnchants) {
        setRequireConfirmForUnsafeEnchants(requireConfirmForNegativeEnchants);
    }

    public boolean shouldRequireConfirmForUnsafeEnchants() {
        return requireConfirmForUnsafeEnchants;
    }

    public void setRequireConfirmForUnsafeEnchants(boolean requireConfirmForUnsafeEnchants) {
        this.requireConfirmForUnsafeEnchants = requireConfirmForUnsafeEnchants;
    }

    public int getNegativeEnchantConfirmWindowSeconds() {
        return negativeEnchantConfirmWindowSeconds;
    }

    public void setNegativeEnchantConfirmWindowSeconds(int negativeEnchantConfirmWindowSeconds) {
        this.negativeEnchantConfirmWindowSeconds = Math.max(1, Math.min(30, negativeEnchantConfirmWindowSeconds));
    }

    public boolean shouldRequirePermissionForEnchant() {
        return requirePermissionForEnchant;
    }

    public void setRequirePermissionForEnchant(boolean requirePermissionForEnchant) {
        this.requirePermissionForEnchant = requirePermissionForEnchant;
    }

    public int getEnchantPermissionLevel() {
        return enchantPermissionLevel;
    }

    public void setEnchantPermissionLevel(int enchantPermissionLevel) {
        this.enchantPermissionLevel = clampPermissionLevel(enchantPermissionLevel);
    }

    public boolean shouldRequirePermissionForAnvil() {
        return requirePermissionForAnvil;
    }

    public void setRequirePermissionForAnvil(boolean requirePermissionForAnvil) {
        this.requirePermissionForAnvil = requirePermissionForAnvil;
    }

    public int getAnvilPermissionLevel() {
        return anvilPermissionLevel;
    }

    public void setAnvilPermissionLevel(int anvilPermissionLevel) {
        this.anvilPermissionLevel = clampPermissionLevel(anvilPermissionLevel);
    }

    public boolean shouldPreventCreativePacketCrashOnNegativeEnchants() {
        return shouldPreventCreativePacketCrashOnUnsafeEnchants();
    }

    public void setPreventCreativePacketCrashOnNegativeEnchants(boolean preventCreativePacketCrashOnNegativeEnchants) {
        setPreventCreativePacketCrashOnUnsafeEnchants(preventCreativePacketCrashOnNegativeEnchants);
    }

    public boolean shouldPreventCreativePacketCrashOnUnsafeEnchants() {
        return preventCreativePacketCrashOnUnsafeEnchants;
    }

    public void setPreventCreativePacketCrashOnUnsafeEnchants(boolean preventCreativePacketCrashOnUnsafeEnchants) {
        this.preventCreativePacketCrashOnUnsafeEnchants = preventCreativePacketCrashOnUnsafeEnchants;
    }

    public boolean shouldFixHighLevelEnchantText() {
        return fixHighLevelEnchantText;
    }

    public void setFixHighLevelEnchantText(boolean fixHighLevelEnchantText) {
        this.fixHighLevelEnchantText = fixHighLevelEnchantText;
    }

    public boolean shouldUseRomanForHighLevelEnchantText() {
        return highLevelEnchantStyleRoman;
    }

    public void setHighLevelEnchantStyleRoman(boolean highLevelEnchantStyleRoman) {
        this.highLevelEnchantStyleRoman = highLevelEnchantStyleRoman;
    }

    private int clampRadius(int radius) {
        if (developerMode) {
            return Math.max(1, radius);
        }

        return Math.max(1, Math.min(ABSOLUTE_MAX_SEARCH_RADIUS, radius));
    }

    private int clampPermissionLevel(int permissionLevel) {
        return Math.max(0, Math.min(4, permissionLevel));
    }

    private record StoredConfig(
            int maxSearchRadius,
            int maxItemSearchRadius,
            int maxEntitySearchRadius,
            int maxSearchResults,
            int maxFindItemResults,
            int maxFindEntityResults,
            boolean searchUnloadedChunks,
            boolean searchItemsInUnloadedChunks,
            boolean searchEntitiesInUnloadedChunks,
            boolean developerMode,
            boolean verboseLogging,
            Boolean anvilAutoInsertArmor,
            Boolean anvilAutoInsertWeapons,
            Boolean anvilAutoInsertNameTags,
            Boolean allowNegativeEnchants,
            Boolean allowHighLevelEnchants,
            Boolean requireConfirmForNegativeEnchants,
            Boolean requireConfirmForUnsafeEnchants,
            Integer negativeEnchantConfirmWindowSeconds,
            Boolean requirePermissionForEnchant,
            Integer enchantPermissionLevel,
            Boolean requirePermissionForAnvil,
            Integer anvilPermissionLevel,
            Boolean preventCreativePacketCrashOnNegativeEnchants,
            Boolean preventCreativePacketCrashOnUnsafeEnchants,
            Boolean fixHighLevelEnchantText,
            Boolean highLevelEnchantStyleRoman
    ) {
    }
}