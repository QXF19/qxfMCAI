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
            return fileName.equals("white_dragon.png") ? createWhiteDragonTexture() : DefaultPlayerSkin.getDefaultSkin();
        }
    }

    private static ResourceLocation createWhiteDragonTexture() {
        NativeImage image = new NativeImage(64, 64, true);
        int pearl = 0xFFFFF7F2;   // ABGR: 珍珠白
        int iceBlue = 0xFFFFB45A; // ABGR: 冰蓝
        int deepBlue = 0xFF8A4A1C;
        int gold = 0xFF40C0FF;
        int ink = 0xFF181018;

        // 只绘制玩家模型的基础 UV；第二层保持透明，修复原实现把整张64x64填白
        // 后外层皮肤成为不透明白壳、遮住所有眼睛与花纹的问题。
        fill(image, 0, 0, 32, 16, pearl);   // 头
        fill(image, 0, 16, 16, 32, pearl);  // 右腿
        fill(image, 16, 16, 40, 32, pearl); // 身体
        fill(image, 40, 16, 56, 32, pearl); // 右臂
        fill(image, 16, 48, 32, 64, pearl); // 左腿
        fill(image, 32, 48, 48, 64, pearl); // 左臂

        // 正脸：金色额纹、深蓝眼睛和冰蓝口鼻。
        fill(image, 11, 8, 13, 10, gold);
        image.setPixelRGBA(10, 11, ink);
        image.setPixelRGBA(13, 11, ink);
        image.setPixelRGBA(10, 10, deepBlue);
        image.setPixelRGBA(13, 10, deepBlue);
        fill(image, 10, 13, 14, 16, iceBlue);
        image.setPixelRGBA(11, 14, deepBlue);
        image.setPixelRGBA(12, 14, deepBlue);

        // 胸口与四肢加入可辨识的蓝金鳞纹，不再是纯白人形。
        for (int y = 20; y < 32; y++) {
            int inset = Math.min(3, Math.abs(25 - y) / 2);
            fill(image, 21 + inset, y, 27 - inset, y + 1, (y % 3 == 0) ? gold : iceBlue);
        }
        fill(image, 44, 20, 48, 32, iceBlue);
        fill(image, 36, 52, 40, 64, iceBlue);
        fill(image, 4, 20, 8, 32, deepBlue);
        fill(image, 20, 52, 24, 64, deepBlue);
        ResourceLocation id = new ResourceLocation(QxfMcAi.MOD_ID, "generated/white_dragon");
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    private static void fill(NativeImage image, int x1, int y1, int x2, int y2, int color) {
        for (int y = y1; y < y2; y++) for (int x = x1; x < x2; x++) image.setPixelRGBA(x, y, color);
    }

    public static void clear() {
        CACHE.clear();
    }
}
