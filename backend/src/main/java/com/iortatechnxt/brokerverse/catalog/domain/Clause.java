package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * An entry of the clause library (PMADD02): a warranty, clause, exclusion or deductible wording,
 * for one product line or every line, with its effective period. Insurer x coverage terms of a
 * package version reference clauses by code. Maker-checker master data.
 */
@Entity
@Table(name = "cat_clause")
public class Clause extends EffectiveDatedRecord {

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 20)
  private String kind;

  @Column(name = "line_code", length = 30)
  private String lineCode;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String wording;

  protected Clause() {}

  /**
   * Creates a clause, pending authorization.
   *
   * @param code code (unique)
   * @param details kind, line, title, wording and period
   */
  public Clause(String code, ClauseDetails details) {
    super(details.effectiveFrom(), details.effectiveTo());
    this.code = code;
    apply(details);
  }

  /**
   * Changes the clause; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(ClauseDetails details) {
    setEffectivity(details.effectiveFrom(), details.effectiveTo());
    apply(details);
    markModified();
  }

  private void apply(ClauseDetails details) {
    this.kind = details.kind();
    this.lineCode = details.lineCode();
    this.title = details.title();
    this.wording = details.wording();
  }

  /**
   * Whether the clause may be used on a product line.
   *
   * @param line product line code
   * @return true for an all-lines clause or a clause of that line
   */
  public boolean appliesTo(String line) {
    return lineCode == null || lineCode.equals(line);
  }

  @Override
  public String catalogReference() {
    return code;
  }

  @Override
  public String catalogDescription() {
    return title;
  }

  public String getCode() {
    return code;
  }

  public String getKind() {
    return kind;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getTitle() {
    return title;
  }

  public String getWording() {
    return wording;
  }

  /**
   * Maintainable attributes of a clause.
   *
   * @param kind kind (LOV CLAUSE_KIND)
   * @param lineCode product line, null for every line
   * @param title title
   * @param wording full wording
   * @param effectiveFrom first valid date
   * @param effectiveTo last valid date, null when open ended
   */
  public record ClauseDetails(
      String kind,
      String lineCode,
      String title,
      String wording,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
