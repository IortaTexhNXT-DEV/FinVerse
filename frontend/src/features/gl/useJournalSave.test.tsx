import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ApiError } from '@/api/client';
import { glApi } from '@/api/gl';
import type { Journal, JournalInput } from '@/api/gl';
import { newJournalValues, toJournalInput } from './journalForm';
import { useJournalSave } from './useJournalSave';

const body: JournalInput = toJournalInput(1, newJournalValues(1, 'PHP'));
const draft = { id: 41, batchNo: 'JV-HO-2026-000041', status: 'DRAFT' } as Journal;
const pending = { ...draft, status: 'PENDING_APPROVAL' } as Journal;
const costCentreMissing = new ApiError(422, {
  detail: 'Line 1: Cost centre is mandatory for account 5613',
  code: 'COST_CENTER_REQUIRED',
});

function wrapper({ children }: Readonly<{ children: ReactNode }>) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('journal save & submit', () => {
  afterEach(() => vi.restoreAllMocks());

  it('keeps the draft of a failed submission and updates it on retry instead of creating another', async () => {
    const create = vi.spyOn(glApi, 'createJournal').mockResolvedValue(draft);
    const update = vi.spyOn(glApi, 'updateJournal').mockResolvedValue(draft);
    const submit = vi
      .spyOn(glApi, 'submitJournal')
      .mockRejectedValueOnce(costCentreMissing)
      .mockRejectedValueOnce(costCentreMissing)
      .mockResolvedValueOnce(pending);
    const onSaved = vi.fn();
    const { result } = renderHook(() => useJournalSave(undefined, onSaved), { wrapper });

    act(() => result.current.save.mutate({ mode: 'submit', body }));
    await waitFor(() => expect(result.current.save.isError).toBe(true));
    expect(result.current.save.error).toBe(costCentreMissing);
    expect(result.current.savedDraft).toEqual(draft);
    expect(result.current.draftId).toBe(41);

    for (const mode of ['submit', 'submit'] as const) {
      act(() => result.current.save.mutate({ mode, body }));
      await waitFor(() => expect(result.current.save.isPending).toBe(false));
    }

    expect(create).toHaveBeenCalledTimes(1);
    expect(update).toHaveBeenCalledTimes(2);
    expect(update).toHaveBeenCalledWith(41, body);
    expect(submit).toHaveBeenCalledTimes(3);
    expect(submit).toHaveBeenLastCalledWith(41);
    expect(onSaved).toHaveBeenCalledTimes(1);
    expect(onSaved.mock.calls[0]?.[0]).toEqual(pending);
  });

  it('updates the voucher being edited and never creates one', async () => {
    const create = vi.spyOn(glApi, 'createJournal');
    const update = vi.spyOn(glApi, 'updateJournal').mockResolvedValue(draft);
    const onSaved = vi.fn();
    const { result } = renderHook(() => useJournalSave(41, onSaved), { wrapper });

    act(() => result.current.save.mutate({ mode: 'draft', body }));
    await waitFor(() => expect(onSaved).toHaveBeenCalled());

    expect(create).not.toHaveBeenCalled();
    expect(update).toHaveBeenCalledWith(41, body);
    expect(result.current.savedDraft).toBeUndefined();
  });
});
