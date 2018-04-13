import org.junit.Test;

import static org.junit.Assert.*;


public class AdderTest {

    @Test
    public void evaluate() throws ParseException {
        assertEquals(Adder.evaluate("1+2"), 3);
        assertEquals(Adder.evaluate("1+ 2"), 3);
        assertEquals(Adder.evaluate("1000+ 2"), 1002);
    }
}