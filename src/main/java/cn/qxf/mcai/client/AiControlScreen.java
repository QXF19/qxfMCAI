package cn.qxf.mcai.client;

import cn.qxf.mcai.config.McAiConfig;
import cn.qxf.mcai.network.ModNetwork;
import cn.qxf.mcai.network.AiConfigSnapshotPacket;
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

/** v9 单屏智能体控制台：任务输入为主，密集传统选择菜单已移除。 */
public final class AiControlScreen extends Screen {
    private enum View { CONTROL, INTELLIGENCE }
    private enum PromptSlot { CORE, TASK, AUTONOMY, CHAT }

    private View view = View.CONTROL;
    private PromptSlot promptSlot = PromptSlot.CORE;
    private String provider = "openai";
    private boolean proactive = true;
    private boolean autonomy = true;
    private boolean clearKey;
    private boolean showKey;
    private String corePrompt = McAiConfig.CORE_AGENT_PROMPT;
    private String taskPrompt = McAiConfig.TASK_REASONING_PROMPT;
    private String autonomyPrompt = McAiConfig.AUTONOMY_PROMPT;
    private String proactiveChatPrompt = McAiConfig.PROACTIVE_CHAT_PROMPT;
    private EditBox commandBox;
    private EditBox baseUrlBox;
    private EditBox modelBox;
    private EditBox apiKeyBox;
    private EditBox promptBox;
    private NeonButton providerButton;
    private NeonButton proactiveButton;
    private NeonButton autonomyButton;
    private NeonButton showKeyButton;
    private NeonButton clearKeyButton;
    private NeonButton promptSlotButton;
    private Component localStatus = Component.literal("AI 决策·本地真实执行·结果可验证");
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private boolean snapshotRequested;

    public AiControlScreen() { super(Component.literal("qxfMCAI v9 · 龙龙智能体")); }

    @Override
    protected void init() {
        panelWidth = Math.min(500, width - 16);
        panelHeight = Math.min(310, height - 12);
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(6, (height - panelHeight) / 2);
        int contentLeft = panelLeft + 18;
        int contentWidth = panelWidth - 36;
        int top = panelTop + 64;

        addRenderableWidget(new NeonButton(contentLeft, panelTop + 34, (contentWidth - 8) / 2, 22,
            Component.literal("行动控制台"), true, () -> switchView(View.CONTROL)));
        addRenderableWidget(new NeonButton(contentLeft + (contentWidth + 8) / 2, panelTop + 34,
            (contentWidth - 8) / 2, 22, Component.literal("AI 智能核心"), true,
            () -> switchView(View.INTELLIGENCE)));

        if (view == View.CONTROL) initControl(contentLeft, top, contentWidth);
        else initIntelligence(contentLeft, top, contentWidth);
        if (!snapshotRequested) {
            snapshotRequested = true;
            ModNetwork.CHANNEL.sendToServer(new RequestAiConfigPacket());
        }
    }

    /** 应用服务端脱敏快照；API 密钥从未包含在网络包中。 */
    public void applyServerSnapshot(AiConfigSnapshotPacket snapshot) {
        provider = snapshot.provider();
        proactive = snapshot.proactiveEnabled();
        autonomy = snapshot.autonomyEnabled();
        corePrompt = snapshot.corePrompt();
        taskPrompt = snapshot.taskPrompt();
        autonomyPrompt = snapshot.autonomyPrompt();
        proactiveChatPrompt = snapshot.proactiveChatPrompt();
        if (providerButton != null) providerButton.setMessage(Component.literal(providerLabel()));
        if (proactiveButton != null) {
            proactiveButton.accent = proactive;
            proactiveButton.setMessage(Component.literal(proactiveLabel()));
        }
        if (autonomyButton != null) {
            autonomyButton.accent = autonomy;
            autonomyButton.setMessage(Component.literal(autonomyLabel()));
        }
        if (baseUrlBox != null) baseUrlBox.setValue(snapshot.baseUrl());
        if (modelBox != null) modelBox.setValue(snapshot.model());
        if (promptBox != null) promptBox.setValue(promptValue(promptSlot));
        localStatus = Component.literal("已载入服务端设置·API 密钥保持隐藏");
    }

