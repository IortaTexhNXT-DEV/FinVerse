package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationPreferenceService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordService.PasswordExpiry;
import com.iortatechnxt.brokerverse.security.service.PasswordResetRequested;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The password messages of LOCAL sign-in (UAM-NFR-36, 37; FR-UA-005): the "Forgot password?" link
 * e-mailed to the registered address, and the notice of a password that expires soon (in the app
 * and by e-mail, event {@code PASSWORD_EXPIRY_NOTICE}). {@code security} publishes the reset event
 * and lists the expiring passwords; the messages are sent here because {@code security} does not
 * depend on {@code messaging}.
 */
@Component
public class PasswordNoticeMailer {

  /** Notification event of the expiry notice. */
  public static final String EXPIRY_EVENT = "PASSWORD_EXPIRY_NOTICE";

  private static final String ENTITY = "AppUser";
  private static final String PROFILE = "/profile";
  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH);

  private final MessageService messages;
  private final NotificationService notifications;
  private final NotificationPreferenceService preferences;

  /**
   * Creates the mailer.
   *
   * @param messages e-mail outbox
   * @param notifications in-app notifications
   * @param preferences notification preferences (e-mail on or off per event)
   */
  public PasswordNoticeMailer(
      MessageService messages,
      NotificationService notifications,
      NotificationPreferenceService preferences) {
    this.messages = messages;
    this.notifications = notifications;
    this.preferences = preferences;
  }

  /**
   * E-mails a reset link, in the transaction that stored it (the outbox sends it after the commit).
   *
   * @param event the link
   */
  @EventListener
  public void onResetRequested(PasswordResetRequested event) {
    String body =
        "Dear "
            + event.fullName()
            + ",\n\nA new BrokerVerse password was requested for user "
            + event.username()
            + ". Open this link to set it:\n\n"
            + event.link()
            + "\n\nThe link works once and expires at "
            + WHEN.format(event.expiresAt().atZone(MANILA))
            + " (Philippine time). If you did not ask for it, ignore this e-mail;"
            + " your password stays as it is.";
    messages.queueEmail(
        new OutboundEmail(
            null,
            "PASSWORD_RESET",
            List.of(event.email()),
            List.of(),
            "BrokerVerse: reset your password",
            body,
            List.of(),
            null,
            new RecordLink(ENTITY, event.username(), "Password reset")));
  }

  /**
   * Tells a user that the password expires soon, in the app and by e-mail (unless switched off).
   *
   * @param expiry the password and its expiry
   * @return true when the user was told
   */
  public boolean notifyExpiry(PasswordExpiry expiry) {
    String when = WHEN.format(expiry.expiresAt().atZone(MANILA));
    String body =
        "Your BrokerVerse password expires on "
            + when
            + " (Philippine time). Change it on My Profile before then.";
    boolean told =
        notifications.notifyUser(
            expiry.username(),
            new Notice("Your password expires soon", body, PROFILE, ENTITY, expiry.username()),
            EXPIRY_EVENT);
    if (expiry.email() != null
        && !expiry.email().isBlank()
        && preferences.wantsEmail(expiry.username(), EXPIRY_EVENT)) {
      messages.queueEmail(
          new OutboundEmail(
              null,
              EXPIRY_EVENT,
              List.of(expiry.email()),
              List.of(),
              "BrokerVerse: your password expires soon",
              body,
              List.of(),
              null,
              new RecordLink(ENTITY, expiry.username(), "Password expiry")));
      told = true;
    }
    return told;
  }
}
