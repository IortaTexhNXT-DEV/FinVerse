import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, CircleAlert, FileDown, Save } from 'lucide-react';
import { useState } from 'react';
import { productMaintApi } from '@/api/productmaint';
import type { PackageDates, PackageRequest, Scheme, Signoff } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, TextInput } from '@/features/assets/FormControls';
import { formatDateTime } from '@/utils/format';
import { TermsView } from './TermsView';

const SIGNOFF_COLUMNS: Column<Signoff>[] = [
  { key: 'ref', header: 'Reference', render: (s) => <span className="mono">{s.reference}</span> },
  { key: 'decision', header: 'Decision', render: (s) => <StatusBadge status={s.decision} /> },
  { key: 'by', header: 'By', render: (s) => s.signedBy },
  { key: 'at', header: 'On', render: (s) => formatDateTime(s.signedAt) },
  { key: 'comment', header: 'Comment', render: (s) => s.comment ?? '—' },
];

function RequirementsEditor({ request }: Readonly<{ request: PackageRequest }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const terms = request.proposedTerms ?? request.requestedTerms;
  const [scheme, setScheme] = useState<Scheme>(terms.scheme);
  const [dates, setDates] = useState<PackageDates>(terms.dates);
  const s = (patch: Partial<Scheme>) => setScheme({ ...scheme, ...patch });
  const d = (patch: Partial<PackageDates>) => setDates({ ...dates, ...patch });
  const date = (v: string) => (v === '' ? undefined : v);
  const save = useMutation({
    mutationFn: () => productMaintApi.updateRequirements(request.id, scheme, dates),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['package-request', request.id] });
      await queryClient.invalidateQueries({ queryKey: ['package-request-tab', request.id] });
      toast.success('Requirements saved');
    },
  });
  return (
    <Card
      title="Rate scheme, computation basis and dates"
      actions={
        <Button
          size="sm"
          variant="accent"
          icon={<Save size={14} />}
          busy={save.isPending}
          onClick={() => save.mutate()}
        >
          Save Requirements
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <NumberInput
          label="Scheme rate %"
          step="0.0001"
          value={scheme.defaultRate}
          onChange={(defaultRate) => s({ defaultRate })}
        />
        <NumberInput
          label="Minimum premium"
          value={scheme.minimumPremium}
          onChange={(minimumPremium) => s({ minimumPremium })}
        />
        <NumberInput
          label="Commission %"
          value={scheme.commissionRate}
          onChange={(commissionRate) => s({ commissionRate })}
        />
        <NumberInput
          label="Package TSI limit"
          value={scheme.maxSumInsured}
          onChange={(maxSumInsured) => s({ maxSumInsured })}
        />
        <TextInput
          label="Computation basis"
          required
          hint="How the premium is computed (PQ12)."
          value={scheme.ratingBasisNote}
          onChange={(ratingBasisNote) => s({ ratingBasisNote })}
        />
        <TextInput
          label="Effective from"
          type="date"
          required
          value={dates.effectiveFrom}
          onChange={(v) => d({ effectiveFrom: date(v) })}
        />
        <TextInput
          label="Package start"
          type="date"
          value={dates.packageStartDate}
          onChange={(v) => d({ packageStartDate: date(v) })}
        />
        <TextInput
          label="Package end"
          type="date"
          required
          value={dates.packageEndDate}
          onChange={(v) => d({ packageEndDate: date(v) })}
        />
        <TextInput
          label="Anniversary"
          type="date"
          value={dates.anniversaryDate}
          onChange={(v) => d({ anniversaryDate: date(v) })}
        />
      </div>
    </Card>
  );
}

/**
 * Requirements and ManCom sign-off (BRPM.015): what the requirements pack still lacks, the
 * proposed terms (editable during the preparation), the package slip to print and sign, and the
 * ManCom decisions with their sign-off reference.
 */
export function RequirementsTab({ request }: Readonly<{ request: PackageRequest }>) {
  const { can } = useAuth();
  const download = useFileDownload();
  const status = useQuery({
    queryKey: ['package-request-tab', request.id, 'requirements'],
    queryFn: () => productMaintApi.requirements(request.id),
  });
  const missing = status.data?.missing ?? [];
  const editable = request.status === 'REQUIREMENTS_PREP' && can('PKG_NEGOTIATE');
  return (
    <div className="stack">
      <ErrorAlert error={status.error ?? download.error} />
      <Card
        title="Requirements pack"
        actions={
          <Button
            size="sm"
            variant="secondary"
            icon={<FileDown size={14} />}
            onClick={() => download.mutate(() => productMaintApi.packageSlipPdf(request.id))}
          >
            Package Slip
          </Button>
        }
      >
        {missing.length === 0 ? (
          <p>
            <CheckCircle2 size={16} className="text-success" aria-hidden="true" /> The requirements
            pack is complete.
          </p>
        ) : (
          <ul className="checklist">
            {missing.map((m) => (
              <li key={m} className="checklist-item">
                <CircleAlert size={16} className="text-danger" aria-label="Missing" /> {m}
              </li>
            ))}
          </ul>
        )}
        <p className="muted">
          Print the package slip, have it signed and attach it as “Package slip (signed)” in
          Documents before submitting the requirements to ManCom.
        </p>
      </Card>
      {editable && <RequirementsEditor key={request.milestones.termsFinalAt} request={request} />}
      <TermsView terms={request.proposedTerms ?? request.requestedTerms} title="Proposed terms" />
      <Card title="ManCom decisions" flush>
        <DataTable<Signoff>
          rows={status.data?.signoffs ?? []}
          rowKey={(s) => s.id}
          emptyMessage="No ManCom decision yet"
          columns={SIGNOFF_COLUMNS}
        />
      </Card>
    </div>
  );
}
