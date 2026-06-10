package net.blupillcosby.classicpipesnt.mixin.client.screen;

import jagm.classicpipes.client.screen.widget.SmallerCheckbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SmallerCheckbox.class)
public interface SmallerCheckboxAccessor {
    @Accessor("selected")
    void classicpipesnt$setSelected(boolean selected);
}
