package xyz.agmstudio.neoblock.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class PatternUtil {
    public interface Fragment {
        String regex();

        default Fragment optional() {
            return () -> "(?:" + this.regex() + ")?";
        }

        default Fragment named(String newName) {
            String r = this.regex();
            return () -> r.replaceFirst("\\?<([a-zA-Z_][a-zA-Z0-9_]*)>", "?<" + newName + ">");
        }

        default Fragment then(Fragment next) {
            return () -> this.regex() + next.regex();
        }
        default Fragment then(String text) {
            return () -> this.regex() + text;
        }
        default Fragment space() {
            return () -> this.regex() + "\\s*";
        }

        default Pattern build(boolean anchored) {
            Pattern result = Pattern.compile(anchored ? "^" + this.regex() + "$" : this.regex());
            System.out.println(result.pattern());
            return result;
        }

    }

    public static final Fragment SPACE      = () -> "\\s+";
    public static final Fragment COUNT      = () -> "(?<count>\\d+)x";
    public static final Fragment RANGE      = () -> "(?<count>\\d+(?:-\\d+)?)x";
    public static final Fragment RANGE_NOX  = () -> "(?<count>\\d+(?:-\\d+)?)";
    public static final Fragment NAMESPACE  = () -> "(?<id>([a-z_][a-z0-9_]*:)?[a-z_][a-z0-9_]*)";
    public static final Fragment STRICT_ID  = () -> "(?<id>[a-z_][a-z0-9_]*)";
    public static final Fragment NAME       = () -> "(?<name>[\\w-]+)";
    public static final Fragment CHANCE     = () -> "(?:\\s+(?<chance>\\d+(?:\\.\\d*)?)%?)";

    @Contract(pure = true)
    public static @NotNull Fragment namespace(String name) {
        return () -> "(?<" + name + ">[a-z0-9_]+:[a-z0-9_/]+)";
    }
    @Contract(pure = true)
    public static @NotNull Fragment namespace(String name, String base, String path) {
        return () -> "(?<" + name + ">(?<" + base + ">[a-z0-9_]+):(?<" + path + ">[a-z0-9_/]+))";
    }
    @Contract(pure = true)
    public static @NotNull Fragment group(String name, String divider) {
        return () -> "(?<" + name + ">[^" + divider + "]+)";
    }

    public static @NotNull Fragment literal(String s) {
        String quoted = Pattern.quote(s);
        return () -> quoted;
    }
    @Contract(pure = true)
    public static @NotNull Fragment raw(String regex) {
        return () -> regex;
    }
}

