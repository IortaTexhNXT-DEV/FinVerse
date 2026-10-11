package com.iortatechnxt.brokerverse.configpromo.engine;

import java.time.Instant;

/**
 * Who and when of an import, written to the audit columns of the rows it writes.
 *
 * @param maker user who prepared the import (created_by, updated_by)
 * @param checker user who approved it (authorized_by of active master records)
 * @param at time of the import
 */
public record ApplyActors(String maker, String checker, Instant at) {}
