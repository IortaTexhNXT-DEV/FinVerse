package com.iortatechnxt.brokerverse.common.sequence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link DocumentSequence}. */
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, String> {

  /**
   * Loads a sequence with a pessimistic write lock so concurrent allocations serialize.
   *
   * @param key sequence key
   * @return sequence if present
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from DocumentSequence s where s.sequenceKey = :key")
  Optional<DocumentSequence> lockByKey(@Param("key") String key);

  /**
   * Creates the sequence row if missing. Safe under concurrency (no error when it exists).
   *
   * @param key sequence key
   */
  @Modifying
  @Query(
      value =
          "insert into document_sequence (sequence_key, next_value) values (:key, 1)"
              + " on conflict (sequence_key) do nothing",
      nativeQuery = true)
  void createIfMissing(@Param("key") String key);
}
