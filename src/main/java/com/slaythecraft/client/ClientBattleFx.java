package com.slaythecraft.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.slaythecraft.combat.core.BattleAnim;
import com.slaythecraft.combat.core.CombatFx;
import com.slaythecraft.combat.core.FxSequence;
import com.slaythecraft.network.BattleFxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端战斗表现层（v0.3.0）：把服务端下发的 {@link BattleFxPayload} 变成**音效 + 动作动画 + 粒子**。
 *
 * <p>对应需求：</p>
 * <ul>
 *   <li>攻击（玩家与敌人一样）→ 向前迅速移动一下再回到原位 + 玩家暴击音效；</li>
 *   <li>防御（格挡）→ 快速蹲起一次 + 原版穿护甲音效；</li>
 *   <li>玩家受到伤害 → 后退一步后快速回原位 + 玩家受伤音效；</li>
 *   <li>伤害被格挡完全吸收 → 盾牌格挡音效（不位移）；</li>
 *   <li>v0.5.0.5 敌人特殊效果 → 唤魔者施法音效 + 药水色粒子（{@link CombatFx#ENEMY_CAST}）；</li>
 *   <li>v0.5.0.5 多段攻击 → 按段数排队播 N 次动画，且每段比单次快 1/4（见 {@link BattleAnim#multiHitMs}）。</li>
 * </ul>
 *
 * <p>动画只改**渲染**位置（见 {@link CombatAnimLayer}），不动真实实体坐标，所以不会与服务端打架。</p>
 */
public final class ClientBattleFx {

    /** 一个实体当前正在播的动作。 */
    public record Animation(CombatFx kind, long startedAt, long duration) {
        public float progress(long now) {
            return (float) BattleAnim.progress(now, startedAt, duration);
        }
    }

    private static final Map<Integer, Animation> ANIMATIONS = new HashMap<>();

    /**
     * **排队的动作**（v0.4.6.7 多段攻击）。
     *
     * <p>需求："如果敌人执行的是多段攻击（例如：造成 1 点伤害 × 次），则播放 × 次攻击动画。"
     * 同一个实体的多段攻击会在同一瞬间到达，而 {@link #ANIMATIONS} 一个实体只存得下一个动画
     * （后到覆盖先到）—— 所以后面的段排队，等前一段播完再上。</p>
     */
    private static final Map<Integer, ArrayDeque<Slot>> QUEUES = new HashMap<>();
    /** 每个实体已排到的时间轴（决定下一段什么时候开始）。 */
    private static final Map<Integer, FxSequence> SEQUENCES = new HashMap<>();

    /** 排队中的一段动作：要播动画的实体 + 动画 + 它开始时该播的音效。 */
    private record Slot(int entityId, Animation animation, SoundEvent sound, float volume, float pitch,
                        int soundTarget) {
    }

    /** 敌人施法时撒出的"药水粒子"颜色（紫色，和原版负面药水一致）。 */
    private static final int CAST_COLOR = 0x8B4FC6;

    /**
     * **音效音量整体放大系数**（v0.5.2.5）。
     *
     * <p>需求原文："音效音量大小提高四分之一（现在的音效声量有点低了，至少我是这么认为）" ——
     * 也就是 ×1.25。所有战斗音效都走 {@link #playAt}，所以在这里乘一次就够；
     * 界面按钮音效（{@code UiFeedback}）也引用这个常量，保持同一套音量。</p>
     */
    public static final float SFX_VOLUME_SCALE = 1.25F;

    private ClientBattleFx() {
    }

    /** 收到一个表现层事件。 */
    public static void accept(BattleFxPayload payload) {
        CombatFx kind = payload.kind();
        if (kind == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        int actor = payload.actor();
        int target = payload.target();
        int hits = payload.hits();
        switch (kind) {
            case PLAYER_ATTACK, ENEMY_ATTACK -> {
                // 多段攻击：攻击者按段排队前冲（每段比单次快 1/4）；单段走原路径
                playHits(actor, kind, BattleAnim.ATTACK_MS, hits);
                playAt(minecraft, actor >= 0 ? actor : target,
                        SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 1.0F);
            }
            case PLAYER_BLOCK, ENEMY_BLOCK -> {
                // v0.4.1：敌人获得格挡也用和玩家一样的方式（蹲一下 + 穿甲音效）
                start(actor, kind, BattleAnim.DEFEND_MS);
                // 需求："播放原版穿上护甲的音效"（ARMOR_EQUIP_* 在原版里是 Holder）
                playAt(minecraft, actor, SoundEvents.ARMOR_EQUIP_IRON.value(), 0.9F, 1.0F);
            }
            case PLAYER_HURT -> {
                // 受击也按段数排队：多段攻击要一段一段地退
                playHits(target, kind, BattleAnim.HURT_MS, hits);
                playAt(minecraft, target, SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
            }
            case PLAYER_HURT_BLOCKED -> {
                // 需求："如果玩家格挡住攻击，则播放盾牌格挡攻击的音效"（不位移）
                playAt(minecraft, target, SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
            }
            case ENEMY_HURT -> playHits(target, kind, BattleAnim.HURT_MS, hits);
            case ORB_DAMAGE -> {
                // v0.5.1.7 需求："每颗充能球执行的动画应该分开，而不是在出牌回合结束时一次性全部结算。
                // 如：现在有三颗充能球，则播放三次充能球攻击敌怪的动画。"
                //
                // 以前这里是 start(target, ENEMY_HURT, ...)：一个实体只存得下**一个**动画（后到覆盖先到），
                // 而三颗球的事件在同一瞬间到达 → 看起来只打了一下。现在改成**排队**（与多段攻击同一套机制）：
                // 每颗球占一格时间轴，有几颗就依次播几次受击动画 + 依次响几声。
                int victim = target >= 0 ? target : actor;
                enqueue(victim, CombatFx.ENEMY_HURT, BattleAnim.HURT_MS,
                        SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.8F, victim);
                // 电流冲击那一声音量很低（0.25），仍然即时播放 —— 它和三声钟声叠在一起不明显
                playAt(minecraft, victim, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.25F, 1.9F);
            }
            case ORB_BLOCK -> {
                // v0.5.1.7：冰球同理 —— 每颗球蹲起一次（并各响一声），而不是 N 颗球只响一声
                enqueue(actor, CombatFx.PLAYER_BLOCK, BattleAnim.DEFEND_MS,
                        SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.2F, actor);
            }
            case ENEMY_CAST -> {
                // v0.5.0.5 需求："播放唤魔者施法的音效，并在敌人实体放出药水粒子特效"
                start(actor, kind, BattleAnim.DEFEND_MS);
                playAt(minecraft, actor, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
                ringParticles(minecraft, actor, 10);
                // 目标（被施加效果的那位）也撒一圈，指明"效果落在了谁身上"
                if (target >= 0 && target != actor) {
                    ringParticles(minecraft, target, 8);
                }
            }
            case ENEMY_EXPLODE -> {
                // v0.5.0.8 需求："在结算时播放爆炸动画。"
                // 用原版爆炸音效 + 爆炸粒子（EXPLOSION / EXPLOSION_EMITTER），不做真实爆炸
                // （不破坏地形、不伤及无辜），只播放"表现"。
                start(actor, kind, BattleAnim.EXPLODE_MS);
                playAt(minecraft, actor, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.0F);
                burstParticles(minecraft, actor);
            }
            case FROST_CHANNELLED, FOUR_SAME_CARDS -> {
                // v0.5.0.10：这两个是**成就触发器**（集中时生成冰球 / 连续 4 张同名手牌），
                // 本身没有专门的表现——成就的提示由原版 toast 负责。
                // 这里给一点轻微的音效反馈，免得玩家完全不知道发生了什么。
                playAt(minecraft, actor, SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 1.4F);
            }
        }
    }

    /**
     * 按**段数**播放一个动作：1 段走原来的"立刻开始"，多段则排队依次播。
     *
     * <p>v0.5.0.5 需求："敌人的多段攻击动画会被按照单次攻击结算。应为攻击多少次就播放多少次动画。"
     * 以及"多段攻击时，每一段的攻击动画应该比单次攻击的动画速度快四分之一。"</p>
     *
     * @param baseDuration 单次动作的时长；多段时每段用 {@link BattleAnim#multiHitMs} 缩短
     */
    private static void playHits(int entityId, CombatFx kind, long baseDuration, int hits) {
        if (hits <= 1) {
            start(entityId, kind, baseDuration);
            return;
        }
        if (entityId < 0) {
            return;
        }
        long per = BattleAnim.multiHitMs(baseDuration);
        for (int i = 0; i < hits; i++) {
            queue(entityId, kind, per);
        }
    }

    /** 某个实体当前的动作；没有则返回 {@code null}（排队中的下一段到点会自动接上）。 */
    public static Animation animationOf(int entityId) {
        long now = System.currentTimeMillis();
        Animation animation = ANIMATIONS.get(entityId);
        if (animation != null
                && BattleAnim.finished(now, animation.startedAt(), animation.duration())) {
            ANIMATIONS.remove(entityId);
            animation = null;
        }
        if (animation != null) {
            return animation;
        }
        // 上段播完了：到点的排队动作接上
        ArrayDeque<Slot> queue = QUEUES.get(entityId);
        while (queue != null && !queue.isEmpty()) {
            Slot next = queue.peekFirst();
            if (next.animation().startedAt() > now) {
                return null;
            }
            queue.pollFirst();
            startNow(next);
            return next.animation();
        }
        return null;
    }

    /** 排队一段**无音效**的动作（v0.5.0.5 多段攻击用）。 */
    private static void queue(int entityId, CombatFx kind, long duration) {
        enqueue(entityId, kind, duration, null, 0.0F, 1.0F, entityId);
    }

    /**
     * 排队一段动作：立刻开始，或者接在当前时间轴后面（多段攻击）。
     *
     * @param entityId    做动作的实体
     * @param soundTarget 音效要挂在哪个实体身上（-1 = 玩家自己）
     */
    private static void enqueue(int entityId, CombatFx kind, long duration, SoundEvent sound, float volume,
                                float pitch, int soundTarget) {
        if (entityId < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        FxSequence sequence = SEQUENCES.computeIfAbsent(entityId, key -> new FxSequence());
        long startAt = sequence.schedule(now, duration);
        Slot slot = new Slot(entityId, new Animation(kind, startAt, duration), sound, volume, pitch,
                soundTarget >= 0 ? soundTarget : entityId);
        if (startAt <= now) {
            startNow(slot);
        } else {
            // addLast：多段按调用顺序先后播，不能倒过来
            QUEUES.computeIfAbsent(entityId, key -> new ArrayDeque<>()).addLast(slot);
        }
    }

    /** 真正开始一段动作：登记动画 + 播它自己的音效。 */
    private static void startNow(Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (slot.sound() != null && minecraft.level != null) {
            playAt(minecraft, slot.soundTarget(), slot.sound(), slot.volume(), slot.pitch());
        }
        ANIMATIONS.put(slot.entityId(), slot.animation());
    }

    /** 战斗结束/断线时清空，避免残留动画。 */
    public static void clear() {
        ANIMATIONS.clear();
        QUEUES.clear();
        SEQUENCES.clear();
    }

    /** 进行中的动画数量（调试用）。 */
    public static int activeCount() {
        return ANIMATIONS.size();
    }

    /** 已经排队的段数（自检/调试用；v0.5.0.5 多段攻击）。 */
    public static int queuedCount() {
        int total = 0;
        for (ArrayDeque<Slot> queue : QUEUES.values()) {
            total += queue.size();
        }
        return total;
    }

    private static void start(int entityId, CombatFx kind, long duration) {
        if (entityId < 0) {
            return;
        }
        // 后发的事件覆盖前一个：连续受击时每次都从头播
        ANIMATIONS.put(entityId, new Animation(kind, System.currentTimeMillis(), duration));
    }

    /** 在实体位置播一个本地音效（听不到就不会串到别的玩家那里）。 */
    private static void playAt(Minecraft minecraft, int entityId, SoundEvent sound, float volume, float pitch) {
        if (minecraft.level == null) {
            return;
        }
        Entity entity = entityId < 0 ? null : minecraft.level.getEntity(entityId);
        Vec3 pos = entity == null
                ? (minecraft.player == null ? null : minecraft.player.position())
                : entity.position();
        if (pos == null) {
            return;
        }
        minecraft.level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS,
                volume * SFX_VOLUME_SCALE, pitch, false);
    }

    /**
     * 在实体周围撒一圈**药水色粒子**（v0.5.0.5）。
     *
     * <p>需求："并在敌人实体放出药水粒子特效（作为显示敌人对玩家施加效果的动画）。"
     * 用的就是原版药水效果粒子 {@link ParticleTypes#ENTITY_EFFECT}，颜色取负面药水的紫色。</p>
     *
     * @param count 这一帧撒几颗
     */
    private static void ringParticles(Minecraft minecraft, int entityId, int count) {
        if (minecraft.level == null || entityId < 0) {
            return;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        Vec3 pos = entity.position();
        ColorParticleOption option = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, CAST_COLOR);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            minecraft.level.addParticle(option,
                    pos.x + Math.cos(angle) * 0.7D,
                    pos.y + 0.6D + (i % 3) * 0.25D,
                    pos.z + Math.sin(angle) * 0.7D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    /**
     * 自爆用的**爆炸粒子**（v0.5.0.8）。
     *
     * <p>用原版的爆炸粒子 {@link ParticleTypes#EXPLOSION} / {@link ParticleTypes#EXPLOSION_EMITTER}，
     * 只做表现、不做真实爆炸（不破坏地形、不额外伤及无辜）。</p>
     */
    private static void burstParticles(Minecraft minecraft, int entityId) {
        if (minecraft.level == null || entityId < 0) {
            return;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        Vec3 pos = entity.position();
        // 中心来一发大的
        minecraft.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 0.6D, pos.z, 0.0D, 0.0D, 0.0D);
        // 周围再补一圈小的，看起来更"炸"
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double radius = 0.9D;
            minecraft.level.addParticle(ParticleTypes.EXPLOSION,
                    pos.x + Math.cos(angle) * radius,
                    pos.y + 0.4D + (i % 3) * 0.35D,
                    pos.z + Math.sin(angle) * radius,
                    0.0D, 0.0D, 0.0D);
        }
    }

    /** 清掉已经播完的动画（每帧调用一次即可，渲染层也会顺手清理）。 */    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Animation>> iterator = ANIMATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next().getValue();
            if (BattleAnim.finished(now, animation.startedAt(), animation.duration())) {
                iterator.remove();
            }
        }
        QUEUES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
