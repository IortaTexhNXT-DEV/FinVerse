import type { ProposalLine, ProposedLine } from '@/api/submittedProposals';

/** A rate edited on the Create Proposals screen, with its reason. */
export interface RateEdit {
  rate: string;
  reason: string;
}

/** Applies an edited rate to the selected lines, optionally only of one classification or insurer. */
export function applyRate(
  edits: Record<number, RateEdit>,
  lines: ProposedLine[],
  edit: RateEdit,
  only: { vehicleType?: string; insurerCode?: string },
): Record<number, RateEdit> {
  const next = { ...edits };
  lines
    .filter((l) => !only.vehicleType || l.vehicleType === only.vehicleType)
    .filter((l) => !only.insurerCode || l.insurerCode === only.insurerCode)
    .forEach((l) => {
      next[l.policyId] = edit;
    });
  return next;
}

interface Outcome {
  line?: ProposalLine;
  problem?: string;
}

function typedRate(edit: RateEdit | undefined): number | undefined {
  const typed = edit?.rate.trim() ?? '';
  return typed === '' ? undefined : Number(typed);
}

function outOfRange(rate: number): boolean {
  return Number.isNaN(rate) || rate <= 0 || rate > 100;
}

/** The line to generate for a record, or the problem that prevents it. */
function lineOf(l: ProposedLine, edit: RateEdit | undefined): Outcome {
  const rate = typedRate(edit);
  const nominated: Outcome = { line: { policyId: l.policyId, rate: undefined, reason: undefined } };
  if (rate === undefined) {
    return l.nominatedRate === null
      ? { problem: `Policy ${l.sbmNo} has no nominated rate. Enter the rate or remove the policy.` }
      : nominated;
  }
  if (outOfRange(rate)) {
    return { problem: 'The rate must be between 0 and 100 percent.' };
  }
  if (rate === l.nominatedRate) {
    return nominated;
  }
  const reason = edit?.reason.trim() ?? '';
  return reason === ''
    ? { problem: 'Enter the reason for changing the nominated rate.' }
    : { line: { policyId: l.policyId, rate, reason } };
}

/** The lines to generate, or the first problem that prevents it. */
export function generationLines(
  lines: ProposedLine[],
  edits: Record<number, RateEdit>,
): { lines: ProposalLine[]; problem?: string } {
  const out: ProposalLine[] = [];
  for (const l of lines) {
    const outcome = lineOf(l, edits[l.policyId]);
    if (outcome.line === undefined) {
      return { lines: [], problem: outcome.problem };
    }
    out.push(outcome.line);
  }
  return { lines: out };
}

/** The premium at a rate in percent of the sum insured. */
export function premiumAt(sumInsured: number | null, rate: number | null): number | null {
  if (sumInsured === null || rate === null) {
    return null;
  }
  return Math.round(sumInsured * rate) / 100;
}
