package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.DuplicateKeys;
import com.iortatechnxt.brokerverse.crm.service.ClientRules;
import com.iortatechnxt.brokerverse.crm.service.ClientRules.Violation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClientRulesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

  private static ClientDetails person(LocalDate birth, String tin, String email, String mobile) {
    return new ClientDetails(
        ClientType.INDIVIDUAL,
        new PersonName("Reyes", "Ana", null, null, null),
        birth,
        new Identity(tin, null, null),
        new Contact(email, mobile, null, null, null, null, null),
        null,
        false,
        null);
  }

  @Test
  void acceptsWellFormedData() {
    assertThat(
            ClientRules.violations(
                person(LocalDate.of(1990, 1, 1), "123-456-789-000", "ana@x.ph", "+639171234567"),
                TODAY,
                18))
        .isEmpty();
    assertThat(ClientRules.violations(person(null, null, null, "09171234567"), TODAY, 18))
        .isEmpty();
  }

  @Test
  void reportsEveryBrokenRule() {
    assertThat(
            ClientRules.violations(
                    person(LocalDate.of(2015, 1, 1), "123456789", "not-an-email", "12345"),
                    TODAY,
                    18)
                .stream()
                .map(Violation::code))
        .containsExactly(
            "CLIENT_TIN_FORMAT", "CLIENT_EMAIL_FORMAT", "CLIENT_MOBILE_FORMAT", "CLIENT_UNDER_AGE");
    assertThat(ClientRules.violations(person(TODAY, null, null, null), TODAY, 18))
        .extracting(Violation::code)
        .containsExactly("CLIENT_BIRTH_DATE");
  }

  @Test
  void minimumAgeAppliesToIndividualsOnly() {
    ClientDetails corporate =
        new ClientDetails(
            ClientType.CORPORATE,
            new PersonName(null, null, null, null, "New Co."),
            LocalDate.of(2020, 1, 1),
            null,
            null,
            null,
            false,
            null);
    assertThat(ClientRules.violations(corporate, TODAY, 18)).isEmpty();
    assertThat(
            ClientRules.violations(person(LocalDate.of(2010, 1, 1), null, null, null), TODAY, 10))
        .isEmpty();
  }

  @Test
  void normalisesDuplicateKeys() {
    assertThat(DuplicateKeys.mobileKey("+63 917 123 4567")).isEqualTo("09171234567");
    assertThat(DuplicateKeys.mobileKey("0917-123-4567")).isEqualTo("09171234567");
    assertThat(DuplicateKeys.mobileKey("9171234567")).isEqualTo("09171234567");
    assertThat(DuplicateKeys.mobileKey("  ")).isNull();
    assertThat(DuplicateKeys.nameKey("Dela  Cruz", "JUAN Carlo")).isEqualTo("delacruz|juancarlo");
    assertThat(DuplicateKeys.nameKey("Dela Cruz", null)).isNull();
    assertThat(DuplicateKeys.corporateKey("Dela Cruz Trading Corp.")).isEqualTo("DELACRUZTRADING");
    assertThat(DuplicateKeys.corporateKey("dela cruz trading, inc")).isEqualTo("DELACRUZTRADING");
    assertThat(DuplicateKeys.corporateKey("Inc.")).isNull();
    assertThat(DuplicateKeys.idKey("passport", "p-123 45")).isEqualTo("PASSPORT:P12345");
    assertThat(DuplicateKeys.idKey(null, "1")).isNull();
    assertThat(DuplicateKeys.emailKey(" Ana@X.PH ")).isEqualTo("ana@x.ph");
    assertThat(ClientRules.isEmail("ana.reyes@bdo.com.ph")).isTrue();
    assertThat(ClientRules.isEmail("ana@bdo")).isFalse();
    assertThat(ClientRules.isEmail("@bdo.ph")).isFalse();
    assertThat(ClientRules.isEmail("a@b@bdo.ph")).isFalse();
    assertThat(ClientRules.isEmail("a b@bdo.ph")).isFalse();
    assertThat(ClientRules.isEmail("a@bdo.p")).isFalse();
  }
}
