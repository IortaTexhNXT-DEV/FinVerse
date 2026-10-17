import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, PackageCheck } from 'lucide-react';
import { useMemo, useState } from 'react';
import { configPromotionApi } from '@/api/configPromotion';
import type { CatalogueDataset, ConfigPackage } from '@/api/configPromotion';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { EnvironmentNotice } from './EnvironmentNotice';
import { byGroup, defaultSelection } from './promotion';

/**
 * Export Configuration (Configuration Promotion): the configuration catalogue of this environment
 * by group, with the number of items of each dataset; the administrator exports every dataset or a
 * selection into a signed package, or the datasets changed since a baseline.
 */
export default function ExportPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const catalogue = useQuery({
    queryKey: ['config-promotion', 'catalogue'],
    queryFn: configPromotionApi.catalogue,
  });
  const baselines = useQuery({
    queryKey: ['config-promotion', 'baselines'],
    queryFn: configPromotionApi.baselines,
  });
  const [includeUsers, setIncludeUsers] = useState(false);
  const [chosen, setChosen] = useState<string[] | null>(null);
  const [group, setGroup] = useState('');
  const [baselineId, setBaselineId] = useState('');
  const [description, setDescription] = useState('');
  const [last, setLast] = useState<ConfigPackage | null>(null);
  const datasets = useMemo(
    () => (catalogue.data?.datasets ?? []).filter((d) => !d.collection),
    [catalogue.data],
  );
  const selected = chosen ?? defaultSelection(datasets, includeUsers);
  const exportPackage = useMutation({
    mutationFn: () =>
      configPromotionApi.exportPackage({
        datasets: chosen ?? [],
        includeUsers,
        baselineId: baselineId === '' ? null : Number(baselineId),
        description,
      }),
    onSuccess: async (p) => {
      setLast(p);
      await queryClient.invalidateQueries({ queryKey: ['config-promotion'] });
      toast.success(`Package ${p.packageNo} exported: ${p.datasetCount} datasets`);
    },
  });
  const toggle = (code: string) =>
    setChosen(selected.includes(code) ? selected.filter((c) => c !== code) : [...selected, code]);
  const groups = byGroup(datasets, catalogue.data?.groups ?? []);
  const shown = group === '' ? datasets : datasets.filter((d) => d.group === group);
  const groupName = (code: string) =>
    catalogue.data?.groups.find((g) => g.code === code)?.name ?? code;

  return (
    <div className="stack">
      <PageHeader
        section="Configuration Promotion"
        title="Export Configuration"
        description="Exports the set-up of this environment (masters, configuration, rules and validations) without transactions, as a signed package for another environment."
      />
      <EnvironmentNotice />
      <ErrorAlert error={catalogue.error ?? exportPackage.error} />
      {last !== null && (
        <Notice tone="success" title={`Package ${last.packageNo} is ready`}>
          {`${last.datasetCount} datasets, ${last.rowCount.toLocaleString('en-PH')} items. Download it for the target environment or find it under Packages and History.`}
        </Notice>
      )}
      <Card
        title="What to Export"
        actions={
          <div className="row">
            {last !== null && (
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                onClick={() =>
                  void configPromotionApi.file(last.id).then((f) => saveFile(f.blob, f.fileName))
                }
              >
                Download Package
              </Button>
            )}
            <Button
              variant="accent"
              icon={<PackageCheck size={16} />}
              busy={exportPackage.isPending}
              disabled={!can('CONFIG_EXPORT') || selected.length === 0}
              onClick={() => exportPackage.mutate()}
            >
              Export {selected.length} Dataset(s)
            </Button>
          </div>
        }
      >
        <div className="grid-3">
          <Field
            label="Include users"
            hint="Users with their group profiles and data scope; never passwords or second factor."
          >
            {(id) => (
              <select
                id={id}
                className="select"
                value={includeUsers ? 'Y' : 'N'}
                onChange={(e) => {
                  setIncludeUsers(e.target.value === 'Y');
                  setChosen(null);
                }}
              >
                <option value="N">No</option>
                <option value="Y">Yes</option>
              </select>
            )}
          </Field>
          <Field
            label="Changes since baseline"
            hint="Exports only the datasets changed since the baseline."
          >
            {(id) => (
              <select
                id={id}
                className="select"
                value={baselineId}
                onChange={(e) => setBaselineId(e.target.value)}
              >
                <option value="">Every selected dataset (full)</option>
                {(baselines.data ?? [])
                  .filter((b) => b.active)
                  .map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
              </select>
            )}
          </Field>
          <Field label="Purpose">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={500}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="UAT sign-off configuration"
              />
            )}
          </Field>
        </div>
      </Card>
      <Card
        title="Configuration Catalogue"
        flush
        actions={
          <div className="row">
            <label className="visually-hidden" htmlFor="cfp-group">
              Group
            </label>
            <select
              id="cfp-group"
              className="select"
              value={group}
              onChange={(e) => setGroup(e.target.value)}
            >
              <option value="">All groups</option>
              {groups.map((g) => (
                <option key={g.code} value={g.code}>
                  {g.name}
                </option>
              ))}
            </select>
            <Button variant="ghost" size="sm" onClick={() => setChosen(null)}>
              Select All
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setChosen([])}>
              Clear
            </Button>
          </div>
        }
      >
        <DataTable<CatalogueDataset>
          callout="configuration-catalogue"
          loading={catalogue.isLoading}
          rows={shown}
          rowKey={(d) => d.code}
          columns={[
            {
              key: 'pick',
              header: <span className="visually-hidden">Export</span>,
              width: '48px',
              render: (d) => (
                <input
                  type="checkbox"
                  aria-label={`Export ${d.name}`}
                  checked={selected.includes(d.code)}
                  disabled={d.users && !includeUsers}
                  onChange={() => toggle(d.code)}
                />
              ),
            },
            { key: 'name', header: 'Dataset', render: (d) => <strong>{d.name}</strong> },
            { key: 'group', header: 'Group', render: (d) => groupName(d.group) },
            { key: 'key', header: 'Identified By', render: (d) => d.key.join(', ') },
            {
              key: 'items',
              header: 'Items Here',
              numeric: true,
              width: '110px',
              render: (d) => d.items.toLocaleString('en-PH'),
            },
            {
              key: 'notes',
              header: 'Kept by Each Environment',
              render: (d) => notes(d),
            },
          ]}
        />
      </Card>
      {catalogue.data !== undefined && (
        <Card title="Never Promoted" flush>
          <DataTable
            callout="never-promoted"
            rows={catalogue.data.excluded}
            rowKey={(r) => r.reason}
            columns={[
              { key: 'text', header: 'Kind of Data', render: (r) => r.text },
              {
                key: 'tables',
                header: 'Record Types',
                numeric: true,
                width: '140px',
                render: (r) => r.tables,
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}

function notes(d: CatalogueDataset): string {
  const parts: string[] = [];
  if (d.environmentFields.length > 0) {
    parts.push(d.environmentFields.join(', '));
  }
  if (d.environmentRows) {
    parts.push('Values of this environment (see Environment Overrides)');
  }
  if (d.optional) {
    parts.push('Optional: not selected by default');
  }
  if (d.users) {
    parts.push('Users: only when users are included');
  }
  return parts.length === 0 ? '—' : parts.join('; ');
}
