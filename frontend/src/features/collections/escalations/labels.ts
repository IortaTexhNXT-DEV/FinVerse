import type { Basis, EscalationStage, Rule, RuleInput, TargetLevel } from './api';

/** Labels, tabs and form checks of the escalation screens (BRCLXN.049/050). */

export type EscalationTab = 'TL' | 'UH' | 'ACTION' | 'RETURNED' | 'RESOLVED';

export const ESCALATION_TABS: readonly {
  id: EscalationTab;
  label: string;
  stages: EscalationStage[];
}[] = [
  { id: 'TL', label: 'With Team Lead', stages: ['WITH_TL', 'RAISED'] },
  { id: 'UH', label: 'With Unit / Section Head', stages: ['WITH_UH'] },
  { id: 'ACTION', label: 'In Action', stages: ['IN_ACTION'] },
  { id: 'RETURNED', label: 'Returned', stages: ['RETURNED'] },
  { id: 'RESOLVED', label: 'Resolved', stages: ['RESOLVED'] },
];

export const LEVEL_LABELS: Record<TargetLevel, string> = {
  TL: 'Team Lead',
  UH: 'Unit Head',
  SECTION_HEAD: 'Section Head',
  USER: 'Designated User',
};

export const BASIS_LABELS: Record<Basis, string> = {
  AGING_FROM_BOOKING: 'Days since booking',
  AGING_FROM_INCEPTION: 'Days since inception',
  NO_COMMITMENT_BY_DAY: 'Days since booking without a promise',
  BROKEN_PROMISES_COUNT: 'Broken promises',
  INSTALLMENT_OVERDUE_DAYS: 'Days an installment is overdue',
  AMOUNT_OVER: 'Outstanding at or above',
};

/** The stages behind a tab. */
export function stagesOf(tab: EscalationTab): EscalationStage[] {
  return ESCALATION_TABS.find((t) => t.id === tab)?.stages ?? [];
}

/** How a rule reads: "Days since booking ≥ 45 → Team Lead". */
export function describeRule(
  rule: Pick<Rule, 'basis' | 'threshold' | 'targetLevel' | 'targetUsername'>,
): string {
  const target = rule.targetUsername ?? LEVEL_LABELS[rule.targetLevel];
  return `${BASIS_LABELS[rule.basis]} ≥ ${rule.threshold} → ${target}`;
}

export interface RuleForm {
  code: string;
  name: string;
  basis: Basis;
  threshold: string;
  segment: string;
  targetLevel: TargetLevel;
  targetUsername: string;
  reasonCode: string;
  slaHours: string;
  effectiveFrom: string;
  effectiveTo: string;
}

/** Field errors of a rule (the code only for a new rule). */
export function ruleFormErrors(
  form: RuleForm,
  isNew: boolean,
): Partial<Record<keyof RuleForm, string>> {
  const errors: Partial<Record<keyof RuleForm, string>> = {};
  if (isNew && form.code.trim() === '') {
    errors.code = 'Enter the rule code';
  }
  if (form.name.trim() === '') {
    errors.name = 'Enter the rule name';
  }
  const threshold = Number(form.threshold);
  if (Number.isNaN(threshold) || threshold <= 0) {
    errors.threshold = 'Enter a positive threshold';
  }
  if (form.targetLevel === 'USER' && form.targetUsername.trim() === '') {
    errors.targetUsername = 'Name the user who receives the escalations';
  }
  if (form.reasonCode === '') {
    errors.reasonCode = 'Choose the escalation reason';
  }
  return { ...errors, ...scheduleErrors(form) };
}

/** Errors of the SLA and the effective dates of a rule form. */
function scheduleErrors(form: RuleForm): Partial<Record<keyof RuleForm, string>> {
  const errors: Partial<Record<keyof RuleForm, string>> = {};
  const sla = Number(form.slaHours);
  if (!Number.isInteger(sla) || sla < 1 || sla > 720) {
    errors.slaHours = 'Enter between 1 and 720 hours';
  }
  if (form.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the first day';
  } else if (form.effectiveTo !== '' && form.effectiveTo < form.effectiveFrom) {
    errors.effectiveTo = 'The rule cannot end before it starts';
  }
  return errors;
}

/** The rule form of an existing rule, or of a new one. */
export function ruleFormOf(rule: Rule | undefined, today: string): RuleForm {
  if (rule === undefined) {
    return {
      code: '',
      name: '',
      basis: 'AGING_FROM_BOOKING',
      threshold: '45',
      segment: '',
      targetLevel: 'TL',
      targetUsername: '',
      reasonCode: '',
      slaHours: '48',
      effectiveFrom: today,
      effectiveTo: '',
    };
  }
  return {
    code: rule.code,
    name: rule.name,
    basis: rule.basis,
    threshold: String(rule.threshold),
    segment: rule.segment ?? '',
    targetLevel: rule.targetLevel,
    targetUsername: rule.targetUsername ?? '',
    reasonCode: rule.reasonCode,
    slaHours: String(rule.slaHours),
    effectiveFrom: rule.effectiveFrom,
    effectiveTo: rule.effectiveTo ?? '',
  };
}

/** The request of a rule form. */
export function ruleInputOf(form: RuleForm, companyId: number): RuleInput {
  const text = (v: string) => (v.trim() === '' ? undefined : v.trim());
  return {
    companyId,
    code: form.code.trim(),
    name: form.name.trim(),
    basis: form.basis,
    threshold: Number(form.threshold),
    segment: text(form.segment),
    targetLevel: form.targetLevel,
    targetUsername: text(form.targetUsername),
    reasonCode: form.reasonCode,
    slaHours: Number(form.slaHours),
    notifyTarget: true,
    effectiveFrom: form.effectiveFrom,
    effectiveTo: text(form.effectiveTo),
  };
}
