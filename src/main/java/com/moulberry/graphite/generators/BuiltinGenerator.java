package com.moulberry.graphite.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BuiltinGenerator implements DataGenerator {

    private final ServerLevel serverLevel;

    public BuiltinGenerator(ServerLevel serverLevel) {
        this.serverLevel = serverLevel;
    }

    @Override
    public String name() {
        return "builtin";
    }

    @Override
    public JsonObject run() {
        JsonObject allRegistriesJson = new JsonObject();

        for (Registry registry : BuiltInRegistries.REGISTRY) {
            if (registry == BuiltInRegistries.BLOCK || registry == BuiltInRegistries.FLUID || registry == BuiltInRegistries.BLOCK_TYPE || registry == BuiltInRegistries.ITEM ||
                    registry == BuiltInRegistries.ENTITY_TYPE || registry == BuiltInRegistries.REGISTRY || registry == BuiltInRegistries.PARTICLE_TYPE) {
                continue;
            }

            record FieldWithName(Field field, String name) {}
            List<FieldWithName> serializeFields = null;

            JsonObject registryJsonObject = new JsonObject();
            for (Object value : registry) {
                JsonObject registryValueJson = new JsonObject();
                String key = DataGenerator.simplifyResourceLocation(registry.getKey(value));

                if (value instanceof MenuType<?> menuType) {
                    Inventory inventory = new Inventory(new Player(this.serverLevel, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "Dummy")) {
                        @Override
                        public boolean isSpectator() {
                            return false;
                        }

                        @Override
                        public boolean isCreative() {
                            return false;
                        }

                        @Override
                        public @Nullable GameType gameMode() {
                            return GameType.DEFAULT_MODE;
                        }
                    }, new EntityEquipment());
                    var menu = menuType.create(1, inventory);
                    registryValueJson.addProperty("slot_count", menu.slots.size());
                }
                if (value instanceof ResourceLocation resourceLocation) {
                    registryValueJson.addProperty("location", resourceLocation.toString());
                } else {
                    if (serializeFields == null) {
                        serializeFields = new ArrayList<>();

                        Class<?> clazz = value.getClass();
                        while (clazz != Object.class) {
                            for (Field declaredField : clazz.getDeclaredFields()) {
                                if ((declaredField.getModifiers() & Modifier.STATIC) != 0) {
                                    continue;
                                }
                                declaredField.trySetAccessible();

                                String name = declaredField.getName();
                                if (name.contains("$") || name.contains("hashCode")) {
                                    continue;
                                }
                                name = name.replaceAll("([a-z])([A-Z])", "$1_$2");
                                name = name.toLowerCase(Locale.ROOT);
                                serializeFields.add(new FieldWithName(declaredField, name));
                            }
                            clazz = clazz.getSuperclass();
                        }
                    }

                    for (FieldWithName fieldWithName : serializeFields) {
                        try {
                            Object fieldValue = fieldWithName.field.get(value);
                            if (fieldValue instanceof Number number) {
                                registryValueJson.addProperty(fieldWithName.name(), number);
                            } else if (fieldValue instanceof String string) {
                                registryValueJson.addProperty(fieldWithName.name(), string);
                            } else if (fieldValue instanceof ResourceLocation resourceLocation) {
                                registryValueJson.addProperty(fieldWithName.name(), resourceLocation.toString());
                            } else if (fieldValue instanceof Boolean bool) {
                                registryValueJson.addProperty(fieldWithName.name(), bool);
                            } else if (fieldValue instanceof Character character) {
                                registryValueJson.addProperty(fieldWithName.name(), character);
                            }

                        } catch (Exception ignored) {}
                    }
                }

                registryJsonObject.add(key, registryValueJson);
            }
            allRegistriesJson.add(DataGenerator.simplifyResourceLocation(registry.key().location()), registryJsonObject);
        }

        return allRegistriesJson;
    }

}
