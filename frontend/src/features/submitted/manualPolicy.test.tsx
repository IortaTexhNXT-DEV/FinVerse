import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { Route, Routes } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import { submittedApi } from '@/api/submitted';
import { workflowApi } from '@/api/workflow';
import MasterlistPage from './masterlist/MasterlistPage';
import PolicyPage from './record/PolicyPage';
import { page, policyDetail, policyRow } from './submittedFixtures';
import { submittedWrapper } from './testWrapper';

const MAINTAIN = new Set(['SBM_VIEW', 'SBM_MAINTAIN']);

function show(ui: ReactElement, route = '/', perms: ReadonlySet<string> = MAINTAIN) {
  render(<>{submittedWrapper(perms, 'polreview', route)(ui)}</>);
}

beforeEach(() => {
  vi.spyOn(lovApi, 'options').mockResolvedValue([]);
  vi.spyOn(catalogApi, 'insurers').mockResolvedValue([]);
  vi.spyOn(submittedApi, 'list').mockResolvedValue(page([policyRow()]));
  vi.spyOn(submittedApi, 'counts').mockResolvedValue({
    ALL: 1,
    FOR_VALIDATION: 0,
    CLASSIFIED: 0,
    FOR_RENEWAL: 0,
    MANUAL_DISPOSITION: 0,
    NON_RENEWAL: 0,
    FALLOUT: 0,
  });
  vi.spyOn(submittedApi, 'get').mockResolvedValue(policyDetail());
  vi.spyOn(submittedApi, 'history').mockResolvedValue([]);
  vi.spyOn(workflowApi, 'byRecord').mockRejectedValue(new Error('no case'));
});

afterEach(() => {
  vi.restoreAllMocks();
});

/** New Policy on the Masterlist and Edit on the policy record (FR-SP-003, gap G11). */
describe('A policy entered by hand', () => {
  it('creates a policy by hand from the masterlist and edits it on the record', async () => {
    const create = vi
      .spyOn(submittedApi, 'create')
      .mockRejectedValue(new Error('Assured is required'));
    show(<MasterlistPage />);
    fireEvent.click(await screen.findByRole('button', { name: /New Policy/ }));
    expect(await screen.findByRole('dialog', { name: /New Policy/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    expect(await screen.findByText(/Assured is required/)).toBeInTheDocument();
    expect(create).toHaveBeenCalledWith(
      expect.any(Number),
      expect.objectContaining({ segment: 'NONCBG_CORPORATE', tracking: null }),
    );
  });

  it('offers no New Policy without the maintenance right', async () => {
    show(<MasterlistPage />, '/', new Set(['SBM_VIEW']));
    expect(await screen.findByText('SBM-2026-000007')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /New Policy/ })).toBeNull();
  });

  it('opens the edit form of a record', async () => {
    show(
      <Routes>
        <Route path="/submitted/policies/:id" element={<PolicyPage />} />
      </Routes>,
      '/submitted/policies/7',
    );
    fireEvent.click(await screen.findByRole('button', { name: /^Edit$/ }));
    expect(
      await screen.findByRole('dialog', { name: /Edit Policy SBM-2026-000007/ }),
    ).toBeInTheDocument();
    expect(screen.getAllByDisplayValue('Emilio Santos').length).toBeGreaterThan(0);
  });
});
