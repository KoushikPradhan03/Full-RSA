package com.heavenbites.util;

/** String helpers: JSON quoting and cleaning text before saving to files. */
public final class Text {
    private Text() {}

    public static String q(String s) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append(' '); break;
                default:   sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** Removes characters we use as separators in the data files. */
    public static String clean(String s) {
        if (s == null) return "";
        return s.replace("|", " ").replace("^", " ").replace("~", " ")
                .replace("\n", " ").replace("\r", " ").trim();
    }
}