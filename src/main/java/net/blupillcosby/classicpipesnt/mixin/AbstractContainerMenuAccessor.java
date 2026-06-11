package net.blupillcosby.classicpipesnt.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addDataSlot")
    DataSlot classicpipesnt$addDataSlot(DataSlot dataSlot);

    @Invoker("addSlot")
    net.minecraft.world.inventory.Slot classicpipesnt$addSlot(net.minecraft.world.inventory.Slot slot);
}
