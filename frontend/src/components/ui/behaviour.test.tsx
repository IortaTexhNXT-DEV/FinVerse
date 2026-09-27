import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { Link, MemoryRouter, Route, Routes, useSearchParams } from 'react-router-dom';
import { ApiError, NETWORK_STATUS } from '@/api/client';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import {
  groupByDay,
  kindOf,
  referenceOf,
  relativeTime,
} from '@/features/workspace/notificationLogic';
import { ConfirmDialog } from './ConfirmDialog';
import { ErrorAlert } from './ErrorAlert';
import { FilterChips } from './FilterChips';
import { PageHeader } from './PageHeader';
import { StatusPage } from './StatusPage';
import { Tabs } from './Tabs';
import { useTabParam } from './useTabParam';
import { useUnsavedChangesGuard } from './useUnsavedChangesGuard';

describe('confirmation dialog', () => {
  it('names the record and the effect and needs the reason before confirming', async () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        title="Cancel Voucher"
        record="DV-2026-000003"
        effect="The check is voided and the payable reopens."
        confirmLabel="Cancel Voucher"
        reason="required"
        destructive
        onConfirm={onConfirm}
        onClose={vi.fn()}
      />,
    );
    expect(screen.getByText('DV-2026-000003')).toBeInTheDocument();
    expect(screen.getByText('The check is voided and the payable reopens.')).toBeInTheDocument();
    const confirm = screen.getByRole('button', { name: 'Cancel Voucher' });
    expect(confirm).toHaveClass('btn-danger');
    await userEvent.click(confirm);
    expect(screen.getByRole('alert')).toHaveTextContent('Enter the reason.');
    expect(onConfirm).not.toHaveBeenCalled();
    await userEvent.type(screen.getByLabelText('Reason'), 'Wrong payee');
    await userEvent.click(confirm);
    expect(onConfirm).toHaveBeenCalledWith('Wrong payee');
  });
});

describe('error banner', () => {
  it('shows the business message with the error code and the correlation id', () => {
    render(
      <ErrorAlert
        error={
          new ApiError(422, {
            code: 'VOUCHER_POSTED',
            detail: 'The voucher is already posted (DISID.012).',
            correlationId: 'c0ffee',
          })
        }
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('The voucher is already posted.');
    expect(screen.getByText('Reference: VOUCHER_POSTED · c0ffee')).toBeInTheDocument();
    expect(screen.queryByText(/422/)).not.toBeInTheDocument();
  });

  it('offers Retry for a network failure, in business language', async () => {
    const onRetry = vi.fn();
    render(<ErrorAlert error={new ApiError(NETWORK_STATUS, {})} onRetry={onRetry} />);
    expect(screen.getByRole('alert')).toHaveTextContent('The system could not be reached.');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalled();
  });
});

describe('status pages', () => {
  it('shows a themed page with a way back', () => {
    render(
      <MemoryRouter>
        <StatusPage kind="notFound" />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: 'Page Not Found' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Go Back' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Homepage' })).toHaveAttribute('href', '/');
  });
});

