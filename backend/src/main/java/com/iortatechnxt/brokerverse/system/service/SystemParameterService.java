package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.system.domain.ParameterValueType;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import com.iortatechnxt.brokerverse.system.domain.SystemParameterRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business parameters stored in {@code sys_parameter}. Any module may read them through the typed
 * getters; changes are validated against the parameter type and audited with old and new value.
 */
@Service
@Transactional
public class SystemParameterService {

  /** Web session inactivity timeout in minutes. */
  public static final String SESSION_TIMEOUT_MINUTES = "SESSION_TIMEOUT_MINUTES";

  /** Minutes of inactivity after which the web client warns of the coming sign-out (BRNB.040). */
  public static final String SESSION_IDLE_WARNING_MINUTES = "SESSION_IDLE_WARNING_MINUTES";

  /** Minutes before the absolute session end at which the web client warns (BRNB.040). */
  public static final String SESSION_EXPIRY_WARNING_MINUTES = "SESSION_EXPIRY_WARNING_MINUTES";

  /** Default ageing buckets (days). */
  public static final String AGEING_BUCKETS = "AGEING_BUCKETS";

  /** Footer text of exported reports. */
  public static final String REPORT_FOOTER_TEXT = "REPORT_FOOTER_TEXT";

  /** GL accounts monitored as suspense accounts. */
  public static final String SUSPENSE_ACCOUNT_CODES = "SUSPENSE_ACCOUNT_CODES";

  /** Days of job history shown in the monitor. */
  public static final String JOB_HISTORY_DAYS = "JOB_HISTORY_DAYS";

  /** Consecutive failed logins that lock an account (BDOI NFR, COLLECTIONS_DESIGN section 8). */
  public static final String LOGIN_MAX_FAILED_ATTEMPTS = "LOGIN_MAX_FAILED_ATTEMPTS";

  private static final String ENTITY = "SystemParameter";

  private final SystemParameterRepository repository;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param repository parameter repository
   * @param audit audit trail
   */
  public SystemParameterService(SystemParameterRepository repository, AuditTrailService audit) {
    this.repository = repository;
    this.audit = audit;
  }

  /**
   * Lists all parameters.
   *
   * @return parameters ordered by category and key
   */
  @Transactional(readOnly = true)
  public List<SystemParameter> list() {
    return repository.findAllByOrderByCategoryAscKeyAsc();
  }

  /**
   * Gets a parameter.
   *
   * @param key key
   * @return parameter
   */
  @Transactional(readOnly = true)
  public SystemParameter get(String key) {
    return repository
        .findByKey(key)
        .orElseThrow(() -> new ResourceNotFoundException("System parameter", key));
  }

  /**
   * Changes a parameter value (administrator action, audited).
   *
   * @param key key
   * @param value new value
   * @return updated parameter
   */
  public SystemParameter update(String key, String value) {
    SystemParameter parameter = get(key);
    String previous = parameter.getValue();
    parameter.changeValue(value);
    audit.record(
        ENTITY,
        key,
        AuditAction.UPDATE,
        "Changed " + key + " from '" + previous + "' to '" + parameter.getValue() + "'");
    return parameter;
  }

  /**
   * Reads a whole-number parameter.
   *
   * @param key key
   * @param fallback value when the parameter is missing
   * @return value
   */
  @Transactional(readOnly = true)
  public int intValue(String key, int fallback) {
    return repository
        .findByKey(key)
        .map(SystemParameter::getValue)
        .map(String::trim)
        .map(Integer::parseInt)
        .orElse(fallback);
  }

  /**
   * Reads a text parameter.
   *
   * @param key key
   * @param fallback value when the parameter is missing
   * @return value
   */
  @Transactional(readOnly = true)
  public String text(String key, String fallback) {
    return repository.findByKey(key).map(SystemParameter::getValue).orElse(fallback);
  }

  /**
   * Reads a list parameter (INTEGER_LIST or CODE_LIST) as items.
   *
   * @param key key
   * @return items, empty when missing or blank
   */
  @Transactional(readOnly = true)
  public List<String> items(String key) {
    return repository
        .findByKey(key)
        .map(p -> ParameterValueType.items(p.getValue()))
        .orElse(List.of());
  }
}
