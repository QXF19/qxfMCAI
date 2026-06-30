package cn.qxf.mcai.client;

import cn.qxf.mcai.network.ModNetwork;
import cn.qxf.mcai.network.UpdateAiConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class AiControlScreen extends Screen {
    private enum Page { COMPANION, API }

    private Page page = Page.COMPANION;
    private String provider = "openai";
    private boolean proactive = true;
    private boolean clearKey;
    private boolean showKey;
    private EditBox skinBox;
    private EditBox baseUrlBox;
    private EditBox modelBox;
    private EditBox apiKeyBox;
    private Button providerButton;
    private Button proactiveButton;
    private Button clearKeyButton;
    private Button showKeyButton;
    private Component localStatus = Component.literal("菜单操作不会关闭界面");
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public AiControlScreen() {
        super(Component.literal("qxfMCAI 伙伴控制中心 v2.0"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(320, width - 24);
        panelHeight = Math.min(240, height - 12);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(9, (height - panelHeight) / 2);
        int contentLeft = panelLeft + 10;
        int contentWidth = panelWidth - 20;
        int half = (contentWidth - 5) / 2;

        addRenderableWidget(Button.builder(Component.literal("伙伴控制"), b -> switchPage(Page.COMPANION))
            .bounds(contentLeft, panelTop + 24, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("API 设置（OP4）"), b -> switchPage(Page.API))
            .bounds(contentLeft + half + 5, panelTop + 24, half, 20).build());

        if (page == Page.COMPANION) initCompanionPage(contentLeft, panelTop + 50, contentWidth, half);
        else initApiPage(contentLeft, panelTop + 50, contentWidth, half);
    }

    private void initCompanionPage(int left, int top, int width, int half) {
        int y = top;
        addRenderableWidget(button("召唤/找回", left, y, half, () -> command("mcai summon")));
        addRenderableWidget(button("来到身边", left + half + 5, y, half, () -> command("mcai come")));
        y += 24;
        addRenderableWidget(button("跟随", left, y, half, () -> command("mcai follow")));
        addRenderableWidget(button("等待", left + half + 5, y, half, () -> command("mcai stay")));
        y += 24;
        addRenderableWidget(button("警戒保护", left, y, half, () -> command("mcai guard")));
        addRenderableWidget(button("拾取掉落物", left + half + 5, y, half, () -> command("mcai gather")));
        y += 24;
        addRenderableWidget(button("挖掘附近矿石", left, y, half, () -> command("mcai mine")));
        addRenderableWidget(button("查看当前状态", left + half + 5, y, half, () -> command("mcai status")));
        y += 28;

        skinBox = new EditBox(font, left, y, width - 78, 20, Component.literal("皮肤PNG文件名"));
        skinBox.setMaxLength(128);
        skinBox.setValue("companion.png");
        addRenderableWidget(skinBox);
        addRenderableWidget(Button.builder(Component.literal("换肤"), b -> {
            SkinLoader.clear();
            command("mcai skin " + skinBox.getValue().trim());
            localStatus = Component.literal("已提交换肤请求");
        }).bounds(left + width - 73, y, 73, 20).build());
        y += 26;

        addRenderableWidget(button("开启无敌（OP4）", left, y, half, () -> command("mcai invincible true")));
        addRenderableWidget(button("关闭无敌（OP4）", left + half + 5, y, half, () -> command("mcai invincible false")));
    }

    private void initApiPage(int left, int top, int width, int half) {
        int y = top;
        providerButton = Button.builder(Component.literal("提供商：" + provider), b -> cycleProvider())
            .bounds(left, y, width, 20).build();
        addRenderableWidget(providerButton);
        y += 31;

        baseUrlBox = new EditBox(font, left, y, width, 20, Component.literal("API基础地址"));
        baseUrlBox.setMaxLength(512);
        baseUrlBox.setValue(defaultBaseUrl(provider));
        addRenderableWidget(baseUrlBox);
        y += 31;

        modelBox = new EditBox(font, left, y, width, 20, Component.literal("模型名"));
        modelBox.setMaxLength(128);
        modelBox.setValue(defaultModel(provider));
        addRenderableWidget(modelBox);
        y += 31;

        apiKeyBox = new EditBox(font, left, y, width - 76, 20, Component.literal("API密钥"));
        apiKeyBox.setMaxLength(1024);
        apiKeyBox.setValue("");
        addRenderableWidget(apiKeyBox);
        showKeyButton = Button.builder(Component.literal("显示"), b -> toggleKeyVisibility())
            .bounds(left + width - 71, y, 71, 20).build();
        addRenderableWidget(showKeyButton);
        updateKeyFormatter();
        y += 25;

        proactiveButton = Button.builder(Component.literal(proactiveLabel()), b -> {
            proactive = !proactive;
            proactiveButton.setMessage(Component.literal(proactiveLabel()));
        }).bounds(left, y, half, 20).build();
        addRenderableWidget(proactiveButton);
        clearKeyButton = Button.builder(Component.literal(clearKeyLabel()), b -> {
            clearKey = !clearKey;
            clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
        }).bounds(left + half + 5, y, half, 20).build();
        addRenderableWidget(clearKeyButton);
        y += 25;

        addRenderableWidget(Button.builder(Component.literal("保存全部 API 设置"), b -> saveApiSettings())
            .bounds(left, y, width, 20).build());
    }

    private Button button(String text, int x, int y, int width, Runnable action) {
        return Button.builder(Component.literal(text), b -> {
            action.run();
            localStatus = Component.literal("已提交：" + text);
        }).bounds(x, y, width, 20).build();
    }

    private void switchPage(Page next) {
        if (page == next) return;
        page = next;
        clearWidgets();
        init();
    }

    private void cycleProvider() {
        provider = switch (provider) {
            case "openai" -> "deepseek";
            case "deepseek" -> "custom";
            default -> "openai";
        };
        providerButton.setMessage(Component.literal("提供商：" + provider));
        baseUrlBox.setValue(defaultBaseUrl(provider));
        modelBox.setValue(defaultModel(provider));
        apiKeyBox.setValue("");
        clearKey = false;
        clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
        localStatus = Component.literal("已切换提供商，请输入密钥后保存");
    }

    private void toggleKeyVisibility() {
        showKey = !showKey;
        showKeyButton.setMessage(Component.literal(showKey ? "隐藏" : "显示"));
        updateKeyFormatter();
    }

    private void updateKeyFormatter() {
        if (apiKeyBox == null) return;
        apiKeyBox.setFormatter((value, offset) -> {
            String visible = showKey ? value : "•".repeat(value.length());
            return FormattedCharSequence.forward(visible, Style.EMPTY);
        });
    }

    private void saveApiSettings() {
        String url = baseUrlBox.getValue().trim();
        String model = modelBox.getValue().trim();
        String key = apiKeyBox.getValue().trim();
        if ((!url.startsWith("https://") && !url.startsWith("http://")) || model.isEmpty()) {
            localStatus = Component.literal("地址必须以 http(s):// 开头，模型名不能为空");
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new UpdateAiConfigPacket(
            provider, url, model, key, clearKey, proactive));
        apiKeyBox.setValue("");
        clearKey = false;
        clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
        localStatus = Component.literal("已发送到服务端保存；空密钥会保留原值");
    }

    private String proactiveLabel() {
        return "主动聊天：" + (proactive ? "开启" : "关闭");
    }

    private String clearKeyLabel() {
        return "清除旧密钥：" + (clearKey ? "是" : "否");
    }

    private static String defaultBaseUrl(String provider) {
        return switch (provider) {
            case "deepseek" -> "https://api.deepseek.com";
            case "custom" -> "http://127.0.0.1:11434/v1";
            default -> "https://api.openai.com/v1";
        };
    }

    private static String defaultModel(String provider) {
        return switch (provider) {
            case "deepseek" -> "deepseek-v4-pro";
            case "custom" -> "qwen2.5:7b";
            default -> "gpt-5.2-chat-latest";
        };
    }

    private void command(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null && !command.isBlank())
            minecraft.player.connection.sendCommand(command);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelLeft - 4, panelTop - 4, panelLeft + panelWidth + 4,
            panelTop + panelHeight + 4, 0xB0101010);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 7, 0xFFFFFF);

        if (page == Page.API && baseUrlBox != null) {
            graphics.drawString(font, "API基础地址", baseUrlBox.getX(), baseUrlBox.getY() - 9, 0xC8C8C8, false);
            graphics.drawString(font, "模型名", modelBox.getX(), modelBox.getY() - 9, 0xC8C8C8, false);
            graphics.drawString(font, "API密钥（留空保留服务端原值）", apiKeyBox.getX(), apiKeyBox.getY() - 9, 0xC8C8C8, false);
        }
        graphics.drawCenteredString(font, localStatus, width / 2, panelTop + panelHeight - 14, 0xB8E8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
