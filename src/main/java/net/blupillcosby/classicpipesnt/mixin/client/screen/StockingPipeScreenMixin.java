package net.blupillcosby.classicpipesnt.mixin.client.screen;

import jagm.classicpipes.client.screen.FilterScreen;
import jagm.classicpipes.client.screen.StockingPipeScreen;
import jagm.classicpipes.client.screen.widget.SmallerCheckbox;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.services.Services;
import net.blupillcosby.classicpipesnt.client.gui.ArrowButton;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeMenu;
import net.blupillcosby.classicpipesnt.network.ServerBoundAllowOverstockingPayload;
import net.blupillcosby.classicpipesnt.network.ServerBoundPasteFilterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemStackWithSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(StockingPipeScreen.class)
public abstract class StockingPipeScreenMixin extends FilterScreen<StockingPipeMenu> {

    @Unique
    private SmallerCheckbox classicpipesnt$checkbox;

    @Unique
    private int classicpipesnt$currentPage = 0;

    @Unique
    private final int classicpipesnt$totalPages = 99;

    @Unique
    private static final Identifier EXPANDED_BACKGROUND =
            Identifier.fromNamespaceAndPath(
                    "classicpipes-networktweaks",
                    "textures/gui/container/storage_pipe_expanded_slotted.png"
            );

    protected StockingPipeScreenMixin(StockingPipeMenu menu, Inventory inventory, Component title, int w, int h) {
        super(menu, inventory, title, w, h);
    }

