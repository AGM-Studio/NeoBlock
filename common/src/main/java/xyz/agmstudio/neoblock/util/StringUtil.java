package xyz.agmstudio.neoblock.util;

import net.minecraft.network.chat.*;
import net.minecraft.util.valueproviders.UniformInt;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StringUtil {
    private static final UniformInt ONE = UniformInt.of(1, 1);

    /**
     * Converts string to snake-case
     *
     * @param string the string to be converted
     * @return the snake-case
     */
    public static @NotNull String convertToSnakeCase(@NotNull String string) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('-');
                result.append(Character.toLowerCase(c));
            } else result.append(c);
        }
        return result.toString();
    }

    /**
     * Encodes a string into Base64 format.
     *
     * @param input the string to be encoded
     * @return the Base64 encoded string
     */
    public static @NotNull String encodeToBase64(@NotNull String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    /**
     * Converts amount of ticks into a readable format
     *
     * @param ticks the amount of ticks
     * @return readable time format
     */
    public static @NotNull String formatTicks(long ticks) {
        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder timeString = new StringBuilder();
        if (hours > 0) {
            timeString.append(hours).append(":");
            timeString.append(String.format("%02d:", minutes));
            timeString.append(String.format("%02d", seconds));
        } else if (minutes > 0) {
            timeString.append(minutes).append(":");
            timeString.append(String.format("%02d", seconds));
        } else timeString.append(seconds).append(" seconds");
        return timeString.toString().trim();
    }
    public static @NotNull String formatTicks(int ticks) {
        return formatTicks((long) ticks);
    }

    /**
     * Translate given text with tags to component used by minecraft
     *
     * @param input the text you want to translate
     * @return the parsed component
     */
    public static Component parseMessage(String input) {
        MutableComponent component = Component.literal("");
        StringBuilder buffer = new StringBuilder();
        Style style = Style.EMPTY;

        boolean styling = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (styling && c == '}') {
                styling = false;
                List<String> tags = smartSplit(buffer.toString());
                buffer.setLength(0);

                for (String tag: tags) {
                    if (tag.equals("r")) style = Style.EMPTY;
                    else if (tag.equals("b")) style = style.withBold(true);
                    else if (tag.equals("i")) style = style.withItalic(true);
                    else if (tag.equals("s")) style = style.withStrikethrough(true);
                    else if (tag.equals("u")) style = style.withUnderlined(true);
                    else if (tag.startsWith("c=#")) {
                        try {
                            int rgb = Integer.parseInt(tag.substring(3), 16);
                            style = style.withColor(rgb);
                        } catch (NumberFormatException ignored) {}
                    }
                    else if (tag.startsWith("command=")) {
                        ClickEvent action = new ClickEvent(ClickEvent.Action.RUN_COMMAND, unquote(tag.substring(8)));
                        style = style.withClickEvent(action);
                    }
                    else if (tag.startsWith("suggest=")) {
                        ClickEvent action = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, unquote(tag.substring(8)));
                        style = style.withClickEvent(action);
                    }
                    else if (tag.startsWith("href=")) {
                        ClickEvent action = new ClickEvent(ClickEvent.Action.OPEN_URL,  unquote(tag.substring(5)));
                        style = style.withClickEvent(action);
                    }
                    else if (tag.startsWith("h=")) {
                        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, parseMessage(unquote(tag.substring(2))));
                        style = style.withHoverEvent(hover);
                    }
                }
            } else if (c == '{') {
                if (input.length() > i + 1 && input.charAt(i + 1) == '{') {
                    buffer.append('{');
                    i += 1;
                } else {
                    styling = true;
                    if (!buffer.isEmpty()) {
                        component.append(Component.literal(buffer.toString()).withStyle(style));
                        buffer.setLength(0);
                        style = Style.EMPTY;
                    }
                }
            } else {
                buffer.append(c);
            }
        }

        if (!buffer.isEmpty() && !styling) component.append(Component.literal(buffer.toString()).withStyle(style));

        return component;
    }

    public static List<String> smartSplit(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inQuotes = false;
        char quote = 0;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) escaped = false;
            else if (c == '\\') escaped = true;
            else if ((c == '\'' || c == '"')) {
                if (inQuotes && c == quote) inQuotes = false;
                else if (!inQuotes) {
                    inQuotes = true;
                    quote = c;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) result.add(current.toString().trim());

        return result;
    }

    public static String unquote(String value) {
        if (value == null || value.length() < 2) return value;

        char first = value.charAt(0);
        char last  = value.charAt(value.length() - 1);

        if (first != last) return value;
        if (first != '\'' && first != '"') return value;

        String inner = value.substring(1, value.length() - 1);
        StringBuilder out = new StringBuilder(inner.length());

        boolean escaped = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
            } else if (c == '\\') escaped = true;
            else out.append(c);
        }

        return out.toString();
    }


    /**
     * Formats the double into the digits requested.
     *
     * @param value the value to be rounded
     * @param digits the amount of digits
     * @return formatted text
     */
    public static @NotNull String round(double value, int digits) {
        return String.format("%%.%df".formatted(digits), value);
    }

    /**
     * Formats the double into percentage with specified digits
     *
     * @param value  the value to turn into percentage
     * @param digits the amount of digits
     * @return formatted text
     */
    public static @NotNull String percentage(double value, int digits) {
        return round(value * 100.0, digits) + "%";
    }

    public static double parseChance(String string) {
        if (string != null) try {
            return Double.parseDouble(string) / 100.0;
        } catch (NumberFormatException ignored) {}
        return 1.0;
    }

    public static String stringChance(double chance) {
        return (chance < 1.0) ? " " + (chance * 100) + "%" : "";
    }

    public static UniformInt parseRange(String string) {
        if (string == null) return ONE;
        if (string.endsWith("x")) string = string.substring(0, string.length() - 1);
        if (string.contains("-")) {
            String[] parts = string.split("-");
            return UniformInt.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        int c = Integer.parseInt(string);
        return UniformInt.of(c, c);
    }

    public static String stringUniformInt(UniformInt range) {
        int min = range.getMinValue();
        int max = range.getMaxValue();
        return (min == max) ? min <= 1 ? "" : min + "x" : min + "-" + max + "x";
    }
}