describe('filters and search', () => {
  it('lists the active filters as removable chips with Clear All', async () => {
    const remove = vi.fn();
    const clear = vi.fn();
    render(
      <FilterChips
        filters={[{ key: 's', label: 'Status: Unapplied', onRemove: remove }]}
        onClearAll={clear}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Remove Status: Unapplied' }));
    await userEvent.click(screen.getByRole('button', { name: 'Clear All' }));
    expect(remove).toHaveBeenCalled();
    expect(clear).toHaveBeenCalled();
  });

  function ListPage() {
    const [params] = useSearchParams();
    const [searched, setSearched] = useState('');
    return (
      <>
        <WorklistToolbar placeholder="Search Reference" onSearch={setSearched} />
        <output aria-label="searched">{searched}</output>
        <output aria-label="url">{params.get('q') ?? ''}</output>
      </>
    );
  }

  it('keeps the search in the URL and restores it when the list opens again', async () => {
    render(
      <MemoryRouter initialEntries={['/list?q=PAY-2026']}>
        <Routes>
          <Route path="/list" element={<ListPage />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByLabelText('searched')).toHaveTextContent('PAY-2026');
    expect(screen.getByText('Search: PAY-2026')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Remove Search: PAY-2026' }));
    expect(screen.getByLabelText('url')).toHaveTextContent('');
    await userEvent.type(screen.getByLabelText('Search Reference'), 'UNP-1');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByLabelText('url')).toHaveTextContent('UNP-1');
  });
});

describe('navigation', () => {
  it('sets the browser tab title from the page title', () => {
    render(
      <MemoryRouter>
        <PageHeader title="Unapplied Payments" />
      </MemoryRouter>,
    );
    expect(document.title).toBe('Unapplied Payments · BIBS');
  });

  function Form() {
    const [dirty, setDirty] = useState(false);
    const dialog = useUnsavedChangesGuard(dirty);
    return (
      <>
        <input aria-label="Name" onChange={() => setDirty(true)} />
        <Link to="/other">Other page</Link>
        {dialog}
      </>
    );
  }

  it('asks before leaving a form with unsaved changes', async () => {
    render(
      <MemoryRouter initialEntries={['/form']}>
        <Routes>
          <Route path="/form" element={<Form />} />
          <Route path="/other" element={<p>Other page opened</p>} />
        </Routes>
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Name'), 'x');
    await userEvent.click(screen.getByRole('link', { name: 'Other page' }));
    expect(screen.getByRole('dialog', { name: 'Leave Without Saving?' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Leave Page' }));
    expect(screen.getByText('Other page opened')).toBeInTheDocument();
  });
});

describe('notifications', () => {
  const now = new Date('2026-09-26T04:00:00Z');
  const n = (id: number, createdAt: string, title: string, body?: string) => ({
    id,
    title,
    body,
    createdAt,
    read: false,
  });

  it('groups by day, newest first', () => {
    const groups = groupByDay(
      [
        n(1, '2026-09-24T02:00:00Z', 'Old'),
        n(2, '2026-09-26T03:00:00Z', 'New'),
        n(3, '2026-09-25T03:00:00Z', 'Yesterday'),
      ],
      now,
    );
    expect(groups.map((g) => g.label)).toEqual(['Today', 'Yesterday', '24-Sep-2026']);
  });

  it('gives the relative time, the record reference and the kind', () => {
    expect(relativeTime('2026-09-26T03:55:00Z', now)).toBe('5 min ago');
    expect(relativeTime('2026-09-26T01:00:00Z', now)).toBe('3 h ago');
    expect(referenceOf(n(1, '', 'Voucher returned', 'DV-2026-000003 needs a new payee'))).toBe(
      'DV-2026-000003',
    );
    expect(kindOf({ title: 'Quotation returned to you' })).toBe('returned');
    expect(kindOf({ title: 'Account assigned to you' })).toBe('assigned');
  });
});

describe('tabs', () => {
  function Strip() {
    const [tab, setTab] = useState<'a' | 'b' | 'c'>('a');
    return (
      <Tabs
        tabs={[
          { id: 'a', label: 'Details' },
          { id: 'b', label: 'Documents', count: 3 },
          { id: 'c', label: 'History' },
        ]}
        active={tab}
        onChange={setTab}
      />
    );
  }

  it('shows counts and moves with the arrow keys, Home and End', async () => {
    render(<Strip />);
    expect(screen.getByLabelText('3 items')).toHaveTextContent('3');
    await userEvent.click(screen.getByRole('tab', { name: 'Details' }));
    await userEvent.keyboard('{ArrowRight}');
    expect(screen.getByRole('tab', { name: /Documents/ })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: /Documents/ })).toHaveFocus();
    await userEvent.keyboard('{End}');
    expect(screen.getByRole('tab', { name: 'History' })).toHaveAttribute('aria-selected', 'true');
    await userEvent.keyboard('{ArrowRight}');
    expect(screen.getByRole('tab', { name: 'Details' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'History' })).toHaveAttribute('tabindex', '-1');
  });
});

describe('tab in the URL', () => {
  function RecordTabs() {
    const [tab, setTab] = useTabParam(['details', 'kyc', 'history'] as const, 'details');
    const [params] = useSearchParams();
    return (
      <>
        <Tabs
          tabs={[
            { id: 'details', label: 'Details' },
            { id: 'kyc', label: 'KYC' },
            { id: 'history', label: 'History' },
          ]}
          active={tab}
          onChange={setTab}
        />
        <output aria-label="query">{params.toString()}</output>
      </>
    );
  }

  it('opens the tab named in the link and keeps the chosen tab in the URL', async () => {
    render(
      <MemoryRouter initialEntries={['/crm/clients/9?tab=history']}>
        <Routes>
          <Route path="/crm/clients/:id" element={<RecordTabs />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByRole('tab', { name: 'History' })).toHaveAttribute('aria-selected', 'true');
    await userEvent.click(screen.getByRole('tab', { name: 'KYC' }));
    expect(screen.getByLabelText('query')).toHaveTextContent('tab=kyc');
    await userEvent.click(screen.getByRole('tab', { name: 'Details' }));
    expect(screen.getByLabelText('query')).toHaveTextContent('');
  });
});
