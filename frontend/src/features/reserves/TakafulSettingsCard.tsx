import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { reservesApi } from '@/api/reserves';
import type { TakafulSetting, TakafulSettingInput } from '@/api/reserves';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, TextInput } from '@/features/assets/FormControls';
import { takafulProducts } from './reserveMath';
import { awaitsOtherChecker } from '@/utils/makerChecker';

/** One-line description of the settings in force. */
function takafulSummary(s: TakafulSetting | undefined): string {
  if (!s?.enabled) {
    return 'Not enabled: valuation runs do not compute the takaful surplus.';
  }
  return `Enabled for ${takafulProducts(s.productCodes)}: participants' share ${s.participantSharePct}%, tax ${s.taxPct}%, cost centre ${s.costCenter ?? '–'}.`;
}

interface FormProps {
  form: TakafulSettingInput;
  busy: boolean;
  onChange: (form: TakafulSettingInput) => void;
  onSave: () => void;
  onDiscard: () => void;
}

/** Editable takaful settings. */
function TakafulForm({ form, busy, onChange, onSave, onDiscard }: Readonly<FormProps>) {
  const set = (patch: Partial<TakafulSettingInput>) => onChange({ ...form, ...patch });
  return (
    <div className="form-grid">
      <label className="checkbox" style={{ alignSelf: 'end' }}>
        <input
          type="checkbox"
          checked={form.enabled}
          onChange={(e) => set({ enabled: e.target.checked })}
        />
        Compute the surplus in valuation runs
      </label>
      <TextInput
        label="Takaful products (comma separated, blank = all)"
        upper
        value={form.productCodes}
        onChange={(productCodes) => set({ productCodes })}
      />
      <NumberInput
        label="Participants' share %"
        value={form.participantSharePct}
        onChange={(v) => set({ participantSharePct: v ?? 0 })}
      />
      <NumberInput label="Tax %" value={form.taxPct} onChange={(v) => set({ taxPct: v ?? 0 })} />
      <TextInput
        label="Cost centre"
        upper
        value={form.costCenter}
        onChange={(costCenter) => set({ costCenter })}
      />
      <div className="row" style={{ alignSelf: 'end', gap: 'var(--space-2)' }}>
        <Button variant="accent" busy={busy} onClick={onSave}>
          Save for authorization
        </Button>
        <Button variant="ghost" onClick={onDiscard}>
          Discard
        </Button>
      </div>
    </div>
  );
}

/** Takaful surplus (Mudharabah) settings of the company: optional, maker-checker. */
export function TakafulSettingsCard({ companyId }: Readonly<{ companyId: number }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<TakafulSettingInput | null>(null);
  const setting = useQuery({
    queryKey: ['reserve-takaful', companyId],
    queryFn: () => reservesApi.takaful(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['reserve-takaful'] });
  const save = useMutation({
    mutationFn: (f: TakafulSettingInput) => reservesApi.saveTakaful({ ...f, companyId }),
    onSuccess: async () => {
      await refresh();
      setForm(null);
      toast.success('Takaful settings saved – pending authorization');
    },
  });
  const authorize = useMutation({
    mutationFn: () => reservesApi.authorizeTakaful(companyId),
    onSuccess: async () => {
      await refresh();
      toast.success('Takaful settings authorized');
    },
  });
  const s = setting.data;
  const canAuthorize = awaitsOtherChecker(s, user?.username) && can('MASTER_AUTHORIZE');
  const canEdit = form === null && s !== undefined && can('MASTER_MAINTAIN');

  return (
    <Card
      title="Takaful surplus (PGIBR074)"
      actions={
        <>
          {s?.recordStatus && <StatusBadge status={s.recordStatus} />}
          {canAuthorize && (
            <Button size="sm" variant="secondary" onClick={() => authorize.mutate()}>
              Authorize
            </Button>
          )}
          {canEdit && (
            <Button size="sm" variant="secondary" onClick={() => setForm({ ...s, companyId })}>
              Edit
            </Button>
          )}
        </>
      }
    >
      <ErrorAlert error={setting.error ?? authorize.error ?? save.error} />
      {form === null ? (
        <p className="muted">{takafulSummary(s)}</p>
      ) : (
        <TakafulForm
          form={form}
          busy={save.isPending}
          onChange={setForm}
          onSave={() => save.mutate(form)}
          onDiscard={() => setForm(null)}
        />
      )}
    </Card>
  );
}
