package com.iortatechnxt.finverse.payables.api;

import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Defaults for optional list filters of the payables API. */
final class ApiDefaults {

  private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(2999, 12, 31);
  private static final int MAX_PAGE = 200;

  private ApiDefaults() {}

  static LocalDate from(LocalDate value) {
    return value == null ? EARLIEST : value;
  }

  static LocalDate to(LocalDate value) {
    return value == null ? LATEST : value;
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  static String reasonOrDefault(String reason, String fallback) {
    return reason == null || reason.isBlank() ? fallback : reason;
  }

  static Pageable page(int page, int size, String dateField) {
    return PageRequest.of(
        Math.max(page, 0),
        Math.clamp(size, 1, MAX_PAGE),
        Sort.by(Sort.Direction.DESC, dateField, "id"));
  }
}