    @ModifyArgs(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljagm/classicpipes/client/screen/FilterScreen;<init>(Ljagm/classicpipes/inventory/menu/StockingPipeMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;II)V"
            )
    )
    private void classicpipesnt$resizeCtor(Args args) {
        args.set(3, 176);
        args.set(4, 204);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void classicpipesnt$initSize(CallbackInfo ci) {
        ((net.blupillcosby.classicpipesnt.mixin.client.screen.AbstractContainerScreenAccessor) this).setImageHeight(204);
        ((net.blupillcosby.classicpipesnt.mixin.client.screen.AbstractContainerScreenAccessor) this).setInventoryLabelY(110);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void classicpipesnt$init(CallbackInfo ci) {
        StockingPipeMenu menu = this.getMenu();

        // Shift existing checkboxes down
        for (net.minecraft.client.gui.components.events.GuiEventListener widget : this.children()) {
            if (widget instanceof SmallerCheckbox checkbox) {
                if (checkbox.getY() == this.topPos + 38 || checkbox.getY() == this.topPos + 54) {
                    checkbox.setY(checkbox.getY() + 20);
                }
            }
        }

        // Add Overflow Checkbox
        this.classicpipesnt$checkbox = SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 90)
                .onValueChange((checkbox, checked) -> {
                    Services.LOADER_SERVICE.sendToServer(
                            new ServerBoundAllowOverstockingPayload(checked)
                    );
                })
                .tooltip(Tooltip.create(
                        Component.translatable("tooltip.classicpipesnt.allow_overflow")
                ))
                .selected(((IStockingPipeMenu)(Object) menu).allowOverstocking())
                .label(Component.translatable("widget.classicpipesnt.allow_overflow"), this.font)
                .build();
                
        this.addRenderableWidget(this.classicpipesnt$checkbox);

        // Add Paginator Arrows (Left: x = leftPos + 8, Right: x = leftPos + 154)
        this.addRenderableWidget(new ArrowButton(this.leftPos + 8, this.topPos + 36, true, button -> {
            if (classicpipesnt$currentPage > 0) {
                classicpipesnt$currentPage--;
                classicpipesnt$updateSlots(menu);
            }
        }));

        this.addRenderableWidget(new ArrowButton(this.leftPos + 154, this.topPos + 36, false, button -> {
            if (classicpipesnt$currentPage < classicpipesnt$totalPages - 1) {
                classicpipesnt$currentPage++;
                classicpipesnt$updateSlots(menu);
            }
        }));

        classicpipesnt$updateSlots(menu);
    }

    @Unique
    private void classicpipesnt$updateSlots(StockingPipeMenu menu) {
        List<Slot> slots = menu.slots;
        int filterSize = menu.getFilter().getContainerSize();
        int itemsPerPage = filterSize / classicpipesnt$totalPages;
        
        for (int i = 0; i < filterSize && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot instanceof jagm.classicpipes.inventory.menu.FilterSlot) {
                int page = i / itemsPerPage;
                int indexOnPage = i % itemsPerPage;

                net.blupillcosby.classicpipesnt.mixin.accessor.SlotAccessor accessor = (net.blupillcosby.classicpipesnt.mixin.accessor.SlotAccessor) slot;
                
                if (page == classicpipesnt$currentPage) {
                    accessor.setX(8 + indexOnPage * 18);
                    accessor.setY(18);
                } else {
                    accessor.setX(-10000);
                    accessor.setY(-10000);
                }
            }
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void classicpipesnt$extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (EXPANDED_BACKGROUND == null) return;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                EXPANDED_BACKGROUND,
                this.leftPos, this.topPos,
                0.0F, 0.0F,
                176, 204,
                256, 256
        );

        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 1)
    private void classicpipesnt$drawPageNumber(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        String text = (classicpipesnt$currentPage + 1) + "/" + classicpipesnt$totalPages;
        int width = this.font.width(text);
        int centerX = 8 + (9 * 18) / 2;
        int y = 42; 
        
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)this.leftPos, (float)this.topPos);
        graphics.text(this.font, text, centerX - width / 2, y, 0xFF404040, false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null && this.minecraft.hasControlDown()) {
            boolean shiftHeld = this.minecraft.hasShiftDown();
            if (event.input() == 67) { // C
                Filter filter = this.getMenu().getFilter();
                List<ItemStackWithSlot> copied = new ArrayList<>();
                int itemsPerPage = filter.getContainerSize() / classicpipesnt$totalPages;

                int startPage = shiftHeld ? 0 : classicpipesnt$currentPage;
                int endPage = shiftHeld ? classicpipesnt$totalPages - 1 : classicpipesnt$currentPage;

                for (int page = startPage; page <= endPage; page++) {
                    for (int i = 0; i < itemsPerPage; i++) {
                        int slotIndex = page * itemsPerPage + i;
                        ItemStack stack = filter.getItem(slotIndex);
                        if (!stack.isEmpty()) {
                            int storedIndex = shiftHeld ? slotIndex : i;
                            copied.add(new ItemStackWithSlot(storedIndex, stack.copy()));
                        }
                    }
                }
                
                net.blupillcosby.classicpipesnt.ClientState.copiedFilterItems = copied;
                net.blupillcosby.classicpipesnt.ClientState.copiedAllPages = shiftHeld;

                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable(shiftHeld ? "chat.classicpipesnt.filter_copied_all" : "chat.classicpipesnt.filter_copied"));
                }
                return true;
            } else if (event.input() == 86) { // V
                if (net.blupillcosby.classicpipesnt.ClientState.copiedFilterItems != null) {
                    List<ItemStackWithSlot> toPaste = new ArrayList<>();
                    int itemsPerPage = this.getMenu().getFilter().getContainerSize() / classicpipesnt$totalPages;

                    boolean copiedAll = net.blupillcosby.classicpipesnt.ClientState.copiedAllPages;
                    
                    if (copiedAll && shiftHeld) {
                        toPaste.addAll(net.blupillcosby.classicpipesnt.ClientState.copiedFilterItems);
                    } else if (copiedAll && !shiftHeld) {
                        for (ItemStackWithSlot item : net.blupillcosby.classicpipesnt.ClientState.copiedFilterItems) {
                            int page = item.slot() / itemsPerPage;
                            if (page == classicpipesnt$currentPage) {
                                toPaste.add(new ItemStackWithSlot(item.slot(), item.stack().copy()));
                            }
                        }
                    } else if (!copiedAll) {
                        for (ItemStackWithSlot item : net.blupillcosby.classicpipesnt.ClientState.copiedFilterItems) {
                            int targetSlot = classicpipesnt$currentPage * itemsPerPage + item.slot();
                            toPaste.add(new ItemStackWithSlot(targetSlot, item.stack().copy()));
                        }
                    }

                    ClientPlayNetworking.send(new ServerBoundPasteFilterPayload(toPaste, shiftHeld && copiedAll, classicpipesnt$currentPage));
                    
                    if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.translatable(shiftHeld && copiedAll ? "chat.classicpipesnt.filter_pasted_all" : "chat.classicpipesnt.filter_pasted"));
                    }
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }
}