import type { WorkTile } from '@/components/broking/WorkTiles';
import type { CaseSearchParams, CaseStage, CaseTab, CaseTiles, TemplateField } from './api';

/**
 * Pure helpers of the screening case screens (SNSRP-402, 403, 405, 501; FR-SS-041 to 044, 050):
 * tabs, the filters kept in the address, the search and date checks, the stage and SLA labels and
 * the review form checks.
 */

export const CASE_TABS: readonly { id: CaseTab; label: string }[] = [
  { id: 'MY', label: 'My Cases' },
  { id: 'TEAM', label: 'Team' },
  { id: 'APPROVAL', label: 'For Approval' },
  { id: 'COMMITTEE', label: 'Committee' },
  { id: 'STR', label: 'STR' },
  { id: 'CLOSED', label: 'Closed' },
  { id: 'ALL', label: 'All' },
];

export const CASE_STAGES: readonly CaseStage[] = [
  'NEW',
  'INVESTIGATION',
  'RETURNED',
  'UNIT_HEAD_APPROVAL',
  'COMPLIANCE_REVIEW',
  'AML_COMMITTEE',
  'STR_PREPARATION',
  'STR_EXTRACTION',
  'CLOSED',
];

export const CASE_TYPES = [
  'NAME_MATCH',
  'PEP',
  'HIGH_RISK',
  'EDD',
  'MONITOR',
  'ACCOUNT_APPLICATION',
] as const;

export const SLA_STATES = ['ON_TIME', 'DUE_SOON', 'BREACHED'] as const;

/** Shortest search text (FR-SS-042 R1). */
export const MIN_SEARCH = 3;

/** The filters of the case list that live in the address (FR-SS-041 R2). */
export const FILTER_KEYS = [
  'q',
  'stage',
  'caseType',
  'riskCategory',
  'marketingUnit',
  'unitHead',
  'disposition',
  'assignee',
  'sla',
  'createdFrom',
  'createdTo',
] as const;

export type FilterKey = (typeof FILTER_KEYS)[number];
export type CaseFilters = Partial<Record<FilterKey, string>>;

/** Reads the tab of the address; ALL when missing or unknown. */
export function tabOf(value: string | null): CaseTab {
  return CASE_TABS.some((t) => t.id === value) ? (value as CaseTab) : 'ALL';
}

/** Reads the filters of the address. */
export function filtersOf(search: URLSearchParams): CaseFilters {
  const filters: CaseFilters = {};
  FILTER_KEYS.forEach((key) => {
    const value = search.get(key);
    if (value) {
      filters[key] = value;
    }
  });
  return filters;
}

/** Writes the tab and filters to the address (empty values are left out). */
export function toSearch(tab: CaseTab, filters: CaseFilters): URLSearchParams {
  const search = new URLSearchParams();
  if (tab !== 'ALL') {
    search.set('tab', tab);
  }
  FILTER_KEYS.forEach((key) => {
    const value = filters[key]?.trim();
    if (value) {
      search.set(key, value);
    }
  });
  return search;
}

/** The search request of the list. */
export function searchParams(
  companyId: number,
  tab: CaseTab,
  filters: CaseFilters,
  page: number,
): CaseSearchParams {
  return { companyId, tab, page, ...filters };
}

/** "Enter at least 3 characters" for a shorter search text (FR-SS-042). */
export function searchError(q: string | undefined): string | undefined {
  const text = q?.trim() ?? '';
  return text.length > 0 && text.length < MIN_SEARCH ? 'Enter at least 3 characters' : undefined;
}

/** "The end date must be on or after the start date" for a reversed range (FR-SS-041). */
export function periodError(from?: string, to?: string): string | undefined {
  return from && to && to < from ? 'The end date must be on or after the start date' : undefined;
}

const STAGE_LABELS: Record<CaseStage, string> = {
  NEW: 'New',
  INVESTIGATION: 'Investigation',
  RETURNED: 'Returned',
  UNIT_HEAD_APPROVAL: 'Unit Head Approval',
  COMPLIANCE_REVIEW: 'Compliance Review',
  AML_COMMITTEE: 'AML Committee',
  STR_PREPARATION: 'STR Preparation',
  STR_EXTRACTION: 'STR Extraction',
  CLOSED: 'Closed',
};

/** The label of a stage. */
export function stageLabel(stage: string): string {
  return stage in STAGE_LABELS ? STAGE_LABELS[stage as CaseStage] : stage;
}

