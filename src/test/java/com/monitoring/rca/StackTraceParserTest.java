package com.monitoring.rca;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StackTraceParserTest {

    @Test
    void testParseSimpleStackTrace() {
        String log = """
                java.lang.NullPointerException
                at com.example.MyService.doSomething(MyService.java:17)
                at com.example.App.main(App.java:5)
                """;

        StackTraceParser parser = new StackTraceParser();
        List<StackFrame> frames = parser.parse(log);

        assertEquals(2, frames.size());

        StackFrame frame1 = frames.get(0);
        assertEquals("com.example.MyService", frame1.getClassName());
        assertEquals("doSomething", frame1.getMethodName());
        assertEquals("MyService.java", frame1.getFileName());
        assertEquals(17, frame1.getLineNumber());

        StackFrame frame2 = frames.get(1);
        assertEquals("com.example.App", frame2.getClassName());
        assertEquals("main", frame2.getMethodName());
        assertEquals("App.java", frame2.getFileName());
        assertEquals(5, frame2.getLineNumber());
    }

    @Test
    void testNoStackTraceFound() {
        String log = "This is just a normal message, no stack trace";
        StackTraceParser parser = new StackTraceParser();
        List<StackFrame> frames = parser.parse(log);
        assertTrue(frames.isEmpty());
    }
}