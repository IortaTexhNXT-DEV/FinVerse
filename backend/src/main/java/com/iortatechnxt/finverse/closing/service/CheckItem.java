package com.iortatechnxt.finverse.closing.service;

/**
 * One checklist control with its automatic result.
 *
 * @param code stable code
 * @param label description
 * @param passed whether the control passed
 * @param detail explanation of the result
 * @param blocking whether a failure blocks the close
 */
public record CheckItem(
    String code, String label, boolean passed, String detail, boolean blocking) {

  /**
   * Blocking control.
   *
   * @param code code
   * @param label label
   * @param passed result
   * @param detail detail
   * @return item
   */
  public static CheckItem of(String code, String label, boolean passed, String detail) {
    return new CheckItem(code, label, passed, detail, true);
  }

  /**
   * Non-blocking control: a failure is shown as a warning to review, the close may proceed.
   *
   * @param code code
   * @param label label
   * @param passed result
   * @param detail detail
   * @return item
   */
  public static CheckItem warning(String code, String label, boolean passed, String detail) {
    return new CheckItem(code, label, passed, detail, false);
  }

  /**
   * Whether this item prevents closing.
   *
   * @return true when blocking and failed
   */
  public boolean blocks() {
    return blocking && !passed;
  }
}
