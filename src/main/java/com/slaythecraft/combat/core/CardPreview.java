package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 卡面数值的"实时化"（v0.0.3 W1）。
 *
 * <p>需求："在对局中，收到对应状态影响时，自己的卡牌数值没有变化，很容易导致误判。
 * 需要在玩家收到效果影响时同步改变卡牌描述中的数值。例如，收到虚弱 2 效果，玩家本来的
 * 打击：造成 6 点伤害，应该变为造成 4 点伤害。"</p>
 *
 * <p>做法：卡面文案本身是数据（"造成 6 点伤害。"），这里**解析出基础数值**，
 * 再用 {@link CombatContext#previewDamageNeutral} / {@link CombatContext#previewBlock}
 * 算出的当前数值替换它。解析失败时退回原文并追加一行"当前：…"，所以即使以后改了文案也不会算错、
 * 更不会显示成瞎编的数字。</p>
 *
 * <p>纯逻辑、零 Minecraft 依赖，因此这些替换规则都在引擎自检里钉死。</p>
 */
public final class CardPreview {

    private static final Pattern DAMAGE = Pattern.compile("造成 (\\d+) 点伤害");
    private static final Pattern BLOCK = Pattern.compile("获得 (\\d+) 点格挡");

    private CardPreview() {
    }

    /**
     * 当前状态下这张牌的描述。
     *
     * @param card  卡牌定义（基础数值从文案里解析）
     * @param owner 出牌者（力量/虚弱影响伤害，迅捷/缓慢影响格挡）
     */
    public static String liveDescription(Card card, Combatant owner) {
        if (card == null) {
            return "";
        }
        String description = card.description();
        // v0.5.2.0 A4【撞击】：伤害等于**出牌那一刻的格挡值**，文案里没有固定数字，
        // 这里把当前值补在括号里（否则卡面上看不到这张牌现在能打多少）。
        if (owner != null && description.contains("等同于你格挡值的伤害")) {
            return description + "（当前 " + owner.block() + " 点）";
        }
        // v0.5.2.4 A4-11【精准】：小刀的基础伤害会被"精准"抬高，卡面要显示实际基数
        if (owner != null && owner.shivBonus() > 0 && card.id().startsWith("shiv")
                && description.contains("点伤害。消耗。")) {
            description = description.replaceFirst("造成 \\d+ 点伤害",
                    "造成 " + (4 + owner.shivBonus()) + " 点伤害");
        }
        int baseDamage = parseDamage(description);
        int baseBlock = parseBlock(description);
        if (owner == null || (baseDamage <= 0 && baseBlock <= 0)) {
            return description;
        }

        List<String> extras = new ArrayList<>();
        if (baseDamage > 0) {
            int live = CombatContext.previewDamageNeutral(owner, baseDamage);
            // v0.5.0.7 需求："拥有活力状态时，攻击卡的伤害位置应该出现（+x）"
            // 活力是"下一张攻击牌额外 +X"的**一次性**加成，和力量那种持续加成不同，
            // 所以单独写成 "造成 17（+4）点伤害" 而不是直接算进 17 里。
            String replacement = "造成 " + live + vigorSuffix(owner) + " 点伤害";
            String from = "造成 " + baseDamage + " 点伤害";
            if (description.contains(from)) {
                description = replace(description, from, replacement);
            } else {
                extras.add(replacement);
            }
        }
        if (baseBlock > 0) {
            int live = CombatContext.previewBlock(owner, baseBlock);
            if (live != baseBlock) {
                String from = "获得 " + baseBlock + " 点格挡";
                String replacement = "获得 " + live + " 点格挡";
                if (description.contains(from)) {
                    description = replace(description, from, replacement);
                } else {
                    extras.add(replacement);
                }
            }
        }
        if (!extras.isEmpty()) {
            // 文案格式变过、替换没命中：至少把当前数值附在后面，别让玩家看到错的数字
            description = description + "（当前：" + String.join(" · ", extras) + "）";
        }
        return description;
    }

    /** 活力的卡面后缀（没有活力时是空串）。 */
    private static String vigorSuffix(Combatant owner) {
        return owner != null && owner.vigor() > 0 ? "（+" + owner.vigor() + "）" : "";
    }

    private static String replace(String description, String from, String to) {
        return description.replaceFirst(java.util.regex.Pattern.quote(from),
                java.util.regex.Matcher.quoteReplacement(to));
    }

    /** 从文案里解析基础伤害（没有则 0）。 */
    public static int parseDamage(String description) {
        return parse(DAMAGE, description);
    }

    /** 从文案里解析基础格挡（没有则 0）。 */
    public static int parseBlock(String description) {
        return parse(BLOCK, description);
    }

    private static int parse(Pattern pattern, String description) {
        if (description == null || description.isEmpty()) {
            return 0;
        }
        Matcher matcher = pattern.matcher(description);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 一个战斗单位的全部状态，逐条列出来（v0.0.3 W1：给界面显示"敌人效果 / 自己的效果"用）。
     *
     * <p>顺序固定，方便界面布局与自检比对。</p>
     *
     * <p>v0.5.0.7：需求"失去力量可以为负值，力量为负则代表每次打出的伤害减少 X" ——
     * 所以力量/迅捷/缓慢这几个**可以为负**的状态改成"非 0 就显示"（原来是只显示正数，
     * 于是失去力量之后界面上什么都看不到）。</p>
     */
    public static List<String> statusList(Combatant combatant) {
        List<String> lines = new ArrayList<>();
        if (combatant == null) {
            return lines;
        }
        addSigned(lines, "力量", combatant.strength());
        addStatus(lines, "虚弱", combatant.weak());
        addStatus(lines, "挖掘疲劳", combatant.miningFatigue());
        addStatus(lines, "易伤", combatant.vulnerable());
        addStatus(lines, "活力", combatant.vigor());
        addSigned(lines, "迅捷", combatant.dexterity());
        addSigned(lines, "缓慢", combatant.slow());
        addStatus(lines, "灼烧", combatant.burn());
        addStatus(lines, "中毒", combatant.poison());
        addStatus(lines, "凋零", combatant.decay());
        addStatus(lines, "再生", combatant.regeneration());
        addStatus(lines, "缓降", combatant.slowFall());
        addStatus(lines, "神龟", combatant.platedArmor());
        // v0.5.2.5 新状态：荆棘（可累加，被攻击时反弹）
        addStatus(lines, "荆棘", combatant.thorns());
        // v0.5.0.8 需求："蓄能作为状态也应该像其他状态那样正常显示。"
        addStatus(lines, "蓄能", combatant.charge());
        return lines;
    }

    /** 只显示正数（层数类状态）。 */
    private static void addStatus(List<String> lines, String label, int value) {
        if (value > 0) {
            lines.add(label + " " + value);
        }
    }

    /** **可以为负**的数值状态：非 0 就显示（力量/迅捷/缓慢）。 */
    private static void addSigned(List<String> lines, String label, int value) {
        if (value != 0) {
            lines.add(label + " " + value);
        }
    }
}
