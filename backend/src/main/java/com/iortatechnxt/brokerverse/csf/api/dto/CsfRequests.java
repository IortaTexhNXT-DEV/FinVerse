package com.iortatechnxt.brokerverse.csf.api.dto;

import com.iortatechnxt.brokerverse.csf.service.ContactChangeService.ChangeRequest;
import com.iortatechnxt.brokerverse.csf.service.ContactChangeService.ReferralRequest;
import com.iortatechnxt.brokerverse.csf.service.ResendService.ResendRequest;
import com.iortatechnxt.brokerverse.csf.service.VerificationService.CheckAnswer;
import com.iortatechnxt.brokerverse.csf.service.VerificationService.VerifyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request bodies of the Customer Servicing Facility API. */
public interface CsfRequests {

  /**
   * A caller verification (FR-CSF-020).
   *
   * @param channel channel (list CSF_CHANNEL)
   * @param checks checks with their outcome
   * @param remarks remarks
   */
  record Verify(
      @NotBlank String channel,
      @NotNull @Valid List<Check> checks,
      @Size(max = RequestLimits.REMARKS) String remarks) {

    /**
     * The service request.
     *
     * @return request
     */
    public VerifyRequest toRequest() {
      return new VerifyRequest(
          channel,
          checks.stream().map(c -> new CheckAnswer(c.code(), c.matched())).toList(),
          remarks);
    }
  }

  /**
   * One check.
   *
   * @param code check (list CSF_VERIFY_CHECK)
   * @param matched answer matched
   */
  record Check(@NotBlank String code, boolean matched) {}

  /**
   * A contact change (FR-CSF-021): a null value keeps the field, a blank value clears it.
   *
   * @param verificationId passed verification
   * @param reasonCode reason (list CSF_CHANGE_REASON)
   * @param remarks remarks
   * @param values new value by field (EMAIL, MOBILE, PHONE, ADDRESS_LINE, CITY, PROVINCE,
   *     POSTAL_CODE)
   */
  record Change(
      Long verificationId,
      String reasonCode,
      @Size(max = RequestLimits.REMARKS) String remarks,
      Map<String, @Size(max = RequestLimits.VALUE) String> values) {

    /**
     * The service request.
     *
     * @return request
     */
    public ChangeRequest toRequest() {
      return new ChangeRequest(
          verificationId,
          reasonCode,
          remarks,
          values == null ? Map.of() : new LinkedHashMap<>(values));
    }
  }

  /**
   * A referral to the fulfilment unit.
   *
   * @param channel channel (list CSF_CHANNEL)
   * @param fields value asked for by field (list CSF_REFERRAL_FIELD)
   * @param remarks remarks
   */
  record Referral(
      @NotBlank String channel,
      @NotNull Map<String, @Size(max = RequestLimits.VALUE) String> fields,
      @Size(max = RequestLimits.REMARKS) String remarks) {

    /**
     * The service request.
     *
     * @return request
     */
    public ReferralRequest toRequest() {
      return new ReferralRequest(channel, fields, remarks);
    }
  }

  /**
   * A resend (FR-CSF-030, 031).
   *
   * @param documentId attachment of the renewal advice, or e-policy id
   * @param recipient another address, empty for the registered e-mail
   * @param reason reason for another address
   */
  record Resend(
      @NotNull Long documentId,
      @Size(max = RequestLimits.VALUE) String recipient,
      @Size(max = RequestLimits.REMARKS) String reason) {

    /**
     * The service request.
     *
     * @return request
     */
    public ResendRequest toRequest() {
      return new ResendRequest(documentId, recipient, reason);
    }
  }
}
