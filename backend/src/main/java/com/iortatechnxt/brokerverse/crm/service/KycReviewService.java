package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodic KYC review (BRNB.110): lists clients whose KYC review is due or overdue, and once a
 * month marks overdue KYC as expired and tells the Account Officers how many reviews are due.
 */
@Service
@Transactional
public class KycReviewService {

  /** Frontend route of the KYC reviews due screen. */
  public static final String SCREEN = "/crm/kyc-reviews";

  private static final String KYC_STATUS = "kycStatus";
  private static final String REVIEW_DUE = "kycReviewDue";

  private final ClientRepository clients;
  private final KycReviewPolicy policy;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param policy review cycle
   * @param notifications in-app notifications
   * @param audit audit trail
   * @param clock clock
   */
  public KycReviewService(
      ClientRepository clients,
      KycReviewPolicy policy,
      NotificationService notifications,
      AuditTrailService audit,
      Clock clock) {
    this.clients = clients;
    this.policy = policy;
    this.notifications = notifications;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Clients whose KYC review is overdue (expired) or due by the date, earliest first.
   *
   * @param query filter
   * @param pageable page
   * @return clients
   */
  @Transactional(readOnly = true)
  public Page<Client> due(KycDueQuery query, Pageable pageable) {
    return clients.findAll(specification(query), pageable);
  }

  /**
   * Number of clients whose KYC review is overdue or due.
   *
   * @param query filter
   * @return count
   */
  @Transactional(readOnly = true)
  public long countDue(KycDueQuery query) {
    return clients.count(specification(query));
  }

  /**
   * Monthly run: expires every verified KYC whose review date has passed, then notifies the holders
   * of CLIENT_MAINTAIN of the number of non-bank clients due (one notification per user).
   *
   * @param businessDate business date
   * @return outcome (expired and due counts)
   */
  public KycReviewRun runReview(LocalDate businessDate) {
    int expired = 0;
    for (Client c :
        clients.findByKycStatusAndKycReviewDueBefore(KycStatus.VERIFIED, businessDate)) {
      c.setKycStatus(KycStatus.EXPIRED);
      audit.record(
          ClientService.ENTITY,
          c.getProspectCode(),
          AuditAction.UPDATE,
          "KYC expired: periodic review was due on " + c.getKycReviewDue());
      expired++;
    }
    long due = countDue(new KycDueQuery(null, policy.dueHorizon(businessDate), false, null, null));
    int notified = 0;
    if (due > 0) {
      notified =
          notifications.notifyPermission(
              "CLIENT_MAINTAIN",
              new Notice(
                  "KYC reviews due: " + due + " client(s)",
                  "Non-bank clients whose periodic KYC review is overdue or due by "
                      + policy.dueHorizon(businessDate)
                      + " (BRNB.110).",
                  SCREEN,
                  null,
                  null));
    }
    return new KycReviewRun(expired, due, notified);
  }

  private Specification<Client> specification(KycDueQuery q) {
    LocalDate dueBy = q.dueBy() != null ? q.dueBy() : policy.dueHorizon(LocalDate.now(clock));
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      if (q.companyId() != null) {
        p.add(cb.equal(root.get("companyId"), q.companyId()));
      }
      p.add(cb.notEqual(root.get("status"), ClientStatus.INACTIVE));
      p.add(
          cb.or(
              cb.equal(root.get(KYC_STATUS), KycStatus.EXPIRED),
              cb.and(
                  cb.equal(root.get(KYC_STATUS), KycStatus.VERIFIED),
                  cb.lessThanOrEqualTo(root.get(REVIEW_DUE), dueBy))));
      if (q.bankClient() != null) {
        p.add(cb.equal(root.get("bankClient"), q.bankClient()));
      }
      if (q.riskRating() != null && !q.riskRating().isBlank()) {
        p.add(cb.equal(root.get("riskRating"), q.riskRating()));
      }
      if (q.marketSegment() != null && !q.marketSegment().isBlank()) {
        p.add(cb.equal(root.get("marketSegment"), q.marketSegment()));
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  /**
   * Outcome of the monthly run.
   *
   * @param expired KYC set to EXPIRED
   * @param due non-bank clients due or overdue
   * @param notified users notified
   */
  public record KycReviewRun(int expired, long due, int notified) {}
}
