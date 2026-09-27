package com.iortatechnxt.brokerverse.prodrecon.api.dto;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.MatchMethod;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ReconStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UploadStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSide;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconCycleService.CycleView;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconItemService.Bucket;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService.UploadResult;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Request and response records of the production reconciliation API (PRCID.001-039). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ProdReconDtos {

  private ProdReconDtos() {}

  /**
   * Item counts of a cycle per bucket.
   *
   * @param matched matched
   * @param discrepancy matched with discrepancies
   * @param bdoiOnly booked by BDOI only
   * @param insurerOnly insurer production without a booked invoice
   * @param total every item
   */
  public record BucketCounts(
      long matched, long discrepancy, long bdoiOnly, long insurerOnly, long total) {

    /**
     * Counts from the counts per status.
     *
     * @param c counts per status
     * @return bucket counts
     */
    public static BucketCounts from(Map<ReconStatus, Long> c) {
      long matched = c.getOrDefault(ReconStatus.MATCHED, 0L);
      long discrepancy = c.getOrDefault(ReconStatus.MATCHED_WITH_DISCREPANCY, 0L);
      long bdoi = c.getOrDefault(ReconStatus.BDOI_ONLY, 0L);
      long insurer =
          c.getOrDefault(ReconStatus.UNMATCHED_PREBOOKED, 0L)
              + c.getOrDefault(ReconStatus.UNMATCHED_NO_BOOKING, 0L);
      return new BucketCounts(
          matched, discrepancy, bdoi, insurer, matched + discrepancy + bdoi + insurer);
    }
  }

  /**
   * A reconciliation cycle.
   *
   * @param id id
   * @param cycleNo cycle number
   * @param insurerCode insurer
   * @param productionMonth first day of the production month
   * @param stage workflow stage
   * @param closed closed
   * @param sentAt last sent
   * @param lastUploadAt last insurer upload
   * @param lastMatchedAt last matching run
   * @param closedAt closed at
   * @param counts items per bucket
   */
  public record CycleResponse(
      Long id,
      String cycleNo,
      String insurerCode,
      LocalDate productionMonth,
      String stage,
      boolean closed,
      Instant sentAt,
      Instant lastUploadAt,
      Instant lastMatchedAt,
      Instant closedAt,
      BucketCounts counts) {

    /**
     * Maps a cycle.
     *
     * @param v cycle with counts
     * @return response
     */
    public static CycleResponse from(CycleView v) {
      ReconCycle c = v.cycle();
      return new CycleResponse(
          c.getId(),
          c.getCycleNo(),
          c.getInsurerCode(),
          c.getProductionMonth(),
          c.getStage(),
          c.isClosed(),
          c.getSentAt(),
          c.getLastUploadAt(),
          c.getLastMatchedAt(),
          c.getClosedAt(),
          BucketCounts.from(v.counts()));
    }
  }

  /**
   * An upload attempt.
   *
   * @param id id
   * @param cycleId cycle
   * @param insurerCode insurer
   * @param productionMonth month
   * @param fileName file
   * @param attemptNo attempt number
   * @param status outcome
   * @param runNo flow-in run
   * @param rowsRead rows read
   * @param rowsAccepted rows taken in
   * @param rowsFailed rows failed
   * @param message message
   * @param createdAt uploaded at
   * @param createdBy uploaded by
   */
  public record UploadResponse(
      Long id,
      Long cycleId,
      String insurerCode,
      LocalDate productionMonth,
      String fileName,
      int attemptNo,
      UploadStatus status,
      String runNo,
      int rowsRead,
      int rowsAccepted,
      int rowsFailed,
      String message,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps an attempt.
     *
     * @param u attempt
     * @return response
     */
    public static UploadResponse from(ReconUpload u) {
      return new UploadResponse(
          u.getId(),
          u.getCycleId(),
          u.getInsurerCode(),
          u.getProductionMonth(),
          u.getFileName(),
          u.getAttemptNo(),
          u.getStatus(),
          u.getRunNo(),
          u.getRowsRead(),
          u.getRowsAccepted(),
          u.getRowsFailed(),
          u.getMessage(),
          u.getCreatedAt(),
          u.getCreatedBy());
    }
  }

  /**
   * The result of an upload.
   *
   * @param runNo flow-in run
   * @param runStatus run status
   * @param runMessage run message
   * @param attempts attempts per insurer and month
   */
  public record UploadResultResponse(
      String runNo, String runStatus, String runMessage, List<UploadResponse> attempts) {

    /**
     * Maps a result.
     *
     * @param r result
     * @return response
     */
    public static UploadResultResponse from(UploadResult r) {
      FlowInRun run = r.run();
      return new UploadResultResponse(
          run.getRunNo(),
          run.getStatus().name(),
          run.getMessage(),
          r.attempts().stream().map(UploadResponse::from).toList());
    }
  }

  /**
   * One side of an item.
   *
   * @param policyNo policy
   * @param referenceNo reference / invoice
   * @param pnNo PN
   * @param periodFrom period start
   * @param periodTo period end
   * @param assuredName assured
   * @param commission commission
   * @param basicPremium basic premium
   * @param grossPremium gross premium
   */
  public record SideResponse(
      String policyNo,
      String referenceNo,
      String pnNo,
      LocalDate periodFrom,
      LocalDate periodTo,
      String assuredName,
      BigDecimal commission,
      BigDecimal basicPremium,
      BigDecimal grossPremium) {

    /**
     * Maps a side.
     *
     * @param s side, may be null
     * @return response, null when absent
     */
    public static SideResponse from(ReconSide s) {
      return s == null
          ? null
          : new SideResponse(
              s.policyNo(),
              s.referenceNo(),
              s.pnNo(),
              s.periodFrom(),
              s.periodTo(),
              s.assuredName(),
              s.commission(),
              s.basicPremium(),
              s.grossPremium());
    }
  }

  /**
   * A reconciliation item.
   *
   * @param id id
   * @param cycleId cycle
   * @param status status
   * @param invoiceNo booked invoice
   * @param arn ARN
   * @param bookingDate booking date
   * @param aoUsername account officer
   * @param salesUnit sales unit / AB
   * @param segment segment
   * @param productLine product line
   * @param bdoi booked side
   * @param insurer insurer side
   * @param insurerIncentive incentive reported by the insurer
   * @param insurerRemarks insurer remarks
   * @param inOriginalExtract whether BDOI sent the line
   * @param prebookedArn pre-booked account
   * @param discrepancies differing fields
   * @param matchMethod auto or manual
   * @param matchedAt matched at
   * @param matchedBy matched by
   * @param feedback feedback and disposition
   * @param unbookedStatus resolution of insurer-only production
   */
  public record ItemResponse(
      Long id,
      Long cycleId,
      ReconStatus status,
      String invoiceNo,
      String arn,
      LocalDate bookingDate,
      String aoUsername,
      String salesUnit,
      String segment,
      String productLine,
      SideResponse bdoi,
      SideResponse insurer,
      BigDecimal insurerIncentive,
      String insurerRemarks,
      boolean inOriginalExtract,
      String prebookedArn,
      List<String> discrepancies,
      MatchMethod matchMethod,
      Instant matchedAt,
      String matchedBy,
      FeedbackRequest feedback,
      UnbookedStatus unbookedStatus) {

    /**
     * Maps an item.
     *
     * @param i item
     * @return response
     */
    public static ItemResponse from(ReconItem i) {
      return new ItemResponse(
          i.getId(),
          i.getCycleId(),
          i.getStatus(),
          i.getInvoiceNo(),
          i.getArn(),
          i.getBookingDate(),
          i.getAoUsername(),
          i.getSalesUnit(),
          i.getSegment(),
          i.getProductLine(),
          SideResponse.from(i.getBdoi()),
          SideResponse.from(i.getInsurer()),
          i.getInsurerIncentive(),
          i.getInsurerRemarks(),
          i.isInOriginalExtract(),
          i.getPrebookedArn(),
          i.getDiscrepancies() == null
              ? List.of()
              : Arrays.stream(i.getDiscrepancies().split(";")).map(String::strip).toList(),
          i.getMatchMethod(),
          i.getMatchedAt(),
          i.getMatchedBy(),
          FeedbackRequest.from(i),
          i.getUnbookedStatus());
    }
  }

  /**
   * Feedback and disposition of an item (PRCID.015/016/018).
   *
   * @param companyConcerned company concerned (LOV RECON_COMPANY_CONCERNED)
   * @param instruction instruction or notation
   * @param insurerFeedback insurer's feedback
   * @param marketingFeedback Marketing's feedback
   * @param disposition disposition (LOV RECON_DISPOSITION)
   * @param forClosure confirmed for closure
   */
  public record FeedbackRequest(
      @Size(max = 40) String companyConcerned,
      @Size(max = 500) String instruction,
      @Size(max = 1000) String insurerFeedback,
      @Size(max = 1000) String marketingFeedback,
      @Size(max = 40) String disposition,
      boolean forClosure) {

    /**
     * The feedback of an item.
     *
     * @param i item
     * @return feedback
     */
    public static FeedbackRequest from(ReconItem i) {
      return new FeedbackRequest(
          i.getCompanyConcerned(),
          i.getInstruction(),
          i.getInsurerFeedback(),
          i.getMarketingFeedback(),
          i.getDisposition(),
          i.isForClosure());
    }

    /**
     * The domain feedback.
     *
     * @return feedback
     */
    public ReconItem.Feedback toFeedback() {
      return new ReconItem.Feedback(
          companyConcerned,
          instruction,
          insurerFeedback,
          marketingFeedback,
          disposition,
          forClosure);
    }
  }

  /**
   * Company concerned, disposition and closure for a selection of items.
   *
   * @param ids items
   * @param companyConcerned company concerned, null to keep
   * @param disposition disposition, null to keep
   * @param forClosure confirmed for closure
   */
  public record BulkFeedbackRequest(
      @NotEmpty List<Long> ids,
      @Size(max = 40) String companyConcerned,
      @Size(max = 40) String disposition,
      boolean forClosure) {}

  /**
   * A manual pairing.
   *
   * @param insurerItemId insurer-only item
   */
  public record PairRequest(@NotNull Long insurerItemId) {}

  /**
   * A comment.
   *
   * @param comment comment, may be null
   */
  public record CommentRequest(@Size(max = 500) String comment) {}

  /**
   * Query of the workbench items (PRCID.013/014/021).
   *
   * @param bucket status bucket (ALL by default)
   * @param q part of the invoice, policy or assured name
   * @param ao account officer
   * @param unit sales unit / AB
   * @param segment market segment
   * @param productLine product line
   * @param page page (0 by default)
   * @param size page size (50 by default)
   */
  public record ItemQuery(
      Bucket bucket,
      String q,
      String ao,
      String unit,
      String segment,
      String productLine,
      Integer page,
      Integer size) {

    private static final int DEFAULT_SIZE = 50;

    /** Defaults. */
    public ItemQuery {
      bucket = bucket == null ? Bucket.ALL : bucket;
      page = page == null ? 0 : Math.max(page, 0);
      size = size == null ? DEFAULT_SIZE : Math.max(size, 1);
    }
  }
}
