package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Status;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import java.util.EnumSet;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Disbursement module behind the Operations port (design 1.2, 2.2; DIS 2.6.0, 3.25.0, OQ02):
 * remittance, cashiering, commission, payrequest and frbs send their payments here without knowing
 * which adapter runs. The request is kept on the Operations queue record (the contract the sources
 * read, with its {@code DSQ-} number and {@code DisbursementStatusChanged} events) and received by
 * Disbursement in the same transaction: the payee is matched and, for refunds and remittances, the
 * voucher is built and routed to the approver. Progress (DV stage, DV number, instrument status,
 * payment, return, cancellation) is reported back on the queue record by {@link GatewaySync}.
 */
@Service
@Primary
@Transactional
public class DisbursementGatewayAdapter implements DisbursementGateway {

  private final DisbursementQueueService queue;
  private final RequestIntakeService intake;

  /**
   * Creates the adapter.
   *
   * @param queue Operations payment requests
   * @param intake Disbursement intake
   */
  public DisbursementGatewayAdapter(DisbursementQueueService queue, RequestIntakeService intake) {
    this.queue = queue;
    this.intake = intake;
  }

  @Override
  public DisbursementTicket send(Long companyId, DisbursementRequest.Spec spec) {
    DisbursementRequest queued = queue.send(companyId, spec);
    if (EnumSet.of(Status.SENT, Status.ACKNOWLEDGED).contains(queued.getStatus())) {
      intake.receive(companyId, spec, queued.getId());
    }
    return ticket(queue.get(queued.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DisbursementTicket> status(String sourceModule, String sourceRef) {
    return queue.find(sourceModule, sourceRef).map(DisbursementGatewayAdapter::ticket);
  }

  private static DisbursementTicket ticket(DisbursementRequest r) {
    String message = r.getStatus() == Status.CANCELLED ? r.getCancelReason() : r.getReturnReason();
    return new DisbursementTicket(r.getRequestNo(), r.getStatus(), r.getDvNo(), message);
  }
}
