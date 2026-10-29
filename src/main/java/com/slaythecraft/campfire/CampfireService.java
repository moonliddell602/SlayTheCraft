package com.slaythecraft.campfire;

import com.slaythecraft.chat.StcChat;
import java.util.HashMap;
import java.util.Map;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.network.CampfireStatePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 篝火服务（v0.0.3 W5）：打开"篝火准备界面" + 处理三个选项。
 *
 * <p>选项（每次只能选一个）：</p>
 * <ol>
 *   <li><b>锻造</b>：选一张卡升级（进入选卡页，服务端执行升级）；</li>
 *   <li><b>愈合</b>：回复 25% 生命；</li>
 *   <li><b>包扎</b>（多人）：为你选中的队友回复 25% 生命。</li>
 * </ol>
 *
 * <p>计数规则：选了才算；什么都没选就退出不计；但如果**已经有人选了**，其他人退出也照样计入。</p>
 */
public final class CampfireService {

    /** 同队玩家的"准备半径"（在这个范围内的队友必须都进界面）。 */
    public static final int PARTY_RADIUS = 16;

    private static final Map<String, CampfireSession> SESSIONS = new HashMap<>();

    private CampfireService() {
    }

    // ------------------------------------------------------------------ 会话

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "@" + pos.asLong();
    }

    private static CampfireSession session(ServerLevel level, BlockPos pos) {
        return SESSIONS.computeIfAbsent(key(level, pos), k -> new CampfireSession(level, pos));
    }

    private static CampfireSession sessionOf(ServerPlayer player) {
        String prefix = player.serverLevel().dimension().location() + "@";
        for (Map.Entry<String, CampfireSession> entry : SESSIONS.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue().joinedContains(player)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 打开界面

    /** 打开篝火准备界面；返回拒绝原因，或 {@code null} 表示已打开。 */
    public static String open(ServerPlayer player, BlockPos pos) {
        String blocked = blockReason(player);
        if (blocked != null) {
            return blocked;
        }
        CampfireSession session = session(player.serverLevel(), pos);
        session.join(player);
        // v0.5.0：**不再给菜单传额外数据**（多一个 writeBlockPos 的差异曾让界面开不出来）。
        // 位置只有服务端用得上，客户端界面不需要它 —— 与卡牌商店等其它界面保持同一种写法。
        final BlockPos sessionPos = pos;
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new CampfireMenu(containerId, inventory, sessionPos),
                Component.translatable("stc.campfire.title")));
        pushState(player);
        // 通知队友"有人来了"（他们界面上的等待名单要更新）
        for (ServerPlayer mate : com.slaythecraft.team.TeamResolver.onlineTeammates(player)) {
            pushState(mate);
        }
        SlayTheCraft.LOGGER.info("[STC] {} 打开了篝火准备界面 @ {}（等待名单 {} 人）",
                player.getScoreboardName(), pos, session.joinedCount());
        return null;
    }

    /** 把准备状态推给某个玩家（不在篝火界面时会被客户端忽略）。 */
    public static void pushState(ServerPlayer player) {
        CampfireSession session = sessionOf(player);
        if (session == null) {
            return;
        }
        var data = StcAttachments.dataOf(player);
        var caps = com.slaythecraft.Config.eventCaps();
        var waiting = session.waitingFor(player, PARTY_RADIUS);
        java.util.List<String> allyNames = new java.util.ArrayList<>();
        for (ServerPlayer ally : CampfireMenu.allies(player)) {
            allyNames.add(ally.getName().getString());
        }
        PacketDistributor.sendToPlayer(player,
                new CampfireStatePayload(player.containerMenu.containerId, waiting, allyNames, waiting.isEmpty()));
    }

    /**
     * 兼容旧的调试入口（{@code /stc progress campfire}）：等同于"愈合"。
     *
     * <p>v0.0.3 W5 起篝火不再"右键直接回血"，而是走准备界面选选项；这条仅供无头/指令验收。</p>
     */
    public static String rest(ServerPlayer player) {
        String blocked = blockReason(player);
        if (blocked != null) {
            return blocked;
        }
        return heal(player);
    }

    /**
     * 同队队友是否都已经进入过准备界面。
     *
     * <p>需求：多人时必须**都进入准备界面**才能进行篝火事件。客户端会把按钮置灰，
     * 这里再挡一次，避免抢跑（比如界面还没刷新就点了）。</p>
     */
    public static boolean ready(ServerPlayer player) {
        CampfireSession session = sessionOf(player);
        return session == null || session.waitingFor(player, PARTY_RADIUS).isEmpty();
    }

    /** 玩家关闭界面：处理"没选就退出"的计数规则。 */
    public static void onClosed(ServerPlayer player) {
        CampfireSession session = sessionOf(player);
        if (session == null) {
            return;
        }
        boolean countForLeaver = session.leave(player);
        if (countForLeaver) {
            // 已经有人选过了 → 这个"什么都没选就退出"的玩家也计入一次
            record(player);
            StcChat.tell(player, Component.literal(
                    "[STC] 队友已经做出了选择，本次篝火在你的记录里同样计一次数。"));
        }
        if (session.empty()) {
            SESSIONS.remove(key(session.level(), session.pos()));
        }
    }

    // ------------------------------------------------------------------ 三个选项

    /** 愈合：回复 25% 生命。 */
    public static String heal(ServerPlayer player) {
        String blocked = usableNow(player);
        if (blocked != null) {
            return blocked;
        }
        var data = StcAttachments.dataOf(player);
        var before = data.life();
        if (before.isFull()) {
            return "生命值已经是满的（" + before.describe() + "），不用愈合。";
        }
        int amount = com.slaythecraft.combat.core.CampfireRules.healAmount(before.maxHp(), healPercent());
        data.setLife(before.heal(amount));
        StcAttachments.syncTo(player);
        finish(player);
        return "愈合：回复 " + (data.hp() - before.hp()) + " 点生命（" + data.life().describe() + "）。";
    }

    /** 包扎：为选中的队友回复 25% 生命。 */
    public static String healAlly(ServerPlayer player, ServerPlayer ally) {
        String blocked = usableNow(player);
        if (blocked != null) {
            return blocked;
        }
        if (ally == null) {
            return "找不到这位队友。";
        }
        var data = StcAttachments.dataOf(ally);
        var before = data.life();
        int amount = com.slaythecraft.combat.core.CampfireRules.healAmount(before.maxHp(), healPercent());
        data.setLife(before.heal(amount));
        StcAttachments.syncTo(ally);
        finish(player);
        return "包扎：为 " + ally.getName().getString() + " 回复 " + (data.hp() - before.hp()) + " 点生命（"
                + data.life().describe() + "）。";
    }

    /** 锻造：升级指定的那张卡。 */
    public static String forge(ServerPlayer player, int cardIndex) {
        String blocked = usableNow(player);
        if (blocked != null) {
            return blocked;
        }
        var data = StcAttachments.dataOf(player);
        if (!data.canUpgradeAt(cardIndex)) {
            return "这张卡不能强化（可能已经强化过，或没有强化版）。";
        }
        var instance = data.cards().get(cardIndex);
        if (!data.upgradeCardAt(cardIndex)) {
            return "强化失败。";
        }
        StcAttachments.syncTo(player);
        finish(player);
        var definition = com.slaythecraft.combat.core.CardLibrary
                .resolve(instance.id().getPath(), 1);
        return "锻造：把【" + (definition == null ? instance.id().getPath() : definition.name()) + "】升级了。";
    }

    /**
     * 动手之前的**再检查**（v0.5.2.5 修的两个 bug）。
     *
     * <p>以前只在"打开界面"时查一次 {@link #blockReason}，界面开着的话就能一直点：
     * 于是同一个篝火能反复用、次数用完也不关。</p>
     *
     * <p>这里失败时**顺手把界面关掉**，让"次数已到上限 / 不能连用"立刻退出页面。</p>
     *
     * <p>v0.5.3.0：【灵魂篝火】的持有人**在同一个篝火界面里可以继续选**，
     * 所以这时跳过"不能连用"那一条（界面还开着就说明是同一次访问）。</p>
     *
     * @return 不能用的原因；{@code null} = 可以动手
     */
    private static String usableNow(ServerPlayer player) {
        boolean soulCampfire = hasSoulCampfire(player);
        boolean sameVisit = soulCampfire && sessionOf(player) != null;
        String blocked = blockReason(player, sameVisit);
        if (blocked != null) {
            player.closeContainer();
            return blocked;
        }
        return null;
    }

    /** 【灵魂篝火】：可在篝火处选择任意数量的选项（v0.5.3.0）。 */
    public static boolean hasSoulCampfire(ServerPlayer player) {
        return player != null && StcAttachments.dataOf(player).hasRelic("soul_campfire");
    }

    /**
     * 选完了：记一次事件 + 广播状态 + **立刻关掉界面**（v0.5.2.5）。
     *
     * <p>需求原文："应该不能连续使用 2 次篝火，使用并触发效果后直接离开 UI 并计算次数。"</p>
     *
     * <p>v0.5.3.0：【灵魂篝火】例外 —— 不关界面（可以接着选别的选项），但**只记一次账**
     * （{@link CampfireSession#markCounted} 保证）。</p>
     */
    private static void finish(ServerPlayer player) {
        CampfireSession session = sessionOf(player);
        boolean firstChoice = session == null || session.markCounted(player);
        if (firstChoice) {
            record(player);
        }
        if (session != null) {
            session.markChosen(player);
            for (ServerPlayer mate : com.slaythecraft.team.TeamResolver.onlineTeammates(player)) {
                pushState(mate);
            }
        }
        if (!hasSoulCampfire(player)) {
            // 关掉容器 → CampfireMenu.removed → onClosed（因为已经"选过"，不会再记一次账）
            player.closeContainer();
        }
    }

    private static void record(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        data.recordEvent(com.slaythecraft.combat.core.EventKind.CAMPFIRE, com.slaythecraft.Config.eventCaps());
        StcAttachments.syncTo(player);
    }

    /** 愈合/包扎回复的生命比例（需求：25%）。 */
    public static int healPercent() {
        // 复用配置项，但需求写死 25%，所以配置只作为上下限保护
        return Math.max(1, Math.min(100, com.slaythecraft.Config.campfireHealPercent()));
    }

    // ------------------------------------------------------------------ 门槛

    /** 能不能用篝火；{@code null} = 可以。 */
    public static String blockReason(ServerPlayer player) {
        return blockReason(player, false);
    }

    /**
     * 能不能用篝火。
     *
     * @param sameVisit 【灵魂篝火】：同一次访问里已经选过一次了，跳过"不能连用"那一条
     */
    public static String blockReason(ServerPlayer player, boolean sameVisit) {
        if (CombatManager.inCombat(player)) {
            return "战斗中不能休息（先结束这场战斗）。";
        }
        if (com.slaythecraft.Config.requireBinderToStart() && !com.slaythecraft.binder.BinderResolver
                .hasBinder(player)) {
            return "篝火需要：副手【卡牌收纳包】+ 主手【打火石】（当前位置："
                    + com.slaythecraft.binder.BinderResolver.binderLocation(player) + "）。"
                    + com.slaythecraft.binder.BinderResolver.equipHint();
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        var caps = com.slaythecraft.Config.eventCaps();
        if (!data.canTrigger(com.slaythecraft.combat.core.EventKind.CAMPFIRE, caps)) {
            return "本层的火堆已经用完（" + data.progress().countOf(com.slaythecraft.combat.core.EventKind.CAMPFIRE)
                    + "/" + caps.capOf(com.slaythecraft.combat.core.EventKind.CAMPFIRE) + "）。";
        }
        if (!sameVisit && data.progress().isConsecutive(com.slaythecraft.combat.core.EventKind.CAMPFIRE)) {
            return "不能连续在篝火旁休息：中间要先经历一次其它事件（战斗 / 精英 / 问号）。";
        }
        return null;
    }
}
