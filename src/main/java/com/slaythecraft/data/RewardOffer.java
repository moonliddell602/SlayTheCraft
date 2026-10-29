package com.slaythecraft.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * 一次待领取的卡牌奖励（设计文档 §9.2）。
 *
 * <p>候选在**击杀达标时就抽好并存档**，这样玩家关掉界面再打开看到的还是同样三张，
 * 也不会被读档/重连刷出更好的结果。</p>
 *
 * @param choices  候选卡牌 id（空表示还没抽：老存档的计数升级或调试发放）
 * @param golden   是否是 BOSS 的金色奖励
 * @param upgraded v0.4.6：【幸运药水】—— 领取时**直接以强化版**入手
 */
public record RewardOffer(List<ResourceLocation> choices, boolean golden, boolean upgraded) {

    public static final Codec<RewardOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("choices", List.of()).forGetter(RewardOffer::choices),
            Codec.BOOL.optionalFieldOf("golden", false).forGetter(RewardOffer::golden),
            Codec.BOOL.optionalFieldOf("upgraded", false).forGetter(RewardOffer::upgraded)
    ).apply(instance, RewardOffer::new));

    public RewardOffer {
        choices = List.copyOf(choices);
    }

    /** 还没抽候选的占位（老存档迁移 / 调试发放时用）。 */
    public static RewardOffer unrolled(boolean golden) {
        return new RewardOffer(List.of(), golden, false);
    }

    public static RewardOffer rolled(List<ResourceLocation> choices, boolean golden) {
        return new RewardOffer(choices, golden, false);
    }

    /** v0.4.6：【幸运药水】抽出来的奖励 —— 候选全部以强化版入手。 */
    public static RewardOffer rolledUpgraded(List<ResourceLocation> choices, boolean golden) {
        return new RewardOffer(choices, golden, true);
    }

    /** 候选是否已经抽好。 */
    public boolean rolled() {
        return !choices.isEmpty();
    }
}
