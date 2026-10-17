package com.iortatechnxt.brokerverse.issuance.domain;

/**
 * The account an Insurance Advice is about.
 *
 * @param accountId account id
 * @param arn Account Reference Number
 * @param clientId client id
 * @param clientName insured
 * @param mortgageeBank mortgagee bank (list MORTGAGEE_BANK)
 * @param insurerCode insurer
 * @param policyNumbers policy numbers, null until issued
 */
public record AdviceSubject(
    Long accountId,
    String arn,
    Long clientId,
    String clientName,
    String mortgageeBank,
    String insurerCode,
    String policyNumbers) {}
