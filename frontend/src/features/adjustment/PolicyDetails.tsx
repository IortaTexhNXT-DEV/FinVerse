import { useQueries } from '@tanstack/react-query';
import { InsurerName, ProductName } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Amount } from '@/components/ui/Amount';
import { PeriodCell } from '@/components/ui/PeriodCell';
import { adjustmentApi } from './api';
import type { PolicyLink } from './api';

const COLUMNS: Column<PolicyLink>[] = [
  {
    key: 'policy',
    header: 'Policy No. / ARN',
    kind: 'code',
    render: (p) => <CellStack main={p.policyNo} sub={p.arn} />,
  },
  {
    key: 'invoice',
    header: 'Invoice / Placement Slip',
    kind: 'code',
    render: (p) => <CellStack main={p.invoiceNo} sub={p.slipNo} />,
  },
  {
    key: 'client',
    header: 'Client',
    render: (p) => <CellStack main={p.assuredName} sub={p.clientCode} />,
  },
  {
    key: 'insurer',
    header: 'Insurer / Product',
    render: (p) => (
      <CellStack
        main={<InsurerName code={p.insurerCode} />}
        sub={p.productCode ? <ProductName code={p.productCode} /> : undefined}
      />
    ),
  },
  {
    key: 'period',
    header: 'Period of Cover',
    kind: 'period',
    render: (p) => <PeriodCell from={p.periodFrom} to={p.periodTo} />,
  },
  {
    key: 'gross',
    header: 'Gross Premium',
    kind: 'amount',
    render: (p) => <Amount value={p.grossPremium} />,
  },
];

/**
 * The policies the request will be raised against, shown as soon as an invoice is chosen and before
 * anything is saved: insurer policy number, ARN, invoice, placement slip, client, insurer, product,
 * cover and gross premium.
 */
export function PolicyDetails({
  invoiceNos,
  title = 'Policy Details',
}: Readonly<{ invoiceNos: string[]; title?: string }>) {
  const links = useQueries({
    queries: invoiceNos.map((no) => ({
      queryKey: ['adjustment', 'policy', no],
      queryFn: () => adjustmentApi.policy(no),
      staleTime: 60_000,
    })),
  });
  const rows = links.map((l) => l.data).filter((l): l is PolicyLink => l !== undefined);
  return (
    <Card title={title} flush>
      <ErrorAlert error={links.find((l) => l.error)?.error ?? null} />
      <DataTable
        caption="Policies of the request"
        columns={COLUMNS}
        rows={rows}
        rowKey={(p) => p.invoiceNo}
        loading={links.some((l) => l.isLoading)}
        skeletonRows={invoiceNos.length}
        emptyMessage="Choose a booked invoice to see its policy"
      />
    </Card>
  );
}
