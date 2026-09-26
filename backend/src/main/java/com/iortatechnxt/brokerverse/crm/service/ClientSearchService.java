package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.DuplicateKeys;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Multi-criteria client search (BRNB.046). */
@Service
@Transactional(readOnly = true)
public class ClientSearchService {

  private static final String STATUS = "status";
  private static final String KYC_STATUS = "kycStatus";

  private final ClientRepository clients;
  private final KycReviewPolicy reviewPolicy;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param reviewPolicy KYC review cycle (due window)
   * @param clock clock
   */
  public ClientSearchService(ClientRepository clients, KycReviewPolicy reviewPolicy, Clock clock) {
    this.clients = clients;
    this.reviewPolicy = reviewPolicy;
    this.clock = clock;
  }

  /**
   * Clients matching every given criterion.
   *
   * @param criteria criteria
   * @param pageable page and sort
   * @return page of clients
   */
  public Page<Client> search(ClientSearch criteria, Pageable pageable) {
    return clients.findAll(specification(criteria), pageable);
  }

  private Specification<Client> specification(ClientSearch s) {
    LocalDate horizon = reviewPolicy.dueHorizon(LocalDate.now(clock));
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("companyId"), s.companyId()));
      identityCriteria(s, root, cb, p);
      equalIfPresent(p, cb, root, STATUS, s.status());
      equalIfPresent(p, cb, root, KYC_STATUS, s.kycStatus());
      equalIfPresent(p, cb, root, "marketSegment", blankToNull(s.marketSegment()));
      equalIfPresent(p, cb, root, "bankClient", s.bankClient());
      equalIfPresent(p, cb, root, "clientType", s.clientType());
      if (s.kycDue()) {
        p.add(
            cb.or(
                cb.equal(root.get(KYC_STATUS), KycStatus.EXPIRED),
                cb.and(
                    cb.equal(root.get(KYC_STATUS), KycStatus.VERIFIED),
                    cb.lessThanOrEqualTo(root.get("kycReviewDue"), horizon))));
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static void identityCriteria(
      ClientSearch s, Root<Client> root, CriteriaBuilder cb, List<Predicate> p) {
    String code = blankToNull(s.code());
    if (code != null) {
      String prefix = code.trim().toUpperCase(Locale.ROOT) + "%";
      p.add(
          cb.or(
              cb.like(root.get("prospectCode"), prefix), cb.like(root.get("clientCode"), prefix)));
    }
    String name = blankToNull(s.name());
    if (name != null) {
      p.add(
          cb.like(
              cb.lower(root.get("displayName")), "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
    }
    equalIfPresent(p, cb, root, "tin", blankToNull(s.tin()));
    String idNumber = blankToNull(s.idNumber());
    if (idNumber != null) {
      p.add(cb.equal(cb.upper(root.get("idNumber")), idNumber.trim().toUpperCase(Locale.ROOT)));
    }
    String email = DuplicateKeys.emailKey(s.email());
    if (email != null) {
      p.add(cb.equal(cb.lower(root.get("email")), email));
    }
    String mobile = DuplicateKeys.mobileKey(s.mobile());
    if (mobile != null) {
      p.add(cb.equal(root.get("keys").get("mobileKey"), mobile));
    }
  }

  private static void equalIfPresent(
      List<Predicate> p, CriteriaBuilder cb, Root<Client> root, String attribute, Object value) {
    if (value != null) {
      p.add(cb.equal(root.get(attribute), value));
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
