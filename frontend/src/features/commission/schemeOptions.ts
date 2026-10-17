import { clientShortName } from '@/context/clientNames';
import type { Beneficiary, Calculation, PeriodType, SchemeType } from './commissionApi';

/** The choices of the incentive scheme form, in words. */
export const TYPES: readonly { id: SchemeType; label: string }[] = [
  { id: 'NO_TOUCH', label: 'No Touch' },
  { id: 'TOP_UP', label: 'Top Up' },
  { id: 'MOTOR_MANIA', label: 'Motor Mania' },
  { id: 'OTHER', label: 'Other' },
];
export const CALCULATIONS: readonly { id: Calculation; label: string }[] = [
  { id: 'TARGET_TIERED', label: 'Production Target Tiers' },
  { id: 'FIXED_PER_POLICY', label: 'Fixed Amount per Policy' },
];
export const PERIODS: readonly { id: PeriodType; label: string }[] = [
  { id: 'MONTHLY', label: 'Monthly' },
  { id: 'QUARTERLY', label: 'Quarterly' },
  { id: 'SEMI_ANNUAL', label: 'Semi-annual' },
  { id: 'ANNUAL', label: 'Annual' },
  { id: 'CUSTOM', label: 'Custom' },
];
export const BENEFICIARIES: readonly { id: Beneficiary; label: string }[] = [
  {
    id: 'BROKER',
    get label() {
      return clientShortName();
    },
  },
  { id: 'BRANCH', label: 'Branch (Passed On)' },
];

/** The label of a choice of the scheme form. */
export function optionLabel(options: readonly { id: string; label: string }[], id: string): string {
  return options.find((o) => o.id === id)?.label ?? id;
}
