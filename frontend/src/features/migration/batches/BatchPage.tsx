import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { Batch } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel } from '../common/migrationCodes';
import { useDownload } from '../common/useDownload';
import '../migration.css';
import { ReconPanel } from '../reconciliation/ReconPanel';
import { batchActions } from './batchActions';
import { BatchIssues } from './BatchIssues';
import { BatchLogTable, BatchSignoffs } from './BatchRecords';
import { BatchRows } from './BatchRows';
import { BatchProgress, BatchSummary } from './BatchSummary';

type Tab = 'issues' | 'rows' | 'recon' | 'signoffs' | 'log';

const TABS: { id: Tab; label: string }[] = [
  { id: 'issues', label: 'Issues' },
  { id: 'rows', label: 'Rows' },
  { id: 'recon', label: 'Reconciliation' },
  { id: 'signoffs', label: 'Sign-offs' },
  { id: 'log', label: 'Run Log' },
];

function TabBody({ tab, batch }: Readonly<{ tab: Tab; batch: Batch }>) {
  switch (tab) {
    case 'issues':
      return <BatchIssues batchNo={batch.batchNo} />;
    case 'rows':
      return <BatchRows batch={batch} />;
    case 'recon':
      return <ReconPanel batchNo={batch.batchNo} />;
    case 'signoffs':
      return <BatchSignoffs batchNo={batch.batchNo} />;
    default:
      return <BatchLogTable batchNo={batch.batchNo} />;
  }
}

/**
 * A batch (DATA_MIGRATION_DESIGN sections 10 to 13): the steps timeline, the counts, the actions of
 * its status (validate, sign the validation, approve and run the load, reconcile, sign, accept,
 * rerun the rejected rows, roll back) and its issues, rows, reconciliation, sign-offs and run log.
 */
export default function BatchPage() {
  const { batchNo = '' } = useParams();
  const auth = useAuth();
  const download = useDownload();
  const [tab, setTab] = useState<Tab>('issues');
  const [action, setAction] = useState<MigAction>();
  const batch = useQuery({
    queryKey: ['migration', 'batch', batchNo],
    queryFn: () => migrationApi.batch(batchNo),
  });
  const b = batch.data;
  const actions = b === undefined ? [] : batchActions(b, auth.can, auth.user?.username);
  const hasRejects = b !== undefined && b.counts.rejected + b.counts.invalid > 0;
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title={`Batch ${batchNo}`}
        description={b && `Object ${b.objectCode} · ${migLabel(b.environmentClass)}`}
        backTo="/migration/batches"
        actions={
          <span className="mig-actions">
            <MigStatus status={b?.status} />
            {actions.map((a) => (
              <Button
                key={a.id}
                variant={a.primary === true ? 'primary' : 'secondary'}
                onClick={() => setAction(a.action)}
              >
                {a.label}
              </Button>
            ))}
            {hasRejects && (
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                busy={download.busy}
                onClick={() => download.run(() => migrationApi.rejects(batchNo))}
              >
                Rejects
              </Button>
            )}
          </span>
        }
      />
      <ErrorAlert error={batch.error ?? download.error} onRetry={() => void batch.refetch()} />
      {b !== undefined && (
        <>
          <BatchProgress batch={b} />
          <BatchSummary batch={b} />
          <Tabs<Tab> tabs={TABS} active={tab} onChange={setTab} />
          <TabBody tab={tab} batch={b} />
        </>
      )}
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </div>
  );
}
