package com.example.miscfeatures;

import com.example.miscfeatures.command.ConfigCommand;
import com.example.miscfeatures.command.MFEnchant;
import com.example.miscfeatures.command.EnchantLevelArgument;
import com.example.miscfeatures.command.Anvil;
import com.example.miscfeatures.command.FindEntity;
import com.example.miscfeatures.command.FindItem;
import com.example.miscfeatures.command.Command;
import com.example.miscfeatures.command.FindBlock;
import com.example.miscfeatures.command.version;
import com.example.miscfeatures.command.Wear;
import com.example.miscfeatures.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiscFeatures implements ModInitializer {

    public static final String MOD_ID = "misc-features";
    public static final String LOG_PREFIX = MOD_ID + ": ";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void verbose(String message, Object... args) {
        if (Config.getInstance().isVerboseLogging()) {
            LOGGER.info(LOG_PREFIX + message, args);
        }
    }

    @Override
    public void onInitialize() {
        Config.getInstance().load();

        ArgumentTypeRegistry.registerArgumentType(
            Identifier.fromNamespaceAndPath(MOD_ID, "enchant_level"),
            EnchantLevelArgument.class,
            new EnchantLevelArgument.Info()
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                {
                    Command.register(dispatcher);
                    version.register(dispatcher);
                    FindBlock.register(dispatcher);
                    FindItem.register(dispatcher);
                    FindEntity.register(dispatcher);
                    ConfigCommand.register(dispatcher);
                    Anvil.register(dispatcher);
                    MFEnchant.register(dispatcher, registryAccess);
                    Wear.register(dispatcher);
                });

        LOGGER.info(LOG_PREFIX + "JulieISBaka Misc Features initialized.");
    }
}