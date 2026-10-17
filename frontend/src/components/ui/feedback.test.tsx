import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { mastersApi } from '@/api/masters';
import { CurrencySelect } from '@/components/broking/Lookups';
import { EmptyRow } from './EmptyRow';
import { LoadingPanel } from './LoadingPanel';
import { toastText } from './toastText';

describe('feedback standard', () => {
  it('writes toasts as one capitalised line without a closing full stop', () => {
    expect(toastText('journal posted.')).toBe('Journal posted');
    expect(toastText('  Saved ')).toBe('Saved');
    expect(toastText('Your session has ended. Please sign in again.')).toBe(
      'Your session has ended. Please sign in again.',
    );
    expect(toastText('Saved...')).toBe('Saved...');
  });

  it('shows a skeleton panel while a record loads and keeps the headers of an empty grid', () => {
    render(
      <>
        <LoadingPanel lines={2} />
        <table>
          <thead>
            <tr>
              <th>Coverage</th>
              <th>Limit</th>
            </tr>
          </thead>
          <tbody>
            <EmptyRow columns={2} message="No coverage yet." />
          </tbody>
        </table>
      </>,
    );
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Coverage' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'No coverage yet.' })).toHaveAttribute('colspan', '2');
  });

  it('lets a currency be typed when the list cannot be read', async () => {
    vi.spyOn(mastersApi, 'currencies').mockRejectedValue(new Error('forbidden'));
    const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queries}>
        <CurrencySelect id="ccy" value="USD" onChange={vi.fn()} />
      </QueryClientProvider>,
    );
    await waitFor(() => expect(screen.queryByRole('combobox')).toBeNull());
    expect(screen.getByRole('textbox')).toHaveValue('USD');
  });
});
