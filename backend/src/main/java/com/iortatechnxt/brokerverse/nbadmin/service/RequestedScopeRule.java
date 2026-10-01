package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.security.service.DataScopeService;

/**
 * The data scope a user request may carry (V1240, DATA_SCOPE_DESIGN.md): only a creation or a
 * modification of a user sets it; it is dropped from the other requests, and checked when given
 * (known companies and branches, within the scope of the requester or approver).
 */
final class RequestedScopeRule {

  private RequestedScopeRule() {}

  static RequestedUserData check(
      AccessRequestType type, RequestedUserData data, DataScopeService dataScopes) {
    if (data.dataScope() == null) {
      return data;
    }
    if (!type.carriesUserRoles()) {
      return data.withoutDataScope();
    }
    dataScopes.validateRequested(data.dataScope());
    return data;
  }
}
