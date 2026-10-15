import { describe, expect, it } from 'vitest';
import { UAM_SETTING_KEYS, settingGroup, valueHint } from './uamSettings';

describe('UAM settings', () => {
  it('groups the settings by area', () => {
    expect(settingGroup('SESSION_TIMEOUT_MINUTES')).toBe('Sessions');
    expect(settingGroup('UAM_DORMANT_DAYS')).toBe('Dormancy');
    expect(settingGroup('LOGIN_MAX_FAILED_ATTEMPTS')).toBe('Local test accounts');
    expect(settingGroup('UAM_RECONCILIATION_INTERVAL_DAYS')).toBe('Synchronisation');
    expect(settingGroup('UAM_ANY_APPROVER')).toBe('Group profiles');
    expect(UAM_SETTING_KEYS).toContain('UAM_RECONCILIATION_INTERVAL_DAYS');
  });

  it('describes the value', () => {
    expect(valueHint({ valueType: 'INTEGER', minValue: 0, maxValue: 31 })).toBe(
      'Whole number between 0 and 31',
    );
    expect(valueHint({ valueType: 'BOOLEAN' })).toBe('true or false');
  });
});
