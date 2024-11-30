package com.moulberry.graphite.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockGenerator implements DataGenerator {
    @Override
    public String name() {
        return "blocks";
    }

    @Override
    public JsonObject run() {
        JsonObject allBlocksJson = new JsonObject();

        BlockAttributes defaultAttributes = BlockAttributes.createDefault();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState defaultBlockState = block.defaultBlockState();
            JsonObject blockJsonObject = new JsonObject();

            Item correspondingItem = Item.BY_BLOCK.get(block);
            if (correspondingItem != null) {
                String correspondingItemName = DataGenerator.simplifyResourceLocation(BuiltInRegistries.ITEM.getKey(correspondingItem));
                blockJsonObject.addProperty("correspondingItem", correspondingItemName);
            }

            // Attributes
            BlockAttributes blockAttributes = BlockAttributes.fromState(defaultBlockState);
            JsonObject blockAttributesJson = blockAttributes.write(defaultAttributes);

            blockJsonObject.add("attributes", blockAttributesJson);

            JsonObject allStateAttributesJson = new JsonObject();

            int minStateId = Integer.MAX_VALUE;
            for (BlockState possibleState : block.getStateDefinition().getPossibleStates()) {
                int stateId = Block.BLOCK_STATE_REGISTRY.getId(possibleState);
                minStateId = Math.min(minStateId, stateId);

                BlockAttributes stateAttributes = BlockAttributes.fromState(possibleState);
                JsonObject stateAttributesJson = stateAttributes.write(blockAttributes);

                if (!stateAttributesJson.isEmpty()) {
                    allStateAttributesJson.add(stateId+"", stateAttributesJson);
                }
            }

            blockJsonObject.addProperty("minStateId", minStateId);
            blockJsonObject.add("stateAttributes", allStateAttributesJson);

            // Properties
            JsonObject propertiesJson = new JsonObject();
            for (Property property : block.getStateDefinition().getProperties()) {
                JsonObject propertyJson = new JsonObject();
                if (property instanceof BooleanProperty booleanProperty) {
                    propertyJson.addProperty("type", "bool");
                    boolean defaultValue = defaultBlockState.getValue(booleanProperty);
                    propertyJson.addProperty("defaultValue", defaultValue);
                } else if (property instanceof IntegerProperty integerProperty) {
                    propertyJson.addProperty("type", "int");
                    int defaultValue = defaultBlockState.getValue(integerProperty);
                    propertyJson.addProperty("defaultValue", defaultValue);

                    JsonArray values = new JsonArray();
                    for (int possibleValue : integerProperty.getPossibleValues()) {
                        values.add(possibleValue);
                    }
                    propertyJson.add("values", values);
                } else {
                    propertyJson.addProperty("type", "string");
                    String defaultValue = property.getName(defaultBlockState.getValue(property));
                    propertyJson.addProperty("defaultValue", defaultValue);

                    JsonArray values = new JsonArray();
                    for (Object possibleValue : property.getPossibleValues()) {
                        values.add(property.getName((Comparable) possibleValue));
                    }
                    propertyJson.add("values", values);
                }
                propertiesJson.add(property.getName(), propertyJson);
            }
            blockJsonObject.add("properties", propertiesJson);

            allBlocksJson.add(DataGenerator.simplifyResourceLocation(BuiltInRegistries.BLOCK.getKey(block)), blockJsonObject);
        }
        return allBlocksJson;
    }

    private record BlockAttributes(
        float hardness,
        boolean replaceable,
        boolean air,
        boolean blocksMotion,
        boolean waterlogged,
        boolean isPathfindableLand,
        boolean isPathfindableAir,
        boolean isPathfindableWater,
        int lightEmission,
        boolean fallDamageResetting,
        List<AABB> collisionShape
    ) {
        public static BlockAttributes createDefault() {
            return new BlockAttributes(
                0.0f,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                0,
                false,
                null
            );
        }

        public static BlockAttributes fromState(BlockState blockState) {
            boolean isWaterlogged = blockState.getFluidState().is(Fluids.WATER) || blockState.getFluidState().is(Fluids.FLOWING_WATER);
            return new BlockAttributes(
                blockState.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                blockState.canBeReplaced(),
                blockState.isAir(),
                blockState.blocksMotion(),
                isWaterlogged,
                blockState.isPathfindable(PathComputationType.LAND),
                blockState.isPathfindable(PathComputationType.AIR),
                blockState.isPathfindable(PathComputationType.WATER),
                blockState.getLightEmission(),
                blockState.is(BlockTags.FALL_DAMAGE_RESETTING) || isWaterlogged,
                blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs()
            );
        }

        public JsonObject write(@Nullable BlockGenerator.BlockAttributes defaultValues) {
            JsonObject jsonObject = new JsonObject();
            if (defaultValues == null || this.hardness != defaultValues.hardness) {
                jsonObject.addProperty("hardness", this.hardness);
            }
            if (defaultValues == null || this.replaceable != defaultValues.replaceable) {
                jsonObject.addProperty("replaceable", this.replaceable);
            }
            if (defaultValues == null || this.air != defaultValues.air) {
                jsonObject.addProperty("air", this.air);
            }
            if (defaultValues == null || this.blocksMotion != defaultValues.blocksMotion) {
                jsonObject.addProperty("blocksMotion", this.blocksMotion);
            }
            if (defaultValues == null || this.waterlogged != defaultValues.waterlogged) {
                jsonObject.addProperty("waterlogged", this.waterlogged);
            }
            if (defaultValues == null || this.isPathfindableLand != defaultValues.isPathfindableLand) {
                jsonObject.addProperty("isPathfindableLand", this.isPathfindableLand);
            }
            if (defaultValues == null || this.isPathfindableAir != defaultValues.isPathfindableAir) {
                jsonObject.addProperty("isPathfindableAir", this.isPathfindableAir);
            }
            if (defaultValues == null || this.isPathfindableWater != defaultValues.isPathfindableWater) {
                jsonObject.addProperty("isPathfindableWater", this.isPathfindableWater);
            }
            if (defaultValues == null || this.lightEmission != defaultValues.lightEmission) {
                jsonObject.addProperty("lightEmission", this.lightEmission);
            }
            if (defaultValues == null || this.fallDamageResetting != defaultValues.fallDamageResetting) {
                jsonObject.addProperty("fallDamageResetting", this.fallDamageResetting);
            }
            if (defaultValues == null || !this.collisionShape.equals(defaultValues.collisionShape)) {
                JsonArray collisionShape = new JsonArray();
                for (AABB aabb : this.collisionShape) {
                    JsonArray aabbArray = new JsonArray();
                    aabbArray.add(aabb.minX);
                    aabbArray.add(aabb.minY);
                    aabbArray.add(aabb.minZ);
                    aabbArray.add(aabb.maxX);
                    aabbArray.add(aabb.maxY);
                    aabbArray.add(aabb.maxZ);
                    collisionShape.add(aabbArray);
                }
                jsonObject.add("collisionShape", collisionShape);
            }
            return jsonObject;
        }
    }

}
