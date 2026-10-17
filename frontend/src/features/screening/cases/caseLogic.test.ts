import { describe, expect, it, vi } from 'vitest';
import type { TemplateField } from './api';
import {
  dispositionParent,
  fieldError,
  filtersOf,
  homeTiles,
  missingFields,
  periodError,
  searchError,
  searchParams,
  sections,
  slaLabel,
  stageLabel,
  tabOf,
  toSearch,
  uploadErrors,
} from './caseLogic';

function field(patch: Partial<TemplateField> = {}): TemplateField {
  return {
    code: 'SOURCE_OF_FUNDS',
    section: 'Client',
    label: 'Source of Funds',
    dataType: 'TEXT',
    mandatory: true,
    ...patch,
  };
}

describe('case list address', () => {
  it('reads the tab, defaulting to ALL', () => {
    expect(tabOf('MY')).toBe('MY');
    expect(tabOf('NOPE')).toBe('ALL');
    expect(tabOf(null)).toBe('ALL');
  });

  it('round-trips the tab and filters, leaving out blanks', () => {
    const search = toSearch('TEAM', { q: ' Dela ', stage: 'INVESTIGATION', caseType: ' ' });
    expect(search.toString()).toBe('tab=TEAM&q=Dela&stage=INVESTIGATION');
    expect(filtersOf(search)).toEqual({ q: 'Dela', stage: 'INVESTIGATION' });
    expect(toSearch('ALL', {}).toString()).toBe('');
  });

  it('builds the search request', () => {
    expect(searchParams(1, 'MY', { sla: 'BREACHED' }, 2)).toEqual({
      companyId: 1,
      tab: 'MY',
      page: 2,
      sla: 'BREACHED',
    });
  });
});

describe('search and period checks', () => {
  it('needs at least 3 characters', () => {
    expect(searchError('ab')).toBe('Enter at least 3 characters');
    expect(searchError('abc')).toBeUndefined();
    expect(searchError('  ')).toBeUndefined();
    expect(searchError(undefined)).toBeUndefined();
  });

  it('refuses a reversed period', () => {
    expect(periodError('2026-05-02', '2026-05-01')).toBe(
      'The end date must be on or after the start date',
    );
    expect(periodError('2026-05-01', '2026-05-01')).toBeUndefined();
    expect(periodError(undefined, '2026-05-01')).toBeUndefined();
  });
});

describe('labels', () => {
  it('labels stages and SLA states', () => {
    expect(stageLabel('UNIT_HEAD_APPROVAL')).toBe('Unit Head Approval');
    expect(stageLabel('OTHER')).toBe('OTHER');
    expect(slaLabel('ON_TIME')).toBe('On Time');
    expect(slaLabel('DUE_SOON')).toBe('Due Soon');
    expect(slaLabel('BREACHED')).toBe('Breached');
    expect(slaLabel('NONE')).toBe('—');
  });

  it('offers the investigation dispositions on a returned case', () => {
    expect(dispositionParent('RETURNED')).toBe('INVESTIGATION');
    expect(dispositionParent('COMPLIANCE_REVIEW')).toBe('COMPLIANCE_REVIEW');
  });
});

describe('review form', () => {
  it('groups fields by consecutive section', () => {
    const groups = sections([
      field({ code: 'A', section: 'One' }),
      field({ code: 'B', section: 'One' }),
      field({ code: 'C', section: 'Two' }),
    ]);
    expect(groups.map((g) => [g.section, g.fields.length])).toEqual([
      ['One', 2],
      ['Two', 1],
    ]);
  });

  it('checks numbers, amounts and dates', () => {
    expect(fieldError(field({ dataType: 'AMOUNT', label: 'Income' }), '1,000.50')).toBeUndefined();
    expect(fieldError(field({ dataType: 'AMOUNT', label: 'Income' }), 'abc')).toBe(
      'Income must be an amount',
    );
    expect(fieldError(field({ dataType: 'NUMBER', label: 'Age' }), 'x')).toBe(
      'Age must be a number',
    );
    expect(fieldError(field({ dataType: 'DATE', label: 'Seen' }), '01/02/2026')).toBe(
      'Seen must be a date',
    );
    expect(fieldError(field({ dataType: 'DATE' }), '2026-02-01')).toBeUndefined();
    expect(fieldError(field(), ' ')).toBeUndefined();
  });

  it('lists blank mandatory fields, an unticked checkbox counting as blank', () => {
    const fields = [
      field({ code: 'A', label: 'A' }),
      field({ code: 'B', label: 'B', dataType: 'CHECKBOX' }),
      field({ code: 'C', label: 'C', mandatory: false }),
    ];
    expect(missingFields(fields, { A: ' ', B: 'false' })).toEqual({
      A: 'A is required',
      B: 'B is required',
    });
    expect(missingFields(fields, { A: 'x', B: 'TRUE' })).toEqual({});
  });
});

describe('upload dialog', () => {
  const file = new File(['x'], 'kyc.pdf');

  it('needs every field and a received date not in the future', () => {
    expect(
      uploadErrors({ formType: '', documentType: '', dateReceived: '', source: ' ' }, '2026-05-01'),
    ).toEqual({
      file: 'Choose the file',
      formType: 'Enter the form type of the document',
      documentType: 'Enter the document type of the document',
      dateReceived: 'Enter the date received of the document',
      source: 'Enter the source of the document',
    });
    expect(
      uploadErrors(
        {
          file,
          formType: 'KYC_FORM',
          documentType: 'ID',
          dateReceived: '2026-05-02',
          source: 'Client',
        },
        '2026-05-01',
      ),
    ).toEqual({ dateReceived: 'The date received cannot be in the future' });
  });
});

describe('home tiles', () => {
  it('shows open stages, then SLA and matches tiles that open their lists', () => {
    const go = vi.fn();
    const tiles = homeTiles(
      { openByStage: { INVESTIGATION: 3 }, dueToday: 1, breached: 2, potentialMatches: 4 },
      go,
    );
    expect(tiles.map((t) => t.key)).not.toContain('CLOSED');
    expect(tiles.find((t) => t.key === 'INVESTIGATION')?.value).toBe(3);
    expect(tiles.find((t) => t.key === 'NEW')?.value).toBe(0);
    tiles.find((t) => t.key === 'breached')?.onClick();
    expect(go).toHaveBeenCalledWith('/screening/cases?sla=BREACHED');
    tiles.find((t) => t.key === 'matches')?.onClick();
    expect(go).toHaveBeenCalledWith('/screening/matches');
  });
});
