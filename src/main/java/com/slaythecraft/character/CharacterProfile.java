package com.slaythecraft.character;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * 角色定义（设计文档 §7；v0.0.3 W3 增加描述与初始遗物；v0.4.0 增加充能球栏位）。
 *
 * @param id            角色 id，例如 {@code stc:ironclad}
 * @param maxHp         开局最大生命（当前只影响原版血条；战斗用独立生命值，见 V9）
 * @param energyPerTurn 每回合能量
 * @param startingDeck  起始牌组（卡牌 id 列表，允许重复）
 * @param startingRelic 初始遗物的 id（开新局时自动发放；空串 = 没有）
 * @param orbSlots      充能球栏位数（v0.4.0：**只有故障机器人是 3，其他角色是 0**，界面上不再显示空球位）
 * @param description   角色描述（角色详情页显示）
 */
public record CharacterProfile(ResourceLocation id, int maxHp, int energyPerTurn,
                               List<ResourceLocation> startingDeck, String startingRelic, int orbSlots,
                               String description) {

    public CharacterProfile {
        startingDeck = List.copyOf(startingDeck);
        startingRelic = startingRelic == null ? "" : startingRelic;
        orbSlots = Math.max(0, orbSlots);
        description = description == null ? "" : description;
    }

    /** 这个角色会不会用充能球。 */
    public boolean usesOrbs() {
        return orbSlots > 0;
    }

    /** 有没有初始遗物。 */
    public boolean hasStartingRelic() {
        return !startingRelic.isBlank();
    }

    /** 初始遗物对象；没有或 id 不认识时返回 {@code null}。 */
    public com.slaythecraft.combat.core.Relic startingRelicDefinition() {
        return hasStartingRelic() ? com.slaythecraft.combat.core.RelicLibrary.byId(startingRelic) : null;
    }

    /** 名称的 lang key，例如 {@code character.stc.ironclad}。 */
    public String nameKey() {
        return "character." + id.getNamespace() + "." + id.getPath();
    }

    /** 角色描述的 lang key（描述文本走语言文件，方便中英双语）。 */
    public String descriptionKey() {
        return "character." + id.getNamespace() + "." + id.getPath() + ".desc";
    }

    /** 推荐使用的键位：{@code stc:ironclad} → {@code ironclad}。 */
    public String shortName() {
        return id.getPath();
    }
}
