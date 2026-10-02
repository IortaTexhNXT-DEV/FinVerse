package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.issuance.domain.DocumentTrigger;
import com.iortatechnxt.brokerverse.issuance.domain.DocumentTriggerRepository;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.TriggerAction;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document trigger rules (BRNB.105): an uploaded document type starts the business action of its
 * active rules. The seeded rule turns an EPOLICY upload into an extraction review task: the policy
 * data is extracted and the holders of EPOLICY_MANAGE are notified. The automatic booking that
 * follows belongs to the booking module (it listens for the POLICY_ISSUED stage).
 */
@Service
@Transactional
public class DocumentTriggerService {

  private final DocumentTriggerRepository triggers;
  private final PolicyDataExtractor extractor;
  private final NotificationService notifications;

  /**
   * Creates the service.
   *
   * @param triggers trigger rules
   * @param extractor policy data extraction
   * @param notifications in-app notifications
   */
  public DocumentTriggerService(
      DocumentTriggerRepository triggers,
      PolicyDataExtractor extractor,
      NotificationService notifications) {
    this.triggers = triggers;
    this.extractor = extractor;
    this.notifications = notifications;
  }

  /**
   * The rules, by document type.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<DocumentTrigger> rules() {
    return triggers.findAllByOrderByDocumentTypeAscIdAsc();
  }

  /**
   * Runs the active rules of an uploaded e-policy.
   *
   * @param documentType document type (list DOCUMENT_TYPE)
   * @param epolicy the received e-policy
   * @param insurerCode insurer of the account
   * @param content file bytes
   * @return actions run
   */
  public List<TriggerAction> onEpolicy(
      String documentType, Epolicy epolicy, String insurerCode, byte[] content) {
    List<TriggerAction> done = new ArrayList<>();
    for (DocumentTrigger rule : triggers.findByDocumentTypeAndActiveTrue(documentType)) {
      if (rule.getAction() == TriggerAction.EXTRACTION_REVIEW) {
        extract(epolicy, insurerCode, content);
        notifications.notifyPermission(
            "EPOLICY_MANAGE",
            new Notice(
                "E-policy to review: " + epolicy.getArn(),
                epolicy.getFileName() + " was received; check the extracted policy data",
                "/issuance/epolicies/" + epolicy.getId(),
                AccountService.ENTITY,
                String.valueOf(epolicy.getAccountId())));
        done.add(rule.getAction());
      }
    }
    return done;
  }

  /**
   * Extracts the policy data of an e-policy and opens its review.
   *
   * @param epolicy e-policy
   * @param insurerCode insurer of the account
   * @param content file bytes
   * @return the values found
   */
  public ExtractedPolicy extract(Epolicy epolicy, String insurerCode, byte[] content) {
    ExtractedPolicy found = extractor.extract(content, insurerCode);
    epolicy.extracted(
        found.policyNumbers(), found.periodFrom(), found.periodTo(), found.premium(), found.note());
    return found;
  }
}
