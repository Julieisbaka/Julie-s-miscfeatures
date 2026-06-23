package com.example.miscfeatures;

import com.example.miscfeatures.command.ConfigCommand;
import com.example.miscfeatures.command.EnchantCommand;
import com.example.miscfeatures.command.EnchantLevelArgument;
import com.example.miscfeatures.command.AnvilCommand;
import com.example.miscfeatures.command.FindEntityCommand;
import com.example.miscfeatures.command.FindItemCommand;
import com.example.miscfeatures.command.MiscFeaturesCommand;
import com.example.miscfeatures.command.SearchCommand;
import com.example.miscfeatures.command.WearCommand;
import com.example.miscfeatures.config.MiscFeaturesConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiscFeaturesMod implements ModInitializer {

    public static final String MOD_ID = "misc-features";
    public static final String LOG_PREFIX = MOD_ID + ": ";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void verbose(String message, Object... args) {
        if (MiscFeaturesConfig.getInstance().isVerboseLogging()) {
            LOGGER.info(LOG_PREFIX + message, args);
        }
    }

    @Override
    public void onInitialize() {
        MiscFeaturesConfig.getInstance().load();

        ArgumentTypeRegistry.registerArgumentType(
            Identifier.fromNamespaceAndPath(MOD_ID, "enchant_level"),
            EnchantLevelArgument.class,
            new EnchantLevelArgument.Info()
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                {
                    MiscFeaturesCommand.register(dispatcher);
                    SearchCommand.register(dispatcher);
                    FindItemCommand.register(dispatcher);
                    FindEntityCommand.register(dispatcher);
                    ConfigCommand.register(dispatcher);
                    AnvilCommand.register(dispatcher);
                    EnchantCommand.register(dispatcher, registryAccess);
                    WearCommand.register(dispatcher);
                });

        LOGGER.info(LOG_PREFIX + "JulieISBaka Misc Features initialized.");
    }
}