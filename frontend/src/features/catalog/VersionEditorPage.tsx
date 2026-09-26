import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarDays, FileSignature, Percent, Save, Send, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { productCatalogApi } from '@/api/productCatalog';
import type { VersionDetail } from '@/api/productCatalog';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { VersionCheckpoint } from './VersionCheckpoint';
import { VersionCoveragesSection, VersionInsurersSection } from './VersionPanelSections';
import { VersionSchemeSection } from './VersionSchemeSection';
import { VersionTermsSection } from './VersionTermsSection';
import { formOf, toInput, validateVersionForm } from './versionForm';
import type { VersionForm } from './versionForm';

const TABS = [
  { id: 'scheme', label: 'Rate Scheme & Dates' },
  { id: 'coverages', label: 'Coverages' },
  { id: 'insurers', label: 'Insurers' },
  { id: 'terms', label: 'Insurer Terms' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function Summary({ detail }: Readonly<{ detail: VersionDetail }>) {
  const s = detail.summary;
  return (
    <RecordSummary
      title={s.productName}
      chips={
        <>
          <ReferenceChip label="Product" value={s.productCode} />
          <ReferenceChip label="Version" value={String(s.versionNo)} />
          <StatusBadge status={s.status} />
        </>
      }
      flags={s.sourceRequestNo && <span className="tag">Request {s.sourceRequestNo}</span>}
      facts={[
        { icon: CalendarDays, label: 'Effective', value: formatDate(s.effectiveFrom) },
        { icon: CalendarDays, label: 'Package End', value: formatDate(s.packageEndDate) },
        { icon: Percent, label: 'Rate %', value: detail.scheme.defaultRate ?? 'Per insurer' },
        { icon: UserRound, label: 'Set Up By', value: s.maker },
        { icon: FileSignature, label: 'ManCom Ref.', value: detail.mancomSignoffRef },
      ]}
    />
  );
}

function Editor({ detail }: Readonly<{ detail: VersionDetail }>) {
  const { can } = useAuth();
  const toast = useToast();
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const s = detail.summary;
  const [tab, setTab] = useState<TabId>('scheme');
  const [form, setForm] = useState<VersionForm>(() => formOf(detail));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const readOnly = s.status !== 'DRAFT' || !can('PRODUCT_MAINTAIN');
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['catalog'] });
  const save = useMutation({
    mutationFn: () =>
      productCatalogApi.saveVersion(s.productCode, s.versionNo, toInput(form, companyId)),
    onSuccess: async (saved) => {
      setForm(formOf(saved));
      await refresh();
      toast.success(`Version ${s.versionNo} saved`);
    },
  });
  const submit = useMutation({
    mutationFn: async () => {
      await productCatalogApi.saveVersion(s.productCode, s.versionNo, toInput(form, companyId));
      return productCatalogApi.submitVersion(s.productCode, s.versionNo);
    },
    onSuccess: async () => {
      await refresh();
      toast.success(`Version ${s.versionNo} submitted for validation`);
    },
  });
  const run = (action: typeof save) => {
    const found = validateVersionForm(form, today());
    setErrors(found);
    if (Object.keys(found).length === 0) {
      action.mutate();
    } else {
      toast.error('Correct the highlighted fields');
    }
  };
  const props = { form, errors, readOnly, onChange: setForm, lineCode: detail.lineCode };
  return (
    <div className="stack">
      {!readOnly && (
        <div className="row">
          <Button
            variant="secondary"
            icon={<Save size={16} />}
            busy={save.isPending}
            onClick={() => run(save)}
          >
            Save Draft
          </Button>
          <Button
            variant="accent"
            icon={<Send size={16} />}
            busy={submit.isPending}
            onClick={() => run(submit)}
          >
            Submit for Validation
          </Button>
        </div>
      )}
      <ErrorAlert error={save.error ?? submit.error} />
      {detail.returnedReason && s.status === 'DRAFT' && (
        <div className="alert warning">Returned by the validator: {detail.returnedReason}</div>
      )}
      <VersionCheckpoint detail={detail} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'scheme' && <VersionSchemeSection {...props} />}
      {tab === 'coverages' && <VersionCoveragesSection {...props} />}
      {tab === 'insurers' && <VersionInsurersSection {...props} />}
      {tab === 'terms' && <VersionTermsSection {...props} />}
    </div>
  );
}

/**
 * Version editor (BRPM.007, PMADD01/02/06): the rate scheme, dates, coverages, insurers and insurer
 * terms of a package version. MBS edits a DRAFT and submits it; the validator confirms the
 * checklist and releases it or returns it. Released versions are read-only.
 */
export default function VersionEditorPage() {
  const params = useParams();
  const code = params.code ?? '';
  const versionNo = Number(params.versionNo);
  const version = useQuery({
    queryKey: ['catalog', 'version', code, versionNo],
    queryFn: () => productCatalogApi.version(code, versionNo),
  });
  if (version.data === undefined) {
    return version.error ? (
      <ErrorAlert error={version.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const d = version.data;
  return (
    <div className="stack">
      <PageHeader
        backTo={`/catalog/products/${code}`}
        section="Product Maintenance · Package Version"
        title={`${code} – Version ${versionNo}`}
        description={
          d.summary.changeSummary ?? 'Rate scheme, coverages and insurer terms of the package.'
        }
      />
      <Summary detail={d} />
      <Editor key={`${d.summary.status}-${d.summary.versionNo}`} detail={d} />
    </div>
  );
}
