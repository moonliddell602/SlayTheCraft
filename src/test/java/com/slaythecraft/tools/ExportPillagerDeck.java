package com.slaythecraft.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;

import net.minecraft.resources.ResourceLocation;

/**
 * 导出叛逃掠夺者的**初始牌组**（v0.5.1.0，一次性工具）。
 *
 * <p>初始牌组定义在 {@code CharacterRegistry}（MC 层），所以本工具要用 MC 类路径跑，
 * 并且**不能碰注册表**（`Bootstrap` 没跑，碰了会 ExceptionInInitializerError）——
 * 只读 {@code CharacterRegistry} 的静态字段是安全的。</p>
 */
public final class ExportPillagerDeck {

    private ExportPillagerDeck() {
    }

    public static void main(String[] args) throws Exception {
        CharacterProfile profile = CharacterRegistry.byId(
                ResourceLocation.fromNamespaceAndPath("stc", "pillager"));
        if (profile == null) {
            System.out.println("找不到叛逃掠夺者角色");
            return;
        }
        StringBuilder md = new StringBuilder();
        md.append("\n## 初始牌组（").append(profile.startingDeck().size()).append(" 张）\n\n");
        md.append("> 来源：`CharacterRegistry.PILLAGER`（选择这个角色时发放）。\n");
        md.append("> 生命上限 **").append(profile.maxHp()).append("**，每回合能量 **")
                .append(profile.energyPerTurn()).append("**，充能球栏位 **")
                .append(profile.orbSlots()).append("**。\n\n");
        md.append("| # | 卡牌 | id | 数量 |\n| ---: | --- | --- | ---: |\n");

        // 按出现顺序统计（同 id 合并）
        java.util.LinkedHashMap<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (ResourceLocation id : profile.startingDeck()) {
            counts.merge(id.getPath(), 1, Integer::sum);
        }
        int i = 1;
        for (var entry : counts.entrySet()) {
            Card definition = CardLibrary.byId(entry.getKey());
            String name = definition == null ? "（不在卡池里）" : definition.name();
            md.append("| ").append(i++).append(" | ").append(name).append(" | `")
                    .append(entry.getKey()).append("` | ").append(entry.getValue()).append(" |\n");
        }

        // 初始遗物
        md.append("\n## 初始遗物\n\n");
        if (profile.hasStartingRelic()) {
            var relic = com.slaythecraft.combat.core.RelicLibrary.byId(profile.startingRelic());
            md.append("【").append(relic == null ? profile.startingRelic() : relic.name()).append("】`")
                    .append(profile.startingRelic()).append("`");
            if (relic != null) {
                md.append(" —— ").append(relic.description());
            }
            md.append("\n");
        } else {
            md.append("（无）\n");
        }

        Path target = Path.of("docs", "pillager-deck-section.md");
        Files.writeString(target, md.toString(), StandardCharsets.UTF_8);
        System.out.println("已导出初始牌组（" + profile.startingDeck().size() + " 张）到 " + target);
    }
}
