package com.iortatechnxt.brokerverse.productmaint.report;

import com.iortatechnxt.brokerverse.report.core.CodeSetSource;
import java.util.List;
import org.springframework.stereotype.Component;

/** The columns offered by the Product Maintenance report builder (BDOI FRS FRPM.020.01). */
@Component
public class BuilderColumnCodes implements CodeSetSource {

  /** Key of the source. */
  public static final String SOURCE = "pm.builderColumns";

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public List<CodeOption> options(Long companyId) {
    return BuilderField.all().stream().map(f -> new CodeOption(f.name(), f.label())).toList();
  }
}
