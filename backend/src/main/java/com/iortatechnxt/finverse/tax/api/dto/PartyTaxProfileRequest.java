package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.tax.domain.PayeeClass;
import com.iortatechnxt.finverse.tax.domain.VatTreatment;
import com.iortatechnxt.finverse.tax.service.PartyTaxProfileCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update party tax profile request ({@code partyCode} is immutable after creation).
 *
 * @param companyId company
 * @param partyCode party
 * @param tin TIN as entered (e.g. 123-456-789-000)
 * @param branchCode TIN branch code (optional)
 * @param payeeClass individual or corporate
 * @param registeredName registered name
 * @param lastName last name (individuals)
 * @param firstName first name (individuals)
 * @param middleName middle name (individuals)
 * @param registeredAddress registered address
 * @param zipCode ZIP code
 * @param vatTreatment VAT treatment
 * @param defaultAtcCode default withholding tax code
 */
public record PartyTaxProfileRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) String partyCode,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[0-9\\- ]+") String tin,
    @Size(max = 5) @Pattern(regexp = "[0-9]*") String branchCode,
    @NotNull PayeeClass payeeClass,
    @NotBlank @Size(max = 200) String registeredName,
    @Size(max = 60) String lastName,
    @Size(max = 60) String firstName,
    @Size(max = 60) String middleName,
    @Size(max = 300) String registeredAddress,
    @Size(max = 10) String zipCode,
    VatTreatment vatTreatment,
    @Size(max = 20) String defaultAtcCode) {

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public PartyTaxProfileCommand toCommand() {
    return new PartyTaxProfileCommand(
        companyId,
        partyCode,
        tin,
        branchCode,
        payeeClass,
        registeredName,
        lastName,
        firstName,
        middleName,
        registeredAddress,
        zipCode,
        vatTreatment,
        defaultAtcCode);
  }
}
