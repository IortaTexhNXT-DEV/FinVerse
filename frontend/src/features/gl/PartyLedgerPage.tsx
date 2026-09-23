import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { partiesApi } from '@/api/parties';
import { subledgerApi } from '@/api/subledger';
import type { OpenItem } from '@/api/subledger';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { AgeingStrip, StatementKpis } from './PartyStatementParts';
import { daysOverdue, statementLines, summarize } from './partyStatement';

function columns(asOf: string): Column<OpenItem>[] {
  return [
    { key: 'd', header: 'Doc date', render: (i) => formatDate(i.documentDate) },
    { key: 't', header: 'Type', render: (i) => humanize(i.documentType) },
    { key: 'n', header: 'Document', render: (i) => <strong>{i.documentNo}</strong> },
    { key: 'due', header: 'Due date', render: (i) => formatDate(i.dueDate) },
    { key: 'dr', header: 'Dr/Cr', render: (i) => (i.direction === 'DEBIT' ? 'Dr' : 'Cr') },
    { key: 'c', header: 'Currency', render: (i) => i.currency },
    { key: 'a', header: 'Amount', numeric: true, render: (i) => <Amount value={i.amount} /> },
    {
      key: 's',
      header: 'Settled',
      numeric: true,
      render: (i) => <Amount value={i.settledAmount} />,
    },
    {
      key: 'o',
      header: 'Outstanding',
      numeric: true,
      render: (i) => <Amount value={i.outstanding} />,
    },
    {
      key: 'od',
      header: 'Days overdue',
      numeric: true,
      render: (i) => (i.outstanding === 0 ? '' : daysOverdue(i, asOf)),
    },
    { key: 'j', header: 'GL batch', render: (i) => i.journalBatchNo ?? '' },
    { key: 'st', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
  ];
}

/**
 * Party statement from the open-item sub-ledger: every receivable and payable document of a
 * business partner with the settled and outstanding amounts, days overdue and the ageing.
 */
export default function PartyLedgerPage() {
  const companyId = useCompanyId();
  const [search, setSearch] = useState('');
  const [partyCode, setPartyCode] = useState('');
  const [asOf, setAsOf] = useState(today());
  const [openOnly, setOpenOnly] = useState(true);
  const selected = companyId > 0 && partyCode !== '';

  const parties = useQuery({
    queryKey: ['parties', companyId, 'lookup', search],
    queryFn: () => partiesApi.search(companyId, [], search),
    enabled: companyId > 0 && search.length >= 2,
  });
  const items = useQuery({
    queryKey: ['party-items', companyId, partyCode],
    queryFn: () => subledgerApi.partyItems(companyId, partyCode),
    enabled: selected,
  });
  const ageing = useQuery({
    queryKey: ['party-ageing', companyId, partyCode, asOf],
    queryFn: () => subledgerApi.ageing(companyId, asOf, partyCode),
    enabled: selected,
  });
  const partyOptions = parties.data ?? [];
  const all = items.data ?? [];
  const pick = (value: string) => {
    setSearch(value);
    const match = partyOptions.find((p) => p.code === value);
    if (match !== undefined) {
      setPartyCode(match.code);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Party Statement"
        description="Open items of a policyholder, intermediary, reinsurer or supplier with outstanding balances and ageing (sub-ledger). For the matched / unmatched statement of a period, run report FIN-ARAP-SOA-MATCH."
      />
      <Card>
        <div className="form-grid">
          <Field label="Find party (code or name)">
            {(id) => (
              <input
                id={id}
                className="input"
                list="party-options"
                value={search}
                onChange={(e) => pick(e.target.value)}
              />
            )}
          </Field>
          <datalist id="party-options">
            {partyOptions.map((p) => (
              <option key={p.id} value={p.code}>
                {p.name}
              </option>
            ))}
          </datalist>
          <Field label="Party code">
            {(id) => (
              <input
                id={id}
                className="input"
                value={partyCode}
                onChange={(e) => setPartyCode(e.target.value.toUpperCase().trim())}
              />
            )}
          </Field>
          <Field label="As of">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={asOf}
                onChange={(e) => setAsOf(e.target.value)}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={openOnly}
              onChange={(e) => setOpenOnly(e.target.checked)}
            />
            Open items only
          </label>
        </div>
      </Card>
      <ErrorAlert error={items.error ?? ageing.error} />
      {selected && (
        <StatementKpis
          summary={summarize(all, asOf)}
          partyName={partyOptions.find((p) => p.code === partyCode)?.name}
        />
      )}
      <AgeingStrip ageing={ageing.data} partyCode={partyCode} />
      <Card flush>
        <DataTable<OpenItem>
          loading={selected && items.isLoading}
          rows={statementLines(all, openOnly)}
          rowKey={(i) => i.id}
          caption="Open items"
          emptyMessage={selected ? 'No items.' : 'Select a party.'}
          columns={columns(asOf)}
        />
      </Card>
    </div>
  );
}
