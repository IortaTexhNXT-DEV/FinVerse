package com.iortatechnxt.brokerverse.messaging.domain;

/**
 * Content of an in-app notification.
 *
 * @param title short title
 * @param body optional detail
 * @param link frontend route to open
 * @param entityType related record type
 * @param entityId related record id
 */
public record Notice(String title, String body, String link, String entityType, String entityId) {}
