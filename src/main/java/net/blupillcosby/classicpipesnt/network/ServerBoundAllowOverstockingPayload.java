package net.blupillcosby.classicpipesnt.network;

import jagm.classicpipes.blockentity.StockingPipeEntity;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public record ServerBoundAllowOverstockingPayload(boolean allowOverstocking) implements CustomPacketPayload {

    public static final Type<ServerBoundAllowOverstockingPayload> TYPE = new Type<>(MiscUtil.identifier("allow_overstocking"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundAllowOverstockingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundAllowOverstockingPayload::allowOverstocking,
            ServerBoundAllowOverstockingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerBoundAllowOverstockingPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (player != null && player.containerMenu instanceof IStockingPipeMenu menu) {
            menu.setAllowOverstocking(payload.allowOverstocking());

            if (menu instanceof StockingPipeMenu stockingMenu && stockingMenu.getFilter().getPipe() instanceof StockingPipeEntity stockingPipe) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    stockingPipe.updateCache(serverLevel);
                    ServerPlayNetworking.send(player, new ClientBoundAllowOverstockingPayload(stockingPipe.getBlockPos(), payload.allowOverstocking()));
                }
            }
        }
    }
}
