import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardKeyword;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardRarity;
import com.slaythecraft.combat.core.CardTarget;
import com.slaythecraft.combat.core.CardType;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出**全部卡牌的可读内容**（v0.5.5.4，TXT + Markdown）。
 *
 * <p>用法（由 {@code tools/export-card-data.ps1} 调用）：</p>
 *
 * <pre>
 * java -Dstdout.encoding=UTF-8 -cp &lt;engine classes&gt; ExportCardData &lt;输出目录&gt;
 * </pre>
 *
 * <p>导出内容（每张卡一块）：id、卡名（基础 / 强化）、归属、来源分类、类型、品质、费用（基础 / 强化）、
 * 目标、关键字（基础 / 强化）、标记（生成牌 / X 费 / 多人玩法向 / 可强化档数）、
 * 卡面效果文案（基础 / 强化）。</p>
 *
 * <p>⚠️ **"效果"导出的是卡面文案**：引擎里的效果是 Java 代码（{@code CardEffect} 函数），
 * 没有可序列化的形式，所以这里给出的就是游戏里印在卡面上的那句话 —— 与实现逐字一致。</p>
 */
public final class ExportCardData {

    private ExportCardData() {
    }

    /** 分组顺序与显示名（与卡图 id 表保持同一套）。 */
    private static final Map<String, String> GROUP_LABELS = new LinkedHashMap<>();

    static {
        GROUP_LABELS.put("stc:ironclad", "铁甲战士");
        GROUP_LABELS.put("stc:silent", "静默猎手");
        GROUP_LABELS.put("stc:defect", "故障机器人（机宝）");
        GROUP_LABELS.put("stc:pillager", "叛逃掠夺者");
        GROUP_LABELS.put("colorless", "无色牌");
        GROUP_LABELS.put("basic", "基础牌（打击 / 格挡）");
        GROUP_LABELS.put("status", "状态牌");
        GROUP_LABELS.put("curse", "诅咒牌");
        GROUP_LABELS.put("generated", "生成牌（只能被遗物 / 卡牌生成）");
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        List<Card> cards = new ArrayList<>(CardLibrary.all());
        cards.sort(Comparator.comparing(Card::id));
        Map<String, List<Card>> grouped = group(cards);

        String version = modVersion();
        String txt = text(cards, grouped, version);
        String md = markdown(cards, grouped, version);

        Path dir = Path.of(args.length > 0 ? args[0] : "docs");
        Files.createDirectories(dir);
        Path txtPath = dir.resolve("cards-export.txt");
        Path mdPath = dir.resolve("cards-export.md");
        Files.writeString(txtPath, txt, StandardCharsets.UTF_8);
        Files.writeString(mdPath, md, StandardCharsets.UTF_8);

        out.println("version=" + version);
        out.println("cards=" + cards.size());
        out.println("txt=" + txtPath.toAbsolutePath());
        out.println("md=" + mdPath.toAbsolutePath());
    }

    /** 从 gradle.properties 读版本号（读不到就写 unknown）。 */
    private static String modVersion() {
        try {
            for (String line : Files.readAllLines(Path.of("gradle.properties"), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("mod_version=")) {
                    return trimmed.substring("mod_version=".length()).trim();
                }
            }
        } catch (Exception ignored) {
            // 不在仓库根目录跑的时候就退化成 unknown，不报错
        }
        return "unknown";
    }

    // ------------------------------------------------------------------ 纯文本（给 Word / 记事本看）

