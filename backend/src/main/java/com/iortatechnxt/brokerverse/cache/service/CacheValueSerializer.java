package com.iortatechnxt.brokerverse.cache.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.ClassUtils;

/**
 * JSON serializer of cached values on Redis: {@code {"t": type, "v": value}}, and for lists and
 * sets {@code {"t": "list"|"set", "e": element type, "v": [...]}}.
 *
 * <p>Only application types ({@code com.iortatechnxt.brokerverse.*}) and a short allow-list of JDK
 * value types are ever instantiated, so a tampered cache entry cannot load arbitrary classes (no
 * Java deserialization, no polymorphic Jackson typing). Decimals keep their scale (money). Unknown
 * properties are ignored, so an entry written by the previous release still reads after a
 * deployment that adds a record component.
 */
public class CacheValueSerializer implements RedisSerializer<Object> {

  private static final String APPLICATION_PACKAGE = "com.iortatechnxt.brokerverse.";
  private static final Set<String> JDK_TYPES =
      Set.of(
          String.class.getName(),
          Integer.class.getName(),
          Long.class.getName(),
          Boolean.class.getName(),
          BigDecimal.class.getName(),
          LocalDate.class.getName());
  private static final String TYPE = "t";
  private static final String ELEMENT = "e";
  private static final String VALUE = "v";
  private static final String LIST = "list";
  private static final String SET = "set";

  private final ObjectMapper mapper;

  /**
   * Creates the serializer.
   *
   * @param mapper application object mapper (copied; Java time module expected)
   */
  public CacheValueSerializer(ObjectMapper mapper) {
    this.mapper =
        mapper
            .copy()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);
  }

  @Override
  public byte[] serialize(Object value) {
    if (value == null) {
      return new byte[0];
    }
    ObjectNode node = mapper.createObjectNode();
    if (value instanceof Collection<?> items) {
      node.put(TYPE, value instanceof Set<?> ? SET : LIST);
      node.put(ELEMENT, elementType(items));
    } else {
      node.put(TYPE, value.getClass().getName());
    }
    node.set(VALUE, mapper.valueToTree(value));
    try {
      return mapper.writeValueAsBytes(node);
    } catch (IOException ex) {
      throw new SerializationException("Cannot write cache value " + value.getClass(), ex);
    }
  }

  @Override
  public Object deserialize(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    try {
      JsonNode node = mapper.readTree(bytes);
      String type = node.path(TYPE).asText();
      JsonNode value = node.path(VALUE);
      if (LIST.equals(type) || SET.equals(type)) {
        return collection(type, node.path(ELEMENT).asText(), value);
      }
      return mapper.treeToValue(value, allowed(type));
    } catch (IOException | IllegalArgumentException ex) {
      throw new SerializationException("Cannot read cache value", ex);
    }
  }

  private Object collection(String type, String elementType, JsonNode value) throws IOException {
    List<Object> items = new ArrayList<>();
    if (value instanceof ArrayNode array && !array.isEmpty()) {
      Class<?> element = allowed(elementType);
      for (JsonNode item : array) {
        items.add(mapper.treeToValue(item, element));
      }
    }
    return SET.equals(type)
        ? Collections.unmodifiableSet(new LinkedHashSet<>(items))
        : Collections.unmodifiableList(items);
  }

  private static String elementType(Collection<?> items) {
    return items.stream()
        .filter(Objects::nonNull)
        .findFirst()
        .map(i -> i.getClass().getName())
        .orElse(String.class.getName());
  }

  private static Class<?> allowed(String type) {
    if (!type.startsWith(APPLICATION_PACKAGE) && !JDK_TYPES.contains(type)) {
      throw new IllegalArgumentException("Type not allowed in the cache: " + type);
    }
    return ClassUtils.resolveClassName(type, ClassUtils.getDefaultClassLoader());
  }
}
