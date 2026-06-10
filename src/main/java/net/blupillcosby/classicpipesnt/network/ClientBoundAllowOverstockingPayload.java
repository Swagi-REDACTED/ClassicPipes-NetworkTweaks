package net.blupillcosby.classicpipesnt.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import jagm.classicpipes.util.MiscUtil;

public record ClientBoundAllowOverstockingPayload(BlockPos pos, boolean allowOverstocking) implements CustomPacketPayload {

    public static final Type<ClientBoundAllowOverstockingPayload> TYPE = new Type<>(MiscUtil.identifier("client_allow_overstocking"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundAllowOverstockingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ClientBoundAllowOverstockingPayload::pos,
            ByteBufCodecs.BOOL, ClientBoundAllowOverstockingPayload::allowOverstocking,
            ClientBoundAllowOverstockingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
