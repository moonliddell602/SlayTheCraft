package com.slaythecraft.data;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.resources.ResourceLocation;

/**
 * 把玩家档案里的卡牌实例翻译成战斗引擎的牌组（设计文档 §11）。
 *
 * <p>档案只存 {@code (id, upgrades)}，真正的数值来自卡牌定义；遇到不认识的 id（例如数据包删了某张卡）
 * 只记下来并跳过，**不让整场战斗崩掉**。</p>
 */
public final class DeckBuilder {

    /** @param deck 可用于战斗的卡牌（允许重复） @param unknown 档案里无法识别的卡牌 id */
    public record Result(List<Card> deck, List<ResourceLocation> unknown) {
    }

    private DeckBuilder() {
    }

    public static Result fromArchive(List<CardInstance> instances) {
        List<Card> deck = new ArrayList<>(instances.size());
        List<ResourceLocation> unknown = new ArrayList<>();
        for (CardInstance instance : instances) {
            if (!CardIds.isKnown(instance.id())) {
                unknown.add(instance.id());
                continue;
            }
            Card definition = CardLibrary.resolve(instance.id().getPath(), instance.upgrades());
            if (definition == null) {
                unknown.add(instance.id());
                continue;
            }
            // V6：把该副本的附魔贴到定义上，得到"本场战斗用的这张卡"
            deck.add(definition.withEnchantments(instance.enchantments()));
        }
        return new Result(List.copyOf(deck), List.copyOf(unknown));
    }
}
