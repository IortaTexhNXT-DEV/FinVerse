package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Collection;

/**
 * A document required for a process (BRID-026; FR-EB-034): process type x benefit line (blank for
 * every line) x document type, mandatory or optional. Maintained with maker-checker; checked before
 * a franchise request, a submission and the placement trigger.
 */
@Entity
@Table(name = "eb_required_document")
public class EbRequiredDocument extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "process_type", nullable = false, length = 30)
  private String processType;

  @Column(name = "benefit_line", length = 30)
  private String benefitLine;

  @Column(name = "document_type", nullable = false, length = 40)
  private String documentType;

  @Column(nullable = false)
  private boolean mandatory;

  protected EbRequiredDocument() {}

  /**
   * Creates a requirement pending authorisation.
   *
   * @param companyId company
   * @param data requirement
   */
  public EbRequiredDocument(Long companyId, Data data) {
    this.companyId = companyId;
    apply(data);
  }

  /**
   * Changes the requirement; it must be authorised again.
   *
   * @param data requirement
   */
  public void update(Data data) {
    apply(data);
    markModified();
  }

  private void apply(Data data) {
    this.processType = data.processType();
    this.benefitLine = data.benefitLine();
    this.documentType = data.documentType();
    this.mandatory = data.mandatory();
  }

  /**
   * Whether the requirement applies to a process and a set of lines.
   *
   * @param process process type
   * @param lines benefit lines concerned
   * @return true when active, of the process and of every line or one of the lines
   */
  public boolean appliesTo(String process, Collection<String> lines) {
    return isActive()
        && processType.equals(process)
        && (benefitLine == null || lines.contains(benefitLine));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getProcessType() {
    return processType;
  }

  public String getBenefitLine() {
    return benefitLine;
  }

  public String getDocumentType() {
    return documentType;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Data of a requirement.
   *
   * @param processType process (list EB_PROCESS_TYPE)
   * @param benefitLine benefit line, null for every line
   * @param documentType document type
   * @param mandatory whether the process is refused without it
   */
  public record Data(
      String processType, String benefitLine, String documentType, boolean mandatory) {}
}
