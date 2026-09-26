import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { parseDateText } from '@/utils/dateText';
import { DateInput } from './DateInput';

function Form({ onDate }: Readonly<{ onDate: (iso: string) => void }>) {
  const [value, setValue] = useState('2026-09-23');
  return (
    <>
      <label htmlFor="d">Payment Date</label>
      <DateInput
        id="d"
        value={value}
        onChange={(e) => {
          setValue(e.target.value);
          onDate(e.target.value);
        }}
      />
    </>
  );
}

describe('DateInput', () => {
  it('shows the ISO value as dd-MMM-yyyy', () => {
    render(<Form onDate={vi.fn()} />);
    expect(screen.getByLabelText('Payment Date')).toHaveValue('23-Sep-2026');
  });

  it('accepts a typed dd-MMM-yyyy date and reports it as ISO', async () => {
    const onDate = vi.fn();
    render(<Form onDate={onDate} />);
    const input = screen.getByLabelText('Payment Date');
    await userEvent.clear(input);
    await userEvent.type(input, '05-jan-2027');
    expect(onDate).toHaveBeenLastCalledWith('2027-01-05');
    fireEvent.blur(input);
    expect(input).toHaveValue('05-Jan-2027');
  });

  it('accepts ISO and dd/mm/yyyy, and clears', () => {
    const onDate = vi.fn();
    render(<Form onDate={onDate} />);
    const input = screen.getByLabelText('Payment Date');
    fireEvent.change(input, { target: { value: '2026-12-31' } });
    expect(onDate).toHaveBeenLastCalledWith('2026-12-31');
    fireEvent.change(input, { target: { value: '01/02/2026' } });
    expect(onDate).toHaveBeenLastCalledWith('2026-02-01');
    fireEvent.change(input, { target: { value: '' } });
    expect(onDate).toHaveBeenLastCalledWith('');
  });

  it('parses only complete, valid dates', () => {
    expect(parseDateText('31-Feb-2026')).toBeUndefined();
    expect(parseDateText('23-Sep')).toBeUndefined();
    expect(parseDateText('23 sep 2026')).toBe('2026-09-23');
  });

  it('has a calendar button', () => {
    render(<Form onDate={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Choose date' })).toBeInTheDocument();
  });
});
