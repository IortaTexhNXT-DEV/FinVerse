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

/** What a value of the setting looks like. */
export function valueHint(p: Pick<SystemParameter, 'valueType' | 'minValue' | 'maxValue'>): string {
  const range =
    p.minValue === undefined
      ? ''
      : ` between ${String(p.minValue)} and ${String(p.maxValue ?? '…')}`;
  switch (p.valueType) {
    case 'INTEGER':
      return `Whole number${range}`;
    case 'BOOLEAN':
      return 'true or false';
    default:
      return 'Text';
  }
}
