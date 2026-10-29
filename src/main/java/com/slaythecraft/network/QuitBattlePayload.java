package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：放弃当前战斗（v0.2.3）。
 *
 * <p>ESC 只收起界面（几秒后自动弹回），真正"离开战斗"必须走这个显式操作，
 * 这样敌人不会被永久留在冻结状态、玩家也不会被动卡死。</p>
 */
public record QuitBattlePayload() implements CustomPacketPayload {

    public static final QuitBattlePayload INSTANCE = new QuitBattlePayload();

    public static final CustomPacketPayload.Type<QuitBattlePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "quit_battle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuitBattlePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
