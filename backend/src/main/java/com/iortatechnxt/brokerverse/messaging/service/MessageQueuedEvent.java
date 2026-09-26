package com.iortatechnxt.brokerverse.messaging.service;

/**
 * Published when an e-mail is queued; {@link MailDispatcher} delivers it after the commit.
 *
 * @param messageId message
 */
public record MessageQueuedEvent(Long messageId) {}
