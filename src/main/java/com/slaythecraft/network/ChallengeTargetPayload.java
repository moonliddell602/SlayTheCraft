package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：挑战**名单里点的那一只**（v0.5.0.9）。
 *
 * <p>尖塔入口的"本层可挑战名单"里点某一只时发送；服务端校验"本层允许 + 半径内真的有这一只"
 * 之后开战（见 {@code CombatManager.challengeTarget}）。</p>
 */
public record ChallengeTargetPayload(String entityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChallengeTargetPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "challenge_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChallengeTargetPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ChallengeTargetPayload::entityId,
                    ChallengeTargetPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
