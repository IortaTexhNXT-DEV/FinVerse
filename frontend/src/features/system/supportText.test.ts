import {
  cacheStoreLabel,
  contentAreaName,
  copiedPercent,
  durationText,
  holdActionLabel,
} from './supportText';

describe('support screen texts', () => {
  it('reads durations and periods in words', () => {
    expect(durationText('PT15M')).toBe('15 minutes');
    expect(durationText('PT1H30M')).toBe('1 hour 30 minutes');
    expect(durationText('P10Y')).toBe('10 years');
    expect(durationText('P1D')).toBe('1 day');
    expect(durationText('P')).toBe('P');
    expect(durationText('forever')).toBe('forever');
    expect(durationText('PT1X')).toBe('PT1X');
  });

  it('names the hold actions and counts the copied files', () => {
    expect(holdActionLabel('PLACE')).toBe('Place legal hold');
    expect(holdActionLabel('RELEASE')).toBe('Release legal hold');
    expect(copiedPercent({ total: 0, moved: 0 })).toBe(100);
    expect(copiedPercent({ total: 3, moved: 1 })).toBe(33);
  });

  it('names the cache store', () => {
    expect(cacheStoreLabel('VALKEY')).toBe('Shared (Valkey)');
    expect(cacheStoreLabel('IN_MEMORY')).toBe('This Instance');
  });

  it('names the content areas in business words', () => {
    expect(contentAreaName('plc_slip_file')).toBe('Placement slips');
    expect(contentAreaName('new_area')).toBe('new_area');
  });
});
