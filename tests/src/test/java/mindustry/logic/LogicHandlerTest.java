package mindustry.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import mindustry.logic.LogicHandler;

public class LogicHandlerTest {
    private LogicHandler handler = new LogicHandler();

    @Test
    public void testEvalNull() {
        assertNull(handler.eval(null));
    }

    @Test
    public void testEvalVariable() {
        handler.setVariable("health", 100);
        assertEquals(100, handler.eval("$health"));
    }
}
