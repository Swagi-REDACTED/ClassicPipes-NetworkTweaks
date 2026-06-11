package net.blupillcosby.classicpipesnt.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import jagm.classicpipes.inventory.container.Filter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;

public class FilterBackupManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();

    public static String getBackupDimId(Level level) {
        String dimString = level.dimension().toString();
        String dimId = dimString;
        if (dimString.contains(" / ")) {
            dimId = dimString.substring(dimString.lastIndexOf(" / ") + 3, dimString.length() - 1);
        }
        return dimId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public static File getBackupFile(Level level) {
        String worldName = "default";
        if (level.getServer() != null) {
            worldName = level.getServer().getWorldData().getLevelName().replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "classicpipesnt/filters/" + worldName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "filters.json");
    }

    public static void saveBackupAsync(Level level, BlockPos pos, Filter filter) {
        if (level == null || level.isClientSide()) return;
        
        int size = filter.getContainerSize();
        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            items[i] = filter.getItem(i).copy();
        }
        
        HolderLookup.Provider provider = level.registryAccess();
        String dimId = getBackupDimId(level);
        String posId = pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        
        CompletableFuture.runAsync(() -> {
            synchronized (LOCK) {
                try {
                    File file = getBackupFile(level);
                    JsonObject root = new JsonObject();
                    if (file.exists()) {
                        try (FileReader reader = new FileReader(file)) {
                            root = GSON.fromJson(reader, JsonObject.class);
                            if (root == null) root = new JsonObject();
                        } catch (Exception e) {
                            root = new JsonObject();
                        }
                    }
                    
                    if (!root.has(dimId)) {
                        root.add(dimId, new JsonObject());
                    }
                    JsonObject dimObj = root.getAsJsonObject(dimId);
                    
                    JsonArray filterArray = new JsonArray();
                    for (int i = 0; i < size; i++) {
                        ItemStack stack = items[i];
                        if (!stack.isEmpty()) {
                            JsonObject itemObj = new JsonObject();
                            itemObj.addProperty("slot", i);
                            JsonElement stackJson = ItemStack.OPTIONAL_CODEC.encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow();
                            itemObj.add("item", stackJson);
                            filterArray.add(itemObj);
                        }
                    }
                    
                    if (filterArray.size() > 0) {
                        dimObj.add(posId, filterArray);
                    } else {
                        dimObj.remove(posId);
                    }
                    
                    try (FileWriter writer = new FileWriter(file)) {
                        GSON.toJson(root, writer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void restoreBackup(Level level, BlockPos pos, Filter filter) {
        if (level == null || level.isClientSide()) return;
        
        String dimId = getBackupDimId(level);
        String posId = pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        HolderLookup.Provider provider = level.registryAccess();
        
        synchronized (LOCK) {
            File file = getBackupFile(level);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root != null && root.has(dimId)) {
                        JsonObject dimObj = root.getAsJsonObject(dimId);
                        if (dimObj.has(posId)) {
                            JsonArray filterArray = dimObj.getAsJsonArray(posId);
                            for (int i = 0; i < filterArray.size(); i++) {
                                JsonObject itemObj = filterArray.get(i).getAsJsonObject();
                                int slot = itemObj.get("slot").getAsInt();
                                if (itemObj.has("item")) {
                                    ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(provider.createSerializationContext(JsonOps.INSTANCE), itemObj.get("item")).getOrThrow();
                                    if (!stack.isEmpty() && slot < filter.getContainerSize()) {
                                        filter.setItem(slot, stack);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
