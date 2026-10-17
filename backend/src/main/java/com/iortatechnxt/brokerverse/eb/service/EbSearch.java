package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The free-text filter of the EB work lists: the record's own numbers or its programme. */
public final class EbSearch {

  private EbSearch() {}

  /**
   * Matches a text fragment against fields of the record and the number and client of its programme
   * ({@code programmeId}).
   *
   * @param root record
   * @param query query
   * @param cb criteria builder
   * @param text fragment
   * @param ownFields text fields of the record
   * @param <T> record type
   * @return predicate
   */
  public static <T> Predicate text(
      Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, String text, String... ownFields) {
    String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    Subquery<Long> programme = query.subquery(Long.class);
    Root<EbProgramme> p = programme.from(EbProgramme.class);
    programme
        .select(p.get("id"))
        .where(
            cb.or(
                cb.like(cb.lower(p.get("programmeNo")), like),
                cb.like(cb.lower(p.get("clientName")), like)));
    List<Predicate> any = new ArrayList<>();
    for (String field : ownFields) {
      any.add(cb.like(cb.lower(root.get(field)), like));
    }
    any.add(root.get("programmeId").in(programme));
    return cb.or(any.toArray(Predicate[]::new));
  }
}
