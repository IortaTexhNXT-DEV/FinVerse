package com.iortatechnxt.brokerverse.crm.domain;

/**
 * A change to a client tag or instruction.
 *
 * @param kind TAG or INSTRUCTION
 * @param ref tag code or instruction id
 * @param action ADDED, CHANGED or REMOVED
 * @param from value before
 * @param to value after
 */
public record NoteChange(String kind, String ref, String action, String from, String to) {}
