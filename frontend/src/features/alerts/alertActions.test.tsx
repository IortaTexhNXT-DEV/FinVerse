import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import type { AlertItem } from '@/api/alerts';
import { AlertActionDialog } from './AlertActionDialog';
import { alertRef, recordKind, recordLink, recordText, ruleName, ruleNames } from './alertWording';

const ALERT: AlertItem = {
  id: 12,
  exceptionCode: 'LARGE_JOURNAL',
  severity: 'HIGH',
  module: 'GL',
  entityType: 'MigBatch',
  entityId: 'MGB-2026-000003',
  message: 'Batch MGB-2026-000003 has 1 unmapped legacy code',
  status: 'OPEN',
  raisedAt: '2026-10-08T02:00:00Z',
};

describe('alert wording', () => {
  it('names rules in business words and links the record', () => {
    const names = ruleNames([
      {
        code: 'LARGE_JOURNAL',
        name: 'Large journal posting',
        description: '',
        module: 'GL',
        severity: 'HIGH',
        active: true,
      },
    ]);
    expect(ruleName(names, 'LARGE_JOURNAL')).toBe('Large journal posting');
    expect(ruleName(names, 'WEEKEND_POSTING')).toBe('Weekend Posting');
    expect(alertRef(12)).toBe('ALR-000012');
    expect(recordKind('ScreeningCase')).toBe('Screening case');
    expect(recordText({ entityType: 'BrokerClaim', entityId: '2' })).toBe('Claim');
    expect(recordText({ entityType: 'JournalBatch', entityId: 'J-1' })).toBe('Journal J-1');
    expect(recordLink(ALERT)).toBe('/migration/batches/MGB-2026-000003');
    expect(recordLink({ entityType: 'Role', entityId: 'X' })).toBeUndefined();
  });
});

describe('alert actions', () => {
  it('names the alert, shows its facts and asks a resolution of 10 to 200 characters', () => {
    const onConfirm = vi.fn();
    render(
      <MemoryRouter>
        <AlertActionDialog
          alert={ALERT}
          kind="resolve"
          rule="Large journal posting"
          busy={false}
          error={null}
          onConfirm={onConfirm}
          onClose={vi.fn()}
        />
      </MemoryRouter>,
    );
    const dialog = screen.getByRole('dialog', { name: 'Resolve Alert ALR-000012' });
    expect(within(dialog).getByText('Large journal posting')).toBeTruthy();
    expect(within(dialog).getByText('08-Oct-2026 10:00')).toBeTruthy();
    expect(
      within(dialog).getByRole('link', { name: 'Migration batch MGB-2026-000003' }),
    ).toBeTruthy();
    const buttons = within(dialog)
      .getAllByRole('button')
      .map((b) => b.textContent);
    expect(buttons.slice(-2)).toEqual(['Cancel', 'Resolve Alert']);
    fireEvent.click(within(dialog).getByRole('button', { name: 'Resolve Alert' }));
    expect(within(dialog).getByRole('alert').textContent).toBe('Enter the resolution.');
    fireEvent.change(within(dialog).getByLabelText(/Resolution/), {
      target: { value: 'Journal checked with the maker' },
    });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Resolve Alert' }));
    expect(onConfirm).toHaveBeenCalledWith('Journal checked with the maker');
  });
});
