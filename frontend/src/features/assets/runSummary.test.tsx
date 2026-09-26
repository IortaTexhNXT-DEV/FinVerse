import { fireEvent, render, screen } from '@testing-library/react';
import { NumberInput, SelectInput, TextInput } from './FormControls';
import { codeOptions, enumOptions } from './options';
import { check, summarize } from './runSummary';

describe('run summary', () => {
  it('summarizes a loading, a draft and a posted preview', () => {
    expect(summarize(undefined)).toMatchObject({ count: 0, postable: false, status: 'DRAFT' });
    const draft = summarize({ posted: false, total: 1900, lines: [{ id: 1 }, { id: 2 }] });
    expect(draft).toMatchObject({ count: 2, total: 1900, postable: true, postedBy: undefined });
    const posted = summarize({
      posted: true,
      total: 10,
      lines: [{ id: 1 }],
      run: { createdBy: 'fmanager' },
    });
    expect(posted).toMatchObject({ status: 'POSTED', postable: false, postedBy: 'fmanager' });
  });

  it('keeps the first failing rule per field', () => {
    const errors = check({ a: 0 }, [
      ['a', (f) => f.a === 0, 'first'],
      ['a', () => true, 'second'],
      ['b', () => false, 'never'],
    ]);
    expect(errors).toEqual({ a: 'first' });
  });

  it('builds select options', () => {
    expect(codeOptions([{ code: 'FIN', name: 'Finance' }])).toEqual([
      { value: 'FIN', label: 'FIN – Finance' },
    ]);
    expect(enumOptions(['STRAIGHT_LINE'])).toEqual([
      { value: 'STRAIGHT_LINE', label: 'Straight Line' },
    ]);
  });
});

describe('form controls', () => {
  it('reports typed values through labelled inputs', () => {
    const onText = vi.fn();
    const onNumber = vi.fn();
    const onSelect = vi.fn();
    render(
      <>
        <TextInput label="Tag" upper value="" onChange={onText} />
        <NumberInput label="Cost" value={undefined} onChange={onNumber} error="Required" />
        <SelectInput
          label="Method"
          blank="Select"
          value=""
          options={enumOptions(['STRAIGHT_LINE'])}
          onChange={onSelect}
        />
      </>,
    );
    fireEvent.change(screen.getByLabelText('Tag'), { target: { value: 'fa-1' } });
    fireEvent.change(screen.getByLabelText('Cost'), { target: { value: '12.5' } });
    fireEvent.change(screen.getByLabelText('Cost'), { target: { value: '' } });
    fireEvent.change(screen.getByLabelText('Method'), { target: { value: 'STRAIGHT_LINE' } });
    expect(onText).toHaveBeenCalledWith('FA-1');
    expect(onNumber).toHaveBeenCalledWith(12.5);
    expect(onSelect).toHaveBeenCalledWith('STRAIGHT_LINE');
    expect(screen.getByRole('alert')).toHaveTextContent('Required');
  });
});
