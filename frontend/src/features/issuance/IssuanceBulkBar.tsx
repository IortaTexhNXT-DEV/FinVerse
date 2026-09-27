import { useMutation } from '@tanstack/react-query';
import { FileBadge, Send } from 'lucide-react';
import { useState } from 'react';
import { issuanceApi } from '@/api/issuance';
import type { IssuanceRow, IssuanceTab, Outcome } from '@/api/issuance';
import { useAuth } from '@/auth/authContext';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import type { RowSelection } from '@/components/broking/rowSelection';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { rowKeyOf } from './issuanceLogic';

/** Sends the selected e-policies with the standard password policy (BRNB.077/035). */
export function DispatchManyDialog({
  count,
  busy,
  error,
  onSend,
  onClose,
}: Readonly<{
  count: number;
  busy: boolean;
  error: unknown;
  onSend: (hint: string) => void;
  onClose: () => void;
}>) {
  const [hint, setHint] = useState('');
  return (
    <Modal
      open
      title={`Send ${count} E-policies`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="primary" busy={busy} onClick={() => onSend(hint)}>
            Send via Email
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p className="muted">
          Each e-policy is sent encrypted to the account contact with the e-policy template; the
          password follows in a separate e-mail.
        </p>
        <Field label="Password hint" hint="Leave empty to use the configured hint.">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={300}
              value={hint}
              onChange={(e) => setHint(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Bulk actions of the Issuance Workbench: generate the Insurance Advice of the selected mortgaged
 * accounts (BRNB.070) and send the selected e-policies to their clients (BRNB.077).
 */
export function IssuanceBulkBar({
  tab,
  rows,
  selection,
  onChanged,
}: Readonly<{
  tab: IssuanceTab;
  rows: IssuanceRow[];
  selection: RowSelection;
  onChanged: () => void;
}>) {
  const { can } = useAuth();
  const [dispatching, setDispatching] = useState(false);
  const [results, setResults] = useState<{ title: string; items: Outcome[] } | null>(null);
  const chosen = rows.filter((r) => selection.keys.includes(rowKeyOf(r)));
  const finish = (title: string, items: Outcome[]) => {
    setDispatching(false);
    selection.clear();
    setResults({ title, items });
    onChanged();
  };
  const generate = useMutation({
    mutationFn: () => issuanceApi.generateAdvices(chosen.map((r) => r.arn)),
    onSuccess: (items) => finish('Generate Insurance Advice', items),
  });
  const dispatch = useMutation({
    mutationFn: (hint: string) =>
      issuanceApi.dispatchMany(
        chosen.flatMap((r) => (r.epolicyId === undefined ? [] : [r.epolicyId])),
        hint.trim() || undefined,
      ),
    onSuccess: (items) => finish('Send E-policies', items),
  });
  return (
    <>
      <ErrorAlert error={generate.error} />
      {tab === 'IA_TO_GENERATE' && can('EPOLICY_MANAGE') && (
        <Button
          variant="primary"
          icon={<FileBadge size={16} />}
          disabled={chosen.length === 0}
          busy={generate.isPending}
          onClick={() => generate.mutate()}
        >
          Generate Insurance Advice
        </Button>
      )}
      {tab === 'READY_TO_DISPATCH' && can('EPOLICY_SEND') && (
        <Button
          variant="primary"
          icon={<Send size={16} />}
          disabled={chosen.length === 0}
          onClick={() => setDispatching(true)}
        >
          Send E-policies
        </Button>
      )}
      {dispatching && (
        <DispatchManyDialog
          count={chosen.length}
          busy={dispatch.isPending}
          error={dispatch.error}
          onSend={(hint) => dispatch.mutate(hint)}
          onClose={() => setDispatching(false)}
        />
      )}
      {results && (
        <ItemResultsDialog
          title={results.title}
          results={results.items.map((o) => ({
            reference: o.reference,
            ok: o.ok,
            message: o.message,
          }))}
          onClose={() => setResults(null)}
        />
      )}
    </>
  );
}
