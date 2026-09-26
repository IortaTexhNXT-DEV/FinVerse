import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { DocumentRule, FieldRule, TsuRule } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { humanize } from '@/utils/format';
import { RecordActions } from './RecordActions';
import { RuleEditorModal } from './RuleEditorModal';
import type { RuleKind } from './RuleEditorModal';

const scope = (r: { scope: string; scopeCode: string }) =>
  r.scope === 'ALL' ? 'All products' : `${humanize(r.scope)} ${r.scopeCode}`;

function statusColumns<T extends FieldRule | DocumentRule | TsuRule>(
  kind: 'FIELD_RULE' | 'DOCUMENT_RULE' | 'TSU_RULE',
  queryKey: string,
): Column<T>[] {
  return [
    { key: 'st', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
    {
      key: 'a',
      header: 'Actions',
      render: (r) => <RecordActions kind={kind} record={r} refresh={[['catalog', queryKey]]} />,
    },
  ];
}

function FieldRules() {
  const rules = useQuery({ queryKey: ['catalog', 'field-rules'], queryFn: catalogApi.fieldRules });
  return (
    <>
      <ErrorAlert error={rules.error} />
      <DataTable<FieldRule>
        loading={rules.isLoading}
        rows={rules.data ?? []}
        rowKey={(r) => r.id}
        columns={[
          { key: 's', header: 'Applies to', render: scope },
          {
            key: 't',
            header: 'Level',
            render: (r) => (r.target === 'ITEM' ? 'Risk item' : 'Account'),
          },
          { key: 'l', header: 'Field', render: (r) => <strong>{r.label}</strong> },
          { key: 'k', header: 'Key', render: (r) => <code>{r.fieldKey}</code> },
          { key: 'r', header: 'Mandatory', render: (r) => (r.required ? 'Yes' : 'Optional') },
          ...statusColumns<FieldRule>('FIELD_RULE', 'field-rules'),
        ]}
      />
    </>
  );
}

function DocumentRules() {
  const rules = useQuery({
    queryKey: ['catalog', 'document-rules'],
    queryFn: catalogApi.documentRules,
  });
  return (
    <>
      <ErrorAlert error={rules.error} />
      <DataTable<DocumentRule>
        loading={rules.isLoading}
        rows={rules.data ?? []}
        rowKey={(r) => r.id}
        columns={[
          { key: 's', header: 'Applies to', render: scope },
          {
            key: 'd',
            header: 'Document',
            render: (r) => <strong>{humanize(r.documentType)}</strong>,
          },
          {
            key: 'r',
            header: 'Before Submission',
            render: (r) => (r.required ? 'Required' : 'Optional'),
          },
          ...statusColumns<DocumentRule>('DOCUMENT_RULE', 'document-rules'),
        ]}
      />
    </>
  );
}

function criteria(r: TsuRule): string {
  const parts = [
    r.productClass && r.productClass !== 'ANY' ? humanize(r.productClass) : '',
    r.lineCode ? `line ${r.lineCode}` : '',
    r.minFleetUnits ? `${r.minFleetUnits}+ vehicles` : '',
    r.minLocations ? `${r.minLocations}+ locations` : '',
    r.endorsementType ? `endorsement ${r.endorsementType}` : '',
  ];
  return parts.filter((p) => p !== '').join(', ') || 'Any account';
}

function TsuRules() {
  const rules = useQuery({ queryKey: ['catalog', 'tsu-rules'], queryFn: catalogApi.tsuRules });
  return (
    <>
      <ErrorAlert error={rules.error} />
      <DataTable<TsuRule>
        loading={rules.isLoading}
        rows={rules.data ?? []}
        rowKey={(r) => r.id}
        columns={[
          { key: 'p', header: 'Priority', numeric: true, render: (r) => r.priority },
          { key: 'c', header: 'Code', render: (r) => <strong>{r.code}</strong> },
          { key: 'd', header: 'Description', render: (r) => r.description },
          { key: 'k', header: 'Criteria', render: criteria },
          {
            key: 't',
            header: 'TSI Above',
            numeric: true,
            render: (r) => (r.tsiAbove ? <Amount value={r.tsiAbove} /> : ''),
          },
          ...statusColumns<TsuRule>('TSU_RULE', 'tsu-rules'),
        ]}
      />
    </>
  );
}

const TITLES: Record<RuleKind, string> = {
  fields: 'Field rules: data an account needs before submission',
  documents: 'Document rules: attachments an account needs before submission',
  tsu: 'TSU routing: accounts that need Technical Services review',
};

/** Field, document and TSU routing rules of the catalog, with add and authorize. */
export function CatalogRulesPanel({ kind }: Readonly<{ kind: RuleKind }>) {
  const { can } = useAuth();
  const [adding, setAdding] = useState(false);
  return (
    <Card
      title={TITLES[kind]}
      flush
      actions={
        can('MASTER_MAINTAIN') && (
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => setAdding(true)}
          >
            Add Rule
          </Button>
        )
      }
    >
      {kind === 'fields' && <FieldRules />}
      {kind === 'documents' && <DocumentRules />}
      {kind === 'tsu' && <TsuRules />}
      {adding && <RuleEditorModal kind={kind} onClose={() => setAdding(false)} />}
    </Card>
  );
}
