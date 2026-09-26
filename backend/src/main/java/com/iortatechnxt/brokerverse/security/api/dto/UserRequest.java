package com.iortatechnxt.brokerverse.security.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Create / update user request. {@code username} is ignored on update. The BRD-11 attributes
 * (Windows ID, business unit group, user level; BRD 1.002.1.1.1, UAM-NFR-15) are optional: null
 * keeps the current value on update, a blank value clears it.
 *
 * @param username login name
 * @param fullName full name
 * @param email email
 * @param homeBranchId home branch
 * @param authorizationLimit authorization limit (null = unlimited)
 * @param roleCodes roles
 * @param enabled enabled flag
 * @param windowsId Windows ID used for directory sign-in (unique)
 * @param businessUnitCode business unit group (list of values UAM_BUSINESS_UNIT)
 * @param userLevel user level (list of values UAM_USER_LEVEL)
 */
public record UserRequest(
    @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
    @NotBlank @Size(max = 120) String fullName,
    @Email @Size(max = 120) String email,
    Long homeBranchId,
    @DecimalMin("0") BigDecimal authorizationLimit,
    @NotEmpty Set<String> roleCodes,
    boolean enabled,
    @Size(max = 50) @Pattern(regexp = "[a-zA-Z0-9.@\\\\_-]*") String windowsId,
    @Size(max = 40) String businessUnitCode,
    @Size(max = 40) String userLevel) {

  /**
   * The request without the BRD-11 attributes (they keep their current values on update).
   *
   * @param username login name
   * @param fullName full name
   * @param email email
   * @param homeBranchId home branch
   * @param authorizationLimit authorization limit (null = unlimited)
   * @param roleCodes roles
   * @param enabled enabled flag
   */
  public UserRequest(
      String username,
      String fullName,
      String email,
      Long homeBranchId,
      BigDecimal authorizationLimit,
      Set<String> roleCodes,
      boolean enabled) {
    this(
        username,
        fullName,
        email,
        homeBranchId,
        authorizationLimit,
        roleCodes,
        enabled,
        null,
        null,
        null);
  }
}
