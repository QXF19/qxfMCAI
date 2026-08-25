package cn.qxf.mcai.client;

import cn.qxf.mcai.network.GameBoardActionPacket;
import cn.qxf.mcai.network.ModNetwork;
import cn.qxf.mcai.network.OpenGameBoardPacket;
import cn.qxf.mcai.game.MahjongGame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** 放置式四合一棋桌：五子棋、象棋、围棋和四人麻将都在这里操作。 */
public final class GameBoardScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 420;
    private static final int MAX_PANEL_HEIGHT = 310;
    private OpenGameBoardPacket snapshot;
    private int selectedGame;
    private int selectedX = -1;
    private int selectedY = -1;
    private String status;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private Button passButton;
    private Button chiButton;
    private Button pengButton;
    private Button gangButton;
    private Button huButton;

    public GameBoardScreen(OpenGameBoardPacket snapshot) {
        super(Component.literal("龙龙四合一棋桌"));
        this.snapshot = snapshot;
        this.selectedGame = Math.max(0, Math.min(3, snapshot.selectedGame()));
        this.status = snapshot.message();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(1, width - 8));
        panelHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(1, height - 8));
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - panelHeight) / 2);
        int sidebarX = sidebarX();
        int tabY = top + 16;
        addRenderableWidget(Button.builder(Component.literal("五子棋"), button -> selectGame(0))
            .bounds(left + 18, tabY, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("中国象棋"), button -> selectGame(1))
            .bounds(left + 110, tabY, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("围棋"), button -> selectGame(2))
            .bounds(left + 202, tabY, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("麻将"), button -> selectGame(3))
            .bounds(left + 294, tabY, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("重新开局"), button -> send(GameBoardActionPacket.START, 0, 0, 0, 0))
            .bounds(sidebarX, top + 58, 112, 22).build());
        chiButton = addRenderableWidget(Button.builder(Component.literal("吃"), button ->
                send(GameBoardActionPacket.CHI, 0, 0, 0, 0))
            .bounds(sidebarX, top + 86, 54, 22).build());
        pengButton = addRenderableWidget(Button.builder(Component.literal("碰"), button ->
                send(GameBoardActionPacket.PENG, 0, 0, 0, 0))
            .bounds(sidebarX + 58, top + 86, 54, 22).build());
        gangButton = addRenderableWidget(Button.builder(Component.literal("杠"), button ->
                send(GameBoardActionPacket.GANG, 0, 0, 0, 0))
            .bounds(sidebarX, top + 112, 54, 22).build());
        huButton = addRenderableWidget(Button.builder(Component.literal("胡"), button ->
                send(GameBoardActionPacket.HU, 0, 0, 0, 0))
            .bounds(sidebarX + 58, top + 112, 54, 22).build());
        passButton = addRenderableWidget(Button.builder(Component.literal("过 / 停一手"), button ->
                send(GameBoardActionPacket.PASS, 0, 0, 0, 0))
            .bounds(sidebarX, top + 138, 112, 22).build());
        addRenderableWidget(Button.builder(Component.literal("关闭棋盘"), button -> onClose())
            .bounds(sidebarX, top + panelHeight - 28, 112, 22).build());
        updateButtons();
    }

    private void selectGame(int game) {
        selectedGame = game;
        selectedX = selectedY = -1;
        status = switch (game) {
            case 1 -> "主人执红：先点棋子，再点目标位置";
            case 2 -> "主人执黑：点击交叉点落子，支持提子、禁自杀与劫争";
            case 3 -> "主人点击手牌打出；可用时选择吃、碰、杠、胡或过";
            default -> "主人执黑：点击交叉点落子，先连成五子获胜";
        };
        updateButtons();
    }

    private void updateButtons() {
        int claims = snapshot.mahjongAvailableClaims();
        if (passButton != null) {
            passButton.visible = selectedGame == 2 || selectedGame == 3 && snapshot.mahjongPendingTile() >= 0;
            passButton.setMessage(Component.literal(selectedGame == 2 ? "停一手" : "过"));
        }
        if (chiButton != null) chiButton.visible = selectedGame == 3 && (claims & MahjongGame.CLAIM_CHI) != 0;
        if (pengButton != null) pengButton.visible = selectedGame == 3 && (claims & MahjongGame.CLAIM_PENG) != 0;
        if (gangButton != null) gangButton.visible = selectedGame == 3 && (claims & MahjongGame.CLAIM_GANG) != 0;
        if (huButton != null) huButton.visible = selectedGame == 3 && (claims & MahjongGame.CLAIM_HU) != 0;
    }

    public boolean isSameBoard(BlockPos pos) { return snapshot.pos().equals(pos); }

    public void applySnapshot(OpenGameBoardPacket update) {
        snapshot = update;
        selectedGame = Math.max(0, Math.min(3, update.selectedGame()));
        selectedX = selectedY = -1;
        status = update.message();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(left, top, left + panelWidth, top + panelHeight, 0xF0182438, 0xF009101C);
        graphics.renderOutline(left, top, panelWidth, panelHeight, 0xFF65D8C2);
        graphics.drawCenteredString(font, "龙龙四合一棋桌 · 不占用聊天框", left + panelWidth / 2, top + 4, 0xFFF2F7FF);
        drawBoard(graphics);
        drawSidebar(graphics);
        int statusWidth = Math.max(80, sidebarX() - left - 28);
        graphics.drawString(font, font.plainSubstrByWidth(status, statusWidth),
            left + 18, top + panelHeight - 13, 0xFFA9D9FF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBoard(GuiGraphics graphics) {
        if (selectedGame == 3) { drawMahjong(graphics); return; }
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

    private void drawMahjong(GuiGraphics graphics) {
        MahjongHandGeometry hand = mahjongHandGeometry();
        int tableLeft = left + 18, tableRight = sidebarX() - 18;
        int tableTop = top + 48, tableBottom = hand.y - 5;
        graphics.fill(tableLeft, tableTop, tableRight, tableBottom, 0xFF176B55);
        graphics.renderOutline(tableLeft, tableTop, tableRight - tableLeft, tableBottom - tableTop, 0xFF8DE0C9);
        graphics.drawCenteredString(font, "龙龙", (tableLeft + tableRight) / 2, tableTop + 8, 0xFFF5E6B5);
        graphics.drawString(font, "东东", tableLeft + 7, (tableTop + tableBottom) / 2, 0xFFF5E6B5, false);
        graphics.drawString(font, "西西", tableRight - 27, (tableTop + tableBottom) / 2, 0xFFF5E6B5, false);
        graphics.drawCenteredString(font, "牌墙 " + snapshot.mahjongWallRemaining(),
            (tableLeft + tableRight) / 2, tableTop + 25, 0xFFE7F7F0);

        int[] discards = snapshot.mahjongDiscards();
        int shown = Math.min(8, discards.length);
        int discardX = (tableLeft + tableRight) / 2 - shown * 9;
        for (int i = 0; i < shown; i++) {
            int tile = discards[discards.length - shown + i];
            drawMahjongTile(graphics, discardX + i * 18, tableTop + 45, 17, 23, tile, false);
        }
        if (snapshot.mahjongPendingTile() >= 0)
            graphics.drawCenteredString(font, "待响应 " + MahjongGame.tileName(snapshot.mahjongPendingTile()),
                (tableLeft + tableRight) / 2, tableTop + 72, 0xFFFFD778);

        int[] ownerHand = snapshot.mahjongHand();
        for (int i = 0; i < ownerHand.length; i++)
            drawMahjongTile(graphics, hand.x + i * hand.tileWidth, hand.y,
                hand.tileWidth - 1, hand.tileHeight, ownerHand[i], true);
    }

    private void drawMahjongTile(GuiGraphics graphics, int x, int y, int width, int height, int tile, boolean hand) {
        graphics.fill(x, y, x + width, y + height, hand ? 0xFFF5F0DA : 0xFFE9E0C2);
        graphics.renderOutline(x, y, width, height, 0xFF554A39);
        String label = shortTileName(tile);
        int color = tile < 9 ? 0xFFC0392B : tile < 18 ? 0xFF2E67B1 : tile < 27 ? 0xFF268B57 : 0xFFB32935;
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2, color);
    }

    private static String shortTileName(int tile) {
        if (tile >= 0 && tile < 9) return (tile + 1) + "万";
        if (tile < 18) return (tile - 8) + "筒";
        if (tile < 27) return (tile - 17) + "条";
        return MahjongGame.tileName(tile);
    }

    private void drawSidebar(GuiGraphics graphics) {
        int x = sidebarX();
        String name = selectedGame == 0 ? "五子棋" : selectedGame == 1 ? "中国象棋"
            : selectedGame == 2 ? "13×13 围棋" : "四人麻将";
        graphics.drawString(font, name, x, top + 124, 0xFFFFD27A, false);
        if (selectedGame == 0) {
            graphics.drawString(font, "主人胜 " + snapshot.gomokuWins(), x, top + 144, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.gomokuLosses(), x, top + 158, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.gomokuActive() ? "进行中" : "等待开局", x, top + 178, 0xFF86E8D5, false);
        } else if (selectedGame == 1) {
            graphics.drawString(font, "主人胜 " + snapshot.xiangqiOwnerWins(), x, top + 144, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.xiangqiLonglongWins(), x, top + 158, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.xiangqiActive() ? "主人执红" : "等待开局", x, top + 178, 0xFF86E8D5, false);
        } else if (selectedGame == 2) {
            graphics.drawString(font, "主人胜 " + snapshot.goOwnerWins(), x, top + 144, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.goLonglongWins(), x, top + 158, 0xFFF1F1F1, false);
            graphics.drawString(font, "提子 " + snapshot.goOwnerCaptures() + ":" + snapshot.goLonglongCaptures(),
                x, top + 174, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.goActive() ? "主人执黑" : "等待开局", x, top + 194, 0xFF86E8D5, false);
        } else {
            graphics.drawString(font, "主人胜 " + snapshot.mahjongOwnerWins(), x, top + 166, 0xFFF1F1F1, false);
            graphics.drawString(font, "龙龙胜 " + snapshot.mahjongLonglongWins(), x, top + 180, 0xFFF1F1F1, false);
            graphics.drawString(font, "其他AI " + snapshot.mahjongOtherAiWins(), x, top + 194, 0xFFF1F1F1, false);
            graphics.drawString(font, "副露 " + snapshot.mahjongOwnerMelds() + "组", x, top + 208, 0xFFF1F1F1, false);
            graphics.drawString(font, snapshot.mahjongActive() ? "牌局进行中" : "等待开局", x, top + 222, 0xFF86E8D5, false);
        }
        if (panelHeight >= 290 && selectedGame != 3) {
            graphics.drawWordWrap(font, Component.literal(selectedGame == 1
                ? "点选红方棋子后再点目标格。"
                : selectedGame == 2 ? "按中国规则数子，龙龙执白并贴6.5目。"
                : selectedGame == 3 ? "支持吃碰明杠暗杠、胡、自摸和过。" : "横、竖或斜线先连成五子。"),
                x, top + 218, 112, 0xFFB7C8DD);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (selectedGame == 3) {
                MahjongHandGeometry hand = mahjongHandGeometry();
                int count = snapshot.mahjongHand().length;
                if (mouseY >= hand.y && mouseY <= hand.y + hand.tileHeight && mouseX >= hand.x
                    && mouseX < hand.x + count * hand.tileWidth) {
                    int index = Math.min(count - 1, (int) ((mouseX - hand.x) / hand.tileWidth));
                    if (index >= 0) send(GameBoardActionPacket.MOVE, 0, 0, index, 0);
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
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
        int rows = selectedGame == 1 ? 10 : selectedGame == 2 ? 13 : 9;
        int columns = selectedGame == 1 ? 9 : selectedGame == 2 ? 13 : 9;
        int maximum = selectedGame == 1 ? 22 : selectedGame == 2 ? 18 : 27;
        int minimum = selectedGame == 1 ? 10 : selectedGame == 2 ? 8 : 11;
        int spacing = Math.max(minimum, Math.min(maximum, (panelHeight - 92) / (rows - 1)));
        int boardAreaCenter = (left + 18 + sidebarX() - 18) / 2;
        int x = boardAreaCenter - (columns - 1) * spacing / 2;
        return new Geometry(x, top + 62, spacing, columns, rows);
    }

    private MahjongHandGeometry mahjongHandGeometry() {
        int count = Math.max(1, snapshot.mahjongHand().length);
        int availableWidth = Math.max(120, sidebarX() - left - 36);
        int tileWidth = Math.max(12, Math.min(19, availableWidth / count));
        int totalWidth = tileWidth * count;
        int x = left + 18 + Math.max(0, (availableWidth - totalWidth) / 2);
        return new MahjongHandGeometry(x, top + panelHeight - 48, tileWidth, 30);
    }

    private int sidebarX() { return left + panelWidth - 130; }

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
    private record MahjongHandGeometry(int x, int y, int tileWidth, int tileHeight) {}
}
