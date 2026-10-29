package com.slaythecraft.data;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.CardEnchantment;

import net.minecraft.resources.ResourceLocation;

/**
 * 玩家档案序列化校验：用 {@link PlayerCardData#CODEC} 走一遍 NBT 往返，
 * 这条路径与 Attachment 落盘 / 同步完全一致（NeoForge 的 {@code serialize(Codec)} 内部也用 {@code NbtOps}）。
 *
 * <p>"重登数据还在不在"是 S1 的核心验收点，而 datagen 不会覆盖它，所以单独做成可重复运行的校验。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;&lt;NeoForm server classpath&gt;" com.slaythecraft.data.CardDataCodecCheck
 * </pre>
 */
public final class CardDataCodecCheck {

    private CardDataCodecCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        PlayerCardData original = new PlayerCardData();
        original.addCard(CardInstance.of(id("strike")));
        original.addCard(CardInstance.of(id("strike")));
        original.addCard(CardInstance.of(id("bash")).withUpgrade());
        original.setGold(437);
        original.setCharacter(id("ironclad"));
        original.recordKill(id("zombie"));
        original.recordKill(id("zombie"));
        original.recordKill(id("creeper"));
        original.addPendingRewards(2);
        original.recordBattleFinished(true);
        original.recordBattleFinished(false);
        original.markInitialized();
        original.removeFirstCard(id("defend"));

        PlayerCardData restored;
        try {
            restored = PlayerCardData.fromTag(original.toTag());
        } catch (RuntimeException e) {
            System.out.println("FAIL  档案编解码往返抛异常：" + e);
            System.out.println("存在失败：0 通过 / 1 失败");
            System.exit(1);
            return;
        }

        check(lines, counters, "卡牌数量保持", original.cardCount() == restored.cardCount(),
                original.cardCount(), restored.cardCount());
        check(lines, counters, "第一张卡 id 保持",
                restored.cards().get(0).id().equals(id("strike")), id("strike"), restored.cards().get(0).id());
        check(lines, counters, "强化层数保持",
                restored.cards().get(2).upgrades() == 1, 1, restored.cards().get(2).upgrades());
        check(lines, counters, "金币保持", restored.gold() == 437, 437, restored.gold());
        check(lines, counters, "角色保持",
                id("ironclad").equals(restored.character()), id("ironclad"), restored.character());
        check(lines, counters, "击杀计数：僵尸", restored.killCount(id("zombie")) == 2, 2,
                restored.killCount(id("zombie")));
        check(lines, counters, "击杀计数：苦力怕", restored.killCount(id("creeper")) == 1, 1,
                restored.killCount(id("creeper")));
        check(lines, counters, "待领奖励保持", restored.pendingRewards() == 2, 2, restored.pendingRewards());
        check(lines, counters, "战斗胜负统计保持", restored.stats().battlesWon() == 1 && restored.stats().battlesLost() == 1,
                "1 胜 1 败", restored.stats().battlesWon() + " 胜 " + restored.stats().battlesLost() + " 败");
        check(lines, counters, "总击杀统计保持", restored.stats().kills() == 3, 3, restored.stats().kills());
        check(lines, counters, "初始化标记保持", restored.initialized(), true, restored.initialized());

        // 删卡计数（removeFirstCard 命中）与未命中两种情况
        PlayerCardData fresh = new PlayerCardData();
        fresh.addCard(CardInstance.of(id("strike")));
        check(lines, counters, "删除存在的卡返回 true", fresh.removeFirstCard(id("strike")), true, true);
        check(lines, counters, "删除后卡牌为空", fresh.cardCount() == 0, 0, fresh.cardCount());
        check(lines, counters, "删除不存在的卡返回 false", !fresh.removeFirstCard(id("strike")), false, true);
        check(lines, counters, "删卡统计已累加", fresh.stats().cardsDeleted() == 1, 1, fresh.stats().cardsDeleted());

