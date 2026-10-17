package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Why a receipt is cancelled or reinstated, with the reinstatement type and the information of
 * CSHID.005 (FRS.CSH.03.01.05, 04.01.06).
 *
 * @param reasonCode reason (lists of cancellation and reinstatement reasons)
 * @param reasonText free text of "Others" or the remarks (100 characters)
 * @param reinstatementType full or partial, null for a cancellation
 * @param invoiceNo invoice number of a reinstatement
 * @param accountOfficer Account Officer
 * @param unitHead Unit Head
 * @param teamLeader Team Leader
 */
@Embeddable
public record RecordReason(
    @Column(name = "reason_code", length = 40) String reasonCode,
    @Column(name = "reason_text", length = 250) String reasonText,
    @Enumerated(EnumType.STRING) @Column(name = "reinstatement_type", length = 10)
        ReinstatementType reinstatementType,
    @Column(name = "invoice_no", length = 40) String invoiceNo,
    @Column(name = "account_officer", length = 100) String accountOfficer,
    @Column(name = "unit_head", length = 100) String unitHead,
    @Column(name = "team_leader", length = 100) String teamLeader) {}
