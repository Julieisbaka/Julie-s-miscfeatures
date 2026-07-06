package com.example.miscfeatures.command;

import com.example.miscfeatures.config.Config;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;

public final class EnchantLevelArgument implements ArgumentType<Integer> {

    private static final Collection<String> EXAMPLES = List.of("1", "10", "255", "256", "-1");
    private static final DynamicCommandExceptionType ERROR_INVALID_LEVEL = new DynamicCommandExceptionType(
            value -> Component.literal(String.valueOf(value))
    );
    private final Boolean allowNegativeSnapshot;
    private final Boolean allowHighLevelSnapshot;

    private EnchantLevelArgument(Boolean allowNegativeSnapshot, Boolean allowHighLevelSnapshot) {
        this.allowNegativeSnapshot = allowNegativeSnapshot;
        this.allowHighLevelSnapshot = allowHighLevelSnapshot;
    }

    public static EnchantLevelArgument enchantLevel() {
        return new EnchantLevelArgument(null, null);
    }

    public static EnchantLevelArgument enchantLevel(boolean allowNegative, boolean allowHighLevel) {
        return new EnchantLevelArgument(allowNegative, allowHighLevel);
    }

    public static int getLevel(CommandContext<?> context, String name) {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        int value = reader.readInt();
        boolean allowNegative = allowNegativeSnapshot != null ? allowNegativeSnapshot : canUseNegativeLevelsFromConfig();
        boolean allowHighLevel = allowHighLevelSnapshot != null ? allowHighLevelSnapshot : canUseHighLevelsFromConfig();

        if (value == 0) {
            reader.setCursor(start);
            throw ERROR_INVALID_LEVEL.createWithContext(reader, "Level 0 is not allowed.");
        }

        if (value < 0 && !allowNegative) {
            reader.setCursor(start);
            throw ERROR_INVALID_LEVEL.createWithContext(
                    reader,
                    "Negative levels are disabled. Enable developerMode + allowNegativeEnchants."
            );
        }

        if (value > 255 && !allowHighLevel) {
            reader.setCursor(start);
            throw ERROR_INVALID_LEVEL.createWithContext(
                reader,
                "Enchant levels above 255 are disabled. Enable developerMode + allowHighLevelEnchants."
            );
        }

        return value;
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static boolean canUseNegativeLevelsFromConfig() {
        Config config = Config.getInstance();
        return config.isDeveloperMode() && config.shouldAllowNegativeEnchants();
    }

    private static boolean canUseHighLevelsFromConfig() {
        Config config = Config.getInstance();
        return config.isDeveloperMode() && config.shouldAllowHighLevelEnchants();
    }

    public static final class Info implements ArgumentTypeInfo<EnchantLevelArgument, Info.Template> {

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buffer) {
            buffer.writeBoolean(template.allowNegative);
            buffer.writeBoolean(template.allowHighLevel);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            return new Template(buffer.readBoolean(), buffer.readBoolean());
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("allowNegative", template.allowNegative);
            json.addProperty("allowHighLevel", template.allowHighLevel);
        }

        @Override
        public Template unpack(EnchantLevelArgument argument) {
            return new Template(canUseNegativeLevelsFromConfig(), canUseHighLevelsFromConfig());
        }

        public final class Template implements ArgumentTypeInfo.Template<EnchantLevelArgument> {
            private final boolean allowNegative;
            private final boolean allowHighLevel;

            private Template(boolean allowNegative, boolean allowHighLevel) {
                this.allowNegative = allowNegative;
                this.allowHighLevel = allowHighLevel;
            }

            @Override
            public EnchantLevelArgument instantiate(CommandBuildContext context) {
                return EnchantLevelArgument.enchantLevel(allowNegative, allowHighLevel);
            }

            @Override
            public ArgumentTypeInfo<EnchantLevelArgument, ?> type() {
                return Info.this;
            }
        }
    }
}