        // 金币：余额不足不扣钱
        PlayerCardData wallet = new PlayerCardData();
        wallet.setGold(99);
        check(lines, counters, "金币不足时拒绝消费", !wallet.spendGold(100), false, true);
        check(lines, counters, "被拒绝的消费不动金币", wallet.gold() == 99, 99, wallet.gold());
        wallet.addGold(1);
        check(lines, counters, "刚好够时可以消费", wallet.spendGold(100), true, true);
        check(lines, counters, "消费后余额为 0", wallet.gold() == 0, 0, wallet.gold());

        // 待领奖励：不能领到负数
        PlayerCardData rewards = new PlayerCardData();
        check(lines, counters, "没有待领奖励时领取失败", !rewards.consumePendingReward(), false, true);
        rewards.addPendingRewards(1);
        check(lines, counters, "有待领奖励时领取成功", rewards.consumePendingReward(), true, true);
        check(lines, counters, "领取后计数归零", rewards.pendingRewards() == 0, 0, rewards.pendingRewards());

        // 客户端同步路径：Attachment 的 sync(...) 用的就是这条 StreamCodec
        try {
            io.netty.buffer.ByteBuf buffer = io.netty.buffer.Unpooled.buffer();
            PlayerCardData.STREAM_CODEC.encode(buffer, original);
            PlayerCardData synced = PlayerCardData.STREAM_CODEC.decode(buffer);
            check(lines, counters, "同步流往返：金币", synced.gold() == 437, 437, synced.gold());
            check(lines, counters, "同步流往返：卡牌数", synced.cardCount() == original.cardCount(),
                    original.cardCount(), synced.cardCount());
            check(lines, counters, "同步流往返：待领奖励", synced.pendingRewards() == 2, 2, synced.pendingRewards());
            check(lines, counters, "同步流往返：角色",
                    id("ironclad").equals(synced.character()), id("ironclad"), synced.character());
        } catch (RuntimeException e) {
            check(lines, counters, "同步流往返不抛异常（" + e + "）", false, "正常", e.getClass().getSimpleName());
        }

        // ---------------------------------------------------------- v0.0.2 V6：附魔
        PlayerCardData enchantedArchive = new PlayerCardData();
        enchantedArchive.addCard(CardInstance.of(id("strike"),
                java.util.Set.of(CardEnchantment.SMITE, CardEnchantment.SWEEPING_EDGE)));
        PlayerCardData enchantedBack = PlayerCardData.fromTag(enchantedArchive.toTag());
        check(lines, counters, "附魔随档案落盘往返（张数）",
                enchantedBack.cards().get(0).enchantments().size() == 2, 2,
                enchantedBack.cards().get(0).enchantments().size());
        check(lines, counters, "附魔具体项保持",
                enchantedBack.cards().get(0).enchantments().contains(CardEnchantment.SMITE)
                        && enchantedBack.cards().get(0).enchantments().contains(CardEnchantment.SWEEPING_EDGE),
                true, false);

        DeckBuilder.Result enchantedDeck = DeckBuilder.fromArchive(enchantedArchive.cards());
        check(lines, counters, "组牌时把附魔贴到本场战斗的卡上",
                enchantedDeck.deck().get(0).hasEnchantment(CardEnchantment.SMITE)
                        && enchantedDeck.deck().get(0).hasEnchantment(CardEnchantment.SWEEPING_EDGE),
                true, false);

        PlayerCardData toEnchant = new PlayerCardData();
        toEnchant.addCard(CardInstance.of(id("strike")));
        toEnchant.addCard(CardInstance.of(id("defend")));
        check(lines, counters, "enchantFirstCard 只改第一张匹配的卡",
                toEnchant.enchantFirstCard(id("strike"), CardEnchantment.FIRE_ASPECT)
                        && toEnchant.cards().get(0).enchantments().contains(CardEnchantment.FIRE_ASPECT)
                        && toEnchant.cards().get(1).enchantments().isEmpty(),
                true, false);
        check(lines, counters, "enchantFirstCard 找不到卡时返回 false",
                !toEnchant.enchantFirstCard(id("bash"), CardEnchantment.SMITE), false, true);

