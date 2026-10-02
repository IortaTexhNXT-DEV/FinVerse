package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * New or changed insurer (party data is used on creation only).
 *
 * @param companyId company
 * @param partyCode party code
 * @param name name
 * @param shortName short name
 * @param accreditationNo accreditation number
 * @param accreditedUntil accreditation expiry
 * @param placementChannel placement channel (EMAIL)
 * @param placementEmails placement mailboxes
 * @param defaultCreditDays default credit days
 * @param taxId tax id of the party
 * @param address address of the party
 * @param email general e-mail of the party
 * @param phone phone of the party
 */
public record InsurerRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9\\-]+") String partyCode,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 40) String shortName,
    @Size(max = 40) String accreditationNo,
    LocalDate accreditedUntil,
    @NotNull PlacementChannel placementChannel,
    List<@Email String> placementEmails,
    @Min(0) @Max(365) int defaultCreditDays,
    @Size(max = 30) String taxId,
    @Size(max = 300) String address,
    @Email @Size(max = 120) String email,
    @Size(max = 40) String phone) {

  /**
   * Profile attributes.
   *
   * @return details
   */
  public InsurerDetails details() {
    return new InsurerDetails(
        name.trim(),
        shortName,
        accreditationNo,
        accreditedUntil,
        placementChannel,
        placementEmails == null ? List.of() : List.copyOf(placementEmails),
        defaultCreditDays);
  }

  /**
   * Party contact data.
   *
   * @return contact
   */
  public PartyContact contact() {
    return new PartyContact(taxId, address, email, phone);
  }
}
