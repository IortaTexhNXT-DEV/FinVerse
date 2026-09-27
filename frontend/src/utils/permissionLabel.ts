/** Module prefixes of the permission codes, in words (the same rule as the server's names). */
const PREFIXES: Record<string, string> = {
  UAM: 'User access',
  SCR: 'Screening',
  RNW: 'Renewal',
  EB: 'Employee benefits',
  BCL: 'Claims handling',
  PKG: 'Package',
  MIG: 'Data migration',
  LOV: 'Lists of values',
  NB: 'New business',
};

const ACRONYMS = new Set([
  'TSU',
  'MBS',
  'QS',
  'PRF',
  'KYC',
  'AML',
  'STR',
  'ACSL',
  'FRBS',
  'GL',
  'AR',
  'AP',
  'DV',
  'PR',
  'OR',
  'SOA',
  'EOD',
  'BIR',
  'VAT',
  'EWT',
  'SL',
  'SOD',
  'CSF',
  'ID',
  'LAMD',
]);

/** What each action word of a permission lets the user do; {0} is the subject in words. */
const ACTIONS: Record<string, string> = {
  VIEW: 'Open and search {0} records',
  APPROVE: 'Approve {0} items submitted by others',
  MANAGE: 'Set up and maintain {0}',
  MAINTAIN: 'Create and change {0} records',
  PROCESS: 'Process {0} work',
  REQUEST: 'Raise {0} requests',
  AUTHORIZE: 'Authorise {0} changes made by others',
  ASSIGN: 'Assign {0} work to users',
  EXPORT: 'Export {0} data to files',
  UPLOAD: 'Upload {0} files',
  REVIEW: 'Review {0} items',
  EXTRACT: 'Extract {0} data',
  SETUP: 'Maintain the {0} set-up',
  RUN: 'Run {0}',
  SEND: 'Send {0}',
  SIGNOFF: 'Sign off {0}',
  UPDATE: 'Update {0}',
  OVERRIDE: 'Override {0} checks',
  CREATE: 'Create {0}',
  CLOSE: 'Close {0}',
  PREPARE: 'Prepare {0}',
  CANCEL: 'Cancel {0}',
  APPLY: 'Apply {0}',
  VALIDATE: 'Validate {0}',
  DECIDE: 'Decide on {0}',
  TAG: 'Tag {0}',
  REVERSE: 'Reverse {0}',
  POST: 'Post {0}',
  SUBMIT: 'Submit {0}',
  PRINT: 'Print {0}',
};

/** Permissions whose action word alone does not say it plainly (user access requests). */
const DESCRIPTIONS: Record<string, string> = {
  UAM_ENROLL: 'Request access for new users',
  UAM_MODIFY: 'Request changes to the access of users',
  UAM_DEACTIVATE: 'Request the deactivation of users',
  UAM_REACTIVATE: 'Request the reactivation of users',
  UAM_CORRECT: 'Correct user access requests',
  UAM_CANCEL: 'Cancel user access requests',
  UAM_VIEW: 'Open and search user access requests',
  UAM_GROUP_REQUEST: 'Request new or changed group profiles',
  UAM_REPORT_VIEW: 'Open the user access reports',
  UAM_SECOND_APPROVE: 'Give the second approval of user access requests',
  UAM_SOD_MAINTAIN: 'Maintain the separation of duties rules',
  UAM_SOD_AUTHORIZE: 'Authorise changes to the separation of duties rules',
};

/** Words of permission code parts (prefixes and acronyms as in the name). */
function words(parts: readonly string[]): string {
  return parts
    .map((part, i) => {
      if (i === 0 && PREFIXES[part] !== undefined) {
        return PREFIXES[part].toLowerCase();
      }
      return ACRONYMS.has(part) ? part : part.toLowerCase();
    })
    .join(' ');
}

/**
 * A short plain description of what a permission allows, from its action word: CLIENT_VIEW is
 * "Open and search client records", PKG_TSU_APPROVE "Approve package TSU items submitted by
 * others". A permission without a known action word is described as using its function.
 */
export function permissionDescription(code: string): string {
  const parts = code.trim() === '' ? [] : code.split('_');
  if (parts.length === 0) {
    return '';
  }
  const known = DESCRIPTIONS[code];
  if (known !== undefined) {
    return known;
  }
  const action = parts.at(-1) ?? '';
  const template = ACTIONS[action];
  if (template === undefined || parts.length === 1) {
    return `Use the ${words(parts)} function`;
  }
  return template.replace('{0}', words(parts.slice(0, -1)));
}

/**
 * The readable name of a permission code: the module prefix in words and the rest in lower case,
 * for example UAM_ENROLL is "User access enroll" and PKG_TSU_APPROVE "Package TSU approve".
 */
export function permissionLabel(code: string): string {
  if (code.trim() === '') {
    return '';
  }
  const words = code.split('_').map((part, i) => {
    if (i === 0 && PREFIXES[part] !== undefined) {
      return PREFIXES[part];
    }
    return ACRONYMS.has(part) ? part : part.toLowerCase();
  });
  const text = words.join(' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Readable names of permission codes, sorted by code and separated by commas. */
export function permissionLabels(codes: readonly string[]): string {
  return [...codes]
    .sort((a, b) => a.localeCompare(b))
    .map(permissionLabel)
    .join(', ');
}
