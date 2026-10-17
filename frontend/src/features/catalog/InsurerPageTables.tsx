import { Pencil } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { InsurerBankAccount, InsurerContact, LinkedProduct } from '@/api/insurerPage';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { humanize } from '@/utils/format';
import { RecordActions } from './RecordActions';
import { accountPurposeLabel, contactRoleLabel } from './insurerPageForm';

const REFRESH = [['catalog']] as const;

function EditButton({ label, onEdit }: Readonly<{ label: string; onEdit?: () => void }>) {
  return onEdit ? (
    <Button size="sm" variant="ghost" icon={<Pencil size={14} />} onClick={onEdit}>
      {label}
    </Button>
  ) : null;
}

/** Contact persons of an insurer, each with its maker-checker status and actions. */
export function ContactsTable({
  rows,
  onEdit,
}: Readonly<{ rows: InsurerContact[]; onEdit?: (c: InsurerContact) => void }>) {
  return (
    <DataTable<InsurerContact>
      rows={rows}
      rowKey={(c) => c.id}
      emptyMessage="No contact recorded."
      columns={[
        { key: 'r', header: 'Handles', render: (c) => contactRoleLabel(c.role) },
        { key: 'n', header: 'Name', render: (c) => <strong>{c.name}</strong> },
        { key: 'p', header: 'Position', render: (c) => c.position ?? '' },
        { key: 'e', header: 'E-mail', render: (c) => c.email ?? '' },
        { key: 't', header: 'Phone', render: (c) => c.phone ?? '' },
        { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
        {
          key: 'x',
          header: 'Actions',
          render: (c) => (
            <>
              <EditButton label="Edit" onEdit={onEdit && (() => onEdit(c))} />
              <RecordActions kind="INSURER_CONTACT" record={c} label={c.name} refresh={REFRESH} />
            </>
          ),
        },
      ]}
    />
  );
}

/** Bank accounts of an insurer, each with its maker-checker status and actions. */
export function BankAccountsTable({
  rows,
  onEdit,
}: Readonly<{ rows: InsurerBankAccount[]; onEdit?: (a: InsurerBankAccount) => void }>) {
  return (
    <DataTable<InsurerBankAccount>
      rows={rows}
      rowKey={(a) => a.id}
      emptyMessage="No bank account recorded."
      columns={[
        { key: 'u', header: 'Purpose', render: (a) => accountPurposeLabel(a.purpose) },
        { key: 'b', header: 'Bank', render: (a) => a.bankName },
        { key: 'r', header: 'Branch', render: (a) => a.bankBranch ?? '' },
        { key: 'n', header: 'Account number', render: (a) => <strong>{a.accountNo}</strong> },
        { key: 'a', header: 'Account name', render: (a) => a.accountName },
        { key: 'c', header: 'Currency', render: (a) => a.currency },
        { key: 'p', header: 'Primary', render: (a) => (a.primaryAccount ? 'Yes' : 'No') },
        { key: 's', header: 'Status', render: (a) => <StatusBadge status={a.recordStatus} /> },
        {
          key: 'x',
          header: 'Actions',
          render: (a) => (
            <>
              <EditButton label="Edit" onEdit={onEdit && (() => onEdit(a))} />
              <RecordActions
                kind="INSURER_BANK_ACCOUNT"
                record={a}
                label={`${a.bankName} ${a.accountNo}`}
                refresh={REFRESH}
              />
            </>
          ),
        },
      ]}
    />
  );
}

/** Products the insurer is linked to, by package version or commission rate. */
export function LinkedProductsTable({ rows }: Readonly<{ rows: LinkedProduct[] }>) {
  return (
    <DataTable<LinkedProduct>
      rows={rows}
      rowKey={(p) => p.productCode}
      emptyMessage="No product is linked to this insurer."
      columns={[
        {
          key: 'c',
          header: 'Product',
          render: (p) => (
            <Link to={`/catalog/products/${encodeURIComponent(p.productCode)}`}>
              {p.productCode} – {p.productName}
            </Link>
          ),
        },
        { key: 'r', header: 'Role', render: (p) => (p.role ? humanize(p.role) : '') },
        {
          key: 'v',
          header: 'Latest version',
          render: (p) =>
            p.versionNo === null ? '' : `v${p.versionNo} (${humanize(p.versionStatus ?? '')})`,
        },
        { key: 'm', header: 'Own commission rate', render: (p) => (p.commission ? 'Yes' : 'No') },
      ]}
    />
  );
}
