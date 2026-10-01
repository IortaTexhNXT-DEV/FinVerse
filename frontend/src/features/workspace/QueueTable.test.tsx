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
