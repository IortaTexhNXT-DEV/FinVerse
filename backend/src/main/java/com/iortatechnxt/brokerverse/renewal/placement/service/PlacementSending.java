package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.PlacementFile;
import com.iortatechnxt.brokerverse.renewal.domain.PlacementFileRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacementRepository;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.InsurerDelivery;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sending of the placement documents to the insurers (FRRN.29.03, FRRN.29.04): an insurer enrolled
 * in MFT receives the slips and its placement file in its MFT location (Pending Upload, Uploaded to
 * MFT, Upload Failed, the failure notified to the Processing Officer); the others through CCM to
 * the recipients of Insurer Maintenance, which the user can change, with copy recipients. A sent
 * placement makes the account For Booking and starts its turnaround time; a mortgaged account gets
 * its Insurance Advice.
 */
@Service
public class PlacementSending {

  /** Kind of channel document. */
  public static final String KIND = "PLACEMENT";

  /** Notification: the placement could not be sent. */
  public static final String EVENT_FAILED = "RNW_PLACEMENT_FAILED";

  private static final int SLA_PACKAGED = 3;
  private static final int SLA_NON_PACKAGE = 10;
  private static final int CUTOFF_HOUR = 15;
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");

  private final RenewalRecords records;
  private final RenewalPlacementRepository placements;
  private final PlacementFileRepository files;
  private final InsurerService insurers;
  private final InsurerDelivery delivery;
  private final ChannelService channels;
  private final InsuranceAdvices advices;
  private final RenewalWorkingDays workingDays;
  private final SystemParameterService parameters;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the sending.
   *
   * @param records renewals
   * @param placements placements
   * @param files placement files
   * @param insurers insurer maintenance (recipients)
   * @param delivery MFT enrolment
   * @param channels CCM and MFT
   * @param advices Insurance Advice
   * @param workingDays business days
   * @param parameters SLA and cut-off
   * @param notices notifications
   * @param audit audit trail
   * @param clock clock
   */
  public PlacementSending(
      RenewalRecords records,
      RenewalPlacementRepository placements,
      PlacementFileRepository files,
      InsurerService insurers,
      InsurerDelivery delivery,
      ChannelService channels,
      InsuranceAdvices advices,
      RenewalWorkingDays workingDays,
      SystemParameterService parameters,
      RenewalNotices notices,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.placements = placements;
    this.files = files;
    this.insurers = insurers;
    this.delivery = delivery;
    this.channels = channels;
    this.advices = advices;
    this.workingDays = workingDays;
    this.parameters = parameters;
    this.notices = notices;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The insurers of the generated placements of renewal accounts with their default recipients.
   *
   * @param companyId company
   * @param refs renewals
   * @return one entry per insurer
   */
  @Transactional(readOnly = true)
  public List<Recipients> recipients(Long companyId, List<String> refs) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String ref : refs) {
      RenewalCandidate c = records.get(companyId, ref);
      generated(c).forEach(p -> counts.merge(p.getInsurerCode(), 1, Integer::sum));
    }
    List<Recipients> out = new ArrayList<>();
    counts.forEach(
        (code, n) -> {
          InsurerProfile insurer = insurers.requireInsurer(companyId, code);
          out.add(
              new Recipients(
                  code,
                  insurer.getName(),
                  delivery.mft(companyId, code),
                  insurer.getPlacementEmailList(),
                  n));
        });
    return out;
  }

  /**
   * Sends the generated placements of renewal accounts.
   *
   * @param companyId company
   * @param request renewals, recipients per insurer and copy recipients
   * @return summary
   */
  @Transactional
  public Summary send(Long companyId, Request request) {
    List<String> cc = request.cc() == null ? List.of() : request.cc();
    cc.forEach(PlacementSending::requireEmail);
    Summary.Builder summary = new Summary.Builder();
    Set<Long> filesSent = new HashSet<>();
    for (String ref : request.refs()) {
      RenewalCandidate c = records.get(companyId, ref);
      List<RenewalPlacement> open = generated(c);
      if (open.isEmpty()) {
        summary.refused(ref, "No placement generated to send");
        continue;
      }
      boolean all = true;
      for (RenewalPlacement p : open) {
        List<String> to = to(companyId, p.getInsurerCode(), request.recipients());
        all &= sendOne(c, p, to, cc);
        sendFile(p, to, cc, filesSent);
      }
      afterSending(c, all, summary);
    }
    return summary.build();
  }

  private void afterSending(RenewalCandidate c, boolean all, Summary.Builder summary) {
    String ref = c.getRenewalRef();
    if (!all) {
      summary.refused(ref, "Not all placements could be sent");
      notices.users(
          Collections.singletonList(c.getAssignedPo()),
          EVENT_FAILED,
          c,
          new RenewalNotices.Text(ref + " placement not sent", "Check the Channel Monitor"));
      return;
    }
    c.getPlacement().status(CandidatePlacement.FOR_BOOKING);
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.UPDATE, "Placement sent - For Booking");
    advices.afterPlacement(c, "PLACEMENT_SENT");
    summary.done(ref);
  }

  private boolean sendOne(
      RenewalCandidate c, RenewalPlacement p, List<String> to, List<String> cc) {
    boolean mft = delivery.mft(c.getCompanyId(), p.getInsurerCode());
    ChannelService.Sent sent =
        channels.send(
            c.getCompanyId(),
            new ChannelService.Outbound(
                mft ? ChannelGateways.MFT : ChannelGateways.CCM,
                new ChannelMessage.Document(
                    KIND,
                    p.getSlipFileName(),
                    c.getId(),
                    c.getRenewalRef(),
                    p.getSlipFileName(),
                    p.getSlipAttachmentId()),
                address(mft, p.getInsurerCode(), to, cc, p.getSlipFileName())));
    boolean accepted = sent.error() == null;
    p.sent(
        new RenewalPlacement.Sending(
            sent.message().getChannel(),
            sent.message().getMessageNo(),
            sent.message().getStatus().name(),
            mft ? p.getInsurerCode() : String.join(", ", to),
            mft ? null : String.join(", ", cc),
            accepted),
        clock.instant(),
        tat(c));
    return accepted;
  }

  private void sendFile(RenewalPlacement p, List<String> to, List<String> cc, Set<Long> done) {
    Long id = p.getPlacementFileId();
    if (id == null || !done.add(id)) {
      return;
    }
    PlacementFile file = files.findById(id).orElseThrow();
    boolean mft = delivery.mft(file.getCompanyId(), file.getInsurerCode());
    ChannelService.Sent sent =
        channels.send(
            file.getCompanyId(),
            new ChannelService.Outbound(
                mft ? ChannelGateways.MFT : ChannelGateways.CCM,
                new ChannelMessage.Document(
                    KIND,
                    file.getFileName(),
                    null,
                    null,
                    file.getFileName(),
                    file.getAttachmentId()),
                address(mft, file.getInsurerCode(), to, cc, file.getFileName())));
    file.sent(
        sent.message().getChannel(),
        sent.message().getMessageNo(),
        sent.message().getStatus().name());
  }

  private RenewalPlacement.Tat tat(RenewalCandidate c) {
    int sla =
        c.getSnapshot().packaged()
            ? parameters.intValue("RNW_PLACEMENT_SLA_PACKAGED", SLA_PACKAGED)
            : parameters.intValue("RNW_PLACEMENT_SLA_NON_PACKAGE", SLA_NON_PACKAGE);
    return new RenewalPlacement.Tat(
        PlacementTat.start(
            BusinessClock.now(clock),
            parameters.intValue("RNW_PLACEMENT_CUTOFF_HOUR", CUTOFF_HOUR),
            workingDays.calendar(c.getCompanyId())),
        sla);
  }

  private static ChannelMessage.Address address(
      boolean mft, String insurerCode, List<String> to, List<String> cc, String fileName) {
    if (mft) {
      return new ChannelMessage.Address(insurerCode, null, "Placement " + fileName, null, null);
    }
    return new ChannelMessage.Address(
        String.join(",", to),
        cc.isEmpty() ? null : String.join(",", cc),
        "Renewal placement - " + fileName,
        "Please find attached the renewal placement " + fileName + ".",
        null);
  }

  private List<String> to(Long companyId, String insurerCode, Map<String, List<String>> chosen) {
    List<String> to =
        chosen != null && chosen.containsKey(insurerCode)
            ? chosen.get(insurerCode)
            : insurers.requireInsurer(companyId, insurerCode).getPlacementEmailList();
    if (to.isEmpty() && !delivery.mft(companyId, insurerCode)) {
      throw new BusinessRuleException(
          "RNW_PLACEMENT_RECIPIENT", "Enter a recipient for the insurer " + insurerCode);
    }
    to.forEach(PlacementSending::requireEmail);
    return to;
  }

  private static void requireEmail(String address) {
    if (address == null || !EMAIL.matcher(address.strip()).matches()) {
      throw new BusinessRuleException(
          "RNW_PLACEMENT_EMAIL", "Invalid email address format: " + address);
    }
  }

  private List<RenewalPlacement> generated(RenewalCandidate c) {
    return placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(p -> RenewalPlacement.GENERATED.equals(p.getStatus()))
        .toList();
  }

  /**
   * The request.
   *
   * @param refs renewals
   * @param recipients recipients per insurer code, the insurer's own when absent
   * @param cc copy recipients
   */
  public record Request(List<String> refs, Map<String, List<String>> recipients, List<String> cc) {}

  /**
   * The recipients of an insurer.
   *
   * @param insurerCode insurer
   * @param insurerName name
   * @param mft enrolled in MFT
   * @param recipients default recipients
   * @param accounts placements to send
   */
  public record Recipients(
      String insurerCode, String insurerName, boolean mft, List<String> recipients, int accounts) {}

  /**
   * The outcome of a sending.
   *
   * @param selected accounts selected
   * @param submitted accounts sent
   * @param failed refused accounts with the reason
   */
  public record Summary(int selected, List<String> submitted, Map<String, String> failed) {

    /** Defensive copies. */
    public Summary {
      submitted = List.copyOf(submitted);
      failed = Map.copyOf(failed);
    }

    /** Collects a summary. */
    static final class Builder {
      private final List<String> done = new ArrayList<>();
      private final Map<String, String> refused = new LinkedHashMap<>();

      void done(String ref) {
        done.add(ref);
      }

      void refused(String ref, String reason) {
        refused.put(ref, reason);
      }

      Summary build() {
        return new Summary(done.size() + refused.size(), done, refused);
      }
    }
  }
}
