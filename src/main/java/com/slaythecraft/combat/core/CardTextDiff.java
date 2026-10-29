package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡牌描述**逐段比较**（v0.5.1.2）。
 *
 * <p>需求（卡牌图鉴）："升级后的卡牌得到的改动以黄色字体高亮，例如数值变化。
 * 如果为『失去消耗』等内容则不用。"</p>
 *
 * <p>做法：把两段描述切成**词元**（连续数字算一个词元、其余按字符），然后做一次
 * 简单的**局部对齐** —— 相同的词元跳过；不同的词元记成"变化"并尝试重新对齐。
 * 于是：</p>
 * <ul>
 *   <li>{@code 造成 6 点伤害。} → {@code 造成 9 点伤害。}：只有 {@code 6}/{@code 9} 被标成变化；</li>
 *   <li>{@code 获得 2 点敏捷，消耗。} → {@code 获得 2 点敏捷。}：被删掉的 {@code ，消耗} 不产生"变化"
 *       （它是**消失**的词元，升级版里没有对应内容可以高亮）；</li>
 *   <li>{@code 敌人失去 3 点力量} → {@code 敌人失去 4 点力量}：只有数字变。</li>
 * </ul>
 *
 * <p>纯逻辑、零 Minecraft 依赖，所以能进裸 JVM 自检。</p>
 */
public final class CardTextDiff {

    /** 一段文字：{@code changed = true} 表示它是"升级带来的变化"，界面上要高亮。 */
    public record Span(String text, boolean changed) {
    }

    private CardTextDiff() {
    }

    /**
     * 计算"从 {@code base} 升级到 {@code upgraded} 之后，哪些词元变了"。
     *
     * <p>用**最长公共子序列**（LCS）对齐两份描述：属于公共子序列的词元 = 没变，
     * 其余 = 升级带来的变化。这样"造成 6 点伤害。→ 造成 9 点伤害。"只会把 {@code 9} 标成变化，
     * 而"获得 2 点敏捷，消耗。→ 获得 2 点敏捷。"里被删掉的 {@code ，消耗} 不会产生任何高亮
     * （升级版里没有对应内容可以高亮 —— 正是需求说的"失去消耗那类不用"）。</p>
     *
     * <p>⚠️ 我第一版用的是"贪心重对齐"，它会把 {@code 6 点} 里的 {@code 点} 当成可以对齐的锚点，
     * 于是把后面整串都标成变化（自检当场抓到）。描述很短，直接上 LCS 又准又简单。</p>
     *
     * @param base     基础版描述（{@code null} 视为空）
     * @param upgraded 升级版描述（{@code null} 视为空）
     * @return 升级版描述切成的词元序列，变化的那些 {@code changed = true}
     */
    public static List<Span> diff(String base, String upgraded) {
        List<String> want = tokenize(upgraded == null ? "" : upgraded);
        List<String> have = tokenize(base == null ? "" : base);
        int n = want.size();
        int m = have.size();

        // lcs[i][j] = want[i..] 与 have[j..] 的最长公共子序列长度
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (want.get(i).equals(have.get(j))) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        // 回溯：want 里**不在**公共子序列中的词元就是"变化"
        List<Span> result = new ArrayList<>(n);
        int i = 0;
        int j = 0;
        while (i < n) {
            if (j < m && want.get(i).equals(have.get(j))) {
                result.add(new Span(want.get(i), false));
                i++;
                j++;
            } else if (j < m && lcs[i][j + 1] >= lcs[i + 1][j]) {
                // 这个词元来自 base 被删掉的部分：跳过它（升级版里没有对应内容可高亮）
                j++;
            } else {
                result.add(new Span(want.get(i), true));
                i++;
            }
        }
        return List.copyOf(result);
    }

    /**
     * 把一段描述切成词元：**连续数字算一个词元**，其余每个字符一个词元。
     *
     * <p>"连续数字算一个"是关键：{@code 造成 12 点伤害} 里的 {@code 12} 必须整体高亮，
     * 而不是把 {@code 1} 和 {@code 2} 各标一次。支持小数（{@code 1.5}）。</p>
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                int start = i;
                while (i < text.length() && (Character.isDigit(text.charAt(i)) || text.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(text.substring(start, i));
            } else {
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

    /** 把词元拼回一段文字（丢掉"变化"标记）。 */
    public static String join(List<Span> spans) {
        StringBuilder sb = new StringBuilder();
        for (Span span : spans) {
            sb.append(span.text());
        }
        return sb.toString();
    }

    /**
     * 升级版描述的**逐字符**高亮标记（长度 = 升级版描述长度）。
     *
     * <p>界面折行是按字符走的，所以词元级的 {@link Span} 需要摊平成逐字符数组才能
     * 和高亮渲染对上。{@code marks[i] == true} 表示升级版描述的第 {@code i} 个字符
     * 是"升级带来的变化"，应该用黄色画。</p>
     */
    public static boolean[] marks(String base, String upgraded) {
        List<Span> spans = diff(base, upgraded);
        String full = join(spans);
        boolean[] out = new boolean[full.length()];
        int at = 0;
        for (Span span : spans) {
            int len = span.text().length();
            if (span.changed()) {
                for (int k = 0; k < len; k++) {
                    out[at + k] = true;
                }
            }
            at += len;
        }
        return out;
    }

    /** 有没有任何高亮（界面据此决定要不要走逐段绘制）。 */
    public static boolean hasChanges(List<Span> spans) {
        for (Span span : spans) {
            if (span.changed()) {
                return true;
            }
        }
        return false;
    }
}
