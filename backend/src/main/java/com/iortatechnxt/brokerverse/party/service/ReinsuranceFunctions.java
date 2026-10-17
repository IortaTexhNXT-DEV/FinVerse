package com.iortatechnxt.brokerverse.party.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyRepository;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * The reinsurance functions of the platform, deferred to Phase 2 for BDOI (client decision of
 * 17-Oct-2026; Decision Log): reinsurer and reinsurance broker parties, receipts from a reinsurer
 * and reinsurance settlement payments. They stay in the code and are held only through the
 * permission {@link Permission#RI_BROKING_TRANSACT}, which no BDOI group profile holds; the super
 * user of DEV, SIT and UAT holds it, like every permission. A user without it gets 403.
 */
@Component
public class ReinsuranceFunctions {

  /** Party types of the reinsurance functions. */
  public static final Set<PartyType> PARTY_TYPES =
      EnumSet.of(PartyType.REINSURER, PartyType.RI_BROKER);

  private final CurrentUser currentUser;
  private final PartyRepository parties;

  /**
   * Creates the check.
   *
   * @param currentUser the signed-in user
   * @param parties parties (the type of a payee)
   */
  public ReinsuranceFunctions(CurrentUser currentUser, PartyRepository parties) {
    this.currentUser = currentUser;
    this.parties = parties;
  }

  /**
   * Whether the signed-in user may use the reinsurance functions.
   *
   * @return true when the user holds {@link Permission#RI_BROKING_TRANSACT}
   */
  public boolean allowed() {
    return currentUser.hasAuthority(Permission.RI_BROKING_TRANSACT.name());
  }

  /**
   * Refuses a reinsurance function to a user without the permission.
   *
   * @param reinsurance whether the request is a reinsurance function
   */
  public void require(boolean reinsurance) {
    if (reinsurance && !allowed()) {
      throw new AccessDeniedException("Reinsurance is not in use (deferred to Phase 2)");
    }
  }

  /**
   * Refuses a party of a reinsurance type to a user without the permission.
   *
   * @param type party type
   */
  public void requireForType(PartyType type) {
    require(type != null && PARTY_TYPES.contains(type));
  }

  /**
   * Refuses a transaction with a reinsurer or reinsurance broker to a user without the permission.
   *
   * @param companyId company
   * @param partyCode party code (may be null)
   */
  public void requireForParty(Long companyId, String partyCode) {
    if (partyCode == null || companyId == null || allowed()) {
      return;
    }
    requireForType(
        parties.findByCompanyIdAndCode(companyId, partyCode).map(Party::getPartyType).orElse(null));
  }
}
