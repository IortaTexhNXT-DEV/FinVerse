import type { Alias, EntryRequest, EntryValues, WatchlistEntry } from './api';

/** Pure helpers of the watchlist screens (SNSRP-203, 204). */

const LABELS: readonly [keyof EntryValues, string][] = [
  ['listType', 'List Type'],
  ['entityType', 'Entity Type'],
  ['primaryName', 'Primary Name'],
  ['firstName', 'First Name'],
  ['lastName', 'Last Name'],
  ['birthDate', 'Birth Date'],
  ['nationality', 'Nationality'],
  ['idNumbers', 'ID Numbers'],
  ['listedOn', 'Listed On'],
  ['delistedOn', 'Delisted On'],
  ['aliases', 'Aliases'],
];

export interface ValueLine {
  field: string;
  before: string;
  after: string;
  changed: boolean;
}

/** Aliases as one line ("A; B"). */
export function aliasesText(aliases: readonly Alias[] | undefined): string {
  return (aliases ?? []).map((a) => a.name).join('; ');
}

/** Aliases typed as "A; B" (AKA). */
export function parseAliases(text: string): Alias[] {
  return text
    .split(';')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .map((name) => ({ name, type: 'AKA' }));
}

function valueText(values: EntryValues | null | undefined, key: keyof EntryValues): string {
  if (values === null || values === undefined) {
    return '';
  }
  const v = values[key];
  if (Array.isArray(v)) {
    return aliasesText(v);
  }
  return typeof v === 'string' ? v : '';
}

/** Before / after lines of a change, for the checker's comparison (FR-SS-023). */
export function compareValues(
  before: EntryValues | null | undefined,
  after: EntryValues,
): ValueLine[] {
  return LABELS.map(([key, field]) => {
    const b = valueText(before, key);
    const a = valueText(after, key);
    return { field, before: b, after: a, changed: b !== a };
  }).filter((line) => line.before !== '' || line.after !== '');
}

/** The editable request of an entry (Change) or an empty one (Add Entry). */
export function requestOf(entry?: WatchlistEntry, aliases?: readonly Alias[]): EntryRequest {
  if (entry === undefined) {
    return {
      sourceCode: 'INTERNAL',
      listType: 'INTERNAL',
      entityType: 'INDIVIDUAL',
      aliases: [],
      remarks: '',
    };
  }
  return {
    listType: entry.listType,
    entityType: entry.entityType,
    primaryName: entry.primaryName,
    firstName: entry.firstName,
    lastName: entry.lastName,
    birthDate: entry.birthDate,
    nationality: entry.nationality,
    idNumbers: entry.idNumbers,
    listedOn: entry.listedOn,
    delistedOn: entry.delistedOn,
    aliases: [...(aliases ?? [])],
    remarks: '',
  };
}

/** Field errors of a manual entry before it is sent (the FRS messages). */
export function entryErrors(request: EntryRequest): Record<string, string> {
  const errors: Record<string, string> = {};
  if ((request.primaryName ?? '').trim() === '') {
    errors.primaryName = 'Enter the name of the listed person or entity';
  }
  if ((request.listType ?? '') === '') {
    errors.listType = 'Select the list type';
  }
  if (request.remarks.trim() === '') {
    errors.remarks = 'Enter the reason for the change';
  }
  return errors;
}
