package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * New or changed client (BRNB.030/048/049). Only the type and the name are needed for a prospect
 * (BRNB.029); format rules apply to whatever is entered.
 *
 * @param companyId company (create only)
 * @param clientType individual or corporate
 * @param lastName last name (individual)
 * @param firstName first name (individual)
 * @param middleName middle name
 * @param suffix suffix
 * @param corporateName corporate name
 * @param birthDate birth date (individual)
 * @param tin TIN as 000-000-000-000
 * @param idType ID type (list ID_TYPE)
 * @param idNumber ID number
 * @param email e-mail
 * @param mobile mobile 09xxxxxxxxx or +639xxxxxxxxx
 * @param phone landline
 * @param addressLine street address
 * @param city city
 * @param province province
 * @param postalCode postal code
 * @param marketSegment market segment (list MARKET_SEGMENT)
 * @param bankClient BDO bank client
 * @param bankCif BDO CIF number
 * @param nationality nationality (list NATIONALITY)
 * @param civilStatus civil status (list CIVIL_STATUS)
 * @param occupation occupation or nature of business
 * @param sourceOfFunds source of funds (list SOURCE_OF_FUNDS)
 * @param riskRating risk rating (list KYC_RISK_RATING)
 */
public record ClientRequest(
    Long companyId,
    @NotNull ClientType clientType,
    @Size(max = 100) String lastName,
    @Size(max = 100) String firstName,
    @Size(max = 100) String middleName,
    @Size(max = 20) String suffix,
    @Size(max = 200) String corporateName,
    @Past LocalDate birthDate,
    @Pattern(regexp = "\\d{3}-\\d{3}-\\d{3}-\\d{3}", message = "must be 000-000-000-000")
        String tin,
    @Size(max = 40) String idType,
    @Size(max = 60) String idNumber,
    @Size(max = 120) String email,
    @Pattern(regexp = "09\\d{9}|\\+639\\d{9}", message = "must be 09xxxxxxxxx or +639xxxxxxxxx")
        String mobile,
    @Size(max = 30) String phone,
    @Size(max = 300) String addressLine,
    @Size(max = 80) String city,
    @Size(max = 80) String province,
    @Size(max = 10) String postalCode,
    @Size(max = 40) String marketSegment,
    boolean bankClient,
    @Size(max = 40) String bankCif,
    @Size(max = 40) String nationality,
    @Size(max = 40) String civilStatus,
    @Size(max = 120) String occupation,
    @Size(max = 40) String sourceOfFunds,
    @Size(max = 40) String riskRating) {

  /**
   * Client data (blank values become null).
   *
   * @return details
   */
  public ClientDetails details() {
    return new ClientDetails(
        clientType,
        new PersonName(
            clean(lastName),
            clean(firstName),
            clean(middleName),
            clean(suffix),
            clean(corporateName)),
        birthDate,
        new Identity(clean(tin), clean(idType), clean(idNumber)),
        new Contact(
            clean(email),
            clean(mobile),
            clean(phone),
            clean(addressLine),
            clean(city),
            clean(province),
            clean(postalCode)),
        clean(marketSegment),
        bankClient,
        clean(bankCif));
  }

  /**
   * KYC profile (blank values become null).
   *
   * @return profile
   */
  public ClientProfile profile() {
    return new ClientProfile(
        clean(nationality),
        clean(civilStatus),
        clean(occupation),
        clean(sourceOfFunds),
        clean(riskRating));
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
