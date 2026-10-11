package com.iortatechnxt.brokerverse.renewal.channel.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Channel messages. */
public interface ChannelMessageRepository extends JpaRepository<ChannelMessage, Long> {

  /**
   * A message of a company by its number.
   *
   * @param companyId company
   * @param messageNo number
   * @return message
   */
  Optional<ChannelMessage> findByCompanyIdAndMessageNo(Long companyId, String messageNo);

  /**
   * Messages in some statuses (status refresh and retries).
   *
   * @param statuses statuses
   * @return messages
   */
  List<ChannelMessage> findByStatusIn(Collection<ChannelStatus> statuses);

  /**
   * Messages of a renewal account, newest first.
   *
   * @param candidateId renewal
   * @return messages
   */
  List<ChannelMessage> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * Messages of a document, newest first.
   *
   * @param companyId company
   * @param docRef document reference
   * @return messages
   */
  List<ChannelMessage> findByCompanyIdAndDocRefOrderByIdDesc(Long companyId, String docRef);
}
