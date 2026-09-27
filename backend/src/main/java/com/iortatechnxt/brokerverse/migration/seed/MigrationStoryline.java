package com.iortatechnxt.brokerverse.migration.seed;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.migration.intake.domain.ExtractStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.service.IntakeService;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.load.service.LoadRunner;
import com.iortatechnxt.brokerverse.migration.load.service.ValidationService;
import com.iortatechnxt.brokerverse.migration.recon.domain.MigReconRun;
import com.iortatechnxt.brokerverse.migration.recon.service.ReconciliationService;
import com.iortatechnxt.brokerverse.migration.signoff.service.SignoffService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The SIT/UAT storyline of the data migration (DATA_MIGRATION_DESIGN sections 24 and 25, wave
 * DM3): the seed extracts of {@code db/seed/migration/} go through the real pipeline in load order
 * - intake with a control file, batch, validation (G3, Data Steward), load approval (G4, Data
 * Migration Lead), load, reconciliation (G5) and acceptance (G6, business owner and lead) - each
 * step as the SIT/UAT user who does it. Reference data, a client, a policy header, an open legacy
 * invoice, a legacy unapplied payment and an archive record result, exactly as in a mock run.
 * Used by the seed runner and by the end-to-end test.
 */
public class MigrationStoryline {

  /** The seed extracts in load order: object and its extract files. */
  static final List<Step> STEPS =
      List.of(
          new Step("R01", List.of("R01_EBIX_20271231_01.csv")),
          new Step("R02", List.of("R02_EBIX_20271231_01.csv")),
          new Step("R03", List.of("R03_EBIX_20271231_01.csv")),
          new Step("R04", List.of("R04_EBIX_20271231_01.csv")),
          new Step("R05", List.of("R05_EBIX_20271231_01.csv")),
          new Step("C01", List.of("C01_EBIX_20271231_01.csv")),
          new Step("P01", List.of("P01_EBIX_20271231_01.csv", "P01S_EBIX_20271231_01.csv")),
          new Step(
              "F01",
              List.of(
                  "F01_EBIX_20271231_01.csv",
                  "F01S_EBIX_20271231_01.csv",
                  "F01C_EBIX_20271231_01.csv")),
          new Step("F02", List.of("F02_EBIX_20271231_01.csv")),
          new Step("H01", List.of("H01_EBIX_20271231_01.csv")));

  /** Legacy client number of the storyline (idempotency). */
  public static final String CLIENT = "E970001";

  /** Legacy invoice number of the storyline. */
  public static final String INVOICE = "I97000011";

  private static final String FOLDER = "db/seed/migration/";
  private static final String OPERATOR = "migops";
  private static final String STEWARD = "migsteward";
  private static final String OWNER = "migowner";
  private static final String LEAD = "miglead";
  private static final String RECON = "migrecon";

  private final Services services;
  private final JdbcTemplate jdbc;
  private final RunAs as;

  /**
   * Creates the storyline.
   *
   * @param services pipeline services
   * @param jdbc JDBC (layouts, idempotency)
   * @param as runs a step as a SIT/UAT user
   */
  public MigrationStoryline(Services services, JdbcTemplate jdbc, RunAs as) {
    this.services = services;
    this.jdbc = jdbc;
    this.as = as;
  }

