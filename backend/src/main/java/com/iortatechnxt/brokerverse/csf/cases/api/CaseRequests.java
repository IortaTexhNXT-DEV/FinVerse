package com.iortatechnxt.brokerverse.csf.cases.api;

import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Category;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseService.LogRequest;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Request bodies of the case management API (FR-CSF-050 to 061). */
public final class CaseRequests {

  /** Longest text area. */
  static final int TEXT = 4000;

  /** Longest short text. */
  static final int SHORT = 500;

  private CaseRequests() {}

  /**
   * A new case.
   *
   * @param clientId client, null for a caller who is not a client
   * @param accountId account of the client
   * @param callerName caller name
   * @param callerContact caller contact
   * @param channel channel
   * @param caseType case type
   * @param nature nature of transaction
   * @param productType product type
   * @param marketSegment market segment
   * @param fulfilmentUnit fulfilment unit
   * @param priority priority
   * @param subject subject
   * @param description description
   * @param resolveNow Resolve and Close
   * @param resolution resolution
   * @param closureReason closure reason
   */
  public record LogCase(
      Long clientId,
      Long accountId,
      @Size(max = 200) String callerName,
      @Size(max = 200) String callerContact,
      @Size(max = 40) String channel,
      @Size(max = 40) String caseType,
      @Size(max = 40) String nature,
      @Size(max = 40) String productType,
      @Size(max = 40) String marketSegment,
      @Size(max = 40) String fulfilmentUnit,
      @Size(max = 40) String priority,
      @Size(max = 150) String subject,
      @Size(max = TEXT) String description,
      boolean resolveNow,
      @Size(max = TEXT) String resolution,
      @Size(max = 40) String closureReason) {

    /**
     * The service request.
     *
     * @return request
     */
    public LogRequest toRequest() {
      return new LogRequest(
          clientId,
          accountId,
          callerName,
          callerContact,
          new Category(
              channel, caseType, nature, productType, marketSegment, fulfilmentUnit, priority),
          subject,
          description,
          resolveNow,
          resolution,
          closureReason);
    }
  }

  /**
   * A resolution with its closure reason.
   *
   * @param resolution resolution
   * @param closureReason closure reason
   */
  public record Close(@Size(max = TEXT) String resolution, @Size(max = 40) String closureReason) {}

  /**
   * A reason with a comment.
   *
   * @param reason reason code
   * @param comment comment
   */
  public record Reason(@Size(max = 40) String reason, @Size(max = SHORT) String comment) {}

  /**
   * A text: a note, an instruction, a comment or a reason in words.
   *
   * @param text text
   */
  public record Text(@Size(max = TEXT) String text) {}

  /**
   * A reassignment.
   *
   * @param toUser new owner
   * @param toUnit new fulfilment unit
   * @param comment comment
   */
  public record Reassign(
      @Size(max = 50) String toUser,
      @Size(max = 40) String toUnit,
      @Size(max = SHORT) String comment) {}

  /**
   * A change of category.
   *
   * @param caseType case type
   * @param nature nature of transaction
   * @param reason reason
   */
  public record ChangeCategory(
      @Size(max = 40) String caseType,
      @Size(max = 40) String nature,
      @Size(max = SHORT) String reason) {}

  /**
   * The turnaround time of a case type.
   *
   * @param caseType case type
   * @param tatDays working days
   * @param autoEscalate escalate on breach
   */
  public record TypeSetting(@Size(max = 40) String caseType, int tatDays, boolean autoEscalate) {}

  /**
   * A routing row.
   *
   * @param caseType case type or blank
   * @param marketSegment market segment or blank
   * @param nature nature or blank
   * @param fulfilmentUnit fulfilment unit
   * @param escalationGroup fulfilment unit of the escalations, blank for the supervisors
   * @param active active
   */
  public record Route(
      @Size(max = 40) String caseType,
      @Size(max = 40) String marketSegment,
      @Size(max = 40) String nature,
      @Size(max = 40) String fulfilmentUnit,
      @Size(max = 40) String escalationGroup,
      boolean active) {}

  /**
   * A member of a fulfilment unit.
   *
   * @param unitCode unit
   * @param username user ID
   */
  public record Member(@Size(max = 40) String unitCode, @Size(max = 50) String username) {}

  /**
   * The filters of the Case Dashboard.
   *
   * @param from first creation day
   * @param to last creation day
   * @param caseType case type
   * @param priority priority
   * @param status status
   * @param marketSegment market segment
   * @param fulfilmentUnit fulfilment unit
   */
  public record DashboardFilter(
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @Size(max = 40) String caseType,
      @Size(max = 40) String priority,
      @Size(max = 40) String status,
      @Size(max = 40) String marketSegment,
      @Size(max = 40) String fulfilmentUnit) {}
}
