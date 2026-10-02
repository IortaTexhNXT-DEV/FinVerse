package com.iortatechnxt.brokerverse.disbursement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The SIT/UAT payees and the certificate received carry the names of the parties of the seed
 * catalogue: the insurer INS-MGIC is Mabuhay General Insurance Corp. and the client CL-2026-000001
 * is Santos, Maria Clara Reyes, as on the other screens. The check reads the seed sources.
 */
class SeedPayeeNamesTest {

  private static final Path JAVA =
      Path.of("src/main/java/com/iortatechnxt/brokerverse").toAbsolutePath();

  private static String source(String file) throws IOException {
    return Files.readString(JAVA.resolve(file), StandardCharsets.UTF_8);
  }

  @Test
  void theSeedPayeesAreNamedAsInTheCatalogue() throws IOException {
    String disbursement = source("disbursement/seed/DisbursementSeedData.java");
    assertThat(disbursement)
        .contains("\"Mabuhay General Insurance Corp.\"")
        .contains("\"Santos, Maria Clara Reyes\"")
        .doesNotContain("MAPFRE")
        .doesNotContain("Client Refund Payee")
        .as("the deposited-checks file of the seed has a bank reference, not a seed marker")
        .doesNotContain("\"SEED\"")
        .doesNotContain("brokerverse-seed");
  }

  @Test
  void theCertificateReceivedNamesTheInsurerOfTheCatalogue() throws IOException {
    String frbs = source("frbs/seed/FrbsSeedData.java");
    assertThat(frbs)
        .contains("\"INS-MGIC\"")
        .contains("\"Mabuhay General Insurance Corp.\"")
        .doesNotContain("MAPFRE");
  }
}
