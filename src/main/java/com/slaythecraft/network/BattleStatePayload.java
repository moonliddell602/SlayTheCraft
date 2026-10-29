package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：某个玩家视角的战斗快照（V1 出牌界面用）。
 *
 * <p>只在开局 / 出牌 / 结束回合 / 战斗结束时推送，不做逐 tick 同步。</p>
 */
public record BattleStatePayload(CompoundTag tag) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BattleStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "battle_state"));

    /** COMPOUND_TAG 的缓冲区类型是 ByteBuf，因此这里也声明成 ByteBuf（注册时接受 super 类型）。 */
    public static final StreamCodec<ByteBuf, BattleStatePayload> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(BattleStatePayload::new, BattleStatePayload::tag);

    public static BattleStatePayload of(BattleView view) {
        return new BattleStatePayload(view.toTag());
    }

    public BattleView view() {
        return BattleView.fromTag(tag);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
