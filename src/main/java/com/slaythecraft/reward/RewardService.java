package com.slaythecraft.reward;

import com.slaythecraft.chat.StcChat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.mc.MobTiers;
import com.slaythecraft.combat.core.MobTier;
import com.slaythecraft.data.CardIds;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.RewardOffer;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/**
 * 卡牌奖励服务（设计文档 §9.2 / §8.2）：击杀达标发奖、抽三选一、领取或跳过。
 *
 * <p>候选在**达标那一刻**就抽好并存进档案，所以关掉界面再打开看到的还是同样三张，
 * 也不会靠读档重连刷出更好的结果。</p>
 */
public final class RewardService {

    private RewardService() {
    }

    /** 按玩家当前角色抽一次候选（金色奖励只出金色卡）。 */
    public static RewardOffer rollOffer(ServerPlayer player, boolean golden) {
        PlayerCardData data = StcAttachments.dataOf(player);
        String characterId = data.character() == null ? null : data.character().toString();
        Random random = new Random(player.getRandom().nextLong());
        // v0.5.1.4：多人卡牌只在多人模式进池 —— 用"这一场战斗的参战人数"判断，
        // 不在战斗里（例如指令发奖）就按单人算。
        List<Card> picked = RewardRoller.roll(characterId, golden, Config.rewardChoices(), random,
                com.slaythecraft.combat.mc.CombatManager.partySize(player));
        List<ResourceLocation> ids = new ArrayList<>(picked.size());
        for (Card card : picked) {
            ids.add(CardIds.of(card));
        }
        // v0.4.6【幸运药水】：这里的"幸运"只作用于**下一次**奖励，抽完就用掉
        if (data.consumeLuckyReward()) {
            StcChat.tell(player, Component.literal(
                            "[STC] 【幸运药水】生效：本次奖励的所有候选都会以强化版入手。")
                    .withStyle(ChatFormatting.GOLD));
            StcAttachments.syncTo(player);
            return RewardOffer.rolledUpgraded(ids, golden);
        }
        return RewardOffer.rolled(ids, golden);
    }

    /**
     * 记一次击杀（**纯数据**，便于无头自检）。
     *
     * @return 是否刚好达标（达标时已清空该种类的计数，调用方接着发奖）
     */
    public static boolean recordKill(PlayerCardData data, MobTier tier, ResourceLocation typeId) {
        int count = data.recordKill(typeId);
        if (count < MobTiers.threshold(tier)) {
            return false;
        }
        data.clearKillProgress(typeId);
        return true;
    }

    /**
     * 战斗内击杀（测试反馈修正：奖励在战斗里根本触发不了）。
     *
     * <p>原因：引擎处决怪物走的是 {@code mob.kill()}（无来源伤害），原版 {@code getKillCredit()}
     * 是空的，所以 {@code KillTracker} 从来数不到战斗击杀。现在由引擎把"最后一击是谁打的"
     * （{@link com.slaythecraft.combat.core.Combatant#recordDamageFrom}）传下来，直接在这里记账。</p>
     *
     * @return 是否达标发奖
     */
    public static boolean recordCombatKill(ServerPlayer killer, MobTier tier, ResourceLocation typeId) {
        if (killer == null) {
            return false;
        }
        PlayerCardData data = StcAttachments.dataOf(killer);
        int count = data.recordKill(typeId);
        int threshold = MobTiers.threshold(tier);
        if (count >= threshold) {
            data.clearKillProgress(typeId);
            grantKillReward(killer, tier, typeId);
            return true;
        }
        // 让玩家看得见"真的在计数"（测试反馈：以前完全没反馈）
        StcChat.tell(killer, Component.literal("[STC] 击杀进度：" + shortName(typeId) + " " + count + "/"
                + threshold + "（" + tier.label() + "档）").withStyle(ChatFormatting.GRAY));
        return false;
    }

