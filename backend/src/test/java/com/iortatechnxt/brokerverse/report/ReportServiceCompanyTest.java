package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportRegistry;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.report.render.PrintOptions;
import com.iortatechnxt.brokerverse.report.render.ReportContext;
import com.iortatechnxt.brokerverse.report.render.ReportRenderer;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** A report run without a company names the operating company, as every other client document. */
class ReportServiceCompanyTest {

  /** Writes the company of the header as the file. */
  private static final class CompanyRenderer implements ReportRenderer {
    @Override
    public ExportFormat format() {
      return ExportFormat.CSV;
    }

    @Override
    public byte[] render(ReportResult result, ReportContext context) {
      return context.companyName().getBytes(StandardCharsets.UTF_8);
    }
  }

  private static String header(CompanyRepository companies) {
    CurrentUser user = mock(CurrentUser.class);
    when(user.username()).thenReturn("auditor");
    SystemParameterService parameters = mock(SystemParameterService.class);
    when(parameters.text(any(), any())).thenReturn("");
    ReportService service =
        new ReportService(
            mock(ReportRegistry.class),
            mock(ReportArchiveService.class),
            List.of(new CompanyRenderer()),
            companies,
            mock(BranchRepository.class),
            mock(AuditTrailService.class),
            user,
            mock(UserDirectory.class),
            parameters,
            Clock.systemUTC());
    ReportResult result =
        new ReportResult(
            "UAM-USER-ACCESS", "User Access Report", List.of(), List.of(), List.of(), List.of());
    return new String(
        service.render(result, ExportFormat.CSV, PrintOptions.DEFAULT, null),
        StandardCharsets.UTF_8);
  }

  @Test
  void aReportWithoutCompanyNamesTheOperatingCompany() {
    CompanyRepository companies = mock(CompanyRepository.class);
    when(companies.findFirstByOrderByIdAsc())
        .thenReturn(
            Optional.of(new Company("FVI", "BDO Insurance and Reinsurance Brokers, Inc.", "PHP")));
    assertThat(header(companies)).isEqualTo("BDO Insurance and Reinsurance Brokers, Inc.");
  }

  @Test
  void withoutAnyCompanyTheSystemNameIsKept() {
    CompanyRepository companies = mock(CompanyRepository.class);
    when(companies.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
    assertThat(header(companies)).isNotBlank();
  }
}
