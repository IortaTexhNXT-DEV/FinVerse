package com.iortatechnxt.brokerverse.tax.domain;

/**
 * Identity of a taxpayer as printed on BIR forms and lists.
 *
 * @param tin 9-digit TIN (digits only)
 * @param branchCode TIN branch code (000 for the head office)
 * @param name registered name
 * @param address registered address
 * @param zipCode ZIP code
 */
public record Taxpayer(String tin, String branchCode, String name, String address, String zipCode) {

  private static final int TIN_DIGITS = 9;
  private static final String HEAD_OFFICE = "000";
  private static final int GROUP = 3;
  private static final int SECOND_GROUP_END = 6;

  /**
   * Parses a TIN as captured in the party or company master (e.g. "123-456-789-000"): the first
   * nine digits are the TIN, the remaining digits (if any) the branch code.
   *
   * @param raw TIN text, may be null
   * @param name registered name
   * @param address address
   * @param zipCode ZIP code
   * @return taxpayer; TIN "000000000" when unknown
   */
  public static Taxpayer parse(String raw, String name, String address, String zipCode) {
    String digits = raw == null ? "" : raw.replaceAll("\\D", "");
    if (digits.length() < TIN_DIGITS) {
      return new Taxpayer("0".repeat(TIN_DIGITS), HEAD_OFFICE, name, address, zipCode);
    }
    String branch = digits.substring(TIN_DIGITS);
    return new Taxpayer(
        digits.substring(0, TIN_DIGITS),
        branch.isEmpty() ? HEAD_OFFICE : branch,
        name,
        address,
        zipCode);
  }

  /**
   * TIN formatted as printed on forms: 123-456-789-000.
   *
   * @return formatted TIN
   */
  public String formattedTin() {
    return String.join(
        "-",
        tin.substring(0, GROUP),
        tin.substring(GROUP, SECOND_GROUP_END),
        tin.substring(SECOND_GROUP_END),
        branchCode);
  }
}
