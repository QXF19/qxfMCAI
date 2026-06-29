package cn.qxf.mcai.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AiControlScreen extends Screen {
    private EditBox modelBox;
    private EditBox skinBox;
    private String provider = "openai";
    private Button providerButton;

    public AiControlScreen() {
        super(Component.literal("qxfMCAI 伙伴控制中心"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = height / 2 - 95;

        addRenderableWidget(button("召唤/找回", center - 110, y, () -> command("mcai summon")));
        addRenderableWidget(button("来到身边", center + 5, y, () -> command("mcai come")));
        y += 25;
        addRenderableWidget(button("跟随", center - 110, y, () -> command("mcai follow")));
        addRenderableWidget(button("等待", center + 5, y, () -> command("mcai stay")));
        y += 25;
        addRenderableWidget(button("警戒保护", center - 110, y, () -> command("mcai guard")));
        addRenderableWidget(button("拾取物品", center + 5, y, () -> command("mcai gather")));
        y += 31;

        providerButton = Button.builder(Component.literal("提供商：" + provider), b -> cycleProvider())
            .bounds(center - 110, y, 105, 20).build();
        addRenderableWidget(providerButton);
        addRenderableWidget(Button.builder(Component.literal("保存提供商（OP4）"), b -> command("mcai provider " + provider))
            .bounds(center + 5, y, 105, 20).build());
        y += 25;

        modelBox = new EditBox(font, center - 110, y, 160, 20, Component.literal("模型名"));
        modelBox.setValue("gpt-5.5");
        addRenderableWidget(modelBox);
        addRenderableWidget(Button.builder(Component.literal("保存模型"), b -> command("mcai model " + modelBox.getValue().trim()))
            .bounds(center + 55, y, 55, 20).build());
        y += 25;

        skinBox = new EditBox(font, center - 110, y, 160, 20, Component.literal("皮肤文件"));
        skinBox.setValue("companion.png");
        addRenderableWidget(skinBox);
        addRenderableWidget(Button.builder(Component.literal("换肤"), b -> {
            SkinLoader.clear();
            command("mcai skin " + skinBox.getValue().trim());
        }).bounds(center + 55, y, 55, 20).build());
        y += 25;

        addRenderableWidget(button("开启无敌 OP4", center - 110, y, () -> command("mcai invincible true")));
        addRenderableWidget(button("关闭无敌 OP4", center + 5, y, () -> command("mcai invincible false")));
    }

    private Button button(String text, int x, int y, Runnable action) {
        return Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, 105, 20).build();
    }

    private void cycleProvider() {
        provider = switch (provider) {
            case "openai" -> "deepseek";
            case "deepseek" -> "custom";
            default -> "openai";
        };
        providerButton.setMessage(Component.literal("提供商：" + provider));
        modelBox.setValue(switch (provider) {
            case "deepseek" -> "deepseek-v4-flash";
            case "custom" -> "qwen2.5:7b";
            default -> "gpt-5.5";
        });
    }

    private void command(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null && !command.isBlank()) {
            minecraft.player.connection.sendCommand(command);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 123, 0xFFFFFF);
        graphics.drawCenteredString(font, "API密钥请在服务端配置或环境变量中填写，不会在菜单中显示", width / 2, height / 2 + 128, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
