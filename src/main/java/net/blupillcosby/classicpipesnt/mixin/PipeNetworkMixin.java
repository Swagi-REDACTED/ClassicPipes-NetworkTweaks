package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.block.PipeBlock;
import jagm.classicpipes.blockentity.ProviderPipe;
import jagm.classicpipes.util.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(PipeNetwork.class)
public class PipeNetworkMixin {

    @Unique
    private ServerLevel classicpipesnt$currentLevel;

    @Inject(method = "request", at = @At("HEAD"))
    private void classicpipesnt$captureLevel(ServerLevel level, ItemStack stack, BlockPos requestPos, Player player, boolean partialRequests, CallbackInfo ci) {
        this.classicpipesnt$currentLevel = level;
    }

    @Inject(method = "request", at = @At("RETURN"))
    private void classicpipesnt$clearLevel(ServerLevel level, ItemStack stack, BlockPos requestPos, Player player, boolean partialRequests, CallbackInfo ci) {
        this.classicpipesnt$currentLevel = null;
    }

    @Unique
    private boolean classicpipesnt$areSameContainer(ServerLevel level, BlockPos pos1, BlockPos pos2) {
        if (pos1.equals(pos2)) return true;
        BlockState state1 = level.getBlockState(pos1);
        if (state1.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.CHEST_TYPE) && state1.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state1.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            net.minecraft.world.level.block.state.properties.ChestType type = state1.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.CHEST_TYPE);
            if (type == net.minecraft.world.level.block.state.properties.ChestType.LEFT) {
                return pos1.relative(facing.getClockWise()).equals(pos2);
            } else if (type == net.minecraft.world.level.block.state.properties.ChestType.RIGHT) {
                return pos1.relative(facing.getCounterClockWise()).equals(pos2);
            }
        }
        return false;
    }

    @Redirect(
            method = "amountInNetwork",
            at = @At(value = "INVOKE", target = "Ljagm/classicpipes/blockentity/ProviderPipe;getCache()Ljava/util/List;")
    )
    private List<ItemStack> classicpipesnt$preventSelfRequest(ProviderPipe providerPipe, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) BlockPos requestPos) {
        List<ItemStack> cache = providerPipe.getCache();
        if (cache.isEmpty() || this.classicpipesnt$currentLevel == null) return cache;
        
        Direction providerFacing = providerPipe.getFacing();
        if (providerFacing == null) return cache;
        
        BlockPos providerContainerPos = providerPipe.getProviderPipePos().relative(providerFacing);

        // Layer 1: Self-Request Check
        BlockEntity be = this.classicpipesnt$currentLevel.getBlockEntity(requestPos);
        if (be != null) {
            BlockState requestState = be.getBlockState();
            for (net.minecraft.world.level.block.state.properties.Property<?> prop : requestState.getProperties()) {
                if (prop.getName().equals("facing")) {
                    Object value = requestState.getValue(prop);
                    Direction requestFacing = null;
                    if (value instanceof Direction) {
                        requestFacing = (Direction) value;
                    } else if (value instanceof jagm.classicpipes.util.FacingOrNone) {
                        requestFacing = ((jagm.classicpipes.util.FacingOrNone) value).getDirection();
                    }
                    if (requestFacing != null) {
                        BlockPos requestContainerPos = requestPos.relative(requestFacing);
                        if (this.classicpipesnt$areSameContainer(this.classicpipesnt$currentLevel, providerContainerPos, requestContainerPos)) {
                            return Collections.emptyList();
                        }
                    }
                    break;
                }
            }
        }

        // Layer 2: Self-Preservation (Chest B won't provide what Chest B's StockingPipe wants)
        // Skip Layer 2 completely if a player is requesting items directly via a Request Pipe!
        if (be instanceof jagm.classicpipes.blockentity.RequestPipeEntity) {
            return cache;
        }

        PipeNetwork network = (PipeNetwork) (Object) this;
        List<jagm.classicpipes.blockentity.StockingPipeEntity> sharingStockingPipes = new java.util.ArrayList<>();
        for (jagm.classicpipes.blockentity.StockingPipeEntity stockingPipe : network.getStockingPipes()) {
            Direction stockingFacing = stockingPipe.getBlockState().getValue(jagm.classicpipes.block.StockingPipeBlock.FACING).getDirection();
            if (stockingFacing != null) {
                BlockPos stockingContainerPos = stockingPipe.getBlockPos().relative(stockingFacing);
                if (this.classicpipesnt$areSameContainer(this.classicpipesnt$currentLevel, providerContainerPos, stockingContainerPos)) {
                    sharingStockingPipes.add(stockingPipe);
                }
            }
        }

        if (sharingStockingPipes.isEmpty()) {
            return cache;
        }

        List<ItemStack> filteredCache = new java.util.ArrayList<>();
        for (ItemStack cacheStack : cache) {
            boolean wantedByStockingPipe = false;
            for (jagm.classicpipes.blockentity.StockingPipeEntity stockingPipe : sharingStockingPipes) {
                jagm.classicpipes.inventory.container.Filter filter = stockingPipe.getFilter();
                for (int i = 0; i < filter.getContainerSize(); i++) {
                    ItemStack filterStack = filter.getItem(i);
                    if (!filterStack.isEmpty()) {
                        if (filterStack.getItem() instanceof jagm.classicpipes.item.LabelItem labelItem) {
                            if (labelItem.itemMatches(filterStack, cacheStack)) {
                                wantedByStockingPipe = true;
                                break;
                            }
                        } else if (ItemStack.isSameItemSameComponents(filterStack, cacheStack)) {
                            wantedByStockingPipe = true;
                            break;
                        }
                    }
                }
                if (wantedByStockingPipe) break;
            }
            if (!wantedByStockingPipe) {
                filteredCache.add(cacheStack);
            }
        }

        return filteredCache;
    }
}
