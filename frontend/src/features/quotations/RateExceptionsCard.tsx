import { useQuery } from '@tanstack/react-query';
import { BadgePercent } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { productCatalogApi } from '@/api/productCatalog';
import type { RateException } from '@/api/productCatalog';
import type { Quotation } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import {
  exceptionStatus,
  formatRate,
  rateDifference,
  rateExceptionPath,
  requestedValue,
} from '@/features/catalog/rateException';
import { formatDate, formatDateTime } from '@/utils/format';
import { RateExceptionDialog } from './RateExceptionDialog';

/** The columns of the rate exceptions of a transaction. */
function rateExceptionColumns(schemeRate: number | undefined): Column<RateException>[] {
  return [
    {
      key: 'no',
      header: 'Exception No.',
      kind: 'code',
      render: (e) => <Link to={rateExceptionPath(e.referenceNo)}>{e.referenceNo}</Link>,
    },
    {
      key: 'requested',
      header: 'Requested Rate (%)',
      kind: 'amount',
      render: (e) =>
        e.requestedRate === undefined ? requestedValue(e) : formatRate(e.requestedRate),
    },
    {
      key: 'scheme',
      header: 'Scheme Rate (%)',
      kind: 'amount',
      render: () => (schemeRate === undefined ? '' : formatRate(schemeRate)),
    },
    {
      key: 'diff',
      header: 'Difference (%)',
      kind: 'amount',
      render: (e) => rateDifference(e.requestedRate, schemeRate),
    },
    { key: 'valid', header: 'Valid Until', kind: 'date', render: (e) => formatDate(e.validUntil) },
    {
      key: 'requestedBy',
      header: 'Requested By',
      render: (e) => (
        <CellStack main={<UserName login={e.requestedBy} />} sub={formatDateTime(e.requestedAt)} />
      ),
    },
    {
      key: 'decidedBy',
      header: 'Decided By',
      render: (e) =>
        e.decidedBy ? (
          <CellStack main={<UserName login={e.decidedBy} />} sub={formatDateTime(e.decidedAt)} />
        ) : (
          ''
        ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (e) => <StatusBadge status={exceptionStatus(e)} />,
    },
  ];
}

/**
 * The rate exceptions of a quotation (BRPM.007) as a record table in a titled card: exception
 * number, requested and scheme rate with the difference, validity, requester and decision, status.
 * A short info line says when submission needs an approved exception; Request Rate Exception is
 * in the card header.
 */
export function RateExceptionsCard({ quotation }: Readonly<{ quotation: Quotation }>) {
  const { can } = useAuth();
  const [open, setOpen] = useState(false);
  const exceptions = useQuery({
    queryKey: ['rate-exceptions', quotation.quotationNo],
    queryFn: () => productCatalogApi.rateExceptions(quotation.quotationNo),
    enabled: quotation.content.schemeVersion !== undefined,
  });
  const requested = exceptions.data ?? [];
  const first = requested[0]?.referenceNo;
  const scheme = useQuery({
    queryKey: ['rate-exception', first],
    queryFn: () => productCatalogApi.rateException(first ?? ''),
    enabled: first !== undefined,
    staleTime: 60_000,
  });
  if (!quotation.content.schemeVersion) {
    return null;
  }
  const deviation = quotation.content.schemeDeviation === true;
  const mayRequest = deviation && quotation.status === 'DRAFT' && can('QUOTE_MAINTAIN');
  if (!deviation && requested.length === 0) {
    return null;
  }
  return (
    <Card
      title="Rate Exceptions"
      className="rate-exceptions-card"
      callout="rate-exceptions"
      actions={
        mayRequest && (
          <Button
            size="sm"
            variant="secondary"
            icon={<BadgePercent size={14} />}
            onClick={() => setOpen(true)}
          >
            Request Rate Exception
          </Button>
        )
      }
    >
      <div className="stack">
        {deviation && (
          <Notice tone="info" className="subtle" callout="rate-exceptions-notice">
            Priced on package version {quotation.content.schemeVersion}. An item rate differs from
            the scheme rate, so submission needs an approved rate exception.
          </Notice>
        )}
        <ErrorAlert error={exceptions.error} />
        <DataTable
          caption="Rate exceptions"
          callout="rate-exceptions-table"
          columns={rateExceptionColumns(scheme.data?.schemeRate)}
          rows={requested}
          rowKey={(e) => e.referenceNo}
          loading={exceptions.isLoading}
          emptyMessage="No rate exception requested"
        />
      </div>
      {open && <RateExceptionDialog quotation={quotation} onClose={() => setOpen(false)} />}
    </Card>
  );
}
