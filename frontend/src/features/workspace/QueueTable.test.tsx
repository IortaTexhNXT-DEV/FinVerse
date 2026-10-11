import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import type { WorkItem } from '@/api/workflow';
import type { Company } from '@/api/types';
import { WorkspaceContext } from '@/context/workspaceContext';
import { setUserDirectory } from '@/api/users';
import { workflowApi } from '@/api/workflow';
import { ToastContext } from '@/components/ui/toastContext';
import { AssignDialog } from './AssignDialog';
import { QueueTable } from './QueueTable';

const item = (over: Partial<WorkItem>): WorkItem => ({
  id: 1,
  workflowCode: 'NB_PROPOSAL',
  stageCode: 'WITH_TSU',
  stageName: 'With TSU',
  entityType: 'PROPOSAL',
  entityId: '1',
  reference: 'PRF-2026-900003',
  title: 'Bayside Builders Co.',
  overdue: false,
  stageEnteredAt: '2026-09-27T01:00:00Z',
  createdBy: 'ao',
  createdAt: '2026-09-27T01:00:00Z',
  ...over,
});

function show(items: WorkItem[], handlers: { claim?: () => void; assign?: () => void } = {}) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const workspace = {
    companies: [],
    company: { id: 1 } as Company,
    branches: [],
    branchId: undefined,
    setCompanyId: () => undefined,
    setBranchId: () => undefined,
  };
  render(
    <MemoryRouter>
      <QueryClientProvider client={queries}>
        <WorkspaceContext.Provider value={workspace}>
          <QueueTable
            items={items}
            loading={false}
            canAssign
            claiming={false}
            onOpen={vi.fn()}
            onClaim={handlers.claim ?? vi.fn()}
            onAssign={handlers.assign ?? vi.fn()}
          />
        </WorkspaceContext.Provider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('QueueTable', () => {
  afterEach(() => vi.restoreAllMocks());

  it('names the originating segment or sales unit, never its code', async () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([
      { code: 'CORBANK', label: 'Corporate Banking' },
      { code: 'COMBANK', label: 'Commercial Banking' },
    ]);
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({
      units: [{ code: 'T-CBG1', name: 'CBG Team 1' }],
    } as unknown as Awaited<ReturnType<typeof catalogApi.salesOrganisation>>);
    show([
      item({ id: 1, originatingUnit: 'CORBANK' }),
      item({ id: 2, reference: 'PRF-2026-900004', originatingUnit: 'T-CBG1' }),
    ]);
    expect(await screen.findByText('Corporate Banking')).toBeTruthy();
    expect(await screen.findByText('CBG Team 1')).toBeTruthy();
    expect(screen.queryByText('CORBANK')).toBeNull();
    expect(screen.queryByText('T-CBG1')).toBeNull();
  });

  it('names the product line a package request comes from', async () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({
      units: [],
    } as unknown as Awaited<ReturnType<typeof catalogApi.salesOrganisation>>);
    vi.spyOn(catalogApi, 'lines').mockResolvedValue([
      { code: 'PROPERTY', name: 'Property' },
    ] as unknown as Awaited<ReturnType<typeof catalogApi.lines>>);
    show([item({ id: 1, workflowCode: 'PM_PACKAGE_REQUEST', originatingUnit: 'PROPERTY' })]);
    expect(await screen.findByText('Property')).toBeTruthy();
    expect(screen.queryByText('PROPERTY')).toBeNull();
  });

  it('names employee benefits teams, and shows an unknown unit code muted', async () => {
    vi.spyOn(lovApi, 'options').mockImplementation((type: string) =>
      Promise.resolve(
        type === 'EB_TEAM'
          ? [
              { code: 'NEW_BUSINESS', label: 'New Business' },
              { code: 'BDO', label: 'BDO' },
            ]
          : [],
      ),
    );
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({
      units: [],
    } as unknown as Awaited<ReturnType<typeof catalogApi.salesOrganisation>>);
    show([
      item({ id: 1, workflowCode: 'EB_CYCLE', originatingUnit: 'NEW_BUSINESS' }),
      item({ id: 2, reference: 'EBC-2026-000002', originatingUnit: 'X-UNKNOWN' }),
    ]);
    expect(await screen.findByText('New Business')).toBeTruthy();
    expect(screen.queryByText('NEW_BUSINESS')).toBeNull();
    expect(screen.getByText('X-UNKNOWN')).toHaveClass('muted');
  });

  it('shows the name and the amount of a description in their own columns, formatted', () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    show([
      item({ id: 1, title: 'Maria Clara Santos PHP 500.00' }),
      item({ id: 2, reference: 'DV-2', title: 'Mega Traders Inc. PHP 2500.00' }),
      item({ id: 3, reference: 'X-3', title: 'Bayside Builders Co.', assignee: 'tsu' }),
    ]);
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toContain('Amount');
    expect(headers.at(-1)).toBe('Actions');
    expect(headers.every((h) => h !== '')).toBe(true);
    expect(screen.getByText('Maria Clara Santos')).toBeTruthy();
    expect(screen.getByText('PHP 2,500.00').closest('td')).toHaveClass('num');
    expect(screen.queryByText(/2500\.00/)).toBeNull();
    // A description without an amount: the amount cell is the muted dash.
    const row = screen.getByText('X-3').closest('tr');
    expect(within(row as HTMLElement).getAllByText('—').length).toBeGreaterThan(0);
  });

  it('shows the record type in its own column and the stage pill alone', () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    show([
      item({ id: 1, workflowCode: 'NB_ACCOUNT', stageCode: 'RETURNED_TO_MARKETING' }),
      item({ id: 2, reference: 'EBC-2026-000001', workflowCode: 'EB_CYCLE', stageCode: 'RA_SENT' }),
      item({ id: 3, reference: 'X-1', workflowCode: 'SOMETHING_NEW', stageCode: 'DRAFT' }),
    ]);
    expect(screen.getByRole('columnheader', { name: 'Type' })).toBeTruthy();
    expect(screen.getByText('Account').closest('td')).not.toHaveClass('col-truncate');
    expect(screen.getByText('EB Cycle')).toBeTruthy();
    expect(screen.queryByText('Accounts')).toBeNull();
    const pill = screen.getByText('RA Sent');
    expect(pill).toHaveClass('badge');
    expect(pill.parentElement?.tagName).toBe('TD');
    expect(screen.queryByText('Ra Sent')).toBeNull();
    expect(screen.getByText('Returned to Marketing')).toHaveClass('badge', 'danger');
    // An unknown workflow is a dash, never a blank cell or its code.
    const unknownRow = screen.getByText('X-1').closest('tr');
    expect(unknownRow?.textContent).toContain('—');
    expect(screen.queryByText('SOMETHING_NEW')).toBeNull();
  });

  it('shows age in days right-aligned and the due time on one line, red only when overdue', () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-09-24T02:00:00Z'));
    show([
      item({
        id: 1,
        stageEnteredAt: '2026-09-02T01:00:00Z',
        dueAt: '2026-09-10T02:00:00Z',
        overdue: true,
      }),
      item({ id: 2, reference: 'PRF-2026-900004', dueAt: '2026-10-10T02:00:00Z' }),
      item({ id: 3, reference: 'PRF-2026-900005' }),
    ]);
    vi.useRealTimers();
    // The time in stage is the second line of the due cell, in tabular figures.
    const age = screen.getByText('22 days in stage');
    expect(age).toHaveClass('num');
    const late = screen.getByText('10-Sep-2026 10:00').closest('.due-date');
    expect(late).toHaveClass('overdue');
    expect(late?.textContent).toContain('Overdue');
    expect(late?.querySelector('svg')).not.toBeNull();
    expect(late?.closest('td')).toHaveClass('col-datetime');
    const onTime = screen.getByText('10-Oct-2026 10:00');
    expect(onTime).not.toHaveClass('overdue');
    expect(onTime.textContent).not.toContain('Overdue');
    expect(screen.getByText('PRF-2026-900005').closest('tr')?.textContent).toContain('—');
  });

  it('shows names whole (wrapped in their column, never cut), the login in the tooltip', () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    setUserDirectory([{ username: 'ao', displayName: 'Aileen Account Officer' }]);
    show([item({ id: 1, assignee: 'ao' })]);
    const names = screen.getAllByText('Aileen Account Officer');
    // The assignee column and the "from" line under the description.
    expect(names).toHaveLength(2);
    for (const name of names) {
      expect(name).not.toHaveClass('truncate');
      // The whole name shows; the login id is in the tooltip.
      expect(name).toHaveAttribute('title', 'ao');
      expect(name.closest('td')).not.toHaveClass('col-truncate');
    }
    expect(screen.getByText('Bayside Builders Co.')).toHaveAttribute(
      'title',
      'Bayside Builders Co.',
    );
    // No item of the list has an amount: no column of dashes.
    expect(screen.queryByRole('columnheader', { name: 'Amount' })).toBeNull();
    setUserDirectory([]);
  });

  it('offers Claim and Assign in the row action menu, Claim only while unassigned', () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    const claim = vi.fn();
    const assign = vi.fn();
    show([item({ id: 1 }), item({ id: 2, reference: 'PRF-2026-900004', assignee: 'tsu' })], {
      claim,
      assign,
    });
    expect(screen.queryByRole('button', { name: 'Claim' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Actions for PRF-2026-900003' }));
    const menu = screen.getByRole('menu');
    expect(
      within(menu)
        .getAllByRole('menuitem')
        .map((m) => m.textContent),
    ).toEqual(['Claim', 'Assign']);
    fireEvent.click(within(menu).getByRole('menuitem', { name: 'Claim' }));
    expect(claim).toHaveBeenCalledOnce();
    fireEvent.click(screen.getByRole('button', { name: 'Actions for PRF-2026-900004' }));
    expect(
      within(screen.getByRole('menu'))
        .getAllByRole('menuitem')
        .map((m) => m.textContent),
    ).toEqual(['Assign']);
  });

  it('names the users of the Assign dialog, never by their login', async () => {
    setUserDirectory([{ username: 'ao', displayName: 'Aileen Account Officer' }]);
    vi.spyOn(workflowApi, 'assignees').mockResolvedValue(['ao']);
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ToastContext.Provider
          value={{ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }}
        >
          <AssignDialog item={item({})} onClose={vi.fn()} onDone={() => Promise.resolve()} />
        </ToastContext.Provider>
      </QueryClientProvider>,
    );
    expect(await screen.findByRole('option', { name: 'Aileen Account Officer' })).toBeTruthy();
    expect(screen.queryByRole('option', { name: 'ao' })).toBeNull();
    setUserDirectory([]);
  });
});
