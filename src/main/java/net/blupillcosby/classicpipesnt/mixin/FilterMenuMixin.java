package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.inventory.menu.FilterMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FilterMenu.class)
public abstract class FilterMenuMixin {

    @ModifyArgs(
        method = "addStandardInventorySlots",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;addInventoryExtendedSlots(Lnet/minecraft/world/Container;II)V"
        )
    )
    private void classicpipesnt$shiftInventory(Args args) {
        int y = args.get(2);
        args.set(2, y + 18);
    }
}