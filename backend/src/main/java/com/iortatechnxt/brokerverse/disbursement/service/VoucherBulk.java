package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Approves several vouchers at once (DIS 2.19.0 "single or multiple"), each in its own transaction
 * with its own result. A voucher whose posting fails keeps its stage and is marked FAILED with the
 * error, so it is listed as unregularised (DIS 3.27.0) until approved again.
 */
@Service
public class VoucherBulk {

  private final VoucherActions actions;
  private final VoucherRepository vouchers;
  private final TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param actions voucher actions
   * @param vouchers vouchers
   * @param txManager transactions
   */
  public VoucherBulk(
      VoucherActions actions, VoucherRepository vouchers, PlatformTransactionManager txManager) {
    this.actions = actions;
    this.vouchers = vouchers;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Approves vouchers one by one.
   *
   * @param ids vouchers
   * @param comment comment
   * @return one result per voucher
   */
  public List<ItemResult> approve(List<Long> ids, String comment) {
    List<ItemResult> results = new ArrayList<>();
    for (Long id : ids) {
      try {
        Voucher v = tx.execute(s -> actions.approve(id, comment));
        results.add(new ItemResult(id, v == null ? null : v.getDvNo(), true, "Approved"));
      } catch (BusinessRuleException ex) {
        if (VoucherActions.POSTING_FAILED.equals(ex.getCode())) {
          failed(id, ex.getMessage());
        }
        results.add(new ItemResult(id, dvNo(id), false, ex.getMessage()));
      } catch (ResourceNotFoundException | AccessDeniedException ex) {
        results.add(new ItemResult(id, null, false, ex.getMessage()));
      }
    }
    return results;
  }

  private void failed(Long id, String message) {
    tx.executeWithoutResult(
        s ->
            vouchers
                .findById(id)
                .filter(v -> v.getStage().editable())
                .ifPresent(v -> v.postingFailed(message)));
  }

  private String dvNo(Long id) {
    return vouchers.findById(id).map(Voucher::getDvNo).orElse(null);
  }

  /**
   * The outcome of one voucher.
   *
   * @param id voucher
   * @param dvNo DV number
   * @param ok approved
   * @param message outcome
   */
  public record ItemResult(Long id, String dvNo, boolean ok, String message) {}
}
