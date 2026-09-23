import type {
  LayerInput,
  ParticipantInput,
  Treaty,
  TreatyInput,
  TreatyType,
} from '@/api/reinsurance';

/** Treaty editor state and client-side checks (the server re-validates everything). */

export type TreatyForm = TreatyInput & { id?: number };

let keySeq = 0;

/** Unique row key for editable participant / layer rows. */
export function rowKey(): string {
  keySeq += 1;
  return `row-${keySeq}`;
}

export function blankParticipant(): ParticipantInput {
  return { key: rowKey(), reinsurerCode: '', sharePct: 0, commissionPct: 0 };
}

export function blankLayer(): LayerInput {
  return { key: rowKey(), priority: 0, limit: 0, minDepositPremium: 0, reinstatements: 1 };
}

export function blankTreaty(companyId: number, currency: string, year: number): TreatyForm {
  return {
    companyId,
    code: '',
    name: '',
    treatyType: 'QUOTA_SHARE',
    businessLine: '',
    uwYear: year,
    periodFrom: `${year}-01-01`,
    periodTo: `${year}-12-31`,
    currency,
    quotaSharePct: 40,
    levyPct: 0,
    reserveInterestPct: 0,
    lossReservePct: 0,
    participants: [blankParticipant()],
    layers: [],
  };
}

/** Editor state of an existing treaty. */
export function toForm(t: Treaty): TreatyForm {
  return {
    ...t,
    participants: t.participants.map((p) => ({ ...p, key: rowKey() })),
    layers: t.layers.map((l) => ({ ...l, key: rowKey() })),
  };
}

/** Sum of the participants' shares. */
export function totalShare(participants: ParticipantInput[]): number {
  return Math.round(participants.reduce((sum, p) => sum + (p.sharePct || 0), 0) * 1e6) / 1e6;
}

const TYPE_CHECKS: Record<TreatyType, (f: TreatyForm) => string[]> = {
  QUOTA_SHARE: (f) => ((f.quotaSharePct ?? 0) > 0 ? [] : ['Enter the quota share %.']),
  SURPLUS: (f) =>
    (f.retentionLimit ?? 0) > 0 && (f.lines ?? 0) > 0
      ? []
      : ['Enter the retention (one line) and the number of lines.'],
  XOL: (f) => (f.layers.length > 0 ? [] : ['Add at least one layer.']),
};

/** Problems that would make the server reject the treaty; empty when the form looks valid. */
export function treatyProblems(f: TreatyForm): string[] {
  const problems: string[] = [];
  if (f.code.trim() === '' || f.name.trim() === '' || f.businessLine === '') {
    problems.push('Code, name and class are required.');
  }
  const total = totalShare(f.participants);
  if (total !== 100) {
    problems.push(`Participants' shares must total 100 % (now ${total} %).`);
  }
  if (f.participants.some((p) => p.reinsurerCode === '')) {
    problems.push('Select a reinsurer on every participant line.');
  }
  problems.push(...TYPE_CHECKS[f.treatyType](f));
  return problems;
}

function participantBody(p: ParticipantInput): ParticipantInput {
  return {
    reinsurerCode: p.reinsurerCode,
    sharePct: p.sharePct,
    commissionPct: p.commissionPct,
    profitCommissionPct: p.profitCommissionPct,
    premiumReservePct: p.premiumReservePct,
  };
}

function layerBody(l: LayerInput): LayerInput {
  return {
    priority: l.priority,
    limit: l.limit,
    minDepositPremium: l.minDepositPremium,
    reinstatements: l.reinstatements,
  };
}

/** Request body: fields that do not apply to the treaty type are dropped. */
export function toRequest(f: TreatyForm): TreatyInput {
  const type: TreatyType = f.treatyType;
  return {
    companyId: f.companyId,
    code: f.code,
    name: f.name,
    treatyType: type,
    businessLine: f.businessLine,
    uwYear: f.uwYear,
    periodFrom: f.periodFrom,
    periodTo: f.periodTo,
    currency: f.currency,
    quotaSharePct: type === 'QUOTA_SHARE' ? f.quotaSharePct : undefined,
    treatyLimit: type === 'QUOTA_SHARE' ? f.treatyLimit : undefined,
    retentionLimit: type === 'SURPLUS' ? f.retentionLimit : undefined,
    lines: type === 'SURPLUS' ? f.lines : undefined,
    levyPct: f.levyPct,
    reserveInterestPct: f.reserveInterestPct,
    lossReservePct: f.lossReservePct,
    brokerCode: f.brokerCode === '' ? undefined : f.brokerCode,
    participants: f.participants.map(participantBody),
    layers: type === 'XOL' ? f.layers.map(layerBody) : [],
  };
}

type CapacityFacts = Pick<
  Treaty,
  'treatyType' | 'quotaSharePct' | 'retentionLimit' | 'lines' | 'layers'
>;

const CAPACITY_LABELS: Record<TreatyType, (t: CapacityFacts) => string> = {
  QUOTA_SHARE: (t) => `${t.quotaSharePct ?? 0} % quota share`,
  SURPLUS: (t) => `${t.lines ?? 0} lines of ${(t.retentionLimit ?? 0).toLocaleString('en-PH')}`,
  XOL: (t) => `${t.layers.length} layer(s)`,
};

/** One-line description of a treaty's capacity for lists. */
export function capacityLabel(t: CapacityFacts): string {
  return CAPACITY_LABELS[t.treatyType](t);
}
