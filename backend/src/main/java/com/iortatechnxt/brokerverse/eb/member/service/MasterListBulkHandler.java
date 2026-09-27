package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@value #CODE} (BRID-013, 014; FR-EB-054): the master list the client sends, loaded
 * by the AO on a programme (parameter {@code programmeId}) and policy year (parameter {@code
 * policyYear}; the latest cycle's year when blank) as one STAGED roster version per upload. The
 * roster holds no health data (EBQ15).
 */
@Component
public class MasterListBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "EB_MASTERLIST";

  /** Parameter: programme id. */
  public static final String PROGRAMME = "programmeId";

  /** Parameter: policy year. */
  public static final String POLICY_YEAR = "policyYear";

  static final String EMPLOYEE_NO = "Employee No";
  static final String LAST = "Last Name";
  static final String FIRST = "First Name";
  static final String BIRTH = "Birth Date";
  static final String GENDER = "Gender";
  static final String CIVIL = "Civil Status";
  static final String PLAN = "Plan";
  static final String DEPENDANTS = "Dependants";
  static final String EFFECTIVE = "Effective From";

  private static final Set<String> GENDERS = Set.of("MALE", "FEMALE");

  private final RosterService roster;
  private final EbRecords records;
  private final EbCycleRepository cycles;
  private final LovService lovs;

  /**
   * Creates the handler.
   *
   * @param roster roster versions
   * @param records programme look-up
   * @param cycles latest cycle (default policy year)
   * @param lovs civil status list
   */
  public MasterListBulkHandler(
      RosterService roster, EbRecords records, EbCycleRepository cycles, LovService lovs) {
    this.roster = roster;
    this.records = records;
    this.cycles = cycles;
    this.lovs = lovs;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Employee Benefits master list";
  }

  @Override
  public String permission() {
    return EbCodes.PERMISSION_MARKET;
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(EMPLOYEE_NO, "Employee number, once per list", "E-00123"),
        BulkColumn.required(LAST, "Last name", "Reyes"),
        BulkColumn.required(FIRST, "First name", "Ana"),
        new BulkColumn(BIRTH, "Birth date", true, BulkColumn.Type.DATE, "1990-04-15"),
        BulkColumn.optional(GENDER, "MALE or FEMALE", "FEMALE"),
        BulkColumn.optional(CIVIL, "Civil status code (list CIVIL_STATUS)", "SINGLE"),
        BulkColumn.required(PLAN, "Plan code of the benefit", "PLAN-A"),
        new BulkColumn(DEPENDANTS, "Number of dependants", false, BulkColumn.Type.NUMBER, "1"),
        new BulkColumn(
            EFFECTIVE, "Coverage start; the policy year start when blank", false,
            BulkColumn.Type.DATE, ""));
  }

  @Override
  public String instructions() {
    return "One row per employee. The list becomes a staged roster version of the programme; "
        + "the account officer reviews it on the programme's Members tab and accepts or rejects it. "
        + "Do not include health information.";
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return GENDER.equals(header) || CIVIL.equals(header) ? clean.toUpperCase(Locale.ROOT) : clean;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    String no = row.text(EMPLOYEE_NO);
    return no == null ? null : no.toUpperCase(Locale.ROOT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      programme(context);
    } catch (RuntimeException e) {
      errors.add("Choose the programme of the master list");
      return errors;
    }
    LocalDate birth = row.date(BIRTH);
    if (birth != null && birth.isAfter(context.businessDate())) {
      errors.add("Birth Date cannot be in the future");
    }
    String gender = row.text(GENDER);
    if (gender != null && !GENDERS.contains(gender)) {
      errors.add("Gender must be MALE or FEMALE");
    }
    String civil = row.text(CIVIL);
    if (civil != null && lovs.values("CIVIL_STATUS").stream().noneMatch(v -> v.getCode().equals(civil))) {
      errors.add("Civil Status " + civil + " is not in the list");
    }
    if (row.number(DEPENDANTS) != null && row.number(DEPENDANTS).signum() < 0) {
      errors.add("Dependants cannot be negative");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    EbProgramme programme = programme(context);
    int year = policyYear(programme, context);
    LocalDate effective = row.date(EFFECTIVE) != null ? row.date(EFFECTIVE) : LocalDate.of(year, 1, 1);
    EbRosterVersion version =
        roster.stage(
            programme,
            year,
            context.jobNo(),
            new RosterService.StagedMember(
                row.text(EMPLOYEE_NO),
                new EbMember.Data(
                    row.text(LAST),
                    row.text(FIRST),
                    row.date(BIRTH),
                    row.text(GENDER),
                    row.text(CIVIL),
                    row.text(PLAN),
                    row.number(DEPENDANTS) == null ? null : row.number(DEPENDANTS).intValue()),
                effective));
    return programme.getProgrammeNo() + " roster " + year + " v" + version.getVersionNo();
  }

  private EbProgramme programme(BulkContext context) {
    return records.programme(context.companyId(), Long.valueOf(context.parameter(PROGRAMME)));
  }

  private int policyYear(EbProgramme programme, BulkContext context) {
    String given = context.parameter(POLICY_YEAR);
    if (given != null && !given.isBlank()) {
      return Integer.parseInt(given.strip());
    }
    return cycles.findByProgrammeIdOrderByPolicyYearDescIdDesc(programme.getId()).stream()
        .findFirst()
        .map(EbCycle::getPolicyYear)
        .orElse(context.businessDate().getYear());
  }
}
