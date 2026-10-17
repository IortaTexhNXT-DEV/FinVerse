import { useQuery } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { Field } from '@/components/ui/Field';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount } from '@/utils/format';
import type { AccountLine } from './recordLogic';
import { excessOf, recordTotals } from './recordLogic';
import { recordsApi } from './recordsApi';
import type { AccountRow } from './recordsApi';
import { useDebounced } from './useDebounced';

interface RecordAccountsProps {
  lines: AccountLine[];
  onChange: (lines: AccountLine[]) => void;
  error?: string;
  readOnly?: boolean;
}

const yesNo = (on: boolean) => (on ? 'Yes' : 'No');

/**
 * The account table of a creation record (FRS.CSH.02.01.03 to 02.01.05): search an account by its
 * account, invoice, ARN, policy or PN number, add it with "Add", enter the amount paid to it, and
 * see the premium and commission receivable, the total invoice, the total paid, the outstanding
 * balance, the PR 2307 of an account with the BIR 2307 tag and the excess; the totals of the record
 * are under the table.
 */
export function RecordAccounts({
  lines,
  onChange,
  error,
  readOnly = false,
}: Readonly<RecordAccountsProps>) {
  const companyId = useCompanyId();
  const [term, setTerm] = useState('');
  const settled = useDebounced(term.trim());
  const found = useQuery({
    queryKey: ['cashiering', 'record-accounts', companyId, settled],
    queryFn: () => recordsApi.accounts(companyId, settled),
    enabled: companyId > 0 && settled.length >= 3,
  });
  const add = (row: AccountRow) => {
    if (!lines.some((l) => l.row.invoiceNo === row.invoiceNo)) {
      onChange([
        ...lines,
        { row, amount: row.outstanding === undefined ? '' : String(row.outstanding) },
      ]);
    }
  };
  const setAmount = (invoiceNo: string, amount: string) =>
    onChange(lines.map((l) => (l.row.invoiceNo === invoiceNo ? { ...l, amount } : l)));
  const remove = (invoiceNo: string) =>
    onChange(lines.filter((l) => l.row.invoiceNo !== invoiceNo));
  const totals = recordTotals(lines);
  const columns: Column<AccountLine>[] = [
    {
      key: 'account',
      header: 'Account Number',
      render: (l) => (
        <>
          <strong>{l.row.invoiceNo}</strong>
          <span className="cell-sub">
            {l.row.prebooked ? 'Not booked yet (pre-booked)' : (l.row.assuredName ?? '')}
          </span>
        </>
      ),
    },
    {
      key: 'pr',
      header: 'Premium Receivable',
      numeric: true,
      render: (l) => <Amount value={l.row.premiumReceivable} />,
    },
    {
      key: 'cr',
      header: 'Commission Receivable',
      numeric: true,
      render: (l) => <Amount value={l.row.commissionReceivable} />,
    },
    {
      key: 'invoice',
      header: 'Total Invoice Amount',
      numeric: true,
      render: (l) => <Amount value={l.row.totalInvoice} />,
    },
    {
      key: 'paid',
      header: 'Total Paid Amount',
      numeric: true,
      render: (l) => <Amount value={l.row.totalPaid} />,
    },
    {
      key: 'out',
      header: 'Outstanding Balance',
      numeric: true,
      render: (l) => <Amount value={l.row.outstanding} />,
    },
    { key: 'tag', header: 'BIR 2307 Tag', render: (l) => yesNo(l.row.bir2307) },
    {
      key: 'pr2307',
      header: 'PR 2307 Amount',
      numeric: true,
      render: (l) => (l.row.bir2307 ? <Amount value={l.row.pr2307} /> : ''),
    },
    {
      key: 'amount',
      header: 'Paid Amount',
      numeric: true,
      render: (l) =>
        readOnly ? (
          <Amount value={l.amount} />
        ) : (
          <input
            className="input num"
            type="number"
            step="0.01"
            aria-label={`Paid amount for ${l.row.invoiceNo}`}
            value={l.amount}
            onChange={(e) => setAmount(l.row.invoiceNo, e.target.value)}
          />
        ),
    },
    {
      key: 'excess',
      header: 'Excess Amount',
      numeric: true,
      render: (l) => <Amount value={excessOf(l)} />,
    },
  ];
  if (!readOnly) {
    columns.push({
      key: 'remove',
      header: '',
      width: '48px',
      render: (l) => (
        <Button
          variant="ghost"
          icon={<Trash2 size={16} />}
          aria-label={`Remove ${l.row.invoiceNo}`}
          onClick={() => remove(l.row.invoiceNo)}
        />
      ),
    });
  }
  return (
    <div className="stack">
      {!readOnly && (
        <Field
          label="Account Number"
          hint="Account, invoice, ARN, policy or PN number (at least 3 characters)"
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={term}
              onChange={(e) => setTerm(e.target.value)}
            />
          )}
        </Field>
      )}
      {!readOnly && (found.data ?? []).length > 0 && (
        <ul className="csh-account-hits" aria-label="Accounts found">
          {(found.data ?? []).map((row) => (
            <li key={`${row.invoiceNo}-${row.prebooked}`}>
              <span>
                {row.invoiceNo} {row.assuredName ? `· ${row.assuredName}` : ''}
                {row.outstanding === undefined
                  ? ' · pre-booked'
                  : ` · outstanding ${formatAmount(row.outstanding)}`}
              </span>
              <Button variant="secondary" icon={<Plus size={14} />} onClick={() => add(row)}>
                Add
              </Button>
            </li>
          ))}
        </ul>
      )}
      <DataTable
        caption="Accounts of the receipt"
        columns={columns}
        rows={lines}
        rowKey={(l) => l.row.invoiceNo}
        emptyMessage="No account added: the payment will be recorded as unbooked / unmatched"
        footer={
          <tr>
            <td colSpan={8}>Total Paid Amount / Excess Payments</td>
            <td className="num">{formatAmount(totals.paid)}</td>
            <td className="num">{formatAmount(totals.excess)}</td>
            {!readOnly && <td />}
          </tr>
        }
      />
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
