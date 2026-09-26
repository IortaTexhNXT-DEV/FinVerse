package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput.OutputFile;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutputRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import com.iortatechnxt.brokerverse.storage.service.PermissionFileAccess;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * The output files of end-of-day runs (bank files, printed forms, reports) in the file store (owner
 * type {@value #OWNER_TYPE}, the output; record class {@code WORKING_FILE}; build step ST1) and who
 * may open them: {@code DISB_EOD} and {@code DISB_VIEW}, as for the run screen. Also declares
 * {@code dsb_eod_output} for the copy of the outputs kept in the database before ST1.
 */
@Component
public class EodOutputFiles {

  /** Owner entity type of the stored outputs. */
  public static final String OWNER_TYPE = "EodOutput";

  /** Record class of the outputs. */
  public static final String RECORD_CLASS = "WORKING_FILE";

  private static final List<String> READERS = List.of("DISB_EOD", "DISB_VIEW");

  private final EodOutputRepository outputs;
  private final StoredFileService storedFiles;

  /**
   * Creates the component.
   *
   * @param outputs outputs
   * @param storedFiles file store
   */
  public EodOutputFiles(EodOutputRepository outputs, StoredFileService storedFiles) {
    this.outputs = outputs;
    this.storedFiles = storedFiles;
  }

  /**
   * Records an output of a run and stores its file.
   *
   * @param run run
   * @param kind kind
   * @param code report or output code
   * @param file the file
   * @param count items in it
   * @return the output
   */
  public EodOutput save(EodRun run, OutputKind kind, String code, OutputFile file, int count) {
    EodOutput output = outputs.save(new EodOutput(run.getId(), kind, code, file, count));
    output.storedIn(
        storedFiles
            .storeChecked(
                new StoreRequest(
                    new FileOwner(run.getCompanyId(), OWNER_TYPE, String.valueOf(output.getId())),
                    code,
                    RECORD_CLASS,
                    file.fileName(),
                    file.content(),
                    null),
                file.contentType(),
                FileOrigin.GENERATED)
            .getId());
    return output;
  }

  /**
   * An output for the download endpoint: a presigned link to the stored file, or the bytes of an
   * output produced before ST1.
   *
   * @param outputId output
   * @return download
   */
  public FileDownload download(Long outputId) {
    EodOutput o =
        outputs
            .findById(outputId)
            .orElseThrow(() -> new ResourceNotFoundException("EOD output", outputId));
    return o.getStoredFileId() == null
        ? FileDownload.inline(o.getFileName(), o.getContentType(), o.getContent())
        : FileDownload.stored(o.getStoredFileId());
  }

  /**
   * Who may open the outputs.
   *
   * @param currentUser current user
   * @return resolver
   */
  @Bean
  static FileOwnerAccess eodOutputFileAccess(CurrentUser currentUser) {
    return new PermissionFileAccess(Set.of(OWNER_TYPE), READERS, currentUser);
  }

  /**
   * End-of-day outputs kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable eodOutputTable() {
    return new LegacyFileTable(
        "dsb_eod_output",
        OWNER_TYPE,
        FileOrigin.GENERATED,
        "select o.id as file_key, r.company_id, cast(o.id as varchar) as owner_id, '"
            + RECORD_CLASS
            + "' as record_class, o.code as document_type, o.file_name, o.content_type,"
            + " cast(null as varchar) as recorded_sha256, o.content"
            + " from dsb_eod_output o join dsb_eod_run r on r.id = o.eod_run_id"
            + " where o.content is not null and o.stored_file_id is null and o.id > ?"
            + " order by o.id limit ?",
        "update dsb_eod_output set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from dsb_eod_output where content is not null");
  }
}
