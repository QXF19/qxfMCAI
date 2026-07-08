package cn.qxf.mcai;

import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.ModEntities;
import cn.qxf.mcai.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(QxfMcAi.MOD_ID)
public final class QxfMcAi {
    public static final String MOD_ID = "qxfmcai";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QxfMcAi() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.REGISTER.register(modBus);
        modBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, McAiConfig.SPEC, "qxfmcai-server.toml");
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("qxfMCAI v7.0.0 已加载：单实体原生3D龙龙与轻量任务核心就绪");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            AiService.init();
            // 不在 common setup 写 ForgeConfigSpec：SERVER 配置此时可能尚未绑定 Config 对象。
            LOGGER.info("龙龙任务引擎与 OP4 命令源已启用；未在启动阶段写入 Forge 配置");
        });
    }
}
