package com.slaythecraft.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.slaythecraft.SlayTheCraft;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * FTB Teams 兼容层（编译期 {@code compileOnly}，运行期软依赖）。
 *
 * <p>本类里的任何类型都来自 FTB Teams，因此**只有在 {@code ftbteams} 已加载时才允许被加载**：
 * 调用方必须先过 {@link TeamResolver#ftbTeamsPresent()}，否则会抛 {@link NoClassDefFoundError}。
 * 这是 NeoForge 模组做可选依赖的标准隔离方式。</p>
 *
 * <p>API 已按 {@code ftb-teams-neoforge-2101.1.11.jar} 用 javap 校准：
 * {@code FTBTeamsAPI.api().getManager()} → {@code TeamManager}；
 * {@code arePlayersInSameTeam(UUID, UUID)}、{@code getTeamForPlayer(ServerPlayer)}；
 * {@code Team.isPartyTeam()} / {@code Team.getOnlineMembers()}。</p>
 */
final class FtbTeamsCompat {

    private FtbTeamsCompat() {
    }

    /**
     * 仅当**至少一方处于 FTB 组队（party）**时给出裁断。
     *
     * <p>FTB 会给每个玩家自动建一个「个人队伍」，那不是玩家主动组的队，所以个人队伍不参与裁决，
     * 交给原版计分板判定；一旦涉及真正的 party，FTB 的答案就是最终答案（优先级高于 /team）。</p>
     *
     * @return 空表示"FTB 不裁决"，交给下一优先级
     */
    static Optional<Boolean> decide(ServerPlayer first, ServerPlayer second) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Team firstTeam = manager.getTeamForPlayer(first).orElse(null);
        Team secondTeam = manager.getTeamForPlayer(second).orElse(null);
        boolean partyInvolved = (firstTeam != null && firstTeam.isPartyTeam())
                || (secondTeam != null && secondTeam.isPartyTeam());
        if (!partyInvolved) {
            return Optional.empty();
        }
        return Optional.of(manager.arePlayersInSameTeam(first.getUUID(), second.getUUID()));
    }

    /** FTB 视角下的在线队友（不含自己）；调用前必须确认 FTB Teams 已加载。 */
    static List<ServerPlayer> onlineTeammates(ServerPlayer player) {
        List<ServerPlayer> result = new ArrayList<>();
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return result;
        }
        UUID self = player.getUUID();
        FTBTeamsAPI.api().getManager().getTeamForPlayer(player).ifPresent(team -> {
            for (ServerPlayer member : team.getOnlineMembers()) {
                if (!member.getUUID().equals(self)) {
                    result.add(member);
                }
            }
        });
        SlayTheCraft.LOGGER.debug("[STC] FTB 队友查询：{} → {} 人", player.getScoreboardName(), result.size());
        return result;
    }
}
