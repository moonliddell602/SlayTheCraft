package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.CombatFx;
import com.slaythecraft.combat.core.CombatFxEvent;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：一次战斗**表现层事件**（v0.3.0）。
 *
 * <p>引擎在打出攻击牌 / 上格挡 / 敌人攻击 / 玩家掉血时广播 {@link CombatFxEvent}，
 * 服务端把它转成这个包发给参战的所有玩家；客户端据此播放动作动画与原版音效。</p>
 *
 * <p>沿用 {@link BattleStatePayload} 的做法：用 CompoundTag 承载，字段增删不必动协议结构。</p>
 */
public record BattleFxPayload(CompoundTag tag) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BattleFxPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "battle_fx"));

    public static final StreamCodec<ByteBuf, BattleFxPayload> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(BattleFxPayload::new, BattleFxPayload::tag);

    public static BattleFxPayload of(CombatFxEvent event) {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", event.kind().name());
        tag.putInt("actor", event.actorFxId());
        tag.putInt("target", event.targetFxId());
        // v0.5.0.5：多段攻击的段数（客户端排队播 N 次动画）。老的读法不受影响。
        tag.putInt("hits", event.hits());
        return new BattleFxPayload(tag);
    }

    /** 事件类型；名字认不出来时返回 {@code null}（客户端直接忽略）。 */
    public CombatFx kind() {
        try {
            return CombatFx.valueOf(tag.getString("kind"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 动作发起者的实体编号（没有则 -1）。 */
    public int actor() {
        return tag.getInt("actor");
    }

    /** 目标的实体编号（没有则 -1）。 */
    public int target() {
        return tag.getInt("target");
    }

    /**
     * 这段动作的**段数**（v0.5.0.5 多段攻击）：≥1，缺省 1。
     *
     * <p>用 {@code getInt} 读不到时返回 0，所以这里夹到 1，保证老包也能正常工作。</p>
     */
    public int hits() {
        return Math.max(1, tag.getInt("hits"));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
