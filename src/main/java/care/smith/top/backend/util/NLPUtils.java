package care.smith.top.backend.util;

public class NLPUtils {
    public static String stringConformity(String s) {
        if (s == null || s.isEmpty()) return null;
        return s.toLowerCase().replaceAll("\\s+", "_");
    }
}
