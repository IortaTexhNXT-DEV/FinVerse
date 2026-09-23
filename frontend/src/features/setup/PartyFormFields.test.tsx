import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AgeingStrip, StatementKpis } from '@/features/gl/PartyStatementParts';
import { PartyFormFields } from './PartyFormFields';
import { emptyParty } from './partyForm';

describe('party screens', () => {
  it('shows intermediary fields for a broker and upper-cases the code', async () => {
    const onChange = vi.fn();
    render(
      <PartyFormFields
        form={{ ...emptyParty(1), partyType: 'BROKER' }}
        errors={{ code: 'Code is required' }}
        onChange={onChange}
      />,
    );
    expect(screen.getByLabelText('Commission rate %')).toBeInTheDocument();
    expect(screen.getByLabelText('Licence no.')).toBeInTheDocument();
    expect(screen.getByText('Code is required')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/Party code/), 'b');
    expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ code: 'B' }));
  });

  it('hides intermediary fields for a policyholder', () => {
    render(<PartyFormFields form={emptyParty(1)} errors={{}} onChange={vi.fn()} />);
    expect(screen.queryByLabelText('Commission rate %')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Withholding tax rate %')).not.toBeInTheDocument();
  });

  it('renders statement figures and the party ageing', () => {
    render(
      <>
        <StatementKpis
          summary={{ receivable: 100, payable: 250, net: -150, overdue: 0, openItems: 2 }}
          partyName="Juan"
        />
        <AgeingStrip
          partyCode="C-1"
          ageing={{
            asOf: '2026-09-30',
            buckets: ['Not due', '1-30'],
            rows: [{ partyCode: 'C-1', amounts: [100, 50], total: 150 }],
          }}
        />
      </>,
    );
    expect(screen.getByText('Company owes the party')).toBeInTheDocument();
    expect(screen.getByText('150.00')).toBeInTheDocument();
  });

  it('renders no ageing when the party has no open item', () => {
    const { container } = render(<AgeingStrip partyCode="X" ageing={undefined} />);
    expect(container).toBeEmptyDOMElement();
  });
});
