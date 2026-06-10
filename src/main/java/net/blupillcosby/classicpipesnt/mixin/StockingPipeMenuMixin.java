package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.blockentity.StockingPipeEntity;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeEntity;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StockingPipeMenu.class)
public abstract class StockingPipeMenuMixin implements IStockingPipeMenu {

    @Unique
    private boolean classicpipesnt$allowOverstocking;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljagm/classicpipes/network/ClientBoundTwoBoolsPayload;)V", at = @At("TAIL"))
    private void classicpipesnt$onClientInit(int id, net.minecraft.world.entity.player.Inventory playerInventory, jagm.classicpipes.network.ClientBoundTwoBoolsPayload payload, CallbackInfo ci) {
        this.classicpipesnt$allowOverstocking = net.blupillcosby.classicpipesnt.ClientState.lastAllowOverstocking;
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Ljagm/classicpipes/inventory/container/Filter;Z)V", at = @At("TAIL"))
    private void classicpipesnt$onInit(int id, net.minecraft.world.entity.player.Inventory playerInventory, jagm.classicpipes.inventory.container.Filter filter, boolean activeStocking, CallbackInfo ci) {
        if (filter.getPipe() instanceof StockingPipeEntity stockingPipe) {
            this.classicpipesnt$allowOverstocking = ((IStockingPipeEntity)stockingPipe).classicpipesnt$getAllowOverflow();
        }
    }

    @Override
    public boolean allowOverstocking() {
        return this.classicpipesnt$allowOverstocking;
    }

    @Override
    public void setAllowOverstocking(boolean allowOverstocking) {
        this.classicpipesnt$allowOverstocking = allowOverstocking;
        StockingPipeMenu menu = (StockingPipeMenu)(Object)this;
        if (menu.getFilter().getPipe() instanceof StockingPipeEntity stockingPipe) {
            ((IStockingPipeEntity)stockingPipe).classicpipesnt$setAllowOverflow(allowOverstocking);
        }
    }
}