        // ---------------------------------------------------------- v0.0.2 V7：遗物
        PlayerCardData relicArchive = new PlayerCardData();
        relicArchive.addRelic("vajra");
        relicArchive.addRelic("anchor");
        PlayerCardData relicBack = PlayerCardData.fromTag(relicArchive.toTag());
        check(lines, counters, "遗物随档案落盘往返", relicBack.relicCount() == 2, 2, relicBack.relicCount());
        check(lines, counters, "遗物 id 保持（能被遗物表解析）",
                relicBack.hasRelic("vajra") && relicBack.hasRelic("anchor")
                        && com.slaythecraft.combat.core.RelicLibrary.relicsOf(relicBack.relics()).size() == 2,
                true, false);
        check(lines, counters, "重复获得同一件遗物不会叠加",
                !relicArchive.addRelic("vajra") && relicArchive.relicCount() == 2, 2, relicArchive.relicCount());

        // ---------------------------------------------------------- v0.0.2 V8：药水
        PlayerCardData potionArchive = new PlayerCardData();
        check(lines, counters, "第 1 瓶药水装进空槽位", potionArchive.addPotion("heal_potion", 3), true, true);
        check(lines, counters, "第 2 瓶药水接在后面", potionArchive.addPotion("poison_potion", 3), true, true);
        check(lines, counters, "药水槽位按加入顺序排列",
                "heal_potion".equals(potionArchive.potionAt(0)) && "poison_potion".equals(potionArchive.potionAt(1)),
                "heal_potion/poison_potion", potionArchive.potionAt(0) + "/" + potionArchive.potionAt(1));
        check(lines, counters, "超出槽位上线的药水装不进去",
                !potionArchive.addPotion("weak_potion", 2) && potionArchive.potionCount() == 2, 2,
                potionArchive.potionCount());
        check(lines, counters, "空槽位返回 null", potionArchive.potionAt(2) == null, null, potionArchive.potionAt(2));
        check(lines, counters, "越界的槽位查询也返回 null", potionArchive.potionAt(9) == null, null,
                potionArchive.potionAt(9));

        PlayerCardData potionBack = PlayerCardData.fromTag(potionArchive.toTag());
        check(lines, counters, "药水随档案落盘往返（瓶数）", potionBack.potionCount() == 2, 2, potionBack.potionCount());
        check(lines, counters, "药水槽位顺序落盘后保持",
                "heal_potion".equals(potionBack.potionAt(0)) && "poison_potion".equals(potionBack.potionAt(1)),
                "heal_potion/poison_potion", potionBack.potionAt(0) + "/" + potionBack.potionAt(1));
        check(lines, counters, "药水 id 能被药水表解析",
                com.slaythecraft.combat.core.PotionLibrary.potionsOf(potionBack.potions()).size() == 2, 2,
                com.slaythecraft.combat.core.PotionLibrary.potionsOf(potionBack.potions()).size());

        String taken = potionArchive.takePotionAt(0);
        check(lines, counters, "用掉药水会返回它的 id", "heal_potion".equals(taken), "heal_potion", taken);
        check(lines, counters, "用掉后后面的药水顶上来且瓶数 -1",
                potionArchive.potionCount() == 1 && "poison_potion".equals(potionArchive.potionAt(0)), 1,
                potionArchive.potionCount());
        check(lines, counters, "用掉空槽位返回 null", potionArchive.takePotionAt(2) == null, null,
                potionArchive.takePotionAt(2));
        potionArchive.clearPotions();
        check(lines, counters, "清空后没有药水", potionArchive.potionCount() == 0, 0, potionArchive.potionCount());

        PlayerCardData noPotions = PlayerCardData.fromTag(new PlayerCardData().toTag());
        check(lines, counters, "老档案（无 potions 字段）读出来是空列表", noPotions.potionCount() == 0, 0,
                noPotions.potionCount());

