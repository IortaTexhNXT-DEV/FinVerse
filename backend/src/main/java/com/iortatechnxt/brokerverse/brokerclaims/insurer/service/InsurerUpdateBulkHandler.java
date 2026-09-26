package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.BulkClaimResolver.Match;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService.NewUpdate;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code BCL_INSURER_UPDATE} of insurer updates, e.g. an insurer's bordereau (BRCLM.041;
 * design 9.5, FR-CL-022): each row names the claim by BDOI claim number, or by insurer and insurer
 * claim number, with the update date, source, reference and remarks. Every row is validated before
 * the file is applied; the handler of each claim is notified of the updates loaded.
 */
@Component
public class InsurerUpdateBulkHandler implements BulkImportHandler {

  private static final String UPDATE_DATE = "Update Date";
  private static final String SOURCE = "Source";
  private static final String REFERENCE = "Reference";
  private static final String REMARKS = "Remarks";

  private final BulkClaimResolver resolver;
  private final InsurerUpdateService updates;
  private final LovService lovs;
  private final NotificationService notifications;

  /**
   * Creates the handler.
   *
   * @param resolver claim of a row
   * @param updates insurer updates
   * @param lovs update sources
   * @param notifications handler notification
   */
  public InsurerUpdateBulkHandler(
      BulkClaimResolver resolver,
      InsurerUpdateService updates,
      LovService lovs,
      NotificationService notifications) {
    this.resolver = resolver;
    this.updates = updates;
    this.lovs = lovs;
    this.notifications = notifications;
  }

  @Override
  public String code() {
    return "BCL_INSURER_UPDATE";
  }

  @Override
  public String title() {
    return "Claims - insurer updates";
  }

  @Override
  public String permission() {
    return Permission.BCL_RECORD.name();
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(BulkClaimResolver.CLAIM_NO, "BDOI claim number", "BCL-2026-000001"),
        BulkColumn.optional(BulkClaimResolver.INSURER, "Insurer code, with the insurer claim number", "INS-MGIC"),
        BulkColumn.optional(BulkClaimResolver.INSURER_CLAIM_NO, "Insurer's claim number", "MGIC-CL-7781"),
        new BulkColumn(UPDATE_DATE, "Date of the update, not in the future", true, BulkColumn.Type.DATE, "2026-09-20"),
        BulkColumn.required(SOURCE, "Source: EMAIL, LETTER, PORTAL, CALL or FILE (BCL_UPDATE_SOURCE)", "EMAIL"),
        BulkColumn.optional(REFERENCE, "Insurer's reference", "LTR-2026-114"),
        BulkColumn.required(REMARKS, "What the insurer communicated", "Adjuster appointed"));
  }

  @Override
  public String instructions() {
    return "Name the claim by its BDOI claim number, or by the insurer and the insurer claim number."
        + " Updates are added to the claim timeline and cannot be edited afterwards.";
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      resolver.match(context.companyId(), row);
    } catch (BusinessRuleException ex) {
      errors.add(ex.getMessage());
    }
    LocalDate date = row.date(UPDATE_DATE);
    if (date != null && date.isAfter(context.businessDate())) {
      errors.add("Enter an update date that is not in the future");
    }
    try {
      lovs.requireValid(ClaimCodes.LOV_UPDATE_SOURCE, row.text(SOURCE), context.businessDate());
    } catch (BusinessRuleException ex) {
      errors.add(ex.getMessage());
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Match match = resolver.match(context.companyId(), row);
    Claim claim = match.claim();
    InsurerUpdate saved =
        updates.record(
            claim,
            new NewUpdate(
                match.line() == null ? null : match.line().getId(),
                row.date(UPDATE_DATE),
                row.text(SOURCE),
                row.text(REFERENCE),
                row.text(REMARKS),
                List.of(),
                null,
                context.jobNo()));
    notifications.notifyUser(
        claim.getHandler(),
        new Notice(
            "Insurer update loaded on " + claim.getClaimNo(),
            "Upload " + context.jobNo() + ": " + saved.getSource() + " of " + saved.getUpdateDate(),
            "/claims-handling/" + claim.getId(),
            ClaimCodes.ENTITY_TYPE,
            String.valueOf(claim.getId())));
    return claim.getClaimNo();
  }
}
