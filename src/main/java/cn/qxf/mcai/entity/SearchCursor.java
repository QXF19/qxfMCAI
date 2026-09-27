package cn.qxf.mcai.entity;

/** 从浅层向下逐层扩展的搜索游标；每次 next 恰好前进一个候选，跨帧不重扫前缀。 */
public final class SearchCursor {
    public record Offset(int x, int y, int z) {}
    private final int radius, depth;
    private int layer, x, z;
    public SearchCursor(int radius, int depth, int firstLayer) {
        this.radius = Math.max(1, Math.min(32, radius));
        this.depth = Math.max(1, Math.min(64, depth));
        layer = Math.max(1, firstLayer);
        x = z = -horizontal();
    }
    private int horizontal() { return Math.min(radius, 4 + layer / 3); }
    public boolean hasNext() { return layer <= depth; }
    public Offset next() {
        if (!hasNext()) throw new java.util.NoSuchElementException();
        Offset result = new Offset(x, -layer, z);
        int range = horizontal();
        if (++z > range) {
            z = -range;
            if (++x > range) { layer++; x = z = -horizontal(); }
        }
        return result;
    }
}
