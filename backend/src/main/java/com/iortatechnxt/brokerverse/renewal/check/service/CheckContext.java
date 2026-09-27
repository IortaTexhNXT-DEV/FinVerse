package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.LamdLine;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalOverride;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A renewal under evaluation with the facts the checks read, loaded once and only when a check asks
 * for them: the expiring account, the expiring invoice family, the renewal account, the insurer
 * responses, the LAMD lines and the active overrides.
 */
public final class CheckContext {

  private final RenewalCandidate candidate;
  private final LocalDate today;
  private final Lazy<Optional<Account>> expiringAccount;
  private final Lazy<List<OpsInvoice>> family;
  private final Lazy<Optional<Account>> renewalAccount;
  private final Lazy<List<InsurerResponse>> responses;
  private final Lazy<List<LamdLine>> lamdLines;
  private final Lazy<List<RenewalOverride>> overrides;

  CheckContext(RenewalCandidate candidate, LocalDate today, Loaders loaders) {
    this.candidate = candidate;
    this.today = today;
    this.expiringAccount = new Lazy<>(loaders.expiringAccount());
    this.family = new Lazy<>(loaders.family());
    this.renewalAccount = new Lazy<>(loaders.renewalAccount());
    this.responses = new Lazy<>(loaders.responses());
    this.lamdLines = new Lazy<>(loaders.lamdLines());
    this.overrides = new Lazy<>(loaders.overrides());
  }

  public RenewalCandidate candidate() {
    return candidate;
  }

  public LocalDate today() {
    return today;
  }

  /**
   * Whether the renewal comes from a booked BIBS invoice.
   *
   * @return true for source BIBS_INVOICE
   */
  public boolean bibs() {
    return candidate.getSource() == CandidateSource.BIBS_INVOICE;
  }

  /**
   * The expiring account.
   *
   * @return account, empty for a policy without a BIBS account
   */
  public Optional<Account> expiringAccount() {
    return expiringAccount.get();
  }

  /**
   * The expiring invoice family (root, endorsements, cancellations), loaded.
   *
   * @return invoices, empty for a legacy policy
   */
  public List<OpsInvoice> family() {
    return family.get();
  }

  /**
   * The renewal account created at processing.
   *
   * @return account, empty before processing
   */
  public Optional<Account> renewalAccount() {
    return renewalAccount.get();
  }

  /**
   * The insurer responses, newest first.
   *
   * @return responses
   */
  public List<InsurerResponse> responses() {
    return responses.get();
  }

  /**
   * The LAMD lines matched to the renewal, newest first.
   *
   * @return lines
   */
  public List<LamdLine> lamdLines() {
    return lamdLines.get();
  }

  /**
   * Whether an active override of a kind (and check) exists.
   *
   * @param kind kind
   * @param checkCode check, null for any
   * @return true when overridden
   */
  public boolean overridden(OverrideKind kind, String checkCode) {
    return overrides.get().stream()
        .anyMatch(
            o ->
                o.isActive()
                    && o.getKind() == kind
                    && (checkCode == null || checkCode.equals(o.getCheckCode())));
  }

  /**
   * Loaders of the lazy facts.
   *
   * @param expiringAccount expiring account
   * @param family expiring invoice family
   * @param renewalAccount renewal account
   * @param responses insurer responses
   * @param lamdLines LAMD lines
   * @param overrides active overrides
   */
  record Loaders(
      Supplier<Optional<Account>> expiringAccount,
      Supplier<List<OpsInvoice>> family,
      Supplier<Optional<Account>> renewalAccount,
      Supplier<List<InsurerResponse>> responses,
      Supplier<List<LamdLine>> lamdLines,
      Supplier<List<RenewalOverride>> overrides) {}

  /** A value computed once, on first use. */
  private static final class Lazy<T> {
    private final Supplier<T> supplier;
    private T value;
    private boolean loaded;

    Lazy(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    T get() {
      if (!loaded) {
        value = supplier.get();
        loaded = true;
      }
      return value;
    }
  }
}
