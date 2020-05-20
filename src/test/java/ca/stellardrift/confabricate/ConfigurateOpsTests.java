/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.confabricate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import ninja.leaping.configurate.ConfigurationNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests to validate ConfigurateOps functions properly.
 */
public class ConfigurateOpsTests {
    /**
     * We cannot use Minecraft's Type Serializers in JUnit testing due to loom not liking JUnit.
     */
    private static final DynamicOps<ConfigurationNode> CONFIGURATE_OPS = ConfigurateOps.getWithNodeFactory(ConfigurationNode::root);

    @Test
    @DisplayName("Configurate (Empty) -> Gson (Null)")
    public void emptyToGson() {
        final ConfigurationNode node = ConfigurationNode.root();
        final Dynamic<ConfigurationNode> wrapped = new Dynamic<>(CONFIGURATE_OPS, node);
        final JsonElement element = wrapped.convert(JsonOps.INSTANCE).getValue();

        assertTrue(element.isJsonNull(), "Resulting element was not a json null");
    }

    @Test
    @DisplayName("Gson (Null) -> Configurate (Empty)")
    public void emptyFromGson() {
        final JsonNull jsonNull = JsonNull.INSTANCE;
        final Dynamic<JsonElement> wrapped = new Dynamic<>(JsonOps.INSTANCE, jsonNull);
        final ConfigurationNode result = wrapped.convert(CONFIGURATE_OPS).getValue();

        assertTrue(result.isEmpty(), "Resulting configuration node was not empty");
    }

    @Test
    @DisplayName("Configurate (String) -> Gson")
    public void toGsonFromString() {
        final ConfigurationNode node = ConfigurationNode.root().setValue("Test String");
        final Dynamic<ConfigurationNode> wrapped = new Dynamic<>(CONFIGURATE_OPS, node);
        final JsonElement output = wrapped.convert(JsonOps.INSTANCE).getValue();

        assertTrue(output instanceof JsonPrimitive, "Resulting Element was not a Json Primitive");
        assertTrue(output.getAsJsonPrimitive().isString(), "Resulting Element was not a String");
        assertEquals("Test String", output.getAsString());
    }

    @Test
    @DisplayName("Gson (String) -> Configurate")
    public void fromGsonFromString() {
        final JsonPrimitive string = new JsonPrimitive("Test String");
        final Dynamic<JsonElement> wrapped = new Dynamic<>(JsonOps.INSTANCE, string);
        final ConfigurationNode output = wrapped.convert(CONFIGURATE_OPS).getValue();

        assertTrue(output.getValue() instanceof String, "Resulting configuration node was not a String");
        assertEquals(output.getString(), "Test String");
    }

    @Test
    @DisplayName("Gson (Integer Array) -> Configurate")
    public void fromGsonFromList() {
        final List<Integer> expectedElements = new ArrayList<>();
        expectedElements.add(1);
        expectedElements.add(3);
        expectedElements.add(4);

        final JsonArray jsonArray = new JsonArray();
        jsonArray.add(1);
        jsonArray.add(3);
        jsonArray.add(4);
        final Dynamic<JsonElement> wrapped = new Dynamic<>(JsonOps.INSTANCE, jsonArray);
        final ConfigurationNode output = wrapped.convert(CONFIGURATE_OPS).getValue();

        assertTrue(output.isList(), "Resulting configuration node was not a list.");
        assertEquals(3, output.getChildrenList().size(), "Resulting configuration node had wrong amount of child elements in list");
        assertTrue(output.getChildrenList().stream().map(ConfigurationNode::getInt).collect(Collectors.toList()).containsAll(expectedElements), "Resulting configuration node did not contain every element in original JsonArray");
    }

    @Test
    @DisplayName("Configurate (Integer List) -> Gson")
    public void toGsonFromList() {
        final List<Integer> expectedElements = new ArrayList<>();
        expectedElements.add(1);
        expectedElements.add(3);
        expectedElements.add(4);

        final ConfigurationNode node = ConfigurationNode.root();
        node.setValue(expectedElements);
        //node.appendListNode().setValue(1).setValue(3).setValue(4);
        final Dynamic<ConfigurationNode> wrapped = new Dynamic<>(CONFIGURATE_OPS, node);
        final JsonElement output = wrapped.convert(JsonOps.INSTANCE).getValue();

        assertTrue(output.isJsonArray(), "Resulting Element was not an array");
        assertEquals(3, output.getAsJsonArray().size(), "Resulting array had the wrong amount of elements");

        final List<Integer> elements = StreamSupport.stream(output.getAsJsonArray().spliterator(), false).map(JsonElement::getAsInt).collect(Collectors.toList());
        assertTrue(elements.containsAll(expectedElements), "Resulting array did not contain all the same elements as the original configuration node");
    }

    @Test
    @DisplayName("Gson (JsonObject) -> Configurate")
    public void fromGsonToMap() {
        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("foo", "bar");
        expectedValues.put("bar", "baz");

        final JsonObject object = new JsonObject();
        object.addProperty("foo", "bar");
        object.addProperty("bar", "baz");
        final Dynamic<JsonElement> wrapped = new Dynamic<>(JsonOps.INSTANCE, object);
        final ConfigurationNode output = wrapped.convert(CONFIGURATE_OPS).getValue();

        assertTrue(output.isMap(), "Resulting configuration node was not a map");
        assertEquals(2, output.getChildrenMap().size(), "Resulting configuration node had wrong amount of child elements");
        // TODO: Verify the values in the resulting maps are equal
    }

    @Test
    @DisplayName("Configurate (Map) -> Gson")
    public void toGsonFromMap() {
        final Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("foo", "bar");
        expectedValues.put("bar", "baz");

        final ConfigurationNode node = ConfigurationNode.root();
        node.setValue(expectedValues);
        final Dynamic<ConfigurationNode> wrapped = new Dynamic<>(CONFIGURATE_OPS, node);
        final JsonElement element = wrapped.convert(JsonOps.INSTANCE).getValue();

        assertTrue(element.isJsonObject(), "Resulting element was not a json object");
        assertEquals(2, element.getAsJsonObject().size(), "Resulting json object had wrong amount of child elements");
        // TODO: Verify the values in the resulting maps are equal
    }
}
