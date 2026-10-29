package com.slaythecraft.character;

import com.slaythecraft.data.PlayerCardData;

import net.minecraft.resources.ResourceLocation;

/**
 * 角色选择 / 开新一局的纯数据逻辑（不碰实体，便于无头校验）。
 *
 * <p>规则（v0.0.2 测试反馈修正后）：</p>
 * <ol>
 *   <li>第一次选角色 → 开一局新局，发它的起始牌组；</li>
 *   <li>**本局没结束（通关三层 / 战败）之前不能换角色** —— 这就是"切角色会重复添加初始卡牌"的根治办法，
 *       以前切到没开过局的新角色会在旧牌组之上再发一套牌；</li>
 *   <li>本局结束后再选（换角色）→ 开新局：清空卡牌/遗物/药水/进度并重发起始牌组（见 {@link RunService}）。</li>
 * </ol>
 */
public final class CharacterService {

    /** 选择结果，交给调用方决定提示文案。 */
    public enum Result {
        /** 首次选择该角色，已开了一局新局并发放起始牌组 */
        STARTED,
        /** 上一局已结束，用这个角色开了一局新局 */
        RESTARTED,
        /** 点的就是当前角色 */
        ALREADY_ACTIVE,
        /** 本局仍在进行中：不允许换角色（测试反馈要求） */
        LOCKED
    }

    private CharacterService() {
    }

    public static Result select(PlayerCardData data, CharacterProfile profile) {
        ResourceLocation id = profile.id();
        ResourceLocation current = data.character();
        if (id.equals(current)) {
            return Result.ALREADY_ACTIVE;
        }
        if (current != null && !data.isRunOver()) {
            // 本局还没结束（没通关、也没战败）：锁住角色
            return Result.LOCKED;
        }
        boolean firstEver = current == null;
        RunService.resetForNewRun(data, profile);
        return firstEver ? Result.STARTED : Result.RESTARTED;
    }
}
