package net.blupillcosby.classicpipesnt.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

public class CustomCodecs {
    public static final Codec<ItemStackWithSlot> INT_SLOT_CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.withAlternative(Codec.INT, Codec.BYTE.xmap(Byte::intValue, Integer::byteValue)).fieldOf("Slot").orElse(0).forGetter(ItemStackWithSlot::slot),
            ItemStack.MAP_CODEC.forGetter(ItemStackWithSlot::stack)
        ).apply(instance, ItemStackWithSlot::new)
    );
}
