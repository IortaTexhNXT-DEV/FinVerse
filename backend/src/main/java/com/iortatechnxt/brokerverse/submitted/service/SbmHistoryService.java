package com.iortatechnxt.brokerverse.submitted.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyHistory;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyHistory.Change;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyHistoryRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The field history of the masterlist records (BRIDSP-29): the fields a user sees, written as the
 * user reads them (labels of the lists, names of the users, dd-MMM-yyyy dates, formatted amounts),
 * compared before and after a change; each difference is one history row with its source.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SbmHistoryService {

  private final SbmPolicyHistoryRepository history;
  private final LovService lovs;
  private final UserDirectory users;

  /**
   * Creates the service.
   *
   * @param history history rows
   * @param lovs lists of values (labels)
   * @param users user directory (names)
   */
  public SbmHistoryService(
      SbmPolicyHistoryRepository history, LovService lovs, UserDirectory users) {
    this.history = history;
    this.lovs = lovs;
    this.users = users;
  }

  /**
   * The values of the tracked fields of a record, by label.
   *
   * @param p record
   * @return values
   */
  public Map<String, String> snapshot(SbmPolicy p) {
    Map<String, String> v = new LinkedHashMap<>();
    v.put("Segment", lovs.label(SubmittedCodes.LOV_SEGMENT, p.getSegment()));
    v.put("Business type", DisplayFormat.value(p.getBusinessType()));
    loan(v, p.getLoan());
    v.put("Assured", p.getAssured().assuredName());
    v.put("Mailing address", p.getAssured().mailingAddress());
    v.put("Telephone", p.getAssured().telephone());
    v.put("Mobile", p.getAssured().mobile());
    v.put("E-mail", p.getAssured().email());
    v.put("Bank counterpart e-mail", p.getAssured().bankCounterpartEmail());
    terms(v, p.getTerms());
    risk(v, p.getRisk());
    v.put("Free First Year", yes(p.getMarks().ffy()));
    v.put("Employee account", yes(p.getMarks().employeeAccount()));
    v.put("No Touch", yes(p.getMarks().noTouch()));
    outcome(v, p);
    v.put("Handler", name(p.getHandlerUsername()));
    v.put("Account officer", name(p.getAoUsername()));
    v.put("Conversion status", lovs.label(SubmittedCodes.LOV_CONVERSION, p.getConversionStatus()));
    v.put("Opportunity", p.getOpportunityTag());
    v.put("Remarks", p.getRemarks());
    v.put("Status", DisplayFormat.words(p.getStatus()));
    v.put("Renewal account", p.getRenewalArn());
    v.put("Booked invoice", p.getBookedInvoiceNo());
    v.put("Booked on", DisplayFormat.date(p.getBookedOn()));
    return v;
  }

  private static void loan(Map<String, String> v, SbmLoan l) {
    v.put("PN", l.pnNo());
    v.put("Loan application", l.loanApplicationNo());
    v.put("CIF", l.cif());
    v.put("Value date", DisplayFormat.date(l.valueDate()));
    v.put("Maturity date", DisplayFormat.date(l.maturityDate()));
    v.put("Referring branch", l.referringBranch());
    v.put("Originating unit", l.originatingUnit());
    v.put("Borrower", l.borrowerName());
  }

  private static void terms(Map<String, String> v, SbmTerms t) {
    v.put("Insurer", t.insurerCode());
    v.put("Policy number", t.policyNo());
    v.put("Inception", DisplayFormat.date(t.inceptionDate()));
    v.put("Expiry", DisplayFormat.date(t.expiryDate()));
    v.put("Sum insured", DisplayFormat.amount(t.sumInsured()));
    v.put("Total premium", DisplayFormat.amount(t.totalPremium()));
  }

  private static void risk(Map<String, String> v, SbmRisk r) {
    v.put("Unit", r.unitDescription());
    v.put("Serial number", r.serialNo());
    v.put("Motor number", r.motorNo());
    v.put("Plate number", r.plateNo());
    v.put("Vehicle type", r.vehicleType());
    v.put("Year model", r.vehicleYear() == null ? null : r.vehicleYear().toString());
    v.put("Property location", r.propertyLocation());
    v.put("Occupancy", r.occupancy());
    v.put("Mortgagee", r.mortgagee());
  }

  private void outcome(Map<String, String> v, SbmPolicy p) {
    v.put("Loan status", lovs.label(SubmittedCodes.LOV_LOAN_STATUS, p.getLoanStatus()));
    v.put("Classification", DisplayFormat.words(p.getClassification()));
    v.put("Bucket", lovs.label(SubmittedCodes.LOV_BUCKET, p.getBucket()));
    v.put("Renewal tag", DisplayFormat.words(p.getRenewalTag()));
    v.put("RA template", DisplayFormat.words(p.getRaTemplate()));
    v.put("Insurer approval required", yes(p.isInsurerApprovalRequired()));
  }

  /**
   * Writes the differences between an earlier snapshot and the record now.
   *
   * @param p record
   * @param before snapshot before the change (empty for a new record)
   * @param source what changed it
   * @param reference run, intake, extraction or document reference, may be null
   * @return number of fields changed
   */
  public int record(
      SbmPolicy p, Map<String, String> before, SbmHistorySource source, String reference) {
    int changed = 0;
    for (Map.Entry<String, String> e : snapshot(p).entrySet()) {
      String old = blankToNull(before.get(e.getKey()));
      String now = blankToNull(e.getValue());
      if (!Objects.equals(old, now)) {
        history.save(
            new SbmPolicyHistory(p.getId(), e.getKey(), new Change(old, now), source, reference));
        changed++;
      }
    }
    return changed;
  }

  /**
   * Writes one event of a record that is not a field change (hand-off, letter, TOR...).
   *
   * @param p record
   * @param field what happened
   * @param value detail
   * @param source source
   * @param reference reference, may be null
   */
  public void note(
      SbmPolicy p, String field, String value, SbmHistorySource source, String reference) {
    history.save(
        new SbmPolicyHistory(p.getId(), field, new Change(null, value), source, reference));
  }

  private String name(String username) {
    return username == null ? null : users.displayName(username);
  }

  private static String yes(boolean value) {
    return value ? "Yes" : "No";
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
