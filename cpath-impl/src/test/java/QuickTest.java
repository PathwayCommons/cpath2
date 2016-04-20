import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class QuickTest {
    @Test
    public void splitSliceJoin() {
        String cols[] = "A\tB\tC\tD\tE".split("\t", 4);
        assertEquals(4, cols.length); //A, B, C and "D\tE"
        //join columns 0, 1, 2 -
        String res = StringUtils.join(Arrays.copyOfRange(cols, 0, 3), '\t');
        assertEquals("A\tB\tC", res);
        //System.out.println(String.format("%3.1f",35.6345f));

        Matcher matcher = Pattern.compile("([a-zA-Z0-9\\. ]+)\\s*\\(\\s*(\\d+)\\s*\\)").matcher("Homo sapiens (9606)");
        assertTrue(matcher.find());
        assertEquals(2, matcher.groupCount());
//        System.out.println(String.format("%s - %s",matcher.group(1),matcher.group(2)));
    }
}
