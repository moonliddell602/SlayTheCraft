package com.slaythecraft.combat.core;

import java.util.Objects;
import java.util.Set;

/**
 * An immutable card definition: cost, targeting and the rules text expressed as a
 * {@link CardEffect}. Instances are shared, so a deck is just a list of these.
 *
 * <p>{@code character} 是"归属角色"的命名空间 id 字符串（例如 {@code stc:ironclad}），
 * {@code null} 表示公共卡池（无色牌）。这里用字符串而不是 {@code ResourceLocation}，是为了让整个
 * {@code combat/core} 保持零 Minecraft 依赖——引擎自检才能在裸 JVM 上跑。</p>
 *
 * <p>{@code keywords} 见 {@link CardKeyword}（v0.0.2 V4）。</p>
 */
public record Card(String id, String name, CardType type, int cost, CardTarget target,
                   String description, CardRarity rarity, String character,
                   Set<CardKeyword> keywords, Set<CardEnchantment> enchantments, CardEffect effect) {

    public Card {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(effect, "effect");
        cost = Math.max(0, cost);
        rarity = rarity == null ? CardRarity.COMMON : rarity;
        keywords = keywords == null ? Set.of() : Set.copyOf(keywords);
        enchantments = enchantments == null ? Set.of() : Set.copyOf(enchantments);
    }

    /** 绝大多数卡没有关键字、也没有附魔，保留这个便捷构造，卡池表就不用每行都写空集合。 */
    public Card(String id, String name, CardType type, int cost, CardTarget target, String description,
                CardRarity rarity, String character, CardEffect effect) {
        this(id, name, type, cost, target, description, rarity, character, Set.of(), Set.of(), effect);
    }

    /** 带关键字、没有附魔的构造（V4 起用）。 */
    public Card(String id, String name, CardType type, int cost, CardTarget target, String description,
                CardRarity rarity, String character, Set<CardKeyword> keywords, CardEffect effect) {
        this(id, name, type, cost, target, description, rarity, character, keywords, Set.of(), effect);
    }

    /** 附魔是**卡牌实例**数据：组牌时把玩家那份副本的附魔贴到定义上，生成"本场战斗用的这张卡"。 */
    public Card withEnchantments(Set<CardEnchantment> newEnchantments) {
        if (newEnchantments == null || newEnchantments.isEmpty() || newEnchantments.equals(enchantments)) {
            return this;
        }
        return new Card(id, name, type, cost, target, description, rarity, character, keywords, newEnchantments,
                effect);
    }

    /**
     * 是不是 **X 费**卡（v0.4.7.3）：费用用 {@code -1} 当哨兵值，打出时花光当前全部能量，
     * X 就是花掉的量（见 {@code CombatContext.lastX(...)}）。
     */
    public boolean xCost() {
        // 费用本身是 0（Card 会把负费用夹掉），是不是 X 费看 CardLibrary 的标记集合
        return CardLibrary.isXCost(id);
    }

    public boolean has(CardKeyword keyword) {
        return keywords.contains(keyword);
    }

    public boolean hasEnchantment(CardEnchantment enchantment) {
        return enchantments.contains(enchantment);
    }

    /** 关键字的展示文本（没有则空串）。 */
    public String keywordLabel() {
        return CardKeyword.join(keywords);
    }

    /** 附魔的展示文本（没有则空串）。 */
    public String enchantmentLabel() {
        return CardEnchantment.join(enchantments);
    }

    /** 单行描述，聊天输出与（将来的）卡牌 tooltip 共用。 */
    public String describe() {
        String keywordsText = keywordLabel();
        String enchantmentsText = enchantmentLabel();
        return name + " [" + cost + " 能量 · " + type.label() + " · " + rarity.label()
                + (keywordsText.isEmpty() ? "" : " · " + keywordsText)
                + (enchantmentsText.isEmpty() ? "" : " · 附魔:" + enchantmentsText) + "] " + description;
    }

    /** True when the card needs an explicit enemy choice. */
    public boolean needsTarget() {
        return target == CardTarget.ENEMY;
    }

    /** 是否属于某个角色的卡池（公共卡池返回 false，任何角色都能抽到）。 */
    public boolean belongsTo(String characterId) {
        return character != null && character.equals(characterId);
    }

    @Override
    public String toString() {
        return name + "(" + cost + ")";
    }
}
