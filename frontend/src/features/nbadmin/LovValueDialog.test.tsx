import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { LovType } from '@/api/lov';
import { ToastContext } from '@/components/ui/toastContext';
import { formatDate, today } from '@/utils/format';
import { LovValueDialog } from './LovValueDialog';

describe('LovValueDialog', () => {
  it('takes the effective dates with the dd-MMM-yyyy date picker of BIBS', () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ToastContext.Provider
          value={{ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }}
        >
          <LovValueDialog
            type={{ code: 'ACSL_CASE_TYPE', name: 'ACSL case type' } as LovType}
            nextOrder={60}
            onClose={vi.fn()}
          />
        </ToastContext.Provider>
      </QueryClientProvider>,
    );
    const from = screen.getByLabelText(/^Effective from/);
    expect(from).toHaveValue(formatDate(today()));
    expect(from).toHaveAttribute('placeholder', 'dd-MMM-yyyy');
    expect(screen.getByLabelText(/^Effective to/)).toHaveAttribute('placeholder', 'dd-MMM-yyyy');
  });
});