/** The SLA badge text (FR-SS-044: on time, due soon, breached). */
export function slaLabel(state: string): string {
  switch (state) {
    case 'ON_TIME':
      return 'On Time';
    case 'DUE_SOON':
      return 'Due Soon';
    case 'BREACHED':
      return 'Breached';
    default:
      return '—';
  }
}

/** The dispositions offered in a stage (LOV SCR_DISPOSITION, parent = stage). */
export function dispositionParent(stage: CaseStage): string {
  return stage === 'RETURNED' ? 'INVESTIGATION' : stage;
}

/** Groups template fields by section, in order. */
export function sections(fields: TemplateField[]): { section: string; fields: TemplateField[] }[] {
  const groups: { section: string; fields: TemplateField[] }[] = [];
  fields.forEach((field) => {
    const last = groups[groups.length - 1];
    if (last?.section === field.section) {
      last.fields.push(field);
    } else {
      groups.push({ section: field.section, fields: [field] });
    }
  });
  return groups;
}

const NUMBER = /^-?\d+(\.\d+)?$/;

/** The error of one review value, as the server words it (FR-SS-050). */
export function fieldError(field: TemplateField, raw: string | undefined): string | undefined {
  const value = raw?.trim() ?? '';
  if (value === '') {
    return undefined;
  }
  const numeric = field.dataType === 'NUMBER' || field.dataType === 'AMOUNT';
  if (numeric && !NUMBER.test(value.replaceAll(',', ''))) {
    return `${field.label} must be ${field.dataType === 'AMOUNT' ? 'an amount' : 'a number'}`;
  }
  if (field.dataType === 'DATE' && !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return `${field.label} must be a date`;
  }
  return undefined;
}

/** The mandatory fields left blank ("<Field> is required"). */
export function missingFields(
  fields: TemplateField[],
  values: Record<string, string>,
): Record<string, string> {
  const missing: Record<string, string> = {};
  fields
    .filter((f) => f.mandatory)
    .forEach((f) => {
      const value = values[f.code]?.trim() ?? '';
      const blank = f.dataType === 'CHECKBOX' ? value.toUpperCase() !== 'TRUE' : value === '';
      if (blank) {
        missing[f.code] = `${f.label} is required`;
      }
    });
  return missing;
}

/** The upload dialog of a case document (FR-SS-052). */
export interface UploadForm {
  file?: File;
  formType: string;
  documentType: string;
  dateReceived: string;
  source: string;
}

/** The FRS messages of the upload dialog (FR-SS-052). */
export function uploadErrors(
  form: UploadForm,
  now: string,
): Partial<Record<keyof UploadForm, string>> {
  const errors: Partial<Record<keyof UploadForm, string>> = {};
  if (form.file === undefined) {
    errors.file = 'Choose the file';
  }
  if (form.formType === '') {
    errors.formType = 'Enter the form type of the document';
  }
  if (form.documentType === '') {
    errors.documentType = 'Enter the document type of the document';
  }
  if (form.dateReceived === '') {
    errors.dateReceived = 'Enter the date received of the document';
  } else if (form.dateReceived > now) {
    errors.dateReceived = 'The date received cannot be in the future';
  }
  if (form.source.trim() === '') {
    errors.source = 'Enter the source of the document';
  }
  return errors;
}

/** The tiles of the screening home: open cases per stage, then SLA and matches (SNSRP-402, 405). */
export function homeTiles(tiles: CaseTiles, go: (to: string) => void): WorkTile[] {
  const stages: WorkTile[] = CASE_STAGES.filter((s) => s !== 'CLOSED').map((stage) => ({
    key: stage,
    label: stageLabel(stage),
    value: tiles.openByStage[stage] ?? 0,
    onClick: () => go(`/screening/cases?stage=${stage}`),
  }));
  return [
    ...stages,
    {
      key: 'due',
      label: 'Due Today',
      value: tiles.dueToday,
      alert: true,
      onClick: () => go('/screening/cases?sla=DUE_SOON'),
    },
    {
      key: 'breached',
      label: 'SLA Breached',
      value: tiles.breached,
      alert: true,
      onClick: () => go('/screening/cases?sla=BREACHED'),
    },
    {
      key: 'matches',
      label: 'Potential Matches',
      value: tiles.potentialMatches,
      onClick: () => go('/screening/matches'),
    },
  ];
}
