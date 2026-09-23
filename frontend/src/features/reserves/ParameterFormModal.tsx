import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mastersApi } from '@/api/masters';
import { reservesApi } from '@/api/reserves';
import type { IbnrMethod, ReserveParameterInput } from '@/api/reserves';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { codeOptions, enumOptions } from '@/features/assets/options';
import type { ParameterForm } from './reserveMath';

const METHODS = enumOptions(['RATE', 'CHAIN_LADDER']);
const BASES = enumOptions(['PAID', 'INCURRED']);
const PERIODS = enumOptions(['YEAR', 'QUARTER']);

interface Props {
  companyId: number;
  form: ParameterForm | null;
  onChange: (form: ParameterForm | null) => void;
}

/** Create / edit reserve parameters of a line of business (saved pending authorization). */
export function ParameterFormModal({ companyId, form, onChange }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const lines = useQuery({
    queryKey: ['dimensions', companyId, 'BUSINESS_LINE'],
    queryFn: () => mastersApi.dimensions(companyId, 'BUSINESS_LINE'),
    enabled: companyId > 0 && form !== null,
  });
  const save = useMutation({
    mutationFn: (f: ParameterForm) => {
      const body = { ...f, companyId } as ReserveParameterInput;
      return f.id === undefined
        ? reservesApi.createParameter(body)
        : reservesApi.updateParameter(f.id, body);
    },
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['reserve-parameters'] });
      onChange(null);
      toast.success(
        `Parameters ${p.businessLine} from ${p.effectiveFrom} saved – pending authorization`,
      );
    },
  });
  const set = (patch: ParameterForm) => form && onChange({ ...form, ...patch });
  const chainLadder = form?.ibnrMethod === 'CHAIN_LADDER';

  return (
    <Modal
      title={form?.id === undefined ? 'New reserve parameters' : 'Edit reserve parameters'}
      open={form !== null}
      onClose={() => onChange(null)}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => form && save.mutate(form)}>
          Save for authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      {form !== null && (
        <div className="form-grid">
          <SelectInput
            label="Line of business"
            required
            blank="Select"
            disabled={form.id !== undefined}
            value={form.businessLine}
            options={codeOptions(lines.data ?? [])}
            onChange={(businessLine) => set({ businessLine })}
          />
          <TextInput
            label="Effective from"
            type="date"
            required
            disabled={form.id !== undefined}
            value={form.effectiveFrom}
            onChange={(effectiveFrom) => set({ effectiveFrom })}
          />
          <SelectInput
            label="IBNR method"
            required
            value={form.ibnrMethod}
            options={METHODS}
            onChange={(v) => set({ ibnrMethod: v as IbnrMethod })}
          />
          <NumberInput
            label="IBNR rate % of earned premium"
            disabled={chainLadder}
            value={form.ibnrRate}
            onChange={(ibnrRate) => set({ ibnrRate })}
          />
          <SelectInput
            label="Triangle"
            disabled={!chainLadder}
            value={form.triangleBasis}
            options={BASES}
            onChange={(v) => set({ triangleBasis: v as ParameterForm['triangleBasis'] })}
          />
          <SelectInput
            label="Development period"
            disabled={!chainLadder}
            value={form.developmentPeriod}
            options={PERIODS}
            onChange={(v) => set({ developmentPeriod: v as ParameterForm['developmentPeriod'] })}
          />
          <NumberInput
            label="Accident periods"
            step="1"
            disabled={!chainLadder}
            value={form.accidentPeriods}
            onChange={(accidentPeriods) => set({ accidentPeriods })}
          />
          <NumberInput
            label="Margin for adverse deviation %"
            value={form.mfadPct}
            onChange={(mfadPct) => set({ mfadPct })}
          />
          <NumberInput
            label="ULAE provision %"
            value={form.ulaePct}
            onChange={(ulaePct) => set({ ulaePct })}
          />
          <NumberInput
            label="Expected loss ratio % (LAT)"
            value={form.expectedLossRatio}
            onChange={(expectedLossRatio) => set({ expectedLossRatio })}
          />
          <NumberInput
            label="Treaty RI commission %"
            value={form.treatyCommissionPct}
            onChange={(treatyCommissionPct) => set({ treatyCommissionPct })}
          />
          <NumberInput
            label="FAC RI commission %"
            value={form.facCommissionPct}
            onChange={(facCommissionPct) => set({ facCommissionPct })}
          />
          <TextInput
            label="Remarks"
            value={form.remarks}
            onChange={(remarks) => set({ remarks })}
          />
        </div>
      )}
    </Modal>
  );
}
