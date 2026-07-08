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
    private enum Page { COMPANION, TASKS, APPEARANCE, API }

    private Page page = Page.COMPANION;
    private String provider = "openai";
    private boolean proactive = true;
    private boolean autonomy = true;
    private boolean allowCommands = true;
    private boolean clearKey;
    private boolean showKey;
    private EditBox baseUrlBox;
    private EditBox modelBox;
    private EditBox apiKeyBox;
    private Button providerButton;
    private Button proactiveButton;
    private Button autonomyButton;
    private Button commandButton;
    private Button clearKeyButton;
    private Button showKeyButton;
    private Component localStatus = Component.literal("龙龙会把任务加入实际执行队列");
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public AiControlScreen() { super(Component.literal("qxfMCAI · 龙龙控制中心 v7.0")); }

    @Override
    protected void init() {
        panelWidth = Math.min(360, width - 20);
        panelHeight = Math.min(270, height - 10);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(7, (height - panelHeight) / 2);
        int left = panelLeft + 10;
        int contentWidth = panelWidth - 20;
        int gap = 4;
        int tabWidth = (contentWidth - gap * 3) / 4;
        addTab("伙伴", Page.COMPANION, left, tabWidth);
        addTab("任务", Page.TASKS, left + tabWidth + gap, tabWidth);
        addTab("互动", Page.APPEARANCE, left + (tabWidth + gap) * 2, tabWidth);
        addTab("API/权限", Page.API, left + (tabWidth + gap) * 3, tabWidth);
        int half = (contentWidth - 5) / 2;
        switch (page) {
            case COMPANION -> initCompanionPage(left, panelTop + 50, contentWidth, half);
            case TASKS -> initTasksPage(left, panelTop + 50, contentWidth, half);
            case APPEARANCE -> initAppearancePage(left, panelTop + 50, contentWidth, half);
            case API -> initApiPage(left, panelTop + 50, contentWidth, half);
        }
    }

    private void addTab(String title, Page target, int x, int width) {
        addRenderableWidget(Button.builder(Component.literal((page == target ? "§d" : "") + title), b -> switchPage(target))
            .bounds(x, panelTop + 24, width, 20).build());
    }

    private void initCompanionPage(int left, int top, int width, int half) {
        int y = top;
        addPair(left, y, half, "召唤/找回", "mcai summon", "来到身边", "mcai come"); y += 25;
        addPair(left, y, half, "跟随玩家", "mcai follow", "原地等待", "mcai stay"); y += 25;
        addPair(left, y, half, "警戒保护", "mcai guard", "打开27格背包", "mcai inventory"); y += 25;
        addPair(left, y, half, "查看等级/想法", "mcai status", "停止并等待", "mcai task stop 1"); y += 25;
        addPair(left, y, half, "装备最佳武器", "mcai equip weapon", "装备最佳镐子", "mcai equip pickaxe"); y += 25;
        addPair(left, y, half, "开启无敌（OP4）", "mcai invincible true", "关闭无敌（OP4）", "mcai invincible false"); y += 29;
        addRenderableWidget(Button.builder(Component.literal("Shift + 右键龙龙，也可直接打开他的独立背包"), b -> {})
            .bounds(left, y, width, 20).build()).active = false;
    }

    private void initTasksPage(int left, int top, int width, int half) {
        int y = top;
        addPair(left, y, half, "独立向下挖矿", "mcai mine", "寻找天然矿洞", "mcai cave"); y += 22;
        addPair(left, y, half, "伐木并收进背包", "mcai chop", "自主探索地形", "mcai explore"); y += 22;
        addPair(left, y, half, "照料/收获农田", "mcai farm", "收集附近掉落物", "mcai gather"); y += 22;
        addPair(left, y, half, "使用武器狩猎", "mcai hunt", "允许发现点传送", "mcai permit teleport"); y += 22;
        addPair(left, y, half, "巡视基地", "mcai patrol", "放置火把", "mcai task place_torch 1"); y += 22;
        addPair(left, y, half, "搭建生存庇护所", "mcai build shelter", "建造完整小屋", "mcai build house"); y += 22;
        addPair(left, y, half, "建造三格宽桥梁", "mcai build bridge", "整理到附近箱子", "mcai task deposit 1"); y += 22;
        addPair(left, y, half, "制作基础材料", "mcai task craft 1", "吃东西恢复生命", "mcai task eat 1"); y += 24;
        addRenderableWidget(Button.builder(Component.literal("更多：钓鱼、睡觉、表情、种植等可直接对龙龙说"), b -> {})
            .bounds(left, y, width, 20).build()).active = false;
    }

    private void initAppearancePage(int left, int top, int width, int half) {
        int y = top;
        addPair(left, y, half, "开心互动", "mcai task emote 1", "查看关系状态", "mcai status"); y += 25;
        addPair(left, y, half, "赠送主手饰品", "mcai accessory", "骑乘龙龙", "mcai ride"); y += 25;
        addPair(left, y, half, "开始五子棋", "mcai gomoku start", "棋局状态", "mcai status"); y += 25;
        addPair(left, y, half, "同意组建家庭", "mcai family accept", "撤回家庭同意", "mcai family decline"); y += 25;
        addPair(left, y, half, "迎接小龙宝宝", "mcai family child", "打开物资背包", "mcai inventory"); y += 29;
        addRenderableWidget(Button.builder(Component.literal("单实体原生3D：无女仆依赖、无传送同步卡顿"), b -> {})
            .bounds(left, y, width, 20).build()).active = false;
    }

    private void initApiPage(int left, int top, int width, int half) {
        int y = top;
        providerButton = Button.builder(Component.literal("提供商：" + provider), b -> cycleProvider()).bounds(left, y, width, 20).build();
        addRenderableWidget(providerButton); y += 31;
        baseUrlBox = new EditBox(font, left, y, width, 20, Component.literal("API基础地址"));
        baseUrlBox.setMaxLength(512); baseUrlBox.setValue(defaultBaseUrl(provider)); addRenderableWidget(baseUrlBox); y += 31;
        modelBox = new EditBox(font, left, y, width, 20, Component.literal("模型名"));
        modelBox.setMaxLength(128); modelBox.setValue(defaultModel(provider)); addRenderableWidget(modelBox); y += 31;
        apiKeyBox = new EditBox(font, left, y, width - 76, 20, Component.literal("API密钥"));
        apiKeyBox.setMaxLength(1024); addRenderableWidget(apiKeyBox);
        showKeyButton = Button.builder(Component.literal("显示"), b -> toggleKeyVisibility()).bounds(left + width - 71, y, 71, 20).build();
        addRenderableWidget(showKeyButton); updateKeyFormatter(); y += 25;
        proactiveButton = toggle(left, y, half, proactiveLabel(), () -> { proactive = !proactive; proactiveButton.setMessage(Component.literal(proactiveLabel())); });
        autonomyButton = toggle(left + half + 5, y, half, autonomyLabel(), () -> { autonomy = !autonomy; autonomyButton.setMessage(Component.literal(autonomyLabel())); }); y += 25;
        clearKeyButton = toggle(left, y, half, clearKeyLabel(), () -> { clearKey = !clearKey; clearKeyButton.setMessage(Component.literal(clearKeyLabel())); });
        commandButton = toggle(left + half + 5, y, half, commandLabel(), () -> {}); y += 25;
        commandButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("保存 API 与权限设置（需要 OP4）"), b -> saveApiSettings()).bounds(left, y, width, 20).build());
    }

    private Button toggle(int x, int y, int width, String label, Runnable action) {
        Button button = Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, width, 20).build();
        addRenderableWidget(button);
        return button;
    }

    private void addPair(int left, int y, int half, String a, String commandA, String b, String commandB) {
        addRenderableWidget(button(a, left, y, half, () -> command(commandA)));
        addRenderableWidget(button(b, left + half + 5, y, half, () -> command(commandB)));
    }

    private Button button(String text, int x, int y, int width, Runnable action) {
        return Button.builder(Component.literal(text), b -> { action.run(); localStatus = Component.literal("已提交：" + text); })
            .bounds(x, y, width, 20).build();
    }

    private void switchPage(Page next) { if (page != next) { page = next; clearWidgets(); init(); } }

    private void cycleProvider() {
        provider = switch (provider) { case "openai" -> "deepseek"; case "deepseek" -> "custom"; default -> "openai"; };
        providerButton.setMessage(Component.literal("提供商：" + provider));
        baseUrlBox.setValue(defaultBaseUrl(provider)); modelBox.setValue(defaultModel(provider)); apiKeyBox.setValue("");
        clearKey = false; clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
    }

    private void toggleKeyVisibility() {
        showKey = !showKey; showKeyButton.setMessage(Component.literal(showKey ? "隐藏" : "显示")); updateKeyFormatter();
    }

    private void updateKeyFormatter() {
        if (apiKeyBox != null) apiKeyBox.setFormatter((value, offset) ->
            FormattedCharSequence.forward(showKey ? value : "•".repeat(value.length()), Style.EMPTY));
    }

    private void saveApiSettings() {
        String url = baseUrlBox.getValue().trim(), model = modelBox.getValue().trim(), key = apiKeyBox.getValue().trim();
        if ((!url.startsWith("https://") && !url.startsWith("http://")) || model.isEmpty()) {
            localStatus = Component.literal("地址必须以 http(s):// 开头，模型名不能为空"); return;
        }
        ModNetwork.CHANNEL.sendToServer(new UpdateAiConfigPacket(provider, url, model, key, clearKey,
            proactive, autonomy, true));
        apiKeyBox.setValue(""); clearKey = false; clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
        localStatus = Component.literal(allowCommands ? "已保存；警告：龙龙拥有 OP4 全命令执行权" : "已安全保存到服务端");
    }

    private String proactiveLabel() { return "主动聊天：" + (proactive ? "开" : "关"); }
    private String autonomyLabel() { return "自主行动：" + (autonomy ? "开" : "关"); }
    private String commandLabel() { return "最高权限：固定开启"; }
    private String clearKeyLabel() { return "清除密钥：" + (clearKey ? "是" : "否"); }
    private static String defaultBaseUrl(String p) { return switch (p) { case "deepseek" -> "https://api.deepseek.com"; case "custom" -> "http://127.0.0.1:11434/v1"; default -> "https://api.openai.com/v1"; }; }
    private static String defaultModel(String p) { return switch (p) { case "deepseek" -> "deepseek-v4-pro"; case "custom" -> "qwen2.5:7b"; default -> "gpt-5.2-chat-latest"; }; }

    private void command(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null && !command.isBlank())
            minecraft.player.connection.sendCommand(command);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelLeft - 4, panelTop - 4, panelLeft + panelWidth + 4, panelTop + panelHeight + 4, 0xC00B1020);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 7, 0xFFFFD8);
        if (page == Page.API && baseUrlBox != null) {
            graphics.drawString(font, "API基础地址", baseUrlBox.getX(), baseUrlBox.getY() - 9, 0xC8C8C8, false);
            graphics.drawString(font, "模型名", modelBox.getX(), modelBox.getY() - 9, 0xC8C8C8, false);
            graphics.drawString(font, "API密钥（留空保留旧值）", apiKeyBox.getX(), apiKeyBox.getY() - 9, 0xC8C8C8, false);
        }
        graphics.drawCenteredString(font, localStatus, width / 2, panelTop + panelHeight - 13, 0xB8E8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
