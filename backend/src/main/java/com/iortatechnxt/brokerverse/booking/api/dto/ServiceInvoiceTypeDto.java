package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceType;
import com.iortatechnxt.brokerverse.booking.domain.SiRecipient;
import com.iortatechnxt.brokerverse.booking.domain.SiTrigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A service invoice type (BRNB.100), request and response.
 *
 * @param id id (ignored on input)
 * @param code code (used on creation only)
 * @param name name
 * @param recipient insurer or internal
 * @param trigger on booking, on endorsement or manual
 * @param ownerPermission owning team (permission)
 * @param ownerUsername owning user
 * @param templateCode document template
 * @param active active
 */
public record ServiceInvoiceTypeDto(
    Long id,
    @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,40}") String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull SiRecipient recipient,
    @NotNull SiTrigger trigger,
    @Size(max = 60) String ownerPermission,
    @Size(max = 50) String ownerUsername,
    @NotBlank @Size(max = 40) String templateCode,
    boolean active) {

  /**
   * Maps a type.
   *
   * @param t type
   * @return DTO
   */
  public static ServiceInvoiceTypeDto from(ServiceInvoiceType t) {
    return new ServiceInvoiceTypeDto(
        t.getId(),
        t.getCode(),
        t.getName(),
        t.getRecipient(),
        t.getTrigger(),
        t.getOwnerPermission(),
        t.getOwnerUsername(),
        t.getTemplateCode(),
        t.isActive());
  }

  /**
   * The settings.
   *
   * @return settings
   */
  public ServiceInvoiceType.Settings toSettings() {
    return new ServiceInvoiceType.Settings(
        name, recipient, trigger, ownerPermission, ownerUsername, templateCode, active);
  }
}
