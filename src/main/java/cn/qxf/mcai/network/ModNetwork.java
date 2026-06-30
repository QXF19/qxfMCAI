package cn.qxf.mcai.network;

import cn.qxf.mcai.QxfMcAi;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(QxfMcAi.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static boolean registered;

    private ModNetwork() {}

    public static synchronized void register() {
        if (registered) return;
        int id = 0;
        CHANNEL.registerMessage(id, UpdateAiConfigPacket.class,
            UpdateAiConfigPacket::encode,
            UpdateAiConfigPacket::decode,
            UpdateAiConfigPacket::handle);
        registered = true;
    }
}
