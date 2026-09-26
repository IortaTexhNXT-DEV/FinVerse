import type { Share } from '../cover/api';
import type { NewInsurerLine } from '../insurer/api';
import type { LineDraft } from '../insurer/InsurerLinesEditor';
import type { LocationPick } from '../location/api';
import type { LossInput, RecordClaimInput } from './api';
import type { ClaimForm } from './recordLogic';
import { toAmount } from './recordLogic';

/** Builds the requests of the claim screens from what the user typed. */

function text(value: string): string | undefined {
  const t = value.trim();
  return t === '' ? undefined : t;
}

/** The insurer lines proposed from the invoice shares of the cover. */
export function linesFromShares(shares: Share[]): LineDraft[] {
  return shares.map((s) => ({
    insurerCode: s.insurerCode,
    sharePct: String(s.sharePct),
    insurerClaimNo: '',
    reportedToInsurerOn: '',
    reserveAmount: '',
  }));
}

/** An insurer line request. */
export function toLine(l: LineDraft): NewInsurerLine {
  return {
    insurerCode: l.insurerCode,
    sharePct: toAmount(l.sharePct),
    insurerClaimNo: text(l.insurerClaimNo),
    reportedToInsurerOn: text(l.reportedToInsurerOn),
    reserveAmount: toAmount(l.reserveAmount),
  };
}

/** The loss data of a form. */
export function toLoss(form: ClaimForm): LossInput {
  return {
    lossDate: form.lossDate,
    reportedDate: form.reportedDate,
    lossNature: form.lossNature,
    claimType: form.claimType,
    lossDescription: form.lossDescription.trim(),
    lossPlace: text(form.lossPlace),
    catastropheCode: text(form.catastropheCode),
    catastropheEvent: text(form.catastropheEvent),
    claimAmount: toAmount(form.claimAmount),
    deductible: toAmount(form.deductible),
    initialReserve: toAmount(form.initialReserve),
  };
}

/** The Record Claim request. */
export function toRecordInput(
  companyId: number,
  policyYear: number,
  form: ClaimForm,
  picks: LocationPick[],
  lines: LineDraft[],
  confirm: { outsidePeriod: boolean; reuse: boolean },
): RecordClaimInput {
  return {
    companyId,
    arn: form.arn,
    policyYear,
    source: form.source,
    loss: toLoss(form),
    locations: picks.map((p) => ({ itemNo: p.itemNo, description: text(p.description ?? '') })),
    insurers: lines.map(toLine),
    confirmOutsidePeriod: confirm.outsidePeriod,
    confirmReuse: confirm.reuse,
  };
}
