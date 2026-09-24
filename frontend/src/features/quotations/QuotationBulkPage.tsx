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
 * Bulk quotations (BRNB.024/028/042/063): one product per file; each valid row becomes a draft
 * quotation with its number and ARN (a prospect is created for an unknown client).
 */
export default function QuotationBulkPage() {
  const queryClient = useQueryClient();
  const [product, setProduct] = useState('');
  const [segment, setSegment] = useState('');
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  const parameters: Record<string, string> = { product };
  if (segment !== '') {
    parameters.segment = segment;
  }
  return (
    <div className="stack">
      <PageHeader
        backTo="/quotations"
        section="Quotation / Proposal"
        title="Bulk Quotations"
        description="Choose the product, download the template, fill one row per quotation and upload it."
        actions={<Link to="/bulk/QUOTATION_ACCEPTANCE">Bulk acceptance</Link>}
      />
      <BulkUploadWizard
        handler="QUOTATION_CREATE"
        parameters={parameters}
        parametersReady={product !== ''}
        onCommitted={() => void queryClient.invalidateQueries({ queryKey: ['quotations'] })}
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
          </div>
        }
      />
    </div>
  );
}
