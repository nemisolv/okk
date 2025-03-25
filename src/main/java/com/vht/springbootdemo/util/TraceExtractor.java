package com.vht.springbootdemo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraceExtractor {


    public static String buildTrace(String trace, String status) {
        switch (status) {
            case "RUNNING":
                return "Connector is running";
            case "UNASSIGNED":
                return "Connector is unassigned";
            case "FAILED":
                return trace != null && !trace.isEmpty() ? "Failure reason: " + extractTrace(trace) : "Connector/Task has failed unexpectedly.";
            case "PAUSED":
                return "Connector/Task has been administratively paused.";
            default:
                return "Unknown issue detected in state: " + status;
        }
    }

    private static String extractTrace(String trace) {
        // Regex để lấy phần thông báo lỗi chính
        Pattern pattern = Pattern.compile("Caused by: .*?\\.([a-zA-Z]+Exception): (.+)");
        Matcher matcher = pattern.matcher(trace);

        if (matcher.find()) {
            return matcher.group(1) + ": " + matcher.group(2);
        }

        return trace;
    }
}
