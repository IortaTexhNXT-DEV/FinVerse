package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Result;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates of a renewal account from its expiring account: Refresh Endorsements (FRRN.004.06)
 * re-reads the mother policy and its endorsements, refreshes the renewal account and re-runs the
 * checks; the CBG Motor automatic values (FRRN.009.01) next to the expiring values, the renewal
 * account's values and the differences, for the Computations tab.
 */
@Service
@Transactional
public class RenewalUpdatesService {

  private final RenewalRecords records;
  private final ReevaluationService reevaluation;
  private final CandidateEndorsementRepository endorsements;
  private final CbgMotorAutoUpdate autoUpdate;
  private final AccountQueryService accountQueries;
  private final AccountRepository accounts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param reevaluation checks
   * @param endorsements endorsements linked
   * @param autoUpdate CBG Motor automatic values
   * @param accountQueries expiring account (items)
   * @param accounts renewal account
   * @param audit audit trail
   */
  public RenewalUpdatesService(
      RenewalRecords records,
      ReevaluationService reevaluation,
      CandidateEndorsementRepository endorsements,
      CbgMotorAutoUpdate autoUpdate,
      AccountQueryService accountQueries,
      AccountRepository accounts,
      AuditTrailService audit) {
    this.records = records;
    this.reevaluation = reevaluation;
    this.endorsements = endorsements;
    this.autoUpdate = autoUpdate;
    this.accountQueries = accountQueries;
    this.accounts = accounts;
    this.audit = audit;
  }

  /**
   * Re-reads the endorsements of the mother policy, refreshes the renewal account and re-runs its
   * checks; an endorsement in progress then blocks posting, the Renewal Advice and the acceptance.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return the endorsements linked to the renewal account
   */
  public List<EndorsementRow> refreshEndorsements(Long companyId, String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    reevaluation.reevaluate(c, CheckTrigger.EVENT);
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.UPDATE, "Endorsements refreshed");
    return endorsements.findByCandidateIdOrderByIdAsc(c.getId()).stream()
        .map(
            e ->
                new EndorsementRow(
                    e.getReference(), e.getSource(), e.getStatusAtLink(), e.getEffectiveDate()))
        .toList();
  }

  /**
   * The automatic values of a CBG Motor renewal: one row per field with the expiring value, the
   * automatic renewal value, the renewal account's value and the difference.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return rows, empty when the automatic values do not apply
   */
  @Transactional(readOnly = true)
  public List<ValueRow> autoUpdate(Long companyId, String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    List<RiskItemData> items =
        c.getExpiringArn() == null ? List.of() : accountQueries.draftOf(c.getExpiringArn()).items();
    Optional<Result> values = autoUpdate.values(c, items);
    if (values.isEmpty()) {
      return List.of();
    }
    Result r = values.get();
    CandidateSnapshot.SnapshotPremium p = c.getSnapshot().premium();
    Optional<Account> account =
        c.getRenewalArn() == null ? Optional.empty() : accounts.findByArn(c.getRenewalArn());
    Optional<AccountPremium> ap = account.map(Account::getPremium);
    Optional<RiskItemData> item = items.stream().findFirst();
    Optional<CandidateSnapshot.SnapshotPremium> sp = Optional.ofNullable(p);
    List<ValueRow> rows = new ArrayList<>();
    rows.add(text("Risk Code", c.getSnapshot().product().productCode(), r.riskCode()));
    rows.add(amount("Premium Rate", value(item, RiskItemData::rate), r.premiumRate(), null));
    rows.add(date("New Inception", c.getSnapshot().inceptionDate(), r.inception()));
    rows.add(date("New Expiry", c.getExpiryDate(), r.expiry()));
    rows.add(
        amount(
            "OD / Theft Coverage",
            value(item, RiskItemData::sumInsured),
            r.odTheft(),
            value(account, Account::getTotalSumInsured)));
    rows.add(amount("BI Coverage", value(item, RiskItemData::biLimit), r.bi(), null));
    rows.add(amount("PD Coverage", value(item, RiskItemData::pdLimit), r.pd(), null));
    rows.add(amount("OD / Theft Premium", null, r.odTheftPremium(), null));
    rows.add(amount("BI Premium", null, r.biPremium(), null));
    rows.add(amount("PD Premium", null, r.pdPremium(), null));
    rows.add(amount("Auto PA Premium", null, r.autoPaPremium(), null));
    rows.add(
        amount(
            "Basic Premium",
            value(sp, CandidateSnapshot.SnapshotPremium::basicPremium),
            r.basic(),
            value(ap, AccountPremium::netPremium)));
    rows.add(amount("DST", null, r.dst(), value(ap, AccountPremium::dst)));
    rows.add(amount("VAT", null, r.vat(), value(ap, AccountPremium::vat)));
    rows.add(amount("LGT", null, r.lgt(), value(ap, AccountPremium::lgt)));
    rows.add(
        amount(
            "Total Premium",
            value(sp, CandidateSnapshot.SnapshotPremium::grossPremium),
            r.total(),
            value(ap, AccountPremium::grossPremium)));
    return rows;
  }

  private static <T> BigDecimal value(Optional<T> source, Function<T, BigDecimal> field) {
    return source.map(field).orElse(null);
  }

  private static String plain(BigDecimal v) {
    return v == null ? null : v.toPlainString();
  }

  private static ValueRow text(String field, String expiring, String renewal) {
    return new ValueRow(field, expiring, renewal, null, null);
  }

  private static ValueRow date(String field, LocalDate expiring, LocalDate renewal) {
    return new ValueRow(
        field,
        expiring == null ? null : expiring.toString(),
        renewal == null ? null : renewal.toString(),
        null,
        null);
  }

  private static ValueRow amount(
      String field, BigDecimal expiring, BigDecimal renewal, BigDecimal account) {
    BigDecimal difference = expiring == null || renewal == null ? null : renewal.subtract(expiring);
    return new ValueRow(field, plain(expiring), plain(renewal), plain(account), difference);
  }

  /**
   * An endorsement linked to a renewal account.
   *
   * @param reference endorsement or invoice reference
   * @param source BOOKING or ADJUSTMENT
   * @param status status when linked
   * @param effectiveDate effective date
   */
  public record EndorsementRow(
      String reference, String source, String status, LocalDate effectiveDate) {}

  /**
   * A field of the automatic renewal values.
   *
   * @param field field
   * @param expiring expiring value
   * @param renewal automatic renewal value
   * @param account value of the renewal account, when created
   * @param difference renewal value minus expiring value (the financial impact)
   */
  public record ValueRow(
      String field, String expiring, String renewal, String account, BigDecimal difference) {}
}
