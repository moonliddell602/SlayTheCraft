package com.slaythecraft.data;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.slaythecraft.combat.core.CardEnchantment;

import net.minecraft.resources.ResourceLocation;

/**
 * 玩家档案里的一张具体卡牌（设计文档 §5.1，v0.0.2 V6 起带附魔）。
 *
 * <p>只存"哪张卡 + 强化次数 + 附魔"，卡牌的数值/文案全部由卡牌定义提供；
 * 因此调整卡牌数值不会影响已存档的玩家数据。</p>
 *
 * @param id           卡牌定义 id，例如 {@code stc:strike}
 * @param upgrades     强化层数（0 = 未强化），本阶段占位，用于后续锻造系统
 * @param enchantments 附魔（同一张卡的不同副本可以附不同的魔）
 */
public record CardInstance(ResourceLocation id, int upgrades, Set<CardEnchantment> enchantments) {

    /** 附魔列表按名字存，读到不认识的名字就跳过（数据包删掉某个附魔也不会坏档）。 */
    private static final Codec<Set<CardEnchantment>> ENCHANTMENTS = Codec.STRING.listOf()
            .xmap(CardInstance::parseEnchantments, CardInstance::encodeEnchantments);

    public static final Codec<CardInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(CardInstance::id),
            Codec.INT.optionalFieldOf("upgrades", 0).forGetter(CardInstance::upgrades),
            ENCHANTMENTS.optionalFieldOf("enchantments", Set.of()).forGetter(CardInstance::enchantments)
    ).apply(instance, CardInstance::new));

    public CardInstance {
        upgrades = Math.max(0, upgrades);
        enchantments = enchantments == null ? Set.of() : Set.copyOf(enchantments);
    }

    public static CardInstance of(ResourceLocation id) {
        return new CardInstance(id, 0, Set.of());
    }

    public static CardInstance of(ResourceLocation id, Set<CardEnchantment> enchantments) {
        return new CardInstance(id, 0, enchantments);
    }

    public boolean upgraded() {
        return upgrades > 0;
    }

    public boolean enchanted() {
        return !enchantments.isEmpty();
    }

    /** 附魔展示文本（没有则空串）。 */
    public String enchantmentLabel() {
        return CardEnchantment.join(enchantments);
    }

    /** 强化一次后返回的新实例（不可变，便于存档只记录增量）。 */
    public CardInstance withUpgrade() {
        return new CardInstance(id, upgrades + 1, enchantments);
    }

    /** 追加一个附魔后返回的新实例。 */
    public CardInstance withEnchantment(CardEnchantment enchantment) {
        if (enchantments.contains(enchantment)) {
            return this;
        }
        Set<CardEnchantment> updated = EnumSet.noneOf(CardEnchantment.class);
        updated.addAll(enchantments);
        updated.add(enchantment);
        return new CardInstance(id, upgrades, updated);
    }

    private static Set<CardEnchantment> parseEnchantments(List<String> names) {
        Set<CardEnchantment> parsed = EnumSet.noneOf(CardEnchantment.class);
        for (String name : names) {
            CardEnchantment enchantment = CardEnchantment.byName(name);
            if (enchantment != null) {
                parsed.add(enchantment);
            }
        }
        return parsed;
    }

    private static List<String> encodeEnchantments(Set<CardEnchantment> enchantments) {
        return enchantments.stream().map(CardEnchantment::name).toList();
    }
}
