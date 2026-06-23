package com.example.miscfeatures.mixin;

import com.example.miscfeatures.config.MiscFeaturesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public final class EnchantmentMixin {

    @Inject(method = "getFullname", at = @At("HEAD"), cancellable = true)
    private static void miscFeatures$fixHighLevelEnchantText(
            Holder<Enchantment> enchantment,
            int level,
            CallbackInfoReturnable<Component> cir
    ) {
        MiscFeaturesConfig config = MiscFeaturesConfig.getInstance();
        if (!config.shouldFixHighLevelEnchantText() || level <= 10) {
            return;
        }

        MutableComponent display = enchantment.value().description().copy();
        if (enchantment.is(net.minecraft.tags.EnchantmentTags.CURSE)) {
            display = display.withStyle(ChatFormatting.RED);
        } else {
            display = display.withStyle(ChatFormatting.GRAY);
        }

        String levelText = config.shouldUseRomanForHighLevelEnchantText()
                ? toRoman(level)
                : Integer.toString(level);

        display.append(Component.literal(" " + levelText));
        cir.setReturnValue(display);
    }

    private static String toRoman(int number) {
        if (number <= 0 || number > 3999) {
            return Integer.toString(number);
        }

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder result = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                remaining -= values[i];
                result.append(numerals[i]);
            }
        }
        return result.toString();
    }
}
