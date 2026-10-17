import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Card } from '@/components/ui/Card';
import { Combobox } from '@/components/ui/Combobox';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { ExportButtons } from '@/features/reports/ExportButtons';
import { formatDate, humanize, today } from '@/utils/format';
import { casesApi } from './api';
import type { FilterOption, HighRiskClient } from './api';

/** Client types of the filter. */
const CLIENT_TYPES = [
  { value: 'INDIVIDUAL', label: 'Individual' },
  { value: 'CORPORATE', label: 'Corporate' },
];

/** Filter values as choices: the name shown, the code stored. */
function choices(options: readonly FilterOption[] | undefined) {
  return (options ?? []).map((o) => ({ value: o.code, label: o.label || o.code }));
}

/** Tags in words: "PEP, WATCHLIST_REVIEW" becomes "PEP, Watchlist Review". */
function tagWords(tags: string | null | undefined): string {
  return tags
    ? tags
        .split(',')
        .map((t) => humanize(t.trim()))
        .join(', ')
    : '';
}

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
  const values = useQuery({
    queryKey: ['screening', 'high-risk', 'filters', companyId],
    queryFn: () => casesApi.highRiskFilters(companyId),
    enabled: companyId > 0,
  });
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
        description="Clients rated high risk or tagged PEP or Watchlist Review."
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
              <Combobox
                id={id}
                value={filters.riskCategory}
                emptyLabel="All"
                loading={values.isLoading}
                options={choices(values.data?.riskCategories)}
                onChange={(riskCategory) => setFilters({ ...filters, riskCategory })}
              />
            )}
          </Field>
          <Field label="Marketing Unit">
            {(id) => (
              <Combobox
                id={id}
                value={filters.marketingUnit}
                emptyLabel="All"
                loading={values.isLoading}
                options={choices(values.data?.marketingUnits)}
                onChange={(marketingUnit) => setFilters({ ...filters, marketingUnit })}
              />
            )}
          </Field>
          <Field label="Client Type">
            {(id) => (
              <Combobox
                id={id}
                value={filters.clientType}
                emptyLabel="All"
                options={CLIENT_TYPES}
                onChange={(clientType) => setFilters({ ...filters, clientType })}
              />
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
            {
              key: 'category',
              header: 'Risk Category',
              render: (r) =>
                r.riskCategoryName ?? (r.riskCategory ? humanize(r.riskCategory) : '—'),
            },
            {
              key: 'rating',
              header: 'Risk Rating',
              render: (r) => (r.riskRating ? humanize(r.riskRating) : '—'),
            },
            { key: 'tags', header: 'Tags', render: (r) => tagWords(r.tags) || '—' },
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
            {
              key: 'case',
              header: 'Open Case',
              render: (r) =>
                r.openCaseNo ? (
                  <>
                    <span className="mono">{r.openCaseNo}</span>
                    <span className="cell-sub">{humanize(r.openCaseStage ?? '')}</span>
                  </>
                ) : (
                  'None'
                ),
            },
            { key: 'policy', header: 'Active Policy', render: (r) => r.activePolicy },
            {
              key: 'unit',
              header: 'Marketing Unit / Unit Head',
              render: (r) => (
                <>
                  {r.marketingUnitName ?? r.marketingUnit ?? '—'}
                  {r.unitHead && (
                    <span className="cell-sub">
                      <UserName login={r.unitHead} />
                    </span>
                  )}
                </>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
