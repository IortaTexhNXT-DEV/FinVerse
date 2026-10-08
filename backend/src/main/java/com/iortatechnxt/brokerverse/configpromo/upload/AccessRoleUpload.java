package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * UA-03 Approval rules of user access: gives a user the profile of his role in user access
 * (requestor, approver, second approver, group-profile requester, implementer), his business unit
 * and his authorisation limit. A profile that the separation-of-duties rules forbid next to a
 * profile the user holds is refused. UAM_ANY_APPROVER is a security parameter changed with UA-05.
 */
@Component
public class AccessRoleUpload extends ConfigUploadHandler {
  private static final String APPROVER_ROLE = "Approver";

  static final String ROLE = "Role in user access";
  static final String USER = "User ID";
  static final String UNIT = "Unit";
  static final String ORDER = "Approver order for group profiles";
  static final String ANY = "Any approver may decide";
  static final String LIMIT = "Authorisation limit";

  private static final Map<String, String> PROFILES =
      Map.of(
          "Requestor",
          "UAM_REQUESTOR",
          APPROVER_ROLE,
          "UAM_APPROVER",
          "Second Approver",
          "UAM_SECOND_APPROVER",
          "Group-profile requester",
          "BUSINESS_ADMIN",
          "Implementer",
          "SYSADMIN");
  private static final List<String> ROLES =
      List.of(
          "Requestor", APPROVER_ROLE, "Second Approver", "Group-profile requester", "Implementer");
  private static final String ANY_APPROVER = "UAM_ANY_APPROVER";
  private static final int MAX_ORDER = 9;

  private final ListParameterRows lists;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param lists lists and parameters
   */
  public AccessRoleUpload(UploadSupport db, ListParameterRows lists) {
    super(db);
    this.lists = lists;
  }

  @Override
  public String code() {
    return "CFG_ACCESS_ROLE";
  }

  @Override
  public String templateId() {
    return "UA-03";
  }

  @Override
  public String title() {
    return "User access approval rules";
  }

  @Override
  public String screen() {
    return "System Administration > Users";
  }

  @Override
  public String permission() {
    return "USER_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "ACCESS_APPROVE";
  }

  @Override
  public String filledBy() {
    return "Business Administration (User Access)";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(ROLE, "The user access role of the user", APPROVER_ROLE)
            .values(ROLES.toArray(String[]::new)),
        BulkColumn.required(USER, "The user who holds the role", "a013000205").master("user"),
        BulkColumn.required(UNIT, "Unit the user requests or approves for", "Combank Marketing")
            .lov("UAM_BUSINESS_UNIT"),
        new BulkColumn(
                ORDER,
                "Usual order of the approvers of a group-profile request",
                false,
                Type.NUMBER,
                "1")
            .when(APPROVER_ROLE),
        new BulkColumn(
            ANY, "Must match UAM_ANY_APPROVER (changed with UA-05)", false, Type.YES_NO, "N"),
        new BulkColumn(
            LIMIT,
            "Limit of the approvals of the user; blank = unlimited",
            false,
            Type.NUMBER,
            "500000"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(ROLE) + "|" + row.text(USER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    oneOf(errors, ROLE, row.text(ROLE), ROLES);
    Optional<Long> user = user(row);
    check(errors, user.isEmpty(), USER, row.text(USER) + " is not a user");
    check(
        errors,
        unit(row).isEmpty(),
        UNIT,
        row.text(UNIT) + " is not a value of the list Business unit group");
    whole(errors, ORDER, row.number(ORDER), 1, MAX_ORDER);
    BigDecimal limit = row.number(LIMIT);
    check(errors, limit != null && limit.signum() < 0, LIMIT, "cannot be negative");
    checkAnyApprover(row, errors);
    if (errors.isEmpty()) {
      conflict(user.orElseThrow(), PROFILES.get(row.text(ROLE)))
          .ifPresent(
              c ->
                  errors.add(
                      error(
                          ROLE,
                          "the user holds "
                              + c
                              + ", which the separation of duties forbids with it")));
    }
    return errors;
  }

  private void checkAnyApprover(BulkRow row, List<String> errors) {
    if (row.text(ANY) == null) {
      return;
    }
    String wanted = row.yes(ANY) ? "true" : "false";
    String current = lists.parameterValue(ANY_APPROVER).orElse("false");
    check(
        errors,
        !wanted.equalsIgnoreCase(current),
        ANY,
        ANY_APPROVER + " is " + current + "; change it with the parameters upload (UA-05)");
  }

  private Optional<Long> user(BulkRow row) {
    return db.id("select id from sec_user where lower(username) = lower(?)", row.text(USER));
  }

  private Optional<String> unit(BulkRow row) {
    return db.text(
        "select code from lov_value where type_code = 'UAM_BUSINESS_UNIT' and record_status = 'ACTIVE'"
            + " and (code = ? or lower(label) = lower(?))",
        row.text(UNIT),
        row.text(UNIT));
  }

  /** A profile of the user that an active separation-of-duties rule forbids with the profile. */
  private Optional<String> conflict(Long userId, String profile) {
    return db.text(
        "select r.name from nba_sod_rule s join sec_role r on r.code ="
            + " case when s.profile_a = ? then s.profile_b else s.profile_a end"
            + " join sec_user_role ur on ur.role_id = r.id and ur.user_id = ?"
            + " where s.record_status = 'ACTIVE' and (s.profile_a = ? or s.profile_b = ?)",
        profile,
        userId,
        profile,
        profile);
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.exists(
            "select 1 from sec_user_role ur join sec_role r on r.id = ur.role_id"
                + " join sec_user u on u.id = ur.user_id where lower(u.username) = lower(?) and r.code = ?",
            row.text(USER),
            PROFILES.get(row.text(ROLE)))
        ? UploadSupport.UPDATE
        : UploadSupport.ADD;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    Long userId = user(row).orElseThrow();
    String username = db.text("select username from sec_user where id = ?", userId).orElseThrow();
    db.upsert(
        "sec_user",
        columns("username", username),
        columns(
            "business_unit_code",
            unit(row).orElseThrow(),
            "authorization_limit",
            row.number(LIMIT)),
        context,
        "User");
    String profile = PROFILES.get(row.text(ROLE));
    db.execute(
        "insert into sec_user_role (user_id, role_id) select ?, r.id from sec_role r where r.code = ?"
            + " and not exists (select 1 from sec_user_role x where x.user_id = ? and x.role_id = r.id)",
        userId,
        profile,
        userId);
    return username + " " + row.text(ROLE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select r.code, u.username, coalesce(v.label, u.business_unit_code) as unit, u.authorization_limit"
                + " from sec_user u join sec_user_role ur on ur.user_id = u.id join sec_role r on r.id = ur.role_id"
                + " left join lov_value v on v.type_code = 'UAM_BUSINESS_UNIT' and v.code = u.business_unit_code"
                + " where r.code in ('UAM_REQUESTOR', 'UAM_APPROVER', 'UAM_SECOND_APPROVER',"
                + " 'BUSINESS_ADMIN', 'SYSADMIN')"
                + " and u.enabled and u.business_unit_code is not null order by r.code, u.username")
        .stream()
        .map(
            r ->
                exportRow(
                    ROLE,
                        PROFILES.entrySet().stream()
                            .filter(e -> e.getValue().equals(r.get("code")))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null),
                    USER, r.get("username"),
                    UNIT, r.get("unit"),
                    LIMIT, r.get("authorization_limit")))
        .toList();
  }
}
