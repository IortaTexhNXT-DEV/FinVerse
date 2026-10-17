package com.iortatechnxt.brokerverse.bulk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.excel.GuidedTemplateWriter;
import com.iortatechnxt.brokerverse.lov.domain.LovType;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every upload type of the platform has a guided template: its reader finds the header row with all
 * the handler's headers, skips the guide and the example row, and every list of values a column
 * refers to exists.
 */
@IntegrationTest
class BulkTemplatesIT {

  @Autowired private List<BulkImportHandler> handlers;
  @Autowired private BulkTemplateLists lists;
  @Autowired private BulkFileReader reader;
  @Autowired private LovService lov;

  @Test
  void everyUploadTypeHasAGuidedTemplateItsReaderUnderstands() {
    assertThat(handlers).hasSizeGreaterThan(40);
    for (BulkImportHandler handler : handlers) {
      if (handler.getClass().getSimpleName().startsWith("Test")) {
        continue;
      }
      List<String> headers = handler.columns().stream().map(BulkColumn::header).toList();
      byte[] template = GuidedTemplateWriter.write(BulkTemplates.of(handler, lists, 5000));
      ParsedFile file = reader.read("template.xlsx", template, TextLayout.AUTO, headers);
      assertThat(file.headers()).as(handler.code()).containsAll(headers);
      assertThat(file.rows()).as(handler.code()).isEmpty();
      assertThat(handler.filledBy()).as(handler.code()).doesNotStartWith("The team that runs");
    }
  }

  @Test
  void everyListOfValuesOfATemplateExists() {
    Set<String> types = lov.types().stream().map(LovType::getCode).collect(Collectors.toSet());
    for (BulkImportHandler handler : handlers) {
      for (BulkColumn column : handler.columns()) {
        if (!column.lov().isEmpty()) {
          assertThat(types).as(handler.code() + " " + column.header()).contains(column.lov());
        }
      }
    }
    assertThat(lists.choices("CIVIL_STATUS")).isNotEmpty();
  }
}
