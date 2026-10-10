package com.iortatechnxt.brokerverse.renewal.proposal.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.UserDisplayNames;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.allocation.service.InsurerAllocationService;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalProposal;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalProposalRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quick and Full Proposals of the renewal accounts For Proposal (FRRN.017.01, FRRN.017.02): a
 * proposal is generated only for the disposition For Proposal; a Full Proposal needs two
 * signatories, or three for a non-package account whose sum insured exceeds the set amount; the
 * file is named {@code Renewal_QuickProposal_<Reference>_MMDDYYYY.pdf} or {@code
 * Renewal_FullProposal_<Reference>_MMDDYYYY.pdf}, attached to the account and sent to the nominated
 * client recipients through CCM; a revised proposal sent puts the client's answer back to Pending
 * Client Response.
 */
@Service
public class RenewalProposals {

  /** Message when the signatories are missing. */
  public static final String SIGNATORIES_REQUIRED =
      "Required signatories must be selected before the Full Quote can be generated.";

  /** Message of a successful sending. */
  public static final String SENT =
      "Proposal has been successfully submitted to CCM for delivery to the client.";

  /** Message when CCM cannot be reached. */
  public static final String UNAVAILABLE =
      "Unable to send the proposal. CCM service is currently unavailable.";

  /** Message of an invalid recipient. */
  public static final String INVALID =
      "Unable to send the proposal. One or more recipient email addresses are invalid.";

  /** Message of another failure. */
  public static final String FAILED =
      "Quotation transmission failed. Please review the error details and try again.";

  private static final DateTimeFormatter NAME_DATE =
      DateTimeFormatter.ofPattern("MMddyyyy", Locale.ROOT);
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");
  private static final String DOC_TYPE = "QUOTATION";
  private static final int TWO = 2;
  private static final int THREE = 3;
  private static final BigDecimal THRESHOLD = new BigDecimal("500000000");

  private final RenewalRecords records;
  private final RenewalProposalRepository proposals;
  private final InsurerAllocationService allocations;
  private final DocumentComposer composer;
  private final DocumentService storage;
  private final ChannelService channels;
  private final DocumentNumberService numbers;
  private final OrganizationService organization;
  private final UserDisplayNames users;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param proposals proposals
   * @param allocations insurers and shares
   * @param composer PDF
   * @param storage stored files
   * @param channels CCM
   * @param numbers proposal numbers
   * @param organization letterhead
   * @param users signatory names
   * @param parameters signatory threshold
   * @param audit audit trail
   * @param clock clock
   */
  public RenewalProposals(
      RenewalRecords records,
      RenewalProposalRepository proposals,
      InsurerAllocationService allocations,
      DocumentComposer composer,
      DocumentService storage,
      ChannelService channels,
      DocumentNumberService numbers,
      OrganizationService organization,
      UserDisplayNames users,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.proposals = proposals;
    this.allocations = allocations;
    this.composer = composer;
    this.storage = storage;
    this.channels = channels;
    this.numbers = numbers;
    this.organization = organization;
    this.users = users;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The number of signatories a Full Proposal of an account needs.
   *
   * @param c renewal
   * @return two or three
   */
  public int requiredSignatories(RenewalCandidate c) {
    if (c.getSnapshot().packaged()) {
      return TWO;
    }
    BigDecimal tsi =
        c.getSnapshot().premium() == null ? null : c.getSnapshot().premium().totalSumInsured();
    BigDecimal threshold = threshold();
    return tsi != null && tsi.compareTo(threshold) > 0 ? THREE : TWO;
  }

  /**
   * Generates a proposal.
   *
   * @param companyId company
   * @param ref renewal
   * @param kind QUICK or FULL
   * @param signatories usernames of the signatories of a Full Proposal
   * @return the proposal
   */
  @Transactional
  public RenewalProposal generate(
      Long companyId, String ref, String kind, List<String> signatories) {
    RenewalCandidate c = records.get(companyId, ref);
    boolean full = requireGenerable(c, kind);
    List<String> names = full ? signatories(c, signatories) : List.of();
    String name =
        (full ? "Renewal_FullProposal_" : "Renewal_QuickProposal_")
            + ref
            + "_"
            + NAME_DATE.format(BusinessClock.today(clock))
            + ".pdf";
    RenewalProposal p =
        proposals.save(
            new RenewalProposal(
                c,
                numbers.next("PRP-" + BusinessClock.today(clock).getYear()),
                kind,
                name,
                full ? String.join(",", names) : null));
    p.stored(store(c, name, pdf(c, full, names)));
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.CREATE, "Proposal " + name + " generated");
    return p;
  }

