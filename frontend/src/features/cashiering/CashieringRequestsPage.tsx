import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { CollectorRequestsPanel, ReversalsPanel, ValidationsPanel } from './RequestPanels';
import { requestsApi } from './requestsApi';

type Queue = 'COLLECTORS' | 'VALIDATIONS' | 'REVERSALS';
type View = 'OPEN' | 'DONE';

const QUEUES: readonly Queue[] = ['COLLECTORS', 'VALIDATIONS', 'REVERSALS'];

function initialQueue(value: string | null): Queue {
  if (value === 'REVERSALS' || value === 'VALIDATIONS') {
    return value;
  }
  return 'COLLECTORS';
}

/**
 * Incoming requests from other modules (wave C1-C): collector requests on unapplied items from
 * Collections (BRCLXN.030-033: apply to an invoice, refund, reclass, transfer), refund validations
 * from Payment Requests (MKT 1.11.0: is the premium back in the unapplied list with a new AR?) and
 * payment reversals from ACSL (ACSL 2.6.0-2.6.1), each with its open and decided items.
 */
export default function CashieringRequestsPage() {
  const companyId = useCompanyId();
  const [params] = useSearchParams();
  const [queue, setQueue] = useState<Queue>(initialQueue(params.get('tab')));
  const [view, setView] = useState<View>('OPEN');
  const counts = useQuery({
    queryKey: ['cashiering', 'requests', 'counts', companyId],
    queryFn: () => requestsApi.counts(companyId),
    enabled: companyId > 0,
  });
  const n = counts.data;
  const labels: Record<Queue, string> = {
    COLLECTORS: `Collector Requests (${n?.collectorRequests ?? 0})`,
    VALIDATIONS: `Refund Validations (${n?.refundValidations ?? 0})`,
    REVERSALS: `Payment Reversals (${n?.paymentReversals ?? 0})`,
  };
  const open = view === 'OPEN';
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Cashiering"
        title="Incoming Requests"
        description="What Collections, Payment Requests and ACSL ask of Cashiering: dispositions of unapplied payments, refund validations and payment reversals."
      />
      <Card flush>
        <div>
          <Tabs
            tabs={QUEUES.map((q) => ({ id: q, label: labels[q] }))}
            active={queue}
            onChange={(q) => {
              setQueue(q);
              setView('OPEN');
            }}
          />
          <Tabs
            tabs={[
              { id: 'OPEN' as const, label: 'To Do' },
              { id: 'DONE' as const, label: 'Decided' },
            ]}
            active={view}
            onChange={setView}
          />
          {queue === 'COLLECTORS' && (
            <CollectorRequestsPanel key={view} companyId={companyId} open={open} />
          )}
          {queue === 'VALIDATIONS' && (
            <ValidationsPanel key={view} companyId={companyId} open={open} />
          )}
          {queue === 'REVERSALS' && <ReversalsPanel key={view} companyId={companyId} open={open} />}
        </div>
      </Card>
    </div>
  );
}
