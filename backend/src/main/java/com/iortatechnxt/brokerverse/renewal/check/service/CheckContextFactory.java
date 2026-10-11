package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.LamdLineRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalInsurerResponseRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverrideRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Builds the {@link CheckContext} of a renewal with the loaders of its facts. */
@Component
public class CheckContextFactory {

  private final AccountRepository accounts;
  private final InvoiceLedgerQueryService ledger;
  private final RenewalInsurerResponseRepository responses;
  private final LamdLineRepository lamd;
  private final RenewalOverrideRepository overrides;

  /**
   * Creates the factory.
   *
   * @param accounts accounts
   * @param ledger Operations ledger
   * @param responses insurer responses
   * @param lamd LAMD lines
   * @param overrides overrides
   */
  public CheckContextFactory(
      AccountRepository accounts,
      InvoiceLedgerQueryService ledger,
      RenewalInsurerResponseRepository responses,
      LamdLineRepository lamd,
      RenewalOverrideRepository overrides) {
    this.accounts = accounts;
    this.ledger = ledger;
    this.responses = responses;
    this.lamd = lamd;
    this.overrides = overrides;
  }

  /**
   * The context of a renewal.
   *
   * @param candidate saved candidate
   * @param today business date
   * @return context
   */
  public CheckContext of(RenewalCandidate candidate, LocalDate today) {
    return new CheckContext(
        candidate,
        today,
        new CheckContext.Loaders(
            () -> byArn(candidate.getExpiringArn()),
            () ->
                candidate.getExpiringInvoiceNo() == null
                    ? List.of()
                    : ledger.family(candidate.getExpiringInvoiceNo()),
            () -> byArn(candidate.getRenewalArn()),
            () -> responses.findByCandidateIdOrderByIdDesc(candidate.getId()),
            () -> lamd.findByCandidateIdOrderByIdDesc(candidate.getId()),
            () -> overrides.findByCandidateIdAndActiveTrue(candidate.getId())));
  }

  private Optional<Account> byArn(String arn) {
    return arn == null ? Optional.empty() : accounts.findByArn(arn);
  }
}
