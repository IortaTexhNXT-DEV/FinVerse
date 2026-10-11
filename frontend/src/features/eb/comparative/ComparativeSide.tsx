import { useState } from 'react';
import { ebMarketApi } from '@/api/ebMarket';
import type { ComparativeView } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { useEbMutation } from '../common/useEbMutation';

type Decision = ComparativeView['decisions'][number];

const DECISION_COLUMNS: Column<Decision>[] = [
  { key: 'role', header: 'Role', render: (d) => d.role },
  {
    key: 'who',
    header: 'Signatory',
    render: (d) => (
      <CellStack main={<UserName login={d.signatory} />} sub={formatDateTime(d.decidedAt)} />
    ),
  },
  {
    key: 'decision',
    header: 'Decision',
    kind: 'status',
    render: (d) => <StatusBadge status={d.decision} />,
  },
  { key: 'remarks', header: 'Remarks', render: (d) => d.remarks ?? '' },
];

/** The sign-off and threshold decisions recorded on the comparative. */
export function DecisionsCard({ view }: Readonly<{ view: ComparativeView }>) {
  return (
    <Card title="Approvals">
      <DataTable<Decision>
        rows={view.decisions}
        rowKey={(d) => `${d.role}-${d.decidedAt}`}
        columns={DECISION_COLUMNS}
        emptyMessage="No decision yet"
      />
    </Card>
  );
}

/**
 * The comment thread of the comparative: internal notes, and the client's comments as received by
 * e-mail, recorded by the AO.
 */
export function CommentsCard({ view }: Readonly<{ view: ComparativeView }>) {
  const { can } = useAuth();
  const [text, setText] = useState('');
  const [client, setClient] = useState(false);
  const add = useEbMutation(
    (c, v: { text: string; client: boolean }) => ebMarketApi.comment(c, view.comparative.id, v),
    'Comment added',
    () => {
      setText('');
      setClient(false);
    },
  );
  return (
    <Card title="Comments">
      <div className="stack">
        <DataTable<ComparativeView['comments'][number]>
          caption="Comments"
          rows={view.comments}
          rowKey={(c) => c.id}
          emptyMessage="No comment yet"
          columns={[
            {
              key: 'by',
              header: 'By',
              render: (c) => (
                <CellStack
                  main={<UserName login={c.createdBy} />}
                  sub={formatDateTime(c.createdAt)}
                />
              ),
            },
            {
              key: 'from',
              header: 'From',
              kind: 'status',
              render: (c) => (c.authorKind === 'CLIENT' ? <Tag tone="info">Client</Tag> : 'Broker'),
            },
            { key: 'text', header: 'Comment', render: (c) => c.text },
          ]}
        />
        {can('EB_VIEW') && (
          <>
            <ErrorAlert error={add.error} />
            <Field label="New Comment">
              {(id) => (
                <textarea
                  id={id}
                  className="textarea"
                  rows={2}
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
              )}
            </Field>
            <div className="eb-actions">
              {can('EB_MARKET') && (
                <label className="checkbox">
                  <input
                    type="checkbox"
                    checked={client}
                    onChange={(e) => setClient(e.target.checked)}
                  />
                  The client's comment
                </label>
              )}
              <Button
                size="sm"
                busy={add.isPending}
                disabled={text.trim() === ''}
                onClick={() => add.mutate({ text: text.trim(), client })}
              >
                Add Comment
              </Button>
            </div>
          </>
        )}
      </div>
    </Card>
  );
}
