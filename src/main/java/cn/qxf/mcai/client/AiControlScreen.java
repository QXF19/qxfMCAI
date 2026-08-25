package cn.qxf.mcai.client;

import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.network.AiConfigSnapshotPacket;
import cn.qxf.mcai.network.ModNetwork;
import cn.qxf.mcai.network.RequestAiConfigPacket;
import cn.qxf.mcai.network.UpdateAiConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/** v10 单页紧凑控制台：自然语言、常用操作、互动游戏与 AI 设置合并在同一界面。 */
public final class AiControlScreen extends Screen {
    private enum PromptSlot { CORE, TASK, AUTONOMY, CHAT }
    private static final String[][] TASK_TEMPLATES = {
        {"真实挖矿", "带上镐子向下寻找需要的矿石，发现矿洞后告诉主人"},
        {"基地建设", "观察基地并设计一项最实用且不臃肿的基础设施，然后真正建造"},
        {"农田照料", "检查附近农田，完成可以执行的收获、播种和整理"},
        {"战斗守卫", "判断主人附近的威胁，使用合适武器保护主人"},
        {"伐木收集", "选择附近树木，使用斧头伐木并把原木收进背包"},
        {"探索找洞", "安全探索并向下寻找天然矿洞，找到后报告给主人"}
    };

    private boolean settingsExpanded;
    private boolean snapshotRequested;
    private boolean proactive = true;
    private boolean autonomy = true;
    private boolean clearKey;
    private boolean showKey;
    private int templateIndex;
    private String provider = "openai";
    private String baseUrl = "";
    private String model = "";
    private String pendingApiKey = "";
    private PromptSlot promptSlot = PromptSlot.CORE;
    private String corePrompt = McAiConfig.CORE_AGENT_PROMPT;
    private String taskPrompt = McAiConfig.TASK_REASONING_PROMPT;
    private String autonomyPrompt = McAiConfig.AUTONOMY_PROMPT;
    private String proactiveChatPrompt = McAiConfig.PROACTIVE_CHAT_PROMPT;
    private EditBox commandBox;
    private EditBox baseUrlBox;
    private EditBox modelBox;
    private EditBox apiKeyBox;
    private EditBox promptBox;
    private CompactButton providerButton;
    private CompactButton proactiveButton;
    private CompactButton autonomyButton;
    private CompactButton clearKeyButton;
    private CompactButton showKeyButton;
    private CompactButton promptButton;
    private CompactButton templateButton;
    private Component status = Component.literal("主人，直接告诉我想做什么吧");
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public AiControlScreen() { super(Component.literal("qxfMCAI v10 · 龙龙紧凑控制台")); }

    @Override
    protected void init() {
        panelWidth = Math.min(500, width - 16);
        panelHeight = settingsExpanded ? Math.min(300, height - 12) : Math.min(248, height - 12);
        left = (width - panelWidth) / 2;
        top = Math.max(6, (height - panelHeight) / 2);
        int x = left + 16, contentWidth = panelWidth - 32, y = top + 35;

        commandBox = input(x, y, contentWidth - 94, 22, "对龙龙说话或交付任务", "", 512);
        addRenderableWidget(new CompactButton(x + contentWidth - 88, y, 88, 22,
            Component.literal("发送给龙龙"), true, this::submit));
        y += 26;
        if (settingsExpanded) initSettings(x, y, contentWidth);
        else initHome(x, y, contentWidth);
        if (!snapshotRequested) {
            snapshotRequested = true;
            ModNetwork.CHANNEL.sendToServer(new RequestAiConfigPacket());
        }
    }

    private void initHome(int x, int y, int width) {
        templateButton = new CompactButton(x, y, width - 102, 24, Component.literal(templateLabel()), false, this::cycleTemplate);
        addRenderableWidget(templateButton);
        addRenderableWidget(new CompactButton(x + width - 96, y, 96, 24, Component.literal("执行该方案"), true,
            () -> ask(TASK_TEMPLATES[templateIndex][1], TASK_TEMPLATES[templateIndex][0])));
        y += 29;
        buttonRow(x, y, width, new String[][]{
            {"召回龙龙", "mcai summon"}, {"27格背包", "mcai inventory"}, {"骑乘", "mcai ride"}, {"随机动作", "mcai play"}
        });
        y += 27;
        buttonRow(x, y, width, new String[][]{
            {"五子棋", "mcai gomoku start"}, {"中国象棋", "mcai chess start"}, {"查看状态", "mcai status"}, {"AI设置", "@settings"}
        });
        y += 29;
        addRenderableWidget(new CompactButton(x, y, width, 22,
            Component.literal("动作：wave / dance / cheer / bow / shy / stretch / nod / look / spin / hop"), false,
            () -> run("mcai play dance", "跳舞")));
    }

