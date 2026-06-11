package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.inventory.menu.FilterMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FilterMenu.class)
public abstract class FilterMenuMixin {

    @Inject(method = "addStandardInventorySlots", at = @At("HEAD"))
    private void classicpipesnt$addExtraFilterSlots(Container playerInventory, int x, int y, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        FilterMenu menu = (FilterMenu) (Object) this;
        jagm.classicpipes.inventory.container.Filter filter = menu.getFilter();
        for (int i = 9; i < filter.getContainerSize(); i++) {
            ((net.blupillcosby.classicpipesnt.mixin.AbstractContainerMenuAccessor) menu).classicpipesnt$addSlot(
                    new jagm.classicpipes.inventory.menu.FilterSlot(filter, i, -10000, -10000)
            );
        }
    }

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