        // ---------------------------------------------------------- v0.0.2 V9：独立生命值与三层进度
        PlayerCardData lifeArchive = PlayerCardData.fromTag(new PlayerCardData().toTag());
        check(lines, counters, "新档案默认满血 75", lifeArchive.hp() == 75 && lifeArchive.maxHp() == 75, "75/75",
                lifeArchive.life().describe());
        check(lines, counters, "新档案默认第 1 层", lifeArchive.floor() == 1, 1, lifeArchive.floor());
        check(lines, counters, "新档案默认没有击败 BOSS",
                lifeArchive.progress().bossesDefeated().isEmpty() && !lifeArchive.progress().completed(), true,
                false);

        PlayerCardData progressArchive = new PlayerCardData();
        progressArchive.setLife(progressArchive.life().damage(23));
        progressArchive.recordEvent(com.slaythecraft.combat.core.EventKind.NORMAL,
                com.slaythecraft.combat.core.EventCaps.DEFAULT);
        progressArchive.recordEvent(com.slaythecraft.combat.core.EventKind.ELITE,
                com.slaythecraft.combat.core.EventCaps.DEFAULT);
        progressArchive.recordBossDefeat();
        progressArchive.recordEvent(com.slaythecraft.combat.core.EventKind.CAMPFIRE,
                com.slaythecraft.combat.core.EventCaps.DEFAULT);

        PlayerCardData progressBack = PlayerCardData.fromTag(progressArchive.toTag());
        check(lines, counters, "受伤后的生命值落盘往返", progressBack.hp() == 52 && progressBack.maxHp() == 75,
                "52/75", progressBack.life().describe());
        check(lines, counters, "层数落盘往返", progressBack.floor() == 2, 2, progressBack.floor());
        check(lines, counters, "进阶后本层事件计数从 0 开始（只有火堆 1 次）",
                progressBack.progress().countOf(com.slaythecraft.combat.core.EventKind.CAMPFIRE) == 1
                        && progressBack.progress().countOf(com.slaythecraft.combat.core.EventKind.NORMAL) == 0,
                "火堆 1 / 常规 0",
                progressBack.progress().countOf(com.slaythecraft.combat.core.EventKind.CAMPFIRE) + "/"
                        + progressBack.progress().countOf(com.slaythecraft.combat.core.EventKind.NORMAL));
        check(lines, counters, "已击败 BOSS 落盘往返",
                progressBack.progress().bossDefeated(1) && !progressBack.progress().bossDefeated(2), true, false);
        check(lines, counters, "进阶后目标换成第 2 层的代表 BOSS（v0.4.5 起僵尸疣猪兽）",
                "僵尸疣猪兽".equals(progressBack.progress().boss() == null ? null
                        : progressBack.progress().boss().name()),
                "僵尸疣猪兽",
                progressBack.progress().boss() == null ? null : progressBack.progress().boss().name());

        PlayerCardData completedArchive = new PlayerCardData();
        completedArchive.recordBossDefeat();
        completedArchive.recordBossDefeat();
        completedArchive.recordBossDefeat();
        PlayerCardData completedBack = PlayerCardData.fromTag(completedArchive.toTag());
        check(lines, counters, "通关状态落盘往返",
                completedBack.progress().completed() && completedBack.progress().bossesDefeated().size() == 3, true,
                completedBack.progress().completed());

        PlayerCardData cappedProgress = new PlayerCardData();
        for (int i = 0; i < 15; i++) {
            cappedProgress.recordEvent(com.slaythecraft.combat.core.EventKind.NORMAL,
                    com.slaythecraft.combat.core.EventCaps.DEFAULT);
        }
        check(lines, counters, "档案层也会挡住超出上限的事件记录",
                !cappedProgress.recordEvent(com.slaythecraft.combat.core.EventKind.NORMAL,
                        com.slaythecraft.combat.core.EventCaps.DEFAULT)
                        && cappedProgress.progress().countOf(com.slaythecraft.combat.core.EventKind.NORMAL) == 15,
                15, cappedProgress.progress().countOf(com.slaythecraft.combat.core.EventKind.NORMAL));

