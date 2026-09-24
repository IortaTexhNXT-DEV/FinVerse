package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskIdentifiers;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Criteria API predicates of the account search (BRNB.050). */
final class AccountSpecifications {

  private AccountSpecifications() {}

  /**
   * The specification of a search.
   *
   * @param s criteria
   * @return specification
   */
  static Specification<Account> of(AccountSearch s) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), s.companyId()));
      if (!s.includeVoided()) {
        where.add(cb.notEqual(root.get("status"), AccountStatus.VOIDED));
      }
      text(root, cb, s.text(), where);
      equal(root, cb, "productCode", s.productCode(), where);
      equal(root, cb, "lineCode", s.lineCode(), where);
      equal(root, cb, "insurerCode", s.insurerCode(), where);
      if (!s.statuses().isEmpty()) {
        where.add(root.get("status").in(s.statuses()));
      }
      flags(root, cb, s, where);
      dates(root, cb, s.periodFrom(), s.periodTo(), where);
      if (present(s.accountOfficer())) {
        where.add(
            cb.equal(
                cb.lower(root.get("sales").get("accountOfficer")),
                s.accountOfficer().toLowerCase(Locale.ROOT)));
      }
      if (present(s.pnNumber())) {
        where.add(cb.exists(pnNumber(root, query.subquery(Long.class), cb, s.pnNumber())));
      }
      if (present(s.vehicleId()) || present(s.location())) {
        where.add(cb.exists(items(root, query.subquery(Long.class), cb, s)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static void text(
      Root<Account> root, CriteriaBuilder cb, String text, List<Predicate> where) {
    if (!present(text)) {
      return;
    }
    String like = like(text);
    where.add(
        cb.or(
            cb.like(cb.lower(root.get("arn")), like),
            cb.like(cb.lower(root.get("clientCode")), like),
            cb.like(cb.lower(root.get("clientName")), like),
            cb.like(cb.lower(root.get("quotationRef")), like)));
  }

  private static void flags(
      Root<Account> root, CriteriaBuilder cb, AccountSearch s, List<Predicate> where) {
    if (s.ffy() != null) {
      where.add(cb.equal(root.get("freeFirstYear").get("active"), s.ffy()));
    }
    if (s.directPayment() != null) {
      Expression<PaymentArrangement> arrangement = root.get("paymentArrangement");
      where.add(
          Boolean.TRUE.equals(s.directPayment())
              ? cb.equal(arrangement, PaymentArrangement.DIRECT_TO_INSURER)
              : cb.equal(arrangement, PaymentArrangement.VIA_BDOI));
    }
  }

  private static void dates(
      Root<Account> root, CriteriaBuilder cb, LocalDate from, LocalDate to, List<Predicate> where) {
    if (from != null) {
      where.add(cb.greaterThanOrEqualTo(root.get("periodFrom"), from));
    }
    if (to != null) {
      where.add(cb.lessThanOrEqualTo(root.get("periodFrom"), to));
    }
  }

  private static void equal(
      Root<Account> root, CriteriaBuilder cb, String field, String value, List<Predicate> where) {
    if (present(value)) {
      where.add(cb.equal(root.get(field), value));
    }
  }

  private static Subquery<Long> pnNumber(
      Root<Account> root, Subquery<Long> sub, CriteriaBuilder cb, String pn) {
    Root<Account> account = sub.from(Account.class);
    Join<Account, String> number = account.join("pnNumbers");
    return sub.select(account.get("id"))
        .where(
            cb.equal(account, root),
            cb.like(cb.upper(number), "%" + pn.strip().toUpperCase(Locale.ROOT) + "%"));
  }

  private static Subquery<Long> items(
      Root<Account> root, Subquery<Long> sub, CriteriaBuilder cb, AccountSearch s) {
    Root<RiskItem> item = sub.from(RiskItem.class);
    List<Predicate> where = new ArrayList<>();
    where.add(cb.equal(item.get("account"), root));
    String id = RiskIdentifiers.vehicleId(s.vehicleId());
    if (id != null) {
      where.add(
          cb.or(
              cb.equal(item.get("plateNo"), id),
              cb.equal(item.get("conductionSticker"), id),
              cb.equal(item.get("engineNo"), id),
              cb.equal(item.get("chassisNo"), id)));
    }
    if (present(s.location())) {
      String like = like(s.location());
      where.add(
          cb.or(
              cb.like(cb.lower(item.get("address")), like),
              cb.like(cb.lower(item.get("city")), like)));
    }
    return sub.select(item.get("id")).where(where.toArray(Predicate[]::new));
  }

  private static String like(String text) {
    return "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }
}
