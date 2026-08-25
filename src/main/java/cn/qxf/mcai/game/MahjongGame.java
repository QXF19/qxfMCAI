package cn.qxf.mcai.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** 136张四人麻将：支持吃、碰、明杠、暗杠、胡、自摸、过与牌局存档。 */
public final class MahjongGame {
    public static final int CLAIM_CHI = 1;
    public static final int CLAIM_PENG = 2;
    public static final int CLAIM_GANG = 4;
    public static final int CLAIM_HU = 8;
    private static final int TILE_TYPES = 34;

    private final List<List<Integer>> hands = List.of(
        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    private final List<Integer> wall = new ArrayList<>();
    private final List<Integer> discards = new ArrayList<>();
    private boolean active;
    private boolean ownerMustDiscard;
    private int ownerMelds;
    private int pendingTile = -1;
    private int pendingFrom = -1;
    private int resumeSeat = 1;
    private int availableClaims;
    private int ownerWins;
    private int longlongWins;
    private int otherAiWins;

    public Result start(long seed) {
        for (List<Integer> hand : hands) hand.clear();
        wall.clear(); discards.clear();
        for (int tile = 0; tile < TILE_TYPES; tile++) for (int copy = 0; copy < 4; copy++) wall.add(tile);
        Collections.shuffle(wall, new Random(seed));
        for (int round = 0; round < 13; round++) for (int seat = 0; seat < 4; seat++) draw(seat);
        draw(0);
        sortHands();
        active = true; ownerMustDiscard = true; ownerMelds = 0;
        clearPending();
        if (isOwnerWinning(-1)) return finish(0, "起手天胡");
        updateSelfClaims();
        return result(true, false, "麻将开局：主人可打牌，也可选择暗杠或自摸");
    }

    /** 主人选择手牌打出；AI自动轮转，遇到可吃碰杠胡的牌时暂停等待主人。 */
    public Result play(int handIndex, long seed) {
        if (!active) start(seed);
        if (pendingTile >= 0) return result(false, false, "请先选择吃、碰、杠、胡或过");
        if (!ownerMustDiscard) return result(false, false, "当前还没有轮到主人打牌");
        List<Integer> owner = hands.get(0);
        if (handIndex < 0 || handIndex >= owner.size()) return result(false, false, "请选择主人手牌中的一张牌");
        int tile = owner.remove(handIndex);
        discards.add(tile);
        ownerMustDiscard = false;
        availableClaims = 0;
        for (int seat = 1; seat < 4; seat++) {
            if (isWinningWithMelds(withExtra(hands.get(seat), tile), 0))
                return finish(seat, seat == 1 ? "龙龙荣和主人的" + tileName(tile) : "AI玩家荣和主人的" + tileName(tile));
        }
        return runAiTurns(1, seed, "主人打出" + tileName(tile));
    }

    public Result claim(int claim, long seed) {
        if (!active) return result(false, false, "当前没有进行中的麻将牌局");
        if (claim == CLAIM_HU && pendingTile < 0 && (availableClaims & CLAIM_HU) != 0)
            return finish(0, "主人自摸");
        if (claim == CLAIM_GANG && pendingTile < 0 && (availableClaims & CLAIM_GANG) != 0)
            return concealedGang();
        if (pendingTile < 0 || (availableClaims & claim) == 0)
            return result(false, false, "当前不能执行这个麻将动作");
        if (claim == CLAIM_HU) return finish(0, "主人胡了" + tileName(pendingTile));

        int claimed = pendingTile;
        if (!discards.isEmpty()) discards.remove(discards.size() - 1);
        if (claim == CLAIM_PENG) {
            removeCopies(hands.get(0), claimed, 2); ownerMelds++;
            clearPending(); ownerMustDiscard = true; sortHands(); updateGangOnlyClaims();
            return result(true, false, "主人碰" + tileName(claimed) + "，请选择一张牌打出");
        }
        if (claim == CLAIM_GANG) {
            removeCopies(hands.get(0), claimed, 3); ownerMelds++;
            clearPending();
            if (!draw(0)) return drawGame("牌墙摸完，本局流局");
            sortHands();
            if (isOwnerWinning(-1)) return finish(0, "主人杠上开花");
            ownerMustDiscard = true; updateSelfClaims();
            return result(true, false, "主人明杠" + tileName(claimed) + "并补牌，请打出一张牌");
        }
        if (claim == CLAIM_CHI) {
            int[] sequence = chooseChi(hands.get(0), claimed);
            if (sequence == null) return result(false, false, "没有可组成的顺子");
            for (int tile : sequence) if (tile != claimed) removeCopies(hands.get(0), tile, 1);
            ownerMelds++;
            clearPending(); ownerMustDiscard = true; sortHands(); updateGangOnlyClaims();
            return result(true, false, "主人吃" + tileName(claimed) + "，请选择一张牌打出");
        }
        return result(false, false, "未知麻将动作");
    }

    public Result pass(long seed) {
        if (!active || pendingTile < 0) return result(false, false, "当前没有需要跳过的响应");
        int from = pendingFrom, next = resumeSeat;
        clearPending();
        return next <= 3 ? runAiTurns(next, seed, "主人跳过了对" + seatName(from) + "舍牌的响应")
            : drawForOwner("主人选择过");
    }

    private Result concealedGang() {
        int tile = fourOfAKind(hands.get(0));
        if (tile < 0) return result(false, false, "手中没有可暗杠的四张同牌");
        removeCopies(hands.get(0), tile, 4); ownerMelds++;
        if (!draw(0)) return drawGame("牌墙摸完，本局流局");
        sortHands();
        if (isOwnerWinning(-1)) return finish(0, "主人暗杠后杠上开花");
        ownerMustDiscard = true; updateSelfClaims();
        return result(true, false, "主人暗杠" + tileName(tile) + "并补牌，请打出一张牌");
    }

    private Result runAiTurns(int startSeat, long seed, String prefix) {
        Random random = new Random(seed);
        String latest = prefix;
        for (int seat = startSeat; seat < 4; seat++) {
            if (!draw(seat)) return drawGame("牌墙摸完，本局流局");
            if (isWinningWithMelds(toArray(hands.get(seat)), 0))
                return finish(seat, seat == 1 ? "龙龙自摸" : "AI玩家自摸");
            List<Integer> hand = hands.get(seat);
            int discard = hand.remove(chooseDiscard(hand, random));
            discards.add(discard);
            latest = seatName(seat) + "打出" + tileName(discard);
            int claims = claimsForOwner(discard, seat);
            if (claims != 0) {
                pendingTile = discard; pendingFrom = seat; resumeSeat = seat + 1; availableClaims = claims;
                return result(true, false, latest + "，主人可以" + claimNames(claims));
            }
        }
        return drawForOwner(latest);
    }

    private Result drawForOwner(String prefix) {
        if (!draw(0)) return drawGame("牌墙摸完，本局流局");
        hands.get(0).sort(Comparator.naturalOrder());
        ownerMustDiscard = true;
        if (isOwnerWinning(-1)) {
            availableClaims = CLAIM_HU | (fourOfAKind(hands.get(0)) >= 0 ? CLAIM_GANG : 0);
            return result(true, false, prefix + "；主人已自摸，可点击胡牌");
        }
        updateSelfClaims();
        return result(true, false, prefix + "；轮到主人选择一张牌打出");
    }

    private int claimsForOwner(int tile, int fromSeat) {
        int claims = 0;
        if (isOwnerWinning(tile)) claims |= CLAIM_HU;
        int copies = Collections.frequency(hands.get(0), tile);
        if (copies >= 2) claims |= CLAIM_PENG;
        if (copies >= 3) claims |= CLAIM_GANG;
        if (fromSeat == 3 && chooseChi(hands.get(0), tile) != null) claims |= CLAIM_CHI;
        return claims;
    }

    private void updateSelfClaims() {
        availableClaims = 0;
        if (isOwnerWinning(-1)) availableClaims |= CLAIM_HU;
        if (fourOfAKind(hands.get(0)) >= 0) availableClaims |= CLAIM_GANG;
    }
    private void updateGangOnlyClaims() {
        availableClaims = fourOfAKind(hands.get(0)) >= 0 ? CLAIM_GANG : 0;
    }
    private boolean isOwnerWinning(int extra) {
        return isWinningWithMelds(extra < 0 ? toArray(hands.get(0)) : withExtra(hands.get(0), extra), ownerMelds);
    }

    public static boolean isWinning(int[] tiles) { return isWinningWithMelds(tiles, 0); }
    public static boolean isWinningWithMelds(int[] tiles, int openMelds) {
        if (tiles == null || openMelds < 0 || openMelds > 4 || tiles.length != 14 - openMelds * 3) return false;
        int[] counts = new int[TILE_TYPES];
        for (int tile : tiles) if (tile < 0 || tile >= TILE_TYPES || ++counts[tile] > 4) return false;
        if (openMelds == 0 && (isSevenPairs(counts) || isThirteenOrphans(counts))) return true;
        for (int pair = 0; pair < TILE_TYPES; pair++) {
            if (counts[pair] < 2) continue;
            counts[pair] -= 2;
            if (allMelds(counts)) { counts[pair] += 2; return true; }
            counts[pair] += 2;
        }
        return false;
    }
    private static boolean isSevenPairs(int[] counts) {
        int pairs = 0;
        for (int count : counts) {
            if ((count & 1) != 0) return false;
            pairs += count / 2;
        }
        return pairs == 7;
    }
    private static boolean isThirteenOrphans(int[] counts) {
        int[] required = {0, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33};
        boolean pair = false;
        for (int tile : required) {
            if (counts[tile] == 0) return false;
            if (counts[tile] >= 2) pair = true;
        }
        for (int tile = 0; tile < 27; tile++) if (tile % 9 != 0 && tile % 9 != 8 && counts[tile] > 0) return false;
        return pair;
    }
    private static boolean allMelds(int[] counts) {
        int first = -1;
        for (int i = 0; i < counts.length; i++) if (counts[i] > 0) { first = i; break; }
        if (first < 0) return true;
        if (counts[first] >= 3) {
            counts[first] -= 3;
            if (allMelds(counts)) { counts[first] += 3; return true; }
            counts[first] += 3;
        }
        if (first < 27 && first % 9 <= 6 && counts[first + 1] > 0 && counts[first + 2] > 0) {
            counts[first]--; counts[first + 1]--; counts[first + 2]--;
            if (allMelds(counts)) { counts[first]++; counts[first + 1]++; counts[first + 2]++; return true; }
            counts[first]++; counts[first + 1]++; counts[first + 2]++;
        }
        return false;
    }

    public static boolean canChi(int[] hand, int tile) { return chooseChi(toList(hand), tile) != null; }
    public static boolean canPeng(int[] hand, int tile) { return Collections.frequency(toList(hand), tile) >= 2; }
    public static boolean canGang(int[] hand, int tile) { return Collections.frequency(toList(hand), tile) >= 3; }
    private static int[] chooseChi(List<Integer> hand, int tile) {
        if (tile < 0 || tile >= 27) return null;
        int suitStart = tile / 9 * 9;
        int[][] candidates = {{tile - 2, tile - 1, tile}, {tile - 1, tile, tile + 1}, {tile, tile + 1, tile + 2}};
        for (int[] sequence : candidates) {
            if (sequence[0] < suitStart || sequence[2] >= suitStart + 9) continue;
            boolean valid = true;
            for (int value : sequence) if (value != tile && !hand.contains(value)) { valid = false; break; }
            if (valid) return sequence;
        }
        return null;
    }

    private boolean draw(int seat) { if (wall.isEmpty()) return false; hands.get(seat).add(wall.remove(wall.size() - 1)); return true; }
    private static int chooseDiscard(List<Integer> hand, Random random) {
        int bestIndex = 0, weakest = Integer.MAX_VALUE;
        for (int i = 0; i < hand.size(); i++) {
            int tile = hand.get(i), support = Collections.frequency(hand, tile) * 6;
            if (tile < 27) {
                int rank = tile % 9;
                if (rank > 0 && hand.contains(tile - 1)) support += 3;
                if (rank < 8 && hand.contains(tile + 1)) support += 3;
                if (rank > 1 && hand.contains(tile - 2)) support++;
                if (rank < 7 && hand.contains(tile + 2)) support++;
            }
            support += random.nextInt(2);
            if (support < weakest) { weakest = support; bestIndex = i; }
        }
        return bestIndex;
    }

    private static int fourOfAKind(List<Integer> hand) {
        for (int tile = 0; tile < TILE_TYPES; tile++) if (Collections.frequency(hand, tile) == 4) return tile;
        return -1;
    }
    private static void removeCopies(List<Integer> hand, int tile, int count) {
        for (int i = 0; i < count; i++) if (!hand.remove(Integer.valueOf(tile))) throw new IllegalStateException("牌局状态损坏");
    }
    private static int[] withExtra(List<Integer> hand, int tile) {
        int[] result = new int[hand.size() + 1];
        for (int i = 0; i < hand.size(); i++) result[i] = hand.get(i);
        result[result.length - 1] = tile;
        return result;
    }
    private static List<Integer> toList(int[] values) { List<Integer> result = new ArrayList<>(); for (int value : values) result.add(value); return result; }
    private static int[] toArray(List<Integer> values) { return values.stream().mapToInt(Integer::intValue).toArray(); }
    private void sortHands() { for (List<Integer> hand : hands) hand.sort(Comparator.naturalOrder()); }
    private void clearPending() { pendingTile = -1; pendingFrom = -1; resumeSeat = 1; availableClaims = 0; }

    private Result finish(int seat, String reason) {
        active = false; ownerMustDiscard = false; clearPending();
        if (seat == 0) ownerWins++; else if (seat == 1) longlongWins++; else otherAiWins++;
        return result(true, true, reason + "，" + (seat == 0 ? "主人胡牌" : seat == 1 ? "龙龙胡牌" : "AI玩家胡牌"));
    }
    private Result drawGame(String reason) { active = false; ownerMustDiscard = false; clearPending(); return result(true, true, reason); }

    public static String tileName(int tile) {
        if (tile >= 0 && tile < 9) return (tile + 1) + "万";
        if (tile < 18) return (tile - 8) + "筒";
        if (tile < 27) return (tile - 17) + "条";
        return switch (tile) { case 27 -> "东"; case 28 -> "南"; case 29 -> "西"; case 30 -> "北";
            case 31 -> "中"; case 32 -> "发"; case 33 -> "白"; default -> "?"; };
    }
    private static String seatName(int seat) { return seat == 1 ? "龙龙" : seat == 2 ? "东东" : seat == 3 ? "西西" : "主人"; }
    private static String claimNames(int claims) {
        List<String> names = new ArrayList<>();
        if ((claims & CLAIM_CHI) != 0) names.add("吃"); if ((claims & CLAIM_PENG) != 0) names.add("碰");
        if ((claims & CLAIM_GANG) != 0) names.add("杠"); if ((claims & CLAIM_HU) != 0) names.add("胡"); names.add("过");
        return String.join("/", names);
    }

    public String save() {
        return (active ? 1 : 0) + ";" + (ownerMustDiscard ? 1 : 0) + ";" + ownerMelds + ";" + pendingTile + ";"
            + pendingFrom + ";" + resumeSeat + ";" + availableClaims + ";" + ownerWins + ";" + longlongWins + ";"
            + otherAiWins + ";" + encode(hands.get(0)) + ";" + encode(hands.get(1)) + ";" + encode(hands.get(2)) + ";"
            + encode(hands.get(3)) + ";" + encode(wall) + ";" + encode(discards);
    }
    public void load(String saved) {
        if (saved == null || saved.isBlank()) return;
        try {
            String[] p = saved.split(";", -1); if (p.length != 16) return;
            active = "1".equals(p[0]); ownerMustDiscard = "1".equals(p[1]); ownerMelds = bounded(p[2], 0, 4);
            pendingTile = bounded(p[3], -1, 33); pendingFrom = bounded(p[4], -1, 3); resumeSeat = bounded(p[5], 1, 4);
            availableClaims = bounded(p[6], 0, 15); ownerWins = nonNegative(p[7]); longlongWins = nonNegative(p[8]); otherAiWins = nonNegative(p[9]);
            for (int seat = 0; seat < 4; seat++) { hands.get(seat).clear(); hands.get(seat).addAll(decode(p[10 + seat])); }
            wall.clear(); wall.addAll(decode(p[14])); discards.clear(); discards.addAll(decode(p[15]));
            if (!validState()) { active = false; ownerMustDiscard = false; clearPending(); for (List<Integer> h : hands) h.clear(); wall.clear(); discards.clear(); }
            sortHands();
        } catch (RuntimeException ignored) { active = false; ownerMustDiscard = false; clearPending(); }
    }
    private boolean validState() {
        int[] counts = new int[TILE_TYPES];
        for (List<Integer> values : List.of(hands.get(0), hands.get(1), hands.get(2), hands.get(3), wall, discards))
            for (int tile : values) if (tile < 0 || tile >= TILE_TYPES || ++counts[tile] > 4) return false;
        return ownerMelds * 3 <= 12 && (!active || !hands.get(0).isEmpty());
    }
    private static String encode(List<Integer> values) { return String.join(",", values.stream().map(String::valueOf).toList()); }
    private static List<Integer> decode(String text) { List<Integer> r = new ArrayList<>(); if (!text.isBlank()) for (String v : text.split(",")) r.add(Integer.parseInt(v)); return r; }
    private static int nonNegative(String value) { return Math.max(0, Integer.parseInt(value)); }
    private static int bounded(String value, int min, int max) { return Math.max(min, Math.min(max, Integer.parseInt(value))); }

    public int[] ownerHand() { return toArray(hands.get(0)); }
    public int[] discards() { return toArray(discards); }
    public boolean isActive() { return active; }
    public boolean ownerMustDiscard() { return ownerMustDiscard; }
    public int ownerMelds() { return ownerMelds; }
    public int pendingTile() { return pendingTile; }
    public int availableClaims() { return availableClaims; }
    public int ownerWins() { return ownerWins; }
    public int longlongWins() { return longlongWins; }
    public int otherAiWins() { return otherAiWins; }
    public int wallRemaining() { return wall.size(); }
    private Result result(boolean accepted, boolean gameOver, String message) {
        return new Result(accepted, gameOver, message, ownerHand(), discards(), wallRemaining(), availableClaims, ownerMelds, pendingTile);
    }
    public record Result(boolean accepted, boolean gameOver, String message, int[] ownerHand, int[] discards,
                         int wallRemaining, int availableClaims, int ownerMelds, int pendingTile) {}
}
