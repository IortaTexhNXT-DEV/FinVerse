package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequestRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the payee master (DIS 2.2.8, 2.4.3, 3.25.2): the consolidated list with active /
 * inactive filters, one payee with its bank accounts, the payee maintenance requests and the
 * matching of a payment request's payee (party code first, then the name).
 */
@Service
@Transactional(readOnly = true)
public class PayeeQueryService {

  private static final String STAGE = "stage";

  private final PayeeRepository payees;
  private final PayeeRequestRepository requests;

  /**
   * Creates the service.
   *
   * @param payees payees
   * @param requests payee requests
   */
  public PayeeQueryService(PayeeRepository payees, PayeeRequestRepository requests) {
    this.payees = payees;
    this.requests = requests;
  }

  /**
   * A payee with its bank accounts.
   *
   * @param id payee
   * @return payee
   */
  public Payee get(Long id) {
    return payees
        .findWithAccountsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.PAYEE, id));
  }

  /**
   * Payees of a company (DIS 2.2.8): code or name containing the text, in some stages, of a class.
   *
   * @param search filters
   * @param pageable page
   * @return payees
   */
  public Page<Payee> search(PayeeSearch search, Pageable pageable) {
    Page<Payee> page = payees.findAll(spec(search), pageable);
    // the list shows the primary account: load the accounts of the page in the transaction
    page.getContent().forEach(Payee::getAccounts);
    return page;
  }

  private static Specification<Payee> spec(PayeeSearch s) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), s.companyId()));
      if (s.stages() != null && !s.stages().isEmpty()) {
        where.add(root.get(STAGE).in(s.stages()));
      }
      if (s.payeeClass() != null && !s.payeeClass().isBlank()) {
        where.add(cb.equal(root.get("payeeClass"), s.payeeClass()));
      }
      if (s.text() != null && !s.text().isBlank()) {
        String like = "%" + s.text().strip().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get("payeeCode")), like),
                cb.like(cb.lower(root.get("name")), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  /**
   * Matches a payment request's payee to the master (DIS 3.25.2): the payee of the party code,
   * otherwise the only payee with the same name; only usable payees match.
   *
   * @param companyId company
   * @param payeeCode party code
   * @param payeeName name given by the source, may be null
   * @return usable payee
   */
  public Optional<Payee> match(Long companyId, String payeeCode, String payeeName) {
    Optional<Payee> byCode = payees.findByCompanyIdAndPayeeCode(companyId, payeeCode);
    if (byCode.isPresent()) {
      return byCode.filter(p -> p.getStage().usable());
    }
    if (payeeName == null || payeeName.isBlank()) {
      return Optional.empty();
    }
    List<Payee> byName =
        payees.findByName(companyId, payeeName.strip()).stream()
            .filter(p -> p.getStage().usable())
            .toList();
    return byName.size() == 1 ? Optional.of(byName.get(0)) : Optional.empty();
  }

  /**
   * The payee of a party code, whatever its stage.
   *
   * @param companyId company
   * @param payeeCode party code
   * @return payee
   */
  public Optional<Payee> byCode(Long companyId, String payeeCode) {
    return payees.findByCompanyIdAndPayeeCode(companyId, payeeCode);
  }

  /**
   * Payee maintenance requests with a status, newest first (DIS 2.2.1).
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return requests
   */
  public Page<PayeeRequest> requests(Long companyId, PayeeRequestStatus status, Pageable pageable) {
    return requests.findByCompanyIdAndStatusOrderByIdDesc(companyId, status, pageable);
  }

  /**
   * Payee search filters.
   *
   * @param companyId company
   * @param stages stages, empty for all
   * @param payeeClass class, null for all
   * @param text code or name contains
   */
  public record PayeeSearch(
      Long companyId, List<PayeeStage> stages, String payeeClass, String text) {}
}
