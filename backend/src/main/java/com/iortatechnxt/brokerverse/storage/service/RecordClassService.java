package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.storage.domain.RecordClass;
import com.iortatechnxt.brokerverse.storage.domain.RecordClassRepository;
import com.iortatechnxt.brokerverse.storage.domain.RecordClassSettings;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The parameterised record classes of stored files: bucket, retention mapped to the retention
 * rules, legal hold and "archive to ECM" (DOCUMENT_STORAGE_DECISION, decisions 3 and 5).
 */
@Service
@Transactional
public class RecordClassService {

  private static final String ENTITY = "RecordClass";

  private final RecordClassRepository classes;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param classes repository
   * @param audit audit trail
   */
  public RecordClassService(RecordClassRepository classes, AuditTrailService audit) {
    this.classes = classes;
    this.audit = audit;
  }

  /**
   * All record classes.
   *
   * @return classes by code
   */
  @Transactional(readOnly = true)
  public List<RecordClass> list() {
    return classes.findAllByOrderByCodeAsc();
  }

  /**
   * An active record class.
   *
   * @param code code
   * @return class
   */
  @Transactional(readOnly = true)
  public RecordClass requireActive(String code) {
    RecordClass recordClass = get(code);
    if (!recordClass.isActive()) {
      throw new BusinessRuleException(
          "RECORD_CLASS_INACTIVE", "The record class " + code + " does not accept new files");
    }
    return recordClass;
  }

  /**
   * A record class.
   *
   * @param code code
   * @return class
   */
  @Transactional(readOnly = true)
  public RecordClass get(String code) {
    return classes.findByCode(code).orElseThrow(() -> new ResourceNotFoundException(ENTITY, code));
  }

  /**
   * End of retention of a file of a class stored on a date: the longest active retention rule of
   * the mapped record type (years online plus years in archive), else the class period.
   *
   * @param recordClass class
   * @param from storage date
   * @return last day of retention
   */
  @Transactional(readOnly = true)
  public LocalDate retentionUntil(RecordClass recordClass, LocalDate from) {
    String recordType = recordClass.getRetentionRecordType();
    Integer years = recordType == null ? null : classes.retentionYears(recordType);
    return years != null ? from.plusYears(years) : from.plus(recordClass.fallbackRetention());
  }

  /**
   * Changes a record class (DOA approvers only, see the controller); audited.
   *
   * @param code code
   * @param settings new settings
   * @return class
   */
  public RecordClass change(String code, RecordClassSettings settings) {
    RecordClass recordClass = get(code);
    recordClass.change(settings);
    audit.record(
        ENTITY,
        code,
        AuditAction.UPDATE,
        "Record class "
            + code
            + ": retention "
            + recordClass.getRetentionPeriod()
            + (recordClass.getRetentionRecordType() == null
                ? ""
                : " (rules of " + recordClass.getRetentionRecordType() + ")")
            + ", legal hold "
            + recordClass.isLegalHold()
            + ", archive to ECM "
            + recordClass.isArchiveToEcm()
            + ", active "
            + recordClass.isActive());
    return recordClass;
  }
}