  /**
   * Whether the storyline was already loaded in a company.
   *
   * @param companyId company
   * @return true when its client is migrated
   */
  public boolean loaded(Long companyId) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from mig_key_xref where company_id = ? and object_code = 'C01'"
                + " and legacy_key = ? and rolled_back_at is null",
            Integer.class,
            companyId,
            CLIENT);
    return n != null && n > 0;
  }

  /**
   * Runs the storyline.
   *
   * @param companyId company
   * @return the batches, by object
   */
  public Map<String, String> run(Long companyId) {
    Map<String, String> batches = new LinkedHashMap<>();
    for (Step step : STEPS) {
      as.as(
          OWNER,
          () ->
              services.signoffs().signMapping(companyId, step.object(), true, "Seed maps reviewed"));
      List<String> extracts = new ArrayList<>();
      for (String file : step.files()) {
        extracts.add(receive(companyId, step.object(), file));
      }
      batches.put(step.object(), loadAndAccept(companyId, step.object(), extracts));
    }
    return batches;
  }

  private String receive(Long companyId, String object, String file) {
    String layout = file.substring(0, file.indexOf('_'));
    byte[] content = extract(layout, read(FOLDER + file));
    MigExtract e =
        as.as(
            OPERATOR,
            () ->
                services
                    .intake()
                    .receive(
                        companyId,
                        new IntakeService.Upload(
                            object,
                            "FULL",
                            file,
                            content,
                            file.replace(".csv", "_CONTROL.csv"),
                            control(object, layout, file, content))));
    if (e.getStatus() == ExtractStatus.REJECTED) {
      throw new BusinessRuleException(
          "MIG_SEED_REJECTED", "Seed extract " + file + " rejected: " + e.getRejectMessage());
    }
    return e.getExtractNo();
  }

  private String loadAndAccept(Long companyId, String object, List<String> extracts) {
    MigBatch planned = as.as(OPERATOR, () -> services.plans().plan(companyId, object, extracts));
    String batchNo = planned.getBatchNo();
    as.as(OPERATOR, () -> services.validation().validate(batchNo));
    as.as(STEWARD, () -> services.signoffs().signValidation(batchNo, true, "Seed rows valid"));
    as.as(LEAD, () -> services.signoffs().approveLoad(batchNo, "Seed load"));
    as.as(OPERATOR, () -> services.runner().load(batchNo));
    MigReconRun run = as.as(RECON, () -> services.recon().reconcile(batchNo));
    if (run.getBreakCount() > 0) {
      throw new BusinessRuleException(
          "MIG_SEED_BREAKS", "Seed batch " + batchNo + " has reconciliation breaks");
    }
    as.as(RECON, () -> services.signoffs().signReconciliation(batchNo, true, "Reconciled"));
    as.as(
        OWNER,
        () -> services.signoffs().signAcceptance(batchNo, "DATA_OWNER", true, "Accepted"));
    as.as(
        LEAD,
        () ->
            services.signoffs().signAcceptance(batchNo, "DATA_MIGRATION_LEAD", true, "Accepted"));
    return batchNo;
  }

  /** The seed rows laid out in the columns of the layout in force. */
  private byte[] extract(String layout, String seed) {
    List<String> columns = columns(layout);
    List<String> lines = seed.lines().filter(l -> !l.isBlank()).toList();
    List<String> header = List.of(lines.get(0).split(",", -1));
    StringBuilder out = new StringBuilder(String.join(",", columns)).append('\n');
    for (String line : lines.subList(1, lines.size())) {
      List<String> cells = List.of(line.split(",", -1));
      List<String> row = new ArrayList<>();
      for (String c : columns) {
        int i = header.indexOf(c);
        row.add(i < 0 || i >= cells.size() ? "" : cells.get(i));
      }
      out.append(String.join(",", row)).append('\n');
    }
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  private List<String> columns(String layout) {
    return jdbc.queryForList(
        "select c.name from mig_layout_column c join mig_layout l on l.id = c.layout_id"
            + " where l.code = ? and l.status = 'FROZEN' order by c.seq",
        String.class,
        layout);
  }

  /** The control file: row count, hash total and SHA-256 of the data file. */
  private byte[] control(String object, String layout, String file, byte[] content) {
    Map<String, Object> l =
        jdbc.queryForMap(
            "select hash_rule, coalesce(hash_columns, key_columns) as cols from mig_layout"
                + " where code = ? and status = 'FROZEN'",
            layout);
    List<String> columns = columns(layout);
    List<String> lines =
        new String(content, StandardCharsets.UTF_8).lines().skip(1).filter(x -> !x.isBlank()).toList();
    Set<String> distinct = new HashSet<>();
    List<String> hash = List.of(((String) l.get("cols")).split(","));
    for (String line : lines) {
      List<String> cells = List.of(line.split(",", -1));
      distinct.add(
          String.join("|", hash.stream().map(h -> cells.get(columns.indexOf(h))).toList()));
    }
    int hashTotal = "ROW_COUNT".equals(l.get("hash_rule")) ? lines.size() : distinct.size();
    String[] parts = file.split("_");
    String day =
        parts[2].substring(0, 4) + "-" + parts[2].substring(4, 6) + "-" + parts[2].substring(6, 8);
    String head =
        object + "," + layout + "," + parts[1] + "," + file + "," + day + " 18:00:00," + day
            + " 19:00:00,seed,";
    return ("object,layout,source_system,data_file,as_of,extracted_at,extracted_by,"
            + "measure,column_name,currency,filter,value\n"
            + head + "ROW_COUNT,,,," + lines.size() + "\n"
            + head + "HASH_TOTAL,,,," + hashTotal + "\n"
            + head + "SHA256,,,," + Sha256.hex(content) + "\n")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static String read(String path) {
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * One object of the storyline.
   *
   * @param object data object
   * @param files its extract files
   */
  record Step(String object, List<String> files) {}

  /**
   * The pipeline services.
   *
   * @param intake intake
   * @param plans batches
   * @param validation validation
   * @param runner load runner
   * @param recon reconciliation
   * @param signoffs gates
   */
  public record Services(
      IntakeService intake,
      BatchPlanService plans,
      ValidationService validation,
      LoadRunner runner,
      ReconciliationService recon,
      SignoffService signoffs) {}

  /** Runs work as a SIT/UAT user. */
  public interface RunAs {

    /**
     * Runs work as a user.
     *
     * @param username SIT/UAT user
     * @param work work
     * @param <T> result
     * @return the result
     */
    <T> T as(String username, Supplier<T> work);
  }
}
