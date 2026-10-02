package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionTerms;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changed retention rule.
 *
 * @param statuses comma separated status codes
 * @param yearsOnline years online (1-50)
 * @param yearsArchive years in archive (0-50)
 * @param action REVIEW or ARCHIVE
 * @param active active flag
 * @param description description
 */
public record RetentionRuleRequest(
    @NotBlank @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9_, ]+") String statuses,
    @Min(1) @Max(50) int yearsOnline,
    @Min(0) @Max(50) int yearsArchive,
    @NotNull RetentionAction action,
    boolean active,
    @NotBlank @Size(max = 300) String description) {

  /**
   * Rule terms.
   *
   * @return terms
   */
  public RetentionTerms terms() {
    return new RetentionTerms(
        statuses, yearsOnline, yearsArchive, action, active, description.trim());
  }
}
