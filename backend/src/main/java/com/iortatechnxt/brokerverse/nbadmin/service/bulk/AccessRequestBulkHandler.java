package com.iortatechnxt.brokerverse.nbadmin.service.bulk;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Bulk access requests (BRD 1.009; FR-UA-019), handler {@value #CODE}: one row per user with the
 * action (enrol, modify, deactivate, reactivate) and the data of a single request. Every row is
 * checked as a single request before the batch is created; the valid rows become the draft lines of
 * one batch, which the requester submits to the chosen approver on the Bulk Request screen.
 */
@Component
public class AccessRequestBulkHandler implements BulkImportHandler {

  /** Handler code (template UAM_ACCESS_REQUEST). */
  public static final String CODE = "UAM_ACCESS_REQUEST";

  static final String ACTION = "Action";
  static final String USER_ID = "User ID";
  static final String FULL_NAME = "Full Name";
  static final String EMAIL = "E-mail";
  static final String WINDOWS_ID = "Windows ID";
  static final String BRANCH = "Home Branch Code";
  static final String BUSINESS_UNIT = "Business Unit";
  static final String USER_LEVEL = "User Level";
  static final String PROFILES = "Group Profiles";
  static final String EFFECTIVE = "Effective Date";
  static final String REMARKS = "Remarks";

  private static final Map<String, AccessRequestType> ACTIONS =
      Map.of(
          "ENROL", AccessRequestType.CREATE_USER,
          "ENROLL", AccessRequestType.CREATE_USER,
          "MODIFY", AccessRequestType.MODIFY_USER,
          "DEACTIVATE", AccessRequestType.DISABLE_USER,
          "REACTIVATE", AccessRequestType.ENABLE_USER);

  private final AccessBatchService batches;
  private final BranchRepository branches;

  /**
   * Creates the handler.
   *
   * @param batches bulk batches
   * @param branches branches (home branch code)
   */
  public AccessRequestBulkHandler(AccessBatchService batches, BranchRepository branches) {
    this.batches = batches;
    this.branches = branches;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Bulk access requests";
  }

  @Override
  public String permission() {
    return "UAM_ENROLL";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(ACTION, "ENROL, MODIFY, DEACTIVATE or REACTIVATE", "ENROL"),
        BulkColumn.required(USER_ID, "User ID (USER_ID_PATTERN for a new user)", "a013000201"),
        BulkColumn.optional(FULL_NAME, "Full name (required to enrol)", "Juan Dela Cruz"),
        BulkColumn.optional(EMAIL, "E-mail", "juan.delacruz@bdo.com.ph"),
        BulkColumn.optional(WINDOWS_ID, "Windows ID (unique)", "JDELACRUZ"),
        BulkColumn.optional(BRANCH, "Home branch code", "HO"),
        BulkColumn.optional(BUSINESS_UNIT, "Business unit group (list UAM_BUSINESS_UNIT)", ""),
        BulkColumn.optional(USER_LEVEL, "User level (list UAM_USER_LEVEL)", ""),
        BulkColumn.optional(
            PROFILES, "Group profile codes separated by ';' (required to enrol)", "MKT_AO"),
        new BulkColumn(
            EFFECTIVE,
            "Date the change applies (yyyy-MM-dd); blank = on approval",
            false,
            BulkColumn.Type.DATE,
            ""),
        BulkColumn.required(REMARKS, "Justification of the line", "Joined Marketing"));
  }

  @Override
  public String instructions() {
    return "One row per user. The batch is submitted to one approver with the batch remarks on the"
        + " Bulk Request screen; every line is checked and applied as a single request.";
  }

  @Override
  public String duplicateKey(BulkRow row) {
    String user = row.text(USER_ID);
    return user == null ? null : user.toLowerCase(Locale.ROOT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    try {
      batches.validateLine(content(row, context));
      return List.of();
    } catch (BusinessRuleException | AccessDeniedException e) {
      return List.of(e.getMessage());
    }
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return batches
        .addLine(context.jobNo(), context.parameter("fileName"), content(row, context))
        .getRequestNo();
  }

  private AccessRequestContent content(BulkRow row, BulkContext context) {
    String action = row.text(ACTION).toUpperCase(Locale.ROOT);
    AccessRequestType type = ACTIONS.get(action);
    if (type == null) {
      throw new BusinessRuleException(
          "ACCESS_BULK_ACTION", "Action " + row.text(ACTION) + " is not valid");
    }
    return new AccessRequestContent(
            type,
            row.text(USER_ID),
            row.text(FULL_NAME),
            row.text(EMAIL),
            profiles(row.text(PROFILES)),
            branch(row.text(BRANCH), context),
            row.text(REMARKS),
            null)
        .withUserData(
            new RequestedUserData(
                row.text(WINDOWS_ID), row.text(BUSINESS_UNIT), row.text(USER_LEVEL), null, false))
        .withEffectiveFrom(row.date(EFFECTIVE));
  }

  private Long branch(String code, BulkContext context) {
    if (code == null) {
      return null;
    }
    return branches
        .findByCompanyIdAndCode(context.companyId(), code.trim())
        .map(Branch::getId)
        .orElseThrow(
            () -> new BusinessRuleException("ACCESS_BULK_BRANCH", "Unknown branch " + code));
  }

  private static Set<String> profiles(String value) {
    if (value == null) {
      return Set.of();
    }
    Set<String> codes = new LinkedHashSet<>();
    Arrays.stream(value.split("[;,]"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .forEach(s -> codes.add(s.toUpperCase(Locale.ROOT)));
    return codes;
  }
}
