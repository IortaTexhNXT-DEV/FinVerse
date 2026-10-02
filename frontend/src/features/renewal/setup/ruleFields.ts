import type { BucketRuleData, CheckSettingView, DecisionRuleData } from '@/api/renewal';
import { DISPOSITION_OPTIONS } from '../common/renewalCodes';
import type { RuleField } from './RuleVersionsTab';
import { SEVERITIES, numberOrNull, textOrNull } from './setupCodes';

const ANY = { code: '', label: 'Any' };

function priority<R extends { priority: number }>(): RuleField<R> {
  return {
    key: 'priority',
    label: 'Priority',
    numeric: true,
    get: (r) => String(r.priority),
    set: (r, v) => ({ ...r, priority: Number(v) || 0 }),
  };
}

/** Columns of a bucket rule (BRRN.023). */
export function bucketFields(checks: CheckSettingView[]): RuleField<BucketRuleData>[] {
  return [
    priority<BucketRuleData>(),
    {
      key: 'check',
      label: 'Check',
      options: [ANY, ...checks.map((c) => ({ code: c.checkCode, label: c.checkName }))],
      get: (r) => r.checkCode ?? '',
      set: (r, v) => ({ ...r, checkCode: textOrNull(v) }),
    },
    {
      key: 'severity',
      label: 'Severity',
      options: [ANY, ...SEVERITIES],
      get: (r) => r.severity ?? '',
      set: (r, v) => ({ ...r, severity: textOrNull(v) }),
    },
    {
      key: 'outcome',
      label: 'Outcome',
      options: [
        { code: 'FAIL', label: 'Failed' },
        { code: 'WARN', label: 'Warning' },
      ],
      get: (r) => r.outcome,
      set: (r, v) => ({ ...r, outcome: v }),
    },
    {
      key: 'bucket',
      label: 'Classification',
      options: [
        { code: 'CLEAN', label: 'Clean' },
        { code: 'REVIEW', label: 'Review' },
        { code: 'EXCEPTION', label: 'Exception' },
      ],
      get: (r) => r.bucket,
      set: (r, v) => ({ ...r, bucket: v }),
    },
  ];
}

/** A new bucket rule. */
export function blankBucketRule(p: number): BucketRuleData {
  return { priority: p, checkCode: null, severity: null, outcome: 'FAIL', bucket: 'REVIEW' };
}

type Criteria = DecisionRuleData['criteria'];

function criterion(
  key: keyof Criteria,
  label: string,
  options?: { code: string; label: string }[],
): RuleField<DecisionRuleData> {
  const numeric = key === 'daysFrom' || key === 'daysTo';
  return {
    key,
    label,
    options: options ? [ANY, ...options] : undefined,
    numeric,
    get: (r) => {
      const v = r.criteria[key];
      return v === null ? '' : String(v);
    },
    set: (r, v) => {
      let value: string | number | boolean | null = numeric ? numberOrNull(v) : textOrNull(v);
      if (key === 'mortgaged') value = v === '' ? null : v === 'true';
      return { ...r, criteria: { ...r.criteria, [key]: value } };
    },
  };
}

/** Columns of a decision matrix rule (BRRN.031, 034). */
export const MATRIX_FIELDS: RuleField<DecisionRuleData>[] = [
  priority<DecisionRuleData>(),
  criterion('segment', 'Segment'),
  criterion('lineCode', 'Line'),
  criterion('productCode', 'Product'),
  criterion('mortgaged', 'Mortgaged', [
    { code: 'true', label: 'Yes' },
    { code: 'false', label: 'No' },
  ]),
  criterion('bucket', 'Classification', [
    { code: 'CLEAN', label: 'Clean' },
    { code: 'REVIEW', label: 'Review' },
  ]),
  criterion('claims', 'Claims', [
    { code: 'NONE', label: 'None' },
    { code: 'OPEN', label: 'Open' },
    { code: 'PAID', label: 'Paid' },
    { code: 'TOTAL_LOSS', label: 'Total loss' },
  ]),
  criterion('endorsement', 'Endorsement', [
    { code: 'NONE', label: 'None' },
    { code: 'POSTED_IN_TERM', label: 'Posted in term' },
    { code: 'PENDING', label: 'Pending' },
  ]),
  criterion('payment', 'Payment', [
    { code: 'PAID', label: 'Paid' },
    { code: 'OUTSTANDING', label: 'Outstanding' },
    { code: 'DP', label: 'Direct payment' },
  ]),
  criterion('daysFrom', 'Days from'),
  criterion('daysTo', 'Days to'),
  {
    key: 'outcome',
    label: 'Disposition',
    options: DISPOSITION_OPTIONS,
    get: (r) => r.outcome,
    set: (r, v) => ({ ...r, outcome: v }),
  },
  {
    key: 'automation',
    label: 'Automation',
    options: [
      { code: 'AUTO', label: 'Automatic' },
      { code: 'MANUAL', label: 'Proposal to the officer' },
    ],
    get: (r) => r.automation,
    set: (r, v) => ({ ...r, automation: v }),
  },
  {
    key: 'letter',
    label: 'Letter',
    options: [
      { code: '', label: 'None' },
      { code: 'RA', label: 'Renewal Advice' },
      { code: 'NFR', label: 'Not for Renewal' },
      { code: 'NAL', label: 'No Advice Letter' },
    ],
    get: (r) => r.letterHint ?? '',
    set: (r, v) => ({ ...r, letterHint: textOrNull(v) }),
  },
];

/** A new decision rule. */
export function blankDecisionRule(p: number): DecisionRuleData {
  return {
    priority: p,
    criteria: {
      segment: null,
      lineCode: null,
      productCode: null,
      mortgaged: null,
      bucket: null,
      claims: null,
      endorsement: null,
      payment: null,
      daysFrom: null,
      daysTo: null,
    },
    outcome: 'FOR_RENEWAL',
    automation: 'MANUAL',
    letterHint: null,
  };
}
