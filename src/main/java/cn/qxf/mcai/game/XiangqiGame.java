package cn.qxf.mcai.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 轻量中国象棋：主人执红，龙龙执黑；不创建实体、不做逐 tick 计算。 */
public final class XiangqiGame {
    private static final int WIDTH = 9;
    private static final int HEIGHT = 10;
    private final char[] board = new char[WIDTH * HEIGHT];
    private boolean active;
    private int ownerWins;
    private int longlongWins;

    public XiangqiGame() {
        start();
        active = false;
    }

    public void start() {
        java.util.Arrays.fill(board, '.');
        placeRow(0, "rnbakabnr");
        set(1, 2, 'c'); set(7, 2, 'c');
        for (int x = 0; x < WIDTH; x += 2) set(x, 3, 'p');
        for (int x = 0; x < WIDTH; x += 2) set(x, 6, 'P');
        set(1, 7, 'C'); set(7, 7, 'C');
        placeRow(9, "RNBAKABNR");
        active = true;
    }

    public Result play(int fromX, int fromY, int toX, int toY, long seed) {
        if (!active) start();
        if (!inside(fromX, fromY) || !inside(toX, toY))
            return result(false, false, "坐标范围：x 为0到8，y 为0到9");
        char piece = get(fromX, fromY);
        if (!isRed(piece)) return result(false, false, "主人需要移动红方棋子");
        if (!legalMove(fromX, fromY, toX, toY, true)) return result(false, false, "这一步不符合该棋子的走法");
        move(fromX, fromY, toX, toY);
        if (!contains('k')) {
            active = false;
            ownerWins++;
            return result(true, true, "主人将军！这局主人赢了");
        }

        List<Move> choices = legalMoves(false);
        if (choices.isEmpty()) {
            active = false;
            ownerWins++;
            return result(true, true, "龙龙无棋可走，主人获胜");
        }
        int bestCapture = choices.stream().mapToInt(choice -> pieceValue(get(choice.tx, choice.ty))).max().orElse(0);
        List<Move> best = choices.stream()
            .filter(choice -> pieceValue(get(choice.tx, choice.ty)) == bestCapture).toList();
        Move reply = best.get(new Random(seed).nextInt(best.size()));
        char captured = get(reply.tx, reply.ty);
        move(reply.fx, reply.fy, reply.tx, reply.ty);
        if (captured == 'K') {
            active = false;
            longlongWins++;
            return result(true, true, "龙龙从 " + reply.fx + "," + reply.fy + " 走到 " + reply.tx + "," + reply.ty + "，这局龙龙赢了");
        }
        return result(true, false, "龙龙从 " + reply.fx + "," + reply.fy + " 走到 " + reply.tx + "," + reply.ty + "，轮到主人");
    }

    private List<Move> legalMoves(boolean red) {
        List<Move> moves = new ArrayList<>();
        for (int fy = 0; fy < HEIGHT; fy++) for (int fx = 0; fx < WIDTH; fx++) {
            char piece = get(fx, fy);
            if (piece == '.' || isRed(piece) != red) continue;
            for (int ty = 0; ty < HEIGHT; ty++) for (int tx = 0; tx < WIDTH; tx++)
                if (legalMove(fx, fy, tx, ty, red)) moves.add(new Move(fx, fy, tx, ty));
        }
        return moves;
    }

    private boolean legalMove(int fx, int fy, int tx, int ty, boolean red) {
        if (fx == tx && fy == ty) return false;
        char piece = get(fx, fy), target = get(tx, ty);
        if (piece == '.' || isRed(piece) != red || (target != '.' && isRed(target) == red)) return false;
        int dx = tx - fx, dy = ty - fy;
        boolean legal = pseudoLegalMove(piece, fx, fy, tx, ty, red, target);
        if (!legal) return false;
        char oldTarget = target;
        set(tx, ty, piece); set(fx, fy, '.');
        boolean kingSafe = !kingInCheck(red);
        set(fx, fy, piece); set(tx, ty, oldTarget);
        return kingSafe;
    }

    private boolean pseudoLegalMove(char piece, int fx, int fy, int tx, int ty, boolean red, char target) {
        int dx = tx - fx, dy = ty - fy;
        return switch (Character.toLowerCase(piece)) {
            case 'r' -> (dx == 0 || dy == 0) && blockers(fx, fy, tx, ty) == 0;
            case 'c' -> (dx == 0 || dy == 0) && blockers(fx, fy, tx, ty) == (target == '.' ? 0 : 1);
            case 'n' -> horseMove(fx, fy, tx, ty, dx, dy);
            case 'b' -> Math.abs(dx) == 2 && Math.abs(dy) == 2
                && get(fx + dx / 2, fy + dy / 2) == '.' && (red ? ty >= 5 : ty <= 4);
            case 'a' -> Math.abs(dx) == 1 && Math.abs(dy) == 1 && inPalace(tx, ty, red);
            case 'k' -> ((Math.abs(dx) + Math.abs(dy) == 1) && inPalace(tx, ty, red))
                || (tx == fx && Character.toLowerCase(target) == 'k' && blockers(fx, fy, tx, ty) == 0);
            case 'p' -> pawnMove(dx, dy, fy, red);
            default -> false;
        };
    }

