package com.iortatechnxt.brokerverse.account.domain;

import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Risk items, for the duplicate fall-out and identifier look-ups (BRNB.051/066/113). */
public interface RiskItemRepository extends JpaRepository<RiskItem, Long> {

  /**
   * Vehicles of other live accounts sharing a plate, conduction sticker, engine or chassis number.
   *
   * @param companyId company
   * @param excludeId account to leave out (-1 for none)
   * @param closed statuses that no longer count (voided, cancelled)
   * @param kind VEHICLE
   * @param ids normalised identifiers (plate, sticker, engine, chassis)
   * @return matching items with their accounts
   */
  @Query(
      "select i from RiskItem i join fetch i.account a where a.companyId = :companyId"
          + " and a.id <> :excludeId and a.status not in :closed and i.kind = :kind"
          + " and (i.plateNo in :ids or i.conductionSticker in :ids or i.engineNo in :ids"
          + " or i.chassisNo in :ids)")
  List<RiskItem> findVehicles(
      @Param("companyId") Long companyId,
      @Param("excludeId") Long excludeId,
      @Param("closed") Collection<AccountStatus> closed,
      @Param("kind") RiskItemKind kind,
      @Param("ids") Collection<String> ids);

  /**
   * Locations of risk of the same client on other live accounts.
   *
   * @param clientId client
   * @param excludeId account to leave out (-1 for none)
   * @param closed statuses that no longer count
   * @param keys location keys
   * @return matching items with their accounts
   */
  @Query(
      "select i from RiskItem i join fetch i.account a where a.clientId = :clientId"
          + " and a.id <> :excludeId and a.status not in :closed and i.locationKey in :keys")
  List<RiskItem> findLocations(
      @Param("clientId") Long clientId,
      @Param("excludeId") Long excludeId,
      @Param("closed") Collection<AccountStatus> closed,
      @Param("keys") Collection<String> keys);
}
