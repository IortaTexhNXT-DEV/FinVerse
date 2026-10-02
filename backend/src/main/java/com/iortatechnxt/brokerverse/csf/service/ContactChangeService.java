package com.iortatechnxt.brokerverse.csf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ContactChange;
import com.iortatechnxt.brokerverse.crm.domain.ContactChange.FieldChange;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.csf.domain.ChangedField;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChange;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChangeRepository;
import com.iortatechnxt.brokerverse.csf.domain.CsfVerification;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ChangeView;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contact changes from the Customer Servicing Facility (FR-CSF-021, 022; BRCSF-002, 004, 010): a
 * passed verification younger than {@code CSF_VERIFICATION_VALID_MINUTES}, then the contact-only
 * update of the client master (e-mail, mobile, phone, address) with a reason; the change is kept
 * with its fields before and after and queued for the legacy systems. Any other field is refused
 * and the refusal kept; such changes are referred to the fulfilment unit as an Operations hand-off.
 */
@Service
@Transactional
public class ContactChangeService {

  private static final Map<String, String> CONTACT_LABELS =
      Map.of(
          "EMAIL", "E-mail",
          "MOBILE", "Mobile",
          "PHONE", "Phone",
          "ADDRESS_LINE", "Address line",
          "CITY", "City",
          "PROVINCE", "Province",
          "POSTAL_CODE", "Postal code");

  private final CsfContactChangeRepository changes;
  private final CsfClients clients;
  private final ClientService clientMaster;
  private final VerificationService verifications;
  private final RefusalRecorder refusals;
  private final LegacySyncService sync;
  private final HandoffService handoffs;
  private final ChangeViews views;
  private final CsfSupport support;

  /**
   * Creates the service.
   *
   * @param changes contact changes
   * @param clients clients of the company
   * @param clientMaster client master (contact-only contract)
   * @param verifications caller verifications
   * @param refusals refused changes
   * @param sync legacy write-back
   * @param handoffs Operations hand-offs (fulfilment unit)
   * @param views change views
   * @param support numbering, lists, audit, activity, user, clock and JSON
   */
  @SuppressWarnings("java:S107") // collaborators
  public ContactChangeService(
      CsfContactChangeRepository changes,
      CsfClients clients,
      ClientService clientMaster,
      VerificationService verifications,
      RefusalRecorder refusals,
      LegacySyncService sync,
      HandoffService handoffs,
      ChangeViews views,
      CsfSupport support) {
    this.changes = changes;
    this.clients = clients;
    this.clientMaster = clientMaster;
    this.verifications = verifications;
    this.refusals = refusals;
    this.sync = sync;
    this.handoffs = handoffs;
    this.views = views;
    this.support = support;
  }

  /**
   * Applies a contact change after a passed verification.
   *
   * @param companyId company
   * @param clientId client
   * @param request verification, reason, remarks and the new values by field
   * @return the change
   */
  public ChangeView apply(Long companyId, Long clientId, ChangeRequest request) {
    Client client = clients.require(companyId, clientId);
    List<String> other =
        request.values().entrySet().stream()
            .filter(e -> !CONTACT_LABELS.containsKey(e.getKey()) && Objects.nonNull(e.getValue()))
            .map(Map.Entry::getKey)
            .toList();
    if (!other.isEmpty()) {
      refuse(client, request, other);
    }
    LocalDate today = BusinessClock.today(support.clock());
    if (request.reasonCode() == null || request.reasonCode().isBlank()) {
      throw new BusinessRuleException("CSF_REASON_REQUIRED", "Select the reason for the change");
    }
    support.lovs().requireValid(CsfCodes.LOV_CHANGE_REASON, request.reasonCode(), today);
    CsfVerification verification = verifications.requireValid(clientId, request.verificationId());
    String no = nextNumber();
    List<FieldChange> changed =
        clientMaster.updateContact(
            clientId, contactChange(request, no + " verification " + verification.getId()));
    if (changed.isEmpty()) {
      throw new BusinessRuleException("CSF_NO_CHANGE", "Change at least one contact detail");
    }
    CsfContactChange saved =
        changes.save(
            new CsfContactChange(
                header(no, client, ChangeStatus.APPLIED),
                new CsfContactChange.Request(
                    verification.getId(),
                    verification.getChannel(),
                    request.reasonCode(),
                    blankToNull(request.remarks())),
                changed.stream()
                    .map(c -> new ChangedField(c.field(), c.oldValue(), c.newValue()))
                    .toList()));
    sync.queue(saved, bankCifOf(client));
    support
        .audit()
        .record(
            CsfCodes.ENTITY_CHANGE,
            no,
            AuditAction.CREATE,
            "Contact change of " + client.getCode());
    support
        .activity()
        .record(
            companyId,
            ActivityAction.CONTACT_CHANGE,
            new CsfActivity.Subject(client.getId(), client.getCode(), no, "Applied"));
    return views.of(List.of(saved)).get(0);
  }

  private ContactChange contactChange(ChangeRequest request, String reference) {
    Map<String, String> v = request.values();
    return new ContactChange(
        v.get("EMAIL"),
        v.get("MOBILE"),
        v.get("PHONE"),
        v.get("ADDRESS_LINE"),
        v.get("CITY"),
        v.get("PROVINCE"),
        v.get("POSTAL_CODE"),
        CsfCodes.SOURCE,
        support.lovs().label(CsfCodes.LOV_CHANGE_REASON, request.reasonCode()),
        reference);
  }

  private static String bankCifOf(Client client) {
    return client.isBankClient() ? client.getBankCif() : null;
  }

