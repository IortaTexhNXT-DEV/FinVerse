package com.iortatechnxt.brokerverse.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.placement.service.InsurerDirectory;
import com.iortatechnxt.brokerverse.placement.service.SlipPrerequisites;
import com.iortatechnxt.brokerverse.placement.service.SlipPrerequisites.Unmet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The missing documents of a placement slip are named by their document type, not its code. */
class SlipPrerequisitesTest {

  @Test
  void missingDocumentsAreNamedByTheirType() {
    ProductCatalogService catalog = mock(ProductCatalogService.class);
    ProductRuleService rules = mock(ProductRuleService.class);
    DocumentService documents = mock(DocumentService.class);
    LovService lovs = mock(LovService.class);
    RiskProduct product = mock(RiskProduct.class);
    when(catalog.requireProduct("PAR08")).thenReturn(product);
    when(rules.requiredDocuments(product)).thenReturn(Set.of("IDF", "VALID_ID"));
    when(documents.documentTypesOf(any())).thenReturn(Set.of("VALID_ID"));
    when(lovs.label("DOCUMENT_TYPE", "IDF")).thenReturn("Insurance declaration form");
    Account account = mock(Account.class, RETURNS_DEEP_STUBS);
    when(account.getStatus()).thenReturn(AccountStatus.READY_FOR_PLACEMENT);
    when(account.getLifecycle().getPaymentStatus()).thenReturn(PaymentStatus.PAID);
    when(account.getTsu().satisfied()).thenReturn(true);
    when(account.getProductCode()).thenReturn("PAR08");
    when(account.getId()).thenReturn(7L);

    List<Unmet> unmet =
        new SlipPrerequisites(catalog, rules, documents, mock(InsurerDirectory.class), lovs)
            .check(account);

    assertThat(unmet)
        .containsExactly(
            new Unmet("DOCUMENTS_MISSING", "Missing documents: Insurance declaration form"));
  }
}
