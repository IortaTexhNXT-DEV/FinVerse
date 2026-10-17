package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import java.util.Optional;

/**
 * Default {@link DisbursementGateway}: the in-app Disbursement queue worked by users with {@code
 * DISB_PROCESS} (OQ02).
 */
public class QueueDisbursementGateway implements DisbursementGateway {

  private final DisbursementQueueService queue;

  /**
   * Creates the adapter.
   *
   * @param queue Disbursement queue
   */
  public QueueDisbursementGateway(DisbursementQueueService queue) {
    this.queue = queue;
  }

  @Override
  public DisbursementTicket send(Long companyId, DisbursementRequest.Spec spec) {
    return ticket(queue.send(companyId, spec));
  }

  @Override
  public Optional<DisbursementTicket> status(String sourceModule, String sourceRef) {
    return queue.find(sourceModule, sourceRef).map(QueueDisbursementGateway::ticket);
  }

  private static DisbursementTicket ticket(DisbursementRequest r) {
    return new DisbursementTicket(
        r.getRequestNo(), r.getStatus(), r.getDvNo(), r.getReturnReason());
  }
}
