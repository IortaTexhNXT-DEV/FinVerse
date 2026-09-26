package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun.FileRef;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUpload.FileKey;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUploadRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upload of an insurer production report from the reconciliation screens (PRCID.009/010): a
 * duplicate is refused up front with {@code RECON_UPLOAD_DUPLICATE}; otherwise the file runs as an
 * upload of the flow-in feed {@code INSURER_PRODUCTION} for the user's company, and the attempts it
 * made are returned with the run. Not transactional: the flow-in run commits each record on its
 * own.
 */
@Service
public class ReconUploadService {

  private final FlowInService flowIn;
  private final InsurerProductionHandler handler;
  private final ReconUploadRecorder recorder;
  private final ReconUploadRepository uploads;

  /**
   * Creates the service.
   *
   * @param flowIn flow-in framework
   * @param handler feed handler
   * @param recorder upload attempts
   * @param uploads upload history
   */
  public ReconUploadService(
      FlowInService flowIn,
      InsurerProductionHandler handler,
      ReconUploadRecorder recorder,
      ReconUploadRepository uploads) {
    this.flowIn = flowIn;
    this.handler = handler;
    this.recorder = recorder;
    this.uploads = uploads;
  }

  /**
   * Uploads a file.
   *
   * @param companyId company
   * @param fileName file name
   * @param content bytes
   * @return the run and the attempts it made
   */
  public UploadResult upload(Long companyId, String fileName, byte[] content) {
    FileKey key = new FileKey(fileName, Sha256.hex(content));
    Optional<String> duplicate = recorder.blockIfDuplicate(key, null);
    if (duplicate.isPresent()) {
      throw new BusinessRuleException(
          ReconUploadRecorder.DUPLICATE, "Upload refused: " + duplicate.get());
    }
    FlowInFile file = new FlowInFile(fileName, content);
    FlowInRun run =
        flowIn.run(
            InsurerProductionHandler.FEED,
            Trigger.UPLOAD,
            new FileRef(fileName, key.sha256()),
            ctx -> handler.process(companyId, file, ctx));
    return new UploadResult(run, uploads.findByRunNoOrderByIdAsc(run.getRunNo()));
  }

  /**
   * Upload history (PRCID.031/032).
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param pageable page
   * @return attempts, newest first
   */
  @Transactional(readOnly = true)
  public Page<ReconUpload> history(Long companyId, String insurer, Pageable pageable) {
    return uploads.search(companyId, insurer, pageable);
  }

  /**
   * The result of an upload.
   *
   * @param run flow-in run
   * @param attempts attempts per insurer and month
   */
  public record UploadResult(FlowInRun run, List<ReconUpload> attempts) {

    /** Defensive copy. */
    public UploadResult {
      attempts = List.copyOf(attempts);
    }
  }
}
