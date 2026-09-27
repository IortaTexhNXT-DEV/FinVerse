package com.iortatechnxt.brokerverse.brokerclaims.location.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRef;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService.NewRef;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code BCL_LOCATION_REF} of insurer location references (BRCLM.042; design 9.5,
 * FR-CM-023): ARN, location item number, insurer, insurer location reference and effective date.
 * Each row becomes the current reference of its location and insurer, ending the previous one.
 */
@Component
public class LocationRefBulkHandler implements BulkImportHandler {

  private static final String ARN = "ARN";
  private static final String ITEM_NO = "Item No.";
  private static final String INSURER = "Insurer";
  private static final String REFERENCE = "Insurer Location Ref";
  private static final String EFFECTIVE_FROM = "Effective From";

  private final CoverService covers;
  private final LocationRefService refs;

  /**
   * Creates the handler.
   *
   * @param covers covers
   * @param refs references
   */
  public LocationRefBulkHandler(CoverService covers, LocationRefService refs) {
    this.covers = covers;
    this.refs = refs;
  }

  @Override
  public String code() {
    return "BCL_LOCATION_REF";
  }

  @Override
  public String title() {
    return "Claims - insurer location references";
  }

  @Override
  public String permission() {
    return Permission.BCL_LOCATION_REF_MAINTAIN.name();
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(ARN, "Account reference number of the cover", "ARN-2026-940002"),
        new BulkColumn(
            ITEM_NO, "Item number of the location on the cover", true, BulkColumn.Type.NUMBER, "1"),
        BulkColumn.required(INSURER, "Insurer code", "INS-MGIC"),
        BulkColumn.required(REFERENCE, "The insurer's reference of the location", "MGIC-LOC-0091"),
        new BulkColumn(
            EFFECTIVE_FROM,
            "First day the reference applies",
            true,
            BulkColumn.Type.DATE,
            "2026-09-01"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(ARN) + "|" + row.text(ITEM_NO) + "|" + row.text(INSURER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      Account account = covers.account(context.companyId(), row.text(ARN));
      LocationRefService.requireLocation(account, row.number(ITEM_NO).intValue());
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      errors.add(ex.getMessage());
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    LocationRef saved =
        refs.maintain(
            context.companyId(),
            new NewRef(
                row.text(ARN),
                row.number(ITEM_NO).intValue(),
                row.text(INSURER),
                row.text(REFERENCE),
                row.date(EFFECTIVE_FROM)));
    return saved.getArn()
        + " item "
        + saved.getAccountItemNo()
        + " "
        + saved.getInsurerLocationRef();
  }
}
