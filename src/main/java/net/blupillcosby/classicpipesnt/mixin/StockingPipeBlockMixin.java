package net.blupillcosby.classicpipesnt.mixin;

import jagm.classicpipes.block.StockingPipeBlock;
import jagm.classicpipes.blockentity.StockingPipeEntity;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeEntity;
import net.blupillcosby.classicpipesnt.network.ClientBoundAllowOverstockingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StockingPipeBlock.class)
public class StockingPipeBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void classicpipesnt$onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof StockingPipeEntity stockingPipe) {
                ServerPlayNetworking.send(serverPlayer, new ClientBoundAllowOverstockingPayload(pos, ((IStockingPipeEntity)stockingPipe).classicpipesnt$getAllowOverflow()));
            }
        }
    }

}
