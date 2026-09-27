package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The PACKAGE code map of migrated policies (DMQ36): legacy package code and version, optional
 * qualifiers, and the BIBS package version it renews on, or REJECT. Loaded by the migration (source
 * MIGRATION, through the bulk upload) and maintained in Renewal Setup (source SETUP); every entry
 * waits for a checker other than its maker before the sanitation check reads it.
 */
@Service
@Transactional
public class PackageMapService {

  /** Audit entity of a map entry. */
  public static final String ENTITY = "RenewalPackageMap";

  /** Source of entries loaded by the migration. */
  public static final String SOURCE_MIGRATION = "MIGRATION";

  /** Source of entries maintained in Renewal Setup. */
  public static final String SOURCE_SETUP = "SETUP";

  private static final Set<ProductVersionStatus> USABLE =
      EnumSet.of(ProductVersionStatus.RELEASED, ProductVersionStatus.SUPERSEDED);

  private final PackageMapEntryRepository entries;
  private final ProductVersionRepository versions;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param entries map entries
   * @param versions package versions
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PackageMapService(
      PackageMapEntryRepository entries,
      ProductVersionRepository versions,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.entries = entries;
    this.versions = versions;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The map of a company.
   *
   * @param companyId company
   * @return entries by legacy code
   */
  @Transactional(readOnly = true)
  public List<PackageMapEntry> entries(Long companyId) {
    return entries.findByCompanyIdOrderByLegacyPackageCodeAscIdAsc(companyId);
  }

  /**
   * Adds an entry pending authorization.
   *
   * @param companyId company
   * @param data entry
   * @param source MIGRATION or SETUP
   * @return entry
   */
  public PackageMapEntry create(Long companyId, PackageMapEntry.Data data, String source) {
    List<String> problems = problems(data);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("RNW_PACKAGE_MAP_INVALID", String.join("; ", problems));
    }
    PackageMapEntry entry = entries.save(new PackageMapEntry(companyId, clean(data), source));
    audit.record(ENTITY, entry.getId(), AuditAction.CREATE, describe(entry));
    return entry;
  }

  /**
   * Changes an entry; it must be authorized again.
   *
   * @param companyId company
   * @param id entry
   * @param data entry
   * @return entry
   */
  public PackageMapEntry update(Long companyId, Long id, PackageMapEntry.Data data) {
    List<String> problems = problems(data);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("RNW_PACKAGE_MAP_INVALID", String.join("; ", problems));
    }
    PackageMapEntry entry = entry(companyId, id);
    entry.update(clean(data));
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(entry));
    return entry;
  }

  /**
   * Authorizes an entry.
   *
   * @param companyId company
   * @param id entry
   * @return entry
   */
  public PackageMapEntry authorize(Long companyId, Long id) {
    PackageMapEntry entry = entry(companyId, id);
    entry.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, describe(entry));
    return entry;
  }

  /**
   * Deactivates an entry.
   *
   * @param companyId company
   * @param id entry
   * @return entry
   */
  public PackageMapEntry deactivate(Long companyId, Long id) {
    PackageMapEntry entry = entry(companyId, id);
    entry.deactivate();
    audit.record(ENTITY, id, AuditAction.DEACTIVATE, describe(entry));
    return entry;
  }

  /**
   * Problems of an entry (form and upload validation).
   *
   * @param data entry
   * @return messages, empty when valid
   */
  @Transactional(readOnly = true)
  public List<String> problems(PackageMapEntry.Data data) {
    List<String> problems = new ArrayList<>();
    if (data.legacyPackageCode() == null || data.legacyPackageCode().isBlank()) {
      problems.add("Enter the legacy package code");
    }
    if (data.siFrom() != null && data.siTo() != null && data.siFrom().compareTo(data.siTo()) > 0) {
      problems.add("The sum insured band ends before it starts");
    }
    if (data.productCode() != null) {
      problems.addAll(targetProblems(data.productCode(), data.productVersionNo()));
    }
    return problems;
  }

  /**
   * Problems of a BIBS package version chosen as a target.
   *
   * @param productCode package (risk code)
   * @param versionNo version
   * @return messages, empty when the version is released
   */
  @Transactional(readOnly = true)
  public List<String> targetProblems(String productCode, Integer versionNo) {
    if (versionNo == null) {
      return List.of("Enter the version of package " + productCode);
    }
    boolean usable =
        versions
            .findByProductCodeAndVersionNo(productCode, versionNo)
            .map(ProductVersion::getStatus)
            .filter(USABLE::contains)
            .isPresent();
    return usable
        ? List.of()
        : List.of("Package " + productCode + " version " + versionNo + " is not released");
  }

  private PackageMapEntry entry(Long companyId, Long id) {
    return entries
        .findById(id)
        .filter(e -> e.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Package map entry", id));
  }

  private static PackageMapEntry.Data clean(PackageMapEntry.Data d) {
    return new PackageMapEntry.Data(
        d.legacyPackageCode().strip(),
        blankToNull(d.legacyPackageVersion()),
        blankToNull(d.riskCode()),
        blankToNull(d.insurerCode()),
        d.siFrom(),
        d.siTo(),
        blankToNull(d.productCode()),
        d.productCode() == null ? null : d.productVersionNo(),
        blankToNull(d.remarks()));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String describe(PackageMapEntry e) {
    String legacy =
        e.getLegacyPackageCode()
            + (e.getLegacyPackageVersion() == null ? "" : " v" + e.getLegacyPackageVersion());
    return e.isReject()
        ? "Legacy package " + legacy + " has no BIBS package"
        : "Legacy package "
            + legacy
            + " renews on "
            + e.getProductCode()
            + " v"
            + e.getProductVersionNo();
  }
}
