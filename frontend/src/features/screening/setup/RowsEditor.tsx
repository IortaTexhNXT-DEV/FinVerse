import { Plus, Trash2 } from 'lucide-react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { humanize } from '@/utils/format';
import type { ConfigRow } from './api';
import { ROW_KEY, cellText, newRowKey, parseCell } from './configSpec';
import type { FieldSpec, TableSpec } from './configSpec';

interface CellProps {
  field: FieldSpec;
  value: unknown;
  label: string;
  onChange: (value: unknown) => void;
}

/** One editable cell of a configuration row, by field kind. */
function Cell({ field, value, label, onChange }: Readonly<CellProps>) {
  const text = cellText(value);
  if (field.kind === 'check') {
    return (
      <input
        type="checkbox"
        aria-label={label}
        checked={value === true}
        onChange={(e) => onChange(e.target.checked)}
      />
    );
  }
  if (field.kind === 'lov' && field.lovType !== undefined) {
    return (
      <LovSelect
        id={label}
        type={field.lovType}
        value={text}
        placeholder="Any"
        onChange={(code) => onChange(code === '' ? null : code)}
      />
    );
  }
  if (field.kind === 'select') {
    return (
      <select
        className="select"
        aria-label={label}
        value={text}
        onChange={(e) => onChange(parseCell('select', e.target.value))}
      >
        <option value="">—</option>
        {(field.options ?? []).map((o) => (
          <option key={o} value={o}>
            {humanize(o)}
          </option>
        ))}
      </select>
    );
  }
  return (
    <input
      className="input"
      aria-label={label}
      title={field.hint}
      inputMode={field.kind === 'text' || field.kind === 'codes' ? undefined : 'decimal'}
      defaultValue={text}
      onBlur={(e) => onChange(parseCell(field.kind, e.target.value))}
    />
  );
}

function shown(field: FieldSpec, value: unknown): string {
  if (field.kind === 'check') {
    return value === true ? 'Yes' : 'No';
  }
  const text = cellText(value);
  return text === '' ? '—' : text;
}

interface RowsEditorProps {
  table: TableSpec;
  rows: ConfigRow[];
  editable: boolean;
  onChange: (rows: ConfigRow[]) => void;
}

/**
 * Table of configuration rows (FR-SS-011 to 017): read only for an approved version, editable
 * cells with Add Row / Remove for a draft. The server validates the rows on Save.
 */
export function RowsEditor({ table, rows, editable, onChange }: Readonly<RowsEditorProps>) {
  const update = (index: number, key: string, value: unknown) =>
    onChange(rows.map((r, i) => (i === index ? { ...r, [key]: value } : r)));
  const indexed = rows.map((row, index) => ({ row, index }));
  const columns: Column<{ row: ConfigRow; index: number }>[] = table.fields.map((field) => ({
    key: field.key,
    header: field.label,
    render: ({ row, index }) =>
      editable ? (
        <Cell
          field={field}
          value={row[field.key]}
          label={`${field.label} row ${index + 1}`}
          onChange={(value) => update(index, field.key, value)}
        />
      ) : (
        shown(field, row[field.key])
      ),
  }));
  if (editable) {
    columns.push({
      key: 'remove',
      header: '',
      render: ({ index }) => (
        <Button
          size="sm"
          variant="ghost"
          aria-label={`Remove row ${index + 1}`}
          icon={<Trash2 size={14} />}
          onClick={() => onChange(rows.filter((_, i) => i !== index))}
        >
          Remove
        </Button>
      ),
    });
  }
  return (
    <Card
      title={table.title}
      flush
      actions={
        editable && (
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => onChange([...rows, { ...table.blank, [ROW_KEY]: newRowKey() }])}
          >
            Add Row
          </Button>
        )
      }
    >
      <DataTable
        caption={table.title}
        columns={columns}
        rows={indexed}
        rowKey={(r) => cellText(r.row[ROW_KEY]) || String(r.index)}
        emptyMessage="No rows yet"
      />
    </Card>
  );
}
