package com.slaythecraft.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.fml.ModList;

/**
 * 全模组唯一的"是不是自己人"判定入口（设计文档 §10.1）。
 *
 * <p>优先级：</p>
 * <ol>
 *   <li>装了 FTB Teams 且**至少一方在 FTB party 里** → 以 FTB 为准（不再看 /team）</li>
 *   <li>否则看原版计分板队伍（{@code /team}）</li>
 *   <li>都没有队伍 → 只有自己算自己人</li>
 * </ol>
 *
 * <p>多人玩法（≤4 人）的准备位、加入校验、奖励归属都会走这里，避免判定逻辑散落各处。</p>
 */
public final class TeamResolver {
    private static final String FTB_TEAMS_MOD_ID = "ftbteams";

    private TeamResolver() {
    }

    /** FTB Teams 是否在场（决定能否触碰 {@link FtbTeamsCompat}）。 */
    public static boolean ftbTeamsPresent() {
        return ModList.get().isLoaded(FTB_TEAMS_MOD_ID);
    }

    /** 两名玩家是否算同一队。 */
    public static boolean areTeammates(ServerPlayer first, ServerPlayer second) {
        if (first.getUUID().equals(second.getUUID())) {
            return true;
        }
        if (first.getServer() != second.getServer()) {
            return false;
        }
        if (ftbTeamsPresent()) {
            Optional<Boolean> verdict = FtbTeamsCompat.decide(first, second);
            if (verdict.isPresent()) {
                return verdict.get();
            }
        }
        return sameScoreboardTeam(first, second);
    }

    /** 指定玩家的在线队友（不含自己）。 */
    public static List<ServerPlayer> onlineTeammates(ServerPlayer player) {
        if (ftbTeamsPresent()) {
            List<ServerPlayer> ftb = FtbTeamsCompat.onlineTeammates(player);
            if (!ftb.isEmpty()) {
                return ftb;
            }
        }
        return scoreboardTeammates(player);
    }

    private static boolean sameScoreboardTeam(ServerPlayer first, ServerPlayer second) {
        PlayerTeam team = first.getScoreboard().getPlayersTeam(first.getScoreboardName());
        return team != null && team.getPlayers().contains(second.getScoreboardName());
    }

    private static List<ServerPlayer> scoreboardTeammates(ServerPlayer player) {
        List<ServerPlayer> result = new ArrayList<>();
        PlayerTeam team = player.getScoreboard().getPlayersTeam(player.getScoreboardName());
        if (team == null || player.getServer() == null) {
            return result;
        }
        for (String name : team.getPlayers()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayerByName(name);
            if (member != null && !member.getUUID().equals(player.getUUID())) {
                result.add(member);
            }
        }
        return result;
    }
}
