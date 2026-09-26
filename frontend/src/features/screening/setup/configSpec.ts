import type {
  ConfigContent,
  ConfigRow,
  ConfigType,
  ConfigVersion,
  LayoutContent,
  TemplateContent,
} from './api';

/**
 * Editor specification of the screening configuration types (FR-SS-011 to 017): the tables of each
 * type, their columns and blank rows, plus the pure helpers the configuration screens use.
 */

export type FieldKind = 'text' | 'number' | 'decimal' | 'select' | 'lov' | 'check' | 'codes';

export interface FieldSpec {
  key: string;
  label: string;
  kind: FieldKind;
  options?: readonly string[];
  lovType?: string;
  hint?: string;
}

export type TableKey =
  | 'matchRules'
  | 'riskCategories'
  | 'riskRules'
  | 'routes'
  | 'assignmentRules'
  | 'slaRules'
  | 'validationRules'
  | 'template.fields'
  | 'layout.columns';

export interface TableSpec {
  key: TableKey;
  title: string;
  fields: FieldSpec[];
  blank: ConfigRow;
}

export interface TypeSpec {
  type: ConfigType;
  label: string;
  description: string;
  tables: TableSpec[];
}

export const CASE_STAGES = [
  'INVESTIGATION',
  'RETURNED',
  'UNIT_HEAD_APPROVAL',
  'COMPLIANCE_REVIEW',
  'AML_COMMITTEE',
  'STR_PREPARATION',
  'STR_EXTRACTION',
  'CLOSED',
] as const;

export const TEMPLATE_TYPES = ['KYC_REVIEW', 'TRANSACTION_REVIEW', 'EDD', 'STR'] as const;

/** Rows of a table of the content. */
export function rowsOf(content: ConfigContent | undefined, key: TableKey): ConfigRow[] {
  if (content === undefined) {
    return [];
  }
  if (key === 'template.fields') {
    return content.template?.fields ?? [];
  }
  if (key === 'layout.columns') {
    return content.layout?.columns ?? [];
  }
  return content[key];
}

const DEFAULT_LAYOUT: LayoutContent = {
  format: 'CSV',
  delimiter: ',',
  encoding: 'UTF-8',
  columns: [],
};

/** The content with the rows of one table replaced. */
export function withRows(content: ConfigContent, key: TableKey, rows: ConfigRow[]): ConfigContent {
  if (key === 'template.fields') {
    const template: TemplateContent = content.template ?? {
      templateType: '',
      name: '',
      fields: [],
    };
    return { ...content, template: { ...template, fields: rows } };
  }
  if (key === 'layout.columns') {
    return { ...content, layout: { ...(content.layout ?? DEFAULT_LAYOUT), columns: rows } };
  }
  return { ...content, [key]: rows };
}

/** Comma-separated codes to a list (upper-cased, blanks dropped). */
export function toCodes(text: string): string[] {
  return text
    .split(',')
    .map((c) => c.trim().toUpperCase())
    .filter((c) => c.length > 0);
}

/** A cell value as editable text. */
export function cellText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  if (Array.isArray(value)) {
    return value.join(', ');
  }
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return JSON.stringify(value);
}

/** The typed value of an edited cell. */
export function parseCell(kind: FieldKind, text: string): unknown {
  if (kind === 'codes') {
    return toCodes(text);
  }
  if (kind === 'number' || kind === 'decimal') {
    return text.trim() === '' ? null : Number(text);
  }
  return text.trim() === '' ? null : text;
}

/** The version in force on a date: the ACTIVE / SUPERSEDED one with the latest effective date. */
export function inForce(
  versions: readonly ConfigVersion[],
  date: string,
  scope?: string,
): ConfigVersion | undefined {
  return versions
    .filter((v) => (v.scope ?? undefined) === scope)
    .filter((v) => (v.status === 'ACTIVE' || v.status === 'SUPERSEDED') && v.effectiveFrom <= date)
    .sort((a, b) => b.effectiveFrom.localeCompare(a.effectiveFrom) || b.versionNo - a.versionNo)[0];
}

/** The version a screen opens first: the open draft or pending version, else the one in force. */
export function defaultVersion(
  versions: readonly ConfigVersion[],
  date: string,
  scope?: string,
): ConfigVersion | undefined {
  const own = versions.filter((v) => (v.scope ?? undefined) === scope);
  return (
    own.find((v) => v.status === 'DRAFT' || v.status === 'PENDING') ??
    inForce(own, date, scope) ??
    own[0]
  );
}

export interface VersionRoles {
  /** A draft the user may edit (SCR_CONFIG_MAINTAIN). */
  editable: boolean;
  /** The user created or submitted the version. */
  isMaker: boolean;
  /** A pending version the user may approve or reject: a checker who is not its maker. */
  mayDecide: boolean;
}

/** What the user may do with a version (FR-SS-010, 019: the maker never decides). */
export function versionRoles(
  version: ConfigVersion,
  username: string | undefined,
  can: (permission: string) => boolean,
): VersionRoles {
  const isMaker =
    username !== undefined && [version.createdBy, version.submittedBy].includes(username);
  return {
    editable: version.status === 'DRAFT' && can('SCR_CONFIG_MAINTAIN'),
    isMaker,
    mayDecide: version.status === 'PENDING' && can('SCR_CONFIG_APPROVE') && !isMaker,
  };
}

/** Client-side key of an edited row (never sent to the server). */
export const ROW_KEY = '__key';

let rowCounter = 0;

/** A new client-side row key. */
export function newRowKey(): string {
  rowCounter += 1;
  return `r${rowCounter}`;
}

/** Rows with client-side keys, so edited inputs stay with their row. */
export function keyed(rows: readonly ConfigRow[]): ConfigRow[] {
  return rows.map((r) => ({ ...r, [ROW_KEY]: newRowKey() }));
}

/** Rows without the client-side keys. */
export function unkeyed(rows: readonly ConfigRow[]): ConfigRow[] {
  return rows.map((r) => Object.fromEntries(Object.entries(r).filter(([k]) => k !== ROW_KEY)));
}

const TABLE_KEYS: readonly TableKey[] = [
  'matchRules',
  'riskCategories',
  'riskRules',
  'routes',
  'assignmentRules',
  'slaRules',
  'validationRules',
  'template.fields',
  'layout.columns',
];

/** Content with every table keyed (on load) or unkeyed (before save). */
export function mapTables(
  content: ConfigContent,
  map: (rows: readonly ConfigRow[]) => ConfigRow[],
): ConfigContent {
  return TABLE_KEYS.reduce((c, key) => {
    const rows = rowsOf(c, key);
    return rows.length === 0 ? c : withRows(c, key, map(rows));
  }, content);
}
