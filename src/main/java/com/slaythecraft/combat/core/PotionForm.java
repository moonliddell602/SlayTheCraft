package com.slaythecraft.combat.core;

/**
 * 药水的**贴图形态**（v0.4.6 美术需求）。
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>"对敌人使用的药水使用喷溅的对应效果药水贴图"；</li>
 *   <li>"对自己/队友使用的药水使用常规的对应效果药水贴图"；</li>
 *   <li>"特殊药水使用滞留的对应效果药水贴图"。</li>
 * </ul>
 *
 * <p>所以形态只描述"画成哪一类物品"，具体显示由客户端按 {@link Potion#color()} 给原版
 * 药水贴图上色（{@code PotionContents}），{@link #ITEM} 则直接用指定物品的贴图
 * （例如【谜之炖菜】用 {@code minecraft:suspicious_stew}）。</p>
 */
public enum PotionForm {
    /** 常规药水（对自己 / 队友）。 */
    NORMAL("常规"),
    /** 喷溅药水（对敌人）。 */
    SPLASH("喷溅"),
    /** 滞留药水（特殊药水）。 */
    LINGERING("滞留"),
    /** 直接用某个原版物品的贴图（谜之炖菜 / 蘑菇煲）。 */
    ITEM("物品");

    private final String label;

    PotionForm(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
