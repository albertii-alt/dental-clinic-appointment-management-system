package com.dentalclinic.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MiniJson {
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\\"(username|password|selectedRole)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"");

    private MiniJson() {
    }

    static Map<String, String> parseLoginBody(String json) {
        if (json == null) {
            return new HashMap<>();
        }

        Matcher matcher = FIELD_PATTERN.matcher(json);
        Map<String, String> values = new HashMap<>();
        while (matcher.find()) {
            values.put(matcher.group(1), unescape(matcher.group(2)));
        }
        return values;
    }

    static String string(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    static String bool(boolean value) {
        return value ? "true" : "false";
    }

    static String number(int value) {
        return Integer.toString(value);
    }

    static String stringArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            escaped.add(string(value));
        }
        return "[" + String.join(",", escaped) + "]";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '\"':
                        out.append('"');
                        break;
                    default:
                        out.append(c);
                        break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }

        if (escaping) {
            out.append('\\');
        }

        return out.toString();
    }
}
