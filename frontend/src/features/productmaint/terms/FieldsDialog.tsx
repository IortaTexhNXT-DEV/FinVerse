import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { pmTermsApi } from '@/api/pmTerms';
import type { TermsRecordType, TermsView } from '@/api/pmTerms';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';

interface FieldsDialogProps {
  type: TermsRecordType;
  id: number;
  view: TermsView;
  onSaved: (view: TermsView) => void;
  onClose: () => void;
}

function without(list: string[], key: string): string[] {
  return list.filter((k) => k !== key);
}

/**
 * Chooses the comparison fields (BDOI FRS FRPM.006.02, FRPM.012.02): the rows shown in the
 * comparative table and the fields sent to the client in the proposal and the exported table.
 */
export function FieldsDialog({ type, id, view, onSaved, onClose }: Readonly<FieldsDialogProps>) {
  const [shown, setShown] = useState<string[]>(view.table.shown);
  const [client, setClient] = useState<string[]>(view.table.clientFields);
  const save = useMutation({
    mutationFn: () =>
      pmTermsApi.fields(
        type,
        id,
        view.table.fields.filter((k) => shown.includes(k)),
        view.table.fields.filter((k) => client.includes(k) && shown.includes(k)),
      ),
    onSuccess: (saved) => {
      onSaved(saved);
      onClose();
    },
  });
  return (
    <Modal
      title="Choose Comparison Fields"
      open
      size="md"
      onClose={onClose}
      helper="The fields shown are the rows of the table; the client fields go into the proposal."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={shown.length === 0} busy={save.isPending} onClick={() => save.mutate()}>
            Save Fields
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <table className="table">
        <thead>
          <tr>
            <th>Field</th>
            <th>Shown</th>
            <th>Sent to Client</th>
          </tr>
        </thead>
        <tbody>
          {view.table.fields.map((key) => (
            <tr key={key}>
              <td>{view.labels[key] ?? key}</td>
              <td>
                <input
                  type="checkbox"
                  aria-label={`Show ${view.labels[key] ?? key}`}
                  checked={shown.includes(key)}
                  onChange={(e) =>
                    setShown(e.target.checked ? [...shown, key] : without(shown, key))
                  }
                />
              </td>
              <td>
                <input
                  type="checkbox"
                  aria-label={`Send ${view.labels[key] ?? key} to the client`}
                  checked={client.includes(key)}
                  disabled={!shown.includes(key)}
                  onChange={(e) =>
                    setClient(e.target.checked ? [...client, key] : without(client, key))
                  }
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Modal>
  );
}
