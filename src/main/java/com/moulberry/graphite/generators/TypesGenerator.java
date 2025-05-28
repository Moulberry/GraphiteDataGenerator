package com.moulberry.graphite.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TypesGenerator implements DataGenerator {
    @Override
    public String name() {
        return "types";
    }

    @Override
    public JsonObject run() {
        JsonObject allTypesJson = new JsonObject();

        addType(allTypesJson, AdvancementType.class);
        addType(allTypesJson, Armadillo.ArmadilloState.class);
        addType(allTypesJson, BossEvent.BossBarColor.class);
        addType(allTypesJson, BossEvent.BossBarOverlay.class);
        addType(allTypesJson, ChatVisiblity.class, "chat_visibility");
        addType(allTypesJson, ClickType.class);
        addType(allTypesJson, Direction.class);
        addType(allTypesJson, DisplaySlot.class);
        addType(allTypesJson, DyeColor.class);
        addType(allTypesJson, EquipmentSlot.class);
        addType(allTypesJson, HumanoidArm.class);
        addType(allTypesJson, InteractionHand.class);
        addType(allTypesJson, MapPostProcessing.class);
        addType(allTypesJson, ServerboundPlayerCommandPacket.Action.class, "move_action");
        addType(allTypesJson, ServerboundPlayerActionPacket.Action.class, "hand_action");
        addType(allTypesJson, ClientboundPlayerInfoUpdatePacket.Action.class, "player_list_action");
        addType(allTypesJson, ObjectiveCriteria.RenderType.class, "objective_render_type");
        addType(allTypesJson, ParticleStatus.class);
        addType(allTypesJson, Relative.class, "relative_movement");
        addType(allTypesJson, Pose.class);
        addType(allTypesJson, Rarity.class);
        addType(allTypesJson, Sniffer.State.class, "sniffer_state");
        addType(allTypesJson, SoundSource.class);
        addType(allTypesJson, ItemUseAnimation.class);
        addType(allTypesJson, ServerboundClientCommandPacket.Action.class, "client_action");
        addType(allTypesJson, Display.BillboardConstraints.class, "billboard_constraint");

        addStaticConstants(allTypesJson, ClientboundAnimatePacket.class, int.class, Function.identity(), "entity_animation");
        addStaticConstants(allTypesJson, LevelEvent.class, int.class, Function.identity(), "level_event");
        addStaticConstants(allTypesJson, ClientboundGameEventPacket.class, ClientboundGameEventPacket.Type.class, t -> t.id, "game_event");

        return allTypesJson;
    }

    private static void addType(JsonObject jsonObject, Class<? extends Enum<?>> enumClass) {
        addType(jsonObject, enumClass, null);
    }

    private static void addType(JsonObject jsonObject, Class<? extends Enum<?>> enumClass, @Nullable String alternativeName) {
        JsonArray enumValues = new JsonArray();
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant instanceof StringRepresentable stringRepresentable) {
                enumValues.add(stringRepresentable.getSerializedName());
            } else {
                enumValues.add(enumConstant.name().toLowerCase(Locale.ROOT));
            }
        }
        if (alternativeName != null) {
            jsonObject.add(alternativeName, enumValues);
        } else {
            String name = enumClass.getSimpleName();
            name = name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(Locale.ROOT);
            jsonObject.add(name, enumValues);
        }
    }

    private static <T> void addStaticConstants(JsonObject jsonObject, Class<?> clazz, Class<T> constantType, Function<T, Integer> indexer, String name) {
        JsonObject staticValues = new JsonObject();
        for (Field declaredField : clazz.getDeclaredFields()) {
            if ((declaredField.getModifiers() & Modifier.STATIC) == 0) {
                continue;
            }
            if (declaredField.getType() != constantType) {
                continue;
            }

            declaredField.trySetAccessible();

            try {
                T t = (T) declaredField.get(null);
                staticValues.addProperty(declaredField.getName().toLowerCase(Locale.ROOT), indexer.apply(t));
            } catch (Exception ignored) {}
        }
        jsonObject.add(name, staticValues);
    }



}
