package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.submitted.SubmittedHandOffRecord;
import com.iortatechnxt.brokerverse.renewal.submitted.SubmittedHandOffRecordRepository;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The content of the renewal letters (FR-RN-080, 082, 083): the template of the letter type and
 * notice merged with the renewal's values, and its PDF with the policy details. The renewal terms
 * come from the renewal account when it exists, else from the expiring invoice.
 */
@Component
public class LetterContent {

  private static final String EMPTY = "";
  private static final CandidateSnapshot.SnapshotPremium NO_PREMIUM =
      new CandidateSnapshot.SnapshotPremium(null, null, null, null, null, null);

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organization;
  private final AccountRepository accounts;
  private final InsurerProfileRepository insurers;
  private final AppUserRepository users;
  private final LovService lovs;
  private final SubmittedHandOffRecordRepository handOffs;

  /**
   * Creates the content builder.
   *
   * @param templates document templates
   * @param composer PDF writer
   * @param organization companies
   * @param accounts renewal accounts
   * @param insurers insurers
   * @param users user names
   * @param lovs labels
   * @param handOffs terms of the submitted policies handed over (FFY template)
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LetterContent(
      DocTemplateService templates,
      DocumentComposer composer,
      OrganizationService organization,
      AccountRepository accounts,
      InsurerProfileRepository insurers,
      AppUserRepository users,
      LovService lovs,
      SubmittedHandOffRecordRepository handOffs) {
    this.templates = templates;
    this.composer = composer;
    this.organization = organization;
    this.accounts = accounts;
    this.insurers = insurers;
    this.users = users;
    this.lovs = lovs;
    this.handOffs = handOffs;
  }

  /**
   * The template of a letter.
   *
   * @param type letter type
   * @param notice RA notice
   * @return template code
   */
  public static String template(LetterType type, RaNotice notice) {
    return switch (type) {
      case RA ->
          notice == RaNotice.SECOND
              ? RenewalCodes.TEMPLATE_RA_SECOND
              : RenewalCodes.TEMPLATE_RA_FIRST;
      case NAL -> RenewalCodes.TEMPLATE_NAL;
      case NFR -> RenewalCodes.TEMPLATE_NFR;
      case NRNS_REMINDER -> RenewalCodes.TEMPLATE_NRNS;
      case NON_ACCEPTANCE -> RenewalCodes.TEMPLATE_NON_ACCEPTANCE;
    };
  }

  /**
   * The template of a letter of a renewal: the first RA of a Free First Year submitted policy is
   * the FFY variant (wave R3).
   *
   * @param c renewal
   * @param type letter type
   * @param notice RA notice
   * @return template code
   */
  public String templateOf(RenewalCandidate c, LetterType type, RaNotice notice) {
    boolean freeFirstYear =
        type == LetterType.RA
            && notice != RaNotice.SECOND
            && c.getSource() == CandidateSource.SUBMITTED_POLICY
            && c.getId() != null
            && handOffs
                .findByCandidateId(c.getId())
                .map(SubmittedHandOffRecord::isFreeFirstYear)
                .orElse(false);
    return freeFirstYear ? RenewalCodes.TEMPLATE_RA_FFY : template(type, notice);
  }

