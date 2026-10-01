package com.iortatechnxt.brokerverse.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.common.exception.DataScopeDeniedException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.security.UserDataScope;
import com.iortatechnxt.brokerverse.common.security.UserDataScope.CompanyScope;
import com.iortatechnxt.brokerverse.security.domain.UserDataScopeGrant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class DataScopeGuardTest {

  private static final UserDataScope RESTRICTED =
      UserDataScope.of(
          List.of(CompanyScope.allBranchesOf(1L), CompanyScope.branchesOf(2L, List.of(20L))));

  private final DataScopeLookup lookup = mock(DataScopeLookup.class);
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final OrganizationUnits units = mock(OrganizationUnits.class);
  private final DataScopeGuard guard = new DataScopeGuard(lookup, currentUser, units);

  @AfterEach
  void unbind() {
    RequestContextHolder.resetRequestAttributes();
  }

  private void userRequest(String path, String username, UserDataScope scope) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    when(currentUser.optionalUsername()).thenReturn(Optional.ofNullable(username));
    if (username != null) {
      when(lookup.scopeOf(username)).thenReturn(scope);
    }
  }

  @Test
  void backgroundProcessingRunsAsTheSystem() {
    assertThat(DataScopeGuard.systemProcessing()).isTrue();
    assertThat(guard.allowed()).isEqualTo(UserDataScope.ALL);
    assertThatCode(() -> guard.requireCompany(5L)).doesNotThrowAnyException();
  }

  @Test
  void integrationApisRunAsTheSystem() {
    userRequest("/integration/v1/ping", "gateway-client", UserDataScope.NONE);
    assertThat(DataScopeGuard.systemProcessing()).isTrue();
    assertThatCode(() -> guard.requireCompany(5L)).doesNotThrowAnyException();
  }

  @Test
  void aUserRequestWithoutUserIsRefused() {
    userRequest("/api/v1/gl/accounts", null, null);
    assertThat(guard.allowed()).isEqualTo(UserDataScope.NONE);
    assertThatThrownBy(() -> guard.requireCompany(1L))
        .isInstanceOf(DataScopeDeniedException.class)
        .hasMessageContaining("this company");
  }

  @Test
  void companiesOutsideTheScopeAreRefused() {
    userRequest("/api/v1/gl/accounts", "maker", RESTRICTED);
    assertThatCode(() -> guard.requireCompany(1L)).doesNotThrowAnyException();
    assertThatCode(() -> guard.requireCompany(null)).doesNotThrowAnyException();
    assertThatThrownBy(() -> guard.requireCompany(3L)).isInstanceOf(DataScopeDeniedException.class);
  }

  @Test
  void branchesOutsideTheScopeAreRefused() {
    userRequest("/api/v1/dashboard", "maker", RESTRICTED);
    assertThatCode(() -> guard.requireBranch(1L, 99L)).doesNotThrowAnyException();
    assertThatCode(() -> guard.requireBranch(2L, 20L)).doesNotThrowAnyException();
    assertThatCode(() -> guard.requireBranch(2L, null)).doesNotThrowAnyException();
    assertThatThrownBy(() -> guard.requireBranch(2L, 21L))
        .isInstanceOf(DataScopeDeniedException.class)
        .hasMessageContaining("this branch");
    assertThatThrownBy(() -> guard.requireBranch(3L, 30L))
        .isInstanceOf(DataScopeDeniedException.class)
        .hasMessageContaining("this company");
  }

  @Test
  void aBranchWithoutItsCompanyIsLookedUp() {
    userRequest("/api/v1/cashiering/cwt/1/settle-cash", "maker", RESTRICTED);
    when(units.companyOfBranch(20L)).thenReturn(Optional.of(2L));
    when(units.companyOfBranch(21L)).thenReturn(Optional.of(2L));
    when(units.companyOfBranch(30L)).thenReturn(Optional.of(3L));
    when(units.companyOfBranch(40L)).thenReturn(Optional.empty());
    assertThatCode(() -> guard.requireBranch(null, 20L)).doesNotThrowAnyException();
    assertThatThrownBy(() -> guard.requireBranch(null, 21L))
        .isInstanceOf(DataScopeDeniedException.class);
    assertThatThrownBy(() -> guard.requireBranch(null, 30L))
        .isInstanceOf(DataScopeDeniedException.class)
        .hasMessageContaining("this branch");
    assertThatThrownBy(() -> guard.requireBranch(null, 40L))
        .isInstanceOf(DataScopeDeniedException.class);
  }

  @Test
  void allCompaniesPassesEveryBranch() {
    userRequest("/api/v1/dashboard", "admin", UserDataScope.ALL);
    assertThatCode(() -> guard.requireBranch(null, 40L)).doesNotThrowAnyException();
  }

  @Test
  void grantsBecomeAScope() {
    Instant now = Instant.parse("2026-10-01T00:00:00Z");
    UserDataScope scope =
        DataScopeLookup.toScope(
            List.of(
                new UserDataScopeGrant(7L, 1L, null, now, "admin"),
                new UserDataScopeGrant(7L, 2L, 20L, now, "admin"),
                new UserDataScopeGrant(7L, 2L, 22L, now, "admin")));
    assertThat(scope.text()).isEqualTo("1:*;2:20,22");
    assertThat(DataScopeLookup.toScope(List.of())).isEqualTo(UserDataScope.NONE);
    assertThat(new DataScopeLookup(null, null).key("MaKer")).isEqualTo("maker");
    assertThat(new DataScopeLookup(null, null).key(null)).isEmpty();
  }
}
