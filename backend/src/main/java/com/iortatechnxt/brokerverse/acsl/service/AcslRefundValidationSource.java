package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ACSL's answer to the Operations port {@link RefundValidationSource} (validator {@code ACSL}; MKT
 * 1.11.0, ACSL 2.5.5): a refund of a cancelled account opens an account analysis case, idempotent
 * on the requester's reference; the result given on the case comes back to payrequest as {@code
 * RefundValidationCompleted} ({@link CaseService#provideResult}).
 */
@Service
@Transactional
public class AcslRefundValidationSource implements RefundValidationSource {

  private final CaseService cases;

  /**
   * Creates the adapter.
   *
   * @param cases ACSL cases
   */
  public AcslRefundValidationSource(CaseService cases) {
    this.cases = cases;
  }

  @Override
  public String validator() {
    return ACSL;
  }

  @Override
  public ValidationTicket open(ValidationRequest request) {
    AcslCase c = cases.openAnalysis(request);
    return new ValidationTicket(
        ACSL, Status.OPENED, c.getCaseNo(), "Account analysis case " + c.getCaseNo() + " opened");
  }
}
