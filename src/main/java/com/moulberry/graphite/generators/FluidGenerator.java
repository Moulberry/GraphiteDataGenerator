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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidGenerator implements DataGenerator {
    @Override
    public String name() {
        return "fluids";
    }

    @Override
    public JsonObject run() {
        JsonObject allFluidsJson = new JsonObject();

        FluidAttributes defaultAttributes = FluidAttributes.createDefault();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            FluidState defaultFluidState = fluid.defaultFluidState();
            JsonObject fluidJsonObject = new JsonObject();

            // Attributes
            FluidAttributes fluidAttributes = FluidAttributes.fromState(defaultFluidState);
            JsonObject fluidAttributesJson = fluidAttributes.write(defaultAttributes);

            fluidJsonObject.add("attributes", fluidAttributesJson);

            JsonObject allStateAttributesJson = new JsonObject();

            int minStateId = Integer.MAX_VALUE;
            for (FluidState possibleState : fluid.getStateDefinition().getPossibleStates()) {
                int stateId = Fluid.FLUID_STATE_REGISTRY.getId(possibleState);
                minStateId = Math.min(minStateId, stateId);

                System.out.println("got state: " + stateId + ": " + possibleState.getOwnHeight());

                FluidAttributes stateAttributes = FluidAttributes.fromState(possibleState);
                JsonObject stateAttributesJson = stateAttributes.write(fluidAttributes);

                if (!stateAttributesJson.isEmpty()) {
                    allStateAttributesJson.add(stateId+"", stateAttributesJson);
                }
            }

            fluidJsonObject.addProperty("minStateId", minStateId);
            fluidJsonObject.add("stateAttributes", allStateAttributesJson);

            // Properties
            JsonObject propertiesJson = new JsonObject();
            for (Property property : fluid.getStateDefinition().getProperties()) {
                JsonObject propertyJson = new JsonObject();
                if (property instanceof BooleanProperty booleanProperty) {
                    propertyJson.addProperty("type", "bool");
                    boolean defaultValue = defaultFluidState.getValue(booleanProperty);
                    propertyJson.addProperty("defaultValue", defaultValue);
                } else if (property instanceof IntegerProperty integerProperty) {
                    propertyJson.addProperty("type", "int");
                    int defaultValue = defaultFluidState.getValue(integerProperty);
                    propertyJson.addProperty("defaultValue", defaultValue);

                    JsonArray values = new JsonArray();
                    for (int possibleValue : integerProperty.getPossibleValues()) {
                        values.add(possibleValue);
                    }
                    propertyJson.add("values", values);
                } else {
                    propertyJson.addProperty("type", "string");
                    String defaultValue = property.getName(defaultFluidState.getValue(property));
                    propertyJson.addProperty("defaultValue", defaultValue);

                    JsonArray values = new JsonArray();
                    for (Object possibleValue : property.getPossibleValues()) {
                        values.add(property.getName((Comparable) possibleValue));
                    }
                    propertyJson.add("values", values);
                }
                propertiesJson.add(property.getName(), propertyJson);
            }
            fluidJsonObject.add("properties", propertiesJson);

            allFluidsJson.add(DataGenerator.simplifyResourceLocation(BuiltInRegistries.FLUID.getKey(fluid)), fluidJsonObject);
        }
        return allFluidsJson;
    }

    private record FluidAttributes(
        double ownHeight,
        int amount,
        boolean isSource
    ) {
        public static FluidAttributes createDefault() {
            return new FluidAttributes(
                0.0,
                0,
                false
            );
        }

        public static FluidAttributes fromState(FluidState fluidState) {
            return new FluidAttributes(
                fluidState.getOwnHeight(),
                fluidState.getAmount(),
                fluidState.isSource()
            );
        }

        public JsonObject write(@Nullable FluidAttributes defaultValues) {
            JsonObject jsonObject = new JsonObject();
            if (defaultValues == null || this.ownHeight != defaultValues.ownHeight) {
                jsonObject.addProperty("ownHeight", this.ownHeight);
            }
            if (defaultValues == null || this.amount != defaultValues.amount) {
                jsonObject.addProperty("amount", this.amount);
            }
            if (defaultValues == null || this.isSource != defaultValues.isSource) {
                jsonObject.addProperty("isSource", this.isSource);
            }
            return jsonObject;
        }
    }

}
