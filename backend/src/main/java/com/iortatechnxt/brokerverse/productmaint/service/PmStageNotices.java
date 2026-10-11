package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Notices of the Product Maintenance stages (BDOI FRS FRPM.029.01): every TSU Team Lead is told, in
 * the system and by e-mail as preferred, when a package request or a quotation request of a
 * non-package product enters TSU review.
 */
@Component
public class PmStageNotices {

  /** Notification event of a request entering TSU review. */
  public static final String TSU_REVIEW_EVENT = "PM_TSU_REVIEW";

  /** Permission of the TSU Team Leads. */
  public static final String TSU_TEAM_LEAD = "PKG_TSU_RECOMMEND";

  private static final String QUOTATION = "ProposalRequest";

  private final NoticeDelivery delivery;
  private final WorkCaseRepository cases;

  /**
   * Creates the notices.
   *
   * @param delivery in-app and e-mail delivery
   * @param cases work cases (reference, title and link)
   */
  public PmStageNotices(NoticeDelivery delivery, WorkCaseRepository cases) {
    this.delivery = delivery;
    this.cases = cases;
  }

  /**
   * Tells the TSU Team Leads of a request entering TSU review.
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    boolean packageReview =
        PackageRequests.ENTITY.equals(event.entityType())
            && "FOR_TSU_REVIEW".equals(event.toStage());
    boolean quotationReview =
        QUOTATION.equals(event.entityType()) && "WITH_TSU".equals(event.toStage());
    if (!packageReview && !quotationReview) {
      return;
    }
    cases
        .findById(event.caseId())
        .ifPresent(
            c ->
                delivery.toPermission(
                    TSU_TEAM_LEAD,
                    new Notice(
                        c.getReference() + " is for TSU review",
                        c.getTitle(),
                        c.getLink(),
                        c.getEntityType(),
                        c.getEntityId()),
                    TSU_REVIEW_EVENT,
                    true));
  }
}
