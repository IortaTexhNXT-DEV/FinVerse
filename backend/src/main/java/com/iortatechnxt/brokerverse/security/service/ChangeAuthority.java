package com.iortatechnxt.brokerverse.security.service;

/**
 * The authority of an access change, written to the access change log (BRD 4.003.1): the approved
 * request that asked for it and its approver, or nothing for a direct change on the Users / Roles
 * screens.
 *
 * @param requestNo access request number, null for a direct change
 * @param approvedBy approver of the request, null for a direct change
 */
public record ChangeAuthority(String requestNo, String approvedBy) {

  /** A direct change: no request. */
  public static final ChangeAuthority DIRECT = new ChangeAuthority(null, null);

  /**
   * An approved access request.
   *
   * @param requestNo request number
   * @param approvedBy approver
   * @return authority
   */
  public static ChangeAuthority request(String requestNo, String approvedBy) {
    return new ChangeAuthority(requestNo, approvedBy);
  }

  /**
   * Whether the change was made without a request.
   *
   * @return true for a direct change
   */
  public boolean isDirect() {
    return requestNo == null;
  }
}
