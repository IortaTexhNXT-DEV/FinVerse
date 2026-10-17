import { describe, expect, it } from 'vitest';
import {
  UAM_SETTING_KEYS,
  formatSettingValue,
  rangeHint,
  settingGroup,
  settingLabel,
  settingValueError,
} from './uamSettings';

describe('UAM settings', () => {
  it('groups the settings by area', () => {
    expect(settingGroup('SESSION_TIMEOUT_MINUTES')).toBe('Sessions');
    expect(settingGroup('UAM_DORMANT_DAYS')).toBe('Dormancy');
    expect(settingGroup('LOGIN_MAX_FAILED_ATTEMPTS')).toBe('Local test accounts');
    expect(settingGroup('UAM_RECONCILIATION_INTERVAL_DAYS')).toBe('Synchronisation');
    expect(settingGroup('UAM_ANY_APPROVER')).toBe('Group profiles');
    expect(UAM_SETTING_KEYS).toContain('UAM_RECONCILIATION_INTERVAL_DAYS');
  });

  it('names every setting in business words and formats its value', () => {
    UAM_SETTING_KEYS.forEach((key) => expect(settingLabel(key)).not.toContain('_'));
    expect(settingLabel('SESSION_TIMEOUT_MINUTES')).toBe('Session timeout');
    const timeout = { key: 'SESSION_TIMEOUT_MINUTES', valueType: 'INTEGER' } as const;
    expect(formatSettingValue(timeout, '30')).toBe('30 minutes');
    expect(formatSettingValue({ key: 'UAM_DORMANT_DAYS', valueType: 'INTEGER' }, '90')).toBe(
      '90 days',
    );
    expect(formatSettingValue({ key: 'UAM_ANY_APPROVER', valueType: 'BOOLEAN' }, 'true')).toBe(
      'Yes',
    );
    expect(formatSettingValue({ key: 'UAM_ANY_APPROVER', valueType: 'BOOLEAN' }, 'false')).toBe(
      'No',
    );
  });

  it('checks a whole number against the range of the setting', () => {
    const p = { valueType: 'INTEGER', minValue: 5, maxValue: 120 } as const;
    expect(settingValueError(p, '30')).toBeUndefined();
    expect(settingValueError(p, '3')).toBe('Enter at least 5');
    expect(settingValueError(p, '121')).toBe('Enter at most 120');
    expect(settingValueError(p, 'x')).toBe('Enter a whole number');
    expect(rangeHint(p)).toBe('Between 5 and 120');
  });
});
