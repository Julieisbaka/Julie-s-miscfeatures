package com.example.miscfeatures.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEnchantments.Mutable.class)
public interface ItemEnchantmentsMutableAccessor {

    @Accessor("enchantments")
    Object2IntOpenHashMap<Holder<Enchantment>> miscFeatures$getEnchantments();
}