    private void initControl(int left, int top, int width) {
        commandBox = new EditBox(font, left + 2, top + 2, width - 114, 20, Component.literal("对龙龙说"));
        commandBox.setMaxLength(512);
        commandBox.setHint(Component.literal("例：去找铁矿，发现矿洞后告诉我"));
        commandBox.setBordered(false);
        addRenderableWidget(commandBox);
        addRenderableWidget(new NeonButton(left + width - 106, top, 106, 24, Component.literal("发送给 AI 龙龙"), true,
            this::submitNaturalLanguage));

        String[][] cards = {
            {"召唤龙龙", "mcai summon"}, {"回到身边", "mcai come"}, {"立即停止", "mcai task stop 1"},
            {"真实挖矿", "mcai ask 根据现场自主规划并真正挖矿"}, {"寻找矿洞", "mcai ask 向下开凿寻找天然矿洞，找到后告诉我"}, {"AI 建造房屋", "mcai ask 观察基地并设计一座实用小房屋，然后真正建造"},
            {"照料农田", "mcai ask 检查附近农田并完成可执行的收获或播种"}, {"使用武器战斗", "mcai ask 判断附近威胁，正确使用剑或弓战斗"}, {"伐木收集", "mcai ask 自主选择附近树木，使用斧头伐木并收集"},
            {"27格物资空间", "mcai inventory"}, {"骑乘龙龙", "mcai ride"}, {"查看智能体状态", "mcai status"}
        };
        int gap = 6;
        int cardWidth = (width - gap * 2) / 3;
        int y = top + 34;
        for (int i = 0; i < cards.length; i++) {
            int column = i % 3;
            int row = i / 3;
            String label = cards[i][0], command = cards[i][1];
            addRenderableWidget(new NeonButton(left + column * (cardWidth + gap), y + row * 30,
                cardWidth, 24, Component.literal(label), false, () -> runCommand(command, label)));
        }
    }

    private void initIntelligence(int left, int top, int width) {
        int gap = 6;
        int third = (width - gap * 2) / 3;
        providerButton = new NeonButton(left, top, third, 22, Component.literal(providerLabel()), true, this::cycleProvider);
        proactiveButton = new NeonButton(left + third + gap, top, third, 22, Component.literal(proactiveLabel()), proactive,
            () -> { proactive = !proactive; proactiveButton.accent = proactive; proactiveButton.setMessage(Component.literal(proactiveLabel())); });
        autonomyButton = new NeonButton(left + (third + gap) * 2, top, third, 22, Component.literal(autonomyLabel()), autonomy,
            () -> { autonomy = !autonomy; autonomyButton.accent = autonomy; autonomyButton.setMessage(Component.literal(autonomyLabel())); });
        addRenderableWidget(providerButton);
        addRenderableWidget(proactiveButton);
        addRenderableWidget(autonomyButton);

        baseUrlBox = flatBox(left, top + 32, width, 20, "API 基础地址", defaultBaseUrl(provider), 512);
        modelBox = flatBox(left, top + 58, width, 20, "模型名", defaultModel(provider), 128);
        apiKeyBox = flatBox(left, top + 84, width - 150, 20, "API 密钥（留空保留）", "", 1024);
        showKeyButton = new NeonButton(left + width - 144, top + 82, 68, 22, Component.literal("显示密钥"), false, this::toggleKeyVisibility);
        clearKeyButton = new NeonButton(left + width - 70, top + 82, 70, 22, Component.literal(clearKeyLabel()), false,
            () -> { clearKey = !clearKey; clearKeyButton.accent = clearKey; clearKeyButton.setMessage(Component.literal(clearKeyLabel())); });
        addRenderableWidget(showKeyButton);
        addRenderableWidget(clearKeyButton);
        updateKeyFormatter();

        promptSlotButton = new NeonButton(left, top + 114, 138, 22, Component.literal(promptSlotLabel()), true, this::cyclePromptSlot);
        addRenderableWidget(promptSlotButton);
        promptBox = flatBox(left + 144, top + 116, width - 144, 20, "可修改提示词", promptValue(promptSlot), 8192);
        addRenderableWidget(new NeonButton(left, top + 146, 138, 22, Component.literal("恢复当前默认"), false, this::restorePromptDefault));
        addRenderableWidget(new NeonButton(left + 144, top + 146, width - 144, 22,
            Component.literal("安全保存 API 与智能体设置（OP4）"), true, this::saveAiSettings));
    }

