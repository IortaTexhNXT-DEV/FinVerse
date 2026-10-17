import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import type { IncentiveRuleParameter } from '@/api/productCatalog';
import { IncentiveParamsTable } from './IncentiveParamsTable';
import type { ParamRow } from './incentiveForm';

const DEFINITIONS: IncentiveRuleParameter[] = [
  { key: 'minimumPremium', label: 'Minimum Gross Premium', valueType: 'AMOUNT' },
];

function Harness({ typeMissing = false }: Readonly<{ typeMissing?: boolean }>) {
  const [rows, setRows] = useState<ParamRow[]>([]);
  return (
    <>
      <IncentiveParamsTable
        rows={rows}
        definitions={DEFINITIONS}
        errors={{ 'params.0': 'Minimum Gross Premium must be a number' }}
        typeMissing={typeMissing}
        onChange={setRows}
      />
      <output>{JSON.stringify(rows)}</output>
    </>
  );
}

describe('Incentive rule parameters table', () => {
  it('adds a parameter of the type, takes its value and removes it', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    expect(screen.queryByText(/JSON/)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Add Parameter' }));
    expect(screen.getByLabelText('Parameter 1')).toHaveValue('minimumPremium');
    expect(screen.getByRole('button', { name: 'Add Parameter' })).toBeDisabled();
    await user.type(screen.getByLabelText('Value of Minimum Gross Premium'), '5000');
    expect(screen.getByRole('status')).toHaveTextContent(
      '[{"key":"minimumPremium","value":"5000"}]',
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Minimum Gross Premium must be a number');
    await user.selectOptions(screen.getByLabelText('Parameter 1'), '');
    expect(screen.getByLabelText('Value of parameter 1')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove parameter' }));
    // The grid keeps its headers and says that it has no parameter.
    expect(screen.queryByLabelText('Parameter 1')).not.toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Parameter' })).toBeInTheDocument();
    expect(screen.getByText(/No parameter yet/)).toBeInTheDocument();
  });

  it('asks for the incentive type first', () => {
    render(<Harness typeMissing />);
    expect(screen.getByRole('button', { name: 'Add Parameter' })).toBeDisabled();
    expect(screen.getByText('Select the incentive type first')).toBeInTheDocument();
  });
});
