package com.slaythecraft.potion;

import java.util.List;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.CombatContext;
import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.network.UsePotionPayload;

import net.minecraft.server.level.ServerPlayer;

/**
 * 药水的使用与发放（v0.0.2 V8；v0.4.6 支持对自己 / 队友使用）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>**负面**药水只能对敌人（喷溅贴图）；</li>
 *   <li>**正面**药水可以对自己或队友（常规贴图）；点了之后多人战斗会先开"选择目标"页面；</li>
 *   <li>需求点名"对自己使用"的**特殊**药水（不祥之瓶 / 幸运药水 / 奶桶 / 谜之炖菜）只能对自己用，
 *       并且用滞留 / 物品贴图；</li>
 *   <li>药水只能在战斗中使用（它是战斗资源）。所有校验都在服务端做，客户端只负责发"用第几个槽位、打谁"。</li>
 * </ul>
 */
public final class PotionService {

    private PotionService() {
    }

    /**
     * 使用某个槽位的药水。
     *
     * @param slot   0 基槽位
     * @param target -1 = 自己；0..n-1 = 敌人下标；{@link UsePotionPayload#ALLY_BASE} + i = 第 i 名参战玩家
     * @return 给玩家看的提示
     */
    public static String use(ServerPlayer player, int slot, int target) {
        CombatContext context = CombatManager.combatOf(player);
        if (context == null) {
            return "药水只能在战斗中使用。";
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        String potionId = data.potionAt(slot);
        if (potionId == null) {
            return "该槽位是空的。";
        }
        Potion potion = PotionLibrary.byId(potionId);
        if (potion == null) {
            // 药水表变了（例如整合包删了这瓶）：清掉这个坏槽位，别卡住玩家
            data.takePotionAt(slot);
            StcAttachments.syncTo(player);
            return "这瓶药水已经不存在了，已清空该槽位。";
        }
        Combatant user = context.combatantNamed(player.getName().getString());
        if (user == null) {
            return "你不在本场战斗中。";
        }

        Combatant victim;
        if (potion.kind().canTargetEnemy()) {
            if (target < 0 || target >= context.enemies().size()) {
                return "负面药水必须指向一名敌人（点击敌人来指定）。";
            }
            victim = context.enemies().get(target);
            if (!victim.alive()) {
                return "目标已经倒下了。";
            }
        } else {
            victim = resolveAlly(context, user, potion, target);
            if (victim == null) {
                return potion.onlySelf() ? "这瓶药水只能对自己使用。" : "找不到这个队友。";
            }
            if (!victim.alive()) {
                return victim.name() + " 已经倒下了。";
            }
        }

        potion.effect().apply(context, user, victim);
        data.takePotionAt(slot);
        StcAttachments.syncTo(player);
        CombatManager.refresh(player);
        return "使用了【" + potion.name() + "】"
                + (potion.kind().canTargetEnemy() ? " → " + victim.name()
                        : (victim == user ? "（对自己）" : " → " + victim.name())) + "。";
    }

    /** 解析"自己 / 队友"目标；非法时返回 {@code null}。 */
    private static Combatant resolveAlly(CombatContext context, Combatant user, Potion potion, int target) {
        if (target < 0 || target == Integer.MIN_VALUE) {
            return user;
        }
        if (target < UsePotionPayload.ALLY_BASE) {
            // 正面药水被指到敌人身上：当作"对自己"处理（服务端不信任客户端）
            return user;
        }
        if (potion.onlySelf()) {
            return null;
        }
        int index = target - UsePotionPayload.ALLY_BASE;
        List<Combatant> party = context.combatants();
        if (index < 0 || index >= party.size()) {
            return null;
        }
        Combatant member = party.get(index);
        return context.isParticipant(member) ? member : null;
    }

    /** 发放一瓶药水；槽位满时返回 false（栏位数含遗物加成）。 */
    public static boolean give(ServerPlayer player, Potion potion) {
        PlayerCardData data = StcAttachments.dataOf(player);
        if (!data.addPotion(potion.id(), data.potionSlots(Config.potionSlots()))) {
            return false;
        }
        StcAttachments.syncTo(player);
        return true;
    }
}