  /**
   * A letter merged and rendered.
   *
   * @param c renewal
   * @param type letter type
   * @param notice RA notice
   * @param letterNo letter number
   * @param today business date
   * @return subject, body and PDF
   */
  public Rendered render(
      RenewalCandidate c, LetterType type, RaNotice notice, String letterNo, LocalDate today) {
    Map<String, Object> values = values(c, today);
    MergedText text = templates.merge(templateOf(c, type, notice), today, values);
    String subject = DocTemplateService.fill(text.title(), values);
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                organization.getCompany(c.getCompanyId()).getName(),
                subject,
                letterNo,
                List.of(new Text(null, text.text()), new Fields("Policy", fields(values))),
                List.of("Renewal Department"),
                text.versionTag()));
    return new Rendered(text.code(), text.versionNo(), subject, text.text(), pdf);
  }

  /**
   * The values of the placeholders.
   *
   * @param c renewal
   * @param today business date
   * @return values by placeholder
   */
  public Map<String, Object> values(RenewalCandidate c, LocalDate today) {
    CandidateSnapshot s = c.getSnapshot();
    Map<String, Object> v = new HashMap<>();
    v.put("clientName", s.clientName());
    v.put("assuredName", assured(s));
    v.put("policyNo", or(s.policyNo(), c.getExpiringArn()));
    v.put("insurerName", insurer(c));
    v.put("expiryDate", c.getExpiryDate());
    v.put("productName", product(s));
    v.put("renewalRef", c.getRenewalRef());
    v.put("aoName", name(c.getAssignedAo() != null ? c.getAssignedAo() : ao(s)));
    v.put("reason", reason(c));
    v.put("expiringArn", or(c.getExpiringArn(), c.getSourceRef()));
    v.put("asOf", today);
    v.putAll(terms(c));
    return v;
  }

  private Map<String, Object> terms(RenewalCandidate c) {
    CandidateSnapshot.SnapshotPremium expiring =
        c.getSnapshot().premium() == null ? NO_PREMIUM : c.getSnapshot().premium();
    Optional<Account> renewal =
        c.getRenewalArn() == null ? Optional.empty() : accounts.findByArn(c.getRenewalArn());
    Map<String, Object> v = new HashMap<>();
    v.put("currency", or(expiring.currency(), "PHP"));
    v.put(
        "renewalFrom",
        renewal
            .map(Account::getPeriodFrom)
            .map(Object::toString)
            .orElse(c.getExpiryDate().toString()));
    v.put("renewalTo", renewal.map(Account::getPeriodTo).map(Object::toString).orElse(EMPTY));
    v.put(
        "sumInsured",
        amount(renewal.map(Account::getTotalSumInsured).orElse(expiring.totalSumInsured())));
    v.put(
        "grossPremium",
        amount(renewal.map(a -> a.getPremium().grossPremium()).orElse(expiring.grossPremium())));
    return v;
  }

  private static String product(CandidateSnapshot s) {
    return s.product() == null ? EMPTY : or(s.product().productName(), s.product().productCode());
  }

  private List<Field> fields(Map<String, Object> v) {
    return List.of(
        new Field("Renewal reference", str(v.get("renewalRef"))),
        new Field("Policy number", str(v.get("policyNo"))),
        new Field("Insurer", str(v.get("insurerName"))),
        new Field("Expiry date", str(v.get("expiryDate"))),
        new Field("Sum insured", str(v.get("currency")) + " " + str(v.get("sumInsured"))),
        new Field("Premium", str(v.get("currency")) + " " + str(v.get("grossPremium"))));
  }

  private String insurer(RenewalCandidate c) {
    String code = c.getSnapshot().insurerCode();
    return code == null
        ? EMPTY
        : insurers
            .findByCompanyIdAndPartyCode(c.getCompanyId(), code)
            .map(InsurerProfile::getName)
            .orElse(code);
  }

  private String reason(RenewalCandidate c) {
    String code = c.getDisposition().reasonCode();
    return code == null ? EMPTY : lovs.label(RenewalCodes.LOV_NONRENEWAL_REASON, code);
  }

  private String name(String username) {
    return username == null
        ? EMPTY
        : users.findByUsernameIgnoreCase(username).map(AppUser::getFullName).orElse(username);
  }

  private static String ao(CandidateSnapshot s) {
    return s.sales() == null ? null : s.sales().accountOfficer();
  }

  private static String assured(CandidateSnapshot s) {
    return s.client() == null || s.client().assuredName() == null
        ? s.clientName()
        : s.client().assuredName();
  }

  private static String amount(BigDecimal value) {
    return value == null ? EMPTY : String.format(java.util.Locale.ROOT, "%,.2f", value);
  }

  private static String or(String a, String b) {
    return a != null ? a : b;
  }

  private static String str(Object value) {
    return value == null ? EMPTY : value.toString();
  }

  /**
   * A rendered letter.
   *
   * @param templateCode template
   * @param templateVersion template version
   * @param subject subject (e-mail and PDF title)
   * @param body merged text (e-mail body)
   * @param pdf PDF
   */
  public record Rendered(
      String templateCode, int templateVersion, String subject, String body, byte[] pdf) {}
}
