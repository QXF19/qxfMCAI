package cn.qxf.mcai;

import cn.qxf.mcai.ai.AiService;
import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.entity.ModEntities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(QxfMcAi.MOD_ID)
public final class QxfMcAi {
    public static final String MOD_ID = "qxfmcai";

    public QxfMcAi() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.REGISTER.register(modBus);
        modBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, McAiConfig.SPEC, "qxfmcai-server.toml");
        MinecraftForge.EVENT_BUS.register(this);
        McAiConfig.ensureSkinDirectory();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AiService::init);
    }
}

