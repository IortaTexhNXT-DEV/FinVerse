package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Every {@link BulkImportHandler} bean, by code, with the permission check. */
@Component
public class BulkHandlerRegistry {

  private final Map<String, BulkImportHandler> handlers;
  private final CurrentUser currentUser;

  /**
   * Creates the registry.
   *
   * @param handlers handler beans
   * @param currentUser current user
   */
  public BulkHandlerRegistry(List<BulkImportHandler> handlers, CurrentUser currentUser) {
    this.handlers =
        handlers.stream().collect(Collectors.toMap(BulkImportHandler::code, Function.identity()));
    this.currentUser = currentUser;
  }

  /**
   * Handlers the current user may use.
   *
   * @return handlers by title
   */
  public List<BulkImportHandler> available() {
    return handlers.values().stream()
        .filter(h -> currentUser.hasAuthority(h.permission()))
        .sorted(Comparator.comparing(BulkImportHandler::title))
        .toList();
  }

  /**
   * A handler the current user may use.
   *
   * @param code handler code
   * @return handler
   */
  public BulkImportHandler require(String code) {
    BulkImportHandler handler = handlers.get(code);
    if (handler == null) {
      throw new ResourceNotFoundException("Bulk upload type", code);
    }
    if (!currentUser.hasAuthority(handler.permission())) {
      throw new BusinessRuleException(
          "BULK_NOT_PERMITTED", "You are not allowed to use '" + handler.title() + "'");
    }
    return handler;
  }

  /**
   * A handler without permission check (commit of an existing job by its owner).
   *
   * @param code handler code
   * @return handler
   */
  BulkImportHandler get(String code) {
    BulkImportHandler handler = handlers.get(code);
    if (handler == null) {
      throw new ResourceNotFoundException("Bulk upload type", code);
    }
    return handler;
  }
}
