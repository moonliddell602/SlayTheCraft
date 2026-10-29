package com.slaythecraft.campfire;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.slaythecraft.team.TeamResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 一次"篝火准备"（v0.0.3 W5）。
 *
 * <p>多人规则（需求原文）：</p>
 * <ul>
 *   <li>同队玩家要**都进入篝火准备界面**之后才能进行篝火事件（界面会显示谁已经进来、谁还没来）；</li>
 *   <li>什么都不选就退出 → **不计次数**；</li>
 *   <li>只要**已经有人选了**，其他人即使什么都没选就退出，也**照样计入次数**（次数是各人自己的）。</li>
 * </ul>
 *
 * <p>每个篝火方块（维度 + 坐标）一个会话；空会话会被丢弃。</p>
 */
final class CampfireSession {

    private final ServerLevel level;
    private final BlockPos pos;
    /** 此刻正开着准备界面的人（用来判断这次会话还要不要留着）。 */
    private final Set<UUID> present = new LinkedHashSet<>();
    /**
     * 这次篝火里**进来过**的人。
     *
     * <p>用它而不是"此刻在场"来算等待名单：队友只要来过一次（哪怕什么都没选就退出），
     * 就不该再被列进"还没来"，否则先来的那个人会被永远卡住。</p>
     */
    private final Set<UUID> everJoined = new LinkedHashSet<>();
    /** 已经做出选择的人。 */
    private final Set<UUID> chosen = new LinkedHashSet<>();
    /** 这次访问里已经**计过一次数**的人（v0.5.3.0【灵魂篝火】可以多选，但只记一次）。 */
    private final Set<UUID> counted = new LinkedHashSet<>();

    CampfireSession(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    ServerLevel level() {
        return level;
    }

    BlockPos pos() {
        return pos;
    }

    void join(ServerPlayer player) {
        present.add(player.getUUID());
        everJoined.add(player.getUUID());
    }

    boolean joinedContains(ServerPlayer player) {
        return everJoined.contains(player.getUUID());
    }

    /**
     * 离开：返回"是否要为他计一次数"（已经有人选过了 → 计入）。
     *
     * <p>v0.5.2.5 修：**自己已经选过的人不再计一次**。以前只看"别人选过没有"，
     * 于是甲选完（已记账）→ 乙也选完 → 甲关界面时又被记一次，白刷一次篝火次数。</p>
     */
    boolean leave(ServerPlayer player) {
        if (chosen.contains(player.getUUID())) {
            present.remove(player.getUUID());
            chosen.remove(player.getUUID());
            return false;
        }
        int otherChoosers = 0;
        for (UUID id : chosen) {
            if (!id.equals(player.getUUID())) {
                otherChoosers++;
            }
        }
        boolean shouldCount = com.slaythecraft.combat.core.CampfireRules.countOnExit(otherChoosers);
        present.remove(player.getUUID());
        chosen.remove(player.getUUID());
        return shouldCount;
    }

    void markChosen(ServerPlayer player) {
        chosen.add(player.getUUID());
    }

    /**
     * 标记"这次访问已经计过一次账"，返回这次调用**是不是第一次**（v0.5.3.0【灵魂篝火】）。
     *
     * <p>灵魂篝火允许在一次访问里选多个选项，但整次访问只该记一次数 —— 第一次返回 true，
     * 之后都返回 false。</p>
     */
    boolean markCounted(ServerPlayer player) {
        return counted.add(player.getUUID());
    }

    int joinedCount() {
        return present.size();
    }

    boolean anyoneChosen() {
        return !chosen.isEmpty();
    }

    /** 这个玩家所在的同队队伍里，还没进入准备界面的其他玩家（用于"等待"提示）。 */
    List<String> waitingFor(ServerPlayer player, int radius) {
        List<String> waiting = new ArrayList<>();
        double radiusSqr = (double) radius * radius;
        for (ServerPlayer mate : TeamResolver.onlineTeammates(player)) {
            if (mate.getUUID().equals(player.getUUID())) {
                continue;
            }
            if (mate.distanceToSqr(player) > radiusSqr || mate.level() != level) {
                continue;
            }
            if (!everJoined.contains(mate.getUUID())) {
                waiting.add(mate.getName().getString());
            }
        }
        return waiting;
    }

    boolean empty() {
        return present.isEmpty();
    }
}
