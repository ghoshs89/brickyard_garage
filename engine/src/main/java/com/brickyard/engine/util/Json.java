package com.brickyard.engine.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer with no dependencies (Android's org.json is not on
 * the desktop JVM, and the engine must run in both places).
 * Objects parse to LinkedHashMap, arrays to ArrayList, numbers to Double or Long.
 */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.err("trailing characters");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        if (!(o instanceof Map)) throw new IllegalArgumentException("expected JSON object");
        return (Map<String, Object>) o;
    }

    private IllegalArgumentException err(String m) {
        int line = 1;
        for (int k = 0; k < Math.min(i, s.length()); k++) if (s.charAt(k) == '\n') line++;
        return new IllegalArgumentException("JSON " + m + " at line " + line);
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
            else break;
        }
    }

    private Object value() {
        if (i >= s.length()) throw err("unexpected end");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private void expect(String w) {
        if (!s.startsWith(w, i)) throw err("expected " + w);
        i += w.length();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            if (s.charAt(i) != '"') throw err("expected key");
            String k = string();
            ws();
            if (s.charAt(i) != ':') throw err("expected ':'");
            i++;
            ws();
            m.put(k, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') return m;
            if (c != ',') throw err("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        List<Object> a = new ArrayList<>();
        i++;
        ws();
        if (s.charAt(i) == ']') { i++; return a; }
        while (true) {
            ws();
            a.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') return a;
            if (c != ',') throw err("expected ',' or ']'");
        }
    }

    private String string() {
        StringBuilder b = new StringBuilder();
        i++;
        while (true) {
            if (i >= s.length()) throw err("unterminated string");
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                    default: b.append(e);
                }
            } else b.append(c);
        }
    }

    private Object number() {
        int st = i;
        boolean frac = false;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+') i++;
            else if (c == '.' || c == 'e' || c == 'E') { frac = true; i++; }
            else break;
        }
        String t = s.substring(st, i);
        if (t.isEmpty()) throw err("unexpected character");
        return frac ? (Object) Double.parseDouble(t) : (Object) Long.parseLong(t);
    }

    // ---------------------------------------------------------------- writing

    public static String write(Object o) {
        StringBuilder b = new StringBuilder();
        write(b, o, 0, false);
        return b.toString();
    }

    public static String pretty(Object o) {
        StringBuilder b = new StringBuilder();
        write(b, o, 0, true);
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder b, Object o, int ind, boolean pretty) {
        if (o == null) b.append("null");
        else if (o instanceof String) quote(b, (String) o);
        else if (o instanceof Boolean || o instanceof Long || o instanceof Integer) b.append(o);
        else if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d);
            else b.append(d);
        } else if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            b.append('{');
            Iterator<Map.Entry<String, Object>> it = m.entrySet().iterator();
            boolean first = true;
            while (it.hasNext()) {
                Map.Entry<String, Object> e = it.next();
                if (!first) b.append(',');
                first = false;
                if (pretty) nl(b, ind + 1);
                quote(b, e.getKey());
                b.append(pretty ? ": " : ":");
                write(b, e.getValue(), ind + 1, pretty);
            }
            if (pretty && !m.isEmpty()) nl(b, ind);
            b.append('}');
        } else if (o instanceof List) {
            List<Object> l = (List<Object>) o;
            b.append('[');
            for (int k = 0; k < l.size(); k++) {
                if (k > 0) b.append(pretty ? ", " : ",");
                write(b, l.get(k), ind + 1, pretty);
            }
            b.append(']');
        } else if (o instanceof int[]) {
            int[] a = (int[]) o;
            b.append('[');
            for (int k = 0; k < a.length; k++) { if (k > 0) b.append(','); b.append(a[k]); }
            b.append(']');
        } else quote(b, o.toString());
    }

    private static void nl(StringBuilder b, int ind) {
        b.append('\n');
        for (int k = 0; k < ind; k++) b.append("  ");
    }

    private static void quote(StringBuilder b, String v) {
        b.append('"');
        for (int k = 0; k < v.length(); k++) {
            char c = v.charAt(k);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        b.append('"');
    }

    // ---------------------------------------------------------------- typed getters

    public static int getInt(Map<String, Object> m, String k, int def) {
        Object o = m.get(k);
        return o instanceof Number ? ((Number) o).intValue() : def;
    }

    public static long getLong(Map<String, Object> m, String k, long def) {
        Object o = m.get(k);
        return o instanceof Number ? ((Number) o).longValue() : def;
    }

    public static double getDouble(Map<String, Object> m, String k, double def) {
        Object o = m.get(k);
        return o instanceof Number ? ((Number) o).doubleValue() : def;
    }

    public static String getString(Map<String, Object> m, String k, String def) {
        Object o = m.get(k);
        return o instanceof String ? (String) o : def;
    }

    public static boolean getBool(Map<String, Object> m, String k, boolean def) {
        Object o = m.get(k);
        return o instanceof Boolean ? (Boolean) o : def;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> m, String k) {
        Object o = m.get(k);
        return o instanceof List ? (List<Object>) o : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> m, String k) {
        Object o = m.get(k);
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<String, Object>();
    }
}
