package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.LamdLine;
import com.iortatechnxt.brokerverse.renewal.domain.LamdStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BDOI's LAMD lists (FRRN.012.01 to FRRN.012.05): each loan of the CBG loans list (excluding
 * personal loans) or of the paid-off list is saved with its details and matched to the open renewal
 * accounts, on the PN for a home loan and on the PN, the serial number and the motor number for a
 * motor loan. The loan status is derived: Loan Fully Paid from the paid-off list, RMU when the
 * Account Officer of the loan is in the RMU list, else Active; Unmatched without a renewal. A
 * paid-off or RMU loan routes its renewal through the LAMD check; the paid-off accounts are kept in
 * a repository without duplicates.
 */
@Service
@Transactional
public class LamdLoanService {

  /** Parameter: words of the product description of a motor loan. */
  public static final String MOTOR_PRODUCTS = "RNW_LAMD_MOTOR_PRODUCTS";

  /** Kind of the CBG loans list. */
  public static final String CBG_LOANS = "CBG_LOANS";

  /** Kind of the paid-off list. */
  public static final String PAID_OFF = "PAID_OFF";

  /** Matching status of a matched loan. */
  public static final String MATCHED = "MATCHED";

  /** Matching status of an unmatched loan. */
  public static final String UNMATCHED = "UNMATCHED";

  private static final String INSERT =
      "insert into rnw_lamd_loan (company_id, job_no, row_no, report_kind, pn_no, ibg_cbg_tag,"
          + " book_date, maturity_date, branch, product_type, ao_code, ao_name, email,"
          + " mailing_address, serial_no, motor_no, short_name, loan_status, candidate_id,"
          + " matching_status, processing_status, reason, created_at, created_by) values"
          + " (:companyId, :jobNo, :rowNo, :kind, :pn, :tag, :bookDate, :maturityDate, :branch,"
          + " :productType, :aoCode, :aoName, :email, :address, :serial, :motor, :shortName,"
          + " :loanStatus, :candidateId, :matching, :processing, :reason, :at, :by)";

  private static final String REPOSITORY =
      "insert into rnw_paid_off_account (company_id, pn_no, serial_no, motor_no, ibg_cbg_tag,"
          + " product_type, email, mailing_address, first_job_no, created_at, created_by)"
          + " values (:companyId, :pn, :serial, :motor, :tag, :productType, :email, :address,"
          + " :jobNo, :at, :by) on conflict do nothing";

  private static final String VEHICLE =
      "select count(*) from acc_account a join acc_risk_item i on i.account_id = a.id"
          + " where a.arn = :arn and upper(trim(i.chassis_no)) = upper(trim(:serial))"
          + " and upper(trim(i.engine_no)) = upper(trim(:motor))";

  private static final String RMU =
      "select count(*) from rnw_rmu_officer where company_id = :companyId and active"
          + " and ao_code = :aoCode";

