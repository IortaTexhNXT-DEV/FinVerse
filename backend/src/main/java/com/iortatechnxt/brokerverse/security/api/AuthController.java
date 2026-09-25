package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.api.dto.LoginRequest;
import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.api.dto.PasswordChangeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.UserProfileResponse;
import com.iortatechnxt.brokerverse.security.service.AuthService;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints: login, logout, current profile and own password change. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String BEARER = "Bearer ";

  private final AuthService authService;
  private final UserAdminService users;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param authService authentication
   * @param users user administration
   * @param currentUser current user
   */
  public AuthController(AuthService authService, UserAdminService users, CurrentUser currentUser) {
    this.authService = authService;
    this.users = users;
    this.currentUser = currentUser;
  }

  /**
   * Logs in.
   *
   * @param request credentials
   * @return token and profile
   */
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.username(), request.password());
  }

  /**
   * Logs out: revokes the caller's token on every instance and audits the logout.
   *
   * @param authorization the {@code Authorization: Bearer} header of the caller
   */
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    authService.logout(
        authorization.startsWith(BEARER)
            ? authorization.substring(BEARER.length())
            : authorization);
  }

  /**
   * Returns the current user's profile.
   *
   * @return profile
   */
  @GetMapping("/me")
  public UserProfileResponse me() {
    return UserProfileResponse.from(users.getByUsername(currentUser.username()));
  }

  /**
   * Changes the current user's password.
   *
   * @param request passwords
   */
  @PostMapping("/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
    users.changeOwnPassword(request.currentPassword(), request.newPassword());
  }
}
