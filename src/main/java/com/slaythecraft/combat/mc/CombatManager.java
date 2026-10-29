package com.slaythecraft.combat.mc;

import com.slaythecraft.chat.StcChat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.BinderResolver;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.character.RunService;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardPreview;
import com.slaythecraft.combat.core.CardType;
import com.slaythecraft.combat.core.CombatContext;
import com.slaythecraft.combat.core.CombatLog;
import com.slaythecraft.combat.core.CombatPhase;
import com.slaythecraft.combat.core.CombatRules;
import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.combat.core.Enemy;
import com.slaythecraft.combat.core.EncounterRules;
import com.slaythecraft.combat.core.EnemyFormation;
import com.slaythecraft.combat.core.EventCaps;
import com.slaythecraft.combat.core.EventKind;
import com.slaythecraft.combat.core.FloorBoss;
import com.slaythecraft.combat.core.FloorPools;
import com.slaythecraft.combat.core.LifePool;
import com.slaythecraft.combat.core.MobTier;
import com.slaythecraft.combat.core.PlayerState;
import com.slaythecraft.combat.core.ProgressState;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.combat.core.StandoffLayout;
import com.slaythecraft.data.CardIds;
import com.slaythecraft.data.DeckBuilder;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.network.BattleStatePayload;
import com.slaythecraft.network.BattleView;
import com.slaythecraft.reward.RewardService;
import com.slaythecraft.team.TeamResolver;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Bridges the Minecraft world and the {@link CombatContext} engine.
 *
 * <p><b>多人（设计文档 §10.2）</b>：一场遭遇战由 1..4 名同队玩家共享，每个参与者的 UUID 都映射到
 * **同一个** {@link CombatContext}；敌人是共享的，玩家各自拥有能量/手牌/抽牌堆/格挡。所有战斗都是
 * 服务端权威，客户端只收聊天文本与行动栏摘要——因此这一阶段依然不需要额外的 GUI 资源。</p>
 */
public final class CombatManager {
    /**
     * 敌人行动的间隔（tick）：每只敌人之间隔这么久，动画才看得清
     * （v0.4.0 是 12 tick；v0.5.0 按反馈延长到 {@link BattleAnim#ENEMY_STEP_MS} = 1.2 秒）。
     */
    private static final int ENEMY_STEP_TICKS =
            (int) Math.max(1L, com.slaythecraft.combat.core.BattleAnim.ENEMY_STEP_MS / 50L);

    private static final Map<UUID, CombatContext> COMBATS = new HashMap<>();
    private static final Map<UUID, List<MobCombatant>> TRACKED = new HashMap<>();
    /** 引擎处决的怪（UUID → 当时的 gameTime）：让原版击杀事件跳过它们，避免重复计数。 */
    private static final Map<UUID, Long> ENGINE_KILLS = new HashMap<>();
    /** 引擎击杀记录的保鲜期（tick）：够 LivingDeathEvent 在同一 tick 内消费掉即可。 */
    private static final long ENGINE_KILL_WINDOW = 200L;
    /** 行动栏文字上限（超长会顶出屏幕）。 */
    private static final int ACTION_BAR_LIMIT = 110;
    /**
     * 召唤落点离召唤者的**最大水平距离**（格）。
     *
     * <p>v0.5.0.5：原来召唤位置只由 {@code combatants.size()} 推出来，死者也会把它推大，
     * 结果随从飞到屏幕外。现在按存活数排位之外，再加这道硬上限兜底。</p>
     */
    private static final double SUMMON_MAX_DISTANCE = 4.2D;
    /** 顺带冻结的旁观怪物（按遭遇战记，战斗结束时统一解锁）。 */
    private static final Map<CombatContext, List<Mob>> BYSTANDERS = new HashMap<>();
    /** v0.4.0：下一次推进"下一只敌人"的最早 gameTime（多人时只有第一个到的玩家会推进）。 */
    private static final Map<CombatContext, Long> NEXT_ENEMY_STEP = new HashMap<>();
    /** v0.4.4：每场战斗的"房主"（怪物的站位以房主视角对齐 UI）。 */
    private static final Map<CombatContext, UUID> BATTLE_HOSTS = new HashMap<>();
    /**
     * v0.4.6.3：这场战斗最后一个**已经用大标题播报过**的回合数。
     *
     * <p>开战时由 {@link #beginEncounter} 写入 {@code 1}（那一发标题和「战斗开始」一起发了），
     * 之后 {@link #tick} 发现回合数变了就补发一发。多人时同一 tick 只有一个玩家会推进，
     * 所以这张表同时也是"防重复发包"的闸门（与 {@link #NEXT_ENEMY_STEP} 同一个套路）。</p>
     */
    private static final Map<CombatContext, Integer> ANNOUNCED_TURN = new HashMap<>();
    /**
     * v0.5.1.7 A3：**选牌超时**的截止 gameTime（按遭遇战记）。
     *
     * <p>卡牌挂起选牌时，玩家在选完之前既不能再出牌、也不能结束回合 —— 也就是说**整轮都在等他**
     * （新的回合模型要全员确认）。所以这里加一道兜底：等够 {@link #PICK_TIMEOUT_TICKS} 就替他按
     * "贪心"选（等价于他点了「放弃」）。</p>
     */
    private static final Map<CombatContext, Long> PICK_DEADLINE = new HashMap<>();
    /** 选牌的等待上限（tick，20 = 1 秒）。 */
    private static final int PICK_TIMEOUT_TICKS = 20 * 30;
    /**
     * v0.4.6.3：已经收过尾的战斗。
     *
     * <p>{@link #concludeIfOver} 有好几个调用点（tick / 出牌 / 结束回合 / 外部刷新），
     * 而收尾现在会击杀并重开整队 —— 重复执行的代价比过去大得多，所以这里显式去重
     * （{@link #release} 时解除，避免泄漏）。</p>
     */
    private static final Set<CombatContext> CONCLUDED = new HashSet<>();
    /** 第三人称相机与眼睛的距离（原版固定 4 格）。 */
    private static final double THIRD_PERSON_CAMERA_DISTANCE = 4.0D;
    /** 玩家的眼睛高度（原版 1.62 格）。 */
    private static final double PLAYER_EYE_HEIGHT = 1.62D;

    private CombatManager() {
    }

    public static CombatContext combatOf(Player player) {
        return COMBATS.get(player.getUUID());
    }

    public static boolean inCombat(Player player) {
        return COMBATS.containsKey(player.getUUID());
    }

