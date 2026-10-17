import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { ebApi } from '@/api/eb';
import type { ContactView, LineView, ProgrammeView } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { EbLov } from '../common/EbLabels';
import { EB_LOV, ebLabel } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';
import { ContactDialog, LineDialog } from './ProgrammeDialogs';
import {
  contactForm,
  contactInput,
  emptyContact,
  emptyLine,
  lineForm,
  lineInput,
} from './programmeForm';
import type { ContactForm, LineForm } from './programmeForm';

type Editing<T> = { mode: 'add' } | { mode: 'edit'; item: T } | { mode: 'remove'; item: T };

function activeBadge(active: boolean) {
  return (
    <StatusBadge status={active ? 'ACTIVE' : 'INACTIVE'} label={active ? 'Active' : 'Removed'} />
  );
}

function RowActions({
  name,
  onEdit,
  onRemove,
}: Readonly<{ name: string; onEdit: () => void; onRemove: () => void }>) {
  return (
    <span className="eb-actions">
      <Button
        variant="ghost"
        size="sm"
        aria-label={`Edit ${name}`}
        icon={<Pencil size={14} />}
        onClick={onEdit}
      />
      <Button
        variant="ghost"
        size="sm"
        aria-label={`Remove ${name}`}
        icon={<Trash2 size={14} />}
        onClick={onRemove}
      />
    </span>
  );
}

const LINE_COLUMNS: Column<LineView>[] = [
  { key: 'no', header: 'Line', kind: 'center', width: '64px', render: (l) => l.lineNo },
  {
    key: 'line',
    header: 'Benefit Line',
    render: (l) => <EbLov type={EB_LOV.benefitLine} code={l.benefitLine} />,
  },
  {
    key: 'insurer',
    header: 'Incumbent Insurer',
    kind: 'code',
    render: (l) => l.incumbentInsurer ?? '',
  },
  {
    key: 'policy',
    header: 'Current Policy',
    render: (l) => <CellStack main={l.currentPolicyNo ?? '—'} sub={l.currentArn ?? undefined} />,
  },
  { key: 'from', header: 'Period From', kind: 'date', render: (l) => formatDate(l.periodFrom) },
  { key: 'to', header: 'Period To', kind: 'date', render: (l) => formatDate(l.periodTo) },
  { key: 'heads', header: 'Headcount', kind: 'amount', render: (l) => l.headcount ?? '' },
  { key: 'status', header: 'Status', kind: 'status', render: (l) => activeBadge(l.active) },
];

/** Lines tab: the benefit lines with incumbent, current policy, period and headcount. */
export function LinesTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const [editing, setEditing] = useState<Editing<LineView>>();
  const close = () => setEditing(undefined);
  const save = useEbMutation(
    (companyId, v: { lineNo?: number; line: LineForm }) =>
      v.lineNo === undefined
        ? ebApi.addLine(companyId, programme.id, lineInput(v.line))
        : ebApi.updateLine(companyId, programme.id, v.lineNo, lineInput(v.line)),
    'Benefit line saved',
    close,
  );
  const remove = useEbMutation(
    (companyId, lineNo: number) => ebApi.removeLine(companyId, programme.id, lineNo),
    'Benefit line removed',
    close,
  );
  const columns: Column<LineView>[] = can('EB_MARKET')
    ? [
        ...LINE_COLUMNS,
        {
          key: 'actions',
          header: '',
          width: '96px',
          render: (l) =>
            l.active ? (
              <RowActions
                name={`line ${String(l.lineNo)}`}
                onEdit={() => setEditing({ mode: 'edit', item: l })}
                onRemove={() => setEditing({ mode: 'remove', item: l })}
              />
            ) : (
              ''
            ),
        },
      ]
    : LINE_COLUMNS;
  return (
    <Card
      title="Benefit Lines"
      actions={
        can('EB_MARKET') && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => setEditing({ mode: 'add' })}
          >
            Add Line
          </Button>
        )
      }
    >
      <DataTable<LineView>
        rows={programme.lines}
        rowKey={(l) => l.lineNo}
        columns={columns}
        emptyMessage="No benefit lines"
      />
      {editing !== undefined && editing.mode !== 'remove' && (
        <LineDialog
          initial={editing.mode === 'edit' ? lineForm(editing.item) : emptyLine()}
          busy={save.isPending}
          error={save.error}
          onClose={close}
          onSave={(line) =>
            save.mutate({ lineNo: editing.mode === 'edit' ? editing.item.lineNo : undefined, line })
          }
        />
      )}
      {editing?.mode === 'remove' && (
        <ConfirmDialog
          title="Remove Benefit Line"
          record={`Line ${String(editing.item.lineNo)}`}
          effect="The line is kept for history but is no longer part of the programme or its renewal advice."
          confirmLabel="Remove Line"
          destructive
          busy={remove.isPending}
          error={remove.error}
          onConfirm={() => remove.mutate(editing.item.lineNo)}
          onClose={close}
        />
      )}
    </Card>
  );
}

