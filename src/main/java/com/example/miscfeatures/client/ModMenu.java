package com.example.miscfeatures.client;

import com.example.miscfeatures.config.Config;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Config config = Config.getInstance();
            boolean developerMode = config.isDeveloperMode();
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("miscfeatures.config.title"))
                    .setSavingRunnable(config::save);

            ConfigEntryBuilder entries = builder.entryBuilder();
            ConfigCategory generalCategory = builder.getOrCreateCategory(Component.translatable("miscfeatures.config.category.general"));
            ConfigCategory searchCategory = builder.getOrCreateCategory(Component.translatable("miscfeatures.config.category.find_block"));
            ConfigCategory findItemCategory = builder.getOrCreateCategory(Component.translatable("miscfeatures.config.category.find_item"));
            ConfigCategory findEntityCategory = builder.getOrCreateCategory(Component.translatable("miscfeatures.config.category.find_entity"));
            ConfigCategory anvilCategory = builder.getOrCreateCategory(Component.translatable("miscfeatures.config.category.anvil"));
            ConfigCategory enchantCategory = builder.getOrCreateCategory(Component.translatable("miscfeatures.config.category.enchant"));

            var developerModeEntry = entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.developer_mode.label"),
                            config.isDeveloperMode()
                    )
                    .setDefaultValue(Config.DEFAULT_DEVELOPER_MODE)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.developer_mode.tooltip.1"),
                            Component.translatable("miscfeatures.config.developer_mode.tooltip.2"),
                            Component.translatable("miscfeatures.config.developer_mode.tooltip.3")
                    )
                    .setSaveConsumer(config::setDeveloperMode)
                    .build();

            generalCategory.addEntry(developerModeEntry);

            if (developerMode) {
                generalCategory.addEntry(entries.startBooleanToggle(
                                Component.translatable("miscfeatures.config.verbose_logging.label"),
                                config.isVerboseLogging()
                        )
                        .setDefaultValue(Config.DEFAULT_VERBOSE_LOGGING)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.verbose_logging.tooltip.1"),
                                Component.translatable("miscfeatures.config.verbose_logging.tooltip.2")
                        )
                        .setSaveConsumer(config::setVerboseLogging)
                        .build());
            }

            anvilCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.anvil.auto_insert_armor.label"),
                            config.shouldAnvilAutoInsertArmor()
                    )
                    .setDefaultValue(Config.DEFAULT_ANVIL_AUTO_INSERT_ARMOR)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.anvil.auto_insert_armor.tooltip.1"),
                            Component.translatable("miscfeatures.config.anvil.auto_insert_armor.tooltip.2")
                    )
                    .setSaveConsumer(config::setAnvilAutoInsertArmor)
                    .build());

            anvilCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.anvil.auto_insert_weapons.label"),
                            config.shouldAnvilAutoInsertWeapons()
                    )
                    .setDefaultValue(Config.DEFAULT_ANVIL_AUTO_INSERT_WEAPONS)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.anvil.auto_insert_weapons.tooltip.1"),
                            Component.translatable("miscfeatures.config.anvil.auto_insert_weapons.tooltip.2")
                    )
                    .setSaveConsumer(config::setAnvilAutoInsertWeapons)
                    .build());

            anvilCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.anvil.auto_insert_name_tags.label"),
                            config.shouldAnvilAutoInsertNameTags()
                    )
                    .setDefaultValue(Config.DEFAULT_ANVIL_AUTO_INSERT_NAME_TAGS)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.anvil.auto_insert_name_tags.tooltip.1"),
                            Component.translatable("miscfeatures.config.anvil.auto_insert_name_tags.tooltip.2")
                    )
                    .setSaveConsumer(config::setAnvilAutoInsertNameTags)
                    .build());

            if (developerMode) {
                enchantCategory.addEntry(entries.startBooleanToggle(
                                Component.translatable("miscfeatures.config.enchant.allow_negative.label"),
                                config.shouldAllowNegativeEnchants()
                        )
                        .setDefaultValue(Config.DEFAULT_ALLOW_NEGATIVE_ENCHANTS)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.enchant.allow_negative.tooltip.1"),
                                Component.translatable("miscfeatures.config.enchant.allow_negative.tooltip.2")
                        )
                        .setSaveConsumer(config::setAllowNegativeEnchants)
                        .build());

                enchantCategory.addEntry(entries.startBooleanToggle(
                                Component.translatable("miscfeatures.config.enchant.allow_high_level.label"),
                                config.shouldAllowHighLevelEnchants()
                        )
                        .setDefaultValue(Config.DEFAULT_ALLOW_HIGH_LEVEL_ENCHANTS)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.enchant.allow_high_level.tooltip.1"),
                                Component.translatable("miscfeatures.config.enchant.allow_high_level.tooltip.2")
                        )
                        .setSaveConsumer(config::setAllowHighLevelEnchants)
                        .build());

                enchantCategory.addEntry(entries.startBooleanToggle(
                                Component.translatable("miscfeatures.config.enchant.prevent_creative_packet_crash_unsafe.label"),
                                config.shouldPreventCreativePacketCrashOnUnsafeEnchants()
                        )
                        .setDefaultValue(Config.DEFAULT_PREVENT_CREATIVE_PACKET_CRASH_ON_UNSAFE_ENCHANTS)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.enchant.prevent_creative_packet_crash_unsafe.tooltip.1"),
                                Component.translatable("miscfeatures.config.enchant.prevent_creative_packet_crash_unsafe.tooltip.2")
                        )
                        .setSaveConsumer(config::setPreventCreativePacketCrashOnUnsafeEnchants)
                        .build());
            }

            enchantCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.enchant.fix_high_level_text.label"),
                            config.shouldFixHighLevelEnchantText()
                    )
                    .setDefaultValue(Config.DEFAULT_FIX_HIGH_LEVEL_ENCHANT_TEXT)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.enchant.fix_high_level_text.tooltip.1"),
                            Component.translatable("miscfeatures.config.enchant.fix_high_level_text.tooltip.2")
                    )
                    .setSaveConsumer(config::setFixHighLevelEnchantText)
                    .build());

            enchantCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.enchant.high_level_style_roman.label"),
                            config.shouldUseRomanForHighLevelEnchantText()
                    )
                    .setDefaultValue(Config.DEFAULT_HIGH_LEVEL_ENCHANT_STYLE_ROMAN)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.enchant.high_level_style_roman.tooltip.1"),
                            Component.translatable("miscfeatures.config.enchant.high_level_style_roman.tooltip.2")
                    )
                    .setSaveConsumer(config::setHighLevelEnchantStyleRoman)
                    .build());

            if (developerMode) {
                searchCategory.addEntry(entries.startIntField(
                                Component.translatable("miscfeatures.config.max_search_radius.label"),
                                config.getMaxSearchRadius()
                        )
                        .setDefaultValue(Config.DEFAULT_MAX_SEARCH_RADIUS)
                        .setMin(1)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.max_search_radius.tooltip.1"),
                                Component.translatable("miscfeatures.config.max_search_radius.tooltip.2")
                        )
                        .setSaveConsumer(config::setMaxSearchRadius)
                        .build());
            } else {
                searchCategory.addEntry(entries.startIntField(
                                Component.translatable("miscfeatures.config.max_search_radius.label"),
                                Math.min(config.getMaxSearchRadius(), Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        )
                        .setDefaultValue(Config.DEFAULT_MAX_SEARCH_RADIUS)
                        .setMin(1)
                        .setMax(Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.max_search_radius.tooltip.1"),
                                Component.translatable("miscfeatures.config.max_search_radius.normal_cap.tooltip", Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        )
                        .setSaveConsumer(config::setMaxSearchRadius)
                        .build());
            }

            searchCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.label"),
                            config.shouldSearchUnloadedChunks()
                    )
                    .setDefaultValue(Config.DEFAULT_SEARCH_UNLOADED_CHUNKS)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.tooltip.block.1"),
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.tooltip.block.2")
                    )
                    .setSaveConsumer(config::setSearchUnloadedChunks)
                    .build());

            searchCategory.addEntry(entries.startIntField(
                            Component.translatable("miscfeatures.config.max_search_results.label"),
                            config.getMaxSearchResults()
                    )
                    .setDefaultValue(Config.DEFAULT_MAX_SEARCH_RESULTS)
                    .setMin(1)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.max_search_results.tooltip.1"),
                            Component.translatable("miscfeatures.config.max_search_results.tooltip.2")
                    )
                    .setSaveConsumer(config::setMaxSearchResults)
                    .build());

            if (developerMode) {
                findItemCategory.addEntry(entries.startIntField(
                                Component.translatable("miscfeatures.config.max_item_search_radius.label"),
                                config.getMaxItemSearchRadius()
                        )
                        .setDefaultValue(Config.DEFAULT_MAX_ITEM_SEARCH_RADIUS)
                        .setMin(1)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.max_item_search_radius.tooltip.1"),
                                Component.translatable("miscfeatures.config.max_item_search_radius.tooltip.2")
                        )
                        .setSaveConsumer(config::setMaxItemSearchRadius)
                        .build());
            } else {
                findItemCategory.addEntry(entries.startIntField(
                                Component.translatable("miscfeatures.config.max_item_search_radius.label"),
                                Math.min(config.getMaxItemSearchRadius(), Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        )
                        .setDefaultValue(Config.DEFAULT_MAX_ITEM_SEARCH_RADIUS)
                        .setMin(1)
                        .setMax(Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.max_item_search_radius.tooltip.1"),
                                Component.translatable("miscfeatures.config.max_item_search_radius.normal_cap.tooltip", Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        )
                        .setSaveConsumer(config::setMaxItemSearchRadius)
                        .build());
            }

            findItemCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.label"),
                            config.shouldSearchItemsInUnloadedChunks()
                    )
                    .setDefaultValue(Config.DEFAULT_SEARCH_ITEMS_IN_UNLOADED_CHUNKS)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.tooltip.item.1"),
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.tooltip.item.2")
                    )
                    .setSaveConsumer(config::setSearchItemsInUnloadedChunks)
                    .build());

            findItemCategory.addEntry(entries.startIntField(
                            Component.translatable("miscfeatures.config.max_find_item_results.label"),
                            config.getMaxFindItemResults()
                    )
                    .setDefaultValue(Config.DEFAULT_MAX_FIND_ITEM_RESULTS)
                    .setMin(1)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.max_find_item_results.tooltip.1"),
                            Component.translatable("miscfeatures.config.max_find_item_results.tooltip.2")
                    )
                    .setSaveConsumer(config::setMaxFindItemResults)
                    .build());

            if (developerMode) {
                findEntityCategory.addEntry(entries.startIntField(
                                Component.translatable("miscfeatures.config.max_entity_search_radius.label"),
                                config.getMaxEntitySearchRadius()
                        )
                        .setDefaultValue(Config.DEFAULT_MAX_ENTITY_SEARCH_RADIUS)
                        .setMin(1)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.max_entity_search_radius.tooltip.1"),
                                Component.translatable("miscfeatures.config.max_entity_search_radius.tooltip.2")
                        )
                        .setSaveConsumer(config::setMaxEntitySearchRadius)
                        .build());
            } else {
                findEntityCategory.addEntry(entries.startIntField(
                                Component.translatable("miscfeatures.config.max_entity_search_radius.label"),
                                Math.min(config.getMaxEntitySearchRadius(), Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        )
                        .setDefaultValue(Config.DEFAULT_MAX_ENTITY_SEARCH_RADIUS)
                        .setMin(1)
                        .setMax(Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        .setTooltip(
                                Component.translatable("miscfeatures.config.max_entity_search_radius.tooltip.1"),
                                Component.translatable("miscfeatures.config.max_entity_search_radius.normal_cap.tooltip", Config.ABSOLUTE_MAX_SEARCH_RADIUS)
                        )
                        .setSaveConsumer(config::setMaxEntitySearchRadius)
                        .build());
            }

            findEntityCategory.addEntry(entries.startBooleanToggle(
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.label"),
                            config.shouldSearchEntitiesInUnloadedChunks()
                    )
                    .setDefaultValue(Config.DEFAULT_SEARCH_ENTITIES_IN_UNLOADED_CHUNKS)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.tooltip.entity.1"),
                            Component.translatable("miscfeatures.config.search_unloaded_chunks.tooltip.entity.2")
                    )
                    .setSaveConsumer(config::setSearchEntitiesInUnloadedChunks)
                    .build());

            findEntityCategory.addEntry(entries.startIntField(
                            Component.translatable("miscfeatures.config.max_find_entity_results.label"),
                            config.getMaxFindEntityResults()
                    )
                    .setDefaultValue(Config.DEFAULT_MAX_FIND_ENTITY_RESULTS)
                    .setMin(1)
                    .setTooltip(
                            Component.translatable("miscfeatures.config.max_find_entity_results.tooltip.1"),
                            Component.translatable("miscfeatures.config.max_find_entity_results.tooltip.2")
                    )
                    .setSaveConsumer(config::setMaxFindEntityResults)
                    .build());

            return builder.build();
        };
    }
}