    /** True when the given entity is a mob currently locked by somebody's battle. */
    public static boolean isLocked(Entity entity) {
        if (entity == null) {
            return false;
        }
        for (List<MobCombatant> mobs : TRACKED.values()) {
            for (MobCombatant combatant : mobs) {
                if (combatant.mob() == entity) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 某名玩家现在能不能参战；{@code null} 表示可以。
     * 尖塔入口界面用它解释"为什么这位队友不能加入"。
     */
    public static String partyBlockReason(ServerPlayer player) {
        if (inCombat(player)) {
            return "已在另一场战斗中";
        }
        if (Config.requireBinderToStart() && !BinderResolver.hasBinder(player)) {
            return "未携带卡牌收纳包";
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        if (data.character() == null) {
            return "未选择角色";
        }
        if (DeckBuilder.fromArchive(data.cards()).deck().isEmpty()) {
            return "收纳包里没有可用卡牌";
        }
        return null;
    }

    /**
     * 发起者开战时会被一起拉进来的队友：**同队**（FTB 队伍优先）、在半径内、且满足参战条件。
     * 返回的顺序按距离由近到远。
     */
    public static List<ServerPlayer> eligiblePartyMembers(ServerPlayer host, int radius) {
        List<ServerPlayer> result = new ArrayList<>();
        int limit = Math.max(1, Config.maxPartySize()) - 1;
        if (limit <= 0) {
            return result;
        }
        double radiusSqr = (double) radius * radius;
        for (ServerPlayer mate : TeamResolver.onlineTeammates(host)) {
            if (mate.distanceToSqr(host) > radiusSqr) {
                continue;
            }
            if (partyBlockReason(mate) != null) {
                continue;
            }
            result.add(mate);
        }
        result.sort(Comparator.comparingDouble((ServerPlayer mate) -> mate.distanceToSqr(host)));
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    /**
     * 开战前的队伍预览（聊天栏用）：谁能一起打、谁因为什么不能打。
     * 界面本身不持有队伍状态，所以这份信息由服务端直接发给发起者。
     */
    public static List<String> partyPreviewLines(ServerPlayer host, int radius) {
        List<String> lines = new ArrayList<>();
        List<ServerPlayer> eligible = eligiblePartyMembers(host, radius);
        lines.add("参战预览：你" + (eligible.isEmpty() ? "（暂无同队队友可加入）" : " + " + names(eligible))
                + "　上限 " + Config.maxPartySize() + " 人");
        double radiusSqr = (double) radius * radius;
        for (ServerPlayer mate : TeamResolver.onlineTeammates(host)) {
            String reason = partyBlockReason(mate);
            if (reason != null) {
                lines.add("　" + mate.getName().getString() + "：不能参战（" + reason + "）");
            } else if (mate.distanceToSqr(host) > radiusSqr) {
                lines.add("　" + mate.getName().getString() + "：距离超过 " + radius + " 格");
            }
        }
        return lines;
    }

    /** 实体类型的 id 文本（{@code minecraft:zombie}）。 */
    private static String typeId(Mob mob) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
    }

    /** 这只怪在 **BOSS 表 / 生命覆盖表**里的 id（v0.4.6.8：闪电苦力怕已移除，就是实体类型 id）。 */
    static String bossIdOf(Mob mob) {
        return typeId(mob);
    }

    /**
     * 附近**可以挑战**的本层 BOSS（v0.4.6.9）。
     *
     * <p>规则：只找当前层的 BOSS、跳过本局已经打赢过的；如果本层其实没打赢却全被记成"打过"
     * （v0.4.6.7 的旧存档），就自愈清空记录并放行。</p>
     *
     * @return 按距离排序的 BOSS 实体（可能为空）
     */
    private static List<Mob> nearbyChallengableBosses(ServerPlayer host, int radius) {
        PlayerCardData data = StcAttachments.dataOf(host);
        List<String> ids = new ArrayList<>();
        for (FloorBoss boss : FloorBoss.bossesOf(data.floor())) {
            if (!data.progress().bossFought(boss.entityId())) {
                ids.add(boss.entityId());
            }
        }
        if (ids.isEmpty() && !data.progress().bossDefeated(data.floor())) {
            // 自愈：只是"挑战过"却被记成"打过"（0.4.6.7 的记账时机写错）→ 清掉记录重新允许挑战
            data.setProgress(data.progress().clearBossesFought());
            StcAttachments.syncTo(host);
            send(host, "检测到本层 BOSS 只是挑战过、并没有被击败：已重置挑战记录。");
            for (FloorBoss boss : FloorBoss.bossesOf(data.floor())) {
                ids.add(boss.entityId());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Mob> found = host.serverLevel().getEntitiesOfClass(Mob.class,
                host.getBoundingBox().inflate(radius),
                mob -> mob.isAlive() && !mob.isRemoved() && ids.contains(bossIdOf(mob)));
        found.sort(Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(host)));
        return found;
    }

    /**
     * Starts a battle with the nearest hostile mobs around the host; nearby eligible teammates join
     * the same encounter (up to {@code maxPartySize}).
     *
     * @param radius search radius, or {@code <= 0} to use the configured default
     * @return {@code null} on success, otherwise a message explaining why no battle started
     */
    public static String start(ServerPlayer host, int radius) {
        String blocked = startPreconditions(host);
        if (blocked != null) {
            return blocked;
        }
        int searchRadius = radius > 0 ? radius : Config.combatRadius();
        int floor = StcAttachments.dataOf(host).floor();
        // v0.4.6.9 修："无法触发 BOSS 战斗：周围有普通敌怪时会打成普通战斗。"
        // 只要附近有**本层还没打赢过的 BOSS**，一律优先开 BOSS 战 —— 小怪再多也不影响。
        int bossRadius = Math.max(searchRadius, Config.bossChallengeRadius());
        List<Mob> bossFirst = nearbyChallengableBosses(host, bossRadius);
        if (!bossFirst.isEmpty()) {
            List<Mob> pickedBoss = pickTargets(bossFirst, EventKind.BOSS);
            if (!pickedBoss.isEmpty()) {
                send(host, "附近有本层 BOSS：直接进入 BOSS 战（普通敌怪不会抢走这场战斗）。");
                return beginEncounter(host, pickedBoss, EventKind.BOSS, bossRadius);
            }
        }
        // V10：除了原版敌对生物，被标签分档到精英/BOSS 的**中立**生物也能开战（铁傀儡、劫掠兽…）
        // v0.4.6.7 需求："每个层数都有他们包含的怪物池，玩家不能对非本层的怪物触发战斗。"
        //           —— 非本层的怪直接不进候选；BOSS 已经在上面优先处理过，这里排除 BOSS 档
        List<Mob> candidates = host.serverLevel().getEntitiesOfClass(Mob.class,
                host.getBoundingBox().inflate(searchRadius),
                mob -> mob.isAlive() && !mob.isRemoved() && MobTiers.battleEligible(mob)
                        && MobTiers.tierOf(mob) != MobTier.BOSS
                        && FloorPools.allowed(bossIdOf(mob), floor));
        if (candidates.isEmpty()) {
            return "半径 " + searchRadius + " 格内没有第 " + floor + " 层能开战的生物"
                    + "（第 " + floor + " 层的怪物池见图鉴的阵营页）；BOSS 请从尖塔入口挑战。";
        }
        candidates.sort(Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(host)));
        // 测试反馈修正：**离你最近的那只决定战斗类型**；一场战斗只收同一档的怪，
        // 精英 / BOSS 战默认只打 1 只（猪灵蛮兵小队是唯一例外）。
        EventKind kind = EncounterRules.eventKind(MobTiers.tierOf(candidates.get(0)));
        List<Mob> picked = pickTargets(candidates, kind);
        if (picked.isEmpty()) {
            return "半径 " + searchRadius + " 格内没有可开战的生物。";
        }
        // V9：本层的四类事件各有上限，达到上限后该类别不能再触发（BOSS 不受限）
        EventCaps caps = Config.eventCaps();
        if (!StcAttachments.dataOf(host).canTrigger(kind, caps)) {
            return "本层的「" + kind.label() + "」已达上限（" + caps.capOf(kind) + " 次）："
                    + "换个事件，或者直接挑战本层 BOSS。";
        }
        return beginEncounter(host, picked, kind, searchRadius);
    }

    /**
     * 直接挑战当前层的 BOSS（v0.0.2 V9）。
     *
     * <p>BOSS **不由本 mod 生成**（需求："需自行寻找"）：这里只在 {@code bossChallengeRadius} 格内
     * 找当前层的 BOSS 实体（监守者 / 凋零 / 末影龙），找到就开一场 BOSS 战。
     * BOSS 战不吃每层事件上限，所以可以"跳过剩余内容直接挑战"。</p>
     *
     * @return {@code null} 表示开战成功；否则是不能开战的原因
     */
    public static String challengeBoss(ServerPlayer host, int radius) {
        String blocked = startPreconditions(host);
        if (blocked != null) {
            return blocked;
        }
        PlayerCardData data = StcAttachments.dataOf(host);
        // v0.4.5：一层有**多只** BOSS —— 附近找到哪一只就挑战哪一只
        // v0.4.6.7 需求："所有层数的 BOSS 都只能触发一次战斗，且进入下一层后，不能再和上一层的
        // boss 触发战斗。" —— 上一层天然打不到（这里只找本层的 BOSS），本层打过的记在进度里跳过
        int searchRadius = radius > 0 ? radius : Config.bossChallengeRadius();
        List<Mob> found = nearbyChallengableBosses(host, searchRadius);
        if (found.isEmpty()) {
            return "还没找到第 " + data.floor() + " 层还没打过的 BOSS【" + FloorBoss.describeFloor(data.floor())
                    + "】：先在 " + searchRadius + " 格内找到其中任意一只再来挑战（BOSS 需要你自己去找）。";
        }
        List<Mob> picked = pickTargets(found, EventKind.BOSS);
        if (picked.isEmpty()) {
            return "没找到可以挑战的 BOSS。";
        }
        // v0.4.6.8 修：**不再开战就记账**（那样放弃/战败一次就再也打不了 BOSS 了）。
        // 记账改到"打赢"那一刻（见 grantBossProgress），于是"每只 BOSS 只能打过一次"仍然成立，
        // 但打不过的时候可以反复挑战。
        return beginEncounter(host, picked, EventKind.BOSS, searchRadius);
    }

    /**
     * 挑战**指定的**一只目标（v0.5.0.9：尖塔入口的"本层可挑战名单"用）。
     *
     * <p>需求："详情查看当前层数可以触发战斗的敌怪和boss（以阵营筛选……）" ——
     * 名单里点某一只就直接打它。目标必须满足：</p>
     * <ul>
     *   <li>本层允许（{@link FloorPools#allowed(String, int)}，与名单同一条判定）；</li>
     *   <li>在 {@code radius} 格内真的有这只实体（本 mod **不生成**敌人，只打到场的）。</li>
     * </ul>
     *
     * @param entityId 目标实体 id（{@code minecraft:zombie}）
     * @return {@code null} 表示开战成功；否则是不能开战的原因
     */
    public static String challengeTarget(ServerPlayer host, String entityId, int radius) {
        String blocked = startPreconditions(host);
        if (blocked != null) {
            return blocked;
        }
        PlayerCardData data = StcAttachments.dataOf(host);
        int floor = data.floor();
        if (entityId == null || entityId.isEmpty()) {
            return "没指定要挑战谁。";
        }
        if (!FloorPools.allowed(entityId, floor)) {
            return FloorPools.blockedReason(entityId, floor);
        }
        int searchRadius = radius > 0 ? radius : Config.combatRadius();
        List<Mob> found = host.serverLevel().getEntitiesOfClass(Mob.class,
                host.getBoundingBox().inflate(searchRadius),
                mob -> mob.isAlive() && !mob.isRemoved() && entityId.equals(bossIdOf(mob)));
        if (found.isEmpty()) {
            return "半径 " + searchRadius + " 格内没有找到【" + shortName(entityId) + "】"
                    + "（本模组不生成敌人，请先自行找到它）。";
        }
        found.sort(Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(host)));
        Mob target = found.get(0);
        MobTier tier = MobTiers.tierOf(target);
        boolean boss = tier == MobTier.BOSS;
        EventKind kind = boss ? EventKind.BOSS : EncounterRules.eventKind(tier);
        if (!boss) {
            // BOSS 战不吃事件上限，普通遭遇要查
            EventCaps caps = Config.eventCaps();
            if (!data.canTrigger(kind, caps)) {
                return "本层的「" + kind.label() + "」已达上限（" + caps.capOf(kind) + " 次）：换个事件，或者挑战 BOSS。";
            }
        }
        List<Mob> picked = pickTargets(List.of(target), kind);
        if (picked.isEmpty()) {
            return "这一只现在打不起来。";
        }
        return beginEncounter(host, picked, kind, searchRadius);
    }

    /** 实体 id 的短名（提示文案用）。 */
    private static String shortName(String entityId) {
        int colon = entityId.indexOf(':');
        return colon >= 0 ? entityId.substring(colon + 1) : entityId;
    }

    /**
     * 开战前的共同校验（V9 抽出来，普通遭遇与 BOSS 挑战共用）。
     *
     * @return {@code null} 表示通过
     */
    private static String startPreconditions(ServerPlayer host) {
        if (inCombat(host)) {
            return "你已经在一场战斗中了（/stc combat quit 可以放弃）。";
        }
        // 设计文档 §6.1：没有【卡牌收纳包】就不能触发尖塔对战。
        if (Config.requireBinderToStart() && !BinderResolver.hasBinder(host)) {
            return "需要携带【卡牌收纳包】才能开始尖塔对战（当前位置："
                    + BinderResolver.binderLocation(host) + "）。" + BinderResolver.equipHint();
        }
        // 设计文档 §11：卡组与每回合能量都来自玩家档案 / 角色。
        PlayerCardData hostData = StcAttachments.dataOf(host);
        CharacterProfile hostProfile = hostData.character() == null ? null : CharacterRegistry.byId(hostData.character());
        if (hostProfile == null) {
            return "还没有选择角色：右键【引导方块】打开尖塔入口选一个角色。";
        }
        if (DeckBuilder.fromArchive(hostData.cards()).deck().isEmpty()) {
            return "卡牌收纳包里还没有可用的卡牌：先在尖塔入口选好角色领取起始牌组。";
        }
        if (hostData.life().isDown()) {
            return "生命值已经是 0：用 /stc progress heal 或火堆回复后再开战。";
        }
        // 测试反馈修正：奖励必须先领完（界面会自动弹出），否则下一场战斗开始时会把它作废
        if (hostData.pendingRewards() > 0) {
            return "还有 " + hostData.pendingRewards() + " 次战斗奖励没领：先把三选一选完（界面会自动弹出）。";
        }
        return null;
    }

    /**
     * 从（已按距离排序的）候选里挑出本场战斗的敌人（v0.0.2 测试反馈修正）。
     *
     * <p>规则见 {@link EncounterRules}：</p>
     * <ul>
     *   <li>普通战：最多 {@code maxEnemies}（默认 3）只**普通**怪（可以混种类）；</li>
     *   <li>精英战：只收精英，默认 1 只；猪灵蛮兵小队是唯一例外（同类型 2 只）；</li>
     *   <li>BOSS 战：只收 BOSS，默认 1 只。</li>
     * </ul>
     */
    private static List<Mob> pickTargets(List<Mob> candidates, EventKind kind) {
        Mob leader = null;
        for (Mob mob : candidates) {
            if (EncounterRules.joinsEncounter(kind, MobTiers.tierOf(mob))) {
                leader = mob;
                break;
            }
        }
        if (leader == null) {
            return List.of();
        }
        int size = EncounterRules.encounterSize(kind, Config.maxEnemies(), Config.eliteEncounterSize(),
                MobTiers.squadSize(leader.getType()), Config.eliteSquad());
        List<Mob> picked = new ArrayList<>(size);
        for (Mob mob : candidates) {
            if (picked.size() >= size) {
                break;
            }
            if (!EncounterRules.joinsEncounter(kind, MobTiers.tierOf(mob))) {
                continue;
            }
            // 精英 / BOSS 战只收"这一种"（猪灵蛮兵的小队才成立）；普通战允许混种类
            if (kind != EventKind.NORMAL && mob.getType() != leader.getType()) {
                continue;
            }
            picked.add(mob);
        }
        return picked;
    }

    /** 真正开一场仗：组队、把档案里的卡组/遗物/生命带进来、摆位、清奖励、计数、开打。 */
    private static String beginEncounter(ServerPlayer host, List<Mob> picked, EventKind kind, int searchRadius) {
        PlayerCardData hostData = StcAttachments.dataOf(host);
        CharacterProfile hostProfile = CharacterRegistry.byId(hostData.character());

        // ---- 参战名单：发起者在前，队友按距离跟上
        List<ServerPlayer> roster = new ArrayList<>();
        roster.add(host);
        roster.addAll(eligiblePartyMembers(host, searchRadius));

        List<CombatContext.Participant> participants = new ArrayList<>();
        for (ServerPlayer member : roster) {
            PlayerCardData data = StcAttachments.dataOf(member);
            DeckBuilder.Result built = DeckBuilder.fromArchive(data.cards());
            if (!built.unknown().isEmpty()) {
                SlayTheCraft.LOGGER.warn("[STC] {} 的档案里有 {} 张无法识别的卡牌，已跳过：{}",
                        member.getScoreboardName(), built.unknown().size(), built.unknown());
            }
            CharacterProfile profile = CharacterRegistry.byId(data.character());
            int energy = profile == null ? Config.rules().energyPerTurn() : profile.energyPerTurn();
            // V7：把玩家档案里的遗物带进这场战斗（触发点由引擎广播）
            List<Relic> relics = RelicLibrary.relicsOf(data.relics());
            // V9：独立生命值（上限跟随配置）；战斗中掉血在结束时写回档案
            LifePool pool = data.life().withMax(Config.maxPlayerHp());
            if (pool.maxHp() != data.life().maxHp()) {
                data.setLife(pool);
            }
            PlayerCombatant playerCombatant = new PlayerCombatant(member, pool);
            // v0.3.0：把实体编号交给引擎，表现层事件才能找到"该做动作的那个人"
            playerCombatant.setFxId(member.getId());
            // v0.4.0：充能球栏位按角色给 —— **只有故障机器人是 3，其他人是 0**（界面上不再显示空球位）
            playerCombatant.setOrbSlots(profile == null ? 0 : profile.orbSlots());
            participants.add(new CombatContext.Participant(playerCombatant, built.deck(), energy, relics));
        }

        CombatRules base = Config.rules();
        CombatRules rules = new CombatRules(hostProfile.energyPerTurn(), base.handSize(), base.maxHandSize(),
                base.maxEnemies());
        List<MobCombatant> combatants = new ArrayList<>(picked.size());
        Map<String, Integer> nameCounts = new HashMap<>();
        // v0.4.5：多人加成 —— 每多一名组队玩家，敌人的战斗生命 +50%
        double partyFactor = 1.0D + 0.5D * Math.max(0, roster.size() - 1);
        for (Mob mob : picked) {
            MobIntents.Profile mobProfile = MobIntents.profile(mob);
            int seen = nameCounts.merge(mobProfile.displayName(), 1, Integer::sum);
            String name = seen > 1 ? mobProfile.displayName() + "#" + seen : mobProfile.displayName();
            // v0.4.5 BOSS 表 / v0.4.6.2 非 BOSS 覆盖表：尖塔战斗生命（0 = 用默认缩放）
            int battleHp = com.slaythecraft.combat.core.BattleHp.overrideFor(bossIdOf(mob));
            MobCombatant combatant = new MobCombatant(mob, name, mobProfile, partyFactor, battleHp);
            combatant.lock();
            combatant.setFxId(mob.getId());
            combatants.add(combatant);
        }
        if (roster.size() > 1) {
            send(host, "多人加成：敌人战斗生命 ×" + partyFactor + "（" + roster.size() + " 人）");
        }

        List<Enemy> enemies = new ArrayList<>(combatants);
        // 战报广播给这场战斗的所有参与者（v0.4.6.5：走"战报"通道，默认静音；指令回执不受影响）
        CombatLog log = message -> battleLog(roster, message);
        CombatContext context = new CombatContext(participants, enemies, rules, host.getRandom().nextLong(), log);
        // v0.4.1：【海洋之心】看天气 —— 开战前先同步一次，之后每 tick 跟着天气变
        syncWeather(host, context);
        // v0.3.0：表现层事件（攻击/格挡/受击动画与音效）走这条总线发给参战玩家
        CombatFxBus.attach(context, roster);
        // v0.4.6.7：召唤类动作（唤魔者 召唤恼鬼 ×2）需要真的把实体生成出来
        context.setSummoner((entityId, summonerEnemy) ->
                summonForBattle(host, roster, combatants, summonerEnemy, entityId));
        for (ServerPlayer member : roster) {
            COMBATS.put(member.getUUID(), context);
            TRACKED.put(member.getUUID(), combatants);
        }
        BATTLE_HOSTS.put(context, host.getUUID());
        // V3：先把敌人排到玩家正前方（面对面、至少 5 格），再冻结附近的局外怪物，最后才开打
        positionEncounter(host, roster, combatants);
        // v0.4.0：敌人回合改成"一只一只来"，顺序按画面**从左到右**（动画才能依次播放）
        context.setEnemyOrder(leftToRight(host, context.enemies()));
        context.setPacedEnemyTurn(true);
        lockBystanders(host, context);
        send(host, "参战 " + roster.size() + " 人：" + names(roster) + "　先手 " + participants.get(0).deck().size()
                + " 张卡 · 每回合能量按各自角色");
        // V9：开战即处理"上一战留下的未领奖励"与"本层事件计数"
        prepareProgress(roster, kind);
        context.start();
        // v0.5.1.0 开发者模式："每场战斗开始时获得 9999 层缓冲和 9999 力量"
        // （放在 context.start() 之后：这时玩家战斗员才建好）
        for (ServerPlayer member : roster) {
            if (com.slaythecraft.dev.DevMode.isEnabled(member)) {
                com.slaythecraft.dev.DevMode.applyBattleStartBuffs(
                        context.combatantNamed(member.getName().getString()));
                send(member, "【开发者模式】开局加成已发放：" + com.slaythecraft.dev.DevMode.DEV_BUFFER
                        + " 层缓冲 + " + com.slaythecraft.dev.DevMode.DEV_STRENGTH + " 力量。");
            }
        }
        // v0.4.6.3：开战立刻用原版大标题播「战斗开始」（副标题「第 1 回合」），并记下已播报的回合数
        BattleTitles.battleStart(roster);
        ANNOUNCED_TURN.put(context, context.turn());
        // v0.4.1：把开局状态写进日志 —— 万一"第一回合被跳过"，日志能直接定位
        SlayTheCraft.LOGGER.info("[STC] 战斗开始：第 {} 回合 / {} / 行动玩家 {} / 能量 {} / 手牌 {} 张 / 存活敌人 {} 只",
                context.turn(), context.phase(),
                context.activePlayer() == null ? "?" : context.activePlayer().name(),
                context.energy(), context.hand().size(), context.aliveEnemies().size());
        refreshIntentLabels(host, context);
        refreshAll(host, context);
        return null;
    }

    /**
     * 真的把被召唤的实体生成出来，并包成一份可参战的 {@link MobCombatant}（v0.4.6.7）。
     *
     * <p>位置：召唤者旁边（没有召唤者就放在发起者旁边），避免和已有敌人重叠。</p>
     *
     * @return 造好的敌人；失败返回 {@code null}（引擎会记一条"召唤失败"）
     */
    /**
     * 召唤一只随从加入战斗（v0.5.0.5 修正站位与清理）。
     *
     * <p>需求原文："掠夺者召唤同伴时，先前被击杀的同伴UI未被清除，导致生成的掠夺者实体位置和UI位置
     * 都超出屏幕。应该：击败敌人后，清空被击败敌人的实体所在位置的UI和实体位置记录，
     * 以便于下次生成实体和显示UI。"</p>
     *
     * <p>原来这里用 {@code combatants.size()} 算偏移，而 {@code combatants} 里**包含已经阵亡的敌人**
     * —— 每死一只、每召唤一只，size 只增不减，于是 {@code side = 1.4 × (size % 3 + 1)} 越算越大，
     * 召唤出来的实体和它的 UI 一起飞出屏幕。现在改成只按**存活**的敌人计位，
     * 并且把最终落点夹在一个安全半径内（双保险）。</p>
     */
    private static Enemy summonForBattle(ServerPlayer host, List<ServerPlayer> roster,
                                         List<MobCombatant> combatants, Enemy summoner, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) {
            return null;
        }
        EntityType<?> raw = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (!(raw.create(host.serverLevel()) instanceof Mob spawned)) {
            return null;
        }
        // 先清掉已阵亡敌人的实体/UI 位置记录，再按"还活着几只"排位
        purgeDefeated(combatants);
        int living = 0;
        for (MobCombatant combatant : combatants) {
            if (combatant.alive()) {
                living++;
            }
        }
        net.minecraft.world.phys.Vec3 anchor = summoner instanceof MobCombatant source && source.mob() != null
                ? source.mob().position()
                : host.position();
        double side = 1.4D * (living % 3 + 1);
        net.minecraft.world.phys.Vec3 spot = clampToAnchor(anchor, side, side * 0.5D);
        spawned.moveTo(spot.x, spot.y, spot.z, host.getYRot(), 0.0F);
        spawned.setPersistenceRequired();
        host.serverLevel().addFreshEntity(spawned);
        MobIntents.Profile profile = MobIntents.profile(spawned);
        MobCombatant summoned = new MobCombatant(spawned, profile.displayName(), profile, 1.0D,
                com.slaythecraft.combat.core.BattleHp.overrideFor(typeId(spawned)));
        summoned.lock();
        summoned.setFxId(spawned.getId());
        combatants.add(summoned);
        for (ServerPlayer member : roster) {
            List<MobCombatant> tracked = TRACKED.get(member.getUUID());
            if (tracked != null && !tracked.contains(summoned)) {
                tracked.add(summoned);
            }
        }
        SlayTheCraft.LOGGER.info("[STC] 召唤 {} 加入战斗（战斗位置 {}，场上存活 {}）",
                entityId, spawned.position(), living);
        return summoned;
    }

    /**
     * 清掉已阵亡敌人的**实体位置记录**（v0.5.0.5）。
     *
     * <p>需求："击败敌人后，清空被击败敌人的实体所在位置的UI和实体位置记录。"</p>
     *
     * <p>只从 {@code combatants} 里摘掉**已阵亡且实体已消失**的那些——引擎那边
     * {@code CombatContext.enemies()} 仍保留它们，UI 靠 {@code EnemyView.alive()} 自己不再画血条，
     * 所以这里动的是 MC 层的记录，不会影响"打完了没有"的判定。</p>
     */
    private static void purgeDefeated(List<MobCombatant> combatants) {
        combatants.removeIf(combatant -> !combatant.alive() || combatant.isGone());
    }

    /**
     * 把召唤落点**夹在离锚点最多 {@link #SUMMON_MAX_DISTANCE} 格**的位置。
     *
     * <p>即使以后又有人在别处算错了偏移量，随从也不会再跑到屏幕外面去。</p>
     */
    private static net.minecraft.world.phys.Vec3 clampToAnchor(net.minecraft.world.phys.Vec3 anchor,
                                                               double dx, double dz) {
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > SUMMON_MAX_DISTANCE && length > 0.0D) {
            double scale = SUMMON_MAX_DISTANCE / length;
            dx *= scale;
            dz *= scale;
        }
        return new net.minecraft.world.phys.Vec3(anchor.x + dx, anchor.y, anchor.z + dz);
    }


    /**
     * 同步天气到参战者（v0.4.1）：【海洋之心】在雨天/雷暴时减伤 10%。
     *
     * <p>天气是 Minecraft 的东西，引擎只知道一个布尔值，所以由这里每 tick 写入。</p>
     */
    private static void syncWeather(ServerPlayer caller, CombatContext context) {
        var level = caller.serverLevel();
        boolean stormy = level.isRaining() || level.isThundering();
        for (Combatant combatant : context.combatants()) {
            combatant.setStormy(stormy);
        }
    }

    /**
     * 开战时的进度处理（v0.0.2 V9）。
     *
     * <p>两件事：**清空上一场留下的未领奖励**（需求：下一场战斗开始时清空），
     * 以及给每名参与者各记一次本层事件计数（各自独立）。</p>
     */
    private static void prepareProgress(List<ServerPlayer> roster, EventKind kind) {
        EventCaps caps = Config.eventCaps();
        for (ServerPlayer member : roster) {
            PlayerCardData data = StcAttachments.dataOf(member);
            int cleared = data.clearPendingOffers();
            if (cleared > 0) {
                send(member, "上一场战斗留下的 " + cleared + " 个未领奖励已作废（奖励要在下一场战斗开始前领）。");
            }
            if (kind.capped()) {
                if (data.recordEvent(kind, caps)) {
                    send(member, "本层「" + kind.label() + "」进度："
                            + data.progress().countOf(kind) + "/" + caps.capOf(kind));
                } else {
                    send(member, "你的本层「" + kind.label() + "」已达上限，本场不计入进度。");
                }
            } else {
                FloorBoss boss = data.progress().boss();
                send(member, "BOSS 战开始：第 " + data.floor() + " 层 · "
                        + (boss == null ? "?" : boss.name()) + "（BOSS 战不受事件上限限制）");
                // BOSS 也是"一次事件"（篝火不能连用要靠它），只是不占计数
                data.recordEvent(kind, caps);
            }
            StcAttachments.syncTo(member);
        }
    }

    /**
     * Plays a card by 1-based numbers as shown by {@code /stc combat status}.
     *
     * <p>v0.5.1.6 回合模型改造：出牌者就是**发这个请求的玩家**（不再要求"轮到他"）。
     * 多个玩家可以同时出牌，结算顺序 = 请求到达顺序。</p>
     *
     * @param enemyNumber 1-based enemy index, or {@code <= 0} for the first living enemy
     * @return {@code null} on success, otherwise a message
     */
    public static String playCard(ServerPlayer player, int handNumber, int enemyNumber) {
        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return "你当前不在战斗中。";
        }
        Combatant actor = context.combatantNamed(player.getName().getString());
        if (actor == null) {
            return "你不在这场战斗里。";
        }
        CombatContext.PlayResult result = context.playCard(actor, handNumber - 1, enemyNumber - 1);
        if (!result.success()) {
            send(player, result.message());
            return result.message();
        }
        // v0.5.1.7 A3：这张牌要求玩家选牌 → 把候选发下去，等他选完再继续
        offerCardPickIfNeeded(player, context, actor);
        // v0.5.4.1（A4-20）【炼金】：药水是档案层的数据，引擎只记账 —— 打完这张牌立刻兑现，
        // 这样本场战斗就能用上（放到战斗结束才给就失去意义了）。
        grantPendingPotions(player, context, actor);
        refreshAll(player, context);
        concludeIfOver(player, context);
        return null;
    }

