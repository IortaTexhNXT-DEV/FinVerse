package com.iortatechnxt.brokerverse.productmaint.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.CoverageTerm;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Stores package terms and insurer coverage terms as JSON (requested and proposed terms of a
 * request, terms of an insurer response, compiled comparative tables).
 */
@Component
public class TermsCodec {

  private static final TypeReference<List<CoverageTerm>> COVERAGES = new TypeReference<>() {};

  private final ObjectMapper mapper;

  /**
   * Creates the codec.
   *
   * @param mapper JSON mapper
   */
  public TermsCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Terms as JSON.
   *
   * @param terms terms
   * @return JSON
   */
  public String toJson(PackageTerms terms) {
    return write(terms == null ? PackageTerms.EMPTY : terms);
  }

  /**
   * Terms from JSON.
   *
   * @param json JSON, may be null
   * @return terms, empty when null
   */
  public PackageTerms terms(String json) {
    return json == null || json.isBlank() ? PackageTerms.EMPTY : read(json, PackageTerms.class);
  }

  /**
   * Coverage terms as JSON.
   *
   * @param terms coverage terms
   * @return JSON
   */
  public String coveragesJson(List<CoverageTerm> terms) {
    return write(terms == null ? List.of() : terms);
  }

  /**
   * Coverage terms from JSON.
   *
   * @param json JSON, may be null
   * @return coverage terms
   */
  public List<CoverageTerm> coverages(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return mapper.readValue(json, COVERAGES);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Package coverage terms cannot be read", e);
    }
  }

  /**
   * Any value as JSON.
   *
   * @param value value
   * @return JSON
   */
  public String write(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Package data cannot be written", e);
    }
  }

  /**
   * A value from JSON.
   *
   * @param json JSON
   * @param type type
   * @param <T> type
   * @return value
   */
  public <T> T read(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Package data cannot be read", e);
    }
  }
}
