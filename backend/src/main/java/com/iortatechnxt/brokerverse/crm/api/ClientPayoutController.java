package com.iortatechnxt.brokerverse.crm.api;

import com.iortatechnxt.brokerverse.crm.api.dto.PayoutAccountResponse;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CA / SA information of a client (MKT 2.25.0-2.25.1): captured from approved refund requests,
 * listed on the client record, deactivated when no longer valid.
 */
@RestController
@RequestMapping("/api/v1/crm/clients/{id}/payout-accounts")
public class ClientPayoutController {

  private final ClientPayoutAccounts payouts;

  /**
   * Creates the controller.
   *
   * @param payouts payout accounts
   */
  public ClientPayoutController(ClientPayoutAccounts payouts) {
    this.payouts = payouts;
  }

  /**
   * Payout accounts of a client, live first.
   *
   * @param id client
   * @return accounts
   */
  @GetMapping
  @PreAuthorize("hasAuthority('CLIENT_VIEW')")
  public List<PayoutAccountResponse> list(@PathVariable Long id) {
    return payouts.forClient(id).stream().map(PayoutAccountResponse::from).toList();
  }

  /**
   * Deactivates a payout account.
   *
   * @param id client
   * @param accountId account
   * @return the account
   */
  @PostMapping("/{accountId}/deactivate")
  @PreAuthorize("hasAuthority('CLIENT_MAINTAIN')")
  public PayoutAccountResponse deactivate(@PathVariable Long id, @PathVariable Long accountId) {
    return PayoutAccountResponse.from(payouts.deactivate(id, accountId));
  }
}