    private static String text(List<Card> cards, Map<String, List<Card>> grouped, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("SlayTheCraft 卡牌全表（mod 版本 ").append(version).append("，共 ").append(cards.size())
                .append(" 个 id）\n");
        sb.append("=".repeat(78)).append("\n\n");
        sb.append("说明：\n");
        sb.append("  · 一行一个字段；\"效果\"就是游戏里印在卡面上的那句话（引擎里的效果是代码，\n");
        sb.append("    文案与实现逐字一致）。\n");
        sb.append("  · \"强化\"= 用篝火锻造 / 武器匠事件升级后的那一版；没有强化版的卡写「（无强化版）」。\n");
        sb.append("  · 生成牌没有强化版以外的获取途径：只能被遗物 / 卡牌生成，不进任何卡池。\n");
        sb.append("  · 分组总览：");
        for (Map.Entry<String, List<Card>> entry : grouped.entrySet()) {
            sb.append(GROUP_LABELS.getOrDefault(entry.getKey(), entry.getKey())).append(' ')
                    .append(entry.getValue().size()).append("；");
        }
        sb.append("\n\n");

        for (Map.Entry<String, List<Card>> entry : grouped.entrySet()) {
            sb.append("=".repeat(78)).append("\n");
            sb.append("【").append(GROUP_LABELS.getOrDefault(entry.getKey(), entry.getKey())).append("】")
                    .append(entry.getValue().size()).append(" 张\n");
            sb.append("=".repeat(78)).append("\n\n");
            for (Card card : entry.getValue()) {
                appendTextCard(sb, card);
            }
        }
        sb.append("（导出结束，共 ").append(cards.size()).append(" 个 id）\n");
        return sb.toString();
    }

    private static void appendTextCard(StringBuilder sb, Card card) {
        Card upgraded = CardLibrary.upgraded(card.id());
        sb.append("id: ").append(card.id()).append('\n');
        sb.append("  卡名: ").append(card.name())
                .append(upgraded == null ? "" : "   强化后: " + upgraded.name()).append('\n');
        sb.append("  归属: ").append(character(card)).append("   类型: ").append(card.type().label())
                .append("   品质: ").append(card.rarity().label())
                .append("   目标: ").append(target(card.target())).append('\n');
        sb.append("  费用: ").append(card.cost())
                .append(upgraded == null ? "" : "   强化后: " + upgraded.cost()).append('\n');
        sb.append("  关键字: ").append(keywords(card))
                .append(upgraded == null ? "" : "   强化后: " + keywords(upgraded)).append('\n');
        sb.append("  标记: ").append(flags(card)).append('\n');
        sb.append("  效果: ").append(card.description()).append('\n');
        if (upgraded != null) {
            sb.append("  强化效果: ").append(upgraded.description()).append('\n');
        } else {
            sb.append("  强化: （无强化版）\n");
        }
        sb.append('\n');
    }

    // ------------------------------------------------------------------ Markdown（给网页 / 编辑器看）

