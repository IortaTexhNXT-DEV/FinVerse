package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * UA-04 Separation-of-duties rules: a Prevent rule between two group profiles becomes a rule of
 * Separation of Duties (one rule per pair when a cell names several profiles), active once the
 * upload is authorised. Rules controlled by review or by a second approval are not rules of the
 * platform; such rows are refused with where they are handled.
 */
@Component
public class SodRuleUpload extends ConfigUploadHandler {

  static final String RULE_ID = "Rule ID";
  static final String RULE = "Rule";
  static final String STATUS = "Status";
  static final String PROFILE_A = "Profile or permission A";
  static final String PROFILE_B = "Profile or permission B";
  static final String CONTROL = "Control";
  static final String FREQUENCY = "Review frequency";

  static final String PREVENT = "Prevent";
  private static final List<String> STATUSES = List.of("Enforced", "To confirm", "New");
  private static final List<String> CONTROLS =
      List.of(PREVENT, "Detect by review", "Second approval");

  /** A pair of profiles of a rule. */
  private record Pair(String a, String b) {}

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public SodRuleUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_SOD_RULE";
  }

  @Override
  public String templateId() {
    return "UA-04";
  }

  @Override
  public String title() {
    return "Separation-of-duties rules";
  }

  @Override
  public String screen() {
    return "User Access > Separation of Duties";
  }

  @Override
  public String permission() {
    return "UAM_SOD_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "UAM_SOD_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Business Administration; Information Security";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(RULE_ID, "Identifier of the rule", "SOD-01").format("SOD-nn"),
        BulkColumn.required(RULE, "The rule", "The requester never decides his own request"),
        BulkColumn.required(STATUS, "Enforced = a fixed rule of BIBS", "Enforced")
            .values(STATUSES.toArray(String[]::new)),
        BulkColumn.optional(PROFILE_A, "First profile of the combination", "UAM_REQUESTOR")
            .when("combination rule")
            .format("Group profile codes separated by commas"),
        BulkColumn.optional(PROFILE_B, "Profile that may not be held with A", "UAM_APPROVER")
            .when("combination rule")
            .format("Group profile codes separated by commas"),
        BulkColumn.required(CONTROL, "How the rule is controlled", PREVENT)
            .values(CONTROLS.toArray(String[]::new)),
        BulkColumn.optional(FREQUENCY, "How often the reports are reviewed", "Quarterly")
            .values("Monthly", "Quarterly"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(RULE_ID);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    oneOf(errors, STATUS, row.text(STATUS), STATUSES);
    oneOf(errors, CONTROL, row.text(CONTROL), CONTROLS);
    if (!errors.isEmpty()) {
      return errors;
    }
    if (!PREVENT.equals(row.text(CONTROL))) {
      errors.add(
          error(
              CONTROL,
              row.text(CONTROL)
                  + " rules are followed with the access reports and are not loaded;"
                  + " only Prevent rules between two group profiles are"));
      return errors;
    }
    if (row.text(PROFILE_A) == null || row.text(PROFILE_B) == null) {
      errors.add(error(PROFILE_B, "a Prevent rule names the two group profiles"));
      return errors;
    }
    checkProfiles(row, errors);
    return errors;
  }

  private void checkProfiles(BulkRow row, List<String> errors) {
    for (String header : List.of(PROFILE_A, PROFILE_B)) {
      for (String p : names(row.text(header))) {
        check(errors, profile(p).isEmpty(), header, p + " is not a group profile");
      }
    }
    if (errors.isEmpty()) {
      for (Pair pair : pairs(row)) {
        check(errors, pair.a().equals(pair.b()), PROFILE_B, pair.a() + " is also profile A");
      }
    }
  }

  private static List<String> names(String cell) {
    return Arrays.stream(cell.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  /** A group profile by its code or its name. */
  private Optional<String> profile(String text) {
    return db.text(
        "select code from sec_role where (code = ? or lower(name) = lower(?)) and active",
        text,
        text);
  }

  private List<Pair> pairs(BulkRow row) {
    List<Pair> pairs = new ArrayList<>();
    for (String a : names(row.text(PROFILE_A))) {
      for (String b : names(row.text(PROFILE_B))) {
        pairs.add(new Pair(profile(a).orElse(a), profile(b).orElse(b)));
      }
    }
    return pairs;
  }

  private Optional<String> existing(Pair pair) {
    return db.text(
        "select rule_code from nba_sod_rule where record_status <> 'INACTIVE'"
            + " and ((profile_a = ? and profile_b = ?) or (profile_a = ? and profile_b = ?)) order by id",
        pair.a(),
        pair.b(),
        pair.b(),
        pair.a());
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return pairs(row).stream().allMatch(p -> existing(p).isPresent())
        ? UploadSupport.UPDATE
        : UploadSupport.ADD;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    List<Pair> pairs = pairs(row);
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < pairs.size(); i++) {
      Pair pair = pairs.get(i);
      String wanted = pairs.size() == 1 ? row.text(RULE_ID) : row.text(RULE_ID) + "-" + (i + 1);
      String code = existing(pair).orElseGet(() -> freeCode(wanted, context));
      db.upsert(
          "nba_sod_rule",
          columns("rule_code", code),
          columns(
              "profile_a", pair.a(),
              "profile_b", pair.b(),
              "description", row.text(RULE),
              "record_status", "ACTIVE",
              "pending_action", "NONE"),
          context,
          "Separation of duties rule");
      codes.add(code);
    }
    return String.join(", ", codes);
  }

  /** The rule ID of the workbook, or with the upload number when another rule holds it. */
  private String freeCode(String wanted, BulkContext context) {
    return db.exists("select 1 from nba_sod_rule where rule_code = ?", wanted)
        ? wanted + "-" + context.jobNo()
        : wanted;
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select rule_code, description, profile_a, profile_b from nba_sod_rule"
                + " where record_status = 'ACTIVE' order by rule_code")
        .stream()
        .map(
            r ->
                exportRow(
                    RULE_ID, r.get("rule_code"),
                    RULE, r.get("description"),
                    STATUS, "Enforced",
                    PROFILE_A, r.get("profile_a"),
                    PROFILE_B, r.get("profile_b"),
                    CONTROL, PREVENT))
        .toList();
  }
}