    private void initSettings(int x, int y, int width) {
        int gap = 5, third = (width - gap * 2) / 3;
        providerButton = button(x, y, third, providerLabel(), true, this::cycleProvider);
        proactiveButton = button(x + third + gap, y, third, proactiveLabel(), proactive, () -> {
            proactive = !proactive; refreshToggle(proactiveButton, proactive, proactiveLabel());
        });
        autonomyButton = button(x + (third + gap) * 2, y, third, autonomyLabel(), autonomy, () -> {
            autonomy = !autonomy; refreshToggle(autonomyButton, autonomy, autonomyLabel());
        });
        y += 25;
        baseUrlBox = input(x, y, width, 20, "API地址", baseUrl.isBlank() ? defaultBaseUrl(provider) : baseUrl, 512);
        y += 22;
        modelBox = input(x, y, width, 20, "模型名", model.isBlank() ? defaultModel(provider) : model, 128);
        y += 22;
        apiKeyBox = input(x, y, width - 150, 20, "API密钥（留空保留）", pendingApiKey, 1024);
        showKeyButton = button(x + width - 145, y, 68, showKey ? "隐藏" : "显示", false, this::toggleKey);
        clearKeyButton = button(x + width - 72, y, 72, clearKey ? "确认清除" : "保留密钥", clearKey, () -> {
            clearKey = !clearKey; refreshToggle(clearKeyButton, clearKey, clearKey ? "确认清除" : "保留密钥");
        });
        updateKeyFormatter();
        y += 25;
        promptButton = button(x, y, 126, promptLabel(), true, this::cyclePrompt);
        promptBox = input(x + 131, y, width - 131, 20, "提示词", promptValue(), 8192);
        y += 24;
        addRenderableWidget(new CompactButton(x, y, 94, 22, Component.literal("返回控制"), false, this::toggleSettings));
        addRenderableWidget(new CompactButton(x + 99, y, 116, 22, Component.literal("恢复提示词"), false, this::restorePrompt));
        addRenderableWidget(new CompactButton(x + 220, y, width - 220, 22, Component.literal("保存全部设置（OP4）"), true, this::saveSettings));
    }

    private void buttonRow(int x, int y, int width, String[][] definitions) {
        int gap = 5, each = (width - gap * (definitions.length - 1)) / definitions.length;
        for (int i = 0; i < definitions.length; i++) {
            String label = definitions[i][0], command = definitions[i][1];
            addRenderableWidget(new CompactButton(x + i * (each + gap), y, each, 23, Component.literal(label), false,
                () -> { if ("@settings".equals(command)) toggleSettings(); else run(command, label); }));
        }
    }

    private CompactButton button(int x, int y, int width, String label, boolean accent, Runnable action) {
        CompactButton button = new CompactButton(x, y, width, 22, Component.literal(label), accent, action);
        addRenderableWidget(button);
        return button;
    }

