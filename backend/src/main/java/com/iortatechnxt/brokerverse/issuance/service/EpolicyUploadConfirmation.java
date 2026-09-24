package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.UploadBatch;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Confirms a bulk e-policy upload (BRNB.073): each included file is received on its account in its
 * own transaction, and the outcome of each file is recorded on the upload.
 */
@Component
public class EpolicyUploadConfirmation {

  private final EpolicyUploadService uploads;
  private final EpolicyService epolicies;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the orchestrator.
   *
   * @param uploads bulk uploads
   * @param epolicies e-policy receipt
   * @param transactionManager transaction manager
   */
  public EpolicyUploadConfirmation(
      EpolicyUploadService uploads,
      EpolicyService epolicies,
      PlatformTransactionManager transactionManager) {
    this.uploads = uploads;
    this.epolicies = epolicies;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Confirms the upload.
   *
   * @param batchId upload
   * @return the upload with the outcome of each file
   */
  public UploadBatch confirm(Long batchId) {
    List<Long> items = newTransaction.execute(status -> uploads.markConfirmed(batchId));
    for (Long itemId : items == null ? List.<Long>of() : items) {
      Long epolicyId = null;
      String outcome;
      try {
        Epolicy received =
            newTransaction.execute(status -> epolicies.receive(uploads.fileOf(batchId, itemId)));
        epolicyId = received == null ? null : received.getId();
        outcome = "Stored on the account";
      } catch (BusinessRuleException | ResourceNotFoundException e) {
        outcome = e.getMessage();
      }
      Long stored = epolicyId;
      String note = outcome;
      newTransaction.executeWithoutResult(
          status -> uploads.recordOutcome(batchId, itemId, stored, note));
    }
    return uploads.get(batchId);
  }
}
