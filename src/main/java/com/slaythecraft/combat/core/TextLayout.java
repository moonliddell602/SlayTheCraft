package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 文字排版的两个**纯逻辑**工具（v0.5.1.2）。
 *
 * <p>需求（卡牌图鉴）："升级后的卡牌得到的改动以黄色字体高亮，例如数值变化。"</p>
 *
 * <p>要在一句话里给**某几个词**换颜色，就必须知道"折行之后每一行对应原文的哪一段"，
 * 否则高亮会跟着折行错位。这里就是那套下标计算 —— 不碰 Minecraft，宽度靠传入的
 * {@code widthOf} 函数测量，所以能进裸 JVM 自检（自检里用"每个字符 6 像素"的假测量器）。</p>
 */
public final class TextLayout {

    private TextLayout() {
    }

    /** 一行文字在**原文**中的区间 {@code [start, end)}。 */
    public record Range(int start, int end) {
        public String substring(String text) {
            return text.substring(start, end);
        }
    }

    /** 一段连续、颜色相同的文字在原文中的区间。 */
    public record Run(int start, int end, boolean highlighted) {
    }

    /**
     * 按 {@code maxWidth} 把 {@code text} 折行，返回每行在原文里的区间。
     *
     * <p>规则：优先在空格后断行；行尾空格不计入区间（但**下标仍对应原文**，
     * 所以拿着区间去取原文一定不会取错字）；{@code \n} 强制换行。</p>
     *
     * @param text     原文（{@code null} 视为空）
     * @param maxWidth 每行最大像素宽（{@code <= 0} 视为不折行、返回空）
     * @param widthOf  测量一段文字宽度（自检里是假的，游戏里是 {@code Font::width}）
     */
    public static List<Range> wrapRanges(String text, int maxWidth, ToIntFunction<String> widthOf) {
        List<Range> out = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return out;
        }
        int n = text.length();
        int start = 0;
        while (start < n) {
            if (text.charAt(start) == '\n') {
                start++;
                continue;
            }
            int end = start;
            int width = 0;
            int lastSpace = -1;
            while (end < n) {
                char ch = text.charAt(end);
                if (ch == '\n') {
                    break;
                }
                int charWidth = widthOf.applyAsInt(String.valueOf(ch));
                // 第一个字符无论多宽都要收下，否则会死循环
                if (end > start && width + charWidth > maxWidth) {
                    break;
                }
                width += charWidth;
                if (ch == ' ') {
                    lastSpace = end + 1;
                }
                end++;
            }
            boolean cutByWidth = end < n && text.charAt(end) != '\n';
            if (cutByWidth && lastSpace > start) {
                end = lastSpace;
            }
            int trimmedEnd = end;
            while (trimmedEnd > start && text.charAt(trimmedEnd - 1) == ' ') {
                trimmedEnd--;
            }
            out.add(new Range(start, trimmedEnd));
            start = end;
            // 跳过行首空格与换行符
            while (start < n && text.charAt(start) == ' ') {
                start++;
            }
            if (start < n && text.charAt(start) == '\n') {
                start++;
            }
        }
        return out;
    }

    /**
     * 把"逐字符高亮标记"压成若干段连续区间，供渲染时按段换色。
     *
     * @param marks 逐字符标记（可以比 {@code end} 短，越界部分视为未高亮）
     */
    public static List<Run> markRuns(boolean[] marks, int start, int end) {
        List<Run> out = new ArrayList<>();
        if (marks == null || start >= end) {
            return out;
        }
        int i = start;
        while (i < end) {
            boolean highlighted = i < marks.length && marks[i];
            int j = i;
            while (j < end && (j < marks.length && marks[j]) == highlighted) {
                j++;
            }
            out.add(new Run(i, j, highlighted));
            i = j;
        }
        return out;
    }

    /** 把若干段区间的文字拼起来（自检里用来验证"折行没丢字"）。 */
    public static String join(List<? extends Range> ranges, String text) {
        StringBuilder sb = new StringBuilder();
        for (Range range : ranges) {
            sb.append(range.substring(text));
        }
        return sb.toString();
    }
}
