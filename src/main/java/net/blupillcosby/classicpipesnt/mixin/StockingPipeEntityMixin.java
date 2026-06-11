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

    @Unique
    private boolean classicpipesnt$needsBackupRestore = false;

    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljagm/classicpipes/inventory/container/FilterContainer;<init>(Ljagm/classicpipes/blockentity/PipeEntity;IZ)V"),
            index = 1
    )
    private static int classicpipesnt$increaseServerFilterSize(int size) {
        return size * 99;
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void classicpipesnt$onTickServer(ServerLevel level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this.classicpipesnt$needsBackupRestore) {
            StockingPipeEntity entity = (StockingPipeEntity)(Object)this;
            net.blupillcosby.classicpipesnt.util.FilterBackupManager.restoreBackup(level, pos, entity.getFilter());
            this.classicpipesnt$needsBackupRestore = false;
            entity.setChanged();
        }

        if (this.classicpipesnt$allowOverflow && level.getGameTime() % 20 == 0) {
            StockingPipeEntity entity = (StockingPipeEntity)(Object)this;
            entity.updateCache(level);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void classicpipesnt$loadAdditional(ValueInput valueInput, CallbackInfo ci) {
        this.classicpipesnt$allowOverflow = valueInput.getBooleanOr("classicpipesnt_allow_overflow", false);
        
        StockingPipeEntity entity = (StockingPipeEntity)(Object)this;
        jagm.classicpipes.inventory.container.Filter filter = entity.getFilter();
        
        boolean hasExtra = false;
        var extraList = valueInput.listOrEmpty("classicpipesnt_filter_extra", net.blupillcosby.classicpipesnt.util.CustomCodecs.INT_SLOT_CODEC);
        for (net.minecraft.world.ItemStackWithSlot slotStack : extraList) {
            hasExtra = true;
            if (slotStack.slot() >= 9) {
                filter.setItem(slotStack.slot(), slotStack.stack());
            }
        }
        
        if (!hasExtra) {
            this.classicpipesnt$needsBackupRestore = true;
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "saveAdditional",
            at = @At(value = "INVOKE", target = "Ljagm/classicpipes/inventory/container/Filter;getContainerSize()I")
    )
    private int classicpipesnt$limitBaseFilterSaveSize(jagm.classicpipes.inventory.container.Filter filter) {
        return 9; // Base mod only expects 9. This ensures backwards compatibility!
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void classicpipesnt$saveAdditional(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.putBoolean("classicpipesnt_allow_overflow", this.classicpipesnt$allowOverflow);
        
        StockingPipeEntity entity = (StockingPipeEntity)(Object)this;
        jagm.classicpipes.inventory.container.Filter filter = entity.getFilter();
        
        var extraList = valueOutput.list("classicpipesnt_filter_extra", net.blupillcosby.classicpipesnt.util.CustomCodecs.INT_SLOT_CODEC);
        for (int slot = 9; slot < filter.getContainerSize(); slot++) {
            ItemStack stack = filter.getItem(slot);
            if (!stack.isEmpty()) {
                extraList.add(new net.minecraft.world.ItemStackWithSlot(slot, stack));
            }
        }
        
        if (!this.classicpipesnt$needsBackupRestore) {
            net.blupillcosby.classicpipesnt.util.FilterBackupManager.saveBackupAsync(entity.getLevel(), entity.getBlockPos(), filter);
        }
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
                        long inserted = 0;
                        if (filterStack.getItem() instanceof LabelItem labelItem) {
                            for (net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> view : storage) {
                                if (view.isResourceBlank()) {
                                    inserted += view.getCapacity();
                                } else if (labelItem.itemMatches(filterStack, view.getResource().toStack())) {
                                    inserted += (view.getCapacity() - view.getAmount());
                                }
                            }
                        } else {
                            inserted = storage.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(filterStack), Long.MAX_VALUE, transaction);
                        }
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