    private EditBox flatBox(int x, int y, int width, int height, String hint, String value, int max) {
        EditBox box = new EditBox(font, x + 3, y + 2, width - 6, height - 2, Component.literal(hint));
        box.setBordered(false);
        box.setHint(Component.literal(hint));
        box.setMaxLength(max);
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void submitNaturalLanguage() {
        String text = commandBox == null ? "" : commandBox.getValue().trim();
        if (text.isBlank()) { localStatus = Component.literal("请先输入想让龙龙聊天或执行的内容"); return; }
        runCommand("mcai ask " + text, "AI 请求");
        commandBox.setValue("");
    }

    private void runCommand(String command, String label) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand(command);
            localStatus = Component.literal("已提交·" + label);
        }
    }

    private void switchView(View next) {
        if (view == next) return;
        if (promptBox != null) storePromptValue();
        view = next;
        clearWidgets();
        init();
    }

    private void cycleProvider() {
        provider = switch (provider) { case "openai" -> "deepseek"; case "deepseek" -> "custom"; default -> "openai"; };
        providerButton.setMessage(Component.literal(providerLabel()));
        baseUrlBox.setValue(defaultBaseUrl(provider));
        modelBox.setValue(defaultModel(provider));
        apiKeyBox.setValue("");
        clearKey = false;
        clearKeyButton.accent = false;
        clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
    }

    private void cyclePromptSlot() {
        storePromptValue();
        promptSlot = switch (promptSlot) { case CORE -> PromptSlot.TASK; case TASK -> PromptSlot.AUTONOMY;
            case AUTONOMY -> PromptSlot.CHAT; case CHAT -> PromptSlot.CORE; };
        promptSlotButton.setMessage(Component.literal(promptSlotLabel()));
        promptBox.setValue(promptValue(promptSlot));
    }

    private void restorePromptDefault() {
        promptBox.setValue(switch (promptSlot) { case CORE -> McAiConfig.CORE_AGENT_PROMPT;
            case TASK -> McAiConfig.TASK_REASONING_PROMPT; case AUTONOMY -> McAiConfig.AUTONOMY_PROMPT;
            case CHAT -> McAiConfig.PROACTIVE_CHAT_PROMPT; });
        storePromptValue();
        localStatus = Component.literal("已恢复当前提示词默认值，点击保存后生效");
    }

    private void storePromptValue() {
        if (promptBox == null) return;
        switch (promptSlot) { case CORE -> corePrompt = promptBox.getValue(); case TASK -> taskPrompt = promptBox.getValue();
            case AUTONOMY -> autonomyPrompt = promptBox.getValue(); case CHAT -> proactiveChatPrompt = promptBox.getValue(); }
    }

    private String promptValue(PromptSlot slot) { return switch (slot) { case CORE -> corePrompt; case TASK -> taskPrompt;
        case AUTONOMY -> autonomyPrompt; case CHAT -> proactiveChatPrompt; }; }

    private void toggleKeyVisibility() {
        showKey = !showKey;
        showKeyButton.accent = showKey;
        showKeyButton.setMessage(Component.literal(showKey ? "隐藏密钥" : "显示密钥"));
        updateKeyFormatter();
    }

    private void updateKeyFormatter() {
        if (apiKeyBox != null) apiKeyBox.setFormatter((value, offset) ->
            FormattedCharSequence.forward(showKey ? value : "•".repeat(value.length()), Style.EMPTY));
    }

    private void saveAiSettings() {
        storePromptValue();
        String url = baseUrlBox.getValue().trim(), model = modelBox.getValue().trim(), key = apiKeyBox.getValue().trim();
        if ((!url.startsWith("https://") && !url.startsWith("http://")) || model.isBlank()) {
            localStatus = Component.literal("地址必须以 http(s):// 开头，模型名不能为空"); return;
        }
        if (corePrompt.isBlank() || taskPrompt.isBlank() || autonomyPrompt.isBlank() || proactiveChatPrompt.isBlank()) {
            localStatus = Component.literal("四类提示词都不能为空"); return;
        }
        ModNetwork.CHANNEL.sendToServer(new UpdateAiConfigPacket(provider, url, model, key, clearKey,
            proactive, autonomy, true, corePrompt, taskPrompt, autonomyPrompt, proactiveChatPrompt));
        apiKeyBox.setValue("");
        clearKey = false;
        clearKeyButton.accent = false;
        clearKeyButton.setMessage(Component.literal(clearKeyLabel()));
        localStatus = Component.literal("已加密提交到服务端·API 密钥不回传");
    }

    private String providerLabel() { return "模型源·" + provider.toUpperCase(java.util.Locale.ROOT); }
    private String proactiveLabel() { return "5分钟聊天·" + (proactive ? "开" : "关"); }
    private String autonomyLabel() { return "AI运行环·" + (autonomy ? "开" : "关"); }
    private String clearKeyLabel() { return clearKey ? "确认清除" : "保留密钥"; }
    private String promptSlotLabel() { return switch (promptSlot) { case CORE -> "核心人格提示词";
        case TASK -> "任务思维提示词"; case AUTONOMY -> "自主决策提示词"; case CHAT -> "5分钟聊天提示词"; }; }
    private static String defaultBaseUrl(String p) { return switch (p) { case "deepseek" -> "https://api.deepseek.com";
        case "custom" -> "http://127.0.0.1:11434/v1"; default -> "https://api.openai.com/v1"; }; }
    private static String defaultModel(String p) { return switch (p) { case "deepseek" -> "deepseek-v4-pro";
        case "custom" -> "qwen2.5:7b"; default -> "gpt-5.2-chat-latest"; }; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xD0080B18, 0xE0101830);
        graphics.fillGradient(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE0182038, 0xE00C1325);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0x806B8CFF);
        graphics.fill(panelLeft, panelTop, panelLeft + 4, panelTop + panelHeight, 0xFF6B8CFF);
        graphics.drawString(font, "龙龙·LONGLONG", panelLeft + 18, panelTop + 12, 0xFFF4F6FF, false);
        graphics.drawString(font, "v9  AGENT ONLINE", panelLeft + panelWidth - 110, panelTop + 12, 0xFF7EF0D2, false);
        if (commandBox != null) drawInputSurface(graphics, commandBox.getX() - 3, commandBox.getY() - 2,
            commandBox.getWidth() + 6, commandBox.getHeight() + 2);
        if (baseUrlBox != null) {
            drawInputSurface(graphics, baseUrlBox.getX() - 3, baseUrlBox.getY() - 2, baseUrlBox.getWidth() + 6, 20);
            drawInputSurface(graphics, modelBox.getX() - 3, modelBox.getY() - 2, modelBox.getWidth() + 6, 20);
            drawInputSurface(graphics, apiKeyBox.getX() - 3, apiKeyBox.getY() - 2, apiKeyBox.getWidth() + 6, 20);
            drawInputSurface(graphics, promptBox.getX() - 3, promptBox.getY() - 2, promptBox.getWidth() + 6, 20);
        }
        graphics.drawString(font, localStatus, panelLeft + 18, panelTop + panelHeight - 17, 0xFF9EC9FF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawInputSurface(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xA0080D1B);
        graphics.renderOutline(x, y, width, height, 0x604B6D9E);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (view == View.CONTROL && commandBox != null && commandBox.isFocused()
            && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submitNaturalLanguage(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }

    private final class NeonButton extends AbstractButton {
        private final Runnable action;
        private boolean accent;

        private NeonButton(int x, int y, int width, int height, Component text, boolean accent, Runnable action) {
            super(x, y, width, height, text);
            this.action = action;
            this.accent = accent;
        }

        @Override public void onPress() { action.run(); }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int buttonWidth = getWidth(), buttonHeight = getHeight();
            int top = !active ? 0x70313A4E : isHoveredOrFocused() ? 0xE04C70D8 : accent ? 0xC0334F9E : 0xB01B2947;
            int bottom = !active ? 0x70252C3C : isHoveredOrFocused() ? 0xE039BFA5 : accent ? 0xC0247C86 : 0xB0131D34;
            graphics.fillGradient(getX(), getY(), getX() + buttonWidth, getY() + buttonHeight, top, bottom);
            graphics.renderOutline(getX(), getY(), buttonWidth, buttonHeight, isHoveredOrFocused() ? 0xFF8FE8FF : 0x705B78B8);
            graphics.drawCenteredString(font, getMessage(), getX() + buttonWidth / 2, getY() + (buttonHeight - 8) / 2,
                active ? 0xFFF3F7FF : 0xFF8992A5);
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
