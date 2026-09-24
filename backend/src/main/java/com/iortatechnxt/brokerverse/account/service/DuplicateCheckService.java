package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskIdentifiers;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.account.domain.RiskItemRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Duplicate fall-out (BRNB.032/051/066): a risk may be on only one live (not voided, not cancelled)
 * account.
 *
 * <ul>
 *   <li>Motor: the same plate, conduction sticker, engine or chassis / serial number, except
 *       between a CTPL (CTP01 / CTP02) and a comprehensive motor account (MTR...).
 *   <li>Fire: the same client, the same location of risk (normalised address and city) and the same
 *       item(s) insured (an insured item description in common, or no items on either).
 * </ul>
 *
 * <p>Findings name the existing ARN. Endorsements may duplicate: they are reported as findings and
 * flagged instead of rejected ({@link #check} with {@code endorsement = true}, used by booking).
 */
@Service
@Transactional(readOnly = true)
public class DuplicateCheckService {

  /** Statuses that no longer hold the risk. */
  public static final Set<AccountStatus> CLOSED =
      EnumSet.of(AccountStatus.VOIDED, AccountStatus.CANCELLED);

  private static final Long NONE = -1L;
  private static final String NO_MATCH = "#";
  private static final Set<String> CTPL = Set.of("CTP01", "CTP02");
  private static final String MOTOR_PACKAGE_PREFIX = "MTR";

  private final RiskItemRepository items;

  /**
   * Creates the service.
   *
   * @param items risk items
   */
  public DuplicateCheckService(RiskItemRepository items) {
    this.items = items;
  }

  /**
   * Finds live accounts that already insure the risks of a record.
   *
   * @param subject company, client, product, items and the account itself (to leave out)
   * @return findings, empty when the risks are free
   */
  public List<DuplicateFinding> findings(DuplicateSubject subject) {
    List<DuplicateFinding> result = new ArrayList<>();
    result.addAll(vehicleFindings(subject));
    result.addAll(locationFindings(subject));
    return result;
  }

  /**
   * Rejects duplicates, naming the existing ARN(s); endorsements are allowed and returned flagged.
   *
   * @param subject record to check
   * @param endorsement endorsement transaction (duplicates allowed, flagged)
   * @return findings (only for endorsements; otherwise empty)
   */
  public List<DuplicateFinding> check(DuplicateSubject subject, boolean endorsement) {
    List<DuplicateFinding> found = findings(subject);
    if (found.isEmpty() || endorsement) {
      return found;
    }
    String arns =
        found.stream()
            .map(DuplicateFinding::existingArn)
            .distinct()
            .collect(Collectors.joining(", "));
    throw new BusinessRuleException(
        "DUPLICATE_ACCOUNT",
        "Duplicate of existing account "
            + arns
            + ": "
            + found.stream().map(DuplicateFinding::message).collect(Collectors.joining("; ")));
  }

  private List<DuplicateFinding> vehicleFindings(DuplicateSubject s) {
    Map<Integer, Vehicle> vehicles = new LinkedHashMap<>();
    for (int i = 0; i < s.items().size(); i++) {
      Vehicle v = s.items().get(i).vehicle();
      if (v != null) {
        vehicles.put(i + 1, normalised(v));
      }
    }
    Set<String> ids = new HashSet<>();
    vehicles.values().forEach(v -> addIds(ids, v));
    if (ids.isEmpty()) {
      return List.of();
    }
    List<RiskItem> matches =
        items.findVehicles(
            s.companyId(), excluded(s), CLOSED, RiskItemKind.VEHICLE, withSentinel(ids));
    List<DuplicateFinding> result = new ArrayList<>();
    for (RiskItem other : matches) {
      if (ctplException(s.productCode(), other.getAccount().getProductCode())) {
        continue;
      }
      vehicles.forEach((itemNo, v) -> matchVehicle(itemNo, v, other).ifPresent(result::add));
    }
    return result;
  }

  private static Optional<DuplicateFinding> matchVehicle(int itemNo, Vehicle mine, RiskItem other) {
    String[][] pairs = {
      {"plate number", mine.plateNo(), other.getPlateNo()},
      {"conduction sticker", mine.conductionSticker(), other.getConductionSticker()},
      {"engine number", mine.engineNo(), other.getEngineNo()},
      {"chassis number", mine.chassisNo(), other.getChassisNo()}
    };
    for (String[] pair : pairs) {
      if (pair[1] != null && pair[1].equals(pair[2])) {
        return Optional.of(
            new DuplicateFinding(
                itemNo,
                pair[0],
                pair[1],
                other.getAccount().getArn(),
                other.getAccount().getProductCode()));
      }
    }
    return Optional.empty();
  }

  private List<DuplicateFinding> locationFindings(DuplicateSubject s) {
    Map<String, Integer> keys = new LinkedHashMap<>();
    Map<Integer, Set<String>> insured = new LinkedHashMap<>();
    for (int i = 0; i < s.items().size(); i++) {
      RiskItemData.Location l = s.items().get(i).location();
      String key = l == null ? null : RiskIdentifiers.locationKey(l.address(), l.city());
      if (key != null && s.clientId() != null) {
        keys.putIfAbsent(key, i + 1);
        insured.put(i + 1, itemKeys(l.insuredItems()));
      }
    }
    if (keys.isEmpty()) {
      return List.of();
    }
    List<DuplicateFinding> result = new ArrayList<>();
    for (RiskItem other : items.findLocations(s.clientId(), excluded(s), CLOSED, keys.keySet())) {
      Integer itemNo = keys.get(other.getLocationKey());
      if (itemNo != null && sameItems(insured.get(itemNo), itemKeys(other.getInsuredItems()))) {
        result.add(
            new DuplicateFinding(
                itemNo,
                "location of risk",
                other.getLocationKey(),
                other.getAccount().getArn(),
                other.getAccount().getProductCode()));
      }
    }
    return result;
  }

  private static boolean sameItems(Set<String> mine, Set<String> theirs) {
    if (mine.isEmpty() || theirs.isEmpty()) {
      return mine.isEmpty() && theirs.isEmpty();
    }
    return mine.stream().anyMatch(theirs::contains);
  }

  private static Set<String> itemKeys(List<InsuredItem> insuredItems) {
    return insuredItems.stream()
        .map(i -> RiskIdentifiers.text(i.description()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static boolean ctplException(String mine, String theirs) {
    return CTPL.contains(mine) && isMotorPackage(theirs)
        || isMotorPackage(mine) && CTPL.contains(theirs);
  }

  private static boolean isMotorPackage(String product) {
    return product != null && product.toUpperCase(Locale.ROOT).startsWith(MOTOR_PACKAGE_PREFIX);
  }

  private static Vehicle normalised(Vehicle v) {
    return new Vehicle(
        RiskIdentifiers.vehicleId(v.plateNo()),
        RiskIdentifiers.vehicleId(v.conductionSticker()),
        RiskIdentifiers.vehicleId(v.engineNo()),
        RiskIdentifiers.vehicleId(v.chassisNo()),
        v.make(),
        v.model(),
        v.yearModel(),
        v.bodyType(),
        v.colour(),
        v.seatingCapacity());
  }

  private static void addIds(Set<String> ids, Vehicle v) {
    for (String id :
        new String[] {v.plateNo(), v.conductionSticker(), v.engineNo(), v.chassisNo()}) {
      if (id != null) {
        ids.add(id);
      }
    }
  }

  private static Set<String> withSentinel(Set<String> ids) {
    return ids.isEmpty() ? Set.of(NO_MATCH) : ids;
  }

  private static Long excluded(DuplicateSubject s) {
    return s.accountId() == null ? NONE : s.accountId();
  }

  /**
   * What is checked.
   *
   * @param companyId company
   * @param accountId account itself (left out), null for a new record
   * @param clientId client
   * @param productCode product
   * @param items risk items
   */
  public record DuplicateSubject(
      Long companyId, Long accountId, Long clientId, String productCode, List<RiskItemData> items) {

    /** Defensive copy. */
    public DuplicateSubject {
      items = items == null ? List.of() : List.copyOf(items);
    }
  }
}
