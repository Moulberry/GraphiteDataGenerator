package com.moulberry.graphite.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.function.Function;

public class ItemsGenerator implements DataGenerator {
    @Override
    public String name() {
        return "items";
    }

    @Override
    public JsonObject run() {
        JsonObject allItemsJson = new JsonObject();

        for (Item item : BuiltInRegistries.ITEM) {
            JsonObject itemJson = new JsonObject();

            itemJson.addProperty("id", BuiltInRegistries.ITEM.getId(item));
            itemJson.addProperty("translationKey", item.getDescriptionId());
            itemJson.addProperty("maxStackSize", item.getDefaultMaxStackSize());
            Block correspondingBlock = Block.byItem(item);
            if (correspondingBlock != Blocks.AIR) {
                String blockKey = DataGenerator.simplifyResourceLocation(BuiltInRegistries.BLOCK.getKey(correspondingBlock));
                itemJson.addProperty("correspondingBlock", blockKey);
            }

            String key = DataGenerator.simplifyResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            allItemsJson.add(key, itemJson);
        }

        return allItemsJson;
    }

}
