package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetter;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRuleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatchRepository;
import com.iortatechnxt.brokerverse.submitted.service.SbmHistoryService;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.submitted.service.adapter.SubmittedFileStorage;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway.PrintHandOver;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway.PrintRequest;
import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway.PrintedLetter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The letters of Submitted Policies that are not renewal letters (BRIDSP-22; FRS FR-SP-062; job
 * {@code SBM_LETTER_DISPATCH}): for each active letter rule, the records of its segment, bucket and
 * status whose letter day (expiry plus the rule's days) has come within the catch-up window and
 * that have no letter of the rule yet get the letter from its template - sent by e-mail to the
 * client or the bank counterpart, or generated for the day's print batch for the mail house. A
 * letter that cannot be sent is FAILED with alert {@code SBM_LETTER_FAILED} and can be sent again.
 */
@Service("sbmLetterService")
@Transactional
public class LetterService {

  /** Days a missed letter day is still caught up. */
  public static final int CATCH_UP_DAYS = 7;

  private static final String PRINT = "PRINT";
  private static final String EMAIL = "EMAIL";

  private final SbmLetterRuleRepository rules;
  private final SbmLetterRepository letters;
  private final SbmPolicyRepository policies;
  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final StoredFileService files;
  private final MessageService messages;
  private final MailHouseGateway mailHouse;
  private final SbmPrintBatchRepository printBatches;
  private final OrganizationDirectory organizations;
  private final SbmHistoryService history;
  private final AlertService alerts;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules letter rules
   * @param letters letters
   * @param policies masterlist
   * @param templates document templates
   * @param composer document composer
   * @param files file store
   * @param messages outbound e-mail
   * @param mailHouse mail house
   * @param printBatches print batches
   * @param organizations company names
   * @param history record history
   * @param alerts alerts
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the letters
  public LetterService(
      SbmLetterRuleRepository rules,
      SbmLetterRepository letters,
      SbmPolicyRepository policies,
      DocTemplateService templates,
      DocumentComposer composer,
      StoredFileService files,
      MessageService messages,
      MailHouseGateway mailHouse,
      SbmPrintBatchRepository printBatches,
      OrganizationDirectory organizations,
      SbmHistoryService history,
      AlertService alerts,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.rules = rules;
    this.letters = letters;
    this.policies = policies;
    this.templates = templates;
    this.composer = composer;
    this.files = files;
    this.messages = messages;
    this.mailHouse = mailHouse;
    this.printBatches = printBatches;
    this.organizations = organizations;
    this.history = history;
    this.alerts = alerts;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The dispatch of a company: letters due and the print batches of the day.
   *
   * @param companyId company
   * @param today business date
   * @return what the dispatch did
   */
  public Dispatch dispatch(Long companyId, LocalDate today) {
    int produced = 0;
    for (SbmLetterRule rule :
        rules.findByCompanyIdAndRecordStatus(companyId, RecordStatus.ACTIVE)) {
      for (SbmPolicy p : policies.findAll(due(companyId, rule, today))) {
        if (!letters.existsByPolicyIdAndRuleId(p.getId(), rule.getId())) {
          produce(p, rule);
          produced++;
        }
      }
    }
    List<PrintHandOver> batches = printBatches(companyId, today);
    return new Dispatch(produced, batches.size());
  }

  private static Specification<SbmPolicy> due(Long companyId, SbmLetterRule rule, LocalDate today) {
    LocalDate lastExpiry = today.minusDays(rule.getDaysFromExpiry());
    LocalDate firstExpiry = lastExpiry.minusDays(CATCH_UP_DAYS);
    return (root, q, cb) -> {
      var terms = root.get("terms");
      var p =
          cb.and(
              cb.equal(root.get("companyId"), companyId),
              cb.between(terms.get("expiryDate"), firstExpiry, lastExpiry));
      if (rule.getSegment() != null) {
        p = cb.and(p, cb.equal(root.get("segment"), rule.getSegment()));
      }
      if (rule.getBucket() != null) {
        p = cb.and(p, cb.equal(root.get("bucket"), rule.getBucket()));
      }
      if (rule.getStatus() != null) {
        p = cb.and(p, cb.equal(root.get("status").as(String.class), rule.getStatus()));
      }
      return p;
    };
  }

  private SbmLetter produce(SbmPolicy p, SbmLetterRule rule) {
    SbmLetter letter =
        letters.save(
            new SbmLetter(
                p.getCompanyId(),
                numbers.next("SBL-" + clockYear()),
                p.getId(),
                new SbmLetter.Kind(
                    rule.getId(),
                    rule.getLetterType(),
                    rule.getChannel(),
                    rule.getTemplateCode())));
    deliver(p, letter);
    return letter;
  }

  private void deliver(SbmPolicy p, SbmLetter letter) {
    String to = recipient(p, letter.getChannel());
    if (!PRINT.equals(letter.getChannel()) && (to == null || to.isBlank())) {
      fail(
          p,
          letter,
          (EMAIL.equals(letter.getChannel()) ? "Client " : "The bank counterpart of ")
              + p.getAssured().assuredName()
              + " has no e-mail address");
      return;
    }
    MergedText text = templates.merge(letter.getTemplateCode(), today(), values(p));
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                organizations.company(p.getCompanyId()).name(),
                text.title(),
                letter.getLetterNo(),
                List.of(new Text(null, text.text())),
                List.of(),
                text.versionTag()));
    Long fileId = store(p, letter, pdf);
    letter.generated(
        text.versionNo(),
        null,
        fileId,
        PRINT.equals(letter.getChannel()) ? p.getAssured().mailingAddress() : to);
    if (!PRINT.equals(letter.getChannel())) {
      Long messageId =
          messages
              .queueEmail(
                  new OutboundEmail(
                      p.getCompanyId(),
                      "SBM_LETTER",
                      List.of(to),
                      List.of(),
                      text.title(),
                      text.text(),
                      List.of(
                          new MessageFile(letter.getLetterNo() + ".pdf", "application/pdf", pdf)),
                      null,
                      new RecordLink(SubmittedCodes.ENTITY, p.getId().toString(), p.getSbmNo())))
              .messageId();
      letter.sent(messageId, clock.instant());
    }
    history.note(
        p,
        "Letter",
        DisplayFormat.words(letter.getLetterType()) + " " + letter.getLetterNo(),
        SbmHistorySource.MANUAL,
        letter.getLetterNo());
  }

  private Long store(SbmPolicy p, SbmLetter letter, byte[] pdf) {
    return files
        .storeChecked(
            new StoreRequest(
                new FileOwner(
                    p.getCompanyId(), SubmittedFileStorage.LETTER, letter.getId().toString()),
                "SBM_LETTER",
                "GENERAL_DOCUMENT",
                letter.getLetterNo() + ".pdf",
                pdf,
                null),
            "application/pdf",
            FileOrigin.GENERATED)
        .getId();
  }

  private void fail(SbmPolicy p, SbmLetter letter, String why) {
    letter.failed(why);
    alerts.raise(
        "SBM_LETTER_FAILED",
        new AlertFacts(
            p.getCompanyId(),
            null,
            SubmittedCodes.ENTITY,
            p.getId().toString(),
            letter.getLetterNo() + ": " + why,
            BigDecimal.ZERO,
            "SBM_LETTER_FAILED:" + letter.getLetterNo()));
  }

  /**
   * Sends a failed letter again (after the address was corrected).
   *
   * @param id letter
   * @return the letter
   */
  public SbmLetter resend(Long id) {
    SbmLetter letter =
        letters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Letter", id));
    if (!SbmLetter.FAILED.equals(letter.getStatus())) {
      throw new BusinessRuleException(
          "SBM_LETTER_NOT_FAILED", letter.getLetterNo() + " was not refused");
    }
    SbmPolicy p = policies.findById(letter.getPolicyId()).orElseThrow();
    deliver(p, letter);
    audit.record("SubmittedLetter", letter.getLetterNo(), AuditAction.UPDATE, "Sent again");
    return letter;
  }

  /**
   * Builds the print batches of the day from the generated print letters, one per letter type.
   *
   * @param companyId company
   * @param today business date
   * @return batches
   */
  public List<PrintHandOver> printBatches(Long companyId, LocalDate today) {
    Map<String, List<SbmLetter>> byType =
        letters
            .findByCompanyIdAndChannelAndStatusOrderByIdAsc(companyId, PRINT, SbmLetter.GENERATED)
            .stream()
            .collect(
                Collectors.groupingBy(
                    SbmLetter::getLetterType, LinkedHashMap::new, Collectors.toList()));
    List<PrintHandOver> out = new ArrayList<>();
    byType.forEach(
        (type, list) -> {
          List<PrintedLetter> printed =
              list.stream()
                  .map(
                      l -> {
                        SbmPolicy p = policies.findById(l.getPolicyId()).orElseThrow();
                        return new PrintedLetter(
                            l.getLetterNo(),
                            p.getAssured().assuredName(),
                            l.getRecipient(),
                            l.getStoredFileId());
                      })
                  .toList();
          PrintHandOver batch =
              mailHouse.handOver(
                  new PrintRequest(companyId, SubmittedCodes.MODULE, type, today, printed));
          Long batchId =
              printBatches.findByBatchNo(batch.batchNo()).map(SbmPrintBatch::getId).orElse(null);
          list.forEach(l -> l.printed(batchId, clock.instant()));
          out.add(batch);
        });
    return out;
  }

  private String recipient(SbmPolicy p, String channel) {
    if (EMAIL.equals(channel)) {
      return p.getAssured().email();
    }
    if ("BANK_COUNTERPART".equals(channel)) {
      return p.getAssured().bankCounterpartEmail();
    }
    return p.getAssured().mailingAddress();
  }

  private static Map<String, Object> values(SbmPolicy p) {
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("sbmNo", p.getSbmNo());
    v.put("assuredName", p.getAssured().assuredName());
    v.put("policyNo", p.getTerms().policyNo() == null ? "" : p.getTerms().policyNo());
    v.put("insurerName", p.getTerms().insurerCode() == null ? "" : p.getTerms().insurerCode());
    v.put("expiryDate", DisplayFormat.date(p.getTerms().expiryDate()));
    v.put("currency", p.getTerms().currency());
    v.put("sumInsured", DisplayFormat.amount(p.getTerms().sumInsured()));
    v.put(
        "riskDescription",
        p.getRisk().unitDescription() != null
            ? p.getRisk().unitDescription()
            : String.valueOf(p.getRisk().propertyLocation()));
    v.put(
        "bankCounterpart",
        p.getAssured().bankCounterpartEmail() == null ? "" : p.getAssured().bankCounterpartEmail());
    v.put(
        "findings",
        p.getAdequacyStatus() == null ? "no finding" : DisplayFormat.words(p.getAdequacyStatus()));
    v.put("proposedInsurer", p.getTerms().insurerCode() == null ? "" : p.getTerms().insurerCode());
    return v;
  }

  private LocalDate today() {
    return BusinessClock.today(clock);
  }

  private int clockYear() {
    return today().getYear();
  }

  /**
   * What a dispatch did.
   *
   * @param letters letters produced
   * @param printBatches print batches built
   */
  public record Dispatch(int letters, int printBatches) {}
}
