import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { LovSelect } from '@/components/broking/LovSelect';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { SelectInput } from '@/features/assets/FormControls';

/**
 * Bulk account creation (BRNB.024/025): one product per file; each valid row becomes a draft
 * account (a prospect is created for an unknown client), optionally submitted at once.
 */
export default function AccountBulkPage() {
  const queryClient = useQueryClient();
  const [product, setProduct] = useState('');
  const [segment, setSegment] = useState('');
  const [submit, setSubmit] = useState(false);
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  const parameters: Record<string, string> = { product, submit: submit ? 'Y' : 'N' };
  if (segment !== '') {
    parameters.segment = segment;
  }
  return (
    <div className="stack">
      <PageHeader
        section="Accounts & Placement"
        title="Bulk Account Creation"
        description="Choose the product, download the template, fill one row per account and upload it."
        actions={<Link to="/bulk">All upload types</Link>}
      />
      <BulkUploadWizard
        handler="ACCOUNT_CREATE"
        parameters={parameters}
        parametersReady={product !== ''}
        onCommitted={() => void queryClient.invalidateQueries({ queryKey: ['accounts'] })}
        parameterFields={
          <div className="form-grid">
            <SelectInput
              label="Product"
              required
              blank="Select"
              value={product}
              options={(products.data ?? []).map((p) => ({
                value: p.code,
                label: `${p.code} – ${p.name}`,
              }))}
              onChange={setProduct}
            />
            <Field label="Default market segment">
              {(id) => (
                <LovSelect id={id} type="MARKET_SEGMENT" value={segment} onChange={setSegment} />
              )}
            </Field>
            <label className="checkbox" style={{ alignSelf: 'end' }}>
              <input
                type="checkbox"
                checked={submit}
                onChange={(e) => setSubmit(e.target.checked)}
              />
              Submit complete accounts to Processing
            </label>
          </div>
        }
      />
    </div>
  );
}
