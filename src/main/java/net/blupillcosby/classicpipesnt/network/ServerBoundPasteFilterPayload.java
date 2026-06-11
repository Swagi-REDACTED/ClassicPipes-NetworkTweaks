package net.blupillcosby.classicpipesnt.network;

import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.util.MiscUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ServerBoundPasteFilterPayload(List<ItemStackWithSlot> items, boolean allPages, int currentPage) implements CustomPacketPayload {

    public static final Type<ServerBoundPasteFilterPayload> TYPE = new Type<>(MiscUtil.identifier("paste_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundPasteFilterPayload> STREAM_CODEC = StreamCodec.composite(
            MiscUtil.ITEM_STACK_WITH_SLOT_STREAM_CODEC.apply(ByteBufCodecs.list()), ServerBoundPasteFilterPayload::items,
            ByteBufCodecs.BOOL, ServerBoundPasteFilterPayload::allPages,
            ByteBufCodecs.INT, ServerBoundPasteFilterPayload::currentPage,
            ServerBoundPasteFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerBoundPasteFilterPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            if (context.player().containerMenu instanceof FilterMenu filterMenu) {
                Filter filter = filterMenu.getFilter();
                int itemsPerPage = filter.getContainerSize() / 99;
                
                int startSlot = payload.allPages() ? 0 : payload.currentPage() * itemsPerPage;
                int endSlot = payload.allPages() ? filter.getContainerSize() : startSlot + itemsPerPage;
                
                for (int i = startSlot; i < endSlot && i < filter.getContainerSize(); i++) {
                    filter.setItem(i, ItemStack.EMPTY);
                }
                for (ItemStackWithSlot item : payload.items()) {
                    if (item.slot() < filter.getContainerSize()) {
                        filter.setItem(item.slot(), item.stack());
                    }
                }
                filter.setChanged();
            }
        });
    }
}
