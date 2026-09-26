package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.screening.config.domain.SubjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A sanctioned-name, PEP or internal watchlist entry (SNSRP-201, 203). It is identified by source
 * and external reference, never deleted (delisting sets INACTIVE), and screened only while ACTIVE.
 * {@link #getEntryVersion()} goes up with every applied change so that a false-positive suppression
 * is lifted when the entry changes (SQ12).
 */
@Entity
@Table(name = "scr_watchlist_entry")
public class WatchlistEntry extends BaseEntity {

  @Column(name = "source_id", nullable = false, updatable = false)
  private Long sourceId;

  @Column(name = "external_ref", nullable = false, length = 60, updatable = false)
  private String externalRef;

  @Column(name = "list_type", nullable = false, length = 30)
  private String listType;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 20)
  private SubjectType entityType;

  @Column(name = "primary_name", nullable = false, length = 300)
  private String primaryName;

  @Column(name = "first_name", length = 100)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "nationality", length = 60)
  private String nationality;

  @Column(name = "id_numbers", length = 300)
  private String idNumbers;

  @Column(name = "listed_on")
  private LocalDate listedOn;

  @Column(name = "delisted_on")
  private LocalDate delistedOn;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EntryStatus status;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "entry_version", nullable = false)
  private int entryVersion;

  @Column(name = "remarks", length = 2000)
  private String remarks;

  /** For JPA. */
  protected WatchlistEntry() {}

  /**
   * Creates an entry with its first values, PENDING (manual or uploaded) until approved.
   *
   * @param sourceId source
   * @param externalRef reference in the source (unique per source)
   * @param values values
   * @param remarks remarks
   */
  public WatchlistEntry(Long sourceId, String externalRef, EntryValues values, String remarks) {
    this.sourceId = sourceId;
    this.externalRef = externalRef;
    this.status = EntryStatus.PENDING;
    this.remarks = remarks;
    write(values);
  }

  private void write(EntryValues v) {
    this.listType = v.listType();
    this.entityType = v.entityType();
    this.primaryName = v.primaryName();
    this.firstName = v.firstName();
    this.lastName = v.lastName();
    this.birthDate = v.birthDate();
    this.nationality = v.nationality();
    this.idNumbers = v.idNumbers();
    this.listedOn = v.listedOn();
    this.delistedOn = v.delistedOn();
  }

  /**
   * The current values (aliases are held in {@code scr_watchlist_alias} and added by the caller).
   *
   * @return values without aliases
   */
  public EntryValues values() {
    return new EntryValues(
        listType,
        entityType,
        primaryName,
        firstName,
        lastName,
        birthDate,
        nationality,
        idNumbers,
        listedOn,
        delistedOn,
        null);
  }

  /**
   * Applies an approved addition or update: the entry becomes ACTIVE from the date.
   *
   * @param values new values
   * @param remarksOfChange remarks of the change
   * @param effective effective date
   */
  public void activate(EntryValues values, String remarksOfChange, LocalDate effective) {
    write(values);
    this.status = EntryStatus.ACTIVE;
    this.effectiveFrom = effective;
    this.remarks = remarksOfChange;
    this.entryVersion++;
  }

  /**
   * Applies an approved deactivation or a delisting: INACTIVE with the delisting date.
   *
   * @param delisted delisting date
   * @param remarksOfChange remarks
   * @param effective effective date
   */
  public void delist(LocalDate delisted, String remarksOfChange, LocalDate effective) {
    this.status = EntryStatus.INACTIVE;
    this.delistedOn = delisted;
    this.effectiveFrom = effective;
    this.remarks = remarksOfChange;
    this.entryVersion++;
  }

  /** A pending addition was submitted for a draft entry again. */
  public void resubmit() {
    if (status == EntryStatus.DRAFT) {
      this.status = EntryStatus.PENDING;
    }
  }

  /** A pending addition was rejected: the entry goes back to DRAFT and is never screened. */
  public void backToDraft() {
    if (status == EntryStatus.PENDING) {
      this.status = EntryStatus.DRAFT;
    }
  }

  public Long getSourceId() {
    return sourceId;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public String getListType() {
    return listType;
  }

  public SubjectType getEntityType() {
    return entityType;
  }

  public String getPrimaryName() {
    return primaryName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getNationality() {
    return nationality;
  }

  public String getIdNumbers() {
    return idNumbers;
  }

  public LocalDate getListedOn() {
    return listedOn;
  }

  public LocalDate getDelistedOn() {
    return delistedOn;
  }

  public EntryStatus getStatus() {
    return status;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public int getEntryVersion() {
    return entryVersion;
  }

  public String getRemarks() {
    return remarks;
  }
}
