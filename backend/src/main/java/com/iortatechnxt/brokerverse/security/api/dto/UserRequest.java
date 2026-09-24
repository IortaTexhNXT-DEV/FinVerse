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
 * Create / update user request. {@code username} is ignored on update.
 *
 * @param username login name
 * @param fullName full name
 * @param email email
 * @param homeBranchId home branch
 * @param authorizationLimit authorization limit (null = unlimited)
 * @param roleCodes roles
 * @param enabled enabled flag
 */
public record UserRequest(
    @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
    @NotBlank @Size(max = 120) String fullName,
    @Email @Size(max = 120) String email,
    Long homeBranchId,
    @DecimalMin("0") BigDecimal authorizationLimit,
    @NotEmpty Set<String> roleCodes,
    boolean enabled) {}
