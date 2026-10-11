import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { formatMonth, parseMonthText } from '@/utils/dateText';
import { MonthInput } from './MonthInput';

function Harness() {
  const [month, setMonth] = useState('2026-09');
  return (
    <>
      <label htmlFor="m">Production Month</label>
      <MonthInput id="m" value={month} onChange={(e) => setMonth(e.target.value)} />
      <output>{month}</output>
    </>
  );
}

describe('MonthInput', () => {
  it('shows and reads months as MMM-yyyy', () => {
    expect(formatMonth('2026-09')).toBe('Sep-2026');
    expect(parseMonthText('Oct-2026')).toBe('2026-10');
    expect(parseMonthText('09/2026')).toBe('2026-09');
    expect(parseMonthText('2026-9')).toBe('2026-09');
    expect(parseMonthText('Octo')).toBeUndefined();
    render(<Harness />);
    const input = screen.getByLabelText('Production Month');
    expect((input as HTMLInputElement).value).toBe('Sep-2026');
    fireEvent.change(input, { target: { value: 'Nov-2026' } });
    expect(screen.getByText('2026-11')).toBeTruthy();
  });
});
