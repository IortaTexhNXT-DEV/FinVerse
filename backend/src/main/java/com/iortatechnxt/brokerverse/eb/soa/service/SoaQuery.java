package com.iortatechnxt.brokerverse.eb.soa.service;

import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The SOA Register (FR-EB-053): SOAs of a company by status, insurer, programme and text. */
@Service
@Transactional(readOnly = true)
public class SoaQuery {

  private final EbSoaRepository soas;

  /**
   * Creates the query.
   *
   * @param soas SOAs
   */
  public SoaQuery(EbSoaRepository soas) {
    this.soas = soas;
  }

  /**
   * Searches the SOAs.
   *
   * @param companyId company
   * @param filter status, insurer, programme and text
   * @param pageable page
   * @return SOAs, latest first
   */
  public Page<EbSoa> search(Long companyId, Filter filter, Pageable pageable) {
    Specification<EbSoa> spec =
        (root, query, cb) -> {
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(root.get("companyId"), companyId));
          if (filter.status() != null && !filter.status().isBlank()) {
            where.add(cb.equal(root.get("status"), EbSoa.Status.valueOf(filter.status())));
          }
          if (filter.insurerCode() != null && !filter.insurerCode().isBlank()) {
            where.add(cb.equal(root.get("insurerCode"), filter.insurerCode()));
          }
          if (filter.programmeId() != null) {
            where.add(cb.equal(root.get("programmeId"), filter.programmeId()));
          }
          if (filter.text() != null && !filter.text().isBlank()) {
            String like = "%" + filter.text().strip().toLowerCase(Locale.ROOT) + "%";
            Subquery<Long> programme = query.subquery(Long.class);
            var p = programme.from(EbProgramme.class);
            programme
                .select(p.get("id"))
                .where(
                    cb.or(
                        cb.like(cb.lower(p.get("programmeNo")), like),
                        cb.like(cb.lower(p.get("clientName")), like)));
            where.add(
                cb.or(
                    cb.like(cb.lower(root.get("soaNo")), like),
                    cb.like(cb.lower(root.get("insurerSoaNo")), like),
                    root.get("programmeId").in(programme)));
          }
          query.orderBy(cb.desc(root.get("id")));
          return cb.and(where.toArray(Predicate[]::new));
        };
    return soas.findAll(spec, pageable);
  }

  /**
   * Filters of the register.
   *
   * @param status status, may be null
   * @param insurerCode insurer, may be null
   * @param programmeId programme, may be null
   * @param text EBS number, insurer SOA number, programme number or client fragment, may be null
   */
  public record Filter(String status, String insurerCode, Long programmeId, String text) {}
}
