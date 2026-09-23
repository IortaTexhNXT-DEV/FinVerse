import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { underwritingApi } from '@/api/underwriting';
import type { Endorsement, Policy } from '@/api/underwriting';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import { DebitNote } from './DebitNote';
import { WorkflowActions } from './WorkflowActions';

/** Endorsement history of a policy with maker / checker actions and each debit or credit note. */
export function EndorsementHistory({
  policy,
  endorsements,
}: Readonly<{ policy: Policy; endorsements: Endorsement[] }>) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<Endorsement | null>(null);
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['endorsements', policy.id] });
    await queryClient.invalidateQueries({ queryKey: ['policy', policy.id] });
    await queryClient.invalidateQueries({ queryKey: ['policies'] });
  };

  return (
    <Card title="Endorsement history" flush>
      <DataTable<Endorsement>
        rows={endorsements}
        rowKey={(e) => e.id}
        emptyMessage="No endorsements."
        caption="Endorsements"
        columns={[
          { key: 'no', header: 'Endorsement', render: (e) => <strong>{e.documentNo}</strong> },
          { key: 'type', header: 'Type', render: (e) => humanize(e.type) },
          { key: 'eff', header: 'Effective', render: (e) => formatDate(e.effectiveDate) },
          { key: 'desc', header: 'Description', render: (e) => e.description },
          {
            key: 'due',
            header: 'Total due',
            numeric: true,
            render: (e) => <Amount value={e.premium.totalDue} />,
          },
          { key: 'dn', header: 'DN / CN', render: (e) => e.document.debitNoteNo ?? '' },
          {
            key: 'st',
            header: 'Status',
            render: (e) => <StatusBadge status={e.document.status} />,
          },
          {
            key: 'act',
            header: 'Actions',
            render: (e) => (
              <div className="row">
                <Button size="sm" variant="ghost" onClick={() => setSelected(e)}>
                  View
                </Button>
                <WorkflowActions
                  compact
                  label={e.documentNo}
                  facts={e.document}
                  handlers={{
                    submit: () => underwritingApi.submitEndorsement(e.id),
                    discard: () => underwritingApi.discardEndorsement(e.id),
                    approve: (date) => underwritingApi.approveEndorsement(e.id, date),
                    reject: (reason) => underwritingApi.rejectEndorsement(e.id, reason),
                    done: refresh,
                  }}
                />
              </div>
            ),
          },
        ]}
      />
      <Modal
        title={selected === null ? '' : `Endorsement ${selected.documentNo}`}
        open={selected !== null}
        onClose={() => setSelected(null)}
      >
        {selected !== null && (
          <DebitNote
            policy={policy}
            document={selected.document}
            premium={selected.premium}
            reference={selected.documentNo}
          />
        )}
      </Modal>
    </Card>
  );
}
