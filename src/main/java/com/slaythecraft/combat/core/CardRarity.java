package com.slaythecraft.combat.core;

/**
 * 卡牌品质（设计文档 §5.3）。
 *
 * <p>⚠️ **v0.5.1.5 起没有任何卡牌使用 {@link #GOLD}** —— 需求："统一目前每个角色的金色和稀有卡，
 * 全部将品质统一为稀有。" 原来的 5 张金色牌（含强化版共 10 个定义）已经改成 {@link #RARE}，
 * BOSS 奖励也改成从**稀有卡池**抽取。枚举值保留，是为了不破坏附属包 / 存档 / 商店定价里可能存在的引用
 * （`CardShopOffers.priceOf` 的 switch 仍然要求它存在）。自检里有一条"没有任何卡还是金色品质"盯着这件事。</p>
 *
 * <p>刻意留在 {@code combat/core}（无 Minecraft 依赖）：品质只是数值与展示属性，
 * 将来卡牌定义搬到数据包注册表后，这个枚举仍然是共用类型。</p>
 */
public enum CardRarity {
    COMMON("普通", 6),
    UNCOMMON("罕见", 3),
    RARE("稀有", 1),
    /** 旧 BOSS 专属品质；**v0.5.1.5 起已无卡牌使用**（保留以免破坏外部引用）。 */
    GOLD("金色", 1);

    private final String label;
    private final int weight;

    CardRarity(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String label() {
        return label;
    }

    /** 随机卡牌奖励里的抽取权重。 */
    public int weight() {
        return weight;
    }

    /** 展示用颜色（ARGB），界面里暂时不用贴图也能区分品质。 */
    public int color() {
        return switch (this) {
            case COMMON -> 0xFF9A9AA4;
            case UNCOMMON -> 0xFF4AA3D8;
            case RARE -> 0xFFD8C24A;
            case GOLD -> 0xFFFFB300;
        };
    }
}
