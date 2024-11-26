/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.anthropic.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.anthropic.AnthropicTestConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(classes = AnthropicTestConfiguration.class, properties = "spring.ai.retry.on-http-codes=429")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@ActiveProfiles("logging-test")
class AnthropicChatClientMethodInvokingFunctionCallbackIT {

	private static final Logger logger = LoggerFactory
		.getLogger(AnthropicChatClientMethodInvokingFunctionCallbackIT.class);

	public static Map<String, Object> arguments = new ConcurrentHashMap<>();

	@BeforeEach
	void beforeEach() {
		arguments.clear();
	}

	@Test
	void methodGetWeatherGeneratedDescription() {

		// @formatter:off
		String response = ChatClient.create(this.chatModel).prompt()
				.user("What's the weather like in San Francisco, Tokyo, and Paris?  Use Celsius.")
				.functions(FunctionCallback.builder()
					.method("getWeatherInLocation", String.class, Unit.class)
					.targetClass(TestFunctionClass.class)
					.build())
				.call()
				.content();
		// @formatter:on

		logger.info("Response: {}", response);

		assertThat(response).contains("30", "10", "15");
	}

	@Test
	void methodGetWeatherStatic() {

		// @formatter:off
		String response = ChatClient.create(this.chatModel).prompt()
				.user("What's the weather like in San Francisco, Tokyo, and Paris?  Use Celsius.")
				.functions(FunctionCallback.builder()
					.description("Get the weather in location")
					.method("getWeatherStatic", String.class, Unit.class)
					.targetClass(TestFunctionClass.class)
					.build())
				.call()
				.content();
		// @formatter:on

		logger.info("Response: {}", response);

		assertThat(response).contains("30", "10", "15");
	}

	@Test
	void methodTurnLightNoResponse() {

		TestFunctionClass targetObject = new TestFunctionClass();

		// @formatter:off
		String response = ChatClient.create(this.chatModel).prompt()
				.user("Turn light on in the living room.")
				.functions(FunctionCallback.builder()
					.description("Turn light on in the living room.")
					.method("turnLight", String.class, boolean.class)
					.targetObject(targetObject)
					.build())
				.call()
				.content();
		// @formatter:on

		logger.info("Response: {}", response);

		assertThat(arguments).containsEntry("roomName", "living room");
		assertThat(arguments).containsEntry("on", true);
	}

	@Test
	void methodGetWeatherNonStatic() {

		TestFunctionClass targetObject = new TestFunctionClass();

		// @formatter:off
		String response = ChatClient.create(this.chatModel).prompt()
				.user("What's the weather like in San Francisco, Tokyo, and Paris?  Use Celsius.")
				.functions(FunctionCallback.builder()
					.description("Get the weather in location")
					.method("getWeatherNonStatic", String.class, Unit.class)
					.targetObject(targetObject)
					.build())
				.call()
				.content();
		// @formatter:on

		logger.info("Response: {}", response);

		assertThat(response).contains("30", "10", "15");
	}

	@Test
	void methodGetWeatherToolContext() {

		TestFunctionClass targetObject = new TestFunctionClass();

		// @formatter:off
		String response = ChatClient.create(this.chatModel).prompt()
				.user("What's the weather like in San Francisco, Tokyo, and Paris?  Use Celsius.")
				.functions(FunctionCallback.builder()
					.description("Get the weather in location")
					.method("getWeatherWithContext", String.class, Unit.class, ToolContext.class)
					.targetObject(targetObject)
					.build())
				.toolContext(Map.of("tool", "value"))
				.call()
				.content();
		// @formatter:on

		logger.info("Response: {}", response);

		assertThat(response).contains("30", "10", "15");
		assertThat(arguments).containsEntry("tool", "value");
		assertThat(arguments).containsKey(ToolContext.TOOL_CALL_HISTORY);
		List<Message> tootConversationMessages = (List<Message>) arguments.get(ToolContext.TOOL_CALL_HISTORY);
		assertThat(tootConversationMessages.size() == 6 || tootConversationMessages.size() == 2).isTrue();
	}

	@Test
	void methodGetWeatherToolContextButNonContextMethod() {

		TestFunctionClass targetObject = new TestFunctionClass();

		// @formatter:off
		assertThatThrownBy(() -> ChatClient.create(this.chatModel).prompt()
				.user("What's the weather like in San Francisco, Tokyo, and Paris?  Use Celsius.")
				.functions(FunctionCallback.builder()
					.description("Get the weather in location")
					.method("getWeatherNonStatic", String.class, Unit.class)
					.targetObject(targetObject)
					.build())
				.toolContext(Map.of("tool", "value"))
				.call()
				.content())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Configured method does not accept ToolContext as input parameter!");
		// @formatter:on
	}

	@Test
	void methodNoParameters() {

		TestFunctionClass targetObject = new TestFunctionClass();

		// @formatter:off
		String response = ChatClient.create(this.chatModel).prompt()
				.user("Turn light on in the living room.")
				.functions(FunctionCallback.builder()
					.description("Can turn lights on in the Living Room")
					.method("turnLivingRoomLightOn")
					.targetObject(targetObject)
					.build())
				.call()
				.content();
		// @formatter:on

		logger.info("Response: {}", response);

		assertThat(arguments).containsEntry("turnLivingRoomLightOn", true);
	}

	@Autowired
	ChatModel chatModel;

	record MyRecord(String foo, String bar) {
	}

	public enum Unit {

		CELSIUS, FAHRENHEIT

	}

	public static class TestFunctionClass {

		public static void argumentLessReturnVoid() {
			arguments.put("method called", "argumentLessReturnVoid");
		}

		public static String getWeatherInLocation(String city, Unit unit) {
			return getWeatherStatic(city, unit);
		}

		public static String getWeatherStatic(String city, Unit unit) {

			logger.info("City: " + city + " Unit: " + unit);

			arguments.put("city", city);
			arguments.put("unit", unit);

			double temperature = 0;
			if (city.contains("Paris")) {
				temperature = 15;
			}
			else if (city.contains("Tokyo")) {
				temperature = 10;
			}
			else if (city.contains("San Francisco")) {
				temperature = 30;
			}

			return "temperature: " + temperature + " unit: " + unit;
		}

		public String getWeatherNonStatic(String city, Unit unit) {
			return getWeatherStatic(city, unit);
		}

		public String getWeatherWithContext(String city, Unit unit, ToolContext context) {
			arguments.put("tool", context.getContext().get("tool"));
			arguments.put(ToolContext.TOOL_CALL_HISTORY, context.getToolCallHistory());
			return getWeatherStatic(city, unit);
		}

		public void turnLight(String roomName, boolean on) {
			arguments.put("roomName", roomName);
			arguments.put("on", on);
			logger.info("Turn light in room: {} to: {}", roomName, on);
		}

		public void turnLivingRoomLightOn() {
			arguments.put("turnLivingRoomLightOn", true);
		}

	}

}