package io.github.rick007110.displaylinkplus.link;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Text templates for Display Link labels and list rows. A placeholder is {@code {field:format}} where both parts are
 * optional:
 * <ul>
 *     <li>field: empty = the value of a single-line source; {@code name} / {@code count} = a list row's item and amount</li>
 *     <li>format: {@code k} = compact (1.2k, 3.4M), {@code ,} = thousands separators (1,234), {@code .N} = round to N
 *     decimals (0-3). Applied to the first number in the value, so units like "mB" or "RPM" are kept.</li>
 * </ul>
 * Examples: "We have {} bread", "{k} items/s", "{name}: {count:,}". The colon may be left out ({k}, {,}).
 */
public final class LabelTemplate {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(name|count)?:?(k|,|\\.[0-3])?}");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final String[] COMPACT_UNITS = {"k", "M", "B", "T"};

    private LabelTemplate() {
    }

    /** The label as a single-line template, or null if it has no value placeholder ({}, {k}, ...). */
    @Nullable
    public static String templateOf(String label) {
        Matcher matcher = PLACEHOLDER.matcher(label);
        while (matcher.find())
            if (matcher.group(1) == null)
                return label;
        return null;
    }

    /** True if the text contains any placeholder (used for list row formats). */
    public static boolean hasPlaceholder(String text) {
        return PLACEHOLDER.matcher(text).find();
    }

    /** Fills every value placeholder with the value; {name}/{count} are left as typed. */
    public static MutableComponent fill(String template, MutableComponent value) {
        MutableComponent result = Component.empty();
        Matcher matcher = PLACEHOLDER.matcher(template);
        int last = 0;
        while (matcher.find()) {
            if (matcher.group(1) != null)
                continue;
            appendLiteral(result, template.substring(last, matcher.start()));
            String format = matcher.group(2);
            result.append(format == null ? value.copy() : Component.literal(formatNumber(value.getString(), format)));
            last = matcher.end();
        }
        appendLiteral(result, template.substring(last));
        return result;
    }

    /**
     * Fills a list row. {@code {count}} (or {@code {}}) is the amount as Create formatted it, respecting the source's
     * Shortened / Full Number setting; with a format like {@code {count:k}} the raw amount is formatted instead,
     * followed by {@code unit} if there is one (mB for fluids). Components keep item names translatable.
     */
    public static MutableComponent fillRow(String template, MutableComponent name, MutableComponent count, long rawCount,
                                           @Nullable MutableComponent unit) {
        MutableComponent result = Component.empty();
        Matcher matcher = PLACEHOLDER.matcher(template);
        int last = 0;
        while (matcher.find()) {
            appendLiteral(result, template.substring(last, matcher.start()));
            String format = matcher.group(2);
            if ("name".equals(matcher.group(1))) {
                result.append(name.copy());
            } else if (format == null) {
                result.append(count.copy());
            } else {
                result.append(Component.literal(formatNumber(String.valueOf(rawCount), format)));
                if (unit != null)
                    result.append(" ").append(unit.copy());
            }
            last = matcher.end();
        }
        appendLiteral(result, template.substring(last));
        return result;
    }

    /** Reformats the first number in {@code text}; the rest (units, signs) is kept. */
    public static String formatNumber(String text, String format) {
        Matcher matcher = NUMBER.matcher(text);
        if (!matcher.find())
            return text;
        BigDecimal number = new BigDecimal(matcher.group());
        String formatted = switch (format) {
            case "k" -> compact(number);
            case "," -> String.format(Locale.ROOT, number.scale() > 0 ? "%,." + number.scale() + "f" : "%,d",
                    number.scale() > 0 ? number : number.toBigInteger());
            default -> number.setScale(Integer.parseInt(format.substring(1)), RoundingMode.HALF_UP).toPlainString();
        };
        return text.substring(0, matcher.start()) + formatted + text.substring(matcher.end());
    }

    private static String compact(BigDecimal number) {
        BigDecimal abs = number.abs();
        BigDecimal thousand = BigDecimal.valueOf(1000);
        if (abs.compareTo(thousand) < 0)
            return number.stripTrailingZeros().toPlainString();
        int unit = -1;
        BigDecimal scaled = number;
        while (scaled.abs().compareTo(thousand) >= 0 && unit < COMPACT_UNITS.length - 1) {
            scaled = scaled.divide(thousand);
            unit++;
        }
        // One decimal below 100 (1.2k, 15.3k), none above (153k)
        int decimals = scaled.abs().compareTo(BigDecimal.valueOf(100)) < 0 ? 1 : 0;
        return scaled.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + COMPACT_UNITS[unit];
    }

    private static void appendLiteral(MutableComponent result, String text) {
        if (!text.isEmpty())
            result.append(Component.literal(text));
    }
}
