import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { organizationApi } from '@/api/organization';
import type { ClientProfile, Company, TaxRegistration } from '@/api/types';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { awaitsOtherChecker } from '@/utils/makerChecker';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { profileRequest, taxRegistrationProblem } from './setupForms';
import { DateInput } from '@/components/ui/DateInput';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const PROFILE_FIELDS: { key: keyof ClientProfile; label: string; hint: string }[] = [
  { key: 'shortName', label: 'Short name', hint: 'Used in texts and labels; blank = company code' },
  { key: 'groupName', label: 'Group name', hint: 'Names group concepts, e.g. the group bank' },
  { key: 'logoRef', label: 'Document logo', hint: 'theme:logo = logo of the theme pack' },
  { key: 'headOfficeCode', label: 'Head office code', hint: 'Head office in files and reports' },
  { key: 'defaultBankCode', label: 'Default bank account', hint: 'Bank account code proposed' },
];

/** Legal entities keeping their own books (base currency, fiscal year, posting windows). */
export default function CompaniesPage() {
  const { companies } = useWorkspace();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Company | null>(null);
  const [profile, setProfile] = useState<ClientProfile>({});
  const [tax, setTax] = useState<TaxRegistration>({ vatRegistered: true });
  const taxProblem = taxRegistrationProblem(tax);
  const authorize = useMutation({
    mutationFn: (id: number) => organizationApi.authorizeCompany(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['companies'] }),
  });
  const save = useMutation({
    mutationFn: (c: Company) =>
      organizationApi.updateCompany(c.id, profileRequest(c, profile, tax)),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['companies'] });
      setEditing(null);
      toast.success(
        `Client profile and tax registration of ${c.code} saved – pending authorization`,
      );
    },
  });
  const open = (c: Company) => {
    setProfile({
      shortName: c.shortName ?? '',
      groupName: c.groupName ?? '',
      logoRef: c.logoRef ?? '',
      headOfficeCode: c.headOfficeCode ?? '',
      defaultBankCode: c.defaultBankCode ?? '',
    });
    setTax(c.taxRegistration ?? { vatRegistered: true });
    setEditing(c);
  };

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Companies"
        description="Each company keeps its own books. Select a company to maintain its client profile."
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<Company>
          rows={companies}
          rowKey={(c) => c.id}
          onRowClick={can('MASTER_MAINTAIN') ? open : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
            { key: 'n', header: 'Legal Name', render: (c) => c.name },
            { key: 'sn', header: 'Short Name', render: (c) => c.shortName ?? '' },
            { key: 't', header: 'TIN', render: (c) => c.taxId ?? '' },
            { key: 'rdo', header: 'RDO', render: (c) => c.taxRegistration?.rdoCode ?? '' },
            { key: 'b', header: 'Base Currency', render: (c) => c.baseCurrency },
            {
              key: 'f',
              header: 'Fiscal Year Starts',
              render: (c) => MONTHS[c.fiscalYearStartMonth - 1] ?? '',
            },
            {
              key: 'w',
              header: 'Back / Forward Value Days',
              render: (c) => `${c.backValueDays} / ${c.forwardValueDays}`,
            },
            {
              key: 'r',
              header: 'Retained Earnings A/C',
              render: (c) => c.retainedEarningsAccount ?? '',
            },
            { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (c) =>
                awaitsOtherChecker(c, user?.username) &&
                can('MASTER_AUTHORIZE') && (
                  <ConfirmButton
                    size="sm"
                    variant="secondary"
                    confirm={{
                      title: `Authorize Company ${c.code}`,
                      record: c.name,
                      effect: 'The company becomes active.',
                    }}
                    onConfirm={() => authorize.mutateAsync(c.id)}
                  >
                    Authorize
                  </ConfirmButton>
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title={`Client profile and tax registration – ${editing?.code ?? ''}`}
        open={editing !== null}
        onClose={() => setEditing(null)}
        footer={
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={taxProblem !== null}
            onClick={() => editing && save.mutate(editing)}
          >
            Save for Authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        <p className="muted">
          The identity of the company on documents, messages and screens. The legal name, address
          and TIN are those of the company.
        </p>
        <div className="form-grid">
          {PROFILE_FIELDS.map((f) => (
            <Field key={f.key} label={f.label} hint={f.hint}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={profile[f.key] ?? ''}
                  onChange={(e) => setProfile({ ...profile, [f.key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </div>
        <h3>Tax registration</h3>
        {taxProblem && <p className="field-error">{taxProblem}</p>}
        <div className="form-grid">
          <Field label="RDO code" hint="Revenue District Office, e.g. 050">
            {(id) => (
              <input
                id={id}
                className="input"
                value={tax.rdoCode ?? ''}
                onChange={(e) => setTax({ ...tax, rdoCode: e.target.value.toUpperCase() })}
              />
            )}
          </Field>
          <label className="checkbox" style={{ alignSelf: 'end' }}>
            <input
              type="checkbox"
              checked={tax.vatRegistered}
              onChange={(e) => setTax({ ...tax, vatRegistered: e.target.checked })}
            />{' '}
            Registered for VAT
          </label>
          <Field label="CAS permit no." hint="Permit of the computerised accounting system">
            {(id) => (
              <input
                id={id}
                className="input"
                value={tax.casPermitNo ?? ''}
                onChange={(e) => setTax({ ...tax, casPermitNo: e.target.value })}
              />
            )}
          </Field>
          <Field label="CAS permit date">
            {(id) => (
              <DateInput
                id={id}
                value={tax.casPermitDate ?? ''}
                onChange={(e) => setTax({ ...tax, casPermitDate: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="E-invoicing permit no." hint="Permit or enrolment number">
            {(id) => (
              <input
                id={id}
                className="input"
                value={tax.einvoicingPermitNo ?? ''}
                onChange={(e) => setTax({ ...tax, einvoicingPermitNo: e.target.value })}
              />
            )}
          </Field>
          <Field label="E-invoicing permit date">
            {(id) => (
              <DateInput
                id={id}
                value={tax.einvoicingPermitDate ?? ''}
                onChange={(e) =>
                  setTax({ ...tax, einvoicingPermitDate: e.target.value || undefined })
                }
              />
            )}
          </Field>
        </div>
      </Modal>
    </div>
  );
}
