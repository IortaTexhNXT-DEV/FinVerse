package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceType;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceTypeRepository;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service invoice types (BRNB.100): recipient, trigger, owner and template, maintained on the
 * booking setup screen. Changes are audited.
 */
@Service
@Transactional
public class ServiceInvoiceTypeService {

  private static final String ENTITY = "ServiceInvoiceType";

  private final ServiceInvoiceTypeRepository types;
  private final DocTemplateService templates;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param types types
   * @param templates document templates (the template must exist)
   * @param audit audit trail
   * @param clock clock
   */
  public ServiceInvoiceTypeService(
      ServiceInvoiceTypeRepository types,
      DocTemplateService templates,
      AuditTrailService audit,
      Clock clock) {
    this.types = types;
    this.templates = templates;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Every type.
   *
   * @return types by code
   */
  @Transactional(readOnly = true)
  public List<ServiceInvoiceType> list() {
    return types.findAllByOrderByCodeAsc();
  }

  /**
   * Adds a type.
   *
   * @param code code
   * @param settings settings
   * @return type
   */
  public ServiceInvoiceType create(String code, ServiceInvoiceType.Settings settings) {
    if (types.findByCode(code).isPresent()) {
      throw new DuplicateResourceException("Service invoice type", code);
    }
    templates.current(settings.templateCode(), LocalDate.now(clock));
    ServiceInvoiceType type = types.save(new ServiceInvoiceType(code, settings));
    audit.record(ENTITY, code, AuditAction.CREATE, settings.name());
    return type;
  }

  /**
   * Changes a type.
   *
   * @param id type
   * @param settings settings
   * @return type
   */
  public ServiceInvoiceType update(Long id, ServiceInvoiceType.Settings settings) {
    ServiceInvoiceType type =
        types.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    templates.current(settings.templateCode(), LocalDate.now(clock));
    type.apply(settings);
    audit.record(ENTITY, type.getCode(), AuditAction.UPDATE, settings.name());
    return type;
  }
}
