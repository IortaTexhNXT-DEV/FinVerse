package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIntakeRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyOrigin;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.Upserted;
import com.iortatechnxt.brokerverse.submitted.service.SbmPolicyChecks;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.util.ArrayList;
import java.util.List;

/**
 * The upload of a source file of submitted policies (BRIDSP-01; FRS FR-SP-001): one handler per
 * approved source, declared in {@link SourceIntakeHandlers}. Each valid row creates or updates one
 * masterlist record on its natural key (status RECEIVED); the rows of a file form one intake run,
 * which starts the processing run of its records once committed. The same file is refused a second
 * time.
 */
public class SourceIntakeHandler implements BulkImportHandler {

  /** Row created a record. */
  public static final String CREATED = "CREATED";

  /** Row updated a record. */
  public static final String UPDATED = "UPDATED";

  /** Row changed nothing. */
  public static final String DUPLICATE = "DUPLICATE";

  private final SourceSpec spec;
  private final MasterlistService masterlist;
  private final IntakeRunService runs;
  private final LovService lovs;

  /**
   * Creates the handler of a source.
   *
   * @param spec source code, handler code, title, segment and business type
   * @param masterlist masterlist
   * @param runs intake runs
   * @param lovs lists of values (segment)
   */
  public SourceIntakeHandler(
      SourceSpec spec, MasterlistService masterlist, IntakeRunService runs, LovService lovs) {
    this.spec = spec;
    this.masterlist = masterlist;
    this.runs = runs;
    this.lovs = lovs;
  }

  @Override
  public String code() {
    return spec.handlerCode();
  }

  @Override
  public String title() {
    return spec.title();
  }

  @Override
  public String permission() {
    return "SBM_INTAKE";
  }

  @Override
  public List<BulkColumn> columns() {
    return SourceColumns.columns();
  }

  @Override
  public String instructions() {
    return "One row per policy. A row with the PN (or policy number), segment, business type and"
        + " expiry of a policy already in the masterlist updates it. The same file cannot be loaded"
        + " twice.";
  }

  @Override
  public String sanitize(String header, String value) {
    return SourceColumns.sanitize(header, value);
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    String key = row.text(SourceColumns.PN);
    if (key == null) {
      key = row.text("Policy No");
    }
    return key == null ? null : key + "|" + row.text(SourceColumns.EXPIRY);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String type = row.text(SourceColumns.BUSINESS_TYPE);
    if (!SourceColumns.validBusinessType(type)) {
      errors.add("Business Type must be NB or RB");
      return errors;
    }
    SbmPolicyData data = SourceColumns.data(row, spec.segment(), spec.businessType());
    if (data.segment() != null
        && lovs.activeValues(SubmittedCodes.LOV_SEGMENT, context.businessDate()).stream()
            .noneMatch(v -> v.getCode().equals(data.segment()))) {
      errors.add("Segment " + row.text(SourceColumns.SEGMENT) + " is not a segment");
      return errors;
    }
    errors.addAll(SbmPolicyChecks.problems(data, List.of()));
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    SbmIntakeRun run = runs.runOf(context, spec.sourceCode());
    Upserted done =
        masterlist.upsert(
            context.companyId(),
            SourceColumns.data(row, spec.segment(), spec.businessType()),
            new UpsertContext(
                new SbmPolicyOrigin(
                    spec.sourceCode(),
                    run.getId(),
                    context.businessDate(),
                    SbmPolicyStatus.RECEIVED),
                SbmHistorySource.INTAKE,
                run.getRunNo(),
                row.text(SourceColumns.HANDLER)));
    run.count(done.outcome().name());
    return new BulkOutcome(done.policy().getSbmNo(), done.outcome().name());
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of(CREATED, UPDATED, DUPLICATE);
  }

  @Override
  public void afterCommit(BulkContext context, int committed, int failed) {
    runs.complete(context, failed);
  }

  /**
   * What a source handler loads.
   *
   * @param sourceCode source register code
   * @param handlerCode bulk handler code
   * @param title screen title
   * @param segment segment of the source, null when the file gives it
   * @param businessType business type of the source, null when the file gives it
   */
  public record SourceSpec(
      String sourceCode, String handlerCode, String title, String segment, String businessType) {}
}
