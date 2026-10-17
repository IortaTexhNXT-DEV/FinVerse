import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { glApi } from '@/api/gl';
import type { Journal, JournalInput } from '@/api/gl';

export type SaveMode = 'draft' | 'submit';

export interface SaveRequest {
  mode: SaveMode;
  body: JournalInput;
}

/**
 * Saves a manual voucher and, for "Save & submit", submits it.
 *
 * The first save of a new voucher creates the draft; the hook keeps that draft, so every later
 * save on the same screen (a retry after a rejected submission, or "Save draft") updates and
 * resubmits the same draft instead of creating another one. `savedDraft` is set once a draft
 * exists that this screen created, so the page can tell the user that only submission failed.
 */
export function useJournalSave(editingId: number | undefined, onSaved: (j: Journal) => unknown) {
  const queryClient = useQueryClient();
  const [savedDraft, setSavedDraft] = useState<Journal>();
  const draftId = savedDraft?.id ?? editingId;

  const save = useMutation({
    mutationFn: async ({ mode, body }: SaveRequest) => {
      const saved =
        draftId === undefined
          ? await glApi.createJournal(body)
          : await glApi.updateJournal(draftId, body);
      setSavedDraft(saved);
      return mode === 'submit' ? glApi.submitJournal(saved.id) : saved;
    },
    onSuccess: onSaved,
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['journals'] }),
  });
  return { save, draftId, savedDraft: editingId === undefined ? savedDraft : undefined };
}
