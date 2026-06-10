package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @ModifyVariable(
            method = "addInventoryExtendedSlots",
            at = @At("HEAD"),
            ordinal = 1
    )
    private int classicpipesnt$shiftExtendedTop(int top) {
        if ((Object)this instanceof StockingPipeMenu) {
            return top + 18;
        }
        return top;
    }

    @ModifyVariable(
            method = "addInventoryHotbarSlots",
            at = @At("HEAD"),
            ordinal = 1
    )
    private int classicpipesnt$shiftHotbarTop(int top) {
        if ((Object)this instanceof StockingPipeMenu) {
            return top + 18;
        }
        return top;
    }
}