package com.iortatechnxt.finverse.reinsurance;

import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest.LayerRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest.ParticipantRequest;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import com.iortatechnxt.finverse.reinsurance.service.TreatyService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.TestData;
import com.iortatechnxt.finverse.support.TestParties;
import com.iortatechnxt.finverse.underwriting.UwFixtures;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.service.EndorsementService;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Builds reinsurance test data in the demo company. Every test class works in its own underwriting
 * year (policies whose period starts in that year are approved in the open 2026 periods), so its
 * treaty programme never collides with the demo programme (2026) or another test class.
 */
@Component
public class RiFixtures {

  /** Accounting date of the test policies (open period). */
  public static final LocalDate APPROVAL = LocalDate.of(2026, 3, 10);

  /** Reinsurance officer (maker). */
  public static final String MAKER = "reinsurer";

  /** Finance manager (checker). */
  public static final String CHECKER = "fmanager";

  private final UwFixtures uw;
  private final TreatyService treaties;
  private final PolicyApprovalService approvals;
  private final EndorsementService endorsements;
  private final AsUser as;
  private final TestData data;
  private final JdbcTemplate jdbc;
  private final TestParties parties;

  RiFixtures(
      UwFixtures uw,
      TreatyService treaties,
      PolicyApprovalService approvals,
      EndorsementService endorsements,
      AsUser as,
      TestData data,
      JdbcTemplate jdbc,
      TestParties parties) {
    this.uw = uw;
    this.treaties = treaties;
    this.approvals = approvals;
    this.endorsements = endorsements;
    this.as = as;
    this.data = data;
    this.jdbc = jdbc;
    this.parties = parties;
  }

  public Long companyId() {
    return data.company().getId();
  }

  public Product product(String lob) {
    return uw.product(lob, false);
  }

  /** Approved direct policy (100 % share, no discount) starting in the given year. */
  public Policy policy(Product product, int year, String currency, List<RiskRequest> risks) {
    LocalDate from = LocalDate.of(year, 1, 1);
    PolicyRequest r =
        new PolicyRequest(
            companyId(),
            data.branch("HO").getId(),
            product.getId(),
            "C-000201",
            "Luzon Steel Manufacturing Corp.",
            SourceType.DIRECT,
            null,
            APPROVAL,
            from,
            from.plusYears(1).minusDays(1),
            currency,
            BusinessType.DIRECT,
            new BigDecimal("100"),
            null,
            false,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            risks);
    return uw.issue(r, APPROVAL);
  }

  public RiskRequest risk(String si, String premium) {
    return uw.risk(si, premium, "NCR-MAKATI");
  }

  /** Approved endorsement effective on a date of the policy period, accounted on another. */
  public Endorsement endorse(
      Policy policy,
      EndorsementType type,
      String gross,
      String si,
      LocalDate effective,
      LocalDate date) {
    EndorsementRequest r =
        new EndorsementRequest(
            type,
            date,
            effective,
            "Test " + type,
            gross == null ? null : new BigDecimal(gross),
            si == null ? null : new BigDecimal(si),
            null,
            null);
    Endorsement draft = as.run("uw", () -> endorsements.create(policy.getId(), r));
    as.run("uw", () -> endorsements.submit(draft.getId()));
    return as.run(CHECKER, () -> approvals.approveEndorsement(draft.getId(), date));
  }

  /** Created by the reinsurance officer and authorized by the finance manager. */
  public Treaty authorized(TreatyRequest r) {
    Treaty draft = as.run(MAKER, () -> treaties.create(r));
    return as.run(CHECKER, () -> treaties.authorize(draft.getId()));
  }

  /** Fresh reinsurers (a lead, a follower and a facultative market) with no other open items. */
  public Reinsurers reinsurers() {
    return new Reinsurers(
        parties.create(PartyType.REINSURER).getCode(),
        parties.create(PartyType.REINSURER).getCode(),
        parties.create(PartyType.REINSURER).getCode());
  }

  public TreatyRequest quotaShare(
      Reinsurers r, String code, String lob, int year, String pct, String limit) {
    return request(
        r, code, TreatyType.QUOTA_SHARE, lob, year, new Terms(pct, limit, null, null), List.of());
  }

  public TreatyRequest surplus(
      Reinsurers r, String code, String lob, int year, String retention, int lines) {
    return request(
        r, code, TreatyType.SURPLUS, lob, year, new Terms(null, null, retention, lines), List.of());
  }

  public TreatyRequest excessOfLoss(
      Reinsurers r, String code, String lob, int year, String priority, String limit) {
    return request(
        r,
        code,
        TreatyType.XOL,
        lob,
        year,
        new Terms(null, null, null, null),
        List.of(
            new LayerRequest(
                new BigDecimal(priority), new BigDecimal(limit), new BigDecimal("400000"), 0)));
  }

  private TreatyRequest request(
      Reinsurers r,
      String code,
      TreatyType type,
      String lob,
      int year,
      Terms t,
      List<LayerRequest> layers) {
    return new TreatyRequest(
        companyId(),
        code,
        "Test " + type + " " + code,
        type,
        lob,
        year,
        LocalDate.of(year, 1, 1),
        LocalDate.of(year, 12, 31),
        "PHP",
        t.pct() == null ? null : new BigDecimal(t.pct()),
        t.limit() == null ? null : new BigDecimal(t.limit()),
        t.retention() == null ? null : new BigDecimal(t.retention()),
        t.lines(),
        new BigDecimal("1"),
        new BigDecimal("4"),
        type == TreatyType.QUOTA_SHARE ? new BigDecimal("50") : BigDecimal.ZERO,
        null,
        List.of(
            new ParticipantRequest(
                r.lead(),
                new BigDecimal("60"),
                new BigDecimal("30"),
                BigDecimal.ZERO,
                BigDecimal.ZERO),
            new ParticipantRequest(
                r.follow(),
                new BigDecimal("40"),
                new BigDecimal("25"),
                BigDecimal.ZERO,
                new BigDecimal("20"))),
        layers);
  }

  /** Signed (debit - credit) base amount posted to an account for a source reference prefix. */
  public BigDecimal posted(String account, String referencePrefix) {
    BigDecimal value =
        jdbc.queryForObject(
            """
            select coalesce(sum(e.debit_base - e.credit_base), 0) from gl_ledger_entry e
            join coa_account a on a.id = e.account_id
            join jnl_batch b on b.id = e.batch_id
            where a.code = ? and a.company_id = ? and b.source_reference like ?
            """,
            BigDecimal.class,
            account,
            companyId(),
            referencePrefix + "%");
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }

  /**
   * Fresh reinsurer codes of a test.
   *
   * @param lead 60 % treaty participant (30 % commission, no premium reserve)
   * @param follow 40 % treaty participant (25 % commission, 20 % premium reserve)
   * @param fac facultative market
   */
  public record Reinsurers(String lead, String follow, String fac) {}

  /** Numeric treaty terms of a test treaty. */
  private record Terms(String pct, String limit, String retention, Integer lines) {}
}