    /**
     * 兑现【炼金】（v0.5.4.1 A4-20）攒下的"待发药水"。
     *
     * <p>为什么由 MC 层做：药水存在玩家档案（{@code PlayerCardData.potions}）里，
     * 零 MC 依赖的 {@code combat/core} 碰不到它 —— 引擎只记一笔账
     * （{@code PlayerState.pendingPotions}），这里在**这张牌打完之后**立刻兑换成随机药水，
     * 所以这一场战斗就能用上（等到战斗结束再发就失去意义了）。</p>
     */
    private static void grantPendingPotions(ServerPlayer player, CombatContext context, Combatant actor) {
        grantRandomPotions(player, context.takePendingPotions(actor), "炼金");
    }

    /** 把 {@code count} 瓶随机药水塞进玩家的药水栏（槽位满了会明说，不静默丢弃）。 */
    private static void grantRandomPotions(ServerPlayer player, int count, String label) {
        if (count <= 0) {
            return;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        int slots = data.potionSlots(Config.potionSlots());
        boolean changed = false;
        for (int i = 0; i < count; i++) {
            List<com.slaythecraft.combat.core.Potion> all = com.slaythecraft.combat.core.PotionLibrary.all();
            if (all.isEmpty()) {
                return;
            }
            com.slaythecraft.combat.core.Potion potion =
                    all.get(player.getRandom().nextInt(all.size()));
            if (data.addPotion(potion.id(), slots)) {
                send(player, "【" + label + "】：获得「" + potion.name() + "」。");
                changed = true;
            } else {
                send(player, "【" + label + "】：药水栏位已满（" + slots + " 格），这瓶药水没有拿到。");
            }
        }
        if (changed) {
            StcAttachments.syncTo(player);
        }
    }

    /**
     * 把"请选牌"发给出牌者（v0.5.1.7 A3）。
     *
     * <p>候选是**服务端快照**（引擎在挂起那一刻就固定下来了），客户端只负责显示与回传下标。</p>
     */
    private static void offerCardPickIfNeeded(ServerPlayer player, CombatContext context, Combatant actor) {
        CombatContext.PendingCardPick pick = context.pendingPick(actor);
        if (pick == null) {
            return;
        }
        List<com.slaythecraft.network.CardPickOfferPayload.Choice> choices = new ArrayList<>();
        for (Card card : pick.candidates()) {
            choices.add(new com.slaythecraft.network.CardPickOfferPayload.Choice(CardIds.of(card), false));
        }
        PacketDistributor.sendToPlayer(player, new com.slaythecraft.network.CardPickOfferPayload(
                cardPickPrompt(pick), pick.maxPicks(), choices));
        // 超时兜底：一直不选会挡住整轮（别人都在等他），所以记一个截止时间
        PICK_DEADLINE.put(context, player.serverLevel().getGameTime() + PICK_TIMEOUT_TICKS);
    }

    /** 按效果种类生成给玩家看的一句话。 */
    private static String cardPickPrompt(CombatContext.PendingCardPick pick) {
        return switch (pick.kind()) {
            case SCRAP_RECYCLE -> "选择弃牌堆的一张牌，放到抽牌堆顶";
            case CONTINGENCY -> "选择手牌中的一张牌，把它变成「闪光弹」";
            case ABANDON -> "选择最多 " + pick.maxPicks() + " 张手牌消耗";
            case RECYCLE -> "选择手牌中的一张牌消耗，获得等同于其费用的能量";
            case DISCARD -> "选择 " + pick.maxPicks() + " 张手牌弃掉";
            case UPGRADE_ONE -> "选择一张手牌升级（本场战斗有效）";
            case DUPLICATE -> "选择一张攻击牌或能力牌进行复制";
            case BURNING_PACT -> "选择一张手牌消耗，然后抽 " + Math.max(1, pick.amount()) + " 张牌";
            case PUT_ON_TOP -> "选择一张手牌，放到抽牌堆顶";
            // v0.5.3.3【布置】
            case SETUP -> "选择一张手牌，放到抽牌堆顶（它被打出之前费用为 0）";
            // v0.5.3.9（A4-18）
            case EXHUME -> "从消耗堆里选一张牌，把它拿回手牌";
            case NIGHTMARE -> "选择一张手牌，下个回合开始时加入 " + Math.max(1, pick.amount()) + " 张它的复制";
        };
    }

    /**
     * 处理客户端回传的选牌结果（v0.5.1.7 A3）。
     *
     * @param auto 放弃/超时 → 引擎按贪心替他选
     */
    public static void resolveCardPick(ServerPlayer player, List<Integer> indices, boolean auto) {
        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return;
        }
        Combatant actor = context.combatantNamed(player.getName().getString());
        if (actor == null || context.pendingPick(actor) == null) {
            return;
        }
        PICK_DEADLINE.remove(context);
        String detail = context.resolveCardPick(actor, auto ? null : indices);
        send(player, "「选牌」" + detail);
        // 让客户端把选牌页收掉（否则它会一直挡在出牌界面前面）
        PacketDistributor.sendToPlayer(player, com.slaythecraft.network.CardPickOfferPayload.cleared());
        refreshAll(player, context);
        concludeIfOver(player, context);
    }

