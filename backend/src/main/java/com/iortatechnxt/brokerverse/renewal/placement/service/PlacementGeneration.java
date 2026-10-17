package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.allocation.service.InsurerAllocationService;
import com.iortatechnxt.brokerverse.renewal.domain.PlacementFile;
import com.iortatechnxt.brokerverse.renewal.domain.PlacementFileRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generate Placement (FRRN.029.02): for the renewal accounts submitted for placement, one placement
 * slip per insurer per account (one per co-insurer, with its share) attached to the account, and
 * for the packaged accounts one consolidated placement file per insurer and product line. Accounts
 * tagged For Booking Only are left out; a slip not yet sent is replaced by the new one.
 */
@Service
public class PlacementGeneration {

  /** Document type of a placement slip. */
  public static final String DOC_SLIP = "PLACEMENT_SLIP";

  /** Document type of a placement file. */
  public static final String DOC_FILE = "PLACEMENT_FILE";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final RenewalRecords records;
  private final InsurerAllocationService allocations;
  private final RenewalPlacementRepository placements;
  private final PlacementFileRepository files;
  private final PlacementDocuments documents;
  private final DocumentService storage;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the generation.
   *
   * @param records renewals
   * @param allocations insurers and shares
   * @param placements placements
   * @param files placement files
   * @param documents slip and file content
   * @param storage stored documents
   * @param audit audit trail
   * @param clock clock
   */
  public PlacementGeneration(
      RenewalRecords records,
      InsurerAllocationService allocations,
      RenewalPlacementRepository placements,
      PlacementFileRepository files,
      PlacementDocuments documents,
      DocumentService storage,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.allocations = allocations;
    this.placements = placements;
    this.files = files;
    this.documents = documents;
    this.storage = storage;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Generates the placement documents of renewal accounts.
   *
   * @param companyId company
   * @param refs renewals
   * @return generated and refused renewals
   */
  @Transactional
  public BatchOutcome generate(Long companyId, List<String> refs) {
    LocalDate today = BusinessClock.today(clock);
    BatchOutcome.Builder outcome = new BatchOutcome.Builder();
    Map<String, List<PlacementDocuments.Line>> byFile = new LinkedHashMap<>();
    for (String ref : refs) {
      RenewalCandidate c = records.get(companyId, ref);
      String refusal = refusal(c);
      if (refusal != null) {
        outcome.refused(ref, refusal);
        continue;
      }
      for (RenewalPlacement p : slips(c, today)) {
        if (c.getSnapshot().packaged()) {
          byFile
              .computeIfAbsent(p.getInsurerCode() + "|" + line(c), k -> new ArrayList<>())
              .add(new PlacementDocuments.Line(c, p));
        }
      }
      audit.record(RenewalCodes.ENTITY, ref, AuditAction.UPDATE, "Placement generated");
      outcome.done(ref);
    }
    byFile.values().forEach(lines -> consolidate(lines, today));
    return outcome.build();
  }

  /**
   * Cancels the placements of a renewal that were not answered.
   *
   * @param c renewal
   */
  public void cancelOpen(RenewalCandidate c) {
    placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(
            p ->
                RenewalPlacement.GENERATED.equals(p.getStatus())
                    || RenewalPlacement.SENT.equals(p.getStatus()))
        .forEach(RenewalPlacement::cancel);
  }

  private static String refusal(RenewalCandidate c) {
    if (c.getStage() != RenewalStage.FOR_PLACEMENT_BOOKING) {
      return "The account is not submitted for placement";
    }
    if (c.getPlacement().isForBookingOnly()) {
      return "The account is tagged For Booking Only";
    }
    return c.getPlacement().getStatus() == null
        ? null
        : "The placement of the account was already sent";
  }

  private List<RenewalPlacement> slips(RenewalCandidate c, LocalDate today) {
    placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(p -> RenewalPlacement.GENERATED.equals(p.getStatus()))
        .forEach(RenewalPlacement::cancel);
    List<RenewalPlacement> made = new ArrayList<>();
    BigDecimal tsi =
        c.getSnapshot().premium() == null ? null : c.getSnapshot().premium().totalSumInsured();
    for (InsurerAllocationService.Share s : allocations.of(c)) {
      if (s.insurerCode() == null) {
        throw new BusinessRuleException(
            "RNW_PLACEMENT_INSURER", c.getRenewalRef() + " has no insurer to place with");
      }
      BigDecimal sumInsured = s.amount() != null ? s.amount() : portion(tsi, s.percent());
      RenewalPlacement p =
          placements.save(
              new RenewalPlacement(
                  c,
                  new RenewalPlacement.Share(s.insurerCode(), s.percent(), s.premium(), sumInsured),
                  PlacementNames.slip(s.insurerCode(), line(c), c.getRenewalRef(), today)));
      p.stored(store(c, p.getSlipFileName(), documents.slip(c, p), DOC_SLIP), null);
      made.add(p);
    }
    return made;
  }

  private void consolidate(List<PlacementDocuments.Line> lines, LocalDate today) {
    RenewalCandidate first = lines.get(0).candidate();
    String insurer = lines.get(0).placement().getInsurerCode();
    PlacementFile file =
        files.save(
            new PlacementFile(
                first.getCompanyId(),
                insurer,
                line(first),
                PlacementNames.file(insurer, line(first), today),
                lines.size()));
    Long id = store(first, file.getFileName(), documents.file(lines), DOC_FILE);
    file.stored(id);
    List<AttachmentTarget> others = new ArrayList<>();
    lines.stream()
        .skip(1)
        .forEach(
            l ->
                others.add(
                    new AttachmentTarget(RenewalCodes.ENTITY, l.candidate().getId().toString())));
    if (!others.isEmpty()) {
      storage.link(id, others);
    }
    lines.forEach(l -> l.placement().stored(l.placement().getSlipAttachmentId(), file.getId()));
  }

  private Long store(RenewalCandidate c, String name, byte[] content, String type) {
    return storage
        .upload(
            new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
            List.of(new DocumentService.UploadedFile(name, content)),
            new DocumentService.UploadOptions(type, false, c.getRenewalRef(), name, null))
        .get(0)
        .getId();
  }

  private static BigDecimal portion(BigDecimal tsi, BigDecimal percent) {
    return tsi == null ? null : tsi.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  private static String line(RenewalCandidate c) {
    return c.getSnapshot().product() == null ? null : c.getSnapshot().product().lineCode();
  }
}
