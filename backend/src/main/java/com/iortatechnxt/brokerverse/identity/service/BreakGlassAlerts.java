package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.security.service.BreakGlassSignedIn;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.domain.ParameterValueType;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Alerts Information Security of every sign-in of a break-glass System Administrator while single
 * sign-on is on (BDOI FRS FRUM.001.06): an alert {@value #CODE}, a notice to the holders of the
 * security-setting approval (in the system and by e-mail as each prefers) and an e-mail to the
 * addresses of the setting {@value #RECIPIENTS}.
 */
@Component
public class BreakGlassAlerts {

  /** Alert code and notification event. */
  public static final String CODE = "UAM_BREAK_GLASS_SIGN_IN";

  /** Setting: e-mail addresses of Information Security. */
  public static final String RECIPIENTS = "BREAK_GLASS_ALERT_RECIPIENTS";

  private static final String INFORMATION_SECURITY = "SECURITY_PARAMETER_APPROVE";

  private final NoticeDelivery notices;
  private final AlertService alerts;
  private final SystemParameterService parameters;
  private final UserDirectory users;

  /**
   * Creates the listener.
   *
   * @param notices notices
   * @param alerts alert service
   * @param parameters business parameters (the recipients)
   * @param users display names
   */
  public BreakGlassAlerts(
      NoticeDelivery notices,
      AlertService alerts,
      SystemParameterService parameters,
      UserDirectory users) {
    this.notices = notices;
    this.alerts = alerts;
    this.parameters = parameters;
    this.users = users;
  }

  /**
   * Alerts Information Security.
   *
   * @param event the sign-in
   */
  @EventListener
  public void on(BreakGlassSignedIn event) {
    String text =
        "Break-glass administrator "
            + users.displayName(event.username())
            + " ("
            + event.username()
            + ") signed in with a local password at "
            + DisplayFormat.dateTime(event.at())
            + (event.address() == null ? "" : " from " + event.address())
            + ". Review the use within one working day.";
    Notice notice =
        new Notice("Break-glass sign-in", text, "/admin/audit", "AppUser", event.username());
    notices.toPermission(INFORMATION_SECURITY, notice, CODE, true);
    notices.toAddresses(ParameterValueType.items(parameters.text(RECIPIENTS, "")), notice, CODE);
    alerts.raise(
        CODE,
        new AlertFacts(
            null, null, "AppUser", event.username(), text, null, CODE + ":" + event.at()));
  }
}
