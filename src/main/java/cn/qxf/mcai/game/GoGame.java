package cn.qxf.mcai.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** 13×13 轻量围棋：主人执黑，龙龙执白；仅在点击棋盘时计算。 */
public final class GoGame {
    public static final int SIZE = 13;
    private final byte[] board = new byte[SIZE * SIZE];
    private boolean active;
    private int ownerWins;
    private int longlongWins;
    private int ownerCaptures;
    private int longlongCaptures;
    private int consecutivePasses;
    private String koForbidden = "";

    public void start() {
        Arrays.fill(board, (byte) 0);
        active = true;
        ownerCaptures = 0;
        longlongCaptures = 0;
        consecutivePasses = 0;
        koForbidden = "";
    }

    public Result play(int x, int y, long seed) {
        if (!active) start();
        if (!inside(x, y)) return result(false, false, "坐标范围是0到12");
        String beforeOwner = boardKey(board);
        MoveResult ownerMove = simulate(board, x, y, (byte) 1, koForbidden);
        if (!ownerMove.legal) return result(false, false, ownerMove.reason);
        copy(ownerMove.board, board);
        ownerCaptures += ownerMove.captured;
        consecutivePasses = 0;
        String beforeLonglong = boardKey(board);

        MoveChoice reply = chooseMove((byte) 2, beforeOwner, seed);
        if (reply == null) {
            consecutivePasses++;
            if (consecutivePasses >= 2 || isFull()) return finish("龙龙无合法落子，双方数子");
            koForbidden = beforeLonglong;
            return result(true, false, "主人落子成功，龙龙选择停一手");
        }
        copy(reply.result.board, board);
        longlongCaptures += reply.result.captured;
        koForbidden = beforeLonglong;
        return result(true, false, "龙龙落在 " + reply.x + "," + reply.y + "，轮到主人");
    }

    public Result pass(long seed) {
        if (!active) start();
        consecutivePasses++;
        int occupied = 0;
        for (byte point : board) if (point != 0) occupied++;
        if (consecutivePasses >= 2) return finish("双方连续停一手，开始数子");
        String beforeLonglong = boardKey(board);
        MoveChoice reply = chooseMove((byte) 2, "", seed);
        if (reply == null || occupied > board.length * 4 / 5) {
            consecutivePasses++;
            return finish("主人与龙龙停一手，开始数子");
        }
        copy(reply.result.board, board);
        longlongCaptures += reply.result.captured;
        koForbidden = beforeLonglong;
        consecutivePasses = 0;
        return result(true, false, "主人停一手；龙龙落在 " + reply.x + "," + reply.y);
    }

