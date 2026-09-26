package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService.NewLine;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code BCL_INSURER_CLAIM_NO} of insurer claim numbers (BRCLM.043; design 9.5,
 * FR-CL-021): claim number, insurer, insurer claim number and the date reported to the insurer. The
 * number goes on the claim's line of that insurer still waiting for a number, else on a new line;
 * the duplicate rules of the screen apply, and a number already on another claim is accepted with
 * the alert {@code BCL_INSURER_CLAIM_NO_REUSED}.
 */
@Component
public class InsurerClaimNoBulkHandler implements BulkImportHandler {

  private static final String REPORTED_ON = "Reported To Insurer On";

  private final BulkClaimResolver resolver;
  private final InsurerClaimService lines;

  /**
   * Creates the handler.
   *
   * @param resolver claim of a row
   * @param lines insurer lines
   */
  public InsurerClaimNoBulkHandler(BulkClaimResolver resolver, InsurerClaimService lines) {
    this.resolver = resolver;
    this.lines = lines;
  }

  @Override
  public String code() {
    return "BCL_INSURER_CLAIM_NO";
  }

  @Override
  public String title() {
    return "Claims - insurer claim numbers";
  }

  @Override
  public String permission() {
    return Permission.BCL_RECORD.name();
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(BulkClaimResolver.CLAIM_NO, "BDOI claim number", "BCL-2026-000001"),
        BulkColumn.required(BulkClaimResolver.INSURER, "Insurer code", "INS-MGIC"),
        BulkColumn.required(
            BulkClaimResolver.INSURER_CLAIM_NO, "Insurer's claim number", "MGIC-CL-7781"),
        new BulkColumn(
            REPORTED_ON,
            "Date the loss was reported to the insurer",
            false,
            BulkColumn.Type.DATE,
            "2026-09-20"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(BulkClaimResolver.CLAIM_NO)
        + "|"
        + row.text(BulkClaimResolver.INSURER)
        + "|"
        + row.text(BulkClaimResolver.INSURER_CLAIM_NO);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      Claim claim = resolver.byClaimNo(context.companyId(), row);
      if (claim.isClosed()) {
        errors.add("Claim " + claim.getClaimNo() + " is closed");
      }
    } catch (BusinessRuleException ex) {
      errors.add(ex.getMessage());
    }
    LocalDate reported = row.date(REPORTED_ON);
    if (reported != null && reported.isAfter(context.businessDate())) {
      errors.add("The date cannot be in the future");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Claim claim = resolver.byClaimNo(context.companyId(), row);
    String insurer = row.text(BulkClaimResolver.INSURER);
    String number = row.text(BulkClaimResolver.INSURER_CLAIM_NO);
    Optional<InsurerClaim> waiting =
        lines.ofClaim(claim.getId()).stream()
            .filter(l -> l.getInsurerCode().equals(insurer) && l.getInsurerClaimNo() == null)
            .findFirst();
    if (waiting.isPresent()) {
      lines.number(claim, waiting.get().getId(), number, row.date(REPORTED_ON), true);
    } else {
      lines.add(claim, new NewLine(insurer, null, number, row.date(REPORTED_ON), null), true);
    }
    return claim.getClaimNo() + " " + insurer + " " + number;
  }
}
