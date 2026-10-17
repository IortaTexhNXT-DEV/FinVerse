import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { PolicyData, PolicyDetail } from '@/api/submitted';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useBaseCurrency, useCompanyId } from '@/context/workspaceContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { SBM_LOV } from '../common/submittedCodes';
import { emptyPolicy, shown, textOrNull } from './policyForm';

const BUSINESS_TYPES = [
  { value: 'NB', label: 'New Business' },
  { value: 'RB', label: 'Renewal Business' },
];

type Patch<K extends keyof PolicyData> = (patch: Partial<PolicyData[K]>) => void;

function PolicySection({
  data,
  setData,
  terms,
}: Readonly<{
  data: PolicyData;
  setData: (f: (d: PolicyData) => PolicyData) => void;
  terms: Patch<'terms'>;
}>) {
  return (
    <div className="form-grid">
      <Field label="Segment" required>
        {(id) => (
          <LovSelect
            id={id}
            type={SBM_LOV.segment}
            value={data.segment}
            required
            onChange={(segment) => setData((d) => ({ ...d, segment }))}
          />
        )}
      </Field>
      <SelectInput
        label="Business type"
        required
        value={data.businessType}
        options={BUSINESS_TYPES}
        onChange={(v) => setData((d) => ({ ...d, businessType: v === 'RB' ? 'RB' : 'NB' }))}
      />
      <Field label="Insurer" required>
        {(id) => (
          <InsurerSelect
            id={id}
            value={data.terms.insurerCode ?? ''}
            onChange={(insurerCode) => terms({ insurerCode: textOrNull(insurerCode) })}
          />
        )}
      </Field>
      <TextInput
        label="Policy No."
        value={shown(data.terms.policyNo)}
        onChange={(v) => terms({ policyNo: textOrNull(v) })}
      />
      <TextInput
        label="Inception"
        type="date"
        required
        value={shown(data.terms.inceptionDate)}
        onChange={(v) => terms({ inceptionDate: textOrNull(v) })}
      />
      <TextInput
        label="Expiry"
        type="date"
        required
        value={data.terms.expiryDate}
        onChange={(v) => terms({ expiryDate: v })}
      />
      <NumberInput
        label="Amount insured"
        required
        value={shown(data.terms.sumInsured)}
        onChange={(v) => terms({ sumInsured: v ?? null })}
      />
      <NumberInput
        label="Total premium"
        required
        value={shown(data.terms.totalPremium)}
        onChange={(v) => terms({ totalPremium: v ?? null })}
      />
    </div>
  );
}

function AssuredSection({
  data,
  assured,
  loan,
}: Readonly<{ data: PolicyData; assured: Patch<'assured'>; loan: Patch<'loan'> }>) {
  return (
    <div className="form-grid">
      <TextInput
        label="Assured"
        required
        value={data.assured.assuredName}
        onChange={(assuredName) => assured({ assuredName })}
      />
      <TextInput
        label="Mailing address"
        value={shown(data.assured.mailingAddress)}
        onChange={(v) => assured({ mailingAddress: textOrNull(v) })}
      />
      <TextInput
        label="E-mail"
        value={shown(data.assured.email)}
        onChange={(v) => assured({ email: textOrNull(v) })}
      />
      <TextInput
        label="Mobile"
        value={shown(data.assured.mobile)}
        onChange={(v) => assured({ mobile: textOrNull(v) })}
      />
      <TextInput
        label="Borrower"
        value={shown(data.loan.borrowerName)}
        onChange={(v) => loan({ borrowerName: textOrNull(v) })}
      />
      <TextInput
        label="PN No."
        hint="Required for the CBG segments"
        value={shown(data.loan.pnNo)}
        onChange={(v) => loan({ pnNo: textOrNull(v) })}
      />
      <TextInput
        label="Loan application No."
        value={shown(data.loan.loanApplicationNo)}
        onChange={(v) => loan({ loanApplicationNo: textOrNull(v) })}
      />
      <TextInput
        label="CIF"
        value={shown(data.loan.cif)}
        onChange={(v) => loan({ cif: textOrNull(v) })}
      />
    </div>
  );
}

