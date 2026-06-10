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
        if (state1.getBlock() instanceof ChestBlock) {
            Direction facing = state1.getValue(ChestBlock.FACING);
            ChestType type = state1.getValue(ChestBlock.TYPE);
            if (type == ChestType.LEFT) {
                return pos1.relative(facing.getClockWise()).equals(pos2);
            } else if (type == ChestType.RIGHT) {
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
        if (this.classicpipesnt$currentLevel != null) {
            Direction providerFacing = providerPipe.getFacing();
            if (providerFacing != null) {
                BlockPos providerContainerPos = providerPipe.getProviderPipePos().relative(providerFacing);

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
            }
        }
        return providerPipe.getCache();
    }
}
