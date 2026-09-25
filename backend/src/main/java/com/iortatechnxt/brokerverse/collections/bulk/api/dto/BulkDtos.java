package com.iortatechnxt.brokerverse.collections.bulk.api.dto;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Requests of the Collections bulk actions (BRCLXN.050/051). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class BulkDtos {

  private static final int MAX_INVOICES = 500;

  private BulkDtos() {}

  /**
   * A manual escalation.
   *
   * @param companyId company
   * @param invoiceNos invoices
   * @param targetLevel TL, UH, SECTION_HEAD or USER
   * @param targetUsername designated user (required for USER)
   * @param reasonCode reason (CLX_ESCALATION_REASON)
   * @param remarks remarks
   */
  public record EscalateRequest(
      @NotNull Long companyId,
      @NotEmpty @Size(max = MAX_INVOICES) List<@NotBlank String> invoiceNos,
      @NotNull TargetLevel targetLevel,
      @Size(max = 50) String targetUsername,
      @NotBlank @Size(max = 40) String reasonCode,
      @Size(max = 1000) String remarks) {}

  /**
   * One promise on several invoices.
   *
   * @param companyId company
   * @param invoiceNos invoices
   * @param promisedOn day of the promise (today when empty)
   * @param promisedDate promised payment date
   * @param amount amount per invoice (whole outstanding when empty)
   * @param remarks remarks
   */
  public record PromisesRequest(
      @NotNull Long companyId,
      @NotEmpty @Size(max = MAX_INVOICES) List<@NotBlank String> invoiceNos,
      LocalDate promisedOn,
      @NotNull LocalDate promisedDate,
      @Positive BigDecimal amount,
      @Size(max = 500) String remarks) {}
}
