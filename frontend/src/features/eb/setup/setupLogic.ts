import type {
  RequiredDocument,
  RequiredDocumentInput,
  ThresholdRule,
  ThresholdRuleInput,
} from '@/api/ebMarket';

/** The errors of a threshold rule; empty when it may be saved. */
export function ruleErrors(input: ThresholdRuleInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (input.measure === '') {
    errors.measure = 'Select what is measured';
  }
  if (input.amount === '' || input.amount <= 0) {
    errors.amount = 'Enter an amount greater than zero';
  }
  if (input.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the effective date';
  } else if (input.effectiveTo && input.effectiveTo < input.effectiveFrom) {
    errors.effectiveFrom = 'The end date cannot be before the start date';
  }
  return errors;
}

const NEW_RULE: ThresholdRuleInput = {
  benefitLine: '',
  measure: '',
  amount: '',
  currency: 'PHP',
  approverPermission: 'EB_THRESHOLD_APPROVE',
  approvalLevel: 1,
  effectiveFrom: '',
  effectiveTo: '',
  description: '',
};

/** The form of a rule: a new rule, or the rule being edited. */
export function ruleInput(rule: ThresholdRule | undefined): ThresholdRuleInput {
  if (rule === undefined) {
    return NEW_RULE;
  }
  return {
    ...rule,
    benefitLine: rule.benefitLine ?? '',
    effectiveTo: rule.effectiveTo ?? '',
    description: rule.description ?? '',
  };
}

/** The form of a required document: a new one, or the one being edited. */
export function docInput(doc: RequiredDocument | undefined): RequiredDocumentInput {
  if (doc === undefined) {
    return { processType: '', benefitLine: '', documentType: '', mandatory: true };
  }
  return { ...doc, benefitLine: doc.benefitLine ?? '' };
}
