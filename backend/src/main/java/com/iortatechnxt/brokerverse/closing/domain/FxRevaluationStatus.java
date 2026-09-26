package com.iortatechnxt.brokerverse.closing.domain;

/**
 * FX revaluation run status: POSTED (revaluation journal posted), REVERSED (auto-reversal posted on
 * the first day of the next period).
 */
public enum FxRevaluationStatus {
  POSTED,
  REVERSED
}
