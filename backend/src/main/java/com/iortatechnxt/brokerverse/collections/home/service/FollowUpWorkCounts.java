package com.iortatechnxt.brokerverse.collections.home.service;

import com.iortatechnxt.brokerverse.collections.common.service.CollectionsWorkCountSource;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The follow-up tiles of the Collections home for the Collection Handlers and the Team Leads
 * (FR-CL-085; minutes of 27-Apr-2026, CLR-CL-35): broken promises, promises due, overdue
 * installments and open escalations - of the handler's own accounts, or of every account for a Team
 * Lead (who assigns or handles escalations).
 */
@Component
@Transactional(readOnly = true)
public class FollowUpWorkCounts implements CollectionsWorkCountSource {

  private static final int ORDER = 15;
  private static final int ORDER_DUE = 16;
  private static final int ORDER_INSTALLMENTS = 17;
  private static final int ORDER_ESCALATIONS = 18;
  private static final String MINE =
      " and exists (select 1 from CollectionItem c where c.invoiceNo = %s.invoiceNo"
          + " and c.status = com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums"
          + ".ItemStatus.OPEN and lower(c.currentHandler) = lower(:user))";
  private static final String PROMISES =
      "select count(p) from PaymentPromise p where p.companyId = :companyId and p.status ="
          + " com.iortatechnxt.brokerverse.collections.promise.domain.PromiseStatus.";
  private static final String OVERDUE =
      "select count(i) from Installment i join i.plan p where p.companyId = :companyId"
          + " and p.status = com.iortatechnxt.brokerverse.collections.installment.domain"
          + ".PlanEnums.PlanStatus.ACTIVE and i.status = com.iortatechnxt.brokerverse.collections"
          + ".installment.domain.PlanEnums.InstallmentStatus.OVERDUE";
  private static final String ESCALATIONS =
      "select count(e) from Escalation e where e.companyId = :companyId and e.status <>"
          + " com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage"
          + ".RESOLVED";
  private static final String COMPANY = "companyId";
  private static final String USER = "user";

  private final EntityManager em;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the source.
   *
   * @param em entity manager
   * @param currentUser signed-in user
   * @param clock clock
   */
  public FollowUpWorkCounts(EntityManager em, CurrentUser currentUser, Clock clock) {
    this.em = em;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  public List<WorkCount> counts(Long companyId, String username) {
    boolean lead =
        currentUser.hasAuthority("CLX_ASSIGN") || currentUser.hasAuthority("CLX_ESCALATION_HANDLE");
    if (!lead && !currentUser.hasAuthority("CLX_WORK")) {
      return List.of();
    }
    String scope = lead ? null : username;
    List<WorkCount> tiles = new ArrayList<>();
    tiles.add(
        new WorkCount(
            "brokenPromises",
            "Broken Promises",
            count(PROMISES + "BROKEN", "p", companyId, scope, null),
            "/collections/promises?tab=BROKEN",
            true,
            ORDER));
    tiles.add(
        new WorkCount(
            "promisesDue",
            "Promises Due",
            count(
                PROMISES + "OPEN and p.promisedDate <= :today",
                "p",
                companyId,
                scope,
                BusinessClock.today(clock)),
            "/collections/promises?tab=OPEN",
            false,
            ORDER_DUE));
    tiles.add(
        new WorkCount(
            "overdueInstallments",
            "Overdue Installments",
            count(OVERDUE, "i", companyId, scope, null),
            "/collections/installments-due",
            true,
            ORDER_INSTALLMENTS));
    tiles.add(
        new WorkCount(
            "openEscalations",
            "Open Escalations",
            escalations(companyId, scope),
            "/collections/escalations",
            false,
            ORDER_ESCALATIONS));
    return tiles;
  }

  private long count(String jpql, String alias, Long companyId, String user, LocalDate today) {
    String text = user == null ? jpql : jpql + String.format(MINE, alias);
    TypedQuery<Long> q = em.createQuery(text, Long.class).setParameter(COMPANY, companyId);
    if (user != null) {
      q.setParameter(USER, user);
    }
    if (today != null) {
      q.setParameter("today", today);
    }
    return q.getSingleResult();
  }

  private long escalations(Long companyId, String user) {
    String text =
        user == null
            ? ESCALATIONS
            : ESCALATIONS
                + " and (lower(e.targetUsername) = lower(:user) or lower(e.createdBy) = lower(:user))";
    TypedQuery<Long> q = em.createQuery(text, Long.class).setParameter(COMPANY, companyId);
    if (user != null) {
      q.setParameter(USER, user);
    }
    return q.getSingleResult();
  }
}
