package net.blupillcosby.classicpipesnt.mixin.client.screen;

import jagm.classicpipes.client.screen.FilterScreen;
import jagm.classicpipes.client.screen.StockingPipeScreen;
import jagm.classicpipes.client.screen.widget.SmallerCheckbox;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.services.Services;
import net.blupillcosby.classicpipesnt.interfaces.IStockingPipeMenu;
import net.blupillcosby.classicpipesnt.network.ServerBoundAllowOverstockingPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StockingPipeScreen.class)
public abstract class StockingPipeScreenMixin extends FilterScreen<StockingPipeMenu> {

    @Unique
    private SmallerCheckbox classicpipesnt$checkbox;

    @Unique
    private static final Identifier EXPANDED_BACKGROUND =
            Identifier.fromNamespaceAndPath(
                    "classicpipes-networktweaks",
                    "textures/gui/container/storage_pipe_expanded_slotted.png"
            );

    protected StockingPipeScreenMixin(StockingPipeMenu menu, Inventory inventory, Component title, int w, int h) {
        super(menu, inventory, title, w, h);
    }

    // FIX SIZE PROPERLY (constructor args)
    @ModifyArgs(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljagm/classicpipes/client/screen/FilterScreen;<init>(Ljagm/classicpipes/inventory/menu/StockingPipeMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;II)V"
            )
    )
    private void classicpipesnt$resizeCtor(Args args) {
        args.set(3, 176);
        args.set(4, 184);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void classicpipesnt$init(CallbackInfo ci) {
        this.inventoryLabelY += 18;
        StockingPipeMenu menu = this.getMenu();

        this.classicpipesnt$checkbox = SmallerCheckbox.builder()
                .pos(this.leftPos + 8, this.topPos + 70)
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
    }


    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void classicpipesnt$extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        
        if (EXPANDED_BACKGROUND == null) return;

        int x = (this.width - 176) / 2;
        int y = (this.height - 166) / 2;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                EXPANDED_BACKGROUND,
                x, y,
                0.0F, 0.0F,
                176, 184,
                256, 256
        );

        ci.cancel();
    }

    @Unique
    private void classicpipesnt$helper() {}
}