  private static boolean requireGenerable(RenewalCandidate c, String kind) {
    if (c.getDisposition() == null
        || c.getDisposition().code() != RenewalDisposition.FOR_PROPOSAL) {
      throw new BusinessRuleException(
          "RNW_PROPOSAL_DISPOSITION", "A proposal is generated only for an account For Proposal");
    }
    boolean full = RenewalProposal.FULL.equals(kind);
    if (!full && !RenewalProposal.QUICK.equals(kind)) {
      throw new BusinessRuleException("RNW_PROPOSAL_KIND", "Select Quick or Full Proposal");
    }
    return full;
  }

  /**
   * Records a proposal completed by TSU (its file already attached).
   *
   * @param c renewal
   * @param fileName file name
   * @param attachmentId stored file
   * @return the proposal
   */
  public RenewalProposal fromTsu(RenewalCandidate c, String fileName, Long attachmentId) {
    RenewalProposal p =
        proposals.save(
            new RenewalProposal(
                c,
                numbers.next("PRP-" + BusinessClock.today(clock).getYear()),
                RenewalProposal.TSU,
                fileName,
                null));
    p.stored(attachmentId);
    return p;
  }

  /**
   * The proposals of an account, newest first.
   *
   * @param companyId company
   * @param ref renewal
   * @return proposals
   */
  @Transactional(readOnly = true)
  public List<RenewalProposal> of(Long companyId, String ref) {
    return proposals.findByCandidateIdOrderByIdDesc(records.get(companyId, ref).getId());
  }

