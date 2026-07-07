package cn.qxf.mcai.compat;

import cn.qxf.mcai.QxfMcAi;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * YSM 附属模组桥：只安装龙龙自己的模型包，不读取或修改任何玩家模型选择。
 */
public final class YsmAddonBridge {
    private YsmAddonBridge() {}

    public static void installDefaultModel() {
        if (!ModList.get().isLoaded("yes_steve_model")) {
            QxfMcAi.LOGGER.error("缺少必需附属模组 Yes Steve Model 2.6.5，龙龙将使用内置 3D 白龙回退");
            return;
        }
        Path target = FMLPaths.CONFIGDIR.get().resolve("yes_steve_model").resolve("custom").resolve("001.ysm");
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = YsmAddonBridge.class.getResourceAsStream("/assets/qxfmcai/ysm_models/001.ysm")) {
                if (input == null) throw new IllegalStateException("内置 001.ysm 资源不存在");
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            QxfMcAi.LOGGER.info("YSM 附属资源已就绪：{}（不会修改玩家皮肤）", target);
        } catch (Exception e) {
            QxfMcAi.LOGGER.error("安装龙龙 YSM 附属资源失败，将使用内置 3D 白龙回退", e);
        }
    }
}
