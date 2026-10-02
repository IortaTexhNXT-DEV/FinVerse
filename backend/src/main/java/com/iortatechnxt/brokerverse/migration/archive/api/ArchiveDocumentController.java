package com.iortatechnxt.brokerverse.migration.archive.api;

import com.iortatechnxt.brokerverse.migration.archive.api.dto.InquiryDtos.StagedDocument;
import com.iortatechnxt.brokerverse.migration.archive.service.LegacyDocumentDrop;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The legacy documents staged in the console for the document index (object H02) while no transfer
 * folder is connected (DATA_MIGRATION_DESIGN sections 16 and 23).
 */
@RestController
@RequestMapping("/api/v1/migration/archive/documents")
public class ArchiveDocumentController {

  private final LegacyDocumentDrop drop;

  /**
   * Creates the controller.
   *
   * @param drop staged documents
   */
  public ArchiveDocumentController(LegacyDocumentDrop drop) {
    this.drop = drop;
  }

  /**
   * The documents staged for a legacy system.
   *
   * @param companyId company
   * @param system legacy system
   * @return documents
   */
  @GetMapping("/{system}")
  @PreAuthorize("hasAnyAuthority('MIG_INTAKE', 'MIG_LOAD_RUN')")
  public List<StagedDocument> staged(@RequestParam Long companyId, @PathVariable String system) {
    return drop.staged(companyId, system).stream().map(StagedDocument::from).toList();
  }

  /**
   * Stages legacy documents.
   *
   * @param companyId company
   * @param system legacy system
   * @param files files, named as in the document index
   * @return the staged documents
   * @throws IOException when a file cannot be read
   */
  @PostMapping("/{system}")
  @PreAuthorize("hasAuthority('MIG_INTAKE')")
  public List<StagedDocument> stage(
      @RequestParam Long companyId,
      @PathVariable String system,
      @RequestPart("files") List<MultipartFile> files)
      throws IOException {
    List<StagedDocument> out = new ArrayList<>();
    for (MultipartFile f : files) {
      out.add(
          StagedDocument.from(
              drop.stage(companyId, system, f.getOriginalFilename(), f.getBytes())));
    }
    return out;
  }
}
