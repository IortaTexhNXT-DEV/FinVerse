import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookPlus, Pencil, Power } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { disbursementApi } from './api';
import type { Bank, CheckBook } from './api';
import { DialogFooter } from './VoucherDialogs';
import './disbursement.css';

interface BookTarget {
  bank: Bank;
  book?: CheckBook;
}

/** The status shown for a bank account: its status, or the change waiting for authorisation. */
function shownStatus(b: Bank): string {
  if (b.requestedStatus === undefined) {
    return b.status;
  }
  return b.requestedStatus === 'ACTIVE' ? 'FOR_REACTIVATION' : 'FOR_DEACTIVATION';
}

function BookDialog({
  target,
  onClose,
  onDone,
}: Readonly<{ target: BookTarget; onClose: () => void; onDone: (b: Bank) => void }>) {
  const [first, setFirst] = useState(target.book === undefined ? '' : String(target.book.firstNo));
  const [last, setLast] = useState(target.book === undefined ? '' : String(target.book.lastNo));
  const valid = Number(first) > 0 && Number(last) >= Number(first);
  const save = useMutation({
    mutationFn: () => {
      const body = { firstNo: Number(first), lastNo: Number(last) };
      return target.book === undefined
        ? disbursementApi.addBook(target.bank.id, body)
        : disbursementApi.editBook(target.book.id, body);
    },
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title={
        target.book === undefined
          ? `Add Check Series to ${target.bank.code}`
          : `Edit Check Series of ${target.bank.code}`
      }
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save Series"
          busy={save.isPending}
          disabled={!valid}
          onClose={onClose}
          onConfirm={() => save.mutate()}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        {target.book !== undefined && (
          <p>
            A series can be corrected only before its first check is printed; the previous range is
            kept (DIS 2.24.2).
          </p>
        )}
        <div className="dsb-form">
          <Field label="First Check No." required>
            {(id) => (
              <input
                id={id}
                type="number"
                min="1"
                className="input"
                value={first}
                onChange={(e) => setFirst(e.target.value)}
              />
            )}
          </Field>
          <Field label="Last Check No." required>
            {(id) => (
              <input
                id={id}
                type="number"
                min="1"
                className="input"
                value={last}
                onChange={(e) => setLast(e.target.value)}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

function Books({
  bank,
  canEdit,
  onEdit,
}: Readonly<{ bank: Bank; canEdit: boolean; onEdit: (t: BookTarget) => void }>) {
  const columns: Column<CheckBook>[] = [
    { key: 'range', header: 'Series', render: (b) => `${b.firstNo} - ${b.lastNo}` },
    { key: 'next', header: 'Next Check', numeric: true, render: (b) => b.nextNo },
    { key: 'left', header: 'Remaining', numeric: true, render: (b) => b.remaining },
    { key: 'on', header: 'Received', render: (b) => formatDate(b.receivedOn) },
    {
      key: 'prev',
      header: 'Previous Range',
      render: (b) => (b.previousRange ? `${b.previousRange} (${b.editedBy ?? ''})` : ''),
    },
    { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
    {
      key: 'act',
      header: 'Action',
      render: (b) =>
        canEdit && b.status === 'ACTIVE' && b.nextNo === b.firstNo ? (
          <Button
            size="sm"
            variant="secondary"
            icon={<Pencil size={16} />}
            onClick={() => onEdit({ bank, book: b })}
          >
            Edit
          </Button>
        ) : null,
    },
  ];
  return (
    <DataTable
      caption={`Check series of ${bank.code}`}
      columns={columns}
      rows={bank.books}
      rowKey={(b) => b.id}
      emptyMessage="No check series"
    />
  );
}

/**
 * BDOIR bank accounts (DIS 2.23.0-2.24.2): the paying accounts with their GL account, the active /
 * inactive status changed by the team leader and authorised by the approver, and the check series
 * with their remaining leaves.
 */
export default function BanksPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [opened, setOpened] = useState<number>();
  const [book, setBook] = useState<BookTarget>();
  const key = ['disbursement', 'banks', companyId];
  const banks = useQuery({
    queryKey: key,
    queryFn: () => disbursementApi.banks(companyId),
    enabled: companyId > 0,
  });
  const changed = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: key });
    toast.success(message);
  };
  const status = useMutation({
    mutationFn: (b: Bank) =>
      disbursementApi.bankStatus(b.id, b.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'),
    onSuccess: (b) => changed(`${b.code}: status change sent for authorisation`),
  });
  const authorize = useMutation({
    mutationFn: (b: Bank) => disbursementApi.authorizeBank(b.id),
    onSuccess: (b) => changed(`${b.code} authorised`),
  });
  const review = can('DISB_REVIEW');
  const columns: Column<Bank>[] = [
    { key: 'code', header: 'Code', render: (b) => b.code },
    { key: 'bank', header: 'Bank', render: (b) => b.bankName },
    { key: 'no', header: 'Account No.', render: (b) => b.accountNo },
    { key: 'ccy', header: 'Currency', render: (b) => b.currency },
    { key: 'gl', header: 'GL Account', render: (b) => b.glAccountCode },
    { key: 'leaves', header: 'Check Leaves', numeric: true, render: (b) => b.remainingLeaves },
    {
      key: 'status',
      header: 'Status',
      render: (b) => <StatusBadge status={shownStatus(b)} />,
    },
    {
      key: 'act',
      header: 'Action',
      render: (b) => (
        <div className="dsb-actions">
          {review && b.requestedStatus === undefined && (
            <Button
              size="sm"
              variant="secondary"
              icon={<Power size={16} />}
              busy={status.isPending}
              onClick={() => status.mutate(b)}
            >
              {b.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
            </Button>
          )}
          {can('DISB_APPROVE') && b.recordStatus === 'PENDING_AUTHORIZATION' && (
            <Button size="sm" busy={authorize.isPending} onClick={() => authorize.mutate(b)}>
              Authorise
            </Button>
          )}
          {review && (
            <Button
              size="sm"
              variant="secondary"
              icon={<BookPlus size={16} />}
              onClick={() => setBook({ bank: b })}
            >
              Add Series
            </Button>
          )}
        </div>
      ),
    },
  ];
  const selected = banks.data?.find((b) => b.id === opened);
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        title="Bank Accounts and Checks"
        description="Paying bank accounts, their status and check series; status changes are authorised by the approver."
      />
      <ErrorAlert error={banks.error ?? status.error ?? authorize.error} />
      <Card flush>
        <DataTable
          caption="Bank accounts"
          columns={columns}
          rows={banks.data ?? []}
          rowKey={(b) => b.id}
          loading={banks.isLoading}
          onRowClick={(b) => setOpened(b.id)}
        />
      </Card>
      {selected !== undefined && (
        <Card title={`Check Series of ${selected.code}`}>
          <Books bank={selected} canEdit={review} onEdit={setBook} />
        </Card>
      )}
      {book !== undefined && (
        <BookDialog
          target={book}
          onClose={() => setBook(undefined)}
          onDone={(b) => {
            setBook(undefined);
            setOpened(b.id);
            void changed('Check series saved');
          }}
        />
      )}
    </div>
  );
}
