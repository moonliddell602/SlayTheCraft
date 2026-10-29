package com.slaythecraft.dev;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.server.level.ServerPlayer;

/**
 * **开发者模式**（v0.5.1.0）。
 *
 * <p>需求原文："新增：开发者模式 —— 为开发者预留的指令。使用指令启用开发者模式后：
 * 每场战斗开始时获得 9999 层缓冲和 9999 力量。可以在图鉴里直接向卡组添加卡牌。
 * 可以直接在查看卡组页面删牌。获得 9999999 金币。"</p>
 *
 * <h2>状态放在哪里</h2>
 *
 * <p>开关**只存在内存里**（按玩家 UUID），不落盘：这样"开发者模式"不会跟着存档走，
 * 发给别人测试时也不会莫名其妙地带着它。代价是重启服务器要重新开一次 —— 对开发用途正好。</p>
 *
 * <h2>四项便利分别落在哪</h2>
 *
 * <ul>
 *   <li><b>开局 9999 缓冲 + 9999 力量</b>：{@link #applyBattleStartBuffs(Combatant)}，
 *       由 {@code CombatManager.beginEncounter} 在 {@code context.start()} 之后调用；</li>
 *   <li><b>图鉴里直接加卡</b>：{@link #canEditDeckFromCodex(ServerPlayer)} + 客户端在卡牌图鉴
 *       详情页显示「加入牌组」按钮；</li>
 *   <li><b>查看卡组页面直接删牌</b>：{@link #canDeleteFromDeckView(ServerPlayer)} + 客户端在
 *       "查看我的牌组"页面显示「删除」按钮；</li>
 *   <li><b>9999999 金币</b>：{@link #enable(ServerPlayer)} 时一次性发放，之后每次登录也补一次。</li>
 * </ul>
 */
public final class DevMode {

    /** 开发者模式应发的金币数（需求指定 9999999）。 */
    public static final int DEV_GOLD = 9_999_999;
    /** 开局发放的缓冲（缓降）层数（需求指定 9999）。 */
    public static final int DEV_BUFFER = 9999;
    /** 开局发放的力量层数（需求指定 9999）。 */
    public static final int DEV_STRENGTH = 9999;

    /** 已开启开发者模式的玩家（内存态，不落盘）。 */
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private DevMode() {
    }

    /** 是否已开启。 */
    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.getUUID());
    }

    /** 开关状态（按 UUID，便于纯逻辑自检）。 */
    public static boolean isEnabled(UUID id) {
        return id != null && ENABLED.contains(id);
    }

    /**
     * 开启开发者模式，并立刻发放金币。
     *
     * @return 给玩家看的提示
     */
    public static String enable(ServerPlayer player) {
        ENABLED.add(player.getUUID());
        PlayerCardData data = StcAttachments.dataOf(player);
        data.setGold(DEV_GOLD);
        StcAttachments.syncTo(player);
        syncTo(player);
        return "已开启**开发者模式**：金币 " + DEV_GOLD
                + "；每场战斗开始时获得 " + DEV_BUFFER + " 层缓冲与 " + DEV_STRENGTH + " 力量；"
                + "卡牌图鉴可直接加入牌组、查看牌组页可直接删牌。";
    }

    /** 关闭开发者模式（**不回收**已经发出去的金币与卡牌）。 */
    public static String disable(ServerPlayer player) {
        ENABLED.remove(player.getUUID());
        syncTo(player);
        return "已关闭开发者模式。";
    }

    /**
     * 把开关状态同步给客户端（v0.5.1.2）。
     *
     * <p>客户端据此决定"图鉴详情页要不要显示「加入牌组」、查看牌组页要不要显示「删除」"。
     * 这只是**显示用**的信息，真正的校验始终在 {@link DevDeckService#handle} 里再做一次。</p>
     */
    public static void syncTo(ServerPlayer player) {
        if (player != null) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new com.slaythecraft.network.DevStatePayload(isEnabled(player)));
        }
    }

    /**
     * 战斗开始时的开发者加成（需求："每场战斗开始时获得 9999 层缓冲和 9999 力量"）。
     *
     * <p>放在 {@code context.start()} **之后**调用：那时玩家战斗员已经建好。</p>
     */
    public static void applyBattleStartBuffs(Combatant combatant) {
        if (combatant == null) {
            return;
        }
        combatant.addSlowFall(DEV_BUFFER);
        combatant.addStrength(DEV_STRENGTH);
    }

    /** 图鉴里能不能直接加卡。 */
    public static boolean canEditDeckFromCodex(ServerPlayer player) {
        return isEnabled(player);
    }

    /** 查看牌组页面能不能直接删牌。 */
    public static boolean canDeleteFromDeckView(ServerPlayer player) {
        return isEnabled(player);
    }

    /** 生产环境提示：开发者模式开着的时候提醒一句（避免忘记关）。 */
    public static String warningLine() {
        return "⚠ 开发者模式已开启：每个角色开局会白送金币与超额状态，测试结束记得关掉"
                + "（/stc dev off）。";
    }
}