  private final LamdService lamd;
  private final NamedParameterJdbcTemplate jdbc;
  private final SystemParameterService parameters;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lamd LAMD routing
   * @param jdbc loans, repository and vehicles
   * @param parameters system parameters
   * @param currentUser uploading user
   * @param clock clock
   */
  public LamdLoanService(
      LamdService lamd,
      NamedParameterJdbcTemplate jdbc,
      SystemParameterService parameters,
      CurrentUser currentUser,
      Clock clock) {
    this.lamd = lamd;
    this.jdbc = jdbc;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Whether a loan is a motor loan by its product description.
   *
   * @param productDescription product description of the loan
   * @return true for a motor loan
   */
  @Transactional(readOnly = true)
  public boolean motor(String productDescription) {
    return productDescription != null
        && parameters.items(MOTOR_PRODUCTS).stream()
            .map(String::strip)
            .filter(w -> !w.isEmpty())
            .anyMatch(
                w ->
                    Pattern.compile(w, Pattern.CASE_INSENSITIVE | Pattern.LITERAL)
                        .matcher(productDescription)
                        .find());
  }

  /**
   * Saves and matches one loan of a list.
   *
   * @param companyId company
   * @param jobNo upload
   * @param rowNo row of the file
   * @param kind CBG_LOANS or PAID_OFF
   * @param loan loan details
   * @return outcome with the matching status as category
   */
  public BulkOutcome record(Long companyId, String jobNo, int rowNo, String kind, Loan loan) {
    boolean motor = motor(loan.productType());
    Optional<RenewalCandidate> match = match(companyId, loan, motor);
    String loanStatus = loanStatus(companyId, kind, loan, match.isPresent());
    String reason;
    if (match.isEmpty()) {
      reason =
          motor
              ? "No open renewal account with PN " + loan.pn() + ", serial and motor numbers"
              : "No open renewal account with PN " + loan.pn();
    } else {
      reason = route(companyId, jobNo, rowNo, loan, loanStatus, match.get());
    }
    if (PAID_OFF.equals(kind)) {
      jdbc.update(REPOSITORY, args(companyId, jobNo, loan));
    }
    Map<String, Object> a = args(companyId, jobNo, loan);
    a.put("rowNo", rowNo);
    a.put("kind", kind);
    a.put("bookDate", loan.bookDate());
    a.put("maturityDate", loan.maturityDate());
    a.put("branch", loan.branch());
    a.put("aoCode", loan.aoCode());
    a.put("aoName", loan.aoName());
    a.put("shortName", loan.shortName());
    a.put("loanStatus", loanStatus);
    a.put("candidateId", match.map(RenewalCandidate::getId).orElse(null));
    a.put("matching", match.isPresent() ? MATCHED : UNMATCHED);
    a.put("processing", "SUCCESS");
    a.put("reason", reason);
    jdbc.update(INSERT, a);
    return new BulkOutcome(reason, match.isPresent() ? MATCHED : UNMATCHED);
  }

  private String route(
      Long companyId, String jobNo, int rowNo, Loan loan, String loanStatus, RenewalCandidate c) {
    LamdStatus status =
        switch (loanStatus) {
          case "LOAN_FULLY_PAID" -> LamdStatus.PAID_OFF;
          case "RMU" -> LamdStatus.RMU;
          default -> null;
        };
    if (status == null) {
      return c.getRenewalRef() + ": loan Active";
    }
    LamdLine line =
        lamd.lineFor(
            companyId,
            jobNo,
            new LamdService.Header(
                status, BusinessClock.today(clock).toString().substring(0, "yyyy-MM".length())),
            rowNo,
            new LamdLine.Loan(loan.pn(), status, BusinessClock.today(clock), loan.shortName()),
            c);
    return line.getMessage();
  }

  private String loanStatus(Long companyId, String kind, Loan loan, boolean matched) {
    if (PAID_OFF.equals(kind)) {
      return "LOAN_FULLY_PAID";
    }
    if (loan.aoCode() != null) {
      Long rmu =
          jdbc.queryForObject(
              RMU, Map.of("companyId", companyId, "aoCode", loan.aoCode().strip()), Long.class);
      if (rmu != null && rmu > 0) {
        return "RMU";
      }
    }
    return matched ? "ACTIVE" : UNMATCHED;
  }

  private Optional<RenewalCandidate> match(Long companyId, Loan loan, boolean motor) {
    List<RenewalCandidate> byPn = lamd.openByPn(companyId, loan.pn());
    if (!motor) {
      return byPn.size() == 1 ? Optional.of(byPn.get(0)) : Optional.empty();
    }
    if (loan.serial() == null || loan.motor() == null) {
      return Optional.empty();
    }
    List<RenewalCandidate> vehicles =
        byPn.stream().filter(c -> sameVehicle(c, loan.serial(), loan.motor())).toList();
    return vehicles.size() == 1 ? Optional.of(vehicles.get(0)) : Optional.empty();
  }

  private boolean sameVehicle(RenewalCandidate c, String serial, String motor) {
    if (c.getExpiringArn() == null) {
      return false;
    }
    Long count =
        jdbc.queryForObject(
            VEHICLE,
            Map.of("arn", c.getExpiringArn(), "serial", serial, "motor", motor),
            Long.class);
    return count != null && count > 0;
  }

  private Map<String, Object> args(Long companyId, String jobNo, Loan loan) {
    Map<String, Object> a = new HashMap<>();
    a.put("companyId", companyId);
    a.put("jobNo", jobNo);
    a.put("pn", loan.pn().strip());
    a.put("serial", loan.serial());
    a.put("motor", loan.motor());
    a.put("tag", loan.ibgCbgTag());
    a.put("productType", loan.productType());
    a.put("email", loan.email());
    a.put("address", loan.mailingAddress());
    a.put("at", Timestamp.from(clock.instant()));
    a.put("by", currentUser.optionalUsername().orElse("SYSTEM"));
    return a;
  }

  /**
   * A loan of a LAMD list.
   *
   * @param pn PN number
   * @param ibgCbgTag IBG / CBG tag
   * @param bookDate book date
   * @param maturityDate maturity date
   * @param branch logical branch description
   * @param productType product description
   * @param aoCode Account Officer code of the loan
   * @param aoName Account Officer name
   * @param email e-mail address
   * @param mailingAddress mailing address
   * @param serial serial number (motor)
   * @param motor motor number (motor)
   * @param shortName short name of the borrower
   */
  public record Loan(
      String pn,
      String ibgCbgTag,
      LocalDate bookDate,
      LocalDate maturityDate,
      String branch,
      String productType,
      String aoCode,
      String aoName,
      String email,
      String mailingAddress,
      String serial,
      String motor,
      String shortName) {}
}
