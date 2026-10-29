package com.slaythecraft.merchant;

import java.util.function.Supplier;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 实体注册：卡牌商人（继承原版村民，外观一致，但右键开的是我们的商店）。 */
public final class StcEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SlayTheCraft.MODID);

    public static final Supplier<EntityType<CardMerchant>> CARD_MERCHANT = ENTITIES.register("card_merchant",
            () -> EntityType.Builder.of(CardMerchant::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "card_merchant").toString()));

    private StcEntities() {
    }

    /** 卡牌商人沿用原版村民的属性（血量/移速）。 */
    public static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(CARD_MERCHANT.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
    }
}
