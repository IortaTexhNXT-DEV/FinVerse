package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessApprovers.ApproverOption;

/**
 * An eligible approver in the drop-down (FR-UA-015).
 *
 * @param username user name
 * @param fullName full name
 */
public record ApproverResponse(String username, String fullName) {

  /**
   * Maps an option.
   *
   * @param o option
   * @return response
   */
  public static ApproverResponse from(ApproverOption o) {
    return new ApproverResponse(o.username(), o.fullName());
  }
}
