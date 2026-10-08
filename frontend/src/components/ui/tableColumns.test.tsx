import { fireEvent, render, screen, within } from '@testing-library/react';
import { DataTable } from './DataTable';
import type { Column } from './DataTable';
import {
  choiceKey,
  defaultChoice,
  lockedKeys,
  mergeChoice,
  moveColumn,
  toggleColumn,
  visibleColumns,
} from './tableColumns';
import type { ColumnChoice } from './tableColumns';

interface Row {
  id: number;
}

const COLUMNS: Column<Row>[] = [
  { key: 'select', header: '', render: () => '' },
  { key: 'ref', header: 'Reference', kind: 'code', render: (r) => `REF-${String(r.id)}` },
  ...['Client', 'Insurer', 'Product', 'Period', 'Premium', 'Officer'].map((h) => ({
    key: h.toLowerCase(),
    header: h,
    render: () => h,
  })),
  { key: 'branch', header: 'Branch', defaultHidden: true, render: () => 'Makati' },
  { key: 'actions', header: '', kind: 'actions', render: () => 'more' },
];

describe('column choice', () => {
  it('hides the default-hidden columns and never the selection, reference or actions', () => {
    const fresh = defaultChoice(COLUMNS);
    expect(fresh.hidden).toEqual(['branch']);
    const locked = lockedKeys(COLUMNS);
    expect([...locked]).toEqual(['select', 'ref', 'actions']);
    const hideAll = COLUMNS.reduce<ColumnChoice>((c, col) => toggleColumn(c, col.key), {
      ...fresh,
      hidden: [],
    });
    expect(visibleColumns(COLUMNS, hideAll, locked).map((c) => c.key)).toEqual([
      'select',
      'ref',
      'actions',
    ]);
  });

  it('keeps a saved order and adds columns defined since in their place', () => {
    const saved = { order: ['select', 'ref', 'premium', 'client'], hidden: ['client'] };
    const merged = mergeChoice(COLUMNS, saved);
    // The saved columns keep their order; a column defined since follows its defined predecessor.
    expect(merged.order.filter((k) => saved.order.includes(k))).toEqual(saved.order);
    expect(merged.order.indexOf('officer')).toBe(merged.order.indexOf('premium') + 1);
    expect(merged.order).toContain('branch');
    expect(merged.hidden).toEqual(['client', 'branch']);
    expect(mergeChoice(COLUMNS, null)).toEqual(defaultChoice(COLUMNS));
    const moved = moveColumn(defaultChoice(COLUMNS), 'premium', -1);
    expect(moved.order.indexOf('premium')).toBe(moved.order.indexOf('period') - 1);
    expect(choiceKey('ao', 'accounts')).toBe('bibs.columns.ao.accounts');
  });
});

describe('wide list tools', () => {
  beforeEach(() => window.localStorage.clear());

  it('offers the column chooser, compact rows and a sticky reference column', () => {
    const { container } = render(
      <DataTable rows={[{ id: 1 }]} rowKey={(r) => r.id} columns={COLUMNS} list="test-list" />,
    );
    expect(screen.queryByRole('columnheader', { name: 'Branch' })).toBeNull();
    expect(container.querySelector('td.sticky-col-0')).not.toBeNull();
    expect(container.querySelector('td.sticky-col-1')?.textContent).toBe('REF-1');
    fireEvent.click(screen.getByRole('button', { name: 'Columns (1 hidden)' }));
    const chooser = screen.getByRole('dialog', { name: 'Columns of the list' });
    expect(within(chooser).getByRole('checkbox', { name: 'Reference' })).toBeDisabled();
    fireEvent.click(within(chooser).getByRole('checkbox', { name: 'Branch' }));
    expect(screen.getByRole('columnheader', { name: 'Branch' })).toBeTruthy();
    fireEvent.click(within(chooser).getByRole('checkbox', { name: 'Client' }));
    expect(screen.queryByRole('columnheader', { name: 'Client' })).toBeNull();
    // The choice is kept per user in the browser, and Reset restores the default.
    expect(window.localStorage.getItem('bibs.columns.guest.test-list')).toContain('client');
    fireEvent.click(within(chooser).getByRole('button', { name: 'Reset to Default' }));
    expect(screen.getByRole('columnheader', { name: 'Client' })).toBeTruthy();
    expect(window.localStorage.getItem('bibs.columns.guest.test-list')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: /Compact/ }));
    expect(container.querySelector('table')).toHaveClass('table-compact');
  });

  it('leaves short lists as they are', () => {
    const { container } = render(
      <DataTable rows={[{ id: 1 }]} rowKey={(r) => r.id} columns={COLUMNS.slice(1, 5)} />,
    );
    expect(screen.queryByRole('button', { name: /Columns/ })).toBeNull();
    expect(container.querySelector('.sticky-col')).toBeNull();
  });
});
