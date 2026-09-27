import type { FormField } from './RowForm';
import { SBM_LOV } from '../common/submittedCodes';

/** A setup record kind kept with maker and checker. */
export type Kind = 'limits' | 'insurers' | 'letters' | 'matrix';

/** The fields of a new setup record of each kind. */
export const FORMS: Record<Kind, readonly FormField[]> = {
  limits: [
    { key: 'insurerCode', label: 'Insurer', kind: 'insurer', required: true },
    { key: 'segment', label: 'Segment', kind: 'lov', lov: SBM_LOV.segment },
    { key: 'maxSumInsured', label: 'Maximum Sum Insured', kind: 'number' },
    { key: 'maxVehicleAge', label: 'Maximum Vehicle Age', kind: 'number' },
    { key: 'description', label: 'Description', kind: 'text' },
  ],
  insurers: [
    { key: 'segment', label: 'Segment', kind: 'lov', lov: SBM_LOV.segment, required: true },
    { key: 'vehicleType', label: 'Vehicle Type', kind: 'text' },
    { key: 'occupancy', label: 'Occupancy', kind: 'text' },
    { key: 'insurerCode', label: 'Insurer', kind: 'insurer', required: true },
    { key: 'priority', label: 'Priority', kind: 'number', required: true },
    { key: 'excludeExpiring', label: 'Never the Expiring Insurer', kind: 'bool' },
    { key: 'description', label: 'Description', kind: 'text' },
  ],
  letters: [
    { key: 'letterType', label: 'Letter', kind: 'lov', lov: SBM_LOV.letterType, required: true },
    { key: 'segment', label: 'Segment', kind: 'lov', lov: SBM_LOV.segment },
    { key: 'daysFromExpiry', label: 'Days Before Expiry', kind: 'number', required: true },
    {
      key: 'channel',
      label: 'Channel',
      kind: 'choice',
      choices: { EMAIL: 'E-mail', PRINT: 'Print', BANK_COUNTERPART: 'Bank counterpart' },
      required: true,
    },
    {
      key: 'templateCode',
      label: 'Template',
      kind: 'choice',
      choices: {
        SBM_REMINDER: 'Reminder',
        SBM_RENEWAL_NOTICE: 'Renewal notice',
        SBM_RENEWAL_PROPOSAL: 'Renewal proposal',
      },
      required: true,
    },
    { key: 'description', label: 'Description', kind: 'text' },
  ],
  matrix: [
    {
      key: 'document',
      label: 'Document',
      kind: 'choice',
      choices: { IAAF: 'IAAF', TOR: 'TOR' },
      required: true,
    },
    { key: 'segment', label: 'Segment', kind: 'lov', lov: SBM_LOV.segment },
    { key: 'tsiFrom', label: 'Sum Insured From', kind: 'number', required: true },
    { key: 'tsiTo', label: 'Sum Insured To', kind: 'number' },
    { key: 'level', label: 'Level', kind: 'number', required: true },
    {
      key: 'permission',
      label: 'Approver Permission',
      kind: 'choice',
      choices: { IAAF_APPROVE: 'IAAF approver', TOR_APPROVE: 'TOR approver' },
      required: true,
    },
    { key: 'signatoryTitle', label: 'Signatory Title', kind: 'text', required: true },
  ],
};
