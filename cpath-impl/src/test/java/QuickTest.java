import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import static org.junit.Assert.*;

public class QuickTest {
    @Test
    public void splitSliceJoin() {
        String cols[] = "A\tB\tC\tD\tE".split("\t", 4);
        assertEquals(4, cols.length); //A, B, C and "D\tE"
        //join columns 0, 1, 2 -
        String res = StringUtils.join(Arrays.copyOfRange(cols, 0, 3), '\t');
        assertEquals("A\tB\tC", res);
    }
}
