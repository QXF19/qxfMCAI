package cn.qxf.mcai.client;

import cn.qxf.mcai.network.GameBoardActionPacket;
import cn.qxf.mcai.network.ModNetwork;
import cn.qxf.mcai.network.OpenGameBoardPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** 放置式三合一棋盘：所有棋子与操作都在这里显示，不向聊天框输出棋盘。 */
public final class GameBoardScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private OpenGameBoardPacket snapshot;
    private int selectedGame;
    private int selectedX = -1;
    private int selectedY = -1;
    private String status;
    private int left;
    private int top;
    private Button passButton;

    public GameBoardScreen(OpenGameBoardPacket snapshot) {
        super(Component.literal("龙龙三合一棋盘"));
        this.snapshot = snapshot;
        this.selectedGame = Math.max(0, Math.min(2, snapshot.selectedGame()));
        this.status = snapshot.message();
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(8, (height - 320) / 2);
        int tabY = top + 16;
        addRenderableWidget(Button.builder(Component.literal("五子棋"), button -> selectGame(0))
            .bounds(left + 18, tabY, 92, 22).build());
        addRenderableWidget(Button.builder(Component.literal("中国象棋"), button -> selectGame(1))
            .bounds(left + 116, tabY, 92, 22).build());
        addRenderableWidget(Button.builder(Component.literal("围棋"), button -> selectGame(2))
            .bounds(left + 214, tabY, 92, 22).build());
        addRenderableWidget(Button.builder(Component.literal("重新开局"), button -> send(GameBoardActionPacket.START, 0, 0, 0, 0))
            .bounds(left + 290, top + 58, 112, 22).build());
        passButton = addRenderableWidget(Button.builder(Component.literal("停一手"), button ->
                send(GameBoardActionPacket.PASS, 0, 0, 0, 0))
            .bounds(left + 290, top + 86, 112, 22).build());
        addRenderableWidget(Button.builder(Component.literal("关闭棋盘"), button -> onClose())
            .bounds(left + 290, top + 270, 112, 22).build());
        updateButtons();
    }

    private void selectGame(int game) {
        selectedGame = game;
        selectedX = selectedY = -1;
        status = switch (game) {
            case 1 -> "主人执红：先点棋子，再点目标位置";
            case 2 -> "主人执黑：点击交叉点落子，支持提子、禁自杀与劫争";
            default -> "主人执黑：点击交叉点落子，先连成五子获胜";
        };
        updateButtons();
    }

    private void updateButtons() {
        if (passButton != null) passButton.visible = selectedGame == 2;
    }

    public boolean isSameBoard(BlockPos pos) { return snapshot.pos().equals(pos); }

    public void applySnapshot(OpenGameBoardPacket update) {
        snapshot = update;
        selectedGame = Math.max(0, Math.min(2, update.selectedGame()));
        selectedX = selectedY = -1;
        status = update.message();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(left, top, left + PANEL_WIDTH, top + 310, 0xF0182438, 0xF009101C);
        graphics.renderOutline(left, top, PANEL_WIDTH, 310, 0xFF65D8C2);
        graphics.drawCenteredString(font, "龙龙三合一棋盘 · 不占用聊天框", left + PANEL_WIDTH / 2, top + 4, 0xFFF2F7FF);
        drawBoard(graphics);
        drawSidebar(graphics);
        graphics.drawString(font, font.plainSubstrByWidth(status, PANEL_WIDTH - 36),
            left + 18, top + 296, 0xFFA9D9FF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBoard(GuiGraphics graphics) {
        Geometry g = geometry();
        graphics.fill(g.x - 13, g.y - 13, g.x + (g.columns - 1) * g.spacing + 14,
            g.y + (g.rows - 1) * g.spacing + 14, selectedGame == 1 ? 0xFFE1B466 : 0xFFD9A95C);
        for (int row = 0; row < g.rows; row++)
            graphics.hLine(g.x, g.x + (g.columns - 1) * g.spacing, g.y + row * g.spacing, 0xFF51351F);
        for (int column = 0; column < g.columns; column++)
            graphics.vLine(g.x + column * g.spacing, g.y, g.y + (g.rows - 1) * g.spacing, 0xFF51351F);
        if (selectedGame == 1) drawXiangqi(graphics, g);
        else drawStoneGame(graphics, g, selectedGame == 0 ? snapshot.gomokuBoard() : snapshot.goBoard());
    }

    private void drawStoneGame(GuiGraphics graphics, Geometry g, byte[] state) {
        int expected = g.columns * g.rows;
        if (state.length < expected) return;
        for (int y = 0; y < g.rows; y++) for (int x = 0; x < g.columns; x++) {
            byte stone = state[y * g.columns + x];
            if (stone == 0) continue;
            drawDisc(graphics, g.x + x * g.spacing, g.y + y * g.spacing,
                Math.max(4, g.spacing / 2 - 2), stone == 1 ? 0xFF11151C : 0xFFF3F0E6);
        }
    }

    private void drawXiangqi(GuiGraphics graphics, Geometry g) {
        String board = snapshot.xiangqiBoard();
        if (board.length() < 90) return;
        for (int y = 0; y < 10; y++) for (int x = 0; x < 9; x++) {
            char piece = board.charAt(y * 9 + x);
            if (piece == '.') continue;
            int px = g.x + x * g.spacing, py = g.y + y * g.spacing;
            boolean red = Character.isUpperCase(piece);
            drawDisc(graphics, px, py, 8, 0xFFF3D9A2);
            String symbol = String.valueOf(xiangqiSymbol(piece));
            graphics.drawCenteredString(font, symbol, px, py - 4, red ? 0xFFC72F36 : 0xFF252525);
        }
        if (selectedX >= 0) graphics.renderOutline(g.x + selectedX * g.spacing - 10,
            g.y + selectedY * g.spacing - 10, 21, 21, 0xFF59E6FF);
    }

    private void drawSidebar(GuiGraphics graphics) {
        int x = left + 290;
        String name = selectedGame == 0 ? "五子棋" : selectedGame == 1 ? "中国象棋" : "13×13 围棋";
        graphics.drawString(font, name, x, top + 124, 0xFFFFD27A, false);
        if (selectedGame == 0) {
            graphics.drawString(font, "主人胜 " + snapshot.gomokuWins(), x, top + 144, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.gomokuLosses(), x, top + 158, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.gomokuActive() ? "进行中" : "等待开局", x, top + 178, 0xFF86E8D5, false);
        } else if (selectedGame == 1) {
            graphics.drawString(font, "主人胜 " + snapshot.xiangqiOwnerWins(), x, top + 144, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.xiangqiLonglongWins(), x, top + 158, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.xiangqiActive() ? "主人执红" : "等待开局", x, top + 178, 0xFF86E8D5, false);
        } else {
            graphics.drawString(font, "主人胜 " + snapshot.goOwnerWins(), x, top + 144, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.goLonglongWins(), x, top + 158, 0xFFF1F1F1, false);
            graphics.drawString(font, "提子 " + snapshot.goOwnerCaptures() + ":" + snapshot.goLonglongCaptures(),
                x, top + 174, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.goActive() ? "主人执黑" : "等待开局", x, top + 194, 0xFF86E8D5, false);
        }
        graphics.drawWordWrap(font, Component.literal(selectedGame == 1
            ? "点选红方棋子后再点目标格。"
            : selectedGame == 2 ? "按中国规则数子，龙龙执白并贴6.5目。" : "横、竖或斜线先连成五子。"),
            x, top + 218, 112, 0xFFB7C8DD);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Geometry g = geometry();
            int x = (int) Math.round((mouseX - g.x) / g.spacing);
            int y = (int) Math.round((mouseY - g.y) / g.spacing);
            int px = g.x + x * g.spacing, py = g.y + y * g.spacing;
            if (x >= 0 && x < g.columns && y >= 0 && y < g.rows
                && Math.abs(mouseX - px) <= g.spacing * 0.48D && Math.abs(mouseY - py) <= g.spacing * 0.48D) {
                if (selectedGame == 1) {
                    if (selectedX < 0) { selectedX = x; selectedY = y; status = "已选中 " + x + "," + y; }
                    else { send(GameBoardActionPacket.MOVE, selectedX, selectedY, x, y); selectedX = selectedY = -1; }
                } else send(GameBoardActionPacket.MOVE, 0, 0, x, y);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int action, int fromX, int fromY, int toX, int toY) {
        ModNetwork.CHANNEL.sendToServer(new GameBoardActionPacket(snapshot.pos(), selectedGame, action,
            fromX, fromY, toX, toY));
        status = "等待龙龙落子……";
    }

    private Geometry geometry() {
        return switch (selectedGame) {
            case 1 -> new Geometry(left + 30, top + 62, 22, 9, 10);
            case 2 -> new Geometry(left + 28, top + 62, 18, 13, 13);
            default -> new Geometry(left + 40, top + 72, 27, 9, 9);
        };
    }

    private static void drawDisc(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static char xiangqiSymbol(char piece) { return switch (piece) {
        case 'R' -> '俥'; case 'N' -> '傌'; case 'B' -> '相'; case 'A' -> '仕'; case 'K' -> '帥';
        case 'C' -> '炮'; case 'P' -> '兵'; case 'r' -> '車'; case 'n' -> '馬'; case 'b' -> '象';
        case 'a' -> '士'; case 'k' -> '將'; case 'c' -> '砲'; case 'p' -> '卒'; default -> ' ';
    }; }

    @Override public boolean isPauseScreen() { return false; }
    private record Geometry(int x, int y, int spacing, int columns, int rows) {}
}
