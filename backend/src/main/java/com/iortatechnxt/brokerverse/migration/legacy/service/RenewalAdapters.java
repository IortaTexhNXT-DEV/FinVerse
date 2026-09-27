package com.iortatechnxt.brokerverse.migration.legacy.service;

import com.iortatechnxt.brokerverse.migration.legacy.domain.PackageMapRow;
import com.iortatechnxt.brokerverse.migration.legacy.service.port.PackageMapSink;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource;
import com.iortatechnxt.brokerverse.renewal.setup.service.PackageMapService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Data Migration plugged into Renewal (DATA_MIGRATION_DESIGN 15.1 and 15.2): the migrated policy
 * headers and the renewal advices already sent serve Renewal's {@link LegacyPolicySource} (in place
 * of its "not connected" default; the Renewal upload stays the fallback), and the PACKAGE map
 * loaded by object R06 is handed to Renewal's package map, where the sanitation check {@code
 * PACKAGE_REMAP} reads it (source MIGRATION, pending authorisation by the Product Owner).
 */
@Configuration(proxyBeanMethods = false)
public class RenewalAdapters {

  /**
   * The migrated headers as Renewal's legacy policy source.
   *
   * @param policies migrated headers
   * @return source
   */
  @Bean
  public LegacyPolicySource migratedPolicySource(MigratedPolicyService policies) {
    return new LegacyPolicySource() {
      @Override
      public boolean connected() {
        return policies.connected();
      }

      @Override
      public List<LegacyHeader> expiringHeaders(Long companyId, LocalDate from, LocalDate to) {
        return policies.expiringHeaders(companyId, from, to).stream()
            .map(RenewalAdapters::header)
            .toList();
      }

      @Override
      public GoLiveHeaders goLiveCandidates(Long companyId, LocalDate goLive, LocalDate to) {
        MigratedPolicyService.GoLive g = policies.goLiveCandidates(companyId, goLive, to);
        return new GoLiveHeaders(
            g.headers().stream().map(RenewalAdapters::header).toList(), g.renewedInLegacy());
      }
    };
  }

  /**
   * The PACKAGE map rows handed to Renewal's package map.
   *
   * @param packageMap Renewal package map
   * @return sink
   */
  @Bean
  public PackageMapSink renewalPackageMapSink(PackageMapService packageMap) {
    return new PackageMapSink() {
      @Override
      public boolean connected() {
        return true;
      }

      @Override
      public void add(Long companyId, PackageMapRow.PackageMapEntry entry) {
        packageMap.create(companyId, data(entry), PackageMapService.SOURCE_MIGRATION);
      }

      @Override
      public void remove(Long companyId, PackageMapRow.PackageMapEntry entry) {
        packageMap.entries(companyId).stream()
            .filter(e -> PackageMapService.SOURCE_MIGRATION.equals(e.getSource()))
            .filter(PackageMapEntry::isActive)
            .filter(e -> same(e, entry))
            .forEach(e -> packageMap.deactivate(companyId, e.getId()));
      }
    };
  }

  private static boolean same(PackageMapEntry e, PackageMapRow.PackageMapEntry m) {
    boolean keys =
        e.getLegacyPackageCode().equals(m.legacyPackageCode())
            && Objects.equals(
                e.getLegacyPackageVersion(), String.valueOf(m.legacyPackageVersion()));
    boolean qualifiers =
        Objects.equals(e.getRiskCode(), m.riskCode())
            && Objects.equals(e.getInsurerCode(), m.insurerCode());
    return keys
        && qualifiers
        && compare(e.getSiFrom(), m.siFrom())
        && compare(e.getSiTo(), m.siTo());
  }

  private static boolean compare(BigDecimal a, BigDecimal b) {
    return a == null ? b == null : b != null && a.compareTo(b) == 0;
  }

  private static PackageMapEntry.Data data(PackageMapRow.PackageMapEntry m) {
    boolean reject = "REJECT".equals(m.action());
    return new PackageMapEntry.Data(
        m.legacyPackageCode(),
        String.valueOf(m.legacyPackageVersion()),
        m.riskCode(),
        m.insurerCode(),
        m.siFrom(),
        m.siTo(),
        reject ? null : m.productCode(),
        reject ? null : m.productVersionNo(),
        m.remarks());
  }

  private static LegacyPolicySource.LegacyHeader header(MigratedPolicyService.Header h) {
    MigratedPolicyService.Policy p = h.policy();
    MigratedPolicyService.Parties q = h.parties();
    MigratedPolicyService.RaSent r = h.raSent();
    return new LegacyPolicySource.LegacyHeader(
        h.legacyRef(),
        h.sourceSystem(),
        h.arn(),
        new LegacyPolicySource.LegacyPolicy(
            p.policyNo(),
            p.coverNo(),
            p.productCode(),
            p.lineCode(),
            p.legacyPackageCode(),
            p.legacyPackageVersion(),
            p.inceptionDate(),
            p.expiryDate(),
            p.sumInsured(),
            p.grossPremium(),
            p.currency(),
            p.pnNos()),
        new LegacyPolicySource.LegacyParties(
            q.clientCode(),
            q.clientName(),
            q.assuredName(),
            q.insurerCode(),
            q.accountOfficer(),
            q.salesUnit(),
            q.segment(),
            q.mortgageeBank()),
        h.urgent(),
        r == null
            ? null
            : new LegacyPolicySource.LegacyRaSent(
                r.sentOn(), r.reference(), r.channel(), r.recipient()));
  }
}
