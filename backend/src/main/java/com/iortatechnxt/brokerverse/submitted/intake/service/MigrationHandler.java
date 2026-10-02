package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyOrigin;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStatusMap;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStatusMapRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.Upserted;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyChecks;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Migration of the Excel masterlists (BRIDSP-33; FRS FR-SP-004): each valid row of a legacy file
 * becomes a masterlist record flagged Migrated, with its legacy reference, its original date
 * received and its legacy status mapped to a status and bucket (status map). Invalid rows stay in
 * the upload's error log, which the report {@code SBM-MIGRATION-ERRORS} lists with their reason.
 * The migration can be run again: a row with a legacy reference already migrated updates that
 * record. The layout of the file (parameter {@code layout}: NB_MOTOR, RB_MOTOR, FIRE, NONCBG) sets
 * the segment and business type the rows leave blank.
 */
@Component
public class MigrationHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "SBM_MIGRATION";

  /** Parameter: legacy layout. */
  public static final String LAYOUT = "layout";

  /** Legacy reference column. */
  public static final String LEGACY_REF = "Legacy Reference";

  /** Legacy status column. */
  public static final String LEGACY_STATUS = "Legacy Status";

  private static final String DATE_RECEIVED = "Date Received";

  private static final Map<String, String[]> LAYOUTS =
      Map.of(
          "NB_MOTOR", new String[] {SubmittedCodes.CBG_MOTOR, "NB"},
          "RB_MOTOR", new String[] {SubmittedCodes.CBG_MOTOR, "RB"},
          "FIRE", new String[] {SubmittedCodes.CBG_FIRE, "NB"},
          "NONCBG", new String[] {SubmittedCodes.NONCBG_CORPORATE, "NB"});

  private final MasterlistService masterlist;
  private final SbmPolicyRepository policies;
  private final SbmStatusMapRepository statusMap;

  /**
   * Creates the handler.
   *
   * @param masterlist masterlist
   * @param policies masterlist records (legacy reference)
   * @param statusMap legacy status map
   */
  public MigrationHandler(
      MasterlistService masterlist,
      SbmPolicyRepository policies,
      SbmStatusMapRepository statusMap) {
    this.masterlist = masterlist;
    this.policies = policies;
    this.statusMap = statusMap;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Migration of the Excel masterlists";
  }

  @Override
  public String permission() {
    return "SBM_MIGRATE";
  }

  @Override
  public String filledBy() {
    return "The Submitted Policies team, from its Excel masterlists";
  }

  @Override
  public String uploadPath() {
    return "Submitted Policies > Upload & Intake, button Migrate Masterlist (choose the layout)";
  }

  @Override
  public List<BulkColumn> columns() {
    List<BulkColumn> c = new ArrayList<>();
    c.add(
        BulkColumn.required(
            LEGACY_REF, "Reference of the policy in the Excel masterlist", "NBM-2025-0457"));
    c.add(
        BulkColumn.required(
                LEGACY_STATUS, "Submission status in the Excel masterlist", "For Renewal")
            .allowed("A status of the legacy status map (Submitted Policies Setup)"));
    c.add(
        new BulkColumn(
            DATE_RECEIVED, "Date the policy was received", true, Type.DATE, "2025-06-15"));
    c.addAll(SourceColumns.columns());
    return List.copyOf(c);
  }

  @Override
  public String instructions() {
    return "Choose the layout of the Excel masterlist (NB Motor, RB Motor, Fire or Non-CBG). Each"
        + " status must be in the legacy status map of Submitted Policies Setup. A file can be"
        + " loaded again: rows already migrated are updated, not duplicated.";
  }

  @Override
  public String sanitize(String header, String value) {
    if (LEGACY_STATUS.equals(header)) {
      return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
    return SourceColumns.sanitize(header, value);
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(LEGACY_REF);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String layout = context.parameter(LAYOUT);
    if (layout != null && !layout.isBlank() && !LAYOUTS.containsKey(layout)) {
      errors.add("Choose the layout NB Motor, RB Motor, Fire or Non-CBG");
      return errors;
    }
    if (statusMap.findByLegacyStatus(row.text(LEGACY_STATUS)).isEmpty()) {
      errors.add("Status " + row.text(LEGACY_STATUS) + " has no mapping");
    }
    if (!SourceColumns.validBusinessType(row.text(SourceColumns.BUSINESS_TYPE))) {
      errors.add("Business Type must be NB or RB");
      return errors;
    }
    errors.addAll(SbmPolicyChecks.problems(data(row, context), List.of()));
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    SbmStatusMap mapped = statusMap.findByLegacyStatus(row.text(LEGACY_STATUS)).orElseThrow();
    String legacyRef = row.text(LEGACY_REF);
    SbmPolicyData data = data(row, context);
    Optional<SbmPolicy> earlier =
        policies.findFirstByCompanyIdAndLegacyRef(context.companyId(), legacyRef);
    if (earlier.isPresent()) {
      SbmPolicy p = earlier.get();
      if (p.getStatus().isProcessable()) {
        masterlist.write(p, data, SbmHistorySource.MIGRATION, context.jobNo());
      }
      return new BulkOutcome(p.getSbmNo(), SourceIntakeHandler.UPDATED);
    }
    Upserted done =
        masterlist.upsert(
            context.companyId(),
            data,
            new UpsertContext(
                new SbmPolicyOrigin(
                    SubmittedCodes.SOURCE_MIGRATION,
                    null,
                    row.date(DATE_RECEIVED),
                    mapped.getStatus()),
                SbmHistorySource.MIGRATION,
                context.jobNo(),
                row.text(SourceColumns.HANDLER)));
    SbmPolicy p = done.policy();
    p.migratedFrom(legacyRef);
    if (mapped.getBucket() != null) {
      p.bucket(mapped.getBucket(), null, null);
    }
    return new BulkOutcome(p.getSbmNo(), done.outcome().name());
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of(
        SourceIntakeHandler.CREATED, SourceIntakeHandler.UPDATED, SourceIntakeHandler.DUPLICATE);
  }

  private static SbmPolicyData data(BulkRow row, BulkContext context) {
    String[] layout = LAYOUTS.getOrDefault(context.parameter(LAYOUT), new String[] {null, null});
    return SourceColumns.data(row, layout[0], layout[1]);
  }
}
