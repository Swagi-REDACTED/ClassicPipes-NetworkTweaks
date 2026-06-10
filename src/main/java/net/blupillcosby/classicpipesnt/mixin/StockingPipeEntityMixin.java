package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.blockentity.StockingPipeEntity;
import jagm.classicpipes.item.LabelItem;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(StockingPipeEntity.class)
public abstract class StockingPipeEntityMixin implements IStockingPipeEntity {

    @Unique
    private boolean classicpipesnt$allowOverflow = false;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void classicpipesnt$onTickServer(ServerLevel level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this.classicpipesnt$allowOverflow && level.getGameTime() % 20 == 0) {
            StockingPipeEntity entity = (StockingPipeEntity)(Object)this;
            entity.updateCache(level);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void classicpipesnt$loadAdditional(ValueInput valueInput, CallbackInfo ci) {
        this.classicpipesnt$allowOverflow = valueInput.getBooleanOr("classicpipesnt_allow_overflow", false);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void classicpipesnt$saveAdditional(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.putBoolean("classicpipesnt_allow_overflow", this.classicpipesnt$allowOverflow);
    }

    @Inject(method = "updateCache", at = @At("RETURN"))
    private void classicpipesnt$onUpdateCacheReturn(ServerLevel level, CallbackInfo ci) {
        if (this.classicpipesnt$allowOverflow) {
            StockingPipeEntity entity = (StockingPipeEntity)(Object)this;

            Direction facing = entity.getBlockState().getValue(jagm.classicpipes.block.StockingPipeBlock.FACING).getDirection();
            if (facing == null) return;

            BlockPos containerPos = entity.getBlockPos().relative(facing);

            var filter = entity.getFilter();
            List<ItemStack> filterItems = new ArrayList<>();
            for (ItemStack stack : filter) {
                if (!stack.isEmpty()) {
                    filterItems.add(stack.copy());
                }
            }

            if (filterItems.isEmpty()) return;

            entity.getMissingItemsCache().clear();

            net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> storage = 
                net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, containerPos, facing.getOpposite());

            if (storage != null) {
                try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    for (ItemStack filterStack : filterItems) {
                        long inserted = storage.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(filterStack), Long.MAX_VALUE, transaction);
                        if (inserted > 0) {
                            entity.getMissingItemsCache().add(filterStack.copyWithCount((int) Math.min(Integer.MAX_VALUE, inserted)));
                        }
                    }
                    // Transaction is NOT committed! This guarantees it's a simulation.
                }
            }

            if (entity.isActiveStocking()) {
                entity.tryRequests(level);
            }
        }
    }

    @Unique
    public boolean classicpipesnt$getAllowOverflow() {
        return this.classicpipesnt$allowOverflow;
    }

    @Unique
    public void classicpipesnt$setAllowOverflow(boolean allowOverflow) {
        this.classicpipesnt$allowOverflow = allowOverflow;
        StockingPipeEntity entity = (StockingPipeEntity)(Object)this;
        entity.setChanged();
        if (entity.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), 2);
        }
    }
}
