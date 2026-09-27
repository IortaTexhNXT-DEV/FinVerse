import type { RuleInput } from '@/api/submitted';

/** Names of the facts a rule condition can test (vocabulary of the rule engine). */
export const FACT_LABELS: Record<string, string> = {
  always: 'Always',
  segment: 'Segment',
  businessType: 'Business type',
  sourceCode: 'Source',
  migrated: 'Migrated',
  hasDocuments: 'Has documents',
  adequacyStatus: 'Review adequacy',
  insurerCode: 'Insurer',
  sumInsured: 'Sum insured',
  vehicleType: 'Vehicle type',
  vehicleAge: 'Vehicle age',
  daysToExpiry: 'Days to expiry',
  ffy: 'Free First Year',
  employeeAccount: 'Group employee',
  noTouch: 'No Touch',
  pnMissing: 'PN missing',
  duplicatePn: 'Same PN as another policy',
  duplicateUnit: 'Same unit as another policy',
  lamdFound: 'In the LAMD snapshot',
  classification: 'Classification',
  loanStatus: 'Loan status',
  amortised: 'Amortised loan',
  unitMatches: 'Unit matches the loan',
};

/** Names of the condition operators. */
export const OPERATOR_LABELS: Record<string, string> = {
  EQ: 'is',
  NE: 'is not',
  IN: 'is one of',
  NOT_IN: 'is none of',
  GT: 'is above',
  GTE: 'is at least',
  LT: 'is below',
  LTE: 'is at most',
  EMPTY: 'is empty',
  NOT_EMPTY: 'is filled',
};

/** Outcome choices of a rule. */
export const OUTCOME_CHOICES = {
  tag: { RENEWABLE: 'Renewable', NON_RENEWABLE: 'Non-Renewable' },
  classification: { INFORCED: 'In force', SUBMITTED: 'Submitted' },
  raTemplate: { GENERIC: 'Generic', FFY: 'Free First Year' },
  flag: { FALLOUT: 'Fallout', REVIEW: 'Review' },
} as const;

/** Steps of the processing runs. */
export const STEPS: Record<string, string> = {
  SANITATION: 'Sanitation',
  MATCHING: 'Matching',
  CLASSIFICATION: 'Classification',
  DISPOSITION: 'Disposition',
};

/** An empty rule. */
export function emptyRule(priority: number): RuleInput {
  return {
    priority,
    name: '',
    conditions: [{ field: 'always', operator: 'EQ', value: 'true' }],
    outcome: { bucket: null, tag: null, classification: null, raTemplate: null, flag: null },
    reasonCode: null,
    stop: true,
    active: true,
  };
}

/** The problems of a rule, one per field, empty when it can be saved. */
export function ruleProblems(r: RuleInput): Record<string, string> {
  const out: Record<string, string> = {};
  if (r.name.trim() === '') {
    out.name = 'Enter the name of the rule';
  }
  if (!Number.isInteger(r.priority) || r.priority < 1) {
    out.priority = 'Enter a priority of 1 or more';
  }
  if (r.conditions.length === 0) {
    out.conditions = 'Add at least one condition';
  }
  const valueless = new Set(['EMPTY', 'NOT_EMPTY']);
  if (r.conditions.some((c) => !valueless.has(c.operator) && (c.value ?? '').trim() === '')) {
    out.conditions = 'Enter the value of each condition';
  }
  const o = r.outcome;
  if (!o.bucket && !o.tag && !o.classification && !o.raTemplate && !o.flag && !r.reasonCode) {
    out.outcome = 'Select the outcome of the rule';
  }
  return out;
}
