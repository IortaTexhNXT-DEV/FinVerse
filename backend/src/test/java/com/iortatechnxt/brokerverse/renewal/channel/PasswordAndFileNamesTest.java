package com.iortatechnxt.brokerverse.renewal.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.iortatechnxt.brokerverse.renewal.channel.service.PasswordConvention;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterFileNamesProbe;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The password convention of protected files and the client's file names of the letters
 * (FRRN.022.01, FRRN.023.02, FRRN.024.01).
 */
class PasswordAndFileNamesTest {

  @Test
  void anIndividualsPasswordIsTheFirstThreeLettersOfTheFirstAndLastNamesInCapitals() {
    assertThat(PasswordConvention.individual("Juan", "Dela Cruz")).isEqualTo("JUADEL");
    assertThat(PasswordConvention.individual("Li", "Ong")).isEqualTo("LIONG");
    assertThat(PasswordConvention.individual("Ma. Luisa", "O'Neil")).isEqualTo("MALONE");
  }

  @Test
  void aCorporatePasswordHasOneLowerOneUpperOneSpecialAndFiveDigitsAndChangesEachTime() {
    PasswordConvention p =
        new PasswordConvention(
            mock(SystemParameterService.class), mock(NamedParameterJdbcTemplate.class));
    String first = p.corporate();
    assertThat(first).hasSize(8);
    assertThat(first.chars().filter(Character::isLowerCase).count()).isEqualTo(1);
    assertThat(first.chars().filter(Character::isUpperCase).count()).isEqualTo(1);
    assertThat(first.chars().filter(Character::isDigit).count()).isEqualTo(5);
    assertThat(first.chars().filter(ch -> "!@#$%&*?".indexOf(ch) >= 0).count()).isEqualTo(1);
    boolean changes = false;
    for (int i = 0; i < 5 && !changes; i++) {
      changes = !p.corporate().equals(first);
    }
    assertThat(changes).isTrue();
  }

  @Test
  void theLettersAreNamedByProductTypeNoticeReferenceAndDate() {
    LocalDate day = LocalDate.of(2026, 6, 30);
    assertThat(LetterFileNamesProbe.name("MTR", LetterType.RA, RaNotice.FIRST, "1234567890", day))
        .isEqualTo("MTR_RA_First Notice_1234567890_06302026.pdf");
    assertThat(LetterFileNamesProbe.name("MTR", LetterType.RA, RaNotice.SECOND, "1234567890", day))
        .isEqualTo("MTR_RA_Last Notice_1234567890_06302026.pdf");
    assertThat(LetterFileNamesProbe.name("MTR", LetterType.NFR, null, "1234567890", day))
        .isEqualTo("MTR_NRL_1234567890_06302026.pdf");
    assertThat(LetterFileNamesProbe.name("PAR", LetterType.NAL, null, "1234567890", day))
        .isEqualTo("PAR_NAL_1234567890_06302026.pdf");
  }
}
