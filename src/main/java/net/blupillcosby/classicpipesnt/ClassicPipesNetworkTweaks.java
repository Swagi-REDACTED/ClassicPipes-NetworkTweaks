package net.blupillcosby.classicpipesnt;

import net.blupillcosby.classicpipesnt.network.ServerBoundAllowOverstockingPayload;
import net.blupillcosby.classicpipesnt.network.ClientBoundAllowOverstockingPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeEntity;

public class ClassicPipesNetworkTweaks implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(ServerBoundAllowOverstockingPayload.TYPE, ServerBoundAllowOverstockingPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ServerBoundAllowOverstockingPayload.TYPE, ServerBoundAllowOverstockingPayload::handle);

        PayloadTypeRegistry.serverboundPlay().register(net.blupillcosby.classicpipesnt.network.ServerBoundPasteFilterPayload.TYPE, net.blupillcosby.classicpipesnt.network.ServerBoundPasteFilterPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(net.blupillcosby.classicpipesnt.network.ServerBoundPasteFilterPayload.TYPE, net.blupillcosby.classicpipesnt.network.ServerBoundPasteFilterPayload::handle);

        PayloadTypeRegistry.clientboundPlay().register(ClientBoundAllowOverstockingPayload.TYPE, ClientBoundAllowOverstockingPayload.STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ClientBoundAllowOverstockingPayload.TYPE, (payload, context) -> {
            net.blupillcosby.classicpipesnt.ClientState.lastAllowOverstocking = payload.allowOverstocking();
            if (context.client().level != null) {
                BlockEntity be = context.client().level.getBlockEntity(payload.pos());
                if (be instanceof IStockingPipeEntity stockingPipe) {
                    stockingPipe.classicpipesnt$setAllowOverflow(payload.allowOverstocking());
                }
            }
            if (context.client().player != null && context.client().player.containerMenu instanceof net.blupillcosby.classicpipesnt.interfaces.IStockingPipeMenu menu) {
                menu.setAllowOverstocking(payload.allowOverstocking());
            }
        });
    }
}
