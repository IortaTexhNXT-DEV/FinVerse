package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * New user access request (BRNB.085).
 *
 * @param type request type
 * @param username user to create or change
 * @param fullName full name (create)
 * @param email e-mail (create)
 * @param roleCodes roles (create, modify roles)
 * @param homeBranchId home branch (create)
 * @param justification business justification
 */
public record AccessRequestRequest(
    @NotNull AccessRequestType type,
    @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
    @Size(max = 120) String fullName,
    @Email @Size(max = 120) String email,
    Set<String> roleCodes,
    Long homeBranchId,
    @NotBlank @Size(max = 1000) String justification) {

  /**
   * Request content.
   *
   * @return content
   */
  public AccessRequestContent content() {
    return new AccessRequestContent(
        type, username, fullName, email, roleCodes, homeBranchId, justification);
  }
}
