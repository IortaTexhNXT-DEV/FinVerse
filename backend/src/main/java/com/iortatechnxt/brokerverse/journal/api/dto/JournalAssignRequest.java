package com.iortatechnxt.brokerverse.journal.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Assignment of journals to the user who will post them (FRBS 2.5.1).
 *
 * @param ids journals
 * @param assignee user name, blank to clear the assignment
 */
public record JournalAssignRequest(
    @NotEmpty @Size(max = 200) List<@NotNull Long> ids, @Size(max = 50) String assignee) {}
