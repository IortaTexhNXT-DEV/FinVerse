package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.AliasType;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryValues;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Checks of a manual watchlist addition or change (FR-SS-022): name, list type, remarks and entity
 * type are mandatory, the list type must be a value of SCR_LIST_TYPE, the birth date not in the
 * future and the delisting not before the listing. The messages are those of the FRS.
 */
@Component
public class EntryValidator {

  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param lovs lists of values
   * @param clock clock
   */
  public EntryValidator(LovService lovs, Clock clock) {
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Checks and cleans the values of a manual change (FR-SS-022 validations).
   *
   * @param v values
   * @param remarks reason for the change
   * @return trimmed values
   */
  public EntryValues validate(EntryValues v, String remarks) {
    if (v == null || blank(v.primaryName())) {
      throw new BusinessRuleException(
          "SCR_ENTRY_NAME_REQUIRED", "Enter the name of the listed person or entity");
    }
    if (blank(v.listType())) {
      throw new BusinessRuleException("SCR_ENTRY_LIST_TYPE_REQUIRED", "Select the list type");
    }
    requireRemarks(remarks);
    if (v.entityType() == null) {
      throw new BusinessRuleException(
          "SCR_ENTRY_TYPE_REQUIRED", "Select the entity type (individual or entity)");
    }
    lovs.requireValid("SCR_LIST_TYPE", v.listType().trim(), today());
    requireDates(v);
    List<EntryValues.Alias> aliases = new ArrayList<>();
    for (EntryValues.Alias a : v.aliases()) {
      if (a != null && !blank(a.name())) {
        aliases.add(
            new EntryValues.Alias(a.name().trim(), a.type() == null ? AliasType.AKA : a.type()));
      }
    }
    return new EntryValues(
        v.listType().trim(),
        v.entityType(),
        v.primaryName().trim(),
        trim(v.firstName()),
        trim(v.lastName()),
        v.birthDate(),
        trim(v.nationality()),
        trim(v.idNumbers()),
        v.listedOn(),
        v.delistedOn(),
        aliases);
  }

  private void requireDates(EntryValues v) {
    if (v.birthDate() != null && v.birthDate().isAfter(today())) {
      throw new BusinessRuleException(
          "SCR_ENTRY_BIRTH_DATE", "The birth date cannot be in the future");
    }
    if (v.listedOn() != null && v.delistedOn() != null && v.delistedOn().isBefore(v.listedOn())) {
      throw new BusinessRuleException(
          "SCR_ENTRY_DELISTING_DATE", "The delisting date cannot be before the listing date");
    }
  }

  /**
   * Refuses blank remarks.
   *
   * @param remarks remarks
   */
  public static void requireRemarks(String remarks) {
    if (blank(remarks)) {
      throw new BusinessRuleException(
          "SCR_ENTRY_REMARKS_REQUIRED", "Enter the reason for the change");
    }
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String trim(String value) {
    return blank(value) ? null : value.trim();
  }
}