    /**
     * 确认结束回合（v0.5.1.6：**逐人确认**，全员确认后才进敌怪回合）。
     *
     * <p>需求："直到所有玩家都确认【结束回合】，才会进入敌怪的攻击回合。"</p>
     */
    public static String endTurn(ServerPlayer player) {
        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return "你当前不在战斗中。";
        }
        if (context.isOver()) {
            concludeIfOver(player, context);
            return null;
        }
        Combatant actor = context.combatantNamed(player.getName().getString());
        if (actor == null) {
            return "你不在这场战斗里。";
        }
        if (!context.confirmEndTurn(actor)) {
            // 已经确认过 / 已经倒下 / 不在玩家回合
            send(player, context.isActive(actor)
                    ? "现在不是可以结束回合的时候。"
                    : "你已经确认结束回合了，正在等队友。");
            return null;
        }
        // v0.4.0：敌人回合是分步的 —— 先等一小会儿，再一只一只行动（动画依次播放）
        if (context.awaitingEnemyActions()) {
            NEXT_ENEMY_STEP.put(context, player.serverLevel().getGameTime() + ENEMY_STEP_TICKS);
        }
        refreshAll(player, context);
        concludeIfOver(player, context);
        return null;
    }

    /** Human-readable battle state, or an empty list when no battle is running. */
    public static List<String> status(ServerPlayer player) {
        CombatContext context = COMBATS.get(player.getUUID());
        return context == null ? List.of() : context.statusLines();
    }

    /** 外部改动（例如用了药水）之后刷新行动栏与客户端快照。 */
    public static void refresh(ServerPlayer player) {        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return;
        }
        refreshAll(player, context);
        concludeIfOver(player, context);
    }

    /**
     * 把状态牌塞进玩家当前战斗的弃牌堆（v0.0.3 W2 的调试入口：{@code /stc status card …}）。
     *
     * @return 给玩家看的提示
     */
    public static String giveStatusCard(ServerPlayer player, String cardId, int count) {
        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return "状态牌只在战斗中有意义：先用 /stc combat start 开一场战斗。";
        }
        Card card = CardLibrary.byId(cardId);
        if (card == null) {
            return "没有这张卡：" + cardId;
        }
        if (card.type() != CardType.STATUS) {
            return "「" + card.name() + "」不是状态牌（只能是 void / slimed / burn / dazed / venom / call / wound）。";
        }
        String message = context.insertCards(player.getName().getString(), card, Math.max(1, count));
        refresh(player);
        return message;
    }

    /**
     * 当前这场战斗的**参战人数**（v0.5.1.4）。
     *
     * <p>给"多人卡牌只在多人模式进奖励池"用（见 {@code RewardService.rollOffer}）：
     * 不在战斗里就按 1 人算（单人视角），所以指令发奖之类也不会漏出多人卡牌。</p>
     */
    public static int partySize(ServerPlayer player) {
        CombatContext context = player == null ? null : COMBATS.get(player.getUUID());
        return context == null ? 1 : Math.max(1, context.playerCount());
    }

    /**
     * 放弃战斗（v0.4.2：**直接杀死玩家**）。
     *
     * <p>需求原文："新效果：放弃战斗将直接导致玩家死亡。" 所以这里先解散战斗（解锁敌人、写回独立生命值），
     * 再让玩家按原版流程死亡（走 {@code kill()}，死亡消息、掉落、重生都照常），
     * 并把本局标记为"已结束"，方便之后换角色 / 重开。</p>
     */
    public static String quit(ServerPlayer player, String reason) {
        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return "你当前不在战斗中。";
        }
        release(player, context, "已离开战斗（" + reason + "）。");
        PlayerCardData data = StcAttachments.dataOf(player);
        data.markRunDefeated();
        StcAttachments.syncTo(player);
        send(player, "你放弃了战斗 —— 代价是生命。");
        player.kill();
        return null;
    }

    /** Called every player tick: keeps the action bar fresh and cleans up stale battles. */
    public static void tick(ServerPlayer player) {
        CombatContext context = COMBATS.get(player.getUUID());
        if (context == null) {
            return;
        }
        if (player.isRemoved() || player.hasDisconnected()) {
            release(player, context, null);
            return;
        }
        // v0.5.1.7 A3：选牌超时兜底 —— 有人在选完之前既不能出牌也不能结束回合，整轮都在等他；
        // 等够 PICK_TIMEOUT_TICKS 就替他按贪心选（等价于他点了「放弃」）。
        // ⚠️ 只处理"这一 tick 属于那个待选牌的玩家"的情况，绝不能在别人的 tick 上 return ——
        // 否则敌人分步推进与行动栏刷新会被跳掉。
        Long pickDeadline = PICK_DEADLINE.get(context);
        if (pickDeadline != null && player.serverLevel().getGameTime() >= pickDeadline) {
            PICK_DEADLINE.remove(context);
            Combatant waiting = context.combatantNamed(player.getName().getString());
            if (waiting != null && context.pendingPick(waiting) != null) {
                send(player, "选牌等待超时，已按「放弃」自动处理：" + context.cancelCardPick(waiting));
                PacketDistributor.sendToPlayer(player, com.slaythecraft.network.CardPickOfferPayload.cleared());
                refreshAll(player, context);
                concludeIfOver(player, context);
                return;
            }
        }
        // 玩家倒下不再等于战斗结束：其他人可以继续打完（全员倒下才判负）
        List<MobCombatant> mobs = TRACKED.get(player.getUUID());
        if (mobs != null) {
            boolean changed = false;
            for (MobCombatant combatant : mobs) {
                if (combatant.alive() && combatant.isGone()) {
                    combatant.setHp(0);
                    changed = true;
                }
            }
            if (changed) {
                context.recheck();
            }
        }
        if (context.isOver()) {
            concludeIfOver(player, context);
            return;
        }
        // v0.4.1：天气变化要跟着写进战斗（【海洋之心】的减伤看天气）
        syncWeather(player, context);
        // v0.4.6.3：每个新回合用原版大标题播「第 X 回合」。
        // 放在分步敌人回合之前，这样"敌人回合刚结束、下一轮玩家回合刚开始"能第一时间播出来；
        // 回合数用 map 记着，多人时只有第一个到达的玩家会真的发包（与 NEXT_ENEMY_STEP 同一个套路）。
        int announced = ANNOUNCED_TURN.getOrDefault(context, context.turn());
        if (context.turn() != announced) {
            ANNOUNCED_TURN.put(context, context.turn());
            BattleTitles.turn(participantsOf(player, context), context.turn());
        }
        // v0.4.0：分步敌人回合 —— 每隔 ENEMY_STEP_TICKS 让下一只敌人行动（多人时同一 tick 只有一个玩家推进）
        if (context.awaitingEnemyActions()) {
            long now = player.serverLevel().getGameTime();
            long due = NEXT_ENEMY_STEP.getOrDefault(context, 0L);
            if (now >= due) {
                NEXT_ENEMY_STEP.put(context, now + ENEMY_STEP_TICKS);
                context.stepEnemyTurn();
                refreshAll(player, context);
                concludeIfOver(player, context);
            }
            return;
        }
        // v0.5.1.3 看门狗：回合推进如果断了（敌人队列已空、阶段却停在敌人回合），        // 出牌/结束回合/放弃战斗全部会失效 —— v0.5.1.2 的"紧急避险"就是这么把整局锁死的。
        // 这里强制接上下一轮，并把证据写进日志，宁可重开这一轮也不让玩家卡在死局里。
        if (context.stalledBetweenTurns() && context.resumeStalledTurn()) {
            refreshAll(player, context);
            concludeIfOver(player, context);
            return;
        }
        if (player.tickCount % 10 == 0) {
            refreshActionBar(player, context);
        }
    }

    /**
     * Ends the battle of a defeated mob: releases the lock and lets vanilla handle the death.
     *
     * <p>{@code hurt()} refuses to act on an entity that is already dying, and the engine mirrors
     * its 0 HP onto the mob before calling this, so the mob is briefly restored to 1 HP in order to
     * take a real killing blow — that way {@code die()} runs and loot/XP/death effects are normal.
     * </p>
     *
     * <p><b>测试反馈修正</b>：{@code mob.kill()} 是**无来源**伤害，原版击杀归属（{@code getKillCredit()}）
     * 会是空的，所以战斗击杀以前从来数不到、也就永远没有卡牌奖励。现在：</p>
     * <ol>
     *   <li>先把这只怪标记成"引擎击杀"，让 {@code KillTracker} 跳过它（避免重复计数）；</li>
     *   <li>处决之后直接按"最后一击的玩家"记账发奖（{@link RewardService#recordCombatKill}）。</li>
     * </ol>
     */
    static void killMob(Mob mob, ServerPlayer killer) {
        if (mob.isRemoved()) {
            return;
        }
        ENGINE_KILLS.put(mob.getUUID(), mob.level().getGameTime());
        mob.setInvulnerable(false);
        mob.setNoAi(false);
        if (mob.getHealth() <= 0.0F) {
            mob.setHealth(1.0F);
        }
        mob.kill();
        creditKill(mob, killer);
    }

    /** 引擎击杀 → 奖励记账（原版事件拿不到击杀归属，所以这里补上）。 */
    private static void creditKill(Mob mob, ServerPlayer killer) {
        if (killer == null) {
            return;
        }
        MobTier tier = MobTiers.tierOf(mob);
        if (tier == MobTier.NONE) {
            return;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (typeId == null) {
            return;
        }
        RewardService.recordCombatKill(killer, tier, typeId);
    }

    /**
     * 这只怪是不是被引擎处决的（{@code KillTracker} 用来避免重复计数）。
     *
     * <p>只认很短一段时间内的记录，避免实体 UUID 复用时误判。</p>
     */
    public static boolean consumeEngineKill(LivingEntity entity) {
        Long stamp = ENGINE_KILLS.remove(entity.getUUID());
        return stamp != null && entity.level().getGameTime() - stamp <= ENGINE_KILL_WINDOW;
    }

    /**
     * 战斗结束后立刻弹出奖励（测试反馈：不再回收纳包点领取）。
     *
     * <p>v0.5.0.20：改成优先弹**战斗结算页**（领取金币 / 遗物 / 卡牌奖励 / 跳过）；
     * 如果这一场没有结算内容（例如关了奖励开关），退回原来的"直接弹三选一"。</p>
     */
    private static void openPendingRewards(List<ServerPlayer> roster) {
        for (ServerPlayer member : roster) {
            RewardService.openSettlement(member);
        }
    }

    // ------------------------------------------------------------------ 内部

    private static void concludeIfOver(ServerPlayer player, CombatContext context) {
        if (!context.isOver() || CONCLUDED.contains(context)) {
            return;
        }
        // v0.4.6.3：收尾只做一次。现在战败会**击杀并重开**整队，重复收尾的代价比过去大得多，
        // 所以这里显式上一道闸（release 时解除）。
        CONCLUDED.add(context);
        // 先记下参战名单：release 会把映射清掉，之后就问不出来了
        List<ServerPlayer> roster = participantsOf(player, context);
        boolean won = context.phase() == CombatPhase.VICTORY;
        // v0.4.6：诅咒【贪婪】的金币欠款、【幸运药水】的下次奖励强化、【蜂蜜佐料】的最大生命值
        settleBattleExtras(player, context, roster);
        if (won) {
            MobTier tier = battleTier(player, context);
            grantTierRelic(player, context, tier);
            grantBossProgress(player, context);
            // v0.2.1：每场胜利必给一次三选一 + 金币（奖励界面在 release 之后立刻弹出）
            if (Config.rewardPerVictory()) {
                for (ServerPlayer member : roster) {
                    RewardService.grantVictoryReward(member, tier);
                }
            }
        }
        String outcome = won
                ? "战斗胜利！第 " + context.turn() + " 回合结束。"
                : "全员倒下……战斗失败（第 " + context.turn() + " 回合）。";
        // v0.5.0.7 成就：第一次击败常规敌怪 / 第一次战败
        // （帧里没有能表达这两件事的原版触发器，所以 JSON 用 minecraft:impossible，由这里授予）
        for (ServerPlayer member : roster) {
            if (won) {
                com.slaythecraft.achievement.AchievementService.grantFirstBlood(member);
            } else {
                com.slaythecraft.achievement.AchievementService.grantFirstDefeat(member);
            }
        }
        // v0.5.1.6：【不屈】的"彻底从牌组移除"在这里真正落地（必须在 release 之前，之后 context 就没了）
        discardRemovedCards(context, roster);
        release(player, context, outcome);
        if (won) {
            // 测试反馈修正：战斗一结束就进入奖励界面（不再回收纳包点"领取奖励"）
            openPendingRewards(roster);
        } else {
            // v0.4.6.3：全队倒下 = 立刻击杀 + 重开（回第 1 层重选角色）。
            // 必须放在 release 之后：release 里的 persistLife 会写回生命值，之后 clearRun 才能覆盖它。
            wipeAndRestart(roster);
        }
    }

    /**
     * 全队倒下的收尾（v0.4.6.3）。
     *
     * <p>需求原文：</p>
     * <ul>
     *   <li>"单人模式下，玩家战败时应当：立刻击杀此玩家并重开，回到第一层重新选择角色。"</li>
     *   <li>"队伍玩家全部战败：立刻击杀所有同组全部玩家并令同组全部玩家重开，回到第一层重新选择角色。"</li>
     * </ul>
     *
     * <p>单人是"队伍只有一个人"的特例，所以两条需求共用这一条路径：对名单里的每个人
     * {@link RunService#clearRun}（清空本局 → 角色置空、回第 1 层、金币回到初始值），
     * 再走原版 {@code kill()}（死亡消息、掉落、重生都照常）。玩家重生后到尖塔入口重新选角色即可。</p>
     *
     * <p>队友还有存活玩家时**不会**走到这里 —— 那种情况由引擎继续打完，倒下的人在局内等待
     * （见 {@link #refreshActionBar} 的等待提示）；战斗胜利时他由 {@link #persistLife} 恢复 1 点生命值。</p>
     */
    private static void wipeAndRestart(List<ServerPlayer> roster) {
        for (ServerPlayer member : roster) {
            if (member == null || member.isRemoved()) {
                continue;
            }
            PlayerCardData data = StcAttachments.dataOf(member);
            RunService.clearRun(data);
            StcAttachments.syncTo(member);
            StcChat.note(member, "全队倒下：本局已重开 —— 回到第 1 层，请在尖塔入口重新选择角色。");
            member.kill();
        }
    }

    /**
     * 战斗结束时结算 v0.4.6 的几件新东西（都要在 {@link #release} 之前做，那时敌人表还在）。
     *
     * <ul>
     *   <li>诅咒【贪婪】：把引擎记下的金币欠款真正扣掉（**不会变成负数**）；</li>
     *   <li>【幸运药水】：把"下一次卡牌奖励全部强化"写进档案；</li>
     *   <li>【蜂蜜佐料】：每场战斗结束时 +1 最大生命值（当前生命同 +1）。</li>
     * </ul>
     */
    private static void settleBattleExtras(ServerPlayer caller, CombatContext context, List<ServerPlayer> roster) {
        for (ServerPlayer member : roster) {
            Combatant combatant = context.combatantNamed(member.getName().getString());
            if (combatant == null) {
                continue;
            }
            PlayerCardData data = StcAttachments.dataOf(member);
            boolean changed = false;
            int debt = context.goldDebt(combatant);
            if (debt > 0) {
                int paid = Math.min(debt, data.gold());
                data.setGold(data.gold() - paid);
                send(member, "【贪婪】发作：失去 " + paid + " 金币（剩余 " + data.gold() + "）。");
                changed = true;
            }
            if (context.luckyReward(combatant) && !data.luckyReward()) {
                data.setLuckyReward(true);
                send(member, "【幸运药水】生效：下一次卡牌奖励的所有可选卡牌都会是强化版。");
                changed = true;
            }
            if (data.hasRelic("honey_seasoning")) {
                data.setLife(data.life().withMax(data.life().maxHp() + 1));
                data.healHp(1);
                send(member, "【蜂蜜佐料】生效：最大生命值 +1（当前 " + data.life().describe() + "）。");
                changed = true;
            }
            // v0.5.3.1【商队护送】：这一轮护送成功攒下的额外金币，**战斗胜利后**入账
            // （打输了不给 —— "战斗结束后额外获得"按本模组其它结算的惯例只算胜利那一侧）
            int escortGold = context.phase() == CombatPhase.VICTORY ? context.escortGold(combatant) : 0;
            if (escortGold > 0) {
                data.setGold(data.gold() + escortGold);
                send(member, "【商队护送】成功 " + context.escortSuccesses(combatant) + " 次：额外 +"
                        + escortGold + " 金币（当前 " + data.gold() + "）。");
                changed = true;
            }
            // v0.5.4.1（A4-20）【炼金】的兜底：正常路径在"打完这张牌"那一刻就兑现了，
            // 万一有哪条打出路径没经过 CombatManager.playCard（例如引擎内部自动打出），这里补发。
            grantRandomPotions(member, context.takePendingPotions(combatant), "炼金");
            if (changed) {
                StcAttachments.syncTo(member);
            }
        }
        context.clearGoldDebt();
    }

    /**
     * 击败本层 BOSS 后推进进度（v0.0.2 V9）：解锁下一层并清空本层事件计数，第三层则通关。
     *
     * <p>和遗物掉落一样，必须在 {@link #release} 之前判定（那时敌人表还在）。</p>
     */
    private static void grantBossProgress(ServerPlayer caller, CombatContext context) {
        List<MobCombatant> mobs = mobsOf(caller, context);
        if (mobs == null) {
            return;
        }
        boolean boss = false;
        for (MobCombatant mob : mobs) {
            if (MobTiers.tierOf(mob.mob()) == MobTier.BOSS) {
                boss = true;
                break;
            }
        }
        if (!boss) {
            return;
        }
        for (ServerPlayer member : participantsOf(caller, context)) {
            PlayerCardData data = StcAttachments.dataOf(member);
            int defeatedFloor = data.floor();
            // v0.4.6.8：把这一战打掉的 BOSS 记成"已打过"（需求：每只 BOSS 只能打一次）
            ProgressState fought = data.progress();
            for (MobCombatant mob : mobs) {
                if (MobTiers.tierOf(mob.mob()) == MobTier.BOSS) {
                    fought = fought.withBossFought(bossIdOf(mob.mob()));
                }
            }
            data.setProgress(fought);
            ProgressState after = data.recordBossDefeat();
            StcAttachments.syncTo(member);
            if (after.completed()) {
                send(member, "击败第 " + defeatedFloor + " 层 BOSS！三层尖塔全部清空 —— **通关**！");
            } else {
                FloorBoss next = after.boss();
                send(member, "击败第 " + defeatedFloor + " 层 BOSS！进入第 " + after.floor() + " 层，目标【"
                        + (next == null ? "?" : next.name()) + "】，本层事件上限已重置。");
            }
        }
    }

    /**
     * **重进存档时的战斗恢复**（v0.5.0.20）。
     *
     * <p>反馈："玩家意外关闭了游戏后，重启游戏并开启存档会出现立即死亡的情况。
     * 正解：取消最后的战斗。"</p>
     *
     * <p>为什么需要：战斗状态（{@link #COMBATS}）只在**内存**里，进程一结束就没了；
     * 而"本局已战败"这个标记<b>会落盘</b>。意外关闭时 {@code onLoggedOut} 里的
     * {@code quit(...)} 来不及跑完整个流程（它最后会 {@code player.kill()}），
     * 于是存档里可能留下"已战败 / 生命值为 0"的残留状态 —— 重进就像被处决。
     * 这里在登录时把残留状态**取消**掉：清战败标记 → 生命值至少 1 点 → 解锁被冻结的敌人。</p>
     *
     * <p>注意：**不会**清空本局（楼层、卡牌、遗物都保留），只是把"战败"这件事撤销，
     * 因为那场战斗已经不存在了。</p>
     *
     * @return 做了什么（给玩家看的提示）；没需要恢复时返回 {@code null}
     */
    public static String recoverFromInterruptedBattle(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        boolean removedStale = COMBATS.remove(player.getUUID()) != null
                | (TRACKED.remove(player.getUUID()) != null)
                | (BATTLE_HOSTS.values().remove(player.getUUID()));
        boolean wasDefeated = data.progress().defeated();
        boolean lowHp = data.hp() <= 0;

        // 被冻结的敌人：无论哪种情况都要解锁，否则它们会永远僵在那里
        List<MobCombatant> frozen = TRACKED.remove(player.getUUID());
        if (frozen != null) {
            for (MobCombatant combatant : frozen) {
                try {
                    combatant.unlock();
                } catch (RuntimeException ignored) {
                    // 实体可能已经不在了：忽略
                }
            }
        }

        if (!wasDefeated && !lowHp && !removedStale) {
            return null;
        }

        if (wasDefeated) {
            data.setProgress(data.progress().withoutDefeated());
        }
        if (lowHp) {
            data.setLife(data.life().restore());
        }
        StcAttachments.syncTo(player);

        SlayTheCraft.LOGGER.info("[STC] 取消了未正常结束的战斗：玩家={} 战败标记={} 生命值={}（已恢复为 {}/{}）残留战斗={}",
                player.getScoreboardName(), wasDefeated, lowHp ? "<=0" : "正常",
                data.hp(), data.maxHp(), removedStale);
        return "检测到上次的战斗没有正常结束，已为你**取消那场战斗**（生命值已恢复到 "
                + data.hp() + "/" + data.maxHp() + "）。";
    }

    /**
     * 战斗结束（胜负都算）把引擎里的独立生命值写回档案，并在倒下时按配置处理。
     *
     * <p>v0.4.6.3 加了"多人时倒下的人怎么活过来"这条：需求原文"如果战斗胜利，战败玩家会恢复 1 点血量"。
     * 所以**本场战斗中倒下过**（引擎里 HP 已经归 0，且不会中途被救起）的玩家一律只恢复 1 点，
     * 优先级高于 BOSS 战的"回满" —— 那是给活到最后的人的奖励。</p>
     */
    private static void persistLife(ServerPlayer caller, CombatContext context) {
        boolean won = context.phase() == CombatPhase.VICTORY;
        // v0.4.5：需求"每次击败 BOSS 后恢复满血" —— 这一战打赢了 BOSS 就直接回满
        boolean bossVictory = won && battleTier(caller, context) == MobTier.BOSS;
        for (ServerPlayer member : participantsOf(caller, context)) {
            Combatant combatant = context.combatantNamed(member.getName().getString());
            if (combatant == null) {
                continue;
            }
            PlayerCardData data = StcAttachments.dataOf(member);
            LifePool pool = data.life();
            if (combatant.hp() <= 0 && won) {
                // v0.4.6.3：队友打赢了，倒下的你恢复 1 点生命值（不是回满）
                data.setLife(pool.set(1));
                send(member, "你在本场战斗中倒下：队友打赢了，恢复 1 点独立生命值（"
                        + data.hp() + "/" + data.maxHp() + "）。");
            } else if (bossVictory) {
                data.setLife(pool.restore());
                send(member, "击败 BOSS：独立生命值已回满（" + data.hp() + "/" + data.maxHp() + "）。");
            } else if (combatant.hp() <= 0) {
                // 全队倒下：随后 wipeAndRestart 会把本局整个清空（回第 1 层重选角色），这里只保证不留下负值
                if (Config.resetHpOnDefeat()) {
                    data.setLife(pool.restore());
                    send(member, "你倒下了：独立生命值已回满（" + data.hp() + "/" + data.maxHp() + "）。");
                } else {
                    data.setLife(pool.set(1));
                    send(member, "你倒下了：独立生命值保留 1 点。");
                }
            } else {
                data.setLife(pool.set(combatant.hp()));
                send(member, "独立生命值 " + data.hp() + "/" + data.maxHp() + "（跨战斗保留，用火堆回复）。");
            }
            data.recordBattleFinished(won);
            StcAttachments.syncTo(member);
        }
    }

    /**
     * 把"本场战斗要永久移除的牌"真正从卡组里删掉（v0.5.1.6，【不屈】）。
     *
     * <p>引擎只把卡 id 记在 {@link PlayerState#removeFromDeck()} 里（它不认识玩家档案），
     * 而**在此之前没有任何地方消费这个集合** —— 所以【不屈】打完一场后那张牌还会回到牌组，
     * 和卡面写的"彻底从牌组移除"不符。</p>
     *
     * <p>用户给定的语义（v0.5.1.6）："直接从玩家的卡组中删掉（就是删牌）。" 所以这里走
     * {@link PlayerCardData#removeFirstCard(ResourceLocation)} —— 与卡牌回收站**同一个方法**，
     * 连"删牌数 +1"（会推高下次删牌的价格）都一致。胜利与战败都结算：牌是打出去了，
     * 战斗结果不该改变这件事。</p>
     */
    private static void discardRemovedCards(CombatContext context, List<ServerPlayer> roster) {
        for (ServerPlayer member : roster) {
            Combatant combatant = context.combatantNamed(member.getName().getString());
            PlayerState state = combatant == null ? null : context.stateOf(combatant);
            if (state == null || state.removeFromDeck().isEmpty()) {
                continue;
            }
            PlayerCardData data = StcAttachments.dataOf(member);
            int removed = 0;
            StringBuilder names = new StringBuilder();
            for (String cardId : state.removeFromDeck()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, cardId);
                if (data.removeFirstCard(id)) {
                    removed++;
                    Card definition = CardLibrary.byId(cardId);
                    if (names.length() > 0) {
                        names.append('、');
                    }
                    names.append(definition == null ? cardId : definition.name());
                }
            }
            if (removed > 0) {
                StcAttachments.syncTo(member);
                send(member, "【不屈】发动：已从牌组中**永久删除** " + names + "（共 " + removed
                        + " 张，与主动删牌一样计入删牌数）。");
            }
        }
    }

    /**
     * 这场遭遇战属于哪一档（有 BOSS 就是 BOSS、有精英就是精英，否则普通）。
     *
     * <p>用来决定胜利奖励的档位（金币数量 / 是否必出金色）与遗物掉落。</p>
     */
    private static MobTier battleTier(ServerPlayer caller, CombatContext context) {
        List<MobCombatant> mobs = mobsOf(caller, context);
        if (mobs == null) {
            return MobTier.NORMAL;
        }
        MobTier tier = MobTier.NORMAL;
        for (MobCombatant mob : mobs) {
            MobTier current = MobTiers.tierOf(mob.mob());
            if (current == MobTier.BOSS) {
                return MobTier.BOSS;
            }
            if (current == MobTier.ELITE) {
                tier = MobTier.ELITE;
            }
        }
        return tier;
    }

    /**
     * 击败精英 / BOSS **必然**掉落一件遗物（v0.0.2 V7）。
     *
     * <p>按参与者各发一件（每人随机一件尚未拥有的；全部拥有时只提示）；
     * 必须在 {@link #release} 之前调用，因为掉落判定要用到还没被清理的敌人表。</p>
     */
    private static void grantTierRelic(ServerPlayer caller, CombatContext context, MobTier tier) {
        if (tier != MobTier.ELITE && tier != MobTier.BOSS) {
            return;
        }
        for (ServerPlayer member : participantsOf(caller, context)) {
            PlayerCardData data = StcAttachments.dataOf(member);
            Relic relic = RelicLibrary.randomUnowned(new Random(member.getRandom().nextLong()), data.relics());
            if (relic == null) {
                send(member, "精英奖励：所有遗物都已拥有。");
                continue;
            }
            // v0.4.6：走 grantRelic，让【嚒咕菲力】这类遗物的"获得时立刻生效"效果也生效
            data.grantRelic(relic.id());
            StcAttachments.syncTo(member);
            send(member, "击败精英！获得遗物【" + relic.name() + "】—— " + relic.description());
        }
    }

    /** 解散整场遭遇战：把**所有**参与者摘掉并解锁敌人（同一批敌人只解锁一次）。 */
    private static void release(ServerPlayer caller, CombatContext context, String message) {
        // v0.3.0：先把表现层总线摘掉，之后引擎再也不会往网络上发包
        CombatFxBus.detach(context);
        // v0.4.0：分步敌人回合的计时也要清掉
        NEXT_ENEMY_STEP.remove(context);
        BATTLE_HOSTS.remove(context);
        // v0.4.6.3：大标题的记账也清掉
        ANNOUNCED_TURN.remove(context);
        CONCLUDED.remove(context);
        // V9：先把独立生命值写回档案（胜负/放弃都要写），再做解散
        persistLife(caller, context);
        Set<UUID> members = new LinkedHashSet<>();
        for (Map.Entry<UUID, CombatContext> entry : COMBATS.entrySet()) {
            if (entry.getValue() == context) {
                members.add(entry.getKey());
            }
        }
        // v0.4.6.3：战斗结束立刻收掉原版大标题，免得「第 X 回合」飘在奖励/重生界面上
        for (UUID id : members) {
            ServerPlayer member = playerById(caller, id);
            if (member != null) {
                BattleTitles.clear(List.of(member));
            }
        }
        List<MobCombatant> mobs = null;
        for (UUID id : members) {
            COMBATS.remove(id);
            List<MobCombatant> tracked = TRACKED.remove(id);
            if (tracked != null) {
                mobs = tracked;
            }
        }
        if (mobs != null) {
            for (MobCombatant combatant : mobs) {
                combatant.unlock();
            }
        }
        unlockBystanders(context);
        pushInactive(caller, members);
        if (message != null) {
            for (UUID id : members) {
                ServerPlayer member = playerById(caller, id);
                if (member != null) {
                    send(member, message);
                }
            }
        }
    }

    private static ServerPlayer playerById(ServerPlayer caller, UUID id) {
        if (caller.getServer() == null) {
            return null;
        }
        return caller.getServer().getPlayerList().getPlayer(id);
    }

    /** 把某个 Context 的所有参与者（用于广播）。 */
    private static List<ServerPlayer> participantsOf(ServerPlayer caller, CombatContext context) {
        List<ServerPlayer> list = new ArrayList<>();
        for (Map.Entry<UUID, CombatContext> entry : COMBATS.entrySet()) {
            if (entry.getValue() != context) {
                continue;
            }
            ServerPlayer member = playerById(caller, entry.getKey());
            if (member != null) {
                list.add(member);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ V3：站位与冻结

    /**
     * 开战站位（v0.0.2 V3；**测试反馈修正**）。
     *
     * <p>反馈原文："将参与对战的怪物强制移动到玩家当前朝向前方的位置；怪物位置过于分散，影响观感。"</p>
     *
     * <p>现在反过来做：以**发起者的位置与朝向**为基准，把参战怪物排到正前方
     * （{@link EnemyFormation} 负责几何：一字排开 + 浅弧形 + 至少 5 格），
     * 玩家**不再被强制传送**，只是原地转身面向敌人。找不到安全落点的怪物保持原位（绝不硬塞）。</p>
     */
    private static void positionEncounter(ServerPlayer host, List<ServerPlayer> roster,
                                          List<MobCombatant> combatants) {
        if (!Config.battleStandoff() || combatants.isEmpty()) {
            return;
        }
        List<EnemyFormation.Spot> spots = EnemyFormation.compute(host.getX(), host.getZ(), host.getYRot(),
                combatants.size(), Config.battleStandoffDistance());
        int moved = 0;
        for (int i = 0; i < combatants.size() && i < spots.size(); i++) {
            if (PlacementHelper.placeMob(combatants.get(i).mob(), spots.get(i))) {
                moved++;
            }
        }
        // 玩家原地转向敌人重心（不再传送玩家）
        double centroidX = 0.0D;
        double centroidZ = 0.0D;
        int counted = 0;
        for (MobCombatant combatant : combatants) {
            if (combatant.mob().isRemoved()) {
                continue;
            }
            centroidX += combatant.mob().getX();
            centroidZ += combatant.mob().getZ();
            counted++;
        }
        if (counted > 0) {
            centroidX /= counted;
            centroidZ /= counted;
            for (ServerPlayer member : roster) {
                PlacementHelper.faceTowards(member, centroidX, centroidZ);
            }
        }
        send(host, "站位：" + moved + "/" + combatants.size() + " 只敌人被排到你正前方 "
                + Config.battleStandoffDistance() + " 格处（面朝你）。");
    }

    /**
     * 把敌人按"画面**从左到右**"排序（v0.4.0）。
     *
     * <p>需求原文："每个敌人执行行动时，从左到右依次播放动画。" 相机在玩家身后看向敌人，
     * 所以"屏幕向右"= 朝向 × 上方向：设水平朝向为 {@code (fx, fz)}，屏幕右方是 {@code (-fz, fx)}。
     * 把每只敌人相对玩家的位置投影到这个轴上，**投影越小越靠左**，升序排完就是左到右。</p>
     */
    private static List<Enemy> leftToRight(ServerPlayer host, List<Enemy> enemies) {
        double yaw = Math.toRadians(host.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -forwardZ;
        double rightZ = forwardX;
        List<Enemy> sorted = new ArrayList<>(enemies);
        sorted.sort(java.util.Comparator.comparingDouble(enemy -> {
            if (!(enemy instanceof MobCombatant mobCombatant)) {
                return 0.0D;
            }
            double dx = mobCombatant.mob().getX() - host.getX();
            double dz = mobCombatant.mob().getZ() - host.getZ();
            return dx * rightX + dz * rightZ;
        }));
        return sorted;
    }

    /**
     * v0.4.4：按客户端给的"战斗信息栏位置"把怪物摆到栏位正下方（**UI 是基准**）。
     *
     * <p>需求："让怪物实体以战斗 UI 栏位置为基准对齐，而不是让 UI 和实体对齐。UI 的等级更高，
     * 以此显得 UI 整齐。"</p>
     *
     * <p>客户端把自己屏幕上每个信息栏的归一化中心、垂直 FOV 与宽高比发上来；这里用
     * {@code EnemyFormation.computeForCenters} 反算世界坐标并逐个传送（沿用安全落点逻辑）。
     * 怪物顺序与快照一致，所以下标一一对应。</p>
     */
    public static void alignEnemies(ServerPlayer caller, java.util.List<Float> centers, float fov,
                                    float aspect) {
        CombatContext context = COMBATS.get(caller.getUUID());
        if (context == null || centers == null || centers.isEmpty()) {
            return;
        }
        List<MobCombatant> mobs = TRACKED.get(caller.getUUID());
        if (mobs == null || mobs.isEmpty()) {
            return;
        }
        UUID hostId = BATTLE_HOSTS.get(context);
        ServerPlayer host = hostId == null ? caller : playerById(caller, hostId);
        if (host == null) {
            host = caller;
        }
        double[] normalized = new double[Math.min(centers.size(), mobs.size())];
        for (int i = 0; i < normalized.length; i++) {
            Float value = centers.get(i);
            normalized[i] = value == null ? 0.5D : Math.max(0.0D, Math.min(1.0D, value));
        }
        // 俯视角用房主当前的实际俯仰（战斗中客户端会把它压在 battleCameraPitch 上）
        double pitch = Math.abs(host.getXRot()) > 1.0F ? host.getXRot() : Config.battleCameraPitch();
        double standoff = Math.max(Config.battleStandoffDistance(),
                com.slaythecraft.combat.core.StandoffLayout.MIN_DISTANCE);
        double depth = com.slaythecraft.combat.core.EnemyFormation.cameraDepth(standoff, pitch,
                PLAYER_EYE_HEIGHT, THIRD_PERSON_CAMERA_DISTANCE);
        double scale = com.slaythecraft.combat.core.EnemyFormation.lateralScale(depth, fov, aspect);
        List<com.slaythecraft.combat.core.EnemyFormation.Spot> spots =
                com.slaythecraft.combat.core.EnemyFormation.computeForCenters(host.getX(), host.getZ(),
                        host.getYRot(), normalized, standoff, scale);
        int moved = 0;
        for (int i = 0; i < spots.size() && i < mobs.size(); i++) {
            if (PlacementHelper.placeMob(mobs.get(i).mob(), spots.get(i))) {
                moved++;
            }
        }
        if (moved > 0) {
            SlayTheCraft.LOGGER.info("[STC] 按 UI 栏位对齐了 {} 只敌人（fov {} / 宽高比 {} / 横移比例 {}）",
                    moved, fov, aspect, String.format("%.2f", scale));
        }
    }

    /**
     * 顺带冻结战斗半径内**没有参战**的敌对生物：它们既不能过来打玩家，也不会被玩家顺手清掉。
     *
     * <p>范围受控：半径取 {@code freezeRadius}（0 = 跟随 {@code combatRadius}）且**上限 24 格**，
     * 并且只针对敌对生物（players / 被动生物 / 宠物完全不受影响），战斗中每次开战各自记录、结束统一解锁。</p>
     */
    private static void lockBystanders(ServerPlayer host, CombatContext context) {
        if (!Config.freezeNearbyMobs()) {
            return;
        }
        int configured = Config.freezeRadius();
        int radius = configured > 0 ? Math.min(configured, 24) : Math.min(Config.combatRadius(), 24);
        List<Mob> locked = new ArrayList<>();
        for (Mob mob : host.serverLevel().getEntitiesOfClass(Mob.class, host.getBoundingBox().inflate(radius),
                mob -> mob.isAlive() && !mob.isRemoved() && !isLocked(mob)
                        && MobTiers.battleEligible(mob))) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
            mob.setInvulnerable(true);
            locked.add(mob);
        }
        if (!locked.isEmpty()) {
            BYSTANDERS.put(context, locked);
            SlayTheCraft.LOGGER.debug("[STC] 战斗期间顺带冻结 {} 只附近敌对生物（半径 {} 格）", locked.size(), radius);
        }
    }

    /** 战斗结束时把顺带冻结的怪物放回去。 */
    private static void unlockBystanders(CombatContext context) {
        List<Mob> locked = BYSTANDERS.remove(context);
        if (locked == null) {
            return;
        }
        for (Mob mob : locked) {
            if (mob.isRemoved() || !mob.isAlive()) {
                continue;
            }
            mob.setInvulnerable(false);
            mob.setNoAi(false);
        }
    }

    /** 刷新所有人的行动栏（多人时每个人的界面都要跟着更新）。 */
    private static void refreshAll(ServerPlayer caller, CombatContext context) {
        for (ServerPlayer member : participantsOf(caller, context)) {
            refreshActionBar(member, context);
        }
        refreshIntentLabels(caller, context);
        pushBattleState(caller, context);
    }

    // ------------------------------------------------------------------ V2：敌人头顶意图

    /**
     * 把敌人的当前意图刷到头顶（v0.0.2 V2）。
     *
     * <p>每次开局 / 出牌 / 结束回合后都会刷新：意图是每轮重抽的，目标也可能换人；
     * 敌人倒下时这里会顺手把名字牌清掉。</p>
     */
    private static void refreshIntentLabels(ServerPlayer caller, CombatContext context) {
        List<MobCombatant> mobs = mobsOf(caller, context);
        if (mobs == null) {
            return;
        }
        boolean show = Config.showEnemyIntentAboveHead();
        for (MobCombatant mob : mobs) {
            if (show) {
                mob.syncIntentLabel();
            } else {
                mob.clearIntentLabel();
            }
        }
    }

    /** 这场战斗锁定的敌人（同一批敌人只返回一次）。 */
    private static List<MobCombatant> mobsOf(ServerPlayer caller, CombatContext context) {
        for (Map.Entry<UUID, CombatContext> entry : COMBATS.entrySet()) {
            if (entry.getValue() == context) {
                List<MobCombatant> tracked = TRACKED.get(entry.getKey());
                if (tracked != null) {
                    return tracked;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ V1：战斗快照同步

    /** 把"每个人自己视角"的战斗快照推给所有参与者（出牌界面靠它渲染）。 */
    private static void pushBattleState(ServerPlayer caller, CombatContext context) {
        for (ServerPlayer member : participantsOf(caller, context)) {
            PacketDistributor.sendToPlayer(member, BattleStatePayload.of(battleView(context, member)));
        }
    }

    /** 战斗结束/离开时下发"非战斗"快照，客户端据此关掉出牌界面。 */
    private static void pushInactive(ServerPlayer caller, Set<UUID> members) {
        for (UUID id : members) {
            ServerPlayer member = playerById(caller, id);
            if (member != null) {
                PacketDistributor.sendToPlayer(member, BattleStatePayload.of(BattleView.inactive()));
            }
        }
    }

    /** 生成某个玩家视角的快照：手牌/能量/格挡是自己的，敌人列表与意图是共享的。 */
    private static BattleView battleView(CombatContext context, ServerPlayer viewer) {
        String viewerName = viewer.getName().getString();
        Combatant self = context.combatantNamed(viewerName);
        PlayerState state = self == null ? null : context.stateOf(self);
        Combatant active = context.activePlayer();

        List<BattleView.HandCard> hand = new ArrayList<>();
        if (state != null) {
            for (Card card : state.hand()) {
                // v0.5.3.3【布置】：卡面费用用**引擎算出来的实际费用** —— 被布置过的牌在打出前是 0 费，
                // 这样界面显示与实际扣费永远一致（客户端画的就是这个字段）。
                hand.add(new BattleView.HandCard(CardIds.of(card),
                        context.effectiveCost(state, card), card.needsTarget(),
                        // v0.5.4.1（A4-20）：发**档数**（0/1/…/N）而不是"有没有强化过" ——
                        // 手牌里的【灼热打击】+3 才能在客户端显示成 +3 那一档的名字。
                        com.slaythecraft.combat.core.CardLibrary.upgradeLevel(card),
                        card.keywordLabel(), card.enchantmentLabel(),
                        // v0.0.3 W1：卡面数值随状态实时变化（虚弱 2 → 打击显示"造成 4 点伤害"）
                        CardPreview.liveDescription(card, self),
                        // v0.5.3.9（A4-18）：【交锋】这类"条件不满足就打不出去"的牌，客户端要灰掉
                        context.playableNow(state, card)));
            }
        }

        List<BattleView.EnemyView> enemies = new ArrayList<>();
        for (Enemy enemy : context.enemies()) {
            boolean targetingYou = enemy.target() != null && enemy.target().name().equals(viewerName);
            // v0.0.3 W1：敌人效果显示在意图框下面
            String enemyStatuses = String.join(" · ", CardPreview.statusList(enemy));
            enemies.add(new BattleView.EnemyView(enemy.name(), enemy.hp(), enemy.maxHp(), enemy.block(),
                    // v0.5.0.7：意图是"每个行动一行 + 带实际数值"，和头顶名牌共用同一份判定
                    // v0.5.1.6：**数值按观察者算** —— 需求"敌怪的意图对全部玩家统一，但是显示会因为
                    // 不同玩家自身的状态而决定对当前玩家显示的敌怪意图数值"。所以这里把"被瞄准的人"
                    // 换成**看这个快照的玩家自己**（易伤的人看到的数字更高）。
                    enemy.actualIntentLines(self == null ? enemy.target() : self), targetingYou, enemy.alive(),
                    enemyStatuses, enemy.hpLocked()));
        }

        // V8：药水槽位（客户端用 id 去药水表取名字与正负性）
        List<ResourceLocation> potions = new ArrayList<>();
        for (String potionId : StcAttachments.dataOf(viewer).potions()) {
            potions.add(ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, potionId));
        }

        // v0.0.3 W4：充能球（栏位数 + 每颗球）—— 客户端用它把方块环绕在自己腰上
        List<String> selfOrbs = new ArrayList<>();
        if (self != null) {
            for (com.slaythecraft.combat.core.OrbInstance orb : self.orbs()) {
                selfOrbs.add(orb.type().name());
            }
        }

        // v0.4.2：三个牌堆的内容（"查看牌堆"用）
        List<BattleView.CardRef> drawCards = cardRefs(state == null ? null : state.drawPile());
        List<BattleView.CardRef> discardCards = cardRefs(state == null ? null : state.discardPile());
        List<BattleView.CardRef> exhaustCards = cardRefs(state == null ? null : state.exhaustPile());

        // v0.4.6.7：手牌是不是已经到上限（HUD 要在药水栏上方提示"我的手牌太多了..."）
        // 上限来自本场战斗的规则（CombatRules.maxHandSize），客户端自己算不准，所以放进快照
        boolean handFull = state != null && state.hand().size() >= context.rules().maxHandSize();

        return new BattleView(
                true,
                // v0.5.1.6：yourTurn = "玩家回合 + 我还活着 + 我这一轮还没确认"（不再等于"轮到我了"）
                self != null && context.isActive(self),
                active == null ? "" : active.name(),
                context.turn(),
                context.phase().name(),
                state == null ? 0 : state.energy(),
                state == null ? 0 : state.energyPerTurn(),
                hand,
                state == null ? 0 : state.drawPile().size(),
                state == null ? 0 : state.discardPile().size(),
                state == null ? 0 : state.exhaustPile().size(),
                self == null ? 0 : self.hp(),
                self == null ? 0 : self.maxHp(),
                self == null ? 0 : self.block(),
                // v0.0.3 W1：自己的状态逐条列出（客户端画在药水栏左边，换行显示）
                self == null ? List.of() : CardPreview.statusList(self),
                selfOrbs,
                self == null ? 0 : self.orbSlots(),
                enemies,
                potions,
                drawCards,
                discardCards,
                exhaustCards,
                // v0.4.5：BOSS 战标记 + 层号（客户端据此播放该层 BOSS 音乐）
                isBossBattle(context),
                StcAttachments.dataOf(viewer).floor(),
                // v0.4.6：药水栏位（含【药水腰带】加成）+ 参战名单（正面药水可以给队友用）
                StcAttachments.dataOf(viewer).potionSlots(Config.potionSlots()),
                partyMembers(context, viewerName),
                // v0.4.6.7：手牌满标记
                handFull,
                // v0.5.1.6：玩家回合标记（确认之后界面留在屏幕上等队友，靠它区分敌怪回合）
                context.phase() == CombatPhase.PLAYER_TURN);
    }

    /**
     * 参战名单（v0.4.6）：客户端"药水选择目标"页面用；v0.5.1.6 起还带"这一轮是否已确认结束回合"。
     *
     * <p>顺序与 {@code CombatContext.combatants()} 一致，所以客户端回传的
     * {@code ALLY_BASE + i} 能直接映射回参战者。</p>
     */
    private static List<BattleView.PartyMember> partyMembers(CombatContext context, String viewerName) {
        List<BattleView.PartyMember> party = new ArrayList<>();
        for (Combatant combatant : context.combatants()) {
            PlayerState state = context.stateOf(combatant);
            party.add(new BattleView.PartyMember(combatant.name(), combatant.hp(), combatant.maxHp(),
                    combatant.block(), combatant.alive(), combatant.name().equals(viewerName),
                    // v0.5.1.6：倒下的算"已确认"（他不挡推进，界面也不该等他）
                    state == null || !combatant.alive() || state.turnDone()));
        }
        return party;
    }

    /** 这场战斗里是否有 BOSS（BOSS 战才播该层 BOSS 音乐）。 */
    private static boolean isBossBattle(CombatContext context) {
        for (Enemy enemy : context.enemies()) {
            if (enemy instanceof MobCombatant mob && MobTiers.tierOf(mob.mob()) == MobTier.BOSS) {
                return true;
            }
        }
        return false;
    }

    /** 牌堆 → 客户端用的紧凑表示（id + 是否强化；名称/描述客户端自己查卡池）。 */
    private static List<BattleView.CardRef> cardRefs(com.slaythecraft.combat.core.Pile pile) {
        if (pile == null) {
            return List.of();
        }
        List<BattleView.CardRef> refs = new ArrayList<>();
        for (Card card : pile.view()) {
            refs.add(new BattleView.CardRef(CardIds.of(card), card.name().endsWith("+")));
        }
        return refs;
    }

    private static void refreshActionBar(ServerPlayer player, CombatContext context) {
        // v0.4.6.3：多人时倒下的人不会立刻出局 —— 他在局内等队友打完（战斗结束或全军覆没）。
        // 引擎那边 firstLivingPlayer 已经把他的回合跳过了，这里只需要给一句等待提示。
        Combatant self = context.combatantNamed(player.getName().getString());
        if (self != null && !self.alive()) {
            player.displayClientMessage(Component.literal(
                    "你已倒下 —— 等待队友战斗结束（战斗胜利则恢复 1 点生命值，全军覆没则重开本局）"), true);
            return;
        }
        // v0.2.3：行动栏是一行文字，太长会直接顶出屏幕 —— 统一裁剪到 110 字符
        String text = context.actionBar();
        if (text.length() > ACTION_BAR_LIMIT) {
            text = text.substring(0, ACTION_BAR_LIMIT) + "…";
        }
        player.displayClientMessage(Component.literal(text), true);
    }

    /**
     * 战斗系统提示的统一出口（v0.4.6.3 起默认不写进聊天框）。
     *
     * <p>它同时是**引擎战报以外**的战斗提示出口（胜负、奖励、多人加成…）；
     * 引擎战报本身走 {@code StcChat.battle(...)}（{@link #battleLog}）。{@link #beginEncounter} 里
     * {@code CombatLog log = message -> StcChat.battle(roster, message)} 把
     * {@code CombatContext} / {@code CardLibrary} / {@code PotionLibrary} / {@code RelicLibrary}
     * 里的每一条战报都接到这里，所以聊天框的开关一处生效、引擎侧不需要任何改动。
     * 想排查战斗数值时把配置 {@code chatOutput} 打开，战报就会照旧刷出来。</p>
     */
    private static void send(ServerPlayer player, String message) {
        StcChat.note(player, message);
    }

    private static void broadcast(List<ServerPlayer> roster, String message) {
        for (ServerPlayer member : roster) {
            send(member, message);
        }
    }

    /** 战斗战报（引擎每秒会刷几十条）：单独走"战报"通道，默认静音。 */
    private static void battleLog(List<ServerPlayer> roster, String message) {
        StcChat.battle(roster, message);
    }

    private static String names(List<ServerPlayer> players) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                sb.append('、');
            }
            sb.append(players.get(i).getName().getString());
        }
        return sb.toString();
    }
}
