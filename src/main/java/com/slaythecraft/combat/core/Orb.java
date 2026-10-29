package com.slaythecraft.combat.core;

/**
 * 充能球的**类型**（v0.0.3 W4；v0.3.0 补全四种）。
 *
 * <p>参考原版《杀戮尖塔》故障机器人的充能球：球占用"充能球栏位"（默认 3 格），
 * **回合结束时依次触发被动**；栏位满了再获得新球时，会先把**最前面那颗**激发掉
 * （激发 = 一次性大效果）。</p>
 *
 * <p>四种球（数值全部按原版）：</p>
 * <ul>
 *   <li>{@link #LIGHTNING} 闪电球：被动 3 点伤害（随机敌人），激发 8 点伤害 —— 黄色蛙鸣灯；</li>
 *   <li>{@link #FROST} 冰球：被动 2 点格挡，激发 5 点格挡 —— 蓝冰；</li>
 *   <li>{@link #DARK} 暗球：被动每回合"充能 +6"，激发时对**当前生命最低的敌人**造成等同于充能的伤害
 *       —— 哭泣的黑曜石；</li>
 *   <li>{@link #PLASMA} 等离子球：被动是**回合开始时 +1 能量**（不占用"回合结束被动"），
 *       激发时 +2 能量 —— 品红色染色玻璃。</li>
 * </ul>
 *
 * <p>{@code blockId} 是客户端渲染用的方块 id（空栏位用标靶），放在 core 里是为了让"外观"也有单一来源。</p>
 */
public enum Orb {

    LIGHTNING("闪电球", 3, 8, 0, 0, 0, "minecraft:ochre_froglight"),
    FROST("冰球", 2, 5, 0, 0, 0, "minecraft:blue_ice"),
    DARK("暗球", 0, 6, 0, 0, 6, "minecraft:crying_obsidian"),
    PLASMA("等离子球", 0, 0, 1, 2, 0, "minecraft:magenta_stained_glass");

    /** 空栏位显示的方块（需求：以标靶代替空栏位）。 */
    public static final String EMPTY_SLOT_BLOCK = "minecraft:target";

    private final String label;
    private final int passiveValue;
    private final int evokeValue;
    private final int passiveEnergy;
    private final int evokeEnergy;
    private final int growth;
    private final String blockId;

    Orb(String label, int passiveValue, int evokeValue, int passiveEnergy, int evokeEnergy, int growth,
        String blockId) {
        this.label = label;
        this.passiveValue = passiveValue;
        this.evokeValue = evokeValue;
        this.passiveEnergy = passiveEnergy;
        this.evokeEnergy = evokeEnergy;
        this.growth = growth;
        this.blockId = blockId;
    }

    public String label() {
        return label;
    }

    /** 回合结束时的被动数值（闪电=伤害，冰=格挡；暗球/等离子不用这个字段）。 */
    public int passiveValue() {
        return passiveValue;
    }

    /**
     * 激发时的数值。
     *
     * <p>对暗球来说是"**初始充能**"（原版 6 点），真正的伤害取 {@link OrbInstance#charge()}。</p>
     */
    public int evokeValue() {
        return evokeValue;
    }

    /** 回合开始时给的能量（等离子球 = 1）。 */
    public int passiveEnergy() {
        return passiveEnergy;
    }

    /** 激发时给的能量（等离子球 = 2）。 */
    public int evokeEnergy() {
        return evokeEnergy;
    }

    /** 回合结束时的"充能增长"（暗球 = 6）；0 表示这颗球不成长。 */
    public int growth() {
        return growth;
    }

    /** 这颗球是否带"充能"（暗球）。 */
    public boolean grows() {
        return growth > 0;
    }

    public String blockId() {
        return blockId;
    }

    /**
     * 这颗球的功能描述（v0.4.6 需求："将鼠标悬停在充能球上持续 0.6 秒，可以查看充能球功能描述"）。
     *
     * <p>描述放在 core 里，与战斗规则同一份数据，界面只是把它显示出来。</p>
     */
    public String description() {
        return switch (this) {
            case LIGHTNING -> "被动：回合结束时对随机敌人造成 " + passiveValue + " 点伤害。激发：对随机敌人造成 "
                    + evokeValue + " 点伤害。";
            case FROST -> "被动：回合结束时获得 " + passiveValue + " 点格挡。激发：获得 " + evokeValue + " 点格挡。";
            case DARK -> "被动：回合结束时充能 +" + growth + "。激发：对当前生命最低的敌人造成等同于充能的伤害。";
            case PLASMA -> "被动：回合开始时 +" + passiveEnergy + " 能量。激发：+" + evokeEnergy + " 能量。";
        };
    }

    /** 空栏位（标靶）的说明。 */
    public static String emptySlotDescription() {
        return "空的充能球栏位：生成新球时会占用这里；栏位满时最前面的那颗会被先激发。";
    }
}
