package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.ChangeType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistAlias;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistAliasRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChange;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistChangeRepository;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistEntryRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Low-level watchlist operations shared by manual maintenance and ingestion (SNSRP-201, 203, 204):
 * full values with aliases, pending changes with before / after values (JSON) and applying an
 * approved change to its entry.
 */
@Component
@Transactional
public class WatchlistStore {

  private final WatchlistEntryRepository entries;
  private final WatchlistAliasRepository aliases;
  private final WatchlistChangeRepository changes;
  private final ObjectMapper json;

  /**
   * Creates the store.
   *
   * @param entries entries
   * @param aliases aliases
   * @param changes changes
   * @param json JSON mapper
   */
  public WatchlistStore(
      WatchlistEntryRepository entries,
      WatchlistAliasRepository aliases,
      WatchlistChangeRepository changes,
      ObjectMapper json) {
    this.entries = entries;
    this.aliases = aliases;
    this.changes = changes;
    this.json = json;
  }

  /**
   * An entry.
   *
   * @param id id
   * @return entry
   */
  @Transactional(readOnly = true)
  public WatchlistEntry entry(Long id) {
    return entries
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Watchlist entry", id));
  }

  /**
   * The values of an entry with its aliases.
   *
   * @param entry entry
   * @return values
   */
  @Transactional(readOnly = true)
  public EntryValues values(WatchlistEntry entry) {
    EntryValues v = entry.values();
    return new EntryValues(
        v.listType(),
        v.entityType(),
        v.primaryName(),
        v.firstName(),
        v.lastName(),
        v.birthDate(),
        v.nationality(),
        v.idNumbers(),
        v.listedOn(),
        v.delistedOn(),
        aliases.findByEntryIdOrderByIdAsc(entry.getId()).stream()
            .map(a -> new EntryValues.Alias(a.getAliasName(), a.getAliasType()))
            .toList());
  }

  /**
   * The pending change of an entry.
   *
   * @param entryId entry
   * @return the change, empty when none
   */
  @Transactional(readOnly = true)
  public Optional<WatchlistChange> pending(Long entryId) {
    return changes.findFirstByEntryIdAndStatus(entryId, ChangeStatus.PENDING);
  }

  /**
   * Refuses a second change while one waits for approval (FR-SS-022).
   *
   * @param entry entry
   */
  public void requireNoPending(WatchlistEntry entry) {
    if (pending(entry.getId()).isPresent()) {
      throw new BusinessRuleException(
          "SCR_ENTRY_CHANGE_PENDING",
          "Entry " + entry.getExternalRef() + " already has a change waiting for approval");
    }
  }

  /**
   * Records a change (PENDING).
   *
   * @param entry entry
   * @param runId run, {@code null} for a manual change
   * @param type kind
   * @param before values before, {@code null} for an addition
   * @param after values after
   * @param remarks maker remarks
   * @return the change
   */
  public WatchlistChange stage(
      WatchlistEntry entry,
      Long runId,
      ChangeType type,
      EntryValues before,
      EntryValues after,
      String remarks) {
    return new WatchlistChange(
        entry.getId(), runId, type, before == null ? null : write(before), write(after), remarks);
  }

  /**
   * Saves a change.
   *
   * @param change change
   * @return saved change
   */
  public WatchlistChange save(WatchlistChange change) {
    return changes.save(change);
  }

  /**
   * Applies an approved change to its entry: ADD / UPDATE set the values and aliases and make the
   * entry ACTIVE; DEACTIVATE delists it.
   *
   * @param change the approved change
   * @param entry its entry
   * @param today effective date
   */
  public void apply(WatchlistChange change, WatchlistEntry entry, LocalDate today) {
    EntryValues after = read(change.getAfterValues());
    if (change.getChangeType() == ChangeType.DEACTIVATE) {
      LocalDate delisted = after.delistedOn() == null ? today : after.delistedOn();
      entry.delist(delisted, change.getMakerRemarks(), today);
      return;
    }
    entry.activate(after, change.getMakerRemarks(), today);
    aliases.deleteByEntryId(entry.getId());
    aliases.saveAll(
        after.aliases().stream()
            .map(a -> new WatchlistAlias(entry.getId(), a.name(), a.type()))
            .toList());
  }

  /**
   * Values as JSON.
   *
   * @param values values
   * @return JSON
   */
  public String write(EntryValues values) {
    try {
      return json.writeValueAsString(values);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot store watchlist values", ex);
    }
  }

  /**
   * Values from JSON.
   *
   * @param text JSON, may be {@code null}
   * @return values, {@code null} for {@code null}
   */
  public EntryValues read(String text) {
    if (text == null) {
      return null;
    }
    try {
      return json.readValue(text, EntryValues.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot read watchlist values", ex);
    }
  }
}
