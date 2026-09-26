import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import { coverApi } from '../cover/api';
import { RefsTable } from '../cover/CoverTabs';
import { DialogFooter, InputField } from '../record/FormParts';
import type { NewLocationRef } from './api';
import { locationApi } from './api';
import type { RefErrors } from './locationLogic';
import { noErrors, refErrors } from './locationLogic';

function RefDialog({
  companyId,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  companyId: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (ref: NewLocationRef) => void;
}>) {
  const [arn, setArn] = useState('');
  const [item, setItem] = useState('');
  const [insurer, setInsurer] = useState('');
  const [reference, setReference] = useState('');
  const [from, setFrom] = useState(today());
  const [errors, setErrors] = useState<RefErrors>({});
  const cover = useQuery({
    queryKey: ['broker-claims', 'cover', companyId, arn.trim()],
    queryFn: () => coverApi.cover(companyId, arn.trim()),
    enabled: arn.trim().length >= 10,
    retry: false,
  });
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  const locations = (cover.data?.items ?? []).filter((i) => i.locationKey !== undefined);
  const save = () => {
    const found = refErrors(arn, item, insurer, reference);
    setErrors(found);
    if (noErrors(found)) {
      onSave({
        companyId,
        arn: arn.trim(),
        itemNo: Number(item),
        insurerCode: insurer,
        reference: reference.trim(),
        effectiveFrom: from || undefined,
      });
    }
  };
  return (
    <Modal
      title="New Insurer Location Reference"
      open
      onClose={onClose}
      footer={
        <DialogFooter label="Save Reference" busy={busy} onClose={onClose} onConfirm={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <InputField
          label="Cover (ARN)"
          required
          value={arn}
          error={errors.arn}
          maxLength={30}
          onChange={setArn}
        />
        <Field label="Location" required error={errors.item}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={item}
              onChange={(e) => setItem(e.target.value)}
            >
              <option value="">{cover.isFetching ? 'Loading…' : 'Select…'}</option>
              {locations.map((l) => (
                <option key={l.itemNo} value={l.itemNo}>
                  Item {l.itemNo} - {l.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Insurer" required error={errors.insurer}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={insurer}
              onChange={(e) => setInsurer(e.target.value)}
            >
              <option value="">Select…</option>
              {(insurers.data ?? []).map((i) => (
                <option key={i.partyCode} value={i.partyCode}>
                  {i.name} ({i.partyCode})
                </option>
              ))}
            </select>
          )}
        </Field>
        <InputField
          label="Insurer Location Reference"
          required
          value={reference}
          error={errors.reference}
          onChange={setReference}
        />
        <InputField
          label="Effective From"
          type="date"
          required
          value={from}
          hint="The current reference of the location and insurer ends the day before"
          onChange={setFrom}
        />
      </div>
    </Modal>
  );
}

/**
 * Insurer Location References (BRCLM.042; FR-CM-023): the reference each insurer uses for a
 * location of a cover, with effective dates. A new reference ends the current one; nothing is
 * deleted. References also load by the bulk upload BCL_LOCATION_REF.
 */
export default function LocationRefsPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [adding, setAdding] = useState(false);
  const refs = useQuery({
    queryKey: ['broker-claims', 'location-refs', companyId, q, page],
    queryFn: () => locationApi.refs(companyId, q, page),
  });
  const save = useMutation({
    mutationFn: (ref: NewLocationRef) => locationApi.maintain(ref),
    onSuccess: async (r) => {
      toast.success(`Reference ${r.reference} saved for ${r.arn} item ${r.itemNo}`);
      setAdding(false);
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
    },
  });
  const data = refs.data;
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="Insurer Location References"
        description="The reference each insurer uses for an insured location of a cover, with its effective dates. Both references show on every claim location."
        actions={
          <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
            New Reference
          </Button>
        }
      />
      <Card>
        <div className="stack">
          <WorklistToolbar
            placeholder="Search ARN, insurer or reference"
            onSearch={(text) => {
              setQ(text);
              setPage(0);
            }}
          />
          <ErrorAlert error={refs.error} />
          <RefsTable rows={data?.content ?? []} />
          {data && (
            <Pager
              page={data.page}
              totalPages={data.totalPages}
              total={data.totalElements}
              onPage={setPage}
            />
          )}
        </div>
      </Card>
      {adding && (
        <RefDialog
          companyId={companyId}
          busy={save.isPending}
          error={save.error}
          onClose={() => setAdding(false)}
          onSave={(ref) => save.mutate(ref)}
        />
      )}
    </div>
  );
}
