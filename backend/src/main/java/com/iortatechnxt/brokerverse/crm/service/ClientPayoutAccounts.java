package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccount;
import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccountRepository;
import com.iortatechnxt.brokerverse.crm.domain.PayoutDetails;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contract of crm for the CA / SA information of clients (MKT 2.25.0-2.25.1;
 * ACCOUNTING_DISBURSEMENT_DESIGN 12.2): payrequest records the payout account of an approved refund
 * here. Additive only: a known account (same client and account number, or the same check payee) is
 * not recorded twice, and nothing is ever overwritten.
 */
@Service
@Transactional
public class ClientPayoutAccounts {

  /** Entity type on the audit trail. */
  public static final String ENTITY = "ClientPayoutAccount";

  /** Account number: digits only, 10 to 16 (the BDO format is to be confirmed, AQ19). */
  private static final Pattern ACCOUNT_NO = Pattern.compile("\\d{10,16}");

  private final ClientPayoutAccountRepository accounts;
  private final ClientService clients;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts payout accounts
   * @param clients clients
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ClientPayoutAccounts(
      ClientPayoutAccountRepository accounts,
      ClientService clients,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.accounts = accounts;
    this.clients = clients;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Checks CA / SA information before it is used on a request (MKT 2.25.0).
   *
   * @param details mode, payee name and account number
   */
  @Transactional(readOnly = true)
  public void validate(PayoutDetails details) {
    if (details.mode() == null || details.payeeName() == null || details.payeeName().isBlank()) {
      throw new BusinessRuleException(
          "PAYOUT_INCOMPLETE", "Give the payout mode and the account or check payee name");
    }
    if (details.mode() == PayoutMode.CTA
        && (details.accountNo() == null || !ACCOUNT_NO.matcher(details.accountNo()).matches())) {
      throw new BusinessRuleException(
          "PAYOUT_ACCOUNT_INVALID",
          "A credit to account needs the BDO account number (10 to 16 digits, AQ19)");
    }
  }

  /**
   * Adds CA / SA information to a client, unless the client already has it (MKT 2.25.1).
   *
   * @param companyId company
   * @param clientCode client code
   * @param details mode, payee name and account number
   * @param sourceModule capturing module (PAYREQUEST)
   * @param sourceRef its reference
   * @return the account and whether it was new
   */
  public Recorded record(
      Long companyId,
      String clientCode,
      PayoutDetails details,
      String sourceModule,
      String sourceRef) {
    validate(details);
    Client client = clients.requireByCode(companyId, clientCode);
    Optional<ClientPayoutAccount> known = existing(client.getId(), details);
    if (known.isPresent()) {
      return new Recorded(known.get(), false);
    }
    ClientPayoutAccount saved =
        accounts.save(new ClientPayoutAccount(client, details, sourceModule, sourceRef));
    audit.record(
        ENTITY,
        client.getCode(),
        AuditAction.CREATE,
        details.mode() + " payout account of " + saved.getPayeeName() + " from " + sourceRef);
    return new Recorded(saved, true);
  }

  /**
   * CA / SA information of a client, live first.
   *
   * @param clientId client
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<ClientPayoutAccount> forClient(Long clientId) {
    return accounts.findByClientIdOrderByActiveDescIdDesc(clientId);
  }

  /**
   * CA / SA information of a client by its code.
   *
   * @param companyId company
   * @param clientCode client code
   * @return accounts, live first
   */
  @Transactional(readOnly = true)
  public List<ClientPayoutAccount> forClient(Long companyId, String clientCode) {
    return forClient(clients.requireByCode(companyId, clientCode).getId());
  }

  /**
   * Deactivates a payout account (kept for history).
   *
   * @param clientId client
   * @param accountId account
   * @return the account
   */
  public ClientPayoutAccount deactivate(Long clientId, Long accountId) {
    ClientPayoutAccount account =
        accounts
            .findById(accountId)
            .filter(a -> a.getClientId().equals(clientId))
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY, accountId));
    if (account.isActive()) {
      account.deactivate(currentUser.username(), clock.instant());
      audit.record(
          ENTITY,
          account.getClientCode(),
          AuditAction.DEACTIVATE,
          "Payout account of " + account.getPayeeName() + " deactivated");
    }
    return account;
  }

  private Optional<ClientPayoutAccount> existing(Long clientId, PayoutDetails details) {
    return details.mode() == PayoutMode.CTA
        ? accounts.findByClientIdAndAccountNoAndActiveTrue(clientId, details.accountNo())
        : accounts.findByClientIdAndModeAndPayeeKeyAndActiveTrue(
            clientId, PayoutMode.CHECK, ClientPayoutAccount.key(details.payeeName()));
  }

  /**
   * Result of {@link #record}.
   *
   * @param account the account (new or already known)
   * @param created true when it was added now
   */
  public record Recorded(ClientPayoutAccount account, boolean created) {}
}
