import type { ConfigType } from './api';
import { CASE_STAGES } from './configSpec';
import type { FieldSpec, TableSpec, TypeSpec } from './configSpec';

/**
 * The tables and columns of each screening configuration type (FR-SS-011 to 017), for the rows
 * editor of Configuration Versions and Templates.
 */

const CASE_TYPE: FieldSpec = {
  key: 'caseType',
  label: 'Case Type',
  kind: 'lov',
  lovType: 'SCR_CASE_TYPE',
};
const RISK_CATEGORY: FieldSpec = { key: 'riskCategory', label: 'Risk Category', kind: 'text' };
const UNIT: FieldSpec = { key: 'marketingUnit', label: 'Marketing Unit', kind: 'text' };
const ORDER: FieldSpec = { key: 'order', label: 'Order', kind: 'number' };
const STAGE: FieldSpec = { key: 'stage', label: 'Stage', kind: 'select', options: CASE_STAGES };

const MATCH: TableSpec = {
  key: 'matchRules',
  title: 'Matching Rules',
  fields: [
    { key: 'listType', label: 'List Type', kind: 'lov', lovType: 'SCR_LIST_TYPE' },
    { key: 'subjectType', label: 'Subject', kind: 'select', options: ['INDIVIDUAL', 'ENTITY'] },
    {
      key: 'algorithm',
      label: 'Algorithm',
      kind: 'select',
      options: ['EXACT', 'PHONETIC', 'FUZZY'],
    },
    { key: 'threshold', label: 'Threshold', kind: 'decimal', hint: '0 to 1' },
    {
      key: 'fields',
      label: 'Fields Compared',
      kind: 'codes',
      hint: 'NAME, ALIAS, BIRTH_DATE, NATIONALITY, ID',
    },
    {
      key: 'minScoreForCase',
      label: 'Case Threshold',
      kind: 'decimal',
      hint: 'Not below the threshold',
    },
  ],
  blank: {
    listType: 'SANCTION',
    subjectType: 'INDIVIDUAL',
    algorithm: 'FUZZY',
    threshold: 0.85,
    fields: ['NAME', 'ALIAS'],
    minScoreForCase: 0.9,
  },
};

const CATEGORIES: TableSpec = {
  key: 'riskCategories',
  title: 'Risk Categories',
  fields: [
    { key: 'code', label: 'Code', kind: 'text' },
    { key: 'name', label: 'Name', kind: 'text' },
    { key: 'tier', label: 'Tier', kind: 'number' },
    { key: 'kycRiskRating', label: 'Risk Rating', kind: 'lov', lovType: 'KYC_RISK_RATING' },
    { key: 'tags', label: 'Client Tags', kind: 'codes', hint: 'PEP, WATCHLIST_REVIEW' },
    CASE_TYPE,
    { key: 'requiresEdd', label: 'Requires EDD', kind: 'check' },
  ],
  blank: { code: '', name: '', tier: 1, kycRiskRating: 'HIGH', tags: [], requiresEdd: false },
};

const RULES: TableSpec = {
  key: 'riskRules',
  title: 'Tagging Rules (first match by priority wins)',
  fields: [
    { key: 'priority', label: 'Priority', kind: 'number' },
    { key: 'categoryCode', label: 'Category', kind: 'text' },
    {
      key: 'attribute',
      label: 'Attribute',
      kind: 'select',
      options: [
        'MATCH_LIST_TYPE',
        'MATCH_STATUS',
        'PEP',
        'NATIONALITY',
        'OCCUPATION',
        'SOURCE_OF_FUNDS',
        'CLIENT_TYPE',
        'MARKET_SEGMENT',
      ],
    },
    { key: 'operator', label: 'Operator', kind: 'select', options: ['EQ', 'IN', 'NOT_IN'] },
    { key: 'values', label: 'Values', kind: 'codes' },
  ],
  blank: {
    priority: 10,
    categoryCode: '',
    attribute: 'MATCH_LIST_TYPE',
    operator: 'EQ',
    values: [],
  },
};

const ROUTES: TableSpec = {
  key: 'routes',
  title: 'Routes (evaluated in order; blank = any)',
  fields: [
    ORDER,
    { key: 'fromStage', label: 'From Stage', kind: 'select', options: CASE_STAGES },
    CASE_TYPE,
    RISK_CATEGORY,
    UNIT,
    { key: 'disposition', label: 'Disposition', kind: 'lov', lovType: 'SCR_DISPOSITION' },
    { key: 'toStage', label: 'To Stage', kind: 'select', options: CASE_STAGES },
    {
      key: 'approverKind',
      label: 'Approver',
      kind: 'select',
      options: ['UNIT_HEAD', 'ROLE', 'USER'],
    },
    { key: 'approverValue', label: 'Role / User', kind: 'text' },
  ],
  blank: {
    order: 10,
    fromStage: 'INVESTIGATION',
    toStage: 'UNIT_HEAD_APPROVAL',
    approverKind: 'UNIT_HEAD',
  },
};

const ASSIGNMENTS: TableSpec = {
  key: 'assignmentRules',
  title: 'Scenarios (applied when a case is created)',
  fields: [
    ORDER,
    CASE_TYPE,
    { key: 'trigger', label: 'Trigger', kind: 'text' },
    RISK_CATEGORY,
    UNIT,
    {
      key: 'clientType',
      label: 'Client Type',
      kind: 'select',
      options: ['INDIVIDUAL', 'CORPORATE'],
    },
    { key: 'teamRole', label: 'Team Role', kind: 'text' },
    { key: 'user', label: 'User', kind: 'text' },
    {
      key: 'balancing',
      label: 'Balancing',
      kind: 'select',
      options: ['ROUND_ROBIN', 'LEAST_OPEN', 'NONE'],
    },
  ],
  blank: { order: 10, teamRole: 'SCR_INVESTIGATOR', balancing: 'LEAST_OPEN' },
};

