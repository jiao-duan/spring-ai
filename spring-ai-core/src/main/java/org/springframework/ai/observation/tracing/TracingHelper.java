/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.observation.tracing;

import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.model.Content;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utilities to prepare and process traces for observability.
 *
 * @author Thomas Vitale
 */
public final class TracingHelper {

	private static final Logger logger = LoggerFactory.getLogger(TracingHelper.class);

	@Nullable
	public static Span extractOtelSpan(@Nullable TracingObservationHandler.TracingContext tracingContext) {
		if (tracingContext == null) {
			return null;
		}

		io.micrometer.tracing.Span micrometerSpan = tracingContext.getSpan();
		try {
			Method toOtelMethod = tracingContext.getSpan()
				.getClass()
				.getDeclaredMethod("toOtel", io.micrometer.tracing.Span.class);
			toOtelMethod.setAccessible(true);
			Object otelSpanObject = toOtelMethod.invoke(null, micrometerSpan);
			if (otelSpanObject instanceof Span otelSpan) {
				return otelSpan;
			}
		}
		catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
			logger.warn("It wasn't possible to extract the OpenTelemetry Span object from Micrometer", ex);
			return null;
		}

		return null;
	}

	public static String concatenateStrings(List<String> strings) {
		var promptMessagesJoiner = new StringJoiner(", ", "[", "]");
		strings.forEach(string -> promptMessagesJoiner.add("\"" + string + "\""));
		return promptMessagesJoiner.toString();
	}

}