    private MoveChoice chooseMove(byte side, String forbidden, long seed) {
        List<MoveChoice> legal = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE;
        Random random = new Random(seed);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            if (board[index(x, y)] != 0) continue;
            MoveResult result = simulate(board, x, y, side, forbidden);
            if (!result.legal) continue;
            int centerDistance = Math.abs(x - SIZE / 2) + Math.abs(y - SIZE / 2);
            int score = result.captured * 1_000 - centerDistance * 4 + random.nextInt(7);
            if (score > bestScore) { legal.clear(); bestScore = score; }
            if (score == bestScore) legal.add(new MoveChoice(x, y, result));
        }
        return legal.isEmpty() ? null : legal.get(random.nextInt(legal.size()));
    }

    private static MoveResult simulate(byte[] source, int x, int y, byte side, String forbidden) {
        if (!inside(x, y) || source[index(x, y)] != 0) return MoveResult.illegal("这个交叉点已有棋子");
        byte[] next = source.clone();
        next[index(x, y)] = side;
        byte opponent = side == 1 ? (byte) 2 : (byte) 1;
        int captured = 0;
        boolean[] checked = new boolean[next.length];
        for (int neighbor : neighbors(x, y)) {
            if (next[neighbor] != opponent || checked[neighbor]) continue;
            Group group = group(next, neighbor);
            for (int point : group.points) checked[point] = true;
            if (group.liberties == 0) {
                captured += group.points.size();
                for (int point : group.points) next[point] = 0;
            }
        }
        if (group(next, index(x, y)).liberties == 0) return MoveResult.illegal("此处为自杀禁手");
        if (!forbidden.isEmpty() && forbidden.equals(boardKey(next))) return MoveResult.illegal("此处违反劫争规则");
        return new MoveResult(true, "", next, captured);
    }

    private static Group group(byte[] state, int start) {
        byte side = state[start];
        boolean[] visited = new boolean[state.length];
        boolean[] libertySeen = new boolean[state.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        List<Integer> points = new ArrayList<>();
        queue.add(start); visited[start] = true;
        int liberties = 0;
        while (!queue.isEmpty()) {
            int point = queue.removeFirst();
            points.add(point);
            int x = point % SIZE, y = point / SIZE;
            for (int neighbor : neighbors(x, y)) {
                if (state[neighbor] == 0 && !libertySeen[neighbor]) {
                    libertySeen[neighbor] = true; liberties++;
                } else if (state[neighbor] == side && !visited[neighbor]) {
                    visited[neighbor] = true; queue.addLast(neighbor);
                }
            }
        }
        return new Group(points, liberties);
    }

    private Result finish(String reason) {
        Score score = score();
        active = false;
        if (score.owner > score.longlong) ownerWins++; else longlongWins++;
        String winner = score.owner > score.longlong ? "主人胜" : "龙龙胜";
        return result(true, true, reason + "：主人 " + score.owner + "，龙龙 " + score.longlong + "（贴目6.5），" + winner);
    }

    private Score score() {
        double black = 0, white = 6.5D;
        boolean[] visited = new boolean[board.length];
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 1) { black++; continue; }
            if (board[i] == 2) { white++; continue; }
            if (visited[i]) continue;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(i); visited[i] = true;
            int empty = 0; boolean touchesBlack = false, touchesWhite = false;
            while (!queue.isEmpty()) {
                int point = queue.removeFirst(); empty++;
                for (int neighbor : neighbors(point % SIZE, point / SIZE)) {
                    if (board[neighbor] == 1) touchesBlack = true;
                    else if (board[neighbor] == 2) touchesWhite = true;
                    else if (!visited[neighbor]) { visited[neighbor] = true; queue.addLast(neighbor); }
                }
            }
            if (touchesBlack && !touchesWhite) black += empty;
            if (touchesWhite && !touchesBlack) white += empty;
        }
        return new Score(black, white);
    }

    private boolean isFull() { for (byte point : board) if (point == 0) return false; return true; }
    private static int[] neighbors(int x, int y) {
        int[] raw = {x - 1, y, x + 1, y, x, y - 1, x, y + 1};
        int count = 0;
        for (int i = 0; i < raw.length; i += 2) if (inside(raw[i], raw[i + 1])) count++;
        int[] result = new int[count]; int cursor = 0;
        for (int i = 0; i < raw.length; i += 2) if (inside(raw[i], raw[i + 1])) result[cursor++] = index(raw[i], raw[i + 1]);
        return result;
    }
    private static boolean inside(int x, int y) { return x >= 0 && x < SIZE && y >= 0 && y < SIZE; }
    private static int index(int x, int y) { return y * SIZE + x; }
    private static void copy(byte[] source, byte[] target) { System.arraycopy(source, 0, target, 0, target.length); }
    private static String boardKey(byte[] state) {
        char[] key = new char[state.length];
        for (int i = 0; i < state.length; i++) key[i] = (char) ('0' + state[i]);
        return new String(key);
    }

    public byte[] board() { return board.clone(); }
    public String saveBoard() { return boardKey(board); }
    public void load(String saved, boolean savedActive, int savedOwnerWins, int savedLonglongWins,
                     int savedOwnerCaptures, int savedLonglongCaptures, int savedPasses, String savedKo) {
        Arrays.fill(board, (byte) 0);
        if (saved != null && saved.length() == board.length)
            for (int i = 0; i < board.length; i++) board[i] = (byte) Math.max(0, Math.min(2, saved.charAt(i) - '0'));
        active = savedActive;
        ownerWins = Math.max(0, savedOwnerWins); longlongWins = Math.max(0, savedLonglongWins);
        ownerCaptures = Math.max(0, savedOwnerCaptures); longlongCaptures = Math.max(0, savedLonglongCaptures);
        consecutivePasses = Math.max(0, Math.min(2, savedPasses));
        koForbidden = savedKo != null && savedKo.length() == board.length ? savedKo : "";
    }
    public boolean isActive() { return active; }
    public int ownerWins() { return ownerWins; }
    public int longlongWins() { return longlongWins; }
    public int ownerCaptures() { return ownerCaptures; }
    public int longlongCaptures() { return longlongCaptures; }
    public int consecutivePasses() { return consecutivePasses; }
    public String koForbidden() { return koForbidden; }
    private Result result(boolean accepted, boolean gameOver, String message) {
        return new Result(accepted, gameOver, message, board());
    }

    private record Group(List<Integer> points, int liberties) {}
    private record MoveResult(boolean legal, String reason, byte[] board, int captured) {
        private static MoveResult illegal(String reason) { return new MoveResult(false, reason, new byte[0], 0); }
    }
    private record MoveChoice(int x, int y, MoveResult result) {}
    private record Score(double owner, double longlong) {}
    public record Result(boolean accepted, boolean gameOver, String message, byte[] board) {}
}