        // 下一场战斗开始时清空未领奖励
        PlayerCardData rewardWipe = new PlayerCardData();
        rewardWipe.addPendingRewards(3);
        check(lines, counters, "开战时清空未领奖励并返回清掉的个数", rewardWipe.clearPendingOffers() == 3
                && rewardWipe.pendingRewards() == 0, 3, rewardWipe.pendingRewards());
        check(lines, counters, "没有待领奖励时清空返回 0", rewardWipe.clearPendingOffers() == 0, 0,
                rewardWipe.clearPendingOffers());

        // 生命值上限改小时当前生命跟着降（配置改动 / 老档案兼容）
        PlayerCardData shrink = new PlayerCardData();
        shrink.setLife(shrink.life().withMax(40));
        check(lines, counters, "上限调小后当前生命被裁到上限", shrink.hp() == 40 && shrink.maxHp() == 40, "40/40",
                shrink.life().describe());
        shrink.healHp(100);
        check(lines, counters, "治疗不会越过上限", shrink.hp() == 40, 40, shrink.hp());
        shrink.damageHp(1000);
        check(lines, counters, "伤害下限是 0（不会变负数）", shrink.hp() == 0, 0, shrink.hp());

        // V9 开发版的平铺格式（life_hp / floor / event_counts …）也要能读回来
        net.minecraft.nbt.CompoundTag flat = progressArchive.toTag();
        flat.remove("life");
        flat.remove("progress");
        flat.putInt("life_hp", 52);
        flat.putInt("life_max", 75);
        flat.putInt("floor", 2);
        net.minecraft.nbt.CompoundTag flatCounts = new net.minecraft.nbt.CompoundTag();
        flatCounts.putInt("ELITE", 1);
        flat.put("event_counts", flatCounts);
        flat.putIntArray("bosses_defeated", new int[] {1});
        flat.putBoolean("completed", false);
        PlayerCardData flatBack = PlayerCardData.fromTag(flat);
        check(lines, counters, "老平铺格式：生命值仍能读回", flatBack.hp() == 52 && flatBack.maxHp() == 75, "52/75",
                flatBack.life().describe());
        check(lines, counters, "老平铺格式：层数与已击败 BOSS 仍能读回",
                flatBack.floor() == 2 && flatBack.progress().bossDefeated(1), "第 2 层 / 已击败第 1 层",
                flatBack.floor() + " / " + flatBack.progress().bossesDefeated());

        // v0.4.5：问号事件的"本层访问记录"要能存进档案（否则重登又能重复访问）
        PlayerCardData visitedArchive = new PlayerCardData();
        visitedArchive.markQuestionVisited("armorer");
        check(lines, counters, "标记本层访问过的问号事件", visitedArchive.hasVisitedQuestion("armorer"), true,
                visitedArchive.hasVisitedQuestion("armorer"));
        PlayerCardData visitedBack = PlayerCardData.fromTag(visitedArchive.toTag());
        check(lines, counters, "访问记录能存进档案并读回", visitedBack.hasVisitedQuestion("armorer"), true,
                visitedBack.hasVisitedQuestion("armorer"));
        check(lines, counters, "没访问过的事件读回后仍是 false", !visitedBack.hasVisitedQuestion("nitwit"), false,
                visitedBack.hasVisitedQuestion("nitwit"));
        visitedBack.recordBossDefeat();
        check(lines, counters, "进阶后档案里的访问记录被清空（下一层可以再访问）",
                !visitedBack.hasVisitedQuestion("armorer"), false, visitedBack.hasVisitedQuestion("armorer"));

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    private static void check(List<String> lines, int[] counters, String label, boolean condition,
                             Object expected, Object actual) {
        if (condition) {
            counters[0]++;
            lines.add("PASS  " + label);
        } else {
            counters[1]++;
            lines.add("FAIL  " + label + "（期望 " + expected + "，实际 " + actual + "）");
        }
    }
}
