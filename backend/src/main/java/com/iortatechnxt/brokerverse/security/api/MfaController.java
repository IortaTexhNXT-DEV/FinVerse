package com.iortatechnxt.brokerverse.security.api;

import com.iortatechnxt.brokerverse.security.api.dto.LoginResponse;
import com.iortatechnxt.brokerverse.security.api.dto.MfaCodeRequest;
import com.iortatechnxt.brokerverse.security.api.dto.MfaEnrolmentResponse;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordPolicy;
import com.iortatechnxt.brokerverse.security.service.AuthProfileService;
import com.iortatechnxt.brokerverse.security.service.SecondFactorSignIn;
import com.iortatechnxt.brokerverse.security.service.SignInResult;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaPolicy;
import com.iortatechnxt.brokerverse.security.service.mfa.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The second factor (TOTP authenticator app): the sign-in step after the password ({@code verify},
 * {@code enrolment/*} with the challenge of the first step, anonymous and rate limited like the
 * login) and the user's own authenticator app on My Profile ({@code me/**}).
 */
@RestController
@RequestMapping("/api/v1/auth/mfa")
public class MfaController {

  private static final String SIGNED_IN = "isAuthenticated()";
  private static final String ANYONE = "permitAll()";

  private final SecondFactorSignIn signIn;
  private final MfaService mfa;
  private final MfaPolicy policy;
  private final AuthProfileService profiles;
  private final AuthPasswordPolicy passwordPolicy;
  private final RefreshCookies cookies;

  /**
   * Creates the controller.
   *
   * @param signIn second step of the sign-in
   * @param mfa second factor
   * @param policy who needs the second factor
   * @param profiles the signed-in user
   * @param passwordPolicy sign-in mode
   * @param cookies refresh token cookie
   */
  public MfaController(
      SecondFactorSignIn signIn,
      MfaService mfa,
      MfaPolicy policy,
      AuthProfileService profiles,
      AuthPasswordPolicy passwordPolicy,
      RefreshCookies cookies) {
    this.signIn = signIn;
    this.mfa = mfa;
    this.policy = policy;
    this.profiles = profiles;
    this.passwordPolicy = passwordPolicy;
    this.cookies = cookies;
  }

  /**
   * Checks the code of a sign-in and opens the session.
   *
   * @param request challenge, code and whether to remember the device
   * @param http request
   * @param response response (the refresh token cookie)
   * @return the session
   */
  @PostMapping("/verify")
  @PreAuthorize(ANYONE)
  public LoginResponse verify(
      @Valid @RequestBody MfaCodeRequest request,
      HttpServletRequest http,
      HttpServletResponse response) {
    SignInResult result =
        signIn.verify(request.challenge(), request.code(), request.rememberDevice());
    cookies.write(result, http, response);
    return result.response();
  }

  /**
   * Starts the enrolment of an authenticator app during a sign-in.
   *
   * @param request the challenge of the first step
   * @return the secret as a QR code and as text
   */
  @PostMapping("/enrolment/start")
  @PreAuthorize(ANYONE)
  public MfaService.Enrolment startAtSignIn(@Valid @RequestBody MfaCodeRequest request) {
    return signIn.startEnrolment(request.challenge());
  }

  /**
   * Confirms the enrolment during a sign-in with the first code of the app and opens the session.
   *
   * @param request the challenge and the code
   * @param http request
   * @param response response (the refresh token cookie)
   * @return the recovery codes (shown once) and the session
   */
  @PostMapping("/enrolment/confirm")
  @PreAuthorize(ANYONE)
  public MfaEnrolmentResponse confirmAtSignIn(
      @Valid @RequestBody MfaCodeRequest request,
      HttpServletRequest http,
      HttpServletResponse response) {
    SecondFactorSignIn.Enrolled enrolled =
        signIn.confirmEnrolment(request.challenge(), request.code());
    cookies.write(enrolled.signIn(), http, response);
    return new MfaEnrolmentResponse(enrolled.recoveryCodes(), enrolled.signIn().response());
  }

  /**
   * The signed-in user's second factor.
   *
   * @return status
   */
  @GetMapping("/me")
  @PreAuthorize(SIGNED_IN)
  public MyStatus me() {
    AppUser user = profiles.me();
    MfaService.Status status = mfa.status(user.getUsername());
    return new MyStatus(
        status.enrolled(),
        status.enrolledAt(),
        status.lastUsedAt(),
        status.recoveryCodesLeft(),
        policy.available() && policy.required(user, passwordPolicy.mode(), false),
        policy.available(),
        policy.rememberDays());
  }

  /**
   * Starts the enrolment (or replacement) of the signed-in user's authenticator app.
   *
   * @return the secret as a QR code and as text
   */
  @PostMapping("/me/enrolment/start")
  @PreAuthorize(SIGNED_IN)
  public MfaService.Enrolment startMine() {
    return mfa.startEnrolment(profiles.me().getUsername());
  }

  /**
   * Confirms the enrolment of the signed-in user's authenticator app.
   *
   * @param request the first code of the app
   * @return the recovery codes, shown once
   */
  @PostMapping("/me/enrolment/confirm")
  @PreAuthorize(SIGNED_IN)
  public MfaEnrolmentResponse confirmMine(@Valid @RequestBody MfaCodeRequest request) {
    return new MfaEnrolmentResponse(
        mfa.confirmEnrolment(profiles.me().getUsername(), request.code()), null);
  }

  /**
   * Replaces the recovery codes of the signed-in user after a code of the app.
   *
   * @param request current code of the app
   * @return the new recovery codes, shown once
   */
  @PostMapping("/me/recovery-codes")
  @PreAuthorize(SIGNED_IN)
  public List<String> newRecoveryCodes(@Valid @RequestBody MfaCodeRequest request) {
    return mfa.regenerateRecoveryCodes(profiles.me().getUsername(), request.code());
  }

  /**
   * The second factor of the signed-in user.
   *
   * @param enrolled whether an authenticator app is active
   * @param enrolledAt when it was enrolled
   * @param lastUsedAt when a code was last accepted
   * @param recoveryCodesLeft unused recovery codes
   * @param required whether the policy requires the second factor for this user
   * @param available whether the second factor is set up on this system
   * @param rememberDeviceDays days a device may be remembered (0 = never)
   */
  public record MyStatus(
      boolean enrolled,
      Instant enrolledAt,
      Instant lastUsedAt,
      long recoveryCodesLeft,
      boolean required,
      boolean available,
      int rememberDeviceDays) {}
}
