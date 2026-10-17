import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi, RATE_CODES } from '@/api/catalog';
import type { MotorCoverage, RateCode } from '@/api/catalog';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { enumOptions } from '@/features/assets/options';
import { today } from '@/utils/format';

export type RateTable = 'taxes' | 'short-period' | 'motor-limits';

interface RateForm {
  rateCode: RateCode;
  lineCode: string;
  rate?: number;
  monthsCovered?: number;
  coverage: MotorCoverage;
  limitAmount?: number;
  premium?: number;
  effectiveFrom: string;
  effectiveTo: string;
}

const TITLES: Record<RateTable, string> = {
  taxes: 'New tax or rating factor',
  'short-period': 'New short-period rate',
  'motor-limits': 'New BI / PD limit premium',
};

function send(table: RateTable, f: RateForm): Promise<unknown> {
  const dates = { effectiveFrom: f.effectiveFrom, effectiveTo: f.effectiveTo || undefined };
  if (table === 'taxes') {
    return catalogApi.createTax({
      rateCode: f.rateCode,
      lineCode: f.lineCode || undefined,
      rate: f.rate ?? 0,
      ...dates,
    });
  }
  if (table === 'short-period') {
    return catalogApi.createShortPeriod({
      monthsCovered: f.monthsCovered ?? 0,
      percentOfAnnual: f.rate ?? 0,
      ...dates,
    });
  }
  return catalogApi.createMotorLimit({
    coverage: f.coverage,
    limitAmount: f.limitAmount ?? 0,
    premium: f.premium ?? 0,
    ...dates,
  });
}

function TableFields({
  table,
  form,
  set,
}: Readonly<{ table: RateTable; form: RateForm; set: (p: Partial<RateForm>) => void }>) {
  if (table === 'taxes') {
    return (
      <>
        <SelectInput
          label="Rate"
          required
          value={form.rateCode}
          options={enumOptions(RATE_CODES)}
          onChange={(v) => set({ rateCode: v as RateCode })}
        />
        <TextInput
          label="Line code"
          upper
          hint="Blank = every line"
          value={form.lineCode}
          onChange={(lineCode) => set({ lineCode })}
        />
        <NumberInput
          label="Rate %"
          required
          step="0.0001"
          value={form.rate}
          onChange={(rate) => set({ rate })}
        />
      </>
    );
  }
  if (table === 'short-period') {
    return (
      <>
        <NumberInput
          label="Months covered"
          required
          step="1"
          value={form.monthsCovered}
          onChange={(monthsCovered) => set({ monthsCovered })}
        />
        <NumberInput
          label="% of annual premium"
          required
          value={form.rate}
          onChange={(rate) => set({ rate })}
        />
      </>
    );
  }
  return (
    <>
      <SelectInput
        label="Coverage"
        required
        value={form.coverage}
        options={[
          { value: 'BI', label: 'Bodily injury (BI)' },
          { value: 'PD', label: 'Property damage (PD)' },
        ]}
        onChange={(v) => set({ coverage: v as MotorCoverage })}
      />
      <NumberInput
        label="Limit"
        required
        value={form.limitAmount}
        onChange={(limitAmount) => set({ limitAmount })}
      />
      <NumberInput
        label="Premium"
        required
        value={form.premium}
        onChange={(premium) => set({ premium })}
      />
    </>
  );
}

/** Adds a row to one of the rate tables (effective-dated, maker-checker). */
export function RateModal({ table, onClose }: Readonly<{ table: RateTable; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RateForm>({
    rateCode: 'DST',
    lineCode: '',
    coverage: 'BI',
    effectiveFrom: today(),
    effectiveTo: '',
  });
  const set = (patch: Partial<RateForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () => send(table, form),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'rates'] });
      toast.success('Rate saved – pending authorization');
      onClose();
    },
  });
  return (
    <Modal
      title={TITLES[table]}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
          Save for Authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TableFields table={table} form={form} set={set} />
        <TextInput
          label="Effective from"
          type="date"
          required
          value={form.effectiveFrom}
          onChange={(effectiveFrom) => set({ effectiveFrom })}
        />
        <TextInput
          label="Effective to"
          type="date"
          value={form.effectiveTo}
          onChange={(effectiveTo) => set({ effectiveTo })}
        />
      </div>
    </Modal>
  );
}