const CONTACT_COLUMNS: Column<ContactView>[] = [
  { key: 'name', header: 'Name', render: (c) => <CellStack main={c.name} sub={ebLabel(c.role)} /> },
  { key: 'email', header: 'E-mail', render: (c) => c.email },
  { key: 'mobile', header: 'Mobile', render: (c) => c.mobile ?? '' },
  {
    key: 'ra',
    header: 'Renewal Advice',
    kind: 'center',
    render: (c) => (c.receivesRa ? 'Yes' : 'No'),
  },
  { key: 'soa', header: 'SOA', kind: 'center', render: (c) => (c.receivesSoa ? 'Yes' : 'No') },
  { key: 'status', header: 'Status', kind: 'status', render: (c) => activeBadge(c.active) },
];

/** Contacts tab: the HR contacts with the renewal advice and SOA flags. */
export function ContactsTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const [editing, setEditing] = useState<Editing<ContactView>>();
  const close = () => setEditing(undefined);
  const save = useEbMutation(
    (companyId, v: { id?: number; contact: ContactForm }) =>
      v.id === undefined
        ? ebApi.addContact(companyId, programme.id, contactInput(v.contact))
        : ebApi.updateContact(companyId, programme.id, v.id, contactInput(v.contact)),
    'HR contact saved',
    close,
  );
  const remove = useEbMutation(
    (companyId, id: number) => ebApi.removeContact(companyId, programme.id, id),
    'HR contact removed',
    close,
  );
  const columns: Column<ContactView>[] = can('EB_MARKET')
    ? [
        ...CONTACT_COLUMNS,
        {
          key: 'actions',
          header: '',
          width: '96px',
          render: (c) =>
            c.active ? (
              <RowActions
                name={c.name}
                onEdit={() => setEditing({ mode: 'edit', item: c })}
                onRemove={() => setEditing({ mode: 'remove', item: c })}
              />
            ) : (
              ''
            ),
        },
      ]
    : CONTACT_COLUMNS;
  return (
    <Card
      title="HR Contacts"
      actions={
        can('EB_MARKET') && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => setEditing({ mode: 'add' })}
          >
            Add Contact
          </Button>
        )
      }
    >
      <DataTable<ContactView>
        rows={programme.contacts}
        rowKey={(c) => c.id}
        columns={columns}
        emptyMessage="No HR contacts"
      />
      {editing !== undefined && editing.mode !== 'remove' && (
        <ContactDialog
          initial={editing.mode === 'edit' ? contactForm(editing.item) : emptyContact()}
          busy={save.isPending}
          error={save.error}
          onClose={close}
          onSave={(contact) =>
            save.mutate({ id: editing.mode === 'edit' ? editing.item.id : undefined, contact })
          }
        />
      )}
      {editing?.mode === 'remove' && (
        <ConfirmDialog
          title="Remove HR Contact"
          record={editing.item.name}
          effect="The contact is kept for history but no longer receives the renewal advice or SOAs."
          confirmLabel="Remove Contact"
          destructive
          busy={remove.isPending}
          error={remove.error}
          onConfirm={() => remove.mutate(editing.item.id)}
          onClose={close}
        />
      )}
    </Card>
  );
}