  /**
   * Sends a proposal to the client through CCM.
   *
   * @param companyId company
   * @param ref renewal
   * @param proposalNo proposal
   * @param to nominated recipients
   * @param cc copy recipients
   * @return the success message
   */
  @Transactional(noRollbackFor = BusinessRuleException.class)
  public String send(
      Long companyId, String ref, String proposalNo, List<String> to, List<String> cc) {
    RenewalCandidate c = records.get(companyId, ref);
    RenewalProposal p =
        proposals
            .findByCompanyIdAndProposalNo(companyId, proposalNo)
            .filter(x -> x.getCandidateId().equals(c.getId()))
            .orElseThrow(
                () -> new BusinessRuleException("RNW_PROPOSAL_NOT_FOUND", "Proposal not found"));
    List<String> recipients = to == null ? List.of() : to;
    List<String> copies = cc == null ? List.of() : cc;
    requireAddresses(recipients, copies);
    ChannelService.Sent sent =
        channels.send(
            companyId,
            new ChannelService.Outbound(
                ChannelGateways.CCM,
                new ChannelMessage.Document(
                    "PROPOSAL",
                    p.getProposalNo(),
                    c.getId(),
                    ref,
                    p.getFileName(),
                    p.getAttachmentId()),
                new ChannelMessage.Address(
                    String.join(",", recipients),
                    copies.isEmpty() ? null : String.join(",", copies),
                    "Renewal proposal - " + c.getSnapshot().clientName(),
                    "Please find attached the renewal proposal " + p.getFileName() + ".",
                    null)));
    if (sent.error() != null) {
      throw new BusinessRuleException(
          "RNW_PROPOSAL_SEND",
          sent.message().getStatus() == ChannelStatus.PENDING_TRANSMISSION ? UNAVAILABLE : FAILED);
    }
    p.sent(sent.message().getMessageNo(), String.join(",", recipients));
    awaitClient(c);
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        AuditAction.UPDATE,
        "Proposal " + p.getFileName() + " sent through CCM");
    return SENT;
  }

  private static void requireAddresses(List<String> recipients, List<String> copies) {
    if (recipients.isEmpty()
        || recipients.stream().anyMatch(a -> !EMAIL.matcher(a.strip()).matches())
        || copies.stream().anyMatch(a -> !EMAIL.matcher(a.strip()).matches())) {
      throw new BusinessRuleException("RNW_PROPOSAL_RECIPIENT", INVALID);
    }
  }

  private void awaitClient(RenewalCandidate c) {
    String status = c.getPlacement().getDecision().getClientStatus();
    if (status == null || CandidateDecision.REVISION.equals(status)) {
      c.getPlacement()
          .getDecision()
          .client(CandidateDecision.PENDING, null, "SYSTEM", clock.instant());
    }
  }

  private List<String> signatories(RenewalCandidate c, List<String> chosen) {
    Set<String> names = new LinkedHashSet<>();
    if (chosen != null) {
      chosen.stream()
          .filter(Objects::nonNull)
          .map(String::strip)
          .filter(s -> !s.isEmpty())
          .forEach(names::add);
    }
    if (names.size() < requiredSignatories(c)) {
      throw new BusinessRuleException("RNW_PROPOSAL_SIGNATORIES", SIGNATORIES_REQUIRED);
    }
    return new ArrayList<>(names);
  }

  private BigDecimal threshold() {
    String value = parameters.text("RNW_PROPOSAL_THREE_SIGNATORIES_TSI", "").strip();
    return value.isEmpty() ? THRESHOLD : new BigDecimal(value);
  }

  private byte[] pdf(RenewalCandidate c, boolean full, List<String> signatories) {
    LocalDate inception = c.getExpiryDate();
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("Reference Number", c.getRenewalRef()));
    fields.add(new Field("Assured", c.getSnapshot().clientName()));
    fields.add(
        new Field("Previous Policy Number", Objects.toString(c.getSnapshot().policyNo(), "")));
    fields.add(
        new Field("Period of Insurance", DisplayFormat.period(inception, inception.plusYears(1))));
    if (c.getSnapshot().premium() != null) {
      fields.add(
          new Field(
              "Sum Insured", DisplayFormat.value(c.getSnapshot().premium().totalSumInsured())));
      fields.add(
          new Field("Premium", DisplayFormat.value(c.getSnapshot().premium().grossPremium())));
    }
    allocations
        .of(c)
        .forEach(
            s ->
                fields.add(
                    new Field(
                        "Insurer " + s.insurerCode(),
                        s.percent().stripTrailingZeros().toPlainString() + "%")));
    List<String> captions = new ArrayList<>();
    signatories.forEach(
        u -> captions.add(DocumentSpec.signature("Signatory", users.displayName(u))));
    return composer.pdf(
        new DocumentSpec(
            organization.getCompany(c.getCompanyId()).getName(),
            full ? "Full Proposal" : "Quick Proposal",
            c.getRenewalRef(),
            List.of(
                new Fields("Renewal", fields),
                new Text(
                    "Proposal",
                    "We are pleased to propose the renewal of your insurance on the terms above.")),
            captions,
            null));
  }

  private Long store(RenewalCandidate c, String name, byte[] pdf) {
    return storage
        .upload(
            new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
            List.of(new DocumentService.UploadedFile(name, pdf)),
            new DocumentService.UploadOptions(DOC_TYPE, false, c.getRenewalRef(), name, null))
        .get(0)
        .getId();
  }
}
