package com.monitoring.rca;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackTraceParser {
    private static final Pattern pattern = Pattern.compile(
            "at (\\S+)\\.(\\S+)\\((\\S+):(\\d+)\\)"
    );

    public static List<StackFrame> parse(String log) {
        List<StackFrame> frames = new ArrayList<>();
        Matcher matcher = pattern.matcher(log);
        while (matcher.find()) {
            frames.add(new StackFrame(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    Integer.parseInt(matcher.group(4))
            ));
        }
        return frames;
    }
}