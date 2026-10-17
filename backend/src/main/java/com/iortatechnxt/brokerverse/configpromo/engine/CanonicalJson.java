package com.iortatechnxt.brokerverse.configpromo.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;

/**
 * The one JSON form of a package: map entries sorted by key, no indentation, UTF-8. The same value
 * always gives the same bytes, so checksums compare between environments.
 */
public final class CanonicalJson {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
          .configure(SerializationFeature.INDENT_OUTPUT, false);

  private CanonicalJson() {}

  /**
   * The canonical JSON text of a value.
   *
   * @param value maps, lists, strings, booleans
   * @return JSON text
   */
  public static String text(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Value cannot be written as JSON", e);
    }
  }

  /**
   * The canonical JSON bytes of a value.
   *
   * @param value value
   * @return UTF-8 bytes
   */
  public static byte[] bytes(Object value) {
    return text(value).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Reads JSON.
   *
   * @param bytes UTF-8 JSON
   * @param type target type
   * @param <T> type
   * @return value
   */
  public static <T> T read(byte[] bytes, TypeReference<T> type) {
    try {
      return MAPPER.readValue(bytes, type);
    } catch (java.io.IOException e) {
      throw new PackageException("The package holds a file that is not valid JSON", e);
    }
  }

  /**
   * Reads JSON into a record or class.
   *
   * @param bytes UTF-8 JSON
   * @param type target class
   * @param <T> type
   * @return value
   */
  public static <T> T read(byte[] bytes, Class<T> type) {
    try {
      return MAPPER.readValue(bytes, type);
    } catch (java.io.IOException e) {
      throw new PackageException("The package holds a file that is not valid JSON", e);
    }
  }
}
