import { useQuery } from '@tanstack/react-query';
import { ArrowRightLeft } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { disbursementApi } from './api';
import type { Funding } from './api';
import { bankLabel } from './forms';
import { FUNDING_TABS } from './labels';
import type { FundingTab } from './labels';
import './disbursement.css';

/**
 * Account funding (DIS 2.17.0-2.17.5): transfers between BDOIR bank accounts requested by the
 * maker, verified, and approved by two approvers before the transfer entry posts.
 */
export default function FundingPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<FundingTab>(
    () => FUNDING_TABS.find((t) => t.id === params.get('tab'))?.id ?? 'OPEN',
  );
  const [page, setPage] = useState(0);
  const stages = FUNDING_TABS.find((t) => t.id === tab)?.stages ?? [];
  const banks = useQuery({
    queryKey: ['disbursement', 'banks', companyId],
    queryFn: () => disbursementApi.banks(companyId),
    enabled: companyId > 0,
  });
  const fundings = useQuery({
    queryKey: ['disbursement', 'funding', companyId, tab, page],
    queryFn: () => disbursementApi.fundings(companyId, stages, page),
    enabled: companyId > 0,
  });
  const columns: Column<Funding>[] = [
    { key: 'no', header: 'Funding No.', render: (f) => f.fundingNo },
    { key: 'from', header: 'From', render: (f) => bankLabel(banks.data, f.sourceBankAccountId) },
    { key: 'to', header: 'To', render: (f) => bankLabel(banks.data, f.targetBankAccountId) },
    { key: 'date', header: 'Value Date', render: (f) => formatDate(f.valueDate) },
    { key: 'amount', header: 'Amount', numeric: true, render: (f) => <Amount value={f.amount} /> },
    { key: 'ccy', header: 'Currency', render: (f) => f.currency },
    { key: 'by', header: 'Requested By', render: (f) => f.createdBy },
    { key: 'stage', header: 'Status', render: (f) => <StatusBadge status={f.stage} /> },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="Account Funding"
        description="Transfers between BDOIR bank accounts: requested, verified and approved by two approvers before the entry posts."
        actions={
          can('DISB_FUNDING_REQUEST') ? (
            <Button
              variant="accent"
              icon={<ArrowRightLeft size={16} />}
              onClick={() => void navigate('/disbursement/funding/new')}
            >
              New Funding Request
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={fundings.error ?? banks.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={FUNDING_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <DataTable
            caption="Funding requests"
            columns={columns}
            rows={fundings.data?.content ?? []}
            rowKey={(f) => f.id}
            loading={fundings.isLoading}
            onRowClick={(f) => void navigate(`/disbursement/funding/${f.id}`)}
          />
          <PageFooter data={fundings.data} noun="requests" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
