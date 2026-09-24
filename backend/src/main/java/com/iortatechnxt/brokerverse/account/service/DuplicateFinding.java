package com.iortatechnxt.brokerverse.account.service;

/**
 * An existing live account that insures the same risk (BRNB.032/051/066).
 *
 * @param itemNo item of the checked account (1-based)
 * @param field matching identifier: plate, conduction sticker, engine, chassis or location
 * @param value matching value
 * @param existingArn ARN of the existing account
 * @param existingProduct product of the existing account
 */
public record DuplicateFinding(
    int itemNo, String field, String value, String existingArn, String existingProduct) {

  /**
   * One-line explanation.
   *
   * @return message naming the existing ARN
   */
  public String message() {
    return "Item "
        + itemNo
        + ": "
        + field
        + " "
        + value
        + " is already insured under "
        + existingArn
        + " ("
        + existingProduct
        + ")";
  }
}