function RiskSection({
  data,
  risk,
  marks,
}: Readonly<{ data: PolicyData; risk: Patch<'risk'>; marks: Patch<'marks'> }>) {
  return (
    <div className="form-grid">
      <TextInput
        label="Unit"
        hint="Unit and serial number for Motor"
        value={shown(data.risk.unitDescription)}
        onChange={(v) => risk({ unitDescription: textOrNull(v) })}
      />
      <TextInput
        label="Serial No."
        value={shown(data.risk.serialNo)}
        onChange={(v) => risk({ serialNo: textOrNull(v) })}
      />
      <TextInput
        label="Plate No."
        value={shown(data.risk.plateNo)}
        onChange={(v) => risk({ plateNo: textOrNull(v) })}
      />
      <TextInput
        label="Location"
        hint="Location of the property for Fire"
        value={shown(data.risk.propertyLocation)}
        onChange={(v) => risk({ propertyLocation: textOrNull(v) })}
      />
      <label className="checkbox" style={{ alignSelf: 'end' }}>
        <input
          type="checkbox"
          checked={data.marks.ffy}
          onChange={(e) => marks({ ffy: e.target.checked })}
        />
        FFY
      </label>
      <label className="checkbox" style={{ alignSelf: 'end' }}>
        <input
          type="checkbox"
          checked={data.marks.employeeAccount}
          onChange={(e) => marks({ employeeAccount: e.target.checked })}
        />
        Employee account
      </label>
    </div>
  );
}

/**
 * New Policy on the Masterlist and Edit on the policy record (FR-SP-003): the segment, business
 * type, loan, assured, terms, risk and marks of a policy entered by hand. The record is saved
 * VALIDATED with the checks of the upload (mandatory fields of the MANUAL source, a policy already
 * in the masterlist refused) and every changed field is kept in its history.
 */
export function PolicyFormDialog({
  detail,
  onClose,
}: Readonly<{ detail?: PolicyDetail; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currency = useBaseCurrency();
  const [data, setData] = useState<PolicyData>(() => detail?.data ?? emptyPolicy(currency));
  const loan = (patch: Partial<PolicyData['loan']>) =>
    setData((d) => ({ ...d, loan: { ...d.loan, ...patch } }));
  const assured = (patch: Partial<PolicyData['assured']>) =>
    setData((d) => ({ ...d, assured: { ...d.assured, ...patch } }));
  const terms = (patch: Partial<PolicyData['terms']>) =>
    setData((d) => ({ ...d, terms: { ...d.terms, ...patch } }));
  const risk = (patch: Partial<PolicyData['risk']>) =>
    setData((d) => ({ ...d, risk: { ...d.risk, ...patch } }));
  const marks = (patch: Partial<PolicyData['marks']>) =>
    setData((d) => ({ ...d, marks: { ...d.marks, ...patch } }));
  const save = useMutation({
    mutationFn: () =>
      detail === undefined
        ? submittedApi.create(companyId, { ...data, tracking: null })
        : submittedApi.update(detail.row.id, { ...data, tracking: detail.tracking }),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['submitted'] });
      toast.success(`${p.row.sbmNo} saved`);
      onClose();
      if (detail === undefined) {
        void navigate(`/submitted/policies/${String(p.row.id)}`);
      }
    },
  });
  return (
    <Modal
      title={detail === undefined ? 'New Policy' : `Edit Policy ${detail.row.sbmNo}`}
      open
      size="lg"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <h3>Policy</h3>
      <PolicySection data={data} setData={setData} terms={terms} />
      <h3>Assured and loan</h3>
      <AssuredSection data={data} assured={assured} loan={loan} />
      <h3>Risk</h3>
      <RiskSection data={data} risk={risk} marks={marks} />
    </Modal>
  );
}
