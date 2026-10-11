import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalUpdatesApi } from '@/api/renewalUpdates';
import { renewalWrapper } from '../testWrapper';
import { AutoValuesCard, RefreshEndorsementsButton } from './AutoValuesCard';
import { criteriaText } from './duplicateCriteria';
import { ManualCreationDialog } from './ManualCreationDialog';

const POTENTIAL =
  'Potential Duplicate Account Detected. A similar account was found based on the configured' +
  ' duplicate validation criteria. Please review before proceeding.';

describe('Create Renewal Account', () => {
  it('shows a potential duplicate with its link, then creates the account on Proceed', async () => {
    const create = vi
      .spyOn(renewalUpdatesApi, 'createManual')
      .mockResolvedValueOnce({
        renewalRef: null,
        message: POTENTIAL,
        duplicates: [
          { ref: 'RNW-2027-000010', exact: false, cancelled: false, criteria: ['CLIENT'] },
        ],
      })
      .mockResolvedValueOnce({ renewalRef: 'RNW-2027-000011', message: null, duplicates: [] });
    const onClose = vi.fn();
    const wrap = renewalWrapper(new Set(['RNW_EXTRACT']), 'mkttl');
    render(wrap(<ManualCreationDialog onClose={onClose} />));
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'INV-100' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create' }));
    expect(await screen.findByText(POTENTIAL)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'RNW-2027-000010' })).toBeInTheDocument();
    expect(screen.getByText('Potential duplicate')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Proceed' }));
    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(create).toHaveBeenLastCalledWith(1, 'INV-100', true);
  });

  it('refuses an exact duplicate', async () => {
    vi.spyOn(renewalUpdatesApi, 'createManual').mockResolvedValue({
      renewalRef: null,
      message: 'The expiring account already has its renewal account RNW-2027-000012',
      duplicates: [
        { ref: 'RNW-2027-000012', exact: true, cancelled: false, criteria: ['EXPIRING_INVOICE'] },
      ],
    });
    const wrap = renewalWrapper(new Set(['RNW_EXTRACT']), 'mkttl');
    render(wrap(<ManualCreationDialog onClose={vi.fn()} />));
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'INV-101' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create' }));
    expect(await screen.findByText('Exact duplicate')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create' })).toBeDisabled();
  });

  it('names the matching criteria in words', () => {
    expect(criteriaText(['CLIENT', 'RISK_CODE', 'PN', 'EXPIRING_POLICY'])).toBe(
      'Client, Risk code, PN number, Expiring policy number',
    );
  });
});

describe('CBG Motor automatic values and Refresh Endorsements', () => {
  it('lists the expiring, renewal and account values with the difference', async () => {
    vi.spyOn(renewalUpdatesApi, 'autoUpdate').mockResolvedValue([
      { field: 'Risk Code', expiring: 'MTR70', renewal: 'MTR22', account: null, difference: null },
      {
        field: 'OD / Theft Coverage',
        expiring: '1000000.00',
        renewal: '900000.00',
        account: '900000.00',
        difference: -100000,
      },
      {
        field: 'New Expiry',
        expiring: '2027-03-01',
        renewal: '2028-03-01',
        account: null,
        difference: null,
      },
    ]);
    const wrap = renewalWrapper(new Set(['RNW_VIEW']));
    render(wrap(<AutoValuesCard renewalRef="RNW-1" />));
    expect(await screen.findByText('OD / Theft Coverage')).toBeInTheDocument();
    expect(screen.getByText('MTR22')).toBeInTheDocument();
    expect(screen.getByText('(100,000.00)')).toBeInTheDocument();
    expect(screen.getByText('01-Mar-2028')).toBeInTheDocument();
  });

  it('refreshes the endorsements for a user who may act on the renewal', async () => {
    const refresh = vi.spyOn(renewalUpdatesApi, 'refreshEndorsements').mockResolvedValue([]);
    const wrap = renewalWrapper(new Set(['RNW_DISPOSE']));
    render(wrap(<RefreshEndorsementsButton renewalRef="RNW-1" />));
    fireEvent.click(screen.getByRole('button', { name: 'Refresh Endorsements' }));
    await waitFor(() => expect(refresh).toHaveBeenCalledWith(1, 'RNW-1'));
  });

  it('hides Refresh Endorsements from a read-only user', () => {
    const wrap = renewalWrapper(new Set(['RNW_VIEW']));
    render(wrap(<RefreshEndorsementsButton renewalRef="RNW-1" />));
    expect(screen.queryByRole('button', { name: 'Refresh Endorsements' })).toBeNull();
  });
});
