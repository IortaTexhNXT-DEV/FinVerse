package com.iortatechnxt.brokerverse.bulk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Kind;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Need;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplate;
import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class BulkTemplatesTest {

  private static final BulkTemplateLists LISTS =
      type ->
          "CIVIL_STATUS".equals(type)
              ? List.of(new Choice("SINGLE", "Single"), new Choice("MARRIED", "Married"))
              : List.of();

  private static final BulkImportHandler HANDLER =
      new BulkImportHandler() {
        @Override
        public String code() {
          return "T";
        }

        @Override
        public String title() {
          return "Client update";
        }

        @Override
        public String permission() {
          return "X";
        }

        @Override
        public String filledBy() {
          return "Client service officers";
        }

        @Override
        public List<String> rules() {
          return List.of("One row per client.");
        }

        @Override
        public List<BulkColumn> columns() {
          return List.of(
              BulkColumn.required("Client Code", "Client to update", "CL-1").master("client"),
              BulkColumn.optional("Civil Status", "Civil status", "SINGLE").lov("CIVIL_STATUS"),
              BulkColumn.optional("Religion", "Religion", "").lov("RELIGION"),
              BulkColumn.optional("Business Type", "Type", "NEW_BUSINESS")
                  .codes("NEW_BUSINESS", "RENEWAL"),
              BulkColumn.optional("Renewal Of", "Expiring ARN", "")
                  .when("Business Type is RENEWAL"),
              new BulkColumn("Birth Date", "Birth date", false, BulkColumn.Type.DATE, "1980-05-01"),
              new BulkColumn("Fleet", "Fleet", false, BulkColumn.Type.YES_NO, "N"));
        }

        @Override
        public List<String> validate(BulkRow row, BulkContext context) {
          return List.of();
        }
      };

  @Test
  void theHandlerColumnsBecomeTheColumnGuide() {
    GuidedTemplate t = BulkTemplates.of(HANDLER, LISTS, 5000);
    assertThat(t.name()).isEqualTo("Client update");
    assertThat(t.filledBy()).isEqualTo("Client service officers");
    assertThat(t.howToUpload()).startsWith("Bulk Processing > Bulk Uploads > Client update");
    assertThat(t.rules()).first().asString().contains("At most 5000 rows per file");
    assertThat(t.rules()).contains("One row per client.");
    List<GuideColumn> c = t.sheets().get(0).columns();
    assertThat(c.get(0).need()).isEqualTo(Need.YES);
    assertThat(c.get(0).allowedText(6)).isEqualTo("Code of an existing client");
    assertThat(c.get(1).kind()).isEqualTo(Kind.LIST);
    assertThat(c.get(1).choices()).extracting(Choice::code).containsExactly("SINGLE", "MARRIED");
    assertThat(c.get(2).kind()).isEqualTo(Kind.TEXT);
    assertThat(c.get(2).allowedText(6)).isEqualTo("Code of the list Religion (Lists of Values)");
    assertThat(c.get(3).choices())
        .containsExactly(
            new Choice("NEW_BUSINESS", "New business"), new Choice("RENEWAL", "Renewal"));
    assertThat(c.get(4).needText()).isEqualTo("Conditional: Business Type is RENEWAL");
    assertThat(c.get(5).kind()).isEqualTo(Kind.DATE);
    assertThat(c.get(6).kind()).isEqualTo(Kind.YES_NO);
    assertThat(t.sheets().get(0).examples()).hasSize(1);
  }

  @Test
  void aCurrencyExampleShowsTheBaseCurrencyOfTheCompanyAndKeepsItsGuide() {
    BulkColumn currency =
        BulkColumn.required("Currency", "Currency of payment", BulkColumn.BASE_CURRENCY_EXAMPLE)
            .format("ISO currency code, 3 letters")
            .master("currency");
    GuideColumn forCompany = BulkTemplates.column(currency.forCompany("EUR"), LISTS);
    assertThat(forCompany.example()).isEqualTo("EUR");
    assertThat(forCompany.format()).isEqualTo("ISO currency code, 3 letters");
    assertThat(forCompany.allowedText(6)).isEqualTo("Code of an existing currency");
    assertThat(BulkTemplates.column(currency.forCompany(null), LISTS).example()).isEmpty();
  }

  @Test
  void theReaderSkipsTheGuideOfATemplateSavedAsCsvAndReadsPlainFiles() {
    BulkFileReader reader = new BulkFileReader();
    String guided =
        "Client update,,\n"
            + "What it is for,Upload file,\n"
            + ",,\n"
            + "Mandatory,Yes,No\n"
            + "Format,Text,Text\n"
            + "Allowed values,Any,Any\n"
            + "What to enter,Client,Status\n"
            + GuidedTablesMarker.CORNER
            + ",Client Code *,Civil Status\n"
            + "Example – overwrite or delete,CL-1,SINGLE\n"
            + ",CL-2,MARRIED\n";
    ParsedFile file = reader.read("x.csv", guided.getBytes(StandardCharsets.UTF_8));
    assertThat(file.headers()).containsExactly("", "Client Code", "Civil Status");
    assertThat(file.rows())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.rowNo()).isEqualTo(10);
              assertThat(r.values()).containsEntry("Client Code", "CL-2").hasSize(2);
            });
    ParsedFile plain =
        reader.read("x.csv", "Client Code,Civil Status\nCL-3,\n".getBytes(StandardCharsets.UTF_8));
    assertThat(plain.headers()).containsExactly("Client Code", "Civil Status");
    assertThat(plain.rows()).singleElement().satisfies(r -> assertThat(r.rowNo()).isEqualTo(2));
  }

  @Test
  void theErrorFileLayoutKeepsTheGuideWithAnErrorColumnAndNoExample() {
    GuidedTemplate e = BulkTemplates.errorFile(BulkTemplates.of(HANDLER, LISTS, 100));
    List<GuideColumn> c = e.sheets().get(0).columns();
    assertThat(c.get(c.size() - 1).header()).isEqualTo("Error");
    assertThat(e.sheets().get(0).examples()).isEmpty();
    assertThat(GuidedTemplateWriter.write(e)).isNotEmpty();
  }

  /** The header corner text of the guided sheets. */
  private static final class GuidedTablesMarker {
    static final String CORNER =
        com.iortatechnxt.brokerverse.common.excel.GuidedTables.HEADER_CORNER;
  }
}
