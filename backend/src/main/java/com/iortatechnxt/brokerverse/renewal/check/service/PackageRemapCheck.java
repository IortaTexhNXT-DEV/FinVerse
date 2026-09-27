package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code PACKAGE_REMAP} (BRRN.020; DMQ36 answered: remapping at sanitation; FR-RN-028): a migrated
 * policy carries its legacy package code; the check looks it up in the PACKAGE code map. One active
 * BIBS package version found: the check passes and the renewal takes that version. No entry, a
 * REJECT entry, several versions or a retired version: the check fails and the renewal goes to the
 * Exception bucket, where the Renewal processing team chooses the version (maker-checker). Not
 * applicable to policies booked in BIBS.
 */
@Component
public class PackageRemapCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "PACKAGE_REMAP";

  private static final Set<ProductVersionStatus> USABLE =
      EnumSet.of(ProductVersionStatus.RELEASED, ProductVersionStatus.SUPERSEDED);

  private final PackageMapEntryRepository map;
  private final ProductVersionRepository versions;

  /**
   * Creates the check.
   *
   * @param map the PACKAGE code map
   * @param versions package versions of Product Maintenance
   */
  public PackageRemapCheck(PackageMapEntryRepository map, ProductVersionRepository versions) {
    this.map = map;
    this.versions = versions;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    CandidateSnapshot s = c.getSnapshot();
    if (c.getSource() != CandidateSource.LEGACY) {
      return Verdict.notApplicable("The policy is booked in BIBS");
    }
    if (s.legacyPackageCode() == null) {
      return Verdict.notApplicable("The migrated policy is not packaged");
    }
    if (c.getResolvedProductCode() != null) {
      return Verdict.pass(
          "Package " + c.getResolvedProductCode() + " version " + c.getResolvedVersionNo());
    }
    List<PackageMapEntry> matches =
        map.findByCompanyIdAndLegacyPackageCode(c.getCompanyId(), s.legacyPackageCode()).stream()
            .filter(e -> e.appliesTo(s.legacyPackageVersion(), risk(s), s.insurerCode(), si(s)))
            .toList();
    String legacy = legacyLabel(s);
    if (matches.isEmpty()) {
      return Verdict.fail("Legacy package " + legacy + " is not in the package map", "UNMAPPED");
    }
    if (matches.stream().anyMatch(PackageMapEntry::isReject)) {
      return Verdict.fail("Legacy package " + legacy + " has no BIBS package", "REJECT");
    }
    List<String> targets =
        matches.stream()
            .map(e -> e.getProductCode() + " v" + e.getProductVersionNo())
            .distinct()
            .toList();
    if (targets.size() > 1) {
      return Verdict.fail(
          "Legacy package " + legacy + " maps to several versions: " + String.join(", ", targets),
          "AMBIGUOUS");
    }
    PackageMapEntry entry = matches.get(0);
    boolean usable =
        versions
            .findByProductCodeAndVersionNo(entry.getProductCode(), entry.getProductVersionNo())
            .map(ProductVersion::getStatus)
            .filter(USABLE::contains)
            .isPresent();
    if (!usable) {
      return Verdict.fail(
          "Package " + targets.get(0) + " of legacy package " + legacy + " is not active",
          "RETIRED");
    }
    c.resolvePackage(entry.getProductCode(), entry.getProductVersionNo());
    return Verdict.pass("Legacy package " + legacy + " renews on package " + targets.get(0));
  }

  private static String legacyLabel(CandidateSnapshot s) {
    return s.legacyPackageCode()
        + (s.legacyPackageVersion() == null ? "" : " version " + s.legacyPackageVersion());
  }

  private static String risk(CandidateSnapshot s) {
    return s.product() == null ? null : s.product().productCode();
  }

  private static BigDecimal si(CandidateSnapshot s) {
    return s.premium() == null ? null : s.premium().totalSumInsured();
  }
}
