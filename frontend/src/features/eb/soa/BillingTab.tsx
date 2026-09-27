import { useQuery } from '@tanstack/react-query';
import { FileInput } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import type { ProgrammeInvoice } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { SoaReceiveDialog } from './SoaReceiveDialog';
import { SoaTable } from './SoaTable';

const INVOICE_COLUMNS: Column<ProgrammeInvoice>[] = [
  { key: 'no', header: 'Invoice', kind: 'code', render: (i) => i.invoiceNo },
  { key: 'arn', header: 'Account', kind: 'code', render: (i) => i.arn },
  { key: 'insurer', header: 'Insurer', kind: 'code', render: (i) => i.insurerCode },
  { key: 'date', header: 'Booked', kind: 'date', render: (i) => formatDate(i.bookingDate) },
  {
    key: 'gross',
    header: 'Gross Premium',
    kind: 'amount',
    render: (i) => <Amount value={i.grossPremium} />,
  },
  {
    key: 'pay',
    header: 'Payment',
    kind: 'status',
    render: (i) => <StatusBadge status={i.paymentStatus} />,
  },
];

/**
 * Billing & SOA tab: the invoices booked on the programme's accounts with their payment status,
 * and the insurer SOAs received on the programme; Receive SOA.
 */
export function BillingTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [receiving, setReceiving] = useState(false);
  const invoices = useQuery({
    queryKey: ['eb', 'invoices', programme.id],
    queryFn: () => ebServiceApi.invoices(companyId, programme.id),
  });
  return (
    <div className="stack">
      <Card title="Invoices">
        <ErrorAlert error={invoices.error} onRetry={() => void invoices.refetch()} />
        <DataTable<ProgrammeInvoice>
          loading={invoices.isLoading}
          rows={invoices.data ?? []}
          rowKey={(i) => i.invoiceNo}
          columns={INVOICE_COLUMNS}
          emptyMessage="No invoice booked on the programme"
        />
      </Card>
      <Card
        flush
        title="Insurer SOAs"
        actions={
          (can('EB_MARKET') || can('EB_PROCESS')) && (
            <Button
              variant="secondary"
              size="sm"
              icon={<FileInput size={14} />}
              onClick={() => setReceiving(true)}
            >
              Receive SOA
            </Button>
          )
        }
      >
        <SoaTable filters={{ programmeId: programme.id }} showProgramme={false} />
      </Card>
      {receiving && (
        <SoaReceiveDialog
          programmeId={programme.id}
          invoices={invoices.data ?? []}
          onClose={() => setReceiving(false)}
        />
      )}
    </div>
  );
}
