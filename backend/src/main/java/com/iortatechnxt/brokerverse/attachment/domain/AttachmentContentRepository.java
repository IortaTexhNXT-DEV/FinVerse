package com.iortatechnxt.brokerverse.attachment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link AttachmentContent}. */
public interface AttachmentContentRepository extends JpaRepository<AttachmentContent, Long> {}
