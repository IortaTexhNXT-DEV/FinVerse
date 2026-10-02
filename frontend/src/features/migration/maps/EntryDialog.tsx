import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { EntryAction, MapEntryInput } from '@/api/migration';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { ACTION_LABEL, ACTIONS, SOURCES } from './entryCodes';
import { BRAND } from '@/branding';

/** Adds or changes an entry of a draft code map version. */
export function EntryDialog({
  versionId,
  entryId,
  initial,
  onClose,
}: Readonly<{
  versionId: number;
  entryId: number | undefined;
  initial: MapEntryInput;
  onClose: () => void;
}>) {
  const client = useQueryClient();
  const [entry, setEntry] = useState<MapEntryInput>(initial);
  const set = (next: Partial<MapEntryInput>) => setEntry((e) => ({ ...e, ...next }));
  const save = useMutation({
    mutationFn: () =>
      entryId === undefined
        ? migrationApi.addEntry(versionId, entry)
        : migrationApi.updateEntry(versionId, entryId, entry),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['migration'] });
      onClose();
    },
  });
  return (
    <Modal
      title={entryId === undefined ? 'Add Entry' : 'Edit Entry'}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="primary" busy={save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Source" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={entry.sourceSystem}
              onChange={(e) => set({ sourceSystem: e.target.value })}
            >
              {SOURCES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Legacy code" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={entry.legacyCode}
              onChange={(e) => set({ legacyCode: e.target.value })}
            />
          )}
        </Field>
        <Field label="Legacy description">
          {(id) => (
            <input
              id={id}
              className="input"
              value={entry.legacyDescription ?? ''}
              onChange={(e) => set({ legacyDescription: e.target.value })}
            />
          )}
        </Field>
        <Field label="Action" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={entry.action}
              onChange={(e) => set({ action: e.target.value as EntryAction })}
            >
              {ACTIONS.map((a) => (
                <option key={a} value={a}>
                  {ACTION_LABEL[a]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={`${BRAND.product} value`} required={entry.action === 'MAP'}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={entry.targetCode ?? ''}
              onChange={(e) => set({ targetCode: e.target.value })}
            />
          )}
        </Field>
        <Field
          label="Condition column"
          hint="Only for a conditional entry, for example insurer_code"
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={entry.qualifier ?? ''}
              onChange={(e) => set({ qualifier: e.target.value })}
            />
          )}
        </Field>
        <Field label="Condition value">
          {(id) => (
            <input
              id={id}
              className="input"
              value={entry.qualifierValue ?? ''}
              onChange={(e) => set({ qualifierValue: e.target.value })}
            />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <input
              id={id}
              className="input"
              value={entry.remarks ?? ''}
              onChange={(e) => set({ remarks: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
