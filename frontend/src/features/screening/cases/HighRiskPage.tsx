import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { ExportButtons } from '@/features/reports/ExportButtons';
import { formatDate, humanize, today } from '@/utils/format';
import { casesApi } from './api';
import type { HighRiskClient } from './api';

/** The report behind the list (export, SNSRP-901). */
const REPORT = 'SCR-HIGH-RISK-CLIENTS';

interface Filters {
  riskCategory: string;
  marketingUnit: string;
  clientType: string;
}

/**
 * High-risk Clients (capability 4 "view / extract the list of high-risk clients"; FR-SS-045): the
 * clients with a high rating, the PEP or the Watchlist Review tag, their category, open case,
 * active policy and marketing unit, exported with the report SCR-HIGH-RISK-CLIENTS.
 */
export default function HighRiskPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useFileDownload();
  const [filters, setFilters] = useState<Filters>({
    riskCategory: '',
    marketingUnit: '',
    clientType: '',
  });
  const [pending, setPending] = useState<ExportFormat>();
  const list = useQuery({
    queryKey: ['screening', 'high-risk', companyId, filters],
    queryFn: () => casesApi.highRisk({ companyId, ...filters }),
    enabled: companyId > 0,
  });
  const exportTo = (format: ExportFormat) => {
    setPending(format);
    download.mutate(
      () =>
        reportApi.export(
          REPORT,
          {
            companyId: String(companyId),
            asOfDate: today(),
            riskCategory: filters.riskCategory,
            marketingUnit: filters.marketingUnit,
            clientType: filters.clientType,
          },
          format,
        ),
      { onSettled: () => setPending(undefined) },
    );
  };
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Sanction Screening"
        title="High-risk Clients"
        description="Clients rated high risk or tagged PEP or Watchlist Review, with their risk category, open screening case, active policy and marketing unit."
        actions={
          can('SCR_REPORT_VIEW') && can('REPORT_VIEW') ? (
            <ExportButtons formats={['XLSX', 'PDF']} pending={pending} onExport={exportTo} />
          ) : undefined
        }
      />
      <Card flush>
        <div className="worklist-filters form-grid">
          <Field label="Risk Category">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.riskCategory}
                onChange={(e) => setFilters({ ...filters, riskCategory: e.target.value })}
              />
            )}
          </Field>
          <Field label="Marketing Unit">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.marketingUnit}
                onChange={(e) => setFilters({ ...filters, marketingUnit: e.target.value })}
              />
            )}
          </Field>
          <Field label="Client Type">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.clientType}
                onChange={(e) => setFilters({ ...filters, clientType: e.target.value })}
              >
                <option value="">All</option>
                <option value="INDIVIDUAL">Individual</option>
                <option value="CORPORATE">Corporate</option>
              </select>
            )}
          </Field>
        </div>
        <ErrorAlert error={list.error ?? download.error} />
        <DataTable<HighRiskClient>
          caption="High-risk clients"
          rows={list.data ?? []}
          rowKey={(r) => r.clientCode}
          loading={list.isLoading}
          emptyMessage="No high-risk, PEP or watchlist-tagged client for these filters"
          columns={[
            {
              key: 'client',
              header: 'Client',
              render: (r) => (
                <>
                  <strong>{r.clientName}</strong>
                  <span className="cell-sub mono">{r.clientCode}</span>
                </>
              ),
            },
            { key: 'type', header: 'Client Type', render: (r) => humanize(r.clientType) },
            { key: 'category', header: 'Risk Category', render: (r) => r.riskCategory ?? '—' },
            { key: 'rating', header: 'Risk Rating', render: (r) => r.riskRating ?? '—' },
            { key: 'tags', header: 'Tags', render: (r) => r.tags ?? '—' },
            {
              key: 'tagged',
              header: 'Tagged On / Source',
              render: (r) => (
                <>
                  {formatDate(r.taggedOn)}
                  <span className="cell-sub">{r.source ? humanize(r.source) : '—'}</span>
                </>
              ),
            },
            { key: 'case', header: 'Open Case', render: (r) => r.openCase },
            { key: 'policy', header: 'Active Policy', render: (r) => r.activePolicy },
            { key: 'unit', header: 'Marketing Unit / Unit Head', render: (r) => r.unit ?? '—' },
          ]}
        />
      </Card>
    </div>
  );
}
