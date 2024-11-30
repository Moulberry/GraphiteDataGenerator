package com.moulberry.graphite.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EntityGenerator implements DataGenerator {

    private final ServerLevel level;

    public EntityGenerator(ServerLevel level) {
        this.level = level;
    }

    @Override
    public String name() {
        return "entities";
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public JsonObject run() {
        JsonObject allEntitiesJson = new JsonObject();

        Map<EntityDataSerializer, String> entityDataSerializerNames = new HashMap<>();
        for (Field declaredField : EntityDataSerializers.class.getDeclaredFields()) {
            if (declaredField.getType().isAssignableFrom(EntityDataSerializer.class)) {
                declaredField.trySetAccessible();
                EntityDataSerializer serializer;
                try {
                    if ((declaredField.getModifiers() & Modifier.STATIC) != 0) {
                        serializer = (EntityDataSerializer) declaredField.get(null);
                    } else {
                        throw new RuntimeException("Non-static entity data serializer");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                entityDataSerializerNames.put(serializer, declaredField.getName().toLowerCase(Locale.ROOT));
            }
        }

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            JsonObject entityJson = new JsonObject();

            Entity entity;
            if (entityType == EntityType.PLAYER) {
                entity = new Player(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "Dummy")) {
                    @Override
                    public boolean isSpectator() {
                        return false;
                    }

                    @Override
                    public boolean isCreative() {
                        return false;
                    }
                };
            } else {
                entity = entityType.create(level, EntitySpawnReason.COMMAND);
            }

            entityJson.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getId(entityType));
            entityJson.addProperty("translationKey", entityType.getDescriptionId());
            entityJson.addProperty("height", entityType.getDimensions().height());
            entityJson.addProperty("width", entityType.getDimensions().width());

            Map<EntityDataAccessor, String> accessorNames = new HashMap<>();
            Class<?> entityClass = entity.getClass();
            while (entityClass != Object.class) {
                for (Field declaredField : entityClass.getDeclaredFields()) {
                    if (declaredField.getType().isAssignableFrom(EntityDataAccessor.class)) {
                        declaredField.trySetAccessible();
                        EntityDataAccessor accessor;
                        try {
                            if ((declaredField.getModifiers() & Modifier.STATIC) != 0) {
                                accessor = (EntityDataAccessor) declaredField.get(null);
                            } else {
                                accessor = (EntityDataAccessor) declaredField.get(entity);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }

                        accessorNames.put(accessor, declaredField.getName());
                    }
                }
                entityClass = entityClass.getSuperclass();
            }

            JsonArray allMetadata = new JsonArray();
            for (SynchedEntityData.DataItem<?> dataItem : entity.getEntityData().itemsById) {
                JsonObject metadata = new JsonObject();
                String name = accessorNames.get(dataItem.getAccessor());
                name = name.toLowerCase(Locale.ROOT);
                name = name.replace("data_", "");
                name = name.replace("_id", "");
                name = name.replace("_", " ").trim().replace(" ", "_");

                metadata.addProperty("name", name);
                metadata.addProperty("serializer", entityDataSerializerNames.get(dataItem.getAccessor().serializer()));

                Object dataValue = dataItem.getValue();
                if (dataValue instanceof Number number) {
                    metadata.addProperty("defaultValue", number);
                } else if (dataValue instanceof String string) {
                    metadata.addProperty("defaultValue", string);
                } else if (dataValue instanceof Boolean bool) {
                    metadata.addProperty("defaultValue", bool);
                } else if (dataValue instanceof Character character) {
                    metadata.addProperty("defaultValue", character);
                }

                allMetadata.add(metadata);
            }

            entityJson.add("metadata", allMetadata);

            String key = DataGenerator.simplifyResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
            allEntitiesJson.add(key, entityJson);
        }

        return allEntitiesJson;
    }

}
