package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountStatusChanged;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceDocument;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceSubject;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceTrigger;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdviceRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Insurance Advice (BRNB.060/070/095): generated from the INSURANCE_ADVICE template for mortgaged
 * accounts only (mortgagee bank set), single or several at a time, and automatically on the event
 * chosen by the parameter IA_TRIGGER (policy issue by default, or placement; MANUAL turns it off).
 * The register lists, shows and downloads them; sending is done by {@link AdviceDispatchService}.
 */
@Service
@Transactional
public class InsuranceAdviceService {

  /** Audit entity type. */
  public static final String ENTITY = "InsuranceAdvice";

  /** Parameter: automatic generation event. */
  public static final String TRIGGER_PARAMETER = "IA_TRIGGER";

  private static final String TEMPLATE = "INSURANCE_ADVICE";
  private static final String MORTGAGEE_LOV = "MORTGAGEE_BANK";
  private static final String TO_FOLLOW = "to follow";
  private static final Set<AccountStatus> ADVISABLE =
      EnumSet.of(AccountStatus.PLACED, AccountStatus.POLICY_ISSUED, AccountStatus.BOOKED);

  private final InsuranceAdviceRepository advices;
  private final AccountQueryService accounts;
  private final InsurerService insurers;
  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param advices insurance advices
   * @param accounts account reads
   * @param insurers insurer panel
   * @param templates document templates
   * @param composer PDF rendering
   * @param numbers document numbers
   * @param lovs lists of values
   * @param parameters business parameters
   * @param audit audit trail
   * @param clock clock
   */
  public InsuranceAdviceService(
      InsuranceAdviceRepository advices,
      AccountQueryService accounts,
      InsurerService insurers,
      DocTemplateService templates,
      DocumentComposer composer,
      DocumentNumberService numbers,
      LovService lovs,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.advices = advices;
    this.accounts = accounts;
    this.insurers = insurers;
    this.templates = templates;
    this.composer = composer;
    this.numbers = numbers;
    this.lovs = lovs;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The automatic generation event configured.
   *
   * @return event (MANUAL = none)
   */
  @Transactional(readOnly = true)
  public AdviceTrigger configuredTrigger() {
    String value = parameters.text(TRIGGER_PARAMETER, AdviceTrigger.ON_POLICY_ISSUE.name());
    try {
      return AdviceTrigger.valueOf(value.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return AdviceTrigger.ON_POLICY_ISSUE;
    }
  }

  /**
   * Generates the Insurance Advice of a mortgaged account.
   *
   * @param arn account placed, issued or booked
   * @param trigger what generates it
   * @return the advice
   */
  public InsuranceAdvice generate(String arn, AdviceTrigger trigger) {
    Account account = accounts.requireByArn(arn);
    if (account.getMortgageeBank() == null || account.getMortgageeBank().isBlank()) {
      throw new BusinessRuleException(
          "IA_NOT_MORTGAGED", "Account " + arn + " has no mortgagee bank: no Insurance Advice");
    }
    if (!ADVISABLE.contains(account.getStatus())) {
      throw new BusinessRuleException(
          "IA_ACCOUNT_STATUS",
          "Account " + arn + " is " + account.getStatus() + "; it must be placed first");
    }
    LocalDate today = LocalDate.now(clock);
    String iaNo = numbers.next("IA-" + today.getYear());
    String mortgagee = lovs.label(MORTGAGEE_LOV, account.getMortgageeBank());
    String insurerName = insurerName(account);
    String policies =
        account.getPolicyNumbers().isEmpty() ? null : String.join(", ", account.getPolicyNumbers());
    Map<String, Object> values = new HashMap<>();
    values.put("mortgagee", mortgagee);
    values.put("insurerName", insurerName);
    values.put("policyNo", policies == null ? TO_FOLLOW : policies);
    values.put("periodFrom", account.getPeriodFrom());
    values.put("periodTo", account.getPeriodTo());
    MergedText text = templates.merge(TEMPLATE, today, values);
    byte[] pdf =
        composer.pdf(spec(iaNo, account, text, new Parties(mortgagee, insurerName, policies)));
    InsuranceAdvice saved =
        advices.save(
            new InsuranceAdvice(
                account.getCompanyId(),
                iaNo,
                new AdviceSubject(
                    account.getId(),
                    arn,
                    account.getClientId(),
                    account.getClientName(),
                    account.getMortgageeBank(),
                    account.getInsurerCode(),
                    policies),
                new AdviceDocument(trigger, text.versionTag(), iaNo + ".pdf", sha256(pdf), pdf)));
    audit.record(
        ENTITY, iaNo, AuditAction.CREATE, "Insurance Advice for " + arn + " (" + trigger + ")");
    return saved;
  }

  private String insurerName(Account account) {
    if (account.getInsurerCode() == null) {
      return "the insurer";
    }
    try {
      InsurerProfile insurer =
          insurers.requireInsurer(account.getCompanyId(), account.getInsurerCode());
      return insurer.getName();
    } catch (ResourceNotFoundException e) {
      return account.getInsurerCode();
    }
  }

  private static DocumentSpec spec(String iaNo, Account account, MergedText text, Parties parties) {
    List<Field> facts =
        List.of(
            new Field("Account Reference Number", account.getArn()),
            new Field("Insured", account.getClientName()),
            new Field("Mortgagee", parties.mortgagee()),
            new Field("Loan application no.", account.getLoanApplicationNo()),
            new Field("Insurer", parties.insurerName()),
            new Field("Policy no.", parties.policies() == null ? TO_FOLLOW : parties.policies()),
            new Field("Period", account.getPeriodFrom() + " to " + account.getPeriodTo()),
            new Field(
                "Sum insured",
                account.getCurrency() + " " + account.getTotalSumInsured().toPlainString()));
    List<List<String>> items =
        account.getItems().stream()
            .map(i -> List.of(String.valueOf(i.getItemNo()), i.label(), amount(i)))
            .toList();
    return new DocumentSpec(
        "BDO Insurance and Reinsurance Brokers, Inc.",
        text.title(),
        iaNo,
        List.of(
            new Text(null, text.text()),
            new Fields("Insurance", facts),
            new Table("Insured items", List.of("No.", "Item", "Sum insured"), items, List.of(2))),
        List.of("Authorized signatory"),
        text.versionTag());
  }

  private static String amount(RiskItem item) {
    return item.getSumInsured() == null ? "" : item.getSumInsured().toPlainString();
  }

  /**
   * Generates the advice automatically on the configured event (BRNB.095): the placement (PLACED)
   * or the policy issue (POLICY_ISSUED) of a mortgaged account without an advice yet. It runs just
   * before the business transaction commits, once the policy numbers are recorded on the account.
   *
   * @param event account status change
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void on(AccountStatusChanged event) {
    AdviceTrigger configured = configuredTrigger();
    boolean placed = configured == AdviceTrigger.ON_PLACEMENT && event.to() == AccountStatus.PLACED;
    boolean issued =
        configured == AdviceTrigger.ON_POLICY_ISSUE && event.to() == AccountStatus.POLICY_ISSUED;
    if (!placed && !issued) {
      return;
    }
    Account account = accounts.get(event.accountId());
    if (account.getMortgageeBank() != null && !advices.existsByAccountId(account.getId())) {
      generate(account.getArn(), configured);
    }
  }

  /**
   * The register: advices of a company, newest first (BRNB.060).
   *
   * @param companyId company
   * @param text IA number, ARN or client fragment
   * @param pageable page
   * @return advices
   */
  @Transactional(readOnly = true)
  public Page<InsuranceAdvice> register(Long companyId, String text, Pageable pageable) {
    String like = "%" + (text == null ? "" : text.strip().toLowerCase(Locale.ROOT)) + "%";
    return advices.search(companyId, like, pageable);
  }

  /**
   * Advices of an account, newest first.
   *
   * @param arn Account Reference Number
   * @return advices
   */
  @Transactional(readOnly = true)
  public List<InsuranceAdvice> ofAccount(String arn) {
    return advices.findByArnOrderByIdDesc(arn);
  }

  /**
   * One advice.
   *
   * @param id id
   * @return advice
   */
  @Transactional(readOnly = true)
  public InsuranceAdvice get(Long id) {
    return advices.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Downloads an advice; the download is audited.
   *
   * @param id advice
   * @return advice with its PDF
   */
  public InsuranceAdvice download(Long id) {
    InsuranceAdvice advice = get(id);
    audit.record(
        ENTITY, advice.getIaNo(), AuditAction.EXPORT, "Downloaded " + advice.getFileName());
    return advice;
  }

  static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private record Parties(String mortgagee, String insurerName, String policies) {}
}
