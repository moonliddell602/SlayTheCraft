package com.slaythecraft.combat.mc;

import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.MobTier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 怪物分档（设计文档 §9.1）：用**实体类型标签**定义精英与 BOSS，整合包可以自行增删，不需要改代码。
 *
 * <pre>
 * data/stc/tags/entity_type/mob_tier/elite.json
 * data/stc/tags/entity_type/mob_tier/boss.json
 * </pre>
 *
 * <p>普通档不需要标签：没被上面两个标签收录、且属于敌对生物（{@code monster.Enemy}）就是普通。
 * 这样打牛打羊不会产生奖励。</p>
 *
 * <p>分档本身是一组枚举 {@link MobTier}（在 {@code combat/core}，便于无头自检），
 * 本类只负责"实体 → 分档"这一步。</p>
 */
public final class MobTiers {

    public static final TagKey<EntityType<?>> ELITE =
            TagKey.create(Registries.ENTITY_TYPE, id("mob_tier/elite"));
    public static final TagKey<EntityType<?>> BOSS =
            TagKey.create(Registries.ENTITY_TYPE, id("mob_tier/boss"));

    private MobTiers() {
    }

    public static MobTier tierOf(LivingEntity entity) {

        EntityType<?> type = entity.getType();
        if (type.is(BOSS)) {
            return MobTier.BOSS;
        }
        if (type.is(ELITE)) {
            return MobTier.ELITE;
        }
        return entity instanceof net.minecraft.world.entity.monster.Enemy ? MobTier.NORMAL : MobTier.NONE;
    }

    /**
     * 按**实体类型**分档（v0.4.6 图鉴用：图鉴没有实体实例）。
     *
     * <p>只看标签：BOSS / 精英标签命中的就是对应档，其余按"是不是敌对生物类型"返回
     * {@link MobTier#NORMAL}；两者都不是（牛、羊、村民…）返回 {@link MobTier#NONE}。</p>
     */
    public static MobTier tierOf(EntityType<?> type) {
        if (type == null) {
            return MobTier.NONE;
        }
        if (type.is(BOSS)) {
            return MobTier.BOSS;
        }
        if (type.is(ELITE)) {
            return MobTier.ELITE;
        }
        if (type.getCategory() == MobCategory.MONSTER) {
            return MobTier.NORMAL;
        }
        // 有些 BOSS（嗅探兽 / 熊猫）不是 MONSTER，但已在上面被 BOSS 标签拦下
        return MobTier.NONE;
    }

    /** 该档位需要的击杀数（同种类分别累计）。 */
    public static int threshold(MobTier tier) {
        return switch (tier) {
            case NORMAL -> Config.rewardThresholdNormal();
            case ELITE -> Config.rewardThresholdElite();
            case BOSS -> Config.rewardThresholdBoss();
            case NONE -> Integer.MAX_VALUE;
        };
    }

    /**
     * "小队"规模（v0.0.2 V10）：某些精英按需求是成队出现的。
     *
     * <p>目前只有猪灵蛮兵：需求写明"猪灵蛮兵小队(2 只)"。它也是"精英战最多 1 只"规则的
     * **唯一例外**（可用配置 {@code eliteSquad} 关掉）。</p>
     */
    public static int squadSize(EntityType<?> type) {
        return type == EntityType.PIGLIN_BRUTE ? 2 : 1;
    }

    /** 敌对生物（原版 {@code monster.Enemy}）或已经被分档到精英/BOSS 的生物都能开战。 */
    public static boolean battleEligible(LivingEntity entity) {
        return tierOf(entity) != MobTier.NONE;
    }

    /** 该档位达标给的金币。 */
    public static int gold(MobTier tier) {
        return switch (tier) {
            case NORMAL -> Config.goldNormal();
            case ELITE -> Config.goldElite();
            case BOSS -> Config.goldBoss();
            case NONE -> 0;
        };
    }

    /** 是否必出金色（BOSS + 配置开启）。 */
    public static boolean goldenReward(MobTier tier) {
        return tier == MobTier.BOSS && Config.bossRewardGoldenOnly();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, path);
    }
}
