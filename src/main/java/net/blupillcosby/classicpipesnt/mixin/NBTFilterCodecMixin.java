package net.blupillcosby.classicpipesnt.mixin;

import com.mojang.serialization.Codec;
import net.blupillcosby.classicpipesnt.util.CustomCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = {
        jagm.classicpipes.blockentity.StockingPipeEntity.class,
        jagm.classicpipes.blockentity.RoutingPipeEntity.class,
        jagm.classicpipes.blockentity.RecipePipeEntity.class,
        jagm.classicpipes.blockentity.ProviderPipeEntity.class,
        jagm.classicpipes.blockentity.DiamondPipeEntity.class,
        jagm.classicpipes.blockentity.DiamondFluidPipeEntity.class,
        jagm.classicpipes.blockentity.AdvancedCopperPipeEntity.class,
        jagm.classicpipes.blockentity.AdvancedCopperFluidPipeEntity.class,
        jagm.classicpipes.blockentity.NetworkedPipeEntity.class
})
public class NBTFilterCodecMixin {

    @ModifyArg(
            method = "saveAdditional",
            at = @At(value = "INVOKE", target = "Ljagm/classicpipes/util/nbt/ValueOutput;list(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Ljagm/classicpipes/util/nbt/ValueOutput$TypedOutputList;"),
            index = 1,
            require = 0
    )
    private Codec<?> classicpipesnt$modifySaveCodec(Codec<?> originalCodec) {
        if (originalCodec == jagm.classicpipes.util.MiscUtil.UNLIMITED_STACK_WITH_SLOT_CODEC || originalCodec == net.minecraft.world.ItemStackWithSlot.CODEC) {
            return CustomCodecs.INT_SLOT_CODEC;
        }
        return originalCodec;
    }

    @ModifyArg(
            method = "loadAdditional",
            at = @At(value = "INVOKE", target = "Ljagm/classicpipes/util/nbt/ValueInput;listOrEmpty(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Ljagm/classicpipes/util/nbt/ValueInput$TypedInputList;"),
            index = 1,
            require = 0
    )
    private Codec<?> classicpipesnt$modifyLoadCodec(Codec<?> originalCodec) {
        if (originalCodec == jagm.classicpipes.util.MiscUtil.UNLIMITED_STACK_WITH_SLOT_CODEC || originalCodec == net.minecraft.world.ItemStackWithSlot.CODEC) {
            return CustomCodecs.INT_SLOT_CODEC;
        }
        return originalCodec;
    }
}
