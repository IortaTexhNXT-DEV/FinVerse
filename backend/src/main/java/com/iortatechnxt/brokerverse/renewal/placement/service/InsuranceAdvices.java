package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.AdviceVersion;
import com.iortatechnxt.brokerverse.renewal.domain.AdviceVersionRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacementRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Insurance Advice of the mortgaged renewal accounts (FRRN.032): generated as {@code
 * IA_<Reference>_MMDDYYYY.pdf} when the placement is sent to the insurer (or when an account tagged
 * For Booking Only is submitted for placement), attached to the account with its version; a change
 * of the placement makes a new version and the earlier ones are kept. Send Insurance Advice sends
 * the latest version of each selected account through CCM to the nominated and copy recipients.
 */
@Service
public class InsuranceAdvices {

  /** Kind of channel document. */
  public static final String KIND = "IA";

  /** Document type of an Insurance Advice. */
  public static final String DOC_TYPE = "INSURANCE_ADVICE";

  /** Parameter: automatic generation. */
  public static final String AUTO = "RNW_INSURANCE_ADVICE_AUTO";

  private final RenewalRecords records;
  private final AdviceVersionRepository versions;
  private final RenewalPlacementRepository placements;
  private final PlacementDocuments documents;
  private final DocumentService storage;
  private final ChannelService channels;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param versions advice versions
   * @param placements placements (content and fingerprint)
   * @param documents PDF
   * @param storage stored documents
   * @param channels CCM
   * @param parameters automatic generation
   * @param audit audit trail
   * @param clock clock
   */
  public InsuranceAdvices(
      RenewalRecords records,
      AdviceVersionRepository versions,
      RenewalPlacementRepository placements,
      PlacementDocuments documents,
      DocumentService storage,
      ChannelService channels,
      SystemParameterService parameters,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.versions = versions;
    this.placements = placements;
    this.documents = documents;
    this.storage = storage;
    this.channels = channels;
    this.parameters = parameters;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Generates the advice of a mortgaged account when its placement is new or changed.
   *
   * @param c renewal
   * @param trigger what triggers it (PLACEMENT_SENT, FOR_BOOKING_ONLY, PLACEMENT_CHANGED)
   * @return the new version, null when none is needed
   */
  public AdviceVersion afterPlacement(RenewalCandidate c, String trigger) {
    if (!"true".equals(parameters.text(AUTO, "true").strip()) || !mortgaged(c)) {
      return null;
    }
    List<RenewalPlacement> current = current(c);
    String fingerprint = fingerprint(c, current);
    List<AdviceVersion> made = versions.findByCandidateIdOrderByVersionNoDesc(c.getId());
    if (!made.isEmpty() && made.get(0).getFingerprint().equals(fingerprint)) {
      return null;
    }
    int no = made.isEmpty() ? 1 : made.get(0).getVersionNo() + 1;
    String name = PlacementNames.advice(c.getRenewalRef(), BusinessClock.today(clock));
    AdviceVersion v = versions.save(new AdviceVersion(c, no, name, fingerprint, trigger));
    Long id =
        storage
            .upload(
                new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
                List.of(new DocumentService.UploadedFile(name, documents.advice(c, current, no))),
                new DocumentService.UploadOptions(
                    DOC_TYPE, false, c.getRenewalRef(), "Insurance Advice version " + no, null))
            .get(0)
            .getId();
    v.stored(id);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "Insurance Advice " + name + " version " + no);
    return v;
  }

  /**
   * The versions of the advice of a renewal, newest first.
   *
   * @param companyId company
   * @param ref renewal
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<AdviceVersion> of(Long companyId, String ref) {
    return versions.findByCandidateIdOrderByVersionNoDesc(records.get(companyId, ref).getId());
  }

  /**
   * Sends the latest advice of renewal accounts through CCM.
   *
   * @param companyId company
   * @param refs renewals, each with an advice
   * @param to nominated recipients
   * @param cc copy recipients
   * @return the CCM messages, one per account
   */
  @Transactional
  public List<ChannelMessage> send(
      Long companyId, List<String> refs, List<String> to, List<String> cc) {
    if (to == null || to.isEmpty()) {
      throw new BusinessRuleException("RNW_IA_RECIPIENT", "Nominate at least one recipient");
    }
    List<AdviceVersion> latest = latest(companyId, refs);
    List<ChannelMessage> sent = new ArrayList<>();
    for (int i = 0; i < refs.size(); i++) {
      AdviceVersion v = latest.get(i);
      ChannelService.Sent s =
          channels.send(
              companyId,
              new ChannelService.Outbound(
                  ChannelGateways.CCM,
                  new ChannelMessage.Document(
                      KIND,
                      v.getFileName(),
                      v.getCandidateId(),
                      refs.get(i),
                      v.getFileName(),
                      v.getAttachmentId()),
                  new ChannelMessage.Address(
                      String.join(",", to),
                      cc == null || cc.isEmpty() ? null : String.join(",", cc),
                      "Insurance Advice - " + refs.get(i),
                      "Please find attached the Insurance Advice " + v.getFileName() + ".",
                      null)));
      v.sent(s.message().getMessageNo());
      sent.add(s.message());
    }
    return sent;
  }

  private List<AdviceVersion> latest(Long companyId, List<String> refs) {
    List<AdviceVersion> latest = new ArrayList<>();
    for (String ref : refs) {
      List<AdviceVersion> v = of(companyId, ref);
      if (v.isEmpty()) {
        throw new BusinessRuleException(
            "RNW_IA_MISSING", "No Insurance Advice has been generated for " + ref);
      }
      latest.add(v.get(0));
    }
    return latest;
  }

  private List<RenewalPlacement> current(RenewalCandidate c) {
    return placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(RenewalPlacement::isCurrent)
        .toList();
  }

  private static boolean mortgaged(RenewalCandidate c) {
    return c.getSnapshot().mortgage() != null && c.getSnapshot().mortgage().mortgaged();
  }

  private static String fingerprint(RenewalCandidate c, List<RenewalPlacement> current) {
    StringBuilder text = new StringBuilder(c.getRenewalRef());
    current.stream()
        .sorted((a, b) -> a.getInsurerCode().compareTo(b.getInsurerCode()))
        .forEach(
            p ->
                text.append('|')
                    .append(p.getInsurerCode())
                    .append(':')
                    .append(p.getSharePercent().stripTrailingZeros().toPlainString())
                    .append(':')
                    .append(Objects.toString(p.getSumInsured(), ""))
                    .append(':')
                    .append(Objects.toString(p.getPremium(), "")));
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(text.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
