package net.blupillcosby.classicpipesnt.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {

    @Shadow @Final private String directory;

    @Inject(method = "build", at = @At("HEAD"))
    private void classicpipesnt$injectTags(Map<Identifier, List<TagLoader.EntryWithSource>> builders, CallbackInfoReturnable<Map<Identifier, List<?>>> cir) {
        if (!"tags/item".equals(this.directory)) {
            return;
        }

        boolean hasTechReborn = FabricLoader.getInstance().isModLoaded("techreborn");

        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;

            String namespace = id.getNamespace();
            String path = id.getPath();

            // Tech Reborn logic
            if (hasTechReborn && "techreborn".equals(namespace)) {
                if (path.contains("circuit") || path.contains("frame") || path.contains("plate") || path.contains("wire") || path.contains("part") || path.contains("component") || path.contains("casing") || path.contains("cell") || path.contains("upgrade")) {
                    classicpipesnt$addTag(builders, Identifier.parse("techreborn:components"), id);
                } else if (path.contains("cable") || path.contains("generator") || path.contains("battery") || path.contains("solar") || path.contains("panel")) {
                    classicpipesnt$addTag(builders, Identifier.parse("techreborn:power"), id);
                } else if (path.contains("furnace") || path.contains("machine") || path.contains("compressor") || path.contains("extractor") || path.contains("grinder") || path.contains("sawmill") || path.contains("assembler") || path.contains("smelter") || path.contains("centrifuge") || path.contains("pump") || path.contains("tank") || path.contains("heater") || path.contains("reactor")) {
                    classicpipesnt$addTag(builders, Identifier.parse("techreborn:workbenches"), id);
                } else {
                    classicpipesnt$addTag(builders, Identifier.parse("techreborn:materials"), id);
                }
            }

            // Vanilla Dimensions and Light Sources logic
            if ("minecraft".equals(namespace)) {
                // Light Sources
                if (path.contains("glowstone") || path.contains("torch") || path.contains("lantern") || path.contains("lamp") || path.contains("campfire") || path.contains("shroomlight") || path.contains("froglight") || path.contains("beacon") || path.contains("end_rod")) {
                    classicpipesnt$addTag(builders, Identifier.parse("classicpipesnt:light_sources"), id);
                }

                // Nether Items
                if (path.contains("nether") || path.contains("quartz") || path.contains("soul") || path.contains("magma") || path.contains("ghast_tear") || path.contains("wither_skeleton_skull") || path.contains("gold_nugget") || path.contains("crimson") || path.contains("warped") || path.contains("basalt") || path.contains("blackstone") || path.contains("ancient_debris") || path.contains("respawn_anchor") || path.contains("weeping_vines") || path.contains("twisting_vines") || path.contains("nylium") || path.equals("glowstone_dust")) {
                    classicpipesnt$addTag(builders, Identifier.parse("classicpipesnt:nether_dimension"), id);
                }

                // End Items
                if (!path.equals("ender_pearl") && !path.equals("ender_eye") && (path.contains("end") || path.contains("purpur") || path.contains("chorus") || path.contains("shulker_shell") || path.contains("elytra") || path.contains("dragon_breath") || path.contains("dragon_egg"))) {
                    classicpipesnt$addTag(builders, Identifier.parse("classicpipesnt:end_dimension"), id);
                }
                
                // Eye of Ender Ingredients
                if (path.equals("ender_pearl") || path.equals("blaze_powder") || path.equals("blaze_rod") || path.equals("ender_eye")) {
                    classicpipesnt$addTag(builders, Identifier.parse("classicpipesnt:eye_of_ender_ingredients"), id);
                }
                
                // Redstone Items
                if (path.contains("redstone") || path.contains("lever") || path.contains("button") || path.contains("pressure_plate") || path.contains("piston") || path.contains("observer") || path.contains("dispenser") || path.contains("dropper") || path.contains("hopper") || path.contains("repeater") || path.contains("comparator") || path.contains("target") || path.contains("sculk_sensor") || path.contains("tripwire_hook") || path.contains("daylight_detector") || path.contains("lightning_rod") || path.contains("trapped_chest")) {
                    classicpipesnt$addTag(builders, Identifier.parse("classicpipesnt:redstone_item"), id);
                }
            }

            // Damaging Food logic (works for vanilla & modded)
            boolean isDamaging = false;
            try {
                net.minecraft.world.item.component.Consumable consumable = item.components().get(net.minecraft.core.component.DataComponents.CONSUMABLE);
                if (consumable != null) {
                    for (net.minecraft.world.item.consume_effects.ConsumeEffect effect : consumable.onConsumeEffects()) {
                        if (effect instanceof net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect statusEffect) {
                            for (net.minecraft.world.effect.MobEffectInstance instance : statusEffect.effects()) {
                                if (instance.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                                    isDamaging = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Components not bound yet, ignore and fallback to path check
            }
            
            if (isDamaging || path.contains("rotten") || path.contains("poisonous") || path.contains("spider_eye") || path.contains("pufferfish")) {
                classicpipesnt$addTag(builders, Identifier.parse("classicpipesnt:damaging_food"), id);
            }
        }
    }

    private void classicpipesnt$addTag(Map<Identifier, List<TagLoader.EntryWithSource>> builders, Identifier tagId, Identifier elementId) {
        builders.computeIfAbsent(tagId, k -> new ArrayList<>())
                .add(new TagLoader.EntryWithSource(TagEntry.element(elementId), "classicpipesnt"));
    }
}
