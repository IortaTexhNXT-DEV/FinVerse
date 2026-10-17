package com.iortatechnxt.brokerverse.opsledger.service.port;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import java.util.Optional;

/**
 * Port to the Disbursement function (RMTID.011/019/034, DBMID.001, CSHID.024, CMRID.006):
 * remittance payment requests, client refunds, the BIR 2307 report and incentive pass-on. The
 * default adapter is the in-app Disbursement queue ({@code ops_disbursement_request}, screen
 * Operations - Disbursement Queue) until the Disbursement system and BRD are known (OQ02); status
 * changes (acknowledged, DV assigned, paid, returned) are published as {@code
 * OpsLedgerEvents.DisbursementStatusChanged}.
 */
public interface DisbursementGateway {

  /**
   * Sends a payment request, idempotent on (source module, source reference).
   *
   * @param companyId company
   * @param spec what to pay
   * @return the request's ticket
   */
  DisbursementTicket send(Long companyId, DisbursementRequest.Spec spec);

  /**
   * The ticket of an earlier request.
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return ticket, empty when never sent
   */
  Optional<DisbursementTicket> status(String sourceModule, String sourceRef);

  /**
   * Where a payment request stands.
   *
   * @param requestNo request number
   * @param status status
   * @param dvNo disbursement voucher number, when assigned
   * @param message return reason or other information
   */
  record DisbursementTicket(
      String requestNo, DisbursementRequest.Status status, String dvNo, String message) {}
}