    private EditBox input(int x, int y, int width, int height, String hint, String value, int max) {
        EditBox box = new EditBox(font, x + 3, y + 2, width - 6, height - 2, Component.literal(hint));
        box.setBordered(false);
        box.setHint(Component.literal(hint));
        box.setMaxLength(max);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void submit() {
        String text = commandBox.getValue().trim();
        if (text.isBlank()) { status = Component.literal("先告诉龙龙想聊什么或做什么"); return; }
        if (text.startsWith("/")) run(text.substring(1), "游戏命令");
        else ask(text, "自然语言请求");
        commandBox.setValue("");
    }

    private void ask(String text, String label) { run("mcai ask " + text, label); }

    private void run(String command, String label) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand(command);
            status = Component.literal("已提交·" + label);
        }
    }

    private void cycleTemplate() {
        templateIndex = (templateIndex + 1) % TASK_TEMPLATES.length;
        templateButton.setMessage(Component.literal(templateLabel()));
    }

    private void toggleSettings() {
        if (settingsExpanded) preserveConnectionFields();
        storePrompt();
        settingsExpanded = !settingsExpanded;
        clearWidgets();
        init();
    }

    private void cycleProvider() {
        provider = switch (provider) { case "openai" -> "deepseek"; case "deepseek" -> "custom"; default -> "openai"; };
        providerButton.setMessage(Component.literal(providerLabel()));
        baseUrlBox.setValue(defaultBaseUrl(provider));
        modelBox.setValue(defaultModel(provider));
        baseUrl = baseUrlBox.getValue();
        model = modelBox.getValue();
    }

    private void cyclePrompt() {
        storePrompt();
        promptSlot = switch (promptSlot) { case CORE -> PromptSlot.TASK; case TASK -> PromptSlot.AUTONOMY;
            case AUTONOMY -> PromptSlot.CHAT; case CHAT -> PromptSlot.CORE; };
        promptButton.setMessage(Component.literal(promptLabel()));
        promptBox.setValue(promptValue());
    }

    private void restorePrompt() {
        promptBox.setValue(switch (promptSlot) { case CORE -> McAiConfig.CORE_AGENT_PROMPT;
            case TASK -> McAiConfig.TASK_REASONING_PROMPT; case AUTONOMY -> McAiConfig.AUTONOMY_PROMPT;
            case CHAT -> McAiConfig.PROACTIVE_CHAT_PROMPT; });
        storePrompt();
        status = Component.literal("已恢复当前提示词，保存后生效");
    }

    private void storePrompt() {
        if (promptBox == null) return;
        switch (promptSlot) { case CORE -> corePrompt = promptBox.getValue(); case TASK -> taskPrompt = promptBox.getValue();
            case AUTONOMY -> autonomyPrompt = promptBox.getValue(); case CHAT -> proactiveChatPrompt = promptBox.getValue(); }
    }

    private String promptValue() { return switch (promptSlot) { case CORE -> corePrompt; case TASK -> taskPrompt;
        case AUTONOMY -> autonomyPrompt; case CHAT -> proactiveChatPrompt; }; }

    private void toggleKey() {
        showKey = !showKey;
        showKeyButton.setMessage(Component.literal(showKey ? "隐藏" : "显示"));
        updateKeyFormatter();
    }

    private void updateKeyFormatter() {
        if (apiKeyBox != null) apiKeyBox.setFormatter((value, offset) ->
            FormattedCharSequence.forward(showKey ? value : "•".repeat(value.length()), Style.EMPTY));
    }

    private void saveSettings() {
        storePrompt();
        String url = baseUrlBox.getValue().trim(), model = modelBox.getValue().trim(), key = apiKeyBox.getValue().trim();
        if ((!url.startsWith("https://") && !url.startsWith("http://")) || model.isBlank()) {
            status = Component.literal("API地址或模型名无效"); return;
        }
        if (corePrompt.isBlank() || taskPrompt.isBlank() || autonomyPrompt.isBlank() || proactiveChatPrompt.isBlank()) {
            status = Component.literal("四类提示词都不能为空"); return;
        }
        ModNetwork.CHANNEL.sendToServer(new UpdateAiConfigPacket(provider, url, model, key, clearKey,
            proactive, autonomy, true, corePrompt, taskPrompt, autonomyPrompt, proactiveChatPrompt));
        apiKeyBox.setValue("");
        baseUrl = url;
        this.model = model;
        pendingApiKey = "";
        clearKey = false;
        status = Component.literal("设置已加密提交，密钥不会回传");
    }

    public void applyServerSnapshot(AiConfigSnapshotPacket snapshot) {
        provider = snapshot.provider(); proactive = snapshot.proactiveEnabled(); autonomy = snapshot.autonomyEnabled();
        baseUrl = snapshot.baseUrl(); model = snapshot.model();
        corePrompt = snapshot.corePrompt(); taskPrompt = snapshot.taskPrompt(); autonomyPrompt = snapshot.autonomyPrompt();
        proactiveChatPrompt = snapshot.proactiveChatPrompt();
        if (providerButton != null) providerButton.setMessage(Component.literal(providerLabel()));
        if (proactiveButton != null) refreshToggle(proactiveButton, proactive, proactiveLabel());
        if (autonomyButton != null) refreshToggle(autonomyButton, autonomy, autonomyLabel());
        if (baseUrlBox != null) baseUrlBox.setValue(snapshot.baseUrl());
        if (modelBox != null) modelBox.setValue(snapshot.model());
        if (promptBox != null) promptBox.setValue(promptValue());
        status = Component.literal("已载入服务端设置·密钥保持隐藏");
    }

    private void preserveConnectionFields() {
        if (baseUrlBox != null) baseUrl = baseUrlBox.getValue();
        if (modelBox != null) model = modelBox.getValue();
        if (apiKeyBox != null) pendingApiKey = apiKeyBox.getValue();
    }

    private static void refreshToggle(CompactButton button, boolean value, String label) {
        button.accent = value;
        button.setMessage(Component.literal(label));
    }

    private String templateLabel() { return "任务模板·" + TASK_TEMPLATES[templateIndex][0] + "（点击切换）"; }
    private String providerLabel() { return "模型·" + provider.toUpperCase(java.util.Locale.ROOT); }
    private String proactiveLabel() { return "5分钟聊天·" + (proactive ? "开" : "关"); }
    private String autonomyLabel() { return "自主思考·" + (autonomy ? "开" : "关"); }
    private String promptLabel() { return switch (promptSlot) { case CORE -> "核心人格"; case TASK -> "任务思维";
        case AUTONOMY -> "自主决策"; case CHAT -> "主动聊天"; }; }
    private static String defaultBaseUrl(String p) { return switch (p) { case "deepseek" -> "https://api.deepseek.com";
        case "custom" -> "http://127.0.0.1:11434/v1"; default -> "https://api.openai.com/v1"; }; }
    private static String defaultModel(String p) { return switch (p) { case "deepseek" -> "deepseek-v4-pro";
        case "custom" -> "qwen2.5:7b"; default -> "gpt-5.2-chat-latest"; }; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xD0060913, 0xE00D1830);
        graphics.fillGradient(left, top, left + panelWidth, top + panelHeight, 0xED161F35, 0xED0A1120);
        graphics.renderOutline(left, top, panelWidth, panelHeight, 0x906D93FF);
        graphics.fill(left, top, left + 4, top + panelHeight, 0xFF64D8C3);
        graphics.drawString(font, "龙龙 · LONGLONG  v10", left + 16, top + 12, 0xFFF4F7FF, false);
        graphics.drawString(font, settingsExpanded ? "AI设置 · 与控制台合并" : "轻量二维皮肤 · 主人互动模式",
            left + 180, top + 12, 0xFF82E7D5, false);
        inputSurface(graphics, commandBox);
        if (settingsExpanded) {
            inputSurface(graphics, baseUrlBox);
            inputSurface(graphics, modelBox);
            inputSurface(graphics, apiKeyBox);
            inputSurface(graphics, promptBox);
        }
        graphics.drawString(font, status, left + 16, top + panelHeight - 16, 0xFFA8CAFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void inputSurface(GuiGraphics graphics, EditBox box) {
        if (box == null) return;
        graphics.fill(box.getX() - 3, box.getY() - 2, box.getX() + box.getWidth() + 3, box.getY() + box.getHeight(), 0xA0060C19);
        graphics.renderOutline(box.getX() - 3, box.getY() - 2, box.getWidth() + 6, box.getHeight() + 2, 0x604E709E);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (commandBox != null && commandBox.isFocused()
            && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submit(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }

    private final class CompactButton extends AbstractButton {
        private final Runnable action;
        private boolean accent;
        private CompactButton(int x, int y, int width, int height, Component text, boolean accent, Runnable action) {
            super(x, y, width, height, text); this.action = action; this.accent = accent;
        }
        @Override public void onPress() { action.run(); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int topColor = isHoveredOrFocused() ? 0xE05079D8 : accent ? 0xC02C638D : 0xB0182945;
            int bottomColor = isHoveredOrFocused() ? 0xE02FC0A7 : accent ? 0xC024817A : 0xB0101B30;
            graphics.fillGradient(getX(), getY(), getX() + getWidth(), getY() + getHeight(), topColor, bottomColor);
            graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? 0xFF9CEAFF : 0x705878A8);
            graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFF2F7FF);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