    private static String markdown(List<Card> cards, Map<String, List<Card>> grouped, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SlayTheCraft 卡牌全表（mod ").append(version).append("，共 ").append(cards.size())
                .append(" 个 id）\n\n");
        sb.append("> 自动生成；重新导出：`powershell -ExecutionPolicy Bypass -File tools\\export-card-data.ps1`\n");
        sb.append("> 纯文本版（Word / 记事本可直接打开）：[`cards-export.txt`](cards-export.txt)\n\n");
        sb.append("「效果」就是卡面上印的那句话；「强化」是升级后的那一版。\n\n");
        sb.append("| 分组 | id 数 |\n| --- | ---: |\n");
        for (Map.Entry<String, List<Card>> entry : grouped.entrySet()) {
            sb.append("| ").append(GROUP_LABELS.getOrDefault(entry.getKey(), entry.getKey())).append(" | ")
                    .append(entry.getValue().size()).append(" |\n");
        }
        sb.append("| **合计** | **").append(cards.size()).append("** |\n");

        for (Map.Entry<String, List<Card>> entry : grouped.entrySet()) {
            sb.append("\n## ").append(GROUP_LABELS.getOrDefault(entry.getKey(), entry.getKey()))
                    .append("（").append(entry.getValue().size()).append("）\n\n");
            for (Card card : entry.getValue()) {
                Card upgraded = CardLibrary.upgraded(card.id());
                sb.append("### `").append(card.id()).append("` ").append(card.name()).append('\n');
                sb.append("- 归属 ").append(character(card)).append(" · ").append(card.type().label())
                        .append(" · ").append(card.rarity().label()).append(" · ")
                        .append(target(card.target())).append(" · ").append(card.cost()).append(" 费");
                if (upgraded != null) {
                    sb.append("（强化 ").append(upgraded.cost()).append(" 费）");
                }
                sb.append('\n');
                if (!card.keywords().isEmpty() || (upgraded != null && !upgraded.keywords().isEmpty())) {
                    sb.append("- 关键字 ").append(keywords(card));
                    if (upgraded != null) {
                        sb.append("（强化 ").append(keywords(upgraded)).append("）");
                    }
                    sb.append('\n');
                }
                String flags = flags(card);
                if (!flags.isEmpty()) {
                    sb.append("- 标记 ").append(flags).append('\n');
                }
                sb.append("- **效果** ").append(card.description()).append('\n');
                if (upgraded != null) {
                    sb.append("- **强化效果** ").append(upgraded.description()).append('\n');
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 字段

    private static String character(Card card) {
        if (CardLibrary.isBasic(card.id())) {
            return "通用（所有角色起手都有）";
        }
        if (card.character() == null) {
            return card.type() == CardType.STATUS || card.type() == CardType.CURSE ? "（无归属）" : "无色";
        }
        return GROUP_LABELS.getOrDefault(card.character(), card.character());
    }

    private static String target(CardTarget target) {
        return switch (target) {
            case ENEMY -> "敌方单体";
            case ALL_ENEMIES -> "全体敌人";
            case SELF -> "自己";
            case NONE -> "无目标";
        };
    }

    private static String keywords(Card card) {
        if (card.keywords().isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (CardKeyword keyword : CardKeyword.values()) {
            if (card.keywords().contains(keyword)) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(keyword.label());
            }
        }
        return sb.toString();
    }

    private static String flags(Card card) {
        List<String> parts = new ArrayList<>();
        CardLibrary.CardPool pool = CardLibrary.poolOf(card);
        parts.add(switch (pool) {
            case CHARACTER -> "角色牌";
            case COLORLESS -> "无色牌";
            case GENERATED -> "生成牌";
            case STATUS -> "状态牌";
            case CURSE -> "诅咒牌";
        });
        if (CardLibrary.isBasic(card.id())) {
            parts.add("基础牌（所有角色起手都有）");
        }
        if (CardLibrary.isXCost(card.id())) {
            parts.add("X 费");
        }
        if (CardLibrary.isMultiplayerStyle(card.id())) {
            parts.add("多人玩法向（单人奖励池 / 商店不出）");
        }
        if (CardLibrary.hasUpgrade(card.id())) {
            int max = CardLibrary.maxUpgrades(card.id());
            parts.add(max >= Integer.MAX_VALUE ? "可强化（无限次）"
                    : max > 1 ? "可强化（最多 " + max + " 档）" : "可强化（1 次）");
        } else {
            parts.add("不可强化");
        }
        int price = price(card.rarity());
        if (price > 0) {
            parts.add("默认售价 " + price + "（配置项可改）");
        }
        return String.join(" · ", parts);
    }

    /**
     * 商店**默认**基础售价（与 {@code Config} 的默认值一致：普通 75 / 罕见 150 / 稀有 300 / 金 600）。
     *
     * <p>⚠️ 这几个数在 MC 层的配置里（{@code shopPriceCommon} 等），引擎（零 MC 依赖）读不到，
     * 所以这里写的是**默认值**；整合包改过配置的话以游戏内商店为准。</p>
     */
    private static int price(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> 75;
            case UNCOMMON -> 150;
            case RARE -> 300;
            case GOLD -> 600;
        };
    }

    private static String group(Card card) {
        if (CardLibrary.isBasic(card.id())) {
            return "basic";
        }
        if (CardLibrary.isGenerated(card.id())) {
            return "generated";
        }
        if (card.type() == CardType.STATUS) {
            return "status";
        }
        if (card.type() == CardType.CURSE) {
            return "curse";
        }
        return card.character() == null ? "colorless" : card.character();
    }

    private static Map<String, List<Card>> group(List<Card> cards) {
        Map<String, List<Card>> grouped = new LinkedHashMap<>();
        for (String key : GROUP_LABELS.keySet()) {
            grouped.put(key, new ArrayList<>());
        }
        for (Card card : cards) {
            grouped.computeIfAbsent(group(card), k -> new ArrayList<>()).add(card);
        }
        grouped.values().removeIf(List::isEmpty);
        for (List<Card> list : grouped.values()) {
            list.sort(Comparator.comparingInt((Card card) -> card.rarity().ordinal())
                    .thenComparingInt(Card::cost)
                    .thenComparing(Card::id));
        }
        return grouped;
    }
}
