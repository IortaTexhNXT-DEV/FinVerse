package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.DuplicateKeys;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Duplicate client detection (BRNB.032). Keys: exact TIN; exact ID type and number; exact e-mail;
 * exact mobile; last and first name with birth date (case- and space-insensitive); normalised
 * corporate name. TIN, ID and name with birth date are <b>hard</b> keys: creation and confirmation
 * are blocked and the blocked attempt is audited. The other keys only warn.
 */
@Service
@Transactional(readOnly = true)
public class DuplicateCheckService {

  /** Keys that block creation and confirmation. */
  public static final Set<String> HARD_KEYS = Set.of("TIN", "ID", "NAME_BIRTH_DATE");

  private final ClientRepository clients;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param audit audit trail
   * @param currentUser current user
   */
  public DuplicateCheckService(
      ClientRepository clients, AuditTrailService audit, CurrentUser currentUser) {
    this.clients = clients;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Existing clients matching the probe on any key.
   *
   * @param companyId company
   * @param probe identifying data
   * @param excludeId client being edited (null for a new client)
   * @return matches, hard matches first
   */
  public List<DuplicateMatch> candidates(Long companyId, DuplicateProbe probe, Long excludeId) {
    Map<Long, Found> found = new LinkedHashMap<>();
    collect(
        found, "TIN", () -> byValue(probe.tin(), t -> clients.findByCompanyIdAndTin(companyId, t)));
    String idKey = DuplicateKeys.idKey(probe.idType(), probe.idNumber());
    collect(
        found, "ID", () -> byValue(idKey, k -> clients.findByCompanyIdAndKeysIdKey(companyId, k)));
    String nameKey = DuplicateKeys.nameKey(probe.lastName(), probe.firstName());
    collect(
        found,
        "NAME_BIRTH_DATE",
        () ->
            probe.birthDate() == null
                ? List.of()
                : byValue(
                    nameKey,
                    k ->
                        clients.findByCompanyIdAndKeysNameKeyAndBirthDate(
                            companyId, k, probe.birthDate())));
    collect(
        found,
        "EMAIL",
        () ->
            byValue(
                DuplicateKeys.emailKey(probe.email()),
                e -> clients.findByCompanyIdAndEmailIgnoreCase(companyId, e)));
    collect(
        found,
        "MOBILE",
        () ->
            byValue(
                DuplicateKeys.mobileKey(probe.mobile()),
                m -> clients.findByCompanyIdAndKeysMobileKey(companyId, m)));
    collect(
        found,
        "CORPORATE_NAME",
        () ->
            byValue(
                DuplicateKeys.corporateKey(probe.corporateName()),
                k -> clients.findByCompanyIdAndKeysCorporateKey(companyId, k)));
    return found.values().stream()
        .filter(f -> !Objects.equals(f.client().getId(), excludeId))
        .map(
            f ->
                DuplicateMatch.of(
                    f.client(), f.keys(), f.keys().stream().anyMatch(HARD_KEYS::contains)))
        .sorted((a, b) -> Boolean.compare(b.hard(), a.hard()))
        .toList();
  }

  /**
   * Blocks the action on a hard match; the blocked attempt is audited in its own transaction so the
   * log survives the rejection.
   *
   * @param companyId company
   * @param probe identifying data
   * @param excludeId client being edited or confirmed (null for a new client)
   * @param attempt what was attempted, e.g. "creation of Dela Cruz, Juan"
   */
  public void requireNoHardMatch(
      Long companyId, DuplicateProbe probe, Long excludeId, String attempt) {
    List<DuplicateMatch> hard =
        candidates(companyId, probe, excludeId).stream().filter(DuplicateMatch::hard).toList();
    if (hard.isEmpty()) {
      return;
    }
    String existing =
        hard.stream()
            .map(m -> m.code() + " " + m.displayName() + " (" + String.join(", ", m.keys()) + ")")
            .collect(Collectors.joining("; "));
    audit.recordIndependently(
        currentUser.username(),
        ClientService.ENTITY,
        hard.get(0).code(),
        AuditAction.REJECT,
        "Duplicate blocked: " + attempt + " matches " + existing);
    throw new BusinessRuleException("CLIENT_DUPLICATE", "This client already exists: " + existing);
  }

  private static <T> List<Client> byValue(T value, Function<T, List<Client>> query) {
    if (value == null || value instanceof String s && s.isBlank()) {
      return List.of();
    }
    return query.apply(value);
  }

  private static void collect(Map<Long, Found> found, String key, Supplier<List<Client>> query) {
    for (Client c : query.get()) {
      found.computeIfAbsent(c.getId(), id -> new Found(c, new ArrayList<>())).keys().add(key);
    }
  }

  private record Found(Client client, List<String> keys) {}
}
