package com.slaythecraft.network;

import java.util.List;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：**战斗信息栏的屏幕位置**（v0.4.4）。
 *
 * <p>需求："让怪物实体以战斗 UI 栏位置为基准对齐，而不是让 UI 和实体对齐。UI 的等级更高，
 * 以此显得 UI 整齐。"</p>
 *
 * <p>所以客户端把自己屏幕上每个敌人信息栏的**归一化中心**（0..1）连同垂直 FOV 与窗口宽高比发给服务端，
 * 服务端据此把怪物摆到"正好投影在这些栏位下面"的世界坐标上（见
 * {@code EnemyFormation.computeForCenters}）。</p>
 *
 * @param centers 每个敌人信息栏的归一化中心横坐标（从左到右，和快照里的敌人顺序一致）
 * @param fov     客户端垂直视野角（度）
 * @param aspect  客户端窗口宽高比
 */
public record BattleAlignPayload(List<Float> centers, float fov, float aspect)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BattleAlignPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "battle_align"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BattleAlignPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), BattleAlignPayload::centers,
                    ByteBufCodecs.FLOAT, BattleAlignPayload::fov,
                    ByteBufCodecs.FLOAT, BattleAlignPayload::aspect,
                    BattleAlignPayload::new);

    public BattleAlignPayload {
        centers = List.copyOf(centers);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
