package com.slaythecraft.character;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.LifePool;
import com.slaythecraft.combat.core.ProgressState;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * "重开一局"（v0.0.2 测试反馈修正；v0.4.0 改成"重开 = 重新选角色"）。
 *
 * <p>v0.4.0 需求原文："尖塔入口没有切换角色的选项。选择重开后会直接以当前角色重开。应该进行的步骤是：
 * 玩家选择重开，玩家确认重开，系统清空当前玩家所扮演的角色，玩家重新选择角色，玩家确认选择，
 * 系统确认玩家扮演的角色。"</p>
 *
 * <p>所以「重开本局」现在只做一件事：**清空一切并把角色置空**，然后玩家在入口页重新点角色卡选人
 * （选择时会重新发放那个角色的起始牌组与初始遗物）。</p>
 *
 * <p>重开会清掉：角色、卡牌、遗物、药水、生命值（回满）、三层进度、事件计数、击杀计数、待领奖励；
 * **金币重置为初始金币**（v0.4.6.3 需求原文："存在重开后金币没有被重置的问题（初始金币应当为 99）。"）。
 * </p>
 */
public final class RunService {

    private RunService() {
    }

    /**
     * 纯数据：清空本局并把角色置空（"重开 → 重新选角色"用的就是它）。
     *
     * <p>不依赖 Minecraft，便于无头自检。</p>
     */
    public static void clearRun(PlayerCardData data) {
        data.clearCards();
        data.clearRelics();
        data.clearPotions();
        data.clearKillCounters();
        data.clearPendingOffers();
        data.setProgress(ProgressState.fresh());
        data.setLife(LifePool.full(Config.maxPlayerHp()));
        // v0.4.6.3：重开必须把金币一起重置（配置项 startingGold，默认 99）
        data.setGold(Config.startingGold());
        data.setCharacter(null);
    }

    /**
     * 纯数据：把档案重置成一局全新的开局（角色 + 起始牌组 + 初始遗物）。
     */
    public static void resetForNewRun(PlayerCardData data, CharacterProfile profile) {
        clearRun(data);
        data.setCharacter(profile.id());
        data.markCharacterStarted(profile.id());
        for (ResourceLocation cardId : profile.startingDeck()) {
            data.addCard(CardInstance.of(cardId));
        }
        // v0.0.3 W3：开新局时发放角色的**初始遗物**（原版三件按原版设定，叛逃掠夺者是【臂弩】）
        if (profile.hasStartingRelic()) {
            data.addRelic(profile.startingRelic());
        }
    }

    /**
     * MC 侧：「重开本局」= **清空本局 + 清空角色**，回到"请选择角色"的状态。
     *
     * @return 给玩家看的提示
     */
    public static String clearForReselect(ServerPlayer player) {
        if (CombatManager.inCombat(player)) {
            return "战斗中不能重开本局（先打完，或 /stc combat quit 放弃）。";
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        clearRun(data);
        StcAttachments.syncTo(player);
        return "已清空本局：角色、牌组、遗物、药水、三层进度与金币都重置了（金币回到初始 " + data.gold()
                + "，独立生命值 " + data.life().describe() + "）。请在尖塔入口**重新选择角色**。";
    }
}
