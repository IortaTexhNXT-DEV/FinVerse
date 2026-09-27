package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP contract of the Employee Benefits foundation (wave E0) and of the shared work items it
 * delivers: the account business type filter and fields (BT0), the EB lists, and the document
 * access classes on list and download (BRID-025).
 */
@IntegrationTest
class EbFoundationApiIT {

  @Autowired private Api api;
  @Autowired private AsUser as;
  @Autowired private DocumentService documents;
  @Autowired private TestData data;

  @Test
  void accountsAreFilteredByBusinessType() throws Exception {
    String base = "/api/v1/accounts?companyId=" + data.company().getId() + "&size=50";
    api.doGet("ao", base + "&businessType=NEW_BUSINESS")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].businessType").value("NEW_BUSINESS"))
        .andExpect(jsonPath("$.content[?(@.businessType == 'RENEWAL')]").isEmpty());
    api.doGet("ao", base + "&businessType=RENEWAL")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.businessType == 'NEW_BUSINESS')]").isEmpty());
    api.doGet("ao", base + "&businessType=SIDEWAYS").andExpect(status().is4xxClientError());
  }

  @Test
  void theEbListsAreServed() throws Exception {
    api.doGet("ao", "/api/v1/lov/EB_BENEFIT_LINE/options")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    api.doGet("ao", "/api/v1/lov/EB_PROCESS_TYPE/options")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6));
  }

  @Test
  void restrictedDocumentsAreNeitherListedNorDownloadedOutsideTheirDepartments() throws Exception {
    AttachmentTarget target =
        new AttachmentTarget("EbProgramme", Long.toString(System.nanoTime() % 1_000_000_000L));
    Long id =
        as.run(
                "ao",
                () ->
                    documents.upload(
                        target,
                        List.of(
                            new UploadedFile(
                                "utilization.pdf",
                                "%PDF-1.4 utilization".getBytes(StandardCharsets.US_ASCII))),
                        new UploadOptions("EB_UTILIZATION", false, null, null, "PROPOSAL")))
            .get(0)
            .getId();
    api.doGet("ao", "/api/v1/attachments?entityType=EbProgramme&entityId=" + target.entityId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    api.doGet("ao", "/api/v1/attachments/" + id + "/content").andExpect(status().isForbidden());
    api.doGet("ao", "/api/v1/attachments/zip?ids=" + id).andExpect(status().isForbidden());
  }
}