  private void refuse(Client client, ChangeRequest request, List<String> other) {
    List<ChangedField> asked =
        other.stream().map(f -> new ChangedField(f, null, request.values().get(f))).toList();
    refusals.record(
        client,
        new CsfContactChange.Request(
            request.verificationId(),
            null,
            blankToNull(request.reasonCode()),
            blankToNull(request.remarks())),
        asked);
    String names = String.join(", ", other.stream().map(f -> fieldLabel(f)).toList());
    throw new BusinessRuleException(
        "CSF_FIELD_NOT_UPDATABLE",
        names + " cannot be changed here. Refer the client to the fulfilment unit");
  }

  private String fieldLabel(String field) {
    String label = support.lovs().label(CsfCodes.LOV_REFERRAL_FIELD, field);
    return label == null || label.equals(field) ? humanize(field) : label;
  }

  private static String humanize(String code) {
    String text = code.replace('_', ' ').toLowerCase(Locale.ROOT);
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /**
   * Refers a change the contact centre cannot make (name, civil status, ID ...) to the fulfilment
   * unit (e-mail topic 3): the referral is kept as a change record and handed to the users who
   * maintain the client master as an Operations hand-off.
   *
   * @param companyId company
   * @param clientId client
   * @param request channel, fields with the values asked for, remarks
   * @return the referral
   */
  public ChangeView refer(Long companyId, Long clientId, ReferralRequest request) {
    Client client = clients.require(companyId, clientId);
    LocalDate today = BusinessClock.today(support.clock());
    support.lovs().requireValid(CsfCodes.LOV_CHANNEL, request.channel(), today);
    if (request.fields().isEmpty()) {
      throw new BusinessRuleException(
          "CSF_REFERRAL_FIELDS_REQUIRED", "Select the information the client wants to change");
    }
    request
        .fields()
        .keySet()
        .forEach(f -> support.lovs().requireValid(CsfCodes.LOV_REFERRAL_FIELD, f, today));
    String no = nextNumber();
    CsfContactChange saved =
        changes.save(
            new CsfContactChange(
                header(no, client, ChangeStatus.REFERRED),
                new CsfContactChange.Request(
                    null, request.channel(), null, blankToNull(request.remarks())),
                request.fields().entrySet().stream()
                    .map(e -> new ChangedField(e.getKey(), null, blankToNull(e.getValue())))
                    .toList()));
    String summary =
        CsfSupport.cut(
            "Change of "
                + String.join(
                    ", ", request.fields().keySet().stream().map(this::fieldLabel).toList())
                + " asked by client "
                + client.getCode()
                + " "
                + client.getDisplayName()
                + " through the contact centre ("
                + no
                + ")");
    OpsHandoff handoff =
        handoffs.record(
            companyId,
            CsfCodes.PORT_FULFILMENT,
            CsfCodes.FULFILMENT_TEAM,
            new OpsHandoff.Spec(
                CsfCodes.MODULE, no, client.getCode(), null, null, summary, payload(saved)));
    saved.handedOff(handoff.getId());
    support.audit().record(CsfCodes.ENTITY_CHANGE, no, AuditAction.SUBMIT, summary);
    support
        .activity()
        .record(
            companyId,
            ActivityAction.REFERRAL,
            new CsfActivity.Subject(
                client.getId(), client.getCode(), no, "Referred to the fulfilment unit"));
    return views.of(List.of(saved)).get(0);
  }

  private String payload(CsfContactChange change) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("changeNo", change.getChangeNo());
    body.put("clientCode", change.getClientCode());
    body.put("remarks", change.getRemarks());
    List<Map<String, String>> fields = new ArrayList<>();
    change
        .getFields()
        .forEach(
            f ->
                fields.add(
                    Map.of("field", f.getField(), "asked", String.valueOf(f.getNewValue()))));
    body.put("fields", fields);
    try {
      return support.json().writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * The contact changes, refusals and referrals of a client, newest first (Contact History).
   *
   * @param companyId company
   * @param clientId client
   * @return changes
   */
  @Transactional(readOnly = true)
  public List<ChangeView> history(Long companyId, Long clientId) {
    Client client = clients.require(companyId, clientId);
    return views.of(changes.findByClientIdOrderByAppliedAtDescIdDesc(client.getId()));
  }

  private String nextNumber() {
    return support
        .numbers()
        .next(CsfCodes.NUMBER_PREFIX + BusinessClock.currentYear(support.clock()).getValue());
  }

  private CsfContactChange.Header header(String no, Client client, ChangeStatus status) {
    return new CsfContactChange.Header(
        no,
        CsfClients.refOf(client),
        client.getDisplayName(),
        status,
        support.clock().instant(),
        support.currentUser().username());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A contact change asked by the caller.
   *
   * @param verificationId passed verification of the caller
   * @param reasonCode reason (list CSF_CHANGE_REASON)
   * @param remarks remarks, may be null
   * @param values new value by field (EMAIL, MOBILE, PHONE, ADDRESS_LINE, CITY, PROVINCE,
   *     POSTAL_CODE; a blank value clears the field, any other field is refused)
   */
  public record ChangeRequest(
      Long verificationId, String reasonCode, String remarks, Map<String, String> values) {

    /** Defensive copy. */
    public ChangeRequest {
      values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
  }

  /**
   * A change referred to the fulfilment unit.
   *
   * @param channel channel (list CSF_CHANNEL)
   * @param fields value asked for by field (list CSF_REFERRAL_FIELD)
   * @param remarks remarks, may be null
   */
  public record ReferralRequest(String channel, Map<String, String> fields, String remarks) {

    /** Defensive copy that keeps the order. */
    public ReferralRequest {
      fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
  }
}
