package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXrefRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The legacy key cross-reference (DATA_MIGRATION_DESIGN section 11): which BIBS record a legacy key
 * was loaded into, used by the loads (skip or update), by the referential rules and loaders of
 * dependent objects (the client of a policy, the header of an invoice) and by the search of
 * migrated records by legacy key.
 */
@Service
@Transactional
public class XrefService {

  private final KeyXrefRepository xrefs;

  /**
   * Creates the service.
   *
   * @param xrefs cross-references
   */
  public XrefService(KeyXrefRepository xrefs) {
    this.xrefs = xrefs;
  }

  /**
   * The entry of a legacy key.
   *
   * @param companyId company
   * @param source source system
   * @param objectCode object
   * @param legacyKey legacy key
   * @return entry
   */
  @Transactional(readOnly = true)
  public Optional<KeyXref> find(
      Long companyId, String source, String objectCode, String legacyKey) {
    return xrefs.findByCompanyIdAndSourceSystemAndObjectCodeAndLegacyKey(
        companyId, source, objectCode, legacyKey);
  }

  /**
   * The live entry of a legacy key of any source system (the first one).
   *
   * @param companyId company
   * @param objectCode object
   * @param legacyKey legacy key
   * @return entry
   */
  @Transactional(readOnly = true)
  public Optional<KeyXref> live(Long companyId, String objectCode, String legacyKey) {
    return xrefs
        .findByCompanyIdAndObjectCodeAndLegacyKeyInAndRolledBackAtIsNull(
            companyId, objectCode, List.of(legacyKey))
        .stream()
        .findFirst();
  }

  /**
   * The live entry of a legacy key, preferring the given source system.
   *
   * @param companyId company
   * @param source preferred source system
   * @param objectCode object
   * @param legacyKey legacy key
   * @return entry
   */
  @Transactional(readOnly = true)
  public Optional<KeyXref> live(
      Long companyId, String source, String objectCode, String legacyKey) {
    Optional<KeyXref> exact =
        find(companyId, source, objectCode, legacyKey).filter(KeyXref::isLive);
    return exact.isPresent() ? exact : live(companyId, objectCode, legacyKey);
  }

  /**
   * The keys among the given ones that are loaded (live) for an object.
   *
   * @param companyId company
   * @param objectCode object
   * @param keys legacy keys
   * @return loaded keys
   */
  @Transactional(readOnly = true)
  public Set<String> loaded(Long companyId, String objectCode, Collection<String> keys) {
    Set<String> out = new HashSet<>();
    List<String> list = keys.stream().filter(k -> k != null && !k.isBlank()).distinct().toList();
    int page = 1000;
    for (int from = 0; from < list.size(); from += page) {
      xrefs
          .findByCompanyIdAndObjectCodeAndLegacyKeyInAndRolledBackAtIsNull(
              companyId, objectCode, list.subList(from, Math.min(list.size(), from + page)))
          .forEach(x -> out.add(x.getLegacyKey()));
    }
    return out;
  }

  /**
   * Saves an entry.
   *
   * @param entry entry
   * @return saved entry
   */
  public KeyXref save(KeyXref entry) {
    return xrefs.save(entry);
  }

  /**
   * Live entries of a batch, newest first.
   *
   * @param batchId batch
   * @return entries
   */
  @Transactional(readOnly = true)
  public List<KeyXref> ofBatch(Long batchId) {
    return xrefs.findByBatchIdAndRolledBackAtIsNullOrderByIdDesc(batchId);
  }
}
