/*
 * MyFTBLauncher
 * Copyright (C) 2019 MyFTB <https://myftb.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.myftb.launcher.models.minecraft;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Arguments {
    private List<Argument> game;
    private List<Argument> jvm;

    public List<Argument> getGameArguments() {
        return this.game;
    }

    public List<Argument> getJvmArguments() {
        return this.jvm;
    }

    public static List<String> getFromArguments(List<Argument> arguments) {
        return arguments.stream()
                .filter(argument -> !(argument instanceof Arguments.FilteredArgument)
                        || Rule.isValid(((Arguments.FilteredArgument) argument).getRules()))
                .flatMap(argument -> argument.getValue().stream())
                .collect(Collectors.toList());
    }

    public static class Argument {
        protected List<String> value;

        public List<String> getValue() {
            return this.value;
        }
    }

    public static class FilteredArgument extends Argument {
        private List<Rule> rules;

        public List<Rule> getRules() {
            return this.rules;
        }
    }

    public static class ArgumentsDeserializer extends StdDeserializer<Arguments> {
        public static final long serialVersionUID = -601843912938576796L;

        public ArgumentsDeserializer() {
            super(Arguments.class);
        }

        private List<Argument> parseArguments(ObjectCodec codec, ArrayNode array) throws IOException {
            List<Argument> arguments = new ArrayList<>();

            for (int i = 0; i < array.size(); i++) {
                JsonNode node = array.get(i);

                Argument argument;
                if (node instanceof TextNode) {
                    argument = new Argument();
                    argument.value = Collections.singletonList(node.textValue());
                } else if (node instanceof ObjectNode) {
                    argument = new FilteredArgument();
                    JsonNode valueNode = node.get("value");
                    if (valueNode instanceof TextNode) {
                        argument.value = Collections.singletonList(valueNode.textValue());
                    } else if (valueNode instanceof ArrayNode) {
                        Iterable<JsonNode> iterable = valueNode::elements;
                        argument.value = StreamSupport.stream(iterable.spliterator(), false)
                                .map(JsonNode::textValue)
                                .collect(Collectors.toList());
                    }

                    ((FilteredArgument) argument).rules = codec.readValue(node.get("rules").traverse(),
                            TypeFactory.defaultInstance().constructCollectionLikeType(List.class, Rule.class));
                } else {
                    continue;
                }

                arguments.add(argument);
            }

            return arguments;
        }

        @Override
        public Arguments deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            Arguments arguments = new Arguments();

            if (node instanceof TextNode) {
                arguments.game = Arrays.stream(node.textValue().split(" "))
                        .map(Collections::singletonList)
                        .map(argList -> {
                            Argument argument = new Argument();
                            argument.value = argList;
                            return argument;
                        })
                        .collect(Collectors.toList());

                return arguments;
            } else if (node instanceof ObjectNode) {
                if (node.get("game") == null || node.get("game").isNull()) {
                    arguments.game = Collections.emptyList();
                } else {
                    ArrayNode gameArgs = (ArrayNode) node.get("game");
                    arguments.game = this.parseArguments(parser.getCodec(), gameArgs);
                }

                if (node.get("jvm") == null || node.get("jvm").isNull()) {
                    arguments.jvm = Collections.emptyList();
                } else {
                    ArrayNode jvmArgs = (ArrayNode) node.get("jvm");
                    arguments.jvm = this.parseArguments(parser.getCodec(), jvmArgs);
                }

                return arguments;
            } else {
                throw new IOException("Invalid JSON type: " + node.getClass().getCanonicalName());
            }
        }
    }

    public static class ArgumentSerializer extends StdSerializer<Argument> {
        public static final long serialVersionUID = -583983241948231447L;

        public ArgumentSerializer() {
            super(Argument.class);
        }

        @Override
        public void serialize(Argument value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value instanceof FilteredArgument) {
                FilteredArgument filteredArgument = (FilteredArgument) value;
                if (filteredArgument.value.isEmpty()) {
                    return;
                }

                gen.writeStartObject();
                if (filteredArgument.value.size() > 1) {
                    gen.writeArrayFieldStart("value");
                    for (String val : filteredArgument.value) {
                        gen.writeString(val);
                    }
                    gen.writeEndArray();
                } else {
                    gen.writeStringField("value", filteredArgument.value.get(0));
                }

                gen.writeArrayFieldStart("rules");
                for (Rule rule : filteredArgument.rules) {
                    gen.writeObject(rule);
                }
                gen.writeEndArray();

                gen.writeEndObject();
            } else {
                if (!value.value.isEmpty()) {
                    gen.writeString(value.value.get(0));
                }
            }
        }
    }

}