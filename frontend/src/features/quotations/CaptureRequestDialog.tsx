import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { catalogApi } from '@/api/catalog';
import { quotationRequestsApi, REQUEST_ENTITY } from '@/api/quotations';
import type { QuotationRequest } from '@/api/quotations';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { requestErrors } from './requestForm';
import type { RequestForm } from './requestForm';

const EMPTY: RequestForm = {
  channel: 'EMAIL',
  externalRef: '',
  clientCode: '',
  prospectName: '',
  prospectEmail: '',
  prospectMobile: '',
  productCode: '',
  marketSegment: '',
  requestedCover: '',
};

const blank = (v: string) => (v.trim() === '' ? undefined : v.trim());

/**
 * Captures a quotation request received by e-mail (BRNB.041): an existing client or the
 * prospect's details, the requested product and cover, and the e-mail itself attached as a
 * REQUEST_EMAIL document.
 */
export function CaptureRequestDialog({
  onClose,
  onSaved,
}: Readonly<{ onClose: () => void; onSaved: (r: QuotationRequest) => void }>) {
  const companyId = useCompanyId();
  const [form, setForm] = useState(EMPTY);
  const [clientId, setClientId] = useState<number>();
  const [file, setFile] = useState<File>();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<RequestForm>) => setForm((f) => ({ ...f, ...patch }));
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  const save = useMutation({
    mutationFn: async () => {
      const saved = await quotationRequestsApi.create({
        companyId,
        channel: form.channel,
        externalRef: blank(form.externalRef),
        clientCode: blank(form.clientCode),
        prospectName: blank(form.prospectName),
        prospectEmail: blank(form.prospectEmail),
        prospectMobile: blank(form.prospectMobile),
        productCode: blank(form.productCode),
        marketSegment: blank(form.marketSegment),
        requestedCover: form.requestedCover.trim(),
      });
      if (file !== undefined) {
        await attachmentsApi.uploadMany(REQUEST_ENTITY, String(saved.id), [file], {
          documentType: 'REQUEST_EMAIL',
        });
      }
      return saved;
    },
    onSuccess: onSaved,
  });
  const submit = () => {
    const found = requestErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  return (
    <Modal
      open
      title="Capture Quotation Request"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={submit}>
            Save Request
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <Field label="Channel" required>
            {(id) => (
              <LovSelect
                id={id}
                type="SOURCE_CHANNEL"
                value={form.channel}
                onChange={(channel) => set({ channel })}
              />
            )}
          </Field>
          <TextInput
            label="Source reference"
            hint="E-mail subject or reference in the source system"
            value={form.externalRef}
            onChange={(externalRef) => set({ externalRef })}
          />
          <Field
            label="Existing client"
            error={errors.client}
            hint="Or give the prospect's details below"
          >
            {(id) => (
              <ClientPicker
                id={id}
                value={clientId}
                onChange={(c) => {
                  setClientId(c?.id);
                  set({ clientCode: c?.code ?? '' });
                }}
              />
            )}
          </Field>
          <TextInput
            label="Prospect name"
            hint="'Last, First' for a person"
            disabled={clientId !== undefined}
            value={form.prospectName}
            onChange={(prospectName) => set({ prospectName })}
          />
          <TextInput
            label="Prospect e-mail"
            error={errors.prospectEmail}
            disabled={clientId !== undefined}
            value={form.prospectEmail}
            onChange={(prospectEmail) => set({ prospectEmail })}
          />
          <TextInput
            label="Prospect mobile"
            disabled={clientId !== undefined}
            value={form.prospectMobile}
            onChange={(prospectMobile) => set({ prospectMobile })}
          />
          <SelectInput
            label="Requested product"
            blank="Not specified"
            value={form.productCode}
            options={(products.data ?? []).map((p) => ({
              value: p.code,
              label: `${p.code} – ${p.name}`,
            }))}
            onChange={(productCode) => set({ productCode })}
          />
          <Field label="Market segment">
            {(id) => (
              <LovSelect
                id={id}
                type="MARKET_SEGMENT"
                value={form.marketSegment}
                onChange={(marketSegment) => set({ marketSegment })}
              />
            )}
          </Field>
        </div>
        <Field label="Requested cover" required error={errors.requestedCover}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={2000}
              value={form.requestedCover}
              onChange={(e) => set({ requestedCover: e.target.value })}
            />
          )}
        </Field>
        <Field label="Request e-mail" hint="Attach the e-mail received (.eml, .msg or PDF).">
          {(id) => (
            <input
              id={id}
              className="input"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
