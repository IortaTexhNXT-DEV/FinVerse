package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotClient;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotMortgage;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotSales;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Builds renewal candidates and their listing snapshot (RENEWAL_DESIGN section 4.1) from a booked
 * root invoice of the Operations ledger or from a migrated policy header, with a new renewal
 * reference {@code RNW-yyyy-nnnnnn} (BRRN.022; prefix {@code RNW_REFERENCE_PREFIX}).
 */
@Component
public class CandidateFactory {

  private static final int RATE_SCALE = 8;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final DocumentNumberService numbers;
  private final SalesOrganisationService sales;
  private final ClientRepository clients;
  private final RenewalParameters parameters;
  private final Clock clock;

  /**
   * Creates the factory.
   *
   * @param numbers document numbers
   * @param sales sales organisation (unit head)
   * @param clients clients (migrated policies name the client by code)
   * @param parameters renewal parameters (reference prefix)
   * @param clock clock
   */
  public CandidateFactory(
      DocumentNumberService numbers,
      SalesOrganisationService sales,
      ClientRepository clients,
      RenewalParameters parameters,
      Clock clock) {
    this.numbers = numbers;
    this.sales = sales;
    this.clients = clients;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * A new renewal reference.
   *
   * @return RNW-yyyy-nnnnnn
   */
  public String nextReference() {
    return numbers.next(parameters.referencePrefix() + "-" + BusinessClock.today(clock).getYear());
  }

  /**
   * A candidate of a booked root invoice.
   *
   * @param companyId company
   * @param invoice expiring invoice
   * @param runId extraction run
   * @return unsaved candidate
   */
  public RenewalCandidate fromInvoice(Long companyId, ExpiringInvoice invoice, Long runId) {
    return new RenewalCandidate(
        companyId,
        nextReference(),
        new RenewalCandidate.Origin(
            CandidateSource.BIBS_INVOICE,
            null,
            invoice.invoiceNo(),
            invoice.arn(),
            invoice.policyYear()),
        snapshot(companyId, invoice),
        runId);
  }

  /**
   * The listing snapshot of a booked invoice and its account.
   *
   * @param companyId company
   * @param i invoice
   * @return snapshot
   */
  public CandidateSnapshot snapshot(Long companyId, ExpiringInvoice i) {
    ExpiringInvoice.Amounts a = i.amounts();
    return new CandidateSnapshot(
        i.facts().policyNo(),
        null,
        i.product().versionNo(),
        i.facts().pnNos(),
        new SnapshotClient(
            i.client().id(),
            i.client().code(),
            i.client().name(),
            i.client().assured(),
            i.client().email()),
        new SnapshotProduct(
            i.product().code(),
            i.product().name(),
            i.product().line(),
            i.product().segment(),
            i.product().origin(),
            i.client().type()),
        new SnapshotSales(
            i.sales().branch(),
            i.sales().region(),
            i.sales().department(),
            i.sales().team(),
            unitHead(companyId, i.sales().team()),
            i.sales().officer()),
        i.facts().insurerCode(),
        new SnapshotMortgage(a.mortgagee() != null && !a.mortgagee().isBlank(), a.mortgagee()),
        i.product().packaged(),
        i.facts().inception(),
        i.facts().expiry(),
        new SnapshotPremium(
            a.netPremium(),
            a.grossPremium(),
            a.sumInsured(),
            rate(a.netPremium(), a.sumInsured()),
            a.commissionRate(),
            i.facts().currency()),
        null,
        null);
  }

  /**
   * A candidate of a migrated policy header (source LEGACY, key the legacy reference; DMQ37).
   *
   * @param companyId company
   * @param header migrated header
   * @param runId extraction run
   * @return unsaved candidate
   */
  public RenewalCandidate fromLegacy(Long companyId, LegacyHeader header, Long runId) {
    return new RenewalCandidate(
        companyId,
        nextReference(),
        new RenewalCandidate.Origin(
            CandidateSource.LEGACY, header.legacyRef(), null, header.arn(), null),
        legacySnapshot(companyId, header),
        runId);
  }

  private CandidateSnapshot legacySnapshot(Long companyId, LegacyHeader h) {
    var p = h.policy();
    var parties = h.parties();
    Optional<Client> client =
        parties.clientCode() == null
            ? Optional.empty()
            : clients.findByCode(companyId, parties.clientCode());
    return new CandidateSnapshot(
        p.policyNo(),
        p.coverNo(),
        null,
        p.pnNos(),
        new SnapshotClient(
            client.map(Client::getId).orElse(null),
            parties.clientCode(),
            parties.clientName(),
            parties.assuredName(),
            client.map(Client::getEmail).orElse(null)),
        new SnapshotProduct(
            p.productCode(),
            null,
            p.lineCode(),
            parties.segment(),
            h.sourceSystem(),
            client.map(c -> c.getClientType().name()).orElse(null)),
        new SnapshotSales(
            null,
            null,
            null,
            parties.salesUnit(),
            unitHead(companyId, parties.salesUnit()),
            parties.accountOfficer()),
        parties.insurerCode(),
        new SnapshotMortgage(parties.mortgageeBank() != null, parties.mortgageeBank()),
        p.legacyPackageCode() != null,
        p.inceptionDate(),
        p.expiryDate(),
        new SnapshotPremium(null, p.grossPremium(), p.sumInsured(), null, null, p.currency()),
        p.legacyPackageCode(),
        p.legacyPackageVersion());
  }

  private String unitHead(Long companyId, String team) {
    return team == null ? null : sales.unitHead(companyId, team).orElse(null);
  }

  private static BigDecimal rate(BigDecimal premium, BigDecimal sumInsured) {
    if (premium == null || sumInsured == null || sumInsured.signum() == 0) {
      return null;
    }
    return premium.multiply(HUNDRED).divide(sumInsured, RATE_SCALE, RoundingMode.HALF_UP);
  }
}
