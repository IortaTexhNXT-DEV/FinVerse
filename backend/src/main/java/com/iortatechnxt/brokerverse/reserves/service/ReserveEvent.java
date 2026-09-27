package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import java.util.List;

/**
 * Accounting events posted by a valuation run and the reserve movement behind each amount
 * component. Every component is the signed movement (this run's closing balance − the previous
 * posted run's balance) of a reserve, gross or reinsurers' share, per branch and line of business.
 * OSLR is not posted here: claims books it with {@code CLAIM_RESERVE} and reinsurance its share
 * with {@code RI_RESERVE_SHARE}.
 */
public enum ReserveEvent {
  /** Gross UPR and reinsurers' share of UPR. */
  UPR_PROVISION(
      new Component("UPR_CHANGE", ReserveType.UPR, false),
      new Component("RI_UPR_CHANGE", ReserveType.UPR, true)),
  /** Deferred acquisition cost and deferred reinsurance commission (UCR). */
  DAC_PROVISION(
      new Component("DAC_CHANGE", ReserveType.DAC, false),
      new Component("DRC_CHANGE", ReserveType.DAC, true)),
  /** IBNR and reinsurers' share of IBNR. */
  IBNR_PROVISION(
      new Component("IBNR_CHANGE", ReserveType.IBNR, false),
      new Component("RI_IBNR_CHANGE", ReserveType.IBNR, true)),
  /** ULAE provision and margin for adverse deviation with its reinsurers' share. */
  CLAIM_MARGIN_PROVISION(
      new Component("ULAE_CHANGE", ReserveType.ULAE, false),
      new Component("MFAD_CHANGE", ReserveType.MFAD, false),
      new Component("RI_MFAD_CHANGE", ReserveType.MFAD, true)),
  /** Premium deficiency reserve. */
  PREMIUM_DEFICIENCY_PROVISION(new Component("PDR_CHANGE", ReserveType.PDR, false));

  private final List<Component> components;

  ReserveEvent(Component... components) {
    this.components = List.of(components);
  }

  /**
   * Amount components of the event.
   *
   * @return components
   */
  public List<Component> components() {
    return components;
  }

  /**
   * One amount component.
   *
   * @param name component name of the event type
   * @param type reserve whose movement it carries
   * @param reinsurance true for the reinsurers' share, false for the gross amount
   */
  public record Component(String name, ReserveType type, boolean reinsurance) {}
}
