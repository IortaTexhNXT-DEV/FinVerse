import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { insurerPageApi } from '@/api/insurerPage';
import type {
  BankAccountInput,
  ContactInput,
  InsurerBankAccount,
  InsurerContact,
  RemittanceTerms,
} from '@/api/insurerPage';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useBaseCurrency } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import {
  ACCOUNT_PURPOSES,
  CONTACT_ROLES,
  REMITTANCE_CYCLES,
  REMITTANCE_MODES,
  bankAccountInputOf,
  contactInputOf,
  remittanceInputOf,
} from './insurerPageForm';

function useSaved(onClose: () => void, message: string) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: ['catalog'] });
    await queryClient.invalidateQueries({ queryKey: ['change-history'] });
    toast.success(message);
    onClose();
  };
}

function SaveFooter({ busy, onSave }: Readonly<{ busy: boolean; onSave: () => void }>) {
  return (
    <Button variant="accent" busy={busy} onClick={onSave}>
      Save for Authorization
    </Button>
  );
}

/** Changes the payment terms and remittance details of an insurer, pending authorization. */
export function RemittanceModal({
  insurerId,
  terms,
  onClose,
}: Readonly<{ insurerId: number; terms: RemittanceTerms | undefined; onClose: () => void }>) {
  const [form, setForm] = useState<RemittanceTerms>(terms ?? {});
  const saved = useSaved(onClose, 'Remittance details saved – pending authorization');
  const save = useMutation({
    mutationFn: () => insurerPageApi.updateRemittance(insurerId, remittanceInputOf(form)),
    onSuccess: saved,
  });
  const set = (patch: Partial<RemittanceTerms>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title="Payment Terms & Remittance"
      open
      onClose={onClose}
      footer={<SaveFooter busy={save.isPending} onSave={() => save.mutate()} />}
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Payment terms"
          hint="As agreed with the insurer"
          value={form.paymentTerms ?? undefined}
          onChange={(paymentTerms) => set({ paymentTerms })}
        />
        <SelectInput
          label="Remittance cycle"
          blank="Not set"
          value={form.cycle ?? undefined}
          options={REMITTANCE_CYCLES}
          onChange={(cycle) =>
            set({ cycle: cycle === '' ? null : (cycle as RemittanceTerms['cycle']) })
          }
        />
        <SelectInput
          label="Remittance mode"
          blank="Not set"
          value={form.mode ?? undefined}
          options={REMITTANCE_MODES}
          onChange={(mode) => set({ mode: mode === '' ? null : (mode as RemittanceTerms['mode']) })}
        />
        <TextInput
          label="Remittance advice e-mail"
          value={form.email ?? undefined}
          onChange={(email) => set({ email })}
        />
        <TextInput
          label="Remittance currency"
          upper
          value={form.currency ?? undefined}
          onChange={(currency) => set({ currency })}
        />
      </div>
    </Modal>
  );
}

/** Adds or changes a contact person of an insurer, pending authorization. */
export function ContactModal({
  insurerId,
  contact,
  onClose,
}: Readonly<{ insurerId: number; contact?: InsurerContact; onClose: () => void }>) {
  const [form, setForm] = useState<Partial<ContactInput>>(contact ?? { role: 'PLACEMENT' });
  const saved = useSaved(onClose, 'Contact saved – pending authorization');
  const save = useMutation({
    mutationFn: () =>
      contact
        ? insurerPageApi.updateContact(contact.id, contactInputOf(form))
        : insurerPageApi.createContact(insurerId, contactInputOf(form)),
    onSuccess: saved,
  });
  const set = (patch: Partial<ContactInput>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title={contact ? 'Edit Contact' : 'New Contact'}
      open
      onClose={onClose}
      footer={<SaveFooter busy={save.isPending} onSave={() => save.mutate()} />}
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Handles"
          required
          value={form.role}
          options={CONTACT_ROLES}
          onChange={(role) => set({ role: role as ContactInput['role'] })}
        />
        <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
        <TextInput
          label="Position"
          value={form.position ?? undefined}
          onChange={(position) => set({ position })}
        />
        <TextInput
          label="E-mail"
          value={form.email ?? undefined}
          onChange={(email) => set({ email })}
        />
        <TextInput
          label="Phone"
          value={form.phone ?? undefined}
          onChange={(phone) => set({ phone })}
        />
      </div>
    </Modal>
  );
}

/** Adds or changes a bank account of an insurer, pending authorization. */
export function BankAccountModal({
  insurerId,
  account,
  onClose,
}: Readonly<{ insurerId: number; account?: InsurerBankAccount; onClose: () => void }>) {
  const baseCurrency = useBaseCurrency();
  const [form, setForm] = useState<Partial<BankAccountInput>>(
    account ?? { purpose: 'REMITTANCE', currency: baseCurrency, primaryAccount: false },
  );
  const saved = useSaved(onClose, 'Bank account saved – pending authorization');
  const save = useMutation({
    mutationFn: () =>
      account
        ? insurerPageApi.updateBankAccount(account.id, bankAccountInputOf(form))
        : insurerPageApi.createBankAccount(insurerId, bankAccountInputOf(form)),
    onSuccess: saved,
  });
  const set = (patch: Partial<BankAccountInput>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title={account ? 'Edit Bank Account' : 'New Bank Account'}
      open
      onClose={onClose}
      footer={<SaveFooter busy={save.isPending} onSave={() => save.mutate()} />}
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Purpose"
          required
          value={form.purpose}
          options={ACCOUNT_PURPOSES}
          onChange={(purpose) => set({ purpose: purpose as BankAccountInput['purpose'] })}
        />
        <TextInput
          label="Bank"
          required
          value={form.bankName}
          onChange={(bankName) => set({ bankName })}
        />
        <TextInput
          label="Bank branch"
          value={form.bankBranch ?? undefined}
          onChange={(bankBranch) => set({ bankBranch })}
        />
        <TextInput
          label="Account number"
          required
          value={form.accountNo}
          onChange={(accountNo) => set({ accountNo })}
        />
        <TextInput
          label="Account name"
          required
          value={form.accountName}
          onChange={(accountName) => set({ accountName })}
        />
        <TextInput
          label="Currency"
          required
          upper
          value={form.currency}
          onChange={(currency) => set({ currency })}
        />
        <label className="checkbox" style={{ alignSelf: 'end' }}>
          <input
            type="checkbox"
            checked={form.primaryAccount ?? false}
            onChange={(e) => set({ primaryAccount: e.target.checked })}
          />
          Primary account for its purpose
        </label>
      </div>
    </Modal>
  );
}
