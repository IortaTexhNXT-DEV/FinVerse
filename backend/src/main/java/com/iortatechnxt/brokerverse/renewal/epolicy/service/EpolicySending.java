package com.iortatechnxt.brokerverse.renewal.epolicy.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelGateways;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E-policy sending through CCM (FRRN.033.04): the accounts For E-Policy Sending with an attached
 * e-policy, a policy number and a valid client e-mail address can be selected (one, several or
 * all); each account is validated and sent independently to the client with the copy recipients,
 * and the sending ends with a summary of the accounts selected, submitted, failed and pending.
 */
@Service
public class EpolicySending {

  /** Kind of channel document. */
  public static final String KIND = "EPOLICY";

  private static final String PENDING = "Pending submission";
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");

  private final NamedParameterJdbcTemplate jdbc;
  private final RenewalRecords records;
  private final DocumentService storage;
  private final ChannelService channels;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param jdbc accounts For E-Policy Sending
   * @param records renewal lookup
   * @param storage e-policy documents
   * @param channels CCM
   * @param audit audit trail
   */
  public EpolicySending(
      NamedParameterJdbcTemplate jdbc,
      RenewalRecords records,
      DocumentService storage,
      ChannelService channels,
      AuditTrailService audit) {
    this.jdbc = jdbc;
    this.records = records;
    this.storage = storage;
    this.channels = channels;
    this.audit = audit;
  }

  /**
   * The accounts For E-Policy Sending with whether each can be sent.
   *
   * @param companyId company
   * @return accounts
   */
  @Transactional(readOnly = true)
  public List<Account> accounts(Long companyId) {
    return jdbc
        .queryForList(
            "select id from rnw_candidate where company_id = :companyId"
                + " and placement_status = 'FOR_EPOLICY_SENDING' order by renewal_ref",
            Map.of("companyId", companyId),
            Long.class)
        .stream()
        .map(records::byId)
        .map(this::account)
        .toList();
  }

  /**
   * Sends the e-policies of accounts.
   *
   * @param companyId company
   * @param refs accounts
   * @param cc copy recipients
   * @return summary
   */
  @Transactional
  public Summary send(Long companyId, List<String> refs, List<String> cc) {
    List<String> copies = cc == null ? List.of() : cc;
    for (String address : copies) {
      if (!EMAIL.matcher(address.strip()).matches()) {
        throw new BusinessRuleException(
            "RNW_EPOLICY_EMAIL", "Invalid email address format: " + address);
      }
    }
    Map<String, String> outcome = new LinkedHashMap<>();
    int submitted = 0;
    int pending = 0;
    for (String ref : refs) {
      String result = sendOne(companyId, ref, copies);
      if (result == null) {
        submitted++;
      } else {
        outcome.put(ref, result);
        pending += PENDING.equals(result) ? 1 : 0;
      }
    }
    return new Summary(refs.size(), submitted, refs.size() - submitted - pending, pending, outcome);
  }

  private String sendOne(Long companyId, String ref, List<String> copies) {
    RenewalCandidate c = records.get(companyId, ref);
    Account a = account(c);
    if (a.problem() != null) {
      return a.problem();
    }
    ChannelService.Sent sent = send(c, a, copies);
    if (sent.message().getStatus() == ChannelStatus.PENDING_TRANSMISSION) {
      return PENDING;
    }
    if (sent.error() != null) {
      return sent.error();
    }
    c.getPlacement().status(CandidatePlacement.EPOLICY_SENT);
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.UPDATE, "E-policy sent through CCM");
    return null;
  }

  private ChannelService.Sent send(RenewalCandidate c, Account a, List<String> cc) {
    return channels.send(
        c.getCompanyId(),
        new ChannelService.Outbound(
            ChannelGateways.CCM,
            new ChannelMessage.Document(
                KIND, a.policyNo(), c.getId(), c.getRenewalRef(), a.fileName(), a.attachmentId()),
            new ChannelMessage.Address(
                a.email(),
                cc.isEmpty() ? null : String.join(",", cc),
                "Your e-policy " + a.policyNo(),
                "Dear "
                    + c.getSnapshot().clientName()
                    + ", please find attached your e-policy "
                    + a.policyNo()
                    + ".",
                c.getSnapshot().client() == null ? null : c.getSnapshot().client().clientId())));
  }

  private Account account(RenewalCandidate c) {
    Optional<Attachment> doc =
        storage.list(new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString())).stream()
            .filter(x -> EpolicyReceipts.DOC_TYPE.equals(x.getDocumentType()))
            .max(Comparator.comparing(Attachment::getId));
    String email = c.getSnapshot().client() == null ? null : c.getSnapshot().client().email();
    String policyNo = c.getPlacement().getEpolicyNo();
    List<String> problems = new ArrayList<>();
    if (doc.isEmpty()) {
      problems.add("No e-policy document attached");
    }
    if (policyNo == null || policyNo.isBlank()) {
      problems.add("No valid policy number");
    }
    if (email == null || !EMAIL.matcher(email.strip()).matches()) {
      problems.add("No valid client email address");
    }
    return new Account(
        c.getRenewalRef(),
        c.getSnapshot().clientName(),
        policyNo,
        email,
        doc.map(Attachment::getFileName).orElse(null),
        doc.map(Attachment::getId).orElse(null),
        problems.isEmpty() ? null : String.join("; ", problems));
  }

  /**
   * An account For E-Policy Sending.
   *
   * @param renewalRef reference
   * @param clientName client
   * @param policyNo policy number
   * @param email client e-mail
   * @param fileName e-policy document
   * @param attachmentId stored e-policy
   * @param problem why it cannot be sent, null when eligible
   */
  public record Account(
      String renewalRef,
      String clientName,
      String policyNo,
      String email,
      String fileName,
      Long attachmentId,
      String problem) {}

  /**
   * The outcome of a sending.
   *
   * @param selected accounts selected
   * @param submitted successfully submitted
   * @param failed failed submission
   * @param pending pending submission
   * @param messages reason per account not submitted
   */
  public record Summary(
      int selected, int submitted, int failed, int pending, Map<String, String> messages) {

    /** Defensive copy. */
    public Summary {
      messages = Map.copyOf(messages);
    }
  }
}