const SLA: TableSpec = {
  key: 'slaRules',
  title: 'SLA Rows (most specific row applies)',
  fields: [
    STAGE,
    CASE_TYPE,
    RISK_CATEGORY,
    { key: 'slaHours', label: 'SLA Hours', kind: 'number' },
    { key: 'reminderLeadHours', label: 'Reminder Lead', kind: 'number' },
    { key: 'escalateToRole', label: 'Escalate To Role', kind: 'text' },
    { key: 'calendar', label: 'Calendar', kind: 'select', options: ['CALENDAR', 'WORKING'] },
  ],
  blank: {
    stage: 'INVESTIGATION',
    slaHours: 72,
    reminderLeadHours: 24,
    escalateToRole: 'COMPLIANCE_OFFICER',
    calendar: 'CALENDAR',
  },
};

const CHECKS: TableSpec = {
  key: 'validationRules',
  title: 'Checks Before Routing',
  fields: [
    STAGE,
    CASE_TYPE,
    {
      key: 'rule',
      label: 'Check',
      kind: 'select',
      options: [
        'TEMPLATE_COMPLETE',
        'DOCUMENT_TYPES_PRESENT',
        'DISPOSITION_ALLOWED',
        'RECOMMENDATION_PRESENT',
        'STR_FLAG_CONSISTENT',
      ],
    },
    {
      key: 'parameters',
      label: 'Parameters',
      kind: 'text',
      hint: 'Document types, comma separated',
    },
    { key: 'blocking', label: 'Blocking', kind: 'check' },
  ],
  blank: { stage: 'INVESTIGATION', rule: 'TEMPLATE_COMPLETE', blocking: true },
};

export const FIELD_TABLE: TableSpec = {
  key: 'template.fields',
  title: 'Fields',
  fields: [
    { key: 'order', label: 'Order', kind: 'number' },
    { key: 'section', label: 'Section', kind: 'text' },
    { key: 'code', label: 'Code', kind: 'text', hint: 'CAPITALS, digits, underscore' },
    { key: 'label', label: 'Label', kind: 'text' },
    {
      key: 'dataType',
      label: 'Data Type',
      kind: 'select',
      options: ['TEXT', 'LONG_TEXT', 'NUMBER', 'AMOUNT', 'DATE', 'LOV', 'CHECKBOX', 'ATTACHMENT'],
    },
    { key: 'lovType', label: 'List Type', kind: 'text' },
    { key: 'mandatory', label: 'Mandatory', kind: 'check' },
    { key: 'help', label: 'Help Text', kind: 'text' },
    { key: 'prefillSource', label: 'Prefill (STR)', kind: 'text' },
  ],
  blank: { order: 10, section: 'Findings', code: '', label: '', dataType: 'TEXT', mandatory: true },
};

const COLUMNS: TableSpec = {
  key: 'layout.columns',
  title: 'File Columns',
  fields: [
    { key: 'order', label: 'Order', kind: 'number' },
    { key: 'fieldCode', label: 'STR Field', kind: 'text' },
    { key: 'fixedValue', label: 'Fixed Value', kind: 'text' },
    { key: 'header', label: 'Header', kind: 'text' },
    { key: 'length', label: 'Length', kind: 'number' },
    { key: 'pad', label: 'Padding', kind: 'select', options: ['LEFT', 'RIGHT'] },
  ],
  blank: { order: 1, fieldCode: '', header: '' },
};

/** The configuration types edited on Configuration Versions (templates have their own screen). */
const MATCH_TYPE: TypeSpec = {
  type: 'MATCH_CRITERIA',
  label: 'Matching Criteria',
  description:
    'Algorithms, thresholds, fields compared and the case threshold per list and subject type (SNSRP-101).',
  tables: [MATCH],
};

export const CONFIG_TYPES: readonly TypeSpec[] = [
  MATCH_TYPE,
  {
    type: 'RISK_RULES',
    label: 'Risk Rules',
    description:
      'Risk categories with the rating and tags they set, and the rules that assign them (SNSRP-102).',
    tables: [CATEGORIES, RULES],
  },
  {
    type: 'APPROVAL_MATRIX',
    label: 'Approval Matrix',
    description:
      'Where a submitted case goes and who approves it; routes from Compliance review form the escalation matrix (SNSRP-103, 703).',
    tables: [ROUTES],
  },
  {
    type: 'ASSIGNMENT_MATRIX',
    label: 'Assignment Matrix',
    description: 'Which team or user receives a new case, by scenario (SNSRP-106).',
    tables: [ASSIGNMENTS],
  },
  {
    type: 'SLA_MATRIX',
    label: 'SLA Matrix',
    description: 'Turnaround per stage, reminder lead time and escalation role (SNSRP-108).',
    tables: [SLA],
  },
  {
    type: 'VALIDATION_RULES',
    label: 'Validation Rules',
    description: 'Checks a dispositioned case must pass before it is routed (SNSRP-701, 802).',
    tables: [CHECKS],
  },
  {
    type: 'STR_LAYOUT',
    label: 'STR Layout',
    description:
      'Columns of the STR extraction file; the AMLC format is entered when BDOI supplies it (SNSRP-105, 706).',
    tables: [COLUMNS],
  },
];

/** The editor specification of a type (Matching Criteria when unknown). */
export function typeSpec(type: ConfigType | undefined): TypeSpec {
  return CONFIG_TYPES.find((t) => t.type === type) ?? MATCH_TYPE;
}
