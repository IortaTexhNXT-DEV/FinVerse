import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SelectField, TextField } from './MasterControls';
import { PeriodPicker } from './PeriodPicker';

describe('tax period picker', () => {
  it('switches between quarters and months', async () => {
    const onChange = vi.fn();
    render(
      <PeriodPicker value={{ year: 2026, granularity: 'QUARTER', index: 2 }} onChange={onChange} />,
    );
    expect(screen.getByLabelText('Quarter')).toHaveValue('2');
    await userEvent.selectOptions(screen.getByLabelText('Period type'), 'MONTH');
    expect(onChange).toHaveBeenCalledWith({ year: 2026, granularity: 'MONTH', index: 1 });
    await userEvent.selectOptions(screen.getByLabelText('Quarter'), '4');
    expect(onChange).toHaveBeenLastCalledWith({ year: 2026, granularity: 'QUARTER', index: 4 });
  });

  it('offers the months when fixed to monthly periods', () => {
    render(
      <PeriodPicker
        value={{ year: 2026, granularity: 'MONTH', index: 9 }}
        onChange={vi.fn()}
        granularities={['MONTH']}
      />,
    );
    expect(screen.queryByLabelText('Period type')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Month')).toHaveValue('9');
    expect(screen.getAllByRole('option')).toHaveLength(12);
  });
});

describe('tax master controls', () => {
  it('edits text and select fields', async () => {
    const onText = vi.fn();
    const onSelect = vi.fn();
    render(
      <>
        <TextField label="Name" value="" onChange={onText} />
        <SelectField
          label="Side"
          value={undefined}
          options={['DEBIT', 'CREDIT'] as const}
          allowEmpty
          onChange={onSelect}
        />
      </>,
    );
    await userEvent.type(screen.getByLabelText('Name'), 'X');
    expect(onText).toHaveBeenCalledWith('X');
    await userEvent.selectOptions(screen.getByLabelText('Side'), 'CREDIT');
    expect(onSelect).toHaveBeenCalledWith('CREDIT');
    await userEvent.selectOptions(screen.getByLabelText('Side'), '');
    expect(onSelect).toHaveBeenLastCalledWith(undefined);
  });
});
