package com.iortatechnxt.finverse.common.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Stable JSON envelope for paged results (decouples the API from Spring Data internals).
 *
 * @param content page content
 * @param page zero-based page index
 * @param size page size
 * @param totalElements total number of elements
 * @param totalPages total number of pages
 * @param <T> element type
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /**
   * Converts a Spring Data page, mapping each element.
   *
   * @param source page of entities
   * @param mapper entity to DTO mapper
   * @param <E> entity type
   * @param <T> DTO type
   * @return page response
   */
  public static <E, T> PageResponse<T> of(Page<E> source, Function<E, T> mapper) {
    return new PageResponse<>(
        source.getContent().stream().map(mapper).toList(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages());
  }
}
