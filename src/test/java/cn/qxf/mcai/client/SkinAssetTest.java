package cn.qxf.mcai.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class SkinAssetTest {
    @Test void bundledLonglongSkinIsStandardTransparentMinecraftSkin() throws Exception {
        var stream = SkinAssetTest.class.getResourceAsStream("/assets/qxfmcai/textures/entity/longlong.png");
        assertNotNull(stream, "v10 二维皮肤必须打入发行包");
        BufferedImage image = ImageIO.read(stream);
        assertNotNull(image, "皮肤必须是可读取的 PNG");
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha(), "皮肤应保留透明外层");
    }
}
