package com.iortatechnxt.brokerverse.coa.domain;

/**
 * Tier of an account in the multi-level chart of accounts.
 *
 * <p>GROUP accounts are headings used only for roll-up and reporting and can never be posted to.
 * MAIN (GL Head), SUB (Subsidiary GL) and MICRO (Micro GL) accounts are postable when they have no
 * children, which prevents direct posting to heads of accounts.
 */
public enum AccountLevel {
  GROUP,
  MAIN,
  SUB,
  MICRO
}
