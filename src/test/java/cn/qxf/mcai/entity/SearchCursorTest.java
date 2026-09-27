package cn.qxf.mcai.entity;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class SearchCursorTest {
    @Test void scanningInSmallBudgetsEventuallyReachesDeepLayerWithoutRescanning() {
        var cursor = new SearchCursor(6, 12, 1);
        var seen = new HashSet<SearchCursor.Offset>();
        int frames = 0;
        while (cursor.hasNext()) {
            for (int i = 0; i < 7 && cursor.hasNext(); i++)
                assertTrue(seen.add(cursor.next()));
            frames++;
        }
        assertTrue(frames > 10);
        assertTrue(seen.stream().anyMatch(offset -> offset.y() == -12));
        assertFalse(cursor.hasNext());
    }
}
