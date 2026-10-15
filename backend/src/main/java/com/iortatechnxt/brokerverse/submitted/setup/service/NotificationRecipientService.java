package com.iortatechnxt.brokerverse.submitted.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationEventRepository;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNotificationRecipient;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNotificationRecipient.Row;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNotificationRecipientRepository;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification Recipients of Submitted Policies Setup (FR-SP-082 R3): who is told of each lifecycle
 * event type, per segment, kept with maker and checker; approval and deactivation go through {@link
 * SetupService} (kind RECIPIENT).
 */
@Service
@Transactional
public class NotificationRecipientService {

  /**
   * The lifecycle events of a record that are sent to the recipients set up (through {@link
   * com.iortatechnxt.brokerverse.submitted.service.SbmNotifier}).
   */
  public static final Set<String> EVENTS =
      Set.of(
          "SBM_EXPIRY_NEAR",
          "SBM_RENEWAL_STARTED",
          "SBM_PLACEMENT_READY",
          "SBM_PLACEMENT_SENT",
          "SBM_HOLD_COVER_UNBOOKED");

  private static final String ENTITY = "SubmittedSetup";
  private static final Set<String> NAMED =
      Set.of(
          SbmNotificationRecipient.ROLE,
          SbmNotificationRecipient.USER,
          SbmNotificationRecipient.EMAIL);
  private static final Set<String> TYPES =
      Set.of(
          SbmNotificationRecipient.ROLE,
          SbmNotificationRecipient.USER,
          SbmNotificationRecipient.HANDLER,
          SbmNotificationRecipient.AO,
          SbmNotificationRecipient.EMAIL,
          SbmNotificationRecipient.OFF);
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final SbmNotificationRecipientRepository recipients;
  private final NotificationEventRepository events;
  private final RoleRepository roles;
  private final AppUserRepository users;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param recipients recipients
   * @param events notification events
   * @param roles roles
   * @param users users
   * @param lovs lists of values (segments)
   * @param audit audit trail
   * @param clock clock
   */
  public NotificationRecipientService(
      SbmNotificationRecipientRepository recipients,
      NotificationEventRepository events,
      RoleRepository roles,
      AppUserRepository users,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.recipients = recipients;
    this.events = events;
    this.roles = roles;
    this.users = users;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Recipients of a company.
   *
   * @param companyId company
   * @return recipients by event
   */
  @Transactional(readOnly = true)
  public List<SbmNotificationRecipient> list(Long companyId) {
    return recipients.findByCompanyIdOrderByEventCodeAscIdAsc(companyId);
  }

  /**
   * Saves a recipient, pending approval.
   *
   * @param companyId company
   * @param id recipient, null for a new one
   * @param row content
   * @return recipient
   */
  public SbmNotificationRecipient save(Long companyId, Long id, Row row) {
    Row clean = validated(row);
    SbmNotificationRecipient r;
    if (id == null) {
      r = recipients.save(new SbmNotificationRecipient(companyId, clean));
    } else {
      r =
          recipients
              .findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Setup record", id));
      r.change(clean);
    }
    audit.record(
        ENTITY,
        "recipient " + r.getId(),
        AuditAction.UPDATE,
        "Notification recipient of "
            + clean.eventCode()
            + ": "
            + clean.recipientType()
            + (clean.recipient() == null ? "" : " " + clean.recipient()));
    return r;
  }

  private Row validated(Row row) {
    String event = event(row.eventCode());
    String type = blankToNull(row.recipientType());
    if (type == null || !TYPES.contains(type)) {
      throw new BusinessRuleException(
          "SBM_RECIPIENT_TYPE",
          "Select a role, a named user, the assigned handler, the AO, an e-mail address or Off");
    }
    String segment = blankToNull(row.segment());
    if (segment != null) {
      lovs.requireValid(SubmittedCodes.LOV_SEGMENT, segment, BusinessClock.today(clock));
    }
    String recipient = NAMED.contains(type) ? blankToNull(row.recipient()) : null;
    if (NAMED.contains(type)) {
      checkRecipient(type, recipient);
    }
    return new Row(event, segment, type, recipient, blankToNull(row.description()));
  }

  private String event(String code) {
    String event = blankToNull(code);
    if (event == null || !EVENTS.contains(event) || !events.existsById(event)) {
      throw new BusinessRuleException(
          "SBM_RECIPIENT_EVENT", "Select an event of the submitted policies");
    }
    return event;
  }

  private void checkRecipient(String type, String recipient) {
    if (recipient == null) {
      throw new BusinessRuleException(
          "SBM_RECIPIENT_REQUIRED", "Enter the role, the user or the e-mail address");
    }
    boolean valid =
        switch (type) {
          case SbmNotificationRecipient.ROLE ->
              roles.findByCode(recipient).filter(Role::isActive).isPresent();
          case SbmNotificationRecipient.USER ->
              users.findByUsernameIgnoreCase(recipient).isPresent();
          default -> EMAIL.matcher(recipient).matches();
        };
    if (!valid) {
      throw new BusinessRuleException(
          "SBM_RECIPIENT_UNKNOWN",
          switch (type) {
            case SbmNotificationRecipient.ROLE -> "No active role " + recipient;
            case SbmNotificationRecipient.USER -> "No user " + recipient;
            default -> recipient + " is not an e-mail address";
          });
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
