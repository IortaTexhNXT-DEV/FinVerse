package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Submission of a draft or resubmission of a corrected request (BRD 1.002.1.4, 1.006.1.5).
 *
 * @param approvers approvers in order (a user request has one)
 * @param remarks remarks; the correction remark is mandatory on a resubmission
 */
public record AccessSubmitRequest(
    List<@Size(max = 50) String> approvers, @Size(max = 1000) String remarks) {

  /** Absent approvers are none (the submission is refused with "Select the approver"). */
  public AccessSubmitRequest {
    approvers = approvers == null ? List.of() : List.copyOf(approvers);
  }
}
