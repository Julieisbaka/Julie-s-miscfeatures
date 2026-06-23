package com.example.miscfeatures.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsValidationMixin {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap$Entry;getIntValue()I"
            )
    )
    private int miscFeatures$skipLevelRangeValidation(Object2IntMap.Entry<?> entry) {
        int actual = entry.getIntValue();
        if (actual < 0 || actual > 255) {
            return 1;
        }

        return actual;
    }
}
