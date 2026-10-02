import { describe, expect, it } from 'vitest';
import { statusLabel, statusShortLabel } from '@/components/ui/statusTones';
import { ACRONYM_LIST, ACRONYMS, acronymOf } from './acronyms';
import { formatAge, humanize, statusPhrase, titleCase } from './format';

describe('acronym map', () => {
  it('holds each acronym once, in capitals', () => {
    expect(new Set(ACRONYM_LIST).size).toBe(ACRONYM_LIST.length);
    for (const a of ACRONYM_LIST) {
      expect(a).toBe(a.toUpperCase());
    }
  });

  it('covers the business acronyms users read in lists', () => {
    for (const a of [
      'RA',
      'TSU',
      'KYC',
      'PRF',
      'CLPC',
      'OR',
      'DP',
      'SOA',
      'FFY',
      'ACSL',
      'EB',
      'PR',
      'IA',
      'GL',
      'SL',
      'TIN',
      'ARN',
    ]) {
      expect(ACRONYMS.has(a)).toBe(true);
    }
  });

  it('gives the label form of a word', () => {
    expect(acronymOf('Ra')).toBe('RA');
    expect(acronymOf('soa')).toBe('SOA');
    expect(acronymOf('MANCOM')).toBe('ManCom');
    expect(acronymOf('Sent')).toBeNull();
  });
});

describe('labels from codes use the acronym map', () => {
  it('humanizes codes with the acronyms in capitals', () => {
    expect(humanize('RA_SENT')).toBe('RA Sent');
    expect(humanize('EB_SOA')).toBe('EB SOA');
    expect(humanize('FOR_TSU_REVIEW')).toBe('For TSU Review');
    expect(humanize('KYC_VERIFIED')).toBe('KYC Verified');
    expect(humanize('SL_GL_TIE_OUT')).toBe('SL GL Tie Out');
    expect(humanize('FOR_MANCOM')).toBe('For ManCom');
    expect(humanize('NEW_BUSINESS')).toBe('New Business');
  });

  it('never Title-Cases an acronym', () => {
    const codes = ['RA_SENT', 'TSU_REVIEW', 'KYC_REVIEW', 'PRF_DRAFT', 'CLPC_BILLING', 'OR_ISSUED'];
    for (const code of codes) {
      expect(humanize(code)).not.toMatch(/\b(Ra|Tsu|Kyc|Prf|Clpc|Or)\b/);
    }
  });

  it('keeps acronyms in Title Case labels and sentences', () => {
    expect(titleCase('Ra sent to client')).toBe('RA Sent to Client');
    expect(titleCase('Awaiting soa')).toBe('Awaiting SOA');
    expect(titleCase('Cash or check')).toBe('Cash or Check');
    expect(statusPhrase('RA_SENT')).toBe('RA sent');
  });

  it('labels status pills with the acronyms in capitals', () => {
    expect(statusLabel('RA_SENT')).toBe('RA Sent');
    expect(statusShortLabel('RA_SENT')).toBe('RA Sent');
    expect(statusLabel('RETURNED_TO_MARKETING')).toBe('Returned to Marketing');
  });
});

describe('formatAge', () => {
  const now = new Date('2026-09-24T12:00:00Z').getTime();
  it('reads in words, whole days from one day on', () => {
    expect(formatAge('2026-09-02T11:00:00Z', now)).toBe('22 days');
    expect(formatAge('2026-09-23T12:00:00Z', now)).toBe('1 day');
    expect(formatAge('2026-09-24T09:00:00Z', now)).toBe('3 hrs');
    expect(formatAge('2026-09-24T11:59:00Z', now)).toBe('1 min');
  });
  it('gives the empty text for no time or an invalid time', () => {
    expect(formatAge(undefined, now)).toBe('');
    expect(formatAge('not a date', now)).toBe('');
  });
});
