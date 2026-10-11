import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { renewalUpdatesApi } from '@/api/renewalUpdates';
import type { DuplicateMatch, ManualCreationResult } from '@/api/renewalUpdates';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { criteriaText } from './duplicateCriteria';

function matchLabel(m: DuplicateMatch): string {
  if (m.exact) return 'Exact duplicate';
  return m.cancelled ? 'Cancelled' : 'Potential duplicate';
}

function Duplicates({ rows }: Readonly<{ rows: DuplicateMatch[] }>) {
  return (
    <DataTable<DuplicateMatch>
      rows={rows}
      rowKey={(m) => m.ref ?? criteriaText(m.criteria)}
      emptyMessage="No matching renewal account"
      columns={[
        {
          key: 'ref',
          header: 'Renewal account',
          kind: 'code',
          render: (m) =>
            m.ref ? (
              <Link to={`/renewal/candidates/${encodeURIComponent(m.ref)}`}>{m.ref}</Link>
            ) : (
              ''
            ),
        },
        { key: 'kind', header: 'Match', render: matchLabel },
        { key: 'criteria', header: 'Matching on', render: (m) => criteriaText(m.criteria) },
      ]}
    />
  );
}

/**
 * Create Renewal Account (FRRN.005.01, FRRN.006.01): the renewal account of one expiring account,
 * after the duplicate checking; a potential duplicate is shown with links to the matching renewal
 * accounts and the user proceeds or cancels; an exact duplicate cannot be created.
 */
export function ManualCreationDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [invoiceNo, setInvoiceNo] = useState('');
  const [outcome, setOutcome] = useState<ManualCreationResult | null>(null);
  const create = useMutation({
    mutationFn: (confirmed: boolean) =>
      renewalUpdatesApi.createManual(companyId, invoiceNo.trim(), confirmed),
    onSuccess: async (r) => {
      if (r.renewalRef === null) {
        setOutcome(r);
        return;
      }
      toast.success(`Renewal account ${r.renewalRef} created`);
      if (r.message) toast.info(r.message);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
      onClose();
    },
  });
  const exact = outcome?.duplicates.some((m) => m.exact) ?? false;
  const potential = outcome !== null && !exact;
  return (
    <Modal
      open
      size="lg"
      title="Create Renewal Account"
      onClose={onClose}
      helper="The renewal account is checked, classified and routed like a generated one."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={create.isPending}
            disabled={invoiceNo.trim() === '' || exact}
            onClick={() => create.mutate(potential)}
          >
            {potential ? 'Proceed' : 'Create'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={create.error} />
        <Field label="Invoice number of the expiring account" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={invoiceNo}
              onChange={(e) => {
                setInvoiceNo(e.target.value);
                setOutcome(null);
              }}
            />
          )}
        </Field>
        {outcome && (
          <Notice
            tone={exact ? 'error' : 'warning'}
            title={exact ? 'Duplicate account' : undefined}
          >
            {outcome.message}
          </Notice>
        )}
        {outcome && <Duplicates rows={outcome.duplicates} />}
      </div>
    </Modal>
  );
}
