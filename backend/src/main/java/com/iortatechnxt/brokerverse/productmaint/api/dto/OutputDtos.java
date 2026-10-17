package com.iortatechnxt.brokerverse.productmaint.api.dto;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutput;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Scheme;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.Signoff;
import com.iortatechnxt.brokerverse.productmaint.service.AdvisoryService.DocumentCheck;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService.ExpiringPackage;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService.RenewalResult;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService.HomeCounts;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService.StageCount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bodies of the comparative, requirements, set-up, advisory, expiry and home endpoints
 * (BRPM.014-019, PMADD03).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class OutputDtos {

  private OutputDtos() {}

  /**
   * A client comparative view to generate.
   *
   * @param title title of the view
   * @param fields field codes, empty for all
   * @param insurers insurer codes, empty for all
   */
  public record ClientViewBody(
      @NotBlank @Size(max = 200) String title, List<String> fields, List<String> insurers) {}

  /**
   * A stored comparative output.
   *
   * @param id id
   * @param roundNo round
   * @param kind MASTER or CLIENT
   * @param parentOutputId master of a client view
   * @param current current master
   * @param title title
   * @param fields fields shown
   * @param insurers insurers shown
   * @param templateVersion template version
   * @param attachmentId stored PDF
   * @param sha256 file hash
   * @param generatedBy who
   * @param generatedAt when
   */
  public record OutputView(
      Long id,
      int roundNo,
      ComparativeOutput.Kind kind,
      Long parentOutputId,
      boolean current,
      String title,
      List<String> fields,
      List<String> insurers,
      String templateVersion,
      Long attachmentId,
      String sha256,
      String generatedBy,
      Instant generatedAt) {

    /**
     * Maps an output.
     *
     * @param o output
     * @return view
     */
    public static OutputView from(ComparativeOutput o) {
      return new OutputView(
          o.getId(),
          o.getRoundNo(),
          o.getKind(),
          o.getParentOutputId(),
          o.isCurrent(),
          o.getTitle(),
          o.getFieldList(),
          o.getInsurerList(),
          o.getTemplateVersion(),
          o.getAttachmentId(),
          o.getSha256(),
          o.getCreatedBy(),
          o.getCreatedAt());
    }
  }

  /**
   * Requirements pack changes.
   *
   * @param scheme rate scheme and computation basis
   * @param dates effectivity and package term
   */
  public record RequirementsBody(Scheme scheme, Dates dates) {}

  /**
   * Requirements status of a request.
   *
   * @param missing what is still missing, empty when complete
   * @param signoffs ManCom decisions, newest first
   */
  public record RequirementsView(List<String> missing, List<SignoffView> signoffs) {}

  /**
   * A ManCom decision.
   *
   * @param id id
   * @param decision SIGNED or RETURNED
   * @param reference sign-off reference
   * @param signedBy ManCom member
   * @param signedAt when
   * @param comment comment
   * @param signedSheetAttachmentId signed sheet or sign-off record
   */
  public record SignoffView(
      Long id,
      String decision,
      String reference,
      String signedBy,
      Instant signedAt,
      String comment,
      Long signedSheetAttachmentId) {

    /**
     * Maps a decision.
     *
     * @param s decision
     * @return view
     */
    public static SignoffView from(Signoff s) {
      return new SignoffView(
          s.getId(),
          s.getDecision(),
          s.getReference(),
          s.getSignedBy(),
          s.getSignedAt(),
          s.getComment(),
          s.getSignedSheetAttachmentId());
    }
  }

  /**
   * The MBS set-up.
   *
   * @param productCode risk code of a new package
   * @param productName name of a new package
   * @param changeSummary change summary
   * @param comment comment
   */
  public record SetupBody(
      @Size(max = 20) String productCode,
      @Size(max = 200) String productName,
      @Size(max = 500) String changeSummary,
      @Size(max = 1000) String comment) {}

  /**
   * Changes of a draft advisory.
   *
   * @param groups recipient groups
   * @param emailTo e-mail addresses
   * @param subject subject
   * @param body text
   */
  public record AdvisoryBody(
      List<String> groups,
      List<String> emailTo,
      @NotBlank @Size(max = 200) String subject,
      @NotBlank @Size(max = 8000) String body) {}

  /**
   * An advisory with its document checklist.
   *
   * @param id id
   * @param requestId request
   * @param productCode product
   * @param versionNo version
   * @param type type
   * @param status status
   * @param groups recipient groups
   * @param emailTo e-mail addresses
   * @param subject subject
   * @param body text
   * @param templateVersion template version
   * @param documents required documents and whether they are attached
   * @param sentBy sender
   * @param sentAt sent
   * @param createdAt drafted
   */
  public record AdvisoryView(
      Long id,
      Long requestId,
      String productCode,
      Integer versionNo,
      Advisory.Type type,
      Advisory.Status status,
      List<String> groups,
      List<String> emailTo,
      String subject,
      String body,
      String templateVersion,
      List<DocumentCheck> documents,
      String sentBy,
      Instant sentAt,
      Instant createdAt) {

    /**
     * Maps an advisory.
     *
     * @param a advisory
     * @param documents its document checklist
     * @return view
     */
    public static AdvisoryView from(Advisory a, List<DocumentCheck> documents) {
      return new AdvisoryView(
          a.getId(),
          a.getRequestId(),
          a.getProductCode(),
          a.getVersionNo(),
          a.getAdvisoryType(),
          a.getStatus(),
          a.getRecipientGroupList(),
          a.getEmailToList(),
          a.getSubject(),
          a.getBody(),
          a.getTemplateVersion(),
          documents,
          a.getSentBy(),
          a.getSentAt(),
          a.getCreatedAt());
    }
  }

  /**
   * A package reaching its end date.
   *
   * @param productCode product
   * @param productName name
   * @param versionNo version in force
   * @param packageEndDate package end date
   * @param anniversaryDate anniversary date
   * @param daysLeft days left
   * @param renewalRequestId open renewal request
   * @param renewalRequestNo its number
   * @param renewalStage its stage
   */
  public record ExpiryRow(
      String productCode,
      String productName,
      int versionNo,
      LocalDate packageEndDate,
      LocalDate anniversaryDate,
      long daysLeft,
      Long renewalRequestId,
      String renewalRequestNo,
      RequestStage renewalStage) {

    /**
     * Maps an expiring package.
     *
     * @param e expiring package
     * @return row
     */
    public static ExpiryRow from(ExpiringPackage e) {
      ProductVersionView v = e.version();
      PackageRequest r = e.renewal();
      return new ExpiryRow(
          v.productCode(),
          v.productName(),
          v.versionNo(),
          v.dates().packageEndDate(),
          v.dates().anniversaryDate(),
          e.daysLeft(),
          r == null ? null : r.getId(),
          r == null ? null : r.getRequestNo(),
          r == null ? null : r.getStatus());
    }
  }

  /**
   * Packages to renew.
   *
   * @param companyId company
   * @param productCodes products
   */
  public record RenewalBody(Long companyId, List<String> productCodes) {}

  /**
   * Outcome of a renewal generation.
   *
   * @param productCode product
   * @param requestId request
   * @param requestNo request number
   * @param created created now (false: an open renewal existed)
   */
  public record RenewalView(String productCode, Long requestId, String requestNo, boolean created) {

    /**
     * Maps a result.
     *
     * @param r result
     * @return view
     */
    public static RenewalView from(RenewalResult r) {
      return new RenewalView(
          r.productCode(), r.request().getId(), r.request().getRequestNo(), r.created());
    }
  }

  /**
   * Product Maintenance home counts (BRPM.019).
   *
   * @param stages requests by stage
   * @param expiring packages expiring within 30 / 60 / 90 days
   * @param advisoriesPending draft advisories
   * @param outputsThisWeek comparative outputs of the last seven days
   */
  public record CountsView(
      List<StageCount> stages,
      Map<Integer, Integer> expiring,
      long advisoriesPending,
      long outputsThisWeek) {

    /**
     * Maps the counts.
     *
     * @param c counts
     * @return view
     */
    public static CountsView from(HomeCounts c) {
      return new CountsView(c.stages(), c.expiring(), c.advisoriesPending(), c.outputsThisWeek());
    }
  }
}
