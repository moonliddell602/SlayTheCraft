package com.slaythecraft.combat.core;

import java.util.Objects;

/**
 * 一瓶药水（v0.0.2 V8；v0.4.6 增加"目标范围 / 贴图形态 / 上色"）。
 *
 * <p>药水效果只使用 {@link CombatContext} / {@link Combatant} 的公开 API，因此**不需要改引擎**：
 * 新增药水 = 在 {@link PotionLibrary} 里登记一条数据。</p>
 *
 * @param id          稳定 id（存档只存它）
 * @param name        展示名
 * @param description 展示描述
 * @param kind        正负性质，决定能指向哪一方
 * @param form        贴图形态（常规 / 喷溅 / 滞留 / 指定物品）
 * @param color       原版药水贴图的上色（{@code 0xRRGGBB}；{@link PotionForm#ITEM} 时忽略）
 * @param itemId      {@link PotionForm#ITEM} 时使用的物品 id（其余情况为空串）
 * @param selfOnly    true = 只能对自己使用（需求里点名"对自己使用"的那几瓶），
 *                    false = 正面药水可以给自己或队友
 * @param effect      使用时的效果；{@code target} 对负面药水是选中的敌人，对正面药水是自己/队友
 */
public record Potion(String id, String name, String description, PotionKind kind, PotionForm form,
                     int color, String itemId, boolean selfOnly, PotionEffect effect) {

    /** 药水效果。 */
    @FunctionalInterface
    public interface PotionEffect {
        void apply(CombatContext context, Combatant user, Combatant target);
    }

    public Potion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        form = form == null ? PotionForm.NORMAL : form;
        itemId = itemId == null ? "" : itemId;
        Objects.requireNonNull(effect, "effect");
    }

    /** 正面药水：可以给自己 / 队友（除非 {@link #selfOnly()}）。 */
    public boolean canTargetAlly() {
        return kind.canTargetAlly() && !selfOnly;
    }

    /** 负面药水：只能对敌人。 */
    public boolean canTargetEnemy() {
        return kind.canTargetEnemy();
    }

    /** 只能对自己使用。 */
    public boolean onlySelf() {
        return selfOnly;
    }

    /** 这个形态是不是"指定物品贴图"。 */
    public boolean usesItemIcon() {
        return form == PotionForm.ITEM;
    }

    /**
     * {@link PotionForm#ITEM} 时图标用的**物品贴图**路径（{@code textures/item/<物品路径>.png}）。
     *
     * <p>需求（v0.4.6.7）："不祥之瓶（药水）的图标修订：不祥之瓶，而不是滞留药水。" 客户端
     * （{@code PotionIcons}）是按物品 id 去注册表取物品模型渲染的，这里给出的是**同一张原版物品贴图**
     * 的位置，供界面校核与自检断言"这瓶药水到底画的是哪张图"；不使用物品贴图的形态返回空串。</p>
     */
    public String itemTexturePath() {
        if (!usesItemIcon() || itemId.isEmpty()) {
            return "";
        }
        int colon = itemId.indexOf(':');
        String path = colon < 0 ? itemId : itemId.substring(colon + 1);
        return path.isEmpty() ? "" : "textures/item/" + path + ".png";
    }

    /** 展示文本：{@code 治疗药水（正面）—— 回复 10 点生命。} */
    public String describe() {
        return name + "（" + kind.label() + "）—— " + description;
    }
}