    private boolean kingInCheck(boolean red) {
        char king = red ? 'K' : 'k';
        int kingX = -1, kingY = -1;
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++)
            if (get(x, y) == king) { kingX = x; kingY = y; }
        if (kingX < 0) return false;
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            char attacker = get(x, y);
            if (attacker == '.' || isRed(attacker) == red) continue;
            if (pseudoLegalMove(attacker, x, y, kingX, kingY, !red, king)) return true;
        }
        return false;
    }

    private boolean horseMove(int fx, int fy, int tx, int ty, int dx, int dy) {
        if (Math.abs(dx) == 2 && Math.abs(dy) == 1) return get(fx + Integer.signum(dx), fy) == '.';
        if (Math.abs(dx) == 1 && Math.abs(dy) == 2) return get(fx, fy + Integer.signum(dy)) == '.';
        return false;
    }

    private static boolean pawnMove(int dx, int dy, int fromY, boolean red) {
        if (red) return (dx == 0 && dy == -1) || (fromY <= 4 && Math.abs(dx) == 1 && dy == 0);
        return (dx == 0 && dy == 1) || (fromY >= 5 && Math.abs(dx) == 1 && dy == 0);
    }

    private static boolean inPalace(int x, int y, boolean red) {
        return x >= 3 && x <= 5 && (red ? y >= 7 && y <= 9 : y >= 0 && y <= 2);
    }

    private int blockers(int fx, int fy, int tx, int ty) {
        if (fx != tx && fy != ty) return Integer.MAX_VALUE;
        int sx = Integer.signum(tx - fx), sy = Integer.signum(ty - fy), count = 0;
        for (int x = fx + sx, y = fy + sy; x != tx || y != ty; x += sx, y += sy)
            if (get(x, y) != '.') count++;
        return count;
    }

    private void move(int fx, int fy, int tx, int ty) { set(tx, ty, get(fx, fy)); set(fx, fy, '.'); }
    private void placeRow(int y, String row) { for (int x = 0; x < WIDTH; x++) set(x, y, row.charAt(x)); }
    private boolean contains(char piece) { for (char value : board) if (value == piece) return true; return false; }
    private char get(int x, int y) { return board[y * WIDTH + x]; }
    private void set(int x, int y, char piece) { board[y * WIDTH + x] = piece; }
    private static boolean inside(int x, int y) { return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT; }
    private static boolean isRed(char piece) { return Character.isUpperCase(piece); }
    private static int pieceValue(char piece) { return switch (Character.toLowerCase(piece)) {
        case 'k' -> 10_000; case 'r' -> 900; case 'c' -> 450; case 'n' -> 400;
        case 'b', 'a' -> 200; case 'p' -> 100; default -> 0;
    }; }

    public String boardText() {
        StringBuilder text = new StringBuilder("   0 1 2 3 4 5 6 7 8\n");
        for (int y = 0; y < HEIGHT; y++) {
            text.append(y).append(y < 10 ? "  " : " ");
            for (int x = 0; x < WIDTH; x++) text.append(symbol(get(x, y))).append(' ');
            if (y == 4) text.append("  楚河");
            if (y == 5) text.append("  汉界");
            text.append('\n');
        }
        return text.toString().stripTrailing();
    }

    private static char symbol(char piece) { return switch (piece) {
        case 'R' -> '俥'; case 'N' -> '傌'; case 'B' -> '相'; case 'A' -> '仕'; case 'K' -> '帥';
        case 'C' -> '炮'; case 'P' -> '兵'; case 'r' -> '車'; case 'n' -> '馬'; case 'b' -> '象';
        case 'a' -> '士'; case 'k' -> '將'; case 'c' -> '砲'; case 'p' -> '卒'; default -> '·';
    }; }

    public String saveBoard() { return new String(board); }
    public void load(String saved, boolean isActive, int savedOwnerWins, int savedLonglongWins) {
        if (saved != null && saved.length() == board.length) saved.getChars(0, board.length, board, 0);
        else start();
        active = isActive;
        ownerWins = Math.max(0, savedOwnerWins);
        longlongWins = Math.max(0, savedLonglongWins);
    }
    public boolean isActive() { return active; }
    public int ownerWins() { return ownerWins; }
    public int longlongWins() { return longlongWins; }
    private Result result(boolean accepted, boolean gameOver, String message) {
        return new Result(accepted, gameOver, message, boardText());
    }
    private record Move(int fx, int fy, int tx, int ty) {}
    public record Result(boolean accepted, boolean gameOver, String message, String board) {}
}
