package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Locale;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PRF reads for the screens: the list with its filters, one PRF with its details, by client. */
@Service
@Transactional(readOnly = true)
public class ProposalQueryService {

  private final ProposalRequestRepository proposals;
  private final RiskDetailsCodec codec;

  /**
   * Creates the service.
   *
   * @param proposals PRFs
   * @param codec risk details JSON
   */
  public ProposalQueryService(ProposalRequestRepository proposals, RiskDetailsCodec codec) {
    this.proposals = proposals;
    this.codec = codec;
  }

  /**
   * One PRF with its insurers and accounts loaded.
   *
   * @param id id
   * @return PRF
   */
  public ProposalRequest get(Long id) {
    ProposalRequest p =
        proposals
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ProposalService.ENTITY, id));
    Hibernate.initialize(p.getInsurers());
    Hibernate.initialize(p.getAccountArns());
    return p;
  }

  /**
   * Risk details of a PRF.
   *
   * @param p PRF
   * @return details
   */
  public RiskDetails details(ProposalRequest p) {
    return codec.of(p);
  }

  /**
   * PRFs matching the criteria.
   *
   * @param search criteria
   * @param pageable page and sort
   * @return page
   */
  public Page<ProposalRequest> search(Search search, Pageable pageable) {
    return proposals.findAll(search.toSpecification(), pageable);
  }

  /**
   * PRFs of a client, newest first.
   *
   * @param clientId client
   * @return PRFs
   */
  public List<ProposalRequest> byClient(Long clientId) {
    return proposals.findByClientIdOrderByCreatedAtDesc(clientId);
  }

  /**
   * Criteria of the PRF list.
   *
   * @param companyId company
   * @param text PRF number, ARN, QS / PS number, client code or name
   * @param statuses statuses, empty for all
   * @param createdBy maker, may be null
   * @param clientId client, may be null
   */
  public record Search(
      Long companyId, String text, List<ProposalStatus> statuses, String createdBy, Long clientId) {

    /** Defensive copy. */
    public Search {
      statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }

    Specification<ProposalRequest> toSpecification() {
      Specification<ProposalRequest> spec =
          (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
      if (!statuses.isEmpty()) {
        spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
      }
      if (createdBy != null) {
        String maker = createdBy.toLowerCase(Locale.ROOT);
        spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("createdBy")), maker));
      }
      if (clientId != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("clientId"), clientId));
      }
      return text == null || text.isBlank() ? spec : spec.and(matching(text));
    }

    private static Specification<ProposalRequest> matching(String fragment) {
      String pattern = "%" + fragment.strip().toLowerCase(Locale.ROOT) + "%";
      List<String> fields = List.of("prfNo", "arn", "qsNo", "psNo", "clientCode", "clientName");
      return (root, query, cb) ->
          cb.or(
              fields.stream()
                  .map(f -> cb.like(cb.lower(cb.coalesce(root.<String>get(f), "")), pattern))
                  .toArray(Predicate[]::new));
    }
  }
}
