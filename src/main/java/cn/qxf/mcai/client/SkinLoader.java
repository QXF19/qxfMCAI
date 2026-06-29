package cn.qxf.mcai.client;

import cn.qxf.mcai.QxfMcAi;
import cn.qxf.mcai.config.McAiConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinLoader {
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private SkinLoader() {}

    public static ResourceLocation textureFor(String requestedName) {
        String fileName = requestedName != null && requestedName.matches("[A-Za-z0-9._-]+\\.png")
            ? requestedName : "companion.png";
        return CACHE.computeIfAbsent(fileName, SkinLoader::load);
    }

    private static ResourceLocation load(String fileName) {
        try {
            McAiConfig.ensureSkinDirectory();
            Path file = McAiConfig.skinDirectory().resolve(fileName).normalize();
            if (!file.startsWith(McAiConfig.skinDirectory()) || !Files.isRegularFile(file)) return DefaultPlayerSkin.getDefaultSkin();
            try (InputStream input = Files.newInputStream(file)) {
                NativeImage image = NativeImage.read(input);
                ResourceLocation id = new ResourceLocation(QxfMcAi.MOD_ID, "dynamic_skin/" + Integer.toHexString(fileName.hashCode()));
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
                return id;
            }
        } catch (Exception ignored) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}

