import type { SystemParameter } from '@/api/system';

/**
 * The parameters of the UAM Settings screen (the backend list of UamSettingsController); System
 * Parameters leaves them out.
 */
export const UAM_SETTING_KEYS: readonly string[] = [
  'SESSION_TIMEOUT_MINUTES',
  'SESSION_IDLE_WARNING_MINUTES',
  'SESSION_EXPIRY_WARNING_MINUTES',
  'SESSION_BDOI_DIALOG',
  'SESSION_TIMEOUT_PAGE',
  'UAM_DORMANT_DAYS',
  'UAM_DORMANT_NOTICE_DAYS',
  'LOGIN_MAX_FAILED_ATTEMPTS',
  'PASSWORD_MAX_AGE_DAYS',
  'PASSWORD_MIN_AGE_DAYS',
  'PASSWORD_HISTORY_COUNT',
  'PASSWORD_EXPIRY_NOTICE_DAYS',
  'PASSWORD_RESET_BDOI_RULES',
  'UAM_SSO_PROVISIONING',
  'UAM_PROVISIONING_ROLES',
  'UAM_RECONCILIATION_INTERVAL_DAYS',
  'UAM_ROLE_APPLY_ON_APPROVAL',
  'UAM_ANY_APPROVER',
  'UAM_WORKING_HOURS',
  'UAM_DIRECT_ROLE_EDIT',
  'USER_ID_PATTERN',
];

/** The area of a setting on the screen. */
export function settingGroup(key: string): string {
  if (key.startsWith('SESSION_')) {
    return 'Sessions';
  }
  if (key.startsWith('UAM_DORMANT_')) {
    return 'Dormancy';
  }
  if (key.startsWith('PASSWORD_') || key === 'LOGIN_MAX_FAILED_ATTEMPTS') {
    return 'Local test accounts';
  }
  if (key.includes('PROVISIONING') || key.includes('RECONCILIATION')) {
    return 'Synchronisation';
  }
  return 'Group profiles';
}

/** The short business name of each setting; the parameter key stays out of the screen. */
const LABELS: Record<string, string> = {
  SESSION_TIMEOUT_MINUTES: 'Session timeout',
  SESSION_IDLE_WARNING_MINUTES: 'Inactivity warning before sign-out',
  SESSION_EXPIRY_WARNING_MINUTES: 'Warning before the session ends',
  SESSION_BDOI_DIALOG: "Bank's inactivity warning wording",
  SESSION_TIMEOUT_PAGE: 'Session timed-out page',
  UAM_DORMANT_DAYS: 'Dormant after',
  UAM_DORMANT_NOTICE_DAYS: 'Dormancy notice before',
  LOGIN_MAX_FAILED_ATTEMPTS: 'Failed sign-ins before lock-out',
  PASSWORD_MAX_AGE_DAYS: 'Password expires after',
  PASSWORD_MIN_AGE_DAYS: 'Minimum password age',
  PASSWORD_HISTORY_COUNT: 'Previous passwords not reused',
  PASSWORD_EXPIRY_NOTICE_DAYS: 'Password expiry notice',
  PASSWORD_RESET_BDOI_RULES: "Bank's password reset rules",
  UAM_SSO_PROVISIONING: 'Users from UIDM-ISC',
  UAM_PROVISIONING_ROLES: 'Group profiles from UIDM-ISC',
  UAM_RECONCILIATION_INTERVAL_DAYS: 'Reconciliation interval',
  UAM_ROLE_APPLY_ON_APPROVAL: 'Apply group profile changes on approval',
  UAM_ANY_APPROVER: 'Any approver may decide',
  UAM_WORKING_HOURS: 'Working hours',
  UAM_DIRECT_ROLE_EDIT: 'Direct group profile editing',
  USER_ID_PATTERN: 'User ID format',
};

/** The business name of a setting; an unlisted setting is named from its key. */
export function settingLabel(key: string): string {
  const known = LABELS[key];
  if (known !== undefined) {
    return known;
  }
  const text = key.toLowerCase().replaceAll('_', ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** The unit of a whole-number setting, singular and plural, from the key's last word. */
export function settingUnit(key: string): { one: string; many: string } | undefined {
  if (key.endsWith('_MINUTES')) {
    return { one: 'minute', many: 'minutes' };
  }
  if (key.endsWith('_DAYS')) {
    return { one: 'day', many: 'days' };
  }
  if (key.endsWith('_ATTEMPTS')) {
    return { one: 'attempt', many: 'attempts' };
  }
  if (key === 'PASSWORD_HISTORY_COUNT') {
    return { one: 'password', many: 'passwords' };
  }
  return undefined;
}

/** A value as people read it: "30 minutes", "Yes" / "No", "90 days"; text as it is. */
export function formatSettingValue(
  p: Pick<SystemParameter, 'key' | 'valueType'>,
  value: string | null | undefined,
): string {
  if (value === null || value === undefined || value === '') {
    return '';
  }
  if (p.valueType === 'BOOLEAN') {
    return value.toLowerCase() === 'true' ? 'Yes' : 'No';
  }
  const unit = settingUnit(p.key);
  if (p.valueType === 'INTEGER' && unit !== undefined && /^\d+$/.test(value)) {
    return `${value} ${Number(value) === 1 ? unit.one : unit.many}`;
  }
  return value;
}

/**
 * Why a new value cannot be sent, or undefined: a whole number within the setting's range, a
 * non-empty text.
 */
export function settingValueError(
  p: Pick<SystemParameter, 'valueType' | 'minValue' | 'maxValue'>,
  value: string,
): string | undefined {
  const text = value.trim();
  if (text === '') {
    return 'Enter a value';
  }
  if (p.valueType !== 'INTEGER') {
    return undefined;
  }
  if (!/^\d+$/.test(text)) {
    return 'Enter a whole number';
  }
  const n = Number(text);
  if (p.minValue !== undefined && n < p.minValue) {
    return `Enter at least ${String(p.minValue)}`;
  }
  if (p.maxValue !== undefined && n > p.maxValue) {
    return `Enter at most ${String(p.maxValue)}`;
  }
  return undefined;
}

/** The allowed range of a whole-number setting ("Between 1 and 120"), or undefined. */
export function rangeHint(
  p: Pick<SystemParameter, 'valueType' | 'minValue' | 'maxValue'>,
): string | undefined {
  if (p.valueType !== 'INTEGER' || p.minValue === undefined) {
    return undefined;
  }
  return p.maxValue === undefined
    ? `At least ${String(p.minValue)}`
    : `Between ${String(p.minValue)} and ${String(p.maxValue)}`;
}
