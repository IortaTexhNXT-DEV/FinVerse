import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { humanize } from '@/utils/format';
import type { ConfigContent, ConfigRow, TemplateContent } from './api';
import { screeningSetupApi } from './api';
import { TEMPLATE_TYPES, cellText, withRows } from './configSpec';
import { FIELD_TABLE } from './configTables';
import { RowsEditor } from './RowsEditor';
import { VersionWorkbench } from './VersionWorkbench';

type TemplateType = (typeof TEMPLATE_TYPES)[number];

const DESCRIPTIONS: Record<TemplateType, string> = {
  KYC_REVIEW:
    'The KYC review an investigator completes on a name-match or high-risk case (SNSRP-104, 501).',
  TRANSACTION_REVIEW: 'The review of a suspicious transaction (SNSRP-104, 501).',
  EDD: 'Enhanced due diligence of high-risk and PEP clients with an active policy (SNSRP-104, 303).',
  STR: 'The fields of the suspicious transaction report and the case data that prefills them (SNSRP-105, 705).',
};

function controlOf(field: ConfigRow, id: string) {
  const type = cellText(field.dataType);
  if (type === 'LONG_TEXT') {
    return <textarea id={id} className="textarea" rows={2} disabled />;
  }
  if (type === 'CHECKBOX') {
    return <input id={id} type="checkbox" disabled />;
  }
  if (type === 'LOV') {
    return (
      <select id={id} className="select" disabled>
        <option>{`List ${cellText(field.lovType)}`}</option>
      </select>
    );
  }
  const inputType: Record<string, string> = {
    DATE: 'date',
    NUMBER: 'number',
    AMOUNT: 'number',
    ATTACHMENT: 'file',
  };
  return <input id={id} className="input" type={inputType[type] ?? 'text'} disabled />;
}

/** Read-only preview of the review form as the investigator will see it (FR-SS-016). */
function Preview({ template }: Readonly<{ template: TemplateContent }>) {
  const sections = [...new Set(template.fields.map((f) => cellText(f.section)))];
  return (
    <Card title={`Preview: ${template.name}`}>
      {template.fields.length === 0 && <EmptyState message="Add fields to preview the form" />}
      <div className="stack">
        {sections.map((section) => (
          <div key={section} className="stack">
            <strong>{section}</strong>
            <div className="form-grid">
              {template.fields
                .filter((f) => cellText(f.section) === section)
                .map((f) => (
                  <Field
                    key={cellText(f.code)}
                    label={cellText(f.label) || cellText(f.code)}
                    required={f.mandatory === true}
                    hint={cellText(f.help) || undefined}
                  >
                    {(id) => controlOf(f, id)}
                  </Field>
                ))}
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}

function renderTemplate(type: TemplateType) {
  return function TemplateRows(
    content: ConfigContent,
    editable: boolean,
    onChange: (c: ConfigContent) => void,
  ) {
    const template: TemplateContent = content.template ?? {
      templateType: type,
      name: humanize(type),
      fields: [],
    };
    return (
      <>
        <Card title="Template">
          <Field label="Template Name" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={100}
                disabled={!editable}
                value={template.name}
                onChange={(e) =>
                  onChange({ ...content, template: { ...template, name: e.target.value } })
                }
              />
            )}
          </Field>
        </Card>
        <RowsEditor
          table={FIELD_TABLE}
          rows={template.fields}
          editable={editable}
          onChange={(rows) => onChange(withRows({ ...content, template }, 'template.fields', rows))}
        />
        <Preview template={template} />
      </>
    );
  };
}

/**
 * Templates (SNSRP-104, 105; FR-SS-016, 017): the KYC review, transaction review, EDD and STR
 * templates as dated versions with a field designer and a preview. A review keeps the template
 * version it was started with.
 */
export default function TemplatesPage() {
  const [search] = useSearchParams();
  const linked = Number(search.get('version') ?? 0);
  const [tab, setTab] = useState<TemplateType>();
  const link = useQuery({
    queryKey: ['screening-setup', 'version', linked],
    queryFn: () => screeningSetupApi.version(linked),
    enabled: linked > 0,
  });
  const linkedScope = TEMPLATE_TYPES.find((t) => t === link.data?.version.scope);
  const active: TemplateType = tab ?? linkedScope ?? 'KYC_REVIEW';
  return (
    <div className="stack">
      <PageHeader
        section="Setup & Administration · Compliance Setup"
        title="Templates"
        description="Review and STR templates: sections, fields, mandatory flags and lists of values, with a preview of the form. Each change is a new version approved by a Compliance Checker."
      />
      <Tabs
        tabs={TEMPLATE_TYPES.map((t) => ({ id: t, label: humanize(t) }))}
        active={active}
        onChange={setTab}
      />
      <p className="muted">{DESCRIPTIONS[active]}</p>
      <VersionWorkbench
        key={active}
        type="TEMPLATE"
        scope={active}
        initialVersion={linkedScope === active ? linked : undefined}
        renderRows={renderTemplate(active)}
      />
    </div>
  );
}
