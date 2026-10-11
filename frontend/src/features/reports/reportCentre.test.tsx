import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { ReportVariant } from '@/api/nbReports';
import { readCodeSet, writeCodeSet } from './codeSetValue';
import { ReportActions } from './ReportRunnerParts';
import { variantGroups } from './variantGroups';

function variant(id: number, patch: Partial<ReportVariant>): ReportVariant {
  return {
    id,
    reportCode: 'UAM-GROUP-PROFILE',
    name: `Variant ${String(id)}`,
    owner: 'system',
    shared: true,
    standard: false,
    defaultVariant: false,
    mine: false,
    parameters: {},
    ...patch,
  };
}

describe('report variants', () => {
  it('lists the standard variants first, then the user’s, then those shared by others', () => {
    const groups = variantGroups([
      variant(1, { name: 'Mine', owner: 'ao', mine: true, shared: false }),
      variant(2, { name: 'Active Profiles - All Modules', standard: true }),
      variant(3, { name: 'Shared', owner: 'ao2' }),
    ]);
    expect(groups.sections.map((s) => [s.label, s.variants.map((v) => v.name)])).toEqual([
      ['Standard', ['Active Profiles - All Modules']],
      ['My Variants', ['Mine']],
      ['Shared by Others', ['Shared']],
    ]);
    expect(groups.mine.map((v) => v.id)).toEqual([1]);
    expect(variantGroups([variant(2, { standard: true })]).sections).toHaveLength(1);
  });
});

describe('list parameters', () => {
  it('reads and writes "only" and "all except" lists', () => {
    expect(readCodeSet('!A, B')).toEqual({ exclude: true, codes: ['A', 'B'] });
    expect(readCodeSet('')).toEqual({ exclude: false, codes: [] });
    expect(writeCodeSet({ exclude: false, codes: ['A', 'B'] })).toBe('A,B');
    expect(writeCodeSet({ exclude: true, codes: ['A'] })).toBe('!A');
    expect(writeCodeSet({ exclude: true, codes: [] })).toBe('');
  });
});

describe('report actions', () => {
  it('runs first, downloads in the chosen format once valid and prints only after a run', () => {
    const onExport = vi.fn();
    const props = {
      formats: ['XLSX', 'PDF', 'CSV'] as const,
      running: false,
      exporting: false,
      printing: false,
      onRun: vi.fn(),
      onExport,
      onPrint: vi.fn(),
    };
    const { rerender } = render(<ReportActions {...props} valid={false} ran={false} />);
    expect(screen.getAllByRole('button')[0]).toHaveTextContent('Run Report');
    expect(screen.getByRole('button', { name: 'Download' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Print' })).toBeNull();
    rerender(<ReportActions {...props} valid ran />);
    fireEvent.change(screen.getByRole('combobox', { name: 'Download as' }), {
      target: { value: 'PDF' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Download' }));
    expect(onExport).toHaveBeenCalledWith('PDF');
    expect(screen.getByRole('button', { name: 'Print' })).toBeTruthy();
  });
});
