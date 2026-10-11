package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateTags;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_ACCOUNT_TAGS} (FRRN.014.04, FRRN.014.05): the For Booking Only tag with
 * the policy and official receipt numbers, the Direct-to-Insurer Payment tag with its option and
 * the amortized premium indicator of several renewal accounts, each account processed
 * independently.
 */
@Component
public class AccountTagsHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_ACCOUNT_TAGS";

  private static final String REFERENCE = "Reference Number";
  private static final String BOOKING_ONLY = "For Booking Only";
  private static final String POLICY = "Policy Number";
  private static final String OR = "OR Number";
  private static final String DTI = "Direct-to-Insurer Payment";
  private static final String AMORTIZED = "Amortized";
  private static final String YES = "Yes";
  private static final String NO = "No";
  private static final Map<String, String> DTI_OPTIONS =
      Map.of(
          "With Blanket Approval", CandidateTags.BLANKET,
          "Route for UH Approval", CandidateTags.UNIT_HEAD,
          "None", "");

  private final RenewalCandidateRepository candidates;
  private final BookingOnlyAccounts bookingOnly;
  private final DirectToInsurerTags directToInsurer;

  /**
   * Creates the handler.
   *
   * @param candidates renewals
   * @param bookingOnly For Booking Only tag
   * @param directToInsurer Direct-to-Insurer Payment tag
   */
  public AccountTagsHandler(
      RenewalCandidateRepository candidates,
      BookingOnlyAccounts bookingOnly,
      DirectToInsurerTags directToInsurer) {
    this.candidates = candidates;
    this.bookingOnly = bookingOnly;
    this.directToInsurer = directToInsurer;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - account tags";
  }

  @Override
  public String permission() {
    return Permission.RNW_DISPOSE.name();
  }

  @Override
  public String filledBy() {
    return "The Account Officer, for the renewal accounts to tag";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Accounts, button Upload Account Tags";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            REFERENCE, "Renewal reference number of the account", "RNW-2026-000101"),
        BulkColumn.optional(
                BOOKING_ONLY, "Yes to tag the account For Booking Only, No to untag", YES)
            .values(YES, NO),
        BulkColumn.optional(
            POLICY, "Policy number of an account For Booking Only", "MC-PC-2026-0012345"),
        BulkColumn.optional(
            OR, "Official receipt number of an account For Booking Only", "OR-000123"),
        BulkColumn.optional(DTI, "Direct-to-Insurer Payment option", "")
            .values("With Blanket Approval", "Route for UH Approval", "None"),
        BulkColumn.optional(AMORTIZED, "Yes when the premium is amortized in the loan", NO)
            .values(YES, NO));
  }

  @Override
  public Set<String> optionalHeaders() {
    return Set.of(BOOKING_ONLY, POLICY, OR, DTI, AMORTIZED);
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("TAGGED");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    Optional<RenewalCandidate> c = candidate(row, context);
    if (c.isEmpty()) {
      errors.add("Record not found");
      return errors;
    }
    if (c.get().getMarketingLockedAt() != null) {
      errors.add("The renewal account is locked");
    }
    String dti = row.text(DTI);
    if (dti != null && !DTI_OPTIONS.containsKey(dti)) {
      errors.add(
          "Direct-to-Insurer Payment must be With Blanket Approval, Route for UH Approval or None");
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    RenewalCandidate c = candidate(row, context).orElseThrow();
    String ref = c.getRenewalRef();
    if (row.text(BOOKING_ONLY) != null) {
      bookingOnly.tagOne(
          context.companyId(),
          ref,
          YES.equals(row.text(BOOKING_ONLY)),
          new BookingOnlyAccounts.Details(row.text(POLICY), row.text(OR)));
    }
    if (row.text(DTI) != null) {
      directToInsurer.tag(context.companyId(), ref, DTI_OPTIONS.get(row.text(DTI)));
    }
    if (row.text(AMORTIZED) != null) {
      RenewalCandidate fresh = candidates.findById(c.getId()).orElseThrow();
      fresh.getPlacement().getTags().amortized(YES.equals(row.text(AMORTIZED)));
      candidates.save(fresh);
    }
    return new BulkOutcome(ref, "TAGGED");
  }

  private Optional<RenewalCandidate> candidate(BulkRow row, BulkContext context) {
    String ref = row.text(REFERENCE);
    return ref == null
        ? Optional.empty()
        : candidates.findByCompanyIdAndRenewalRef(context.companyId(), ref.strip());
  }
}
