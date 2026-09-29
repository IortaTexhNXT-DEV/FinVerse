package com.iortatechnxt.brokerverse.common.exception;

/**
 * Raised when a request reaches a product module that is switched off in this deployment. Mapped to
 * HTTP 404 with the code {@code MODULE_NOT_IN_USE}.
 */
public class ModuleNotInUseException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param moduleName name of the module shown to users
   */
  public ModuleNotInUseException(String moduleName) {
    super("The " + moduleName + " module is not in use in this deployment");
  }
}
