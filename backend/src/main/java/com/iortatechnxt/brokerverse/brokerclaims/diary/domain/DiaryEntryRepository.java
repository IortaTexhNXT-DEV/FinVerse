package com.iortatechnxt.brokerverse.brokerclaims.diary.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Diary entries of the claims ({@code bcl_diary_entry}, BRCLM.022). */
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

  /**
   * The entries of a claim, newest first.
   *
   * @param claimId claim
   * @return entries
   */
  List<DiaryEntry> findByClaimIdOrderByEntryDateDescIdDesc(Long claimId);
}
