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
            ? requestedName : "white_dragon.png";
        return CACHE.computeIfAbsent(fileName, SkinLoader::load);
    }

    private static ResourceLocation load(String fileName) {
        try {
            McAiConfig.ensureSkinDirectory();
            Path file = McAiConfig.skinDirectory().resolve(fileName).normalize();
            if (!file.startsWith(McAiConfig.skinDirectory())) return DefaultPlayerSkin.getDefaultSkin();
            if (!Files.isRegularFile(file)) {
                if (fileName.equals("white_dragon.png")) return createWhiteDragonTexture();
                return DefaultPlayerSkin.getDefaultSkin();
            }
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

    private static ResourceLocation createWhiteDragonTexture() {
        NativeImage image = new NativeImage(64, 64, true);
        int white = 0xFFF4F7FF;
        int paleBlue = 0xFFFFD7A8;
        int deepBlue = 0xFFFF8A42;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) image.setPixelRGBA(x, y, white);
        for (int y = 8; y < 16; y++) for (int x = 8; x < 16; x++) image.setPixelRGBA(x, y, paleBlue);
        for (int y = 10; y < 12; y++) {
            image.setPixelRGBA(10, y, deepBlue);
            image.setPixelRGBA(13, y, deepBlue);
        }
        for (int y = 20; y < 32; y++) for (int x = 20; x < 28; x++)
            if ((x + y) % 5 == 0) image.setPixelRGBA(x, y, paleBlue);
        ResourceLocation id = new ResourceLocation(QxfMcAi.MOD_ID, "generated/white_dragon");
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    public static void clear() {
        CACHE.clear();
    }
}
