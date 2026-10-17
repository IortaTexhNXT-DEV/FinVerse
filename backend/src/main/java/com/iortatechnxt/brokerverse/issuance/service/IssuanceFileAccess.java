package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileOwnerAccess;
import com.iortatechnxt.brokerverse.storage.service.LegacyFileTable;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Who may open the stored files of issuance: Insurance Advices ({@value
 * InsuranceAdviceService#ENTITY}) for the readers of the issuance screens ({@code ACCOUNT_VIEW},
 * {@code EPOLICY_MANAGE}, {@code EPOLICY_SEND}); the files of a bulk e-policy upload ({@value
 * EpolicyUploadService#OWNER_TYPE}) for {@code EPOLICY_MANAGE}, who also adds them. Also declares
 * {@code iss_insurance_advice} and {@code iss_upload_item} for the copy of the files kept in the
 * database before ST1.
 */
@Component
public class IssuanceFileAccess implements FileOwnerAccess {

  private static final String MANAGE = "EPOLICY_MANAGE";
  private static final List<String> READERS = List.of("ACCOUNT_VIEW", MANAGE, "EPOLICY_SEND");

  private final CurrentUser currentUser;

  /**
   * Creates the resolver.
   *
   * @param currentUser current user
   */
  public IssuanceFileAccess(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public Set<String> ownerTypes() {
    return Set.of(InsuranceAdviceService.ENTITY, EpolicyUploadService.OWNER_TYPE);
  }

  @Override
  public boolean mayRead(FileOwner owner, String documentType) {
    return EpolicyUploadService.OWNER_TYPE.equals(owner.entityType())
        ? currentUser.hasAuthority(MANAGE)
        : READERS.stream().anyMatch(currentUser::hasAuthority);
  }

  @Override
  public boolean mayStore(FileOwner owner, String documentType) {
    return EpolicyUploadService.OWNER_TYPE.equals(owner.entityType())
        && currentUser.hasAuthority(MANAGE);
  }

  /**
   * Insurance Advice PDFs kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable insuranceAdviceTable() {
    return new LegacyFileTable(
        "iss_insurance_advice",
        InsuranceAdviceService.ENTITY,
        FileOrigin.GENERATED,
        "select a.id as file_key, a.company_id, cast(a.id as varchar) as owner_id, '"
            + InsuranceAdviceService.RECORD_CLASS
            + "' as record_class, cast(null as varchar) as document_type, a.file_name,"
            + " 'application/pdf' as content_type, a.sha256 as recorded_sha256, a.content"
            + " from iss_insurance_advice a where a.content is not null"
            + " and a.stored_file_id is null and a.id > ? order by a.id limit ?",
        "update iss_insurance_advice set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from iss_insurance_advice where content is not null");
  }

  /**
   * Files of bulk e-policy uploads kept in the database before ST1.
   *
   * @return table
   */
  @Bean
  static LegacyFileTable uploadItemTable() {
    return new LegacyFileTable(
        "iss_upload_item",
        EpolicyUploadService.OWNER_TYPE,
        FileOrigin.UPLOADED,
        "select i.id as file_key, b.company_id, cast(i.batch_id as varchar) as owner_id, '"
            + EpolicyUploadService.RECORD_CLASS
            + "' as record_class, cast(null as varchar) as document_type, i.file_name,"
            + " 'application/pdf' as content_type, i.sha256 as recorded_sha256, i.content"
            + " from iss_upload_item i join iss_upload_batch b on b.id = i.batch_id"
            + " where i.content is not null and i.stored_file_id is null and i.id > ?"
            + " order by i.id limit ?",
        "update iss_upload_item set stored_file_id = ? where id = ? and stored_file_id is null",
        "select count(*) as total, count(stored_file_id) as moved"
            + " from iss_upload_item where content is not null");
  }
}
