package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.ClaimLocation;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Composes the loss advice of one insurer from template {@code BCL_LOSS_ADVICE} (process p.24-25;
 * FR-CL-024): assured, policy / reference number, date and place of loss (the linked locations of a
 * property claim), nature and description, initial loss reserve, assigned adjuster and the insurer's
 * claim numbers known so far. The template wording is a draft until BDOI gives its layout (CLQ22).
 */
@Component
public class LossAdviceDocument {

  private static final String PENDING = "to be advised";

  private final DocTemplateService templates;
  private final DocumentComposer composer;
  private final OrganizationService organizations;
  private final ClaimLocationService locations;
  private final LovService lovs;

  /**
   * Creates the composer.
   *
   * @param templates document templates
   * @param composer PDF composer
   * @param organizations company name
   * @param locations claim locations
   * @param lovs labels of nature of loss and adjusters
   */
  public LossAdviceDocument(
      DocTemplateService templates,
      DocumentComposer composer,
      OrganizationService organizations,
      ClaimLocationService locations,
      LovService lovs) {
    this.templates = templates;
    this.composer = composer;
    this.organizations = organizations;
    this.locations = locations;
    this.lovs = lovs;
  }

  /**
   * The subject and body of an insurer's advice.
   *
   * @param claim claim
   * @param insurer insurer
   * @param lines insurer lines of the claim
   * @param date date (template version in force)
   * @return subject, body and the template version
   */
  public Advice compose(Claim claim, InsurerProfile insurer, List<InsurerClaim> lines, LocalDate date) {
    List<InsurerClaim> own =
        lines.stream().filter(l -> l.getInsurerCode().equals(insurer.getPartyCode())).toList();
    Map<String, Object> values = values(claim, insurer.getName(), own);
    MergedText text = templates.merge(ClaimCodes.LOSS_ADVICE_TEMPLATE, date, values);
    return new Advice(
        insurer.getPartyCode(),
        DocTemplateService.fill(text.title(), values),
        text.text(),
        "Template " + text.code() + " v" + text.versionNo());
  }

  /**
   * The advice as a PDF on the company letterhead.
   *
   * @param claim claim
   * @param advice composed advice
   * @return PDF bytes
   */
  public byte[] pdf(Claim claim, Advice advice) {
    CoverSnapshot cover = claim.getCover();
    LossDetails loss = claim.getLoss();
    DocumentSpec spec =
        new DocumentSpec(
            organizations.getCompany(claim.getCompanyId()).getName(),
            "LOSS ADVICE",
            claim.getClaimNo(),
            List.of(
                new Fields(
                    "Claim",
                    List.of(
                        new Field("Assured", cover.getAssuredName()),
                        new Field("Policy / reference no.", policyNo(cover)),
                        new Field("Account (ARN)", cover.getArn()),
                        new Field("Date of loss", String.valueOf(loss.getLossDate())),
                        new Field("Insurer", advice.insurerCode()))),
                new Text(advice.subject(), advice.body())),
            List.of("Claims Handling"),
            advice.versionTag());
    return composer.pdf(spec);
  }

  private Map<String, Object> values(Claim claim, String insurerName, List<InsurerClaim> own) {
    CoverSnapshot cover = claim.getCover();
    LossDetails loss = claim.getLoss();
    Map<String, Object> values = new HashMap<>();
    values.put("claimNo", claim.getClaimNo());
    values.put("assuredName", cover.getAssuredName());
    values.put("insurerName", insurerName);
    values.put("policyNo", policyNo(cover));
    values.put("arn", cover.getArn());
    values.put("policyYear", cover.getPolicyYear());
    values.put("lossDate", loss.getLossDate());
    values.put("lossPlace", place(claim));
    values.put("lossNature", label(ClaimCodes.LOV_LOSS_NATURE, loss.getLossNature()));
    values.put("lossDescription", loss.getLossDescription());
    values.put("currency", cover.getCurrency());
    values.put("initialReserve", amount(reserve(claim, own)));
    values.put("adjusterName", label(ClaimCodes.LOV_ADJUSTER, adjuster(claim, own)));
    String numbers =
        own.stream()
            .map(InsurerClaim::getInsurerClaimNo)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
    values.put("insurerClaimNos", numbers.isEmpty() ? PENDING : numbers);
    values.put("handlerName", claim.getHandler());
    return values;
  }

  private String place(Claim claim) {
    List<ClaimLocation> linked = locations.ofClaim(claim.getId());
    if (linked.isEmpty()) {
      return claim.getLoss().getLossPlace() == null ? PENDING : claim.getLoss().getLossPlace();
    }
    return linked.stream()
        .map(l -> l.getAddress() + (l.getCity() == null ? "" : ", " + l.getCity()))
        .collect(Collectors.joining("; "));
  }

  private static BigDecimal reserve(Claim claim, List<InsurerClaim> own) {
    BigDecimal lines =
        own.stream()
            .map(InsurerClaim::getReserveAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(null);
    return lines == null ? claim.getLoss().getInitialReserve() : lines;
  }

  private static String adjuster(Claim claim, List<InsurerClaim> own) {
    return own.stream()
        .map(InsurerClaim::getAdjusterCode)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(claim.getProgress().getAdjusterCode());
  }

  private String label(String type, String code) {
    return code == null ? PENDING : lovs.label(type, code);
  }

  private static String policyNo(CoverSnapshot cover) {
    return cover.getPolicyNo() == null ? "Policy number pending" : cover.getPolicyNo();
  }

  private static String amount(BigDecimal value) {
    return value == null
        ? PENDING
        : new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }

  /**
   * A composed advice.
   *
   * @param insurerCode insurer
   * @param subject subject (merged template title)
   * @param body body (merged template text)
   * @param versionTag template version used
   */
  public record Advice(String insurerCode, String subject, String body, String versionTag) {}
}
