import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mastersApi } from '@/api/masters';
import { reservesApi } from '@/api/reserves';
import type { DevelopmentPeriod, TriangleBasis } from '@/api/reserves';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { codeOptions, enumOptions } from '@/features/assets/options';
import { formatAmount, humanize, today } from '@/utils/format';
import { TriangleGrid } from './TriangleGrid';

const BASES = enumOptions(['PAID', 'INCURRED']);
const PERIODS = enumOptions(['YEAR', 'QUARTER']);

/** IBNR development triangle viewer: paid or incurred, by year or quarter, chain-ladder IBNR. */
export default function TriangleViewerPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [line, setLine] = useState('FIRE');
  const [asOf, setAsOf] = useState(today());
  const [basis, setBasis] = useState('');
  const [period, setPeriod] = useState('');
  const [accidentPeriods, setAccidentPeriods] = useState<number | undefined>();

  const lines = useQuery({
    queryKey: ['dimensions', companyId, 'BUSINESS_LINE'],
    queryFn: () => mastersApi.dimensions(companyId, 'BUSINESS_LINE'),
    enabled: companyId > 0,
  });
  const triangle = useQuery({
    queryKey: ['reserve-triangle', companyId, line, asOf, basis, period, accidentPeriods],
    queryFn: () =>
      reservesApi.triangles({
        companyId,
        businessLine: line,
        asOf,
        basis: (basis || undefined) as TriangleBasis | undefined,
        period: (period || undefined) as DevelopmentPeriod | undefined,
        accidentPeriods,
      }),
    enabled: companyId > 0 && line !== '' && asOf !== '',
  });
  const data = triangle.data;

  return (
    <div className="stack">
      <PageHeader
        section="Actuarial Reserves"
        title="IBNR Triangles"
        description="Claims development by accident period and development age from the posted claim movements; chain-ladder factors (volume weighted), ultimates and IBNR = ultimate − incurred. Blank options use the line's reserve parameters."
        actions={
          <Button
            variant="secondary"
            icon={<FileBarChart2 size={16} />}
            onClick={() => void navigate('/reports/RSV-TRIANGLE')}
          >
            Report & Export
          </Button>
        }
      />
      <Card>
        <div className="form-grid">
          <SelectInput
            label="Line of business"
            required
            value={line}
            options={codeOptions(lines.data ?? [])}
            onChange={setLine}
          />
          <TextInput label="Valuation date" type="date" value={asOf} onChange={setAsOf} />
          <SelectInput
            label="Triangle"
            blank="As parameterised"
            value={basis}
            options={BASES}
            onChange={setBasis}
          />
          <SelectInput
            label="Development period"
            blank="As parameterised"
            value={period}
            options={PERIODS}
            onChange={setPeriod}
          />
          <NumberInput
            label="Accident periods"
            step="1"
            value={accidentPeriods}
            onChange={setAccidentPeriods}
            hint="2 to 20; blank = parameter"
          />
        </div>
      </Card>
      <ErrorAlert error={triangle.error ?? lines.error} />
      {data && (
        <>
          <div className="grid-4">
            <Kpi label="IBNR of the line" value={formatAmount(data.ibnr)} accent />
            <Kpi label="Triangle" value={humanize(data.basis)} />
            <Kpi label="Development period" value={humanize(data.period)} />
            <Kpi label="Accident periods" value={data.rows.length} />
          </div>
          <Card title={`${data.businessLine} cumulative ${data.basis.toLowerCase()} claims`} flush>
            <TriangleGrid data={data} />
          </Card>
        </>
      )}
    </div>
  );
}
