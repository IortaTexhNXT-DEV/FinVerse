import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import type { ConfigContent, ConfigType, LayoutContent } from './api';
import { screeningSetupApi } from './api';
import { rowsOf, withRows } from './configSpec';
import { CONFIG_TYPES, typeSpec } from './configTables';
import type { TypeSpec } from './configSpec';
import { RowsEditor } from './RowsEditor';
import { VersionWorkbench } from './VersionWorkbench';

const DEFAULT_LAYOUT: LayoutContent = {
  format: 'CSV',
  delimiter: ',',
  encoding: 'UTF-8',
  columns: [],
};

function LayoutHeader({
  layout,
  editable,
  onChange,
}: Readonly<{ layout: LayoutContent; editable: boolean; onChange: (l: LayoutContent) => void }>) {
  return (
    <Card title="File Format">
      <div className="form-grid">
        <Field label="Format" required>
          {(id) => (
            <select
              id={id}
              className="select"
              disabled={!editable}
              value={layout.format}
              onChange={(e) => onChange({ ...layout, format: e.target.value })}
            >
              {['CSV', 'FIXED', 'XLSX', 'XML'].map((f) => (
                <option key={f} value={f}>
                  {f === 'FIXED' ? 'Fixed width' : f}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Delimiter" hint="CSV only">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={5}
              disabled={!editable}
              value={layout.delimiter ?? ''}
              onChange={(e) => onChange({ ...layout, delimiter: e.target.value })}
            />
          )}
        </Field>
        <Field label="Encoding" required>
          {(id) => (
            <input
              id={id}
              className="input"
              disabled={!editable}
              value={layout.encoding}
              onChange={(e) => onChange({ ...layout, encoding: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Card>
  );
}

function renderType(spec: TypeSpec) {
  return function TypeRows(
    content: ConfigContent,
    editable: boolean,
    onChange: (c: ConfigContent) => void,
  ) {
    const layout = content.layout ?? DEFAULT_LAYOUT;
    return (
      <>
        {spec.type === 'STR_LAYOUT' && (
          <LayoutHeader
            layout={layout}
            editable={editable}
            onChange={(l) => onChange({ ...content, layout: l })}
          />
        )}
        {spec.tables.map((table) => (
          <RowsEditor
            key={table.key}
            table={table}
            rows={rowsOf(content, table.key)}
            editable={editable}
            onChange={(rows) => onChange(withRows(content, table.key, rows))}
          />
        ))}
      </>
    );
  };
}

/**
 * Configuration Versions (SNSRP-101-109; FR-SS-010 to 015, 017, 019): one tab per configuration
 * type with its versions, the rows of the selected version, its changes against the version in
 * force and the maker-checker actions. Templates have their own screen.
 */
export default function ConfigVersionsPage() {
  const [search] = useSearchParams();
  const linked = Number(search.get('version') ?? 0);
  const [tab, setTab] = useState<ConfigType>();
  const link = useQuery({
    queryKey: ['screening-setup', 'version', linked],
    queryFn: () => screeningSetupApi.version(linked),
    enabled: linked > 0,
  });
  const spec = typeSpec(tab ?? link.data?.version.type);
  return (
    <div className="stack">
      <PageHeader
        section="Setup & Administration · Compliance Setup"
        title="Configuration Versions"
        description="Matching criteria, risk rules, approval, assignment and SLA matrices, validation rules and the STR layout, each as dated versions: a Compliance Officer drafts and submits, a Compliance Checker approves or rejects."
      />
      <Tabs
        tabs={CONFIG_TYPES.map((t) => ({ id: t.type, label: t.label }))}
        active={spec.type}
        onChange={setTab}
      />
      <p className="muted">{spec.description}</p>
      <VersionWorkbench
        key={spec.type}
        type={spec.type}
        initialVersion={link.data?.version.type === spec.type ? linked : undefined}
        renderRows={renderType(spec)}
      />
    </div>
  );
}