    /** 击杀达标：给金币 + 生成奖励 + 通知玩家（奖励界面会在战斗结束后立刻弹出）。 */
    public static void grantKillReward(ServerPlayer player, MobTier tier, ResourceLocation typeId) {
        PlayerCardData data = StcAttachments.dataOf(player);
        int gold = MobTiers.gold(tier);
        boolean golden = MobTiers.goldenReward(tier);
        data.addGold(gold);
        data.addOffer(rollOffer(player, golden));
        StcAttachments.syncTo(player);
        if (Config.rewardAnnounceInChat()) {
            StcChat.tell(player, Component.literal("[STC] " + tier.label() + "奖励就绪（"
                            + shortName(typeId) + " 已累计达标）：+" + gold + " 金币"
                            + (golden ? "，金色卡牌三选一" : "，卡牌三选一")
                            + "　战斗结束后立刻弹出三选一（待领 " + data.pendingRewards() + " 次）")
                    .withStyle(golden ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        }
    }

    /**
     * 战斗胜利奖励（v0.2.1 测试反馈修正；v0.5.0.20 改为"进结算页领取"）。
     *
     * <p>反馈原文（v0.2.1）："存在创造模式无法正常触发获得卡牌奖励的情况。调整思路：
     * 在战斗结束后立刻进入奖励界面。"</p>
     *
     * <p>v0.5.0.20 需求（战斗结算页）："领取 X 金币（X 为在战斗中获得的金币）/
     * 领取遗物：X（如果有则显示此选项，没有掉落则不显示）/ 获得一次卡牌奖励 / 跳过奖励"。
     * 所以金币**不再立刻进余额**，而是暂存进档案（{@code beginSettlement}），
     * 等玩家在结算页点「领取」才真正入账；同时在这里决定本场有没有遗物掉落。</p>
     *
     * <p>遗物掉落规则（同一批需求）："现在，常规战斗的结算有百分之十的概率掉落一件非特殊遗物。"
     * —— 只有**常规**战斗（{@link MobTier#NORMAL}）才掷这个骰子，概率见
     * {@link Config#normalBattleRelicChance()}；"非特殊遗物"用
     * {@link com.slaythecraft.combat.core.RelicLibrary#eliteDroppable(String)} 过滤
     * （排除角色初始遗物、事件专属遗物、"只能商店"与"藏宝箱/商店"那几件），并且只掉**未拥有**的。</p>
     */
    public static void grantVictoryReward(ServerPlayer player, MobTier tier) {
        if (!Config.rewardPerVictory()) {
            return;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        int gold = MobTiers.gold(tier);
        boolean golden = MobTiers.goldenReward(tier);
        data.addOffer(rollOffer(player, golden));

        // 常规战斗才有 10% 的遗物掉落
        String relicId = rollRelicDrop(player, data, tier);
        data.beginSettlement(gold, relicId);
        StcAttachments.syncTo(player);

        if (Config.rewardAnnounceInChat()) {
            StcChat.tell(player, Component.literal("[STC] 战斗胜利奖励（" + tier.label() + "档）：+" + gold
                            + " 金币" + (golden ? "，金色卡牌三选一" : "，卡牌三选一")
                            + (relicId == null ? "" : "，掉落遗物【" + relicDisplayName(relicId) + "】")
                            + "　战斗结算页面已打开")
                    .withStyle(golden ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        }
    }

    /**
     * 掷"常规战斗掉落遗物"这一骰子（v0.5.0.20）。
     *
     * <p>纯逻辑、便于自检：只有常规档、只有未拥有的可掉落遗物、概率来自配置。</p>
     *
     * @return 掉落的遗物 id；没掉返回 {@code null}
     */
    public static String rollRelicDrop(ServerPlayer player, PlayerCardData data, MobTier tier) {
        if (tier != MobTier.NORMAL) {
            return null;
        }
        double chance = Config.normalBattleRelicChance();
        if (chance <= 0.0D || player.getRandom().nextDouble() >= chance) {
            return null;
        }
        var relic = com.slaythecraft.combat.core.RelicLibrary.randomUnowned(
                new Random(player.getRandom().nextLong()), data.relics());
        // randomUnowned 会从全部遗物里挑：再过滤一次"非特殊 + 未拥有"，保证不会掉初始/事件遗物
        // （v0.5.3.1：改用 eliteDroppable —— "只能商店"与"藏宝箱/商店"那几件不进战斗掉落）
        if (relic == null || !com.slaythecraft.combat.core.RelicLibrary.eliteDroppable(relic.id())
                || data.hasRelic(relic.id())) {
            return null;
        }
        return relic.id();
    }

    /** 遗物的显示名（拿不到就退回 id）。 */
    private static String relicName(String relicId) {
        var relic = com.slaythecraft.combat.core.RelicLibrary.byId(relicId);
        return relic == null ? relicId : relic.name();
    }

    /** 兼容旧调用（没有实体类型上下文时用"？"）。 */
    public static void grantKillReward(ServerPlayer player, MobTier tier) {
        grantKillReward(player, tier, null);
    }

    /**
     * 战斗结束后立刻进入奖励界面（测试反馈：改成战斗结束直接弹，而不是回收纳包点领取）。
     *
     * @return 是否打开了界面
     */
    public static boolean openPendingRewards(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        if (data.pendingRewards() <= 0) {
            return false;
        }
        openMenu(player);
        return true;
    }

    /** 击杀提示里用的短名字（去掉 {@code minecraft:} 前缀）。 */
    private static String shortName(ResourceLocation typeId) {
        if (typeId == null) {
            return "?";
        }
        String path = typeId.getPath();
        return path.startsWith("minecraft:") ? path.substring("minecraft:".length()) : path;
    }

    public static void openMenu(ServerPlayer player) {
        StcAttachments.syncTo(player);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new RewardMenu(containerId, inventory, RewardMenu.Mode.CHOICE),
                Component.translatable("stc.reward.title")));
    }

    // ------------------------------------------------------------------ v0.5.0.20 战斗结算页

    /**
     * 打开**战斗结算页**（需求："新UI:战斗结算页面"）。
     *
     * <p>页面上有四件事：领取本场金币、领取掉落遗物（没掉就不显示这一项）、
     * 获得一次卡牌奖励（跳到三选一）、跳过奖励。</p>
     */
    public static void openResultMenu(ServerPlayer player) {
        StcAttachments.syncTo(player);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new RewardMenu(containerId, inventory, RewardMenu.Mode.RESULT),
                Component.translatable("stc.reward.result.title")));
    }

    /** 战斗结束时调用：有结算内容就开结算页，否则按老行为直接开三选一。 */
    public static boolean openSettlement(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        if (data.settlementOpen()) {
            openResultMenu(player);
            return true;
        }
        return openPendingRewards(player);
    }

    /** 从结算页进入**卡牌三选一**。 */
    public static void openChoiceMenu(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        if (data.pendingRewards() <= 0) {
            return;
        }
        openMenu(player);
    }

    /** 还有没有待领的卡牌奖励。 */
    public static boolean hasCardReward(ServerPlayer player) {
        return StcAttachments.dataOf(player).pendingRewards() > 0;
    }

    /** 遗物的显示名（拿不到就退回 id）—— 结算页与聊天提示共用。 */
    public static String relicDisplayName(String relicId) {
        if (relicId == null) {
            return "?";
        }
        var relic = com.slaythecraft.combat.core.RelicLibrary.byId(relicId);
        return relic == null ? relicId : relic.name();
    }

    /**
     * 领取队首奖励里的第 {@code choiceIndex} 张候选。
     *
     * @return 给玩家看的提示；成功时已写入卡牌并移除该奖励
     */
    public static String claim(ServerPlayer player, int choiceIndex) {
        PlayerCardData data = StcAttachments.dataOf(player);
        RewardOffer offer = data.firstOffer();
        if (offer == null) {
            return "没有待领取的奖励。";
        }
        List<ResourceLocation> choices = new ArrayList<>(offer.choices());
        if (choices.isEmpty()) {
            // 老存档迁移 / 调试发放的"未抽候选"奖励，这里补抽一次
            RewardOffer rolled = rollOffer(player, offer.golden());
            data.setOffer(0, rolled);
            choices = new ArrayList<>(rolled.choices());
        }
        if (choiceIndex < 0 || choiceIndex >= choices.size()) {
            return "候选编号无效（当前 " + choices.size() + " 张）。";
        }
        ResourceLocation picked = choices.get(choiceIndex);
        Card definition = CardLibrary.byId(picked.getPath());
        if (definition == null) {
            // 卡池变了（数据包删卡）：跳过这个坏候选，不消耗奖励
            data.claimOffer(0);
            StcAttachments.syncTo(player);
            return "这张候选已经不存在（" + picked + "），已跳过该次奖励。";
        }
        // v0.4.6【幸运药水】：强化版奖励直接以"已强化"入手
        boolean upgraded = offer.upgraded() && CardLibrary.canUpgrade(picked.getPath());
        if (upgraded) {
            data.addCard(CardInstance.of(picked).withUpgrade());
        } else {
            data.addCard(CardInstance.of(picked));
        }
        data.claimOffer(0);
        StcAttachments.syncTo(player);
        return "已领取【" + definition.name() + (upgraded ? "+" : "") + "】，收纳包现有 " + data.cardCount()
                + " 张卡牌，待领 " + data.pendingRewards() + " 次。";
    }

    /** 跳过队首奖励（只消耗次数，不入卡）。 */
    public static String skip(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        if (!data.claimOffer(0)) {
            return "没有待领取的奖励。";
        }
        StcAttachments.syncTo(player);
        return "已跳过本次卡牌奖励（剩余待领 " + data.pendingRewards() + "）。";
    }

    /** 调试/验收用：直接塞一个待领奖励。 */
    public static String grantForDebug(ServerPlayer player, boolean golden) {
        PlayerCardData data = StcAttachments.dataOf(player);
        data.addOffer(rollOffer(player, golden));
        StcAttachments.syncTo(player);
        return "已发放 1 次" + (golden ? "金色" : "") + "奖励（待领 " + data.pendingRewards() + " 次）。";
    }
}
