package com.iortatechnxt.brokerverse.lov.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovType;
import com.iortatechnxt.brokerverse.lov.domain.LovTypeRepository;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.domain.LovValueRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may maintain the values of a list (bean {@code lovAccess}, used in the {@code @PreAuthorize}
 * expressions of the list API). Administrators maintain every list with {@code LOV_MANAGE} and
 * authorize changes with {@code MASTER_AUTHORIZE}. A list with an owner permission ({@code
 * lov_type.owner_permission}, CLAIMS_BROKING_DESIGN 12.1) may also be read, maintained and
 * authorized by the holders of that permission, e.g. the Claims Unit Head ({@code BCL_SETUP}) for
 * the Claims lists (BRCLM.010/014/017/036). Maker-checker still applies: the maker of a change
 * never authorizes it.
 */
@Component("lovAccess")
@Transactional(readOnly = true)
public class LovAccess {

  private static final String LOV_MANAGE = "LOV_MANAGE";
  private static final String MASTER_AUTHORIZE = "MASTER_AUTHORIZE";

  private final LovTypeRepository types;
  private final LovValueRepository values;
  private final CurrentUser currentUser;

  /**
   * Creates the component.
   *
   * @param types list types
   * @param values list values
   * @param currentUser current user
   */
  public LovAccess(LovTypeRepository types, LovValueRepository values, CurrentUser currentUser) {
    this.types = types;
    this.values = values;
    this.currentUser = currentUser;
  }

  /**
   * Whether the current user may list every value of a list, whatever its status.
   *
   * @param typeCode list
   * @return true with LOV_MANAGE, MASTER_AUTHORIZE or the list's owner permission
   */
  public boolean canRead(String typeCode) {
    return currentUser.hasAuthority(LOV_MANAGE)
        || currentUser.hasAuthority(MASTER_AUTHORIZE)
        || holdsOwnerPermission(typeCode);
  }

  /**
   * Whether the current user may add a value to a list.
   *
   * @param typeCode list
   * @return true with LOV_MANAGE or the list's owner permission
   */
  public boolean canMaintain(String typeCode) {
    return currentUser.hasAuthority(LOV_MANAGE) || holdsOwnerPermission(typeCode);
  }

  /**
   * Whether the current user may change or deactivate a value.
   *
   * @param valueId value id
   * @return true with LOV_MANAGE or the owner permission of the value's list
   */
  public boolean canMaintainValue(Long valueId) {
    return currentUser.hasAuthority(LOV_MANAGE) || holdsOwnerPermission(typeOf(valueId));
  }

  /**
   * Whether the current user may authorize a new or changed value.
   *
   * @param valueId value id
   * @return true with MASTER_AUTHORIZE or the owner permission of the value's list
   */
  public boolean canAuthorizeValue(Long valueId) {
    return currentUser.hasAuthority(MASTER_AUTHORIZE) || holdsOwnerPermission(typeOf(valueId));
  }

  private String typeOf(Long valueId) {
    return values.findById(valueId).map(LovValue::getTypeCode).orElse(null);
  }

  private boolean holdsOwnerPermission(String typeCode) {
    if (typeCode == null) {
      return false;
    }
    Optional<String> owner = types.findByCode(typeCode).map(LovType::getOwnerPermission);
    return owner.isPresent() && currentUser.hasAuthority(owner.get());
  }
}
