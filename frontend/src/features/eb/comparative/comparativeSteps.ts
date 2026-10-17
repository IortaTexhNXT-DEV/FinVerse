import type { ComparativeView } from '@/api/ebMarket';

export type Step = 'submit' | 'sign-off' | 'threshold-approve' | 'return' | 'present';

export const STEPS: Record<
  Step,
  { label: string; effect: string; reason?: 'required' | 'optional' }
> = {
  submit: {
    label: 'Submit for Sign-off',
    effect: 'The team lead is asked to sign off the recommendation.',
  },
  'sign-off': {
    label: 'Sign Off',
    effect: 'The recommendation is signed off; above the value threshold it goes to Management.',
    reason: 'optional',
  },
  'threshold-approve': {
    label: 'Approve Above Threshold',
    effect: 'The comparative is approved above the value threshold and is ready to present.',
    reason: 'optional',
  },
  return: {
    label: 'Return to AO',
    effect: 'The comparative goes back to the AO as a draft.',
    reason: 'required',
  },
  present: {
    label: 'Present to Client',
    effect: 'The comparative is sent to the client and the cycle waits for the confirmation.',
  },
};

/** The steps the user may take on the comparative in its status. */
export function stepsFor(view: ComparativeView, can: (p: string) => boolean): Step[] {
  const status = view.comparative.status;
  const market = can('EB_MARKET');
  const steps: Step[] = [];
  if (market && status === 'DRAFT') {
    steps.push('submit');
  }
  if (can('EB_COMPARATIVE_APPROVE') && status === 'FOR_APPROVAL') {
    steps.push('sign-off', 'return');
  }
  if (status === 'THRESHOLD_APPROVAL' && can(view.approverPermission ?? 'EB_THRESHOLD_APPROVE')) {
    steps.push('threshold-approve', 'return');
  }
  if (market && status === 'APPROVED') {
    steps.push('present');
  }
  return steps;
}

/** The recommendation per benefit line as saved on the comparative. */
export function savedRecommendation(view: ComparativeView): Record<string, number> {
  return Object.fromEntries(
    view.lines.flatMap((l) =>
      l.recommendedProposalId === null || l.recommendedProposalId === undefined
        ? []
        : [[l.benefitLine, l.recommendedProposalId]],
    ),
  );
}
