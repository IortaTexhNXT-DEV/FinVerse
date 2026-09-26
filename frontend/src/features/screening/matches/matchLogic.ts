import { formatDate, humanize } from '@/utils/format';
import type { MatchDetail, MatchSide, MatchStatus, ScreeningMatch, ScreeningTrigger } from './api';

/** Status tabs of the Matches screen (FR-SS-032): undecided matches first. */
export type MatchTab = MatchStatus | 'ALL';

export const MATCH_TABS: readonly { id: MatchTab; label: string }[] = [
  { id: 'POTENTIAL', label: 'Potential' },
  { id: 'TRUE_MATCH', label: 'True Matches' },
  { id: 'FALSE_POSITIVE', label: 'False Positives' },
  { id: 'ALL', label: 'All' },
];

export const LIST_TYPES = ['SANCTION', 'PEP', 'INTERNAL', 'ADVERSE_MEDIA'] as const;

export const TRIGGERS: readonly ScreeningTrigger[] = [
  'CLIENT_REGISTERED',
  'CLIENT_CHANGED',
  'ACCOUNT_SUBMITTED',
  'LIST_CHANGE',
  'PERIODIC',
  'MANUAL',
];

/** Longest justification the backend accepts (FR-SS-035). */
export const MAX_JUSTIFICATION = 2000;

/** Score 0-1 as "0.9500 (95%)". */
export function scoreText(score: number): string {
  return `${score.toFixed(4)} (${Math.round(score * 100)}%)`;
}

/** Algorithm and matched fields as one line: "Phonetic · Name, Birth Date". */
export function matchedText(m: Pick<ScreeningMatch, 'algorithm' | 'matchedFields'>): string {
  const fields = m.matchedFields.map(humanize).join(', ');
  return fields ? `${humanize(m.algorithm)} · ${fields}` : humanize(m.algorithm);
}

/** A score filter value: blank or a number from 0 to 1. */
export function scoreError(value: string): string | undefined {
  if (value.trim() === '') {
    return undefined;
  }
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 && n <= 1 ? undefined : 'Enter a score from 0 to 1';
}

/** Whether a match can still be decided from the Matches screen. */
export function isOpen(m: Pick<ScreeningMatch, 'status' | 'caseId'>): boolean {
  return m.status === 'POTENTIAL' && (m.caseId === undefined || m.caseId === null);
}

export interface FalsePositiveForm {
  justification: string;
  riskRating: string;
  removeWatchlistTag: boolean;
}

/** Field errors of "Mark False Positive" (FR-SS-032 / 035 messages). */
export function falsePositiveErrors(
  form: FalsePositiveForm,
  evidenceCount: number,
): Partial<Record<'justification' | 'evidence', string>> {
  const errors: Partial<Record<'justification' | 'evidence', string>> = {};
  if (form.justification.trim() === '') {
    errors.justification = 'Enter the justification and attach the evidence';
  } else if (form.justification.length > MAX_JUSTIFICATION) {
    errors.justification = 'The justification is limited to 2000 characters';
  }
  if (evidenceCount < 1) {
    errors.evidence = 'Attach at least one evidence document';
  }
  return errors;
}

/** The request body of "Mark False Positive". */
export function falsePositiveRequest(form: FalsePositiveForm) {
  const changesProfile = form.riskRating !== '' || form.removeWatchlistTag;
  return {
    justification: form.justification.trim(),
    riskRating: changesProfile && form.riskRating !== '' ? form.riskRating : undefined,
    removeTags: form.removeWatchlistTag ? ['WATCHLIST_REVIEW'] : undefined,
  };
}

export interface ComparisonRow {
  label: string;
  client: string;
  entry: string;
  /** True when both sides are known and equal (highlighted), false when they differ. */
  same?: boolean;
}

function text(value: string | number | undefined | null): string {
  return value === undefined || value === null || value === '' ? '—' : String(value);
}

function compare(label: string, client?: string | null, entry?: string | null): ComparisonRow {
  const known = Boolean(client) && Boolean(entry);
  return {
    label,
    client: text(client),
    entry: text(entry),
    same: known ? client?.trim().toUpperCase() === entry?.trim().toUpperCase() : undefined,
  };
}

const EMPTY_SIDE: MatchSide = { name: '', reference: '', type: '', status: '', aliases: [] };

/** The side-by-side rows of a match (FR-SS-032 "compares the client and the entry"). */
export function comparisonRows(detail: MatchDetail): ComparisonRow[] {
  const c = detail.client;
  const e = detail.entry ?? EMPTY_SIDE;
  return [
    { label: 'Name', client: text(c.name), entry: text(e.name) },
    { label: 'Reference', client: text(c.reference), entry: text(e.reference) },
    { label: 'Type', client: humanize(c.type), entry: e.type ? humanize(e.type) : '—' },
    compare('Birth Date', formatDate(c.birthDate) || null, formatDate(e.birthDate) || null),
    compare('Nationality', c.nationality, e.nationality),
    { label: 'TIN / ID Numbers', client: text(c.ids), entry: text(e.ids) },
    { label: 'Aliases', client: '—', entry: e.aliases.length > 0 ? e.aliases.join('; ') : '—' },
    { label: 'List', client: '—', entry: text(e.list) },
    { label: 'Status', client: humanize(c.status), entry: e.status ? humanize(e.status) : '—' },
  ];
}

/** Whether the list entry changed since the match was recorded (a new version is matched again). */
export function entryChanged(detail: MatchDetail): boolean {
  const current = detail.entry?.entryVersion;
  return current !== undefined && current !== null && current !== detail.match.entryVersion;
}
