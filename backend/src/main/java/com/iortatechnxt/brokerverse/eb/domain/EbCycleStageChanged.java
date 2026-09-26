package com.iortatechnxt.brokerverse.eb.domain;

/**
 * Domain event: the stage of a cycle changed (the {@code EB_CYCLE} work case moved), published by
 * {@code eb.service.EbCycleMirror} inside the transition's transaction. Contract between the EB
 * waves: servicing (E1-C) opens the CONTRACT tracked items when a cycle reaches IN_PLACEMENT and
 * the programme status follows PLACED, CLOSED_LOST and NOT_RENEWED (E1-B).
 *
 * @param cycleId cycle
 * @param cycleNo cycle number
 * @param programmeId programme
 * @param from previous stage
 * @param to new stage
 * @param action workflow action
 * @param reasonCode reason code of the transition, null when none
 */
public record EbCycleStageChanged(
    Long cycleId,
    String cycleNo,
    Long programmeId,
    EbCycleStage from,
    EbCycleStage to,
    String action,
    String reasonCode) {}
