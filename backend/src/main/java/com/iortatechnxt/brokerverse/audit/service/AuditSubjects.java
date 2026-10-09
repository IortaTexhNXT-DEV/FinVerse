package com.iortatechnxt.brokerverse.audit.service;

import java.util.Collection;
import java.util.Map;

/**
 * The client or assured's name of audited records (BDOI FRS FRPM.021.01: "Client/Assured's Name"
 * column), given by the module that owns the record type. The audit trail asks every implementation
 * and keeps the first name found.
 */
public interface AuditSubjects {

  /**
   * The names of audited records of one type.
   *
   * @param entityType audited record type
   * @param entityIds audited record references
   * @return name by reference; references the module does not know are left out
   */
  Map<String, String> subjects(String entityType, Collection<String> entityIds);
}
