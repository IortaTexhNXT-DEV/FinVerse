package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDelivery;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDelivery.Outcome;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDelivery.Stage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDelivery.Status;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDelivery.Target;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceFileDeliveryRepository;
import com.iortatechnxt.brokerverse.remittance.service.port.SharedFolderGateway;
import com.iortatechnxt.brokerverse.remittance.service.port.SharedFolderGateway.Copy;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copies the two extract files of a batch (IF-RMT-01 Excel and IF-RMT-02 text) to the shared folder
 * of its remittance type (FRS BRD-02 FRID.RMT.001.05, ICD IF-RMT-04): when the batch is created,
 * when it is approved and when a user sends them again. Each copy is logged on the batch; a failed
 * copy raises an alert and the files stay downloadable from the batch.
 */
@Service
@Transactional
public class ExtractDelivery {

  /** Alert of a failed copy. */
  public static final String FAILED_ALERT = "REMIT_SHARED_FOLDER_FAILED";

  private final RemittanceExtractFiles files;
  private final RemittanceSettingsService settings;
  private final SharedFolderGateway folders;
  private final RemittanceFileDeliveryRepository deliveries;
  private final RemittanceBatchRepository batches;
  private final FileDropPort repository;
  private final AlertService alerts;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files extract files
   * @param settings folders of the types
   * @param folders shared drive
   * @param deliveries copy log
   * @param batches batches
   * @param repository in-system extract repository (Operations - Interfaces - Extracts)
   * @param alerts alerts
   * @param currentUser signed-in user
   * @param clock clock
   */
  public ExtractDelivery(
      RemittanceExtractFiles files,
      RemittanceSettingsService settings,
      SharedFolderGateway folders,
      RemittanceFileDeliveryRepository deliveries,
      RemittanceBatchRepository batches,
      FileDropPort repository,
      AlertService alerts,
      CurrentUser currentUser,
      Clock clock) {
    this.files = files;
    this.settings = settings;
    this.folders = folders;
    this.deliveries = deliveries;
    this.batches = batches;
    this.repository = repository;
    this.alerts = alerts;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Files of a new batch: both extract files are kept in the in-system extract repository under the
   * folder of the remittance type, then copied to the shared folder.
   *
   * @param batch batch with lines, saved
   * @return repository id of the Excel extract
   */
  public Long created(RemittanceBatch batch) {
    String folder =
        RemittanceSettingsService.folder(
            settings.current(batch.getCompanyId()), batch.getRemittanceType());
    String user = currentUser.optionalUsername().orElse(null);
    ExtractFile.Origin origin =
        new ExtractFile.Origin(RemittanceSettings.MODULE, batch.getBatchNo());
    DroppedFile excel =
        repository.drop(
            batch.getCompanyId(),
            new ExtractFile.Location(folder, files.excelName(batch)),
            new DropContent(RemittanceExtractFiles.XLS, files.excel(batch, user)),
            origin);
    repository.drop(
        batch.getCompanyId(),
        new ExtractFile.Location(folder, files.textName(batch)),
        new DropContent(RemittanceExtractFiles.TEXT, files.text(batch)),
        origin);
    deliver(batch, Stage.CREATED);
    return excel.id();
  }

  /**
   * Copies the two files of a batch to the shared folder.
   *
   * @param batch batch with lines, saved
   * @param stage when
   * @return the copies made
   */
  public List<RemittanceFileDelivery> deliver(RemittanceBatch batch, Stage stage) {
    String folder =
        RemittanceSettingsService.folder(
            settings.current(batch.getCompanyId()), batch.getRemittanceType());
    String user = currentUser.optionalUsername().orElse(null);
    List<RemittanceFileDelivery> made = new ArrayList<>();
    made.add(copy(batch, stage, folder, files.excelName(batch), files.excel(batch, user)));
    made.add(copy(batch, stage, folder, files.textName(batch), files.text(batch)));
    if (made.stream().anyMatch(d -> d.getStatus() == Status.FAILED)) {
      alerts.raise(
          FAILED_ALERT,
          new AlertFacts(
              batch.getCompanyId(),
              null,
              BatchService.ENTITY,
              batch.getBatchNo(),
              "The extract files of " + batch.getSettlementRef() + " were not copied to " + folder,
              null,
              FAILED_ALERT + ":" + batch.getBatchNo() + ":" + stage));
    }
    return made;
  }

  private RemittanceFileDelivery copy(
      RemittanceBatch batch, Stage stage, String folder, String name, byte[] content) {
    Copy copy = folders.copy(folder, name, content);
    return deliveries.save(
        new RemittanceFileDelivery(
            batch.getId(),
            stage,
            new Target(folder, name),
            new Outcome(copy.delivered(), copy.reference()),
            currentUser.optionalUsername().orElse(null),
            clock.instant()));
  }

  /**
   * Copies the files of a batch again (a user, after a failed copy).
   *
   * @param batchId batch
   * @return the copies made
   */
  public List<RemittanceFileDelivery> resend(Long batchId) {
    return deliver(load(batchId), Stage.RESENT);
  }

  /**
   * An extract file of a batch, rendered now.
   *
   * @param batchId batch
   * @param excel true for the Excel extract, false for the text extract
   * @return name, type and content
   */
  @Transactional(readOnly = true)
  public RenderedFile file(Long batchId, boolean excel) {
    RemittanceBatch batch = load(batchId);
    return excel
        ? new RenderedFile(
            files.excelName(batch),
            RemittanceExtractFiles.XLS,
            files.excel(batch, currentUser.optionalUsername().orElse(null)))
        : new RenderedFile(files.textName(batch), RemittanceExtractFiles.TEXT, files.text(batch));
  }

  private RemittanceBatch load(Long batchId) {
    return batches
        .findWithLinesById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException(BatchService.ENTITY, batchId));
  }

  /**
   * Copies of a batch, newest first.
   *
   * @param batchId batch
   * @return copies
   */
  @Transactional(readOnly = true)
  public List<RemittanceFileDelivery> history(Long batchId) {
    return deliveries.findByBatchIdOrderByIdDesc(batchId);
  }

  /**
   * A rendered extract file.
   *
   * @param name file name
   * @param contentType content type
   * @param content bytes
   */
  public record RenderedFile(String name, String contentType, byte[] content) {

    /** Defensive copy. */
    public RenderedFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof RenderedFile f && f.name.equals(name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "RenderedFile[" + name + "]";
    }
  }
}
