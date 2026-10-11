import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ApiError } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useBaseCurrency, useCompanyId, useDefaultBranchId } from '@/context/workspaceContext';
import type { ReceiptKind } from './cashieringTypes';
import { RecordAccounts } from './RecordAccounts';
import { CheckSection, PayorSection, PaymentSection, ReceiptSection } from './RecordFormSections';
import type { RecordForm } from './recordLogic';
import { bodyOf, emptyForm, formOf, isNonPremium, recordErrors } from './recordLogic';
import { recordsApi } from './recordsApi';
import type { FormSettings } from './recordsApi';
import './cashiering.css';

function fieldErrors(error: unknown): Record<string, string> {
  return error instanceof ApiError ? error.fieldErrors : {};
}

interface EditorProps {
  settings: FormSettings;
  initial: RecordForm;
  recordId?: number;
}

function RecordEditor({ settings, initial, recordId }: Readonly<EditorProps>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RecordForm>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [confirm, setConfirm] = useState<'save' | 'cancel' | null>(null);
  const set = <K extends keyof RecordForm>(key: K, value: RecordForm[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: '' }));
  };
  const update = (change: (f: RecordForm) => RecordForm) => setForm(change);
  const save = useMutation({
    mutationFn: () =>
      recordId === undefined
        ? recordsApi.create(bodyOf(companyId, form))
        : recordsApi.edit(recordId, bodyOf(companyId, form)),
    onSuccess: async (r) => {
      toast.success(`${r.recordNo} saved with status ${r.statusLabel}`);
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
      void navigate(`/cashiering/records/${r.id}`);
    },
    onError: (e) => setErrors(fieldErrors(e)),
  });
  const error = (key: string) => (errors[key] === '' ? undefined : errors[key]);
  const sections = { form, settings, set, update, error };
  const trySave = () => {
    const found = recordErrors(form, settings);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      setConfirm('save');
    }
  };
  return (
    <div className="stack">
      <ErrorAlert error={save.error} />
      <ReceiptSection {...sections} />
      <PayorSection {...sections} />
      <PaymentSection {...sections} />
      <CheckSection {...sections} />
      {!isNonPremium(form) && (
        <Card title="Accounts">
          <RecordAccounts
            lines={form.accounts}
            onChange={(lines) => set('accounts', lines)}
            error={error('accounts') ?? error('currency')}
          />
        </Card>
      )}
      <div className="worklist-actions">
        <Button variant="secondary" onClick={() => setConfirm('cancel')}>
          Cancel
        </Button>
        <Button variant="accent" busy={save.isPending} onClick={trySave}>
          Save
        </Button>
      </div>
      {confirm === 'save' && (
        <ConfirmDialog
          title="Save Record"
          effect="Record will be Saved. Do you wish to continue?"
          confirmLabel="Yes"
          onConfirm={() => {
            setConfirm(null);
            save.mutate();
          }}
          onClose={() => setConfirm(null)}
        />
      )}
      {confirm === 'cancel' && (
        <ConfirmDialog
          title="Cancel Record"
          effect="Record will be cancelled. Do you wish to continue?"
          confirmLabel="Yes"
          onConfirm={() => {
            setConfirm(null);
            void navigate(
              recordId === undefined ? '/cashiering' : `/cashiering/records/${recordId}`,
            );
          }}
          onClose={() => setConfirm(null)}
        />
      )}
    </div>
  );
}

/**
 * Create AR / Create OR (FRS.CSH.02.01, 02.02): the creation record with the entry type, the payor,
 * the payment, the bank account to post to, the receipting branch, the remarks and the accounts
 * paid. Saving gives the record its number CR-AR-… / CR-OR-… with status Created; the receipt
 * number is generated only when the Approver/Poster posts it.
 */
export default function RecordFormPage() {
  const companyId = useCompanyId();
  const branchId = useDefaultBranchId();
  const baseCurrency = useBaseCurrency();
  const { id } = useParams();
  const [params] = useSearchParams();
  const kind: ReceiptKind = params.get('kind') === 'OR' ? 'OR' : 'AR';
  const recordId = id === undefined ? undefined : Number(id);
  const settings = useQuery({
    queryKey: ['cashiering', 'record-settings', companyId],
    queryFn: () => recordsApi.settings(companyId),
    enabled: companyId > 0,
  });
  const record = useQuery({
    queryKey: ['cashiering', 'record', recordId],
    queryFn: () => recordsApi.get(recordId ?? 0),
    enabled: recordId !== undefined,
  });
  const initial = useMemo(() => {
    if (!settings.data || (recordId !== undefined && !record.data)) {
      return null;
    }
    return record.data
      ? formOf(record.data, settings.data, baseCurrency)
      : emptyForm(kind, settings.data, branchId, baseCurrency);
  }, [settings.data, record.data, recordId, kind, branchId, baseCurrency]);
  const title = recordId === undefined ? `Create ${kind}` : `Edit ${record.data?.recordNo ?? ''}`;
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title={title}
        description="Creation record of an acknowledgement or official receipt; the receipt number is issued at posting."
        backTo="/cashiering"
      />
      <ErrorAlert error={settings.error ?? record.error} />
      {initial && settings.data ? (
        <RecordEditor
          key={`${kind}-${recordId ?? 'new'}`}
          settings={settings.data}
          initial={initial}
          recordId={recordId}
        />
      ) : (
        <LoadingPanel />
      )}
    </div>
  );
}
