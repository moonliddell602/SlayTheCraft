package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：请求打开卡牌收纳包界面（快捷键触发）。
 *
 * <p>只是"请打开"这一个信号：界面数据走玩家档案的 Attachment 同步，不需要另开数据包（设计文档 §14）。</p>
 */
public record OpenBinderPayload() implements CustomPacketPayload {

    public static final OpenBinderPayload INSTANCE = new OpenBinderPayload();

    public static final CustomPacketPayload.Type<OpenBinderPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "open_binder"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBinderPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
