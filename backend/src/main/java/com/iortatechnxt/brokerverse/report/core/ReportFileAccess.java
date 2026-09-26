package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.report.domain.ReportBatchRepository;
import com.iortatechnxt.brokerverse.report.domain.ReportRunRepository;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Who may open the stored files of reports: an archived run ({@value
 * ReportArchiveService#OWNER_TYPE}) needs the export permission of its report and its availability
 * time (CSHID.018, as the download of the run); a report batch ({@value
 * ReportBatchService#OWNER_TYPE}) is opened only by the user who ran it, with {@code REPORT_VIEW}.
 * Files are stored by the application only. Also declares {@code report_run_file} and {@code
 * report_batch} for the copy of the files kept in the database before ST1.
 */
@Component
public class ReportFileAccess implements FileOwnerAccess {

  private static final String REPORT_VIEW = "REPORT_VIEW";

  private final ReportRunRepository runs;
  private final ReportBatchRepository batches;
  private final ReportArchiveService archive;
  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param runs archived runs
   * @param batches report batches
   * @param archive report archive (download rule)
   * @param currentUser current user
   */
  public ReportFileAccess(
      ReportRunRepository runs,
      ReportBatchRepository batches,
      ReportArchiveService archive,
      CurrentUser currentUser) {
    this.runs = runs;
    this.batches = batches;
    this.archive = archive;
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(ReportArchiveService.OWNER_TYPE, ReportBatchService.OWNER_TYPE);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    if (ReportBatchService.OWNER_TYPE.equals(owner.entityType())) {
      return currentUser.hasAuthority(REPORT_VIEW)
          && owner
              .numericId()
              .flatMap(batches::findById)
              .map(b -> CurrentUser.sameUser(b.getCreatedBy(), currentUser.username()))
              .orElse(false);
    }
    return owner.numericId().flatMap(runs::findById).map(archive::mayDownload).orElse(false);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return false;
  }

  /**
   * Files of archived exports kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable reportRunFileTable() {
    String from = " from report_run_file f join report_run r on r.id = f.run_id";
    return new LegacyFileTable(
        "report_run_file",
        ReportArchiveService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select f.run_id as file_key, cast(null as bigint) as company_id,"
            + " cast(r.id as varchar) as owner_id, '"
            + ReportArchiveService.REPORT_OUTPUT
            + "' as record_class, r.report_code as document_type,"
            + " coalesce(r.file_name, r.report_code) as file_name, r.content_type,"
            + " cast(null as varchar) as recorded_sha256, f.content"
            + from
            + " where r.stored_file_id is null and f.run_id > ? order by f.run_id limit ?",
        "update report_run set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(r.stored_file_id) as moved" + from);
  }

  /**
   * Files of report batches kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable reportBatchTable() {
    return new LegacyFileTable(
        "report_batch",
        ReportBatchService.OWNER_TYPE,
        FileOrigin.GENERATED,
        "select b.id as file_key, b.company_id, cast(b.id as varchar) as owner_id, '"
            + ReportArchiveService.REPORT_OUTPUT
            + "' as record_class, cast(null as varchar) as document_type,"
            + " coalesce(b.file_name, b.batch_no) as file_name, b.content_type,"
            + " cast(null as varchar) as recorded_sha256, b.content"
            + " from report_batch b where b.content is not null and b.stored_file_id is null"
            + " and b.id > ? order by b.id limit ?",
        "update report_batch set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(b.stored_file_id) as moved"
            + " from report_batch b where b.content is not null");
  }
}
