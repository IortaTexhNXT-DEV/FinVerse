package com.iortatechnxt.brokerverse.migration.intake.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.ExtractStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRule;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRuleRepository;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.service.ObjectRegisterService;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receives source extracts (BRID 1.1b; DATA_MIGRATION_DESIGN sections 5 and 6; FR-DM-010): the data
 * file and its control file are stored in the migration bucket (5-day lifecycle), checked against
 * each other and against the layout in force, and on success masked outside production and staged.
 * A failing extract is REJECTED with the reason and nothing is staged; the Data Migration Lead is
 * alerted.
 */
@Service
@Transactional
public class IntakeService {

  private static final int G_LAYOUT = 1;
  private static final int G_SOURCE = 2;
  private static final int G_DAY = 3;
  private static final int G_SEQ = 4;

  private static final Pattern FILE_NAME =
      Pattern.compile("^([A-Z0-9]+)_([A-Z]+)_(\\d{8})_(\\d{2})\\.(CSV|XLSX)$");
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
  private static final String CSV_TYPE = "text/csv";
  private static final String XLSX_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String DELTA = "DELTA";
  private static final String DUPLICATE = "MIG_DUPLICATE_FILE";
  private static final String AS_OF_ORDER = "MIG_ASOF_ORDER";

  private final ObjectRegisterService register;
  private final LayoutService layouts;
  private final MigExtractRepository extracts;
  private final MaskingRuleRepository masking;
  private final BulkFileReader reader;
  private final Masker masker;
  private final StagingWriter staging;
  private final StoredFileService files;
  private final DocumentNumberService numbers;
  private final MigrationParameters parameters;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final ObjectMapper json;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param register data objects
   * @param layouts layouts
   * @param extracts extracts
   * @param masking masking rules
   * @param reader file reader
   * @param masker masking
   * @param staging staging writer
   * @param files file store
   * @param numbers document numbers
   * @param parameters migration parameters
   * @param alerts alerts
   * @param audit audit trail
   * @param json JSON mapper
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public IntakeService(
      ObjectRegisterService register,
      LayoutService layouts,
      MigExtractRepository extracts,
      MaskingRuleRepository masking,
      BulkFileReader reader,
      Masker masker,
      StagingWriter staging,
      StoredFileService files,
      DocumentNumberService numbers,
      MigrationParameters parameters,
      AlertService alerts,
      AuditTrailService audit,
      ObjectMapper json,
      CurrentUser currentUser,
      Clock clock) {
    this.register = register;
    this.layouts = layouts;
    this.extracts = extracts;
    this.masking = masking;
    this.reader = reader;
    this.masker = masker;
    this.staging = staging;
    this.files = files;
    this.numbers = numbers;
    this.parameters = parameters;
    this.alerts = alerts;
    this.audit = audit;
    this.json = json;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Receives an extract with its control file.
   *
   * @param companyId company
   * @param upload the files and the mode
   * @return the extract, STAGED or REJECTED
   */
  public MigExtract receive(Long companyId, Upload upload) {
    Name name = parseName(upload.fileName());
    Layout layout = layouts.requireCurrent(name.layout());
    MigDataObject object = register.get(layout.getObjectCode());
    requireReceivable(object, name, upload.objectCode());
    boolean production = parameters.production();
    if (!production) {
      masker.requireConfigured();
    }
    String sha = Sha256.hex(upload.content());
    ControlFile control =
        ControlFile.of(reader.read(csvName(upload.controlName()), upload.control()));
    LocalDateTime asOf = control.asOf() != null ? control.asOf() : name.day().atStartOfDay();
    MigExtract extract =
        extracts.save(
            new MigExtract(
                companyId,
                numbers.next("MGX-" + BusinessClock.today(clock).getYear()),
                new MigExtract.FileIdentity(
                    object.getCode(),
                    layout.getCode(),
                    layout.getVersionNo(),
                    name.source(),
                    asOf,
                    name.sequence(),
                    mode(upload.mode()),
                    upload.fileName(),
                    upload.controlName(),
                    sha),
                currentUser.username(),
                clock.instant()));
    extract.control(
        control.rowCount(),
        controlTotals(control),
        control.hashTotal(),
        control.extractedAt(),
        control.extractedBy());
    store(companyId, extract, upload);
    Optional<IntakeChecks.Failure> failure = check(extract, layout, control, upload);
    if (failure.isPresent()) {
      return reject(extract, failure.get(), 0);
    }
    return stage(extract, layout, upload, production);
  }

  private Optional<IntakeChecks.Failure> check(
      MigExtract extract, Layout layout, ControlFile control, Upload upload) {
    Optional<IntakeChecks.Failure> order = identityChecks(extract, control);
    if (order.isPresent()) {
      return order;
    }
    ParsedFile parsed = reader.read(upload.fileName(), upload.content());
    List<String> columns =
        layouts.columns(layout.getId()).stream().map(LayoutColumn::getName).toList();
    return IntakeChecks.run(extract.getSha256(), control, layout, columns, parsed);
  }

  private Optional<IntakeChecks.Failure> identityChecks(MigExtract extract, ControlFile control) {
    if (control.layout() != null && !control.layout().equals(extract.getLayoutCode())) {
      return Optional.of(
          new IntakeChecks.Failure(
              IntakeChecks.LAYOUT,
              "The control file is for layout "
                  + control.layout()
                  + ", not "
                  + extract.getLayoutCode()));
    }
    List<MigExtract> same =
        extracts.findByObjectCodeAndSha256AndStatusIn(
            extract.getObjectCode(),
            extract.getSha256(),
            EnumSet.of(ExtractStatus.CHECKED, ExtractStatus.STAGED, ExtractStatus.PURGED));
    Optional<MigExtract> earlier =
        same.stream().filter(e -> !e.getId().equals(extract.getId())).findFirst();
    if (earlier.isPresent()) {
      return Optional.of(
          new IntakeChecks.Failure(
              DUPLICATE,
              "This file was already received as extract " + earlier.get().getExtractNo()));
    }
    if (DELTA.equals(extract.getMode())) {
      Optional<MigExtract> last =
          extracts
              .findByCompanyIdAndLayoutCodeAndStatusInOrderByAsOfDesc(
                  extract.getCompanyId(),
                  extract.getLayoutCode(),
                  EnumSet.of(ExtractStatus.STAGED, ExtractStatus.PURGED))
              .stream()
              .findFirst();
      if (last.isPresent() && !extract.getAsOf().isAfter(last.get().getAsOf())) {
        return Optional.of(
            new IntakeChecks.Failure(
                AS_OF_ORDER, "The as-of date is earlier than the last extract of this object"));
      }
    }
    return Optional.empty();
  }

  private MigExtract stage(MigExtract extract, Layout layout, Upload upload, boolean production) {
    ParsedFile parsed = reader.read(upload.fileName(), upload.content());
    List<MaskingRule> rules =
        production ? List.of() : masking.findByLayoutCodeAndActiveTrue(layout.getCode());
    List<String> keys = layout.keys();
    List<StagingWriter.Row> rows = new ArrayList<>(parsed.rows().size());
    for (ParsedFile.RawRow raw : parsed.rows()) {
      Map<String, String> values =
          rules.isEmpty() ? raw.values() : masker.mask(raw.values(), rules);
      String key =
          keys.stream().map(k -> values.getOrDefault(k, "")).collect(Collectors.joining("|"));
      rows.add(new StagingWriter.Row(raw.rowNo(), key, values, staging.rowHash(values)));
    }
    int written =
        staging.write(
            new StagingWriter.Target(extract.getId(), extract.getObjectCode(), layout.getCode()),
            rows);
    extract.staged(parsed.rows().size(), written, !rules.isEmpty(), clock.instant());
    audit.record(
        MigrationCodes.ENTITY_EXTRACT,
        extract.getExtractNo(),
        AuditAction.CREATE,
        "Staged "
            + written
            + " rows of "
            + extract.getFileName()
            + " (SHA-256 "
            + extract.getSha256()
            + (rules.isEmpty() ? ")" : ", masked)"));
    return extract;
  }

  private MigExtract reject(MigExtract extract, IntakeChecks.Failure failure, int parsed) {
    extract.reject(failure.code(), failure.message(), parsed, clock.instant());
    alerts.raise(
        MigrationCodes.ALERT_EXTRACT_REJECTED,
        new AlertFacts(
            extract.getCompanyId(),
            null,
            MigrationCodes.ENTITY_EXTRACT,
            extract.getExtractNo(),
            "Extract " + extract.getFileName() + " rejected: " + failure.message(),
            null,
            MigrationCodes.ALERT_EXTRACT_REJECTED + ":" + extract.getExtractNo()));
    audit.record(
        MigrationCodes.ENTITY_EXTRACT,
        extract.getExtractNo(),
        AuditAction.REJECT,
        extract.getFileName() + ": " + failure.code() + " " + failure.message());
    return extract;
  }

  private void store(Long companyId, MigExtract extract, Upload upload) {
    FileOwner owner =
        new FileOwner(companyId, MigrationCodes.ENTITY_EXTRACT, String.valueOf(extract.getId()));
    Long data =
        files
            .storeChecked(
                new StoreRequest(
                    owner,
                    null,
                    MigrationCodes.RECORD_CLASS_EXTRACT,
                    upload.fileName(),
                    upload.content(),
                    null),
                upload.fileName().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? XLSX_TYPE : CSV_TYPE,
                FileOrigin.UPLOADED)
            .getId();
    Long control =
        files
            .storeChecked(
                new StoreRequest(
                    owner,
                    null,
                    MigrationCodes.RECORD_CLASS_EXTRACT,
                    upload.controlName(),
                    upload.control(),
                    null),
                CSV_TYPE,
                FileOrigin.UPLOADED)
            .getId();
    extract.files(data, control);
  }

  private String controlTotals(ControlFile control) {
    try {
      return json.writeValueAsString(control.amounts());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("The control totals cannot be kept", e);
    }
  }

  private static void requireReceivable(MigDataObject object, Name name, String expectedObject) {
    if (expectedObject != null
        && !expectedObject.isBlank()
        && !expectedObject.equals(object.getCode())) {
      throw new BusinessRuleException(
          "MIG_FILE_OBJECT",
          "File layout "
              + name.layout()
              + " belongs to object "
              + object.getCode()
              + ", not "
              + expectedObject);
    }
    if (!object.loadable() && !object.archive()) {
      throw new BusinessRuleException(
          "MIG_OBJECT_NOT_DECIDED",
          "Object "
              + object.getCode()
              + " is not decided for migration; extracts cannot be received");
    }
    if (!object.sources().isEmpty() && !object.sources().contains(name.source())) {
      throw new BusinessRuleException(
          "MIG_FILE_SOURCE",
          "Source system " + name.source() + " is not a source of object " + object.getCode());
    }
  }

  private static String mode(String mode) {
    return DELTA.equalsIgnoreCase(mode) ? DELTA : "FULL";
  }

  private static String csvName(String controlName) {
    return controlName != null && controlName.toLowerCase(Locale.ROOT).endsWith(".xlsx")
        ? controlName
        : "control.csv";
  }

  /**
   * Parses a data file name {@code <LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv|xlsx}.
   *
   * @param fileName file name
   * @return its parts
   */
  static Name parseName(String fileName) {
    Matcher m =
        FILE_NAME.matcher(fileName == null ? "" : fileName.strip().toUpperCase(Locale.ROOT));
    if (!m.matches()) {
      throw new BusinessRuleException(
          "MIG_FILE_NAME",
          "Name the file <LAYOUT>_<SOURCE>_<yyyyMMdd>_<nn>.csv or .xlsx, for example F01C_EBIX_20271231_01.csv");
    }
    try {
      return new Name(
          m.group(G_LAYOUT),
          m.group(G_SOURCE),
          LocalDate.parse(m.group(G_DAY), DAY),
          Integer.parseInt(m.group(G_SEQ)));
    } catch (DateTimeParseException e) {
      throw new BusinessRuleException(
          "MIG_FILE_NAME", "The date in the file name is not a valid yyyyMMdd date", e);
    }
  }

  /**
   * An extract.
   *
   * @param extractNo number
   * @return extract
   */
  @Transactional(readOnly = true)
  public MigExtract get(String extractNo) {
    return extracts
        .findByExtractNo(extractNo)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_EXTRACT, extractNo));
  }

  /**
   * Extracts of a company, newest first.
   *
   * @param companyId company
   * @param objectCode object filter, null for all
   * @param pageable page
   * @return extracts
   */
  @Transactional(readOnly = true)
  public Page<MigExtract> search(Long companyId, String objectCode, Pageable pageable) {
    return objectCode == null || objectCode.isBlank()
        ? extracts.findByCompanyIdOrderByIdDesc(companyId, pageable)
        : extracts.findByCompanyIdAndObjectCodeOrderByIdDesc(companyId, objectCode, pageable);
  }

  /**
   * An upload.
   *
   * @param objectCode object chosen on the screen (checked against the layout), may be null
   * @param mode FULL or DELTA
   * @param fileName data file name
   * @param content data file
   * @param controlName control file name
   * @param control control file
   */
  public record Upload(
      String objectCode,
      String mode,
      String fileName,
      byte[] content,
      String controlName,
      byte[] control) {}

  /**
   * Parts of a file name.
   *
   * @param layout layout code
   * @param source source system
   * @param day as-of date
   * @param sequence sequence of the day
   */
  record Name(String layout, String source, LocalDate day, int sequence) {}
}
