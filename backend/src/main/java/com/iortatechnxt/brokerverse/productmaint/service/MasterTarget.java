package com.iortatechnxt.brokerverse.productmaint.service;

/**
 * Where the product master changes are delivered (BDOI FRS FRPM.029.01): a folder the receiving
 * systems read (connection details from BDOI IT at SIT) or, in SIT and UAT, the simulated receiving
 * system.
 */
public interface MasterTarget {

  /**
   * The target in words (monitoring screen).
   *
   * @return name
   */
  String name();

  /**
   * Whether the target can receive files (a folder is set, or the simulator is on).
   *
   * @return true when connected
   */
  boolean connected();

  /**
   * Delivers a file.
   *
   * @param fileName file name
   * @param content content
   * @param records detail records
   * @throws MasterTransferException when the delivery fails
   */
  void deliver(String fileName, String content, int records);

  /** A failed delivery. */
  class MasterTransferException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message why
     * @param cause cause
     */
    public MasterTransferException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
