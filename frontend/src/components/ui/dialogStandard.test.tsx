import { fireEvent, render, screen, within } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { Button } from './Button';
import { Combobox } from './Combobox';
import { filterOptions } from './comboOptions';
import { CommentField } from './CommentField';
import { commentProblem } from './commentRules';
import { ConfirmDialog } from './ConfirmDialog';
import { Modal } from './Modal';
import { cancelLabelFor, consequenceOf, dialogTitle, needsCancel } from './dialogStandard';

describe('dialog standard', () => {
  it('writes titles in Title Case and keeps references, codes and names', () => {
    expect(dialogTitle('Resolve alert ALR-000012')).toBe('Resolve Alert ALR-000012');
    expect(dialogTitle('Return journal to its maker')).toBe('Return Journal to Its Maker');
    expect(dialogTitle('Edit dela Cruz extract_2026.csv')).toBe('Edit Dela Cruz extract_2026.csv');
    expect(dialogTitle('New non-renewable risk code')).toBe('New Non-Renewable Risk Code');
  });

  it('adds Cancel only to a footer without a way back', () => {
    expect(needsCancel(<Button>Save</Button>)).toBe(true);
    expect(
      needsCancel(
        <>
          <Button variant="secondary">Back</Button>
          <Button>Save</Button>
        </>,
      ),
    ).toBe(false);
    expect(needsCancel(<Button onClick={vi.fn()}>Close</Button>)).toBe(false);
    expect(needsCancel(<span>custom</span>)).toBe(false);
    expect(cancelLabelFor('Cancel Voucher')).toBe('Go Back');
    expect(cancelLabelFor('Approve')).toBe('Cancel');
    expect(consequenceOf('The voucher is voided.')).toBe('This cannot be undone.');
    expect(consequenceOf('The record is deleted permanently.')).toBe('');
  });

  it('shows the facts, puts Cancel first, confirms on Enter and closes on Escape', () => {
    const onClose = vi.fn();
    const onSave = vi.fn();
    render(
      <Modal
        title="Change limit"
        open
        onClose={onClose}
        facts={[{ label: 'Reference', value: 'LIM-1' }]}
        helper="The new limit applies from today."
        footer={<Button onClick={onSave}>Save Limit</Button>}
      >
        <input aria-label="Limit" />
      </Modal>,
    );
    const dialog = screen.getByRole('dialog', { name: 'Change Limit' });
    expect(within(dialog).getByText('LIM-1')).toBeTruthy();
    expect(within(dialog).getByText('The new limit applies from today.')).toBeTruthy();
    const buttons = within(dialog)
      .getAllByRole('button')
      .map((b) => b.textContent);
    expect(buttons.slice(-2)).toEqual(['Cancel', 'Save Limit']);
    const field = screen.getByRole('textbox', { name: 'Limit' });
    expect(document.activeElement).toBe(field);
    fireEvent.keyDown(field, { key: 'Enter' });
    expect(onSave).toHaveBeenCalledTimes(1);
    fireEvent.keyDown(field, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes only the top dialog on Escape', () => {
    const outer = vi.fn();
    const inner = vi.fn();
    render(
      <>
        <Modal title="Outer" open onClose={outer}>
          <p>outer</p>
        </Modal>
        <Modal title="Inner" open onClose={inner}>
          <p>inner</p>
        </Modal>
      </>,
    );
    fireEvent.keyDown(document.body, { key: 'Escape' });
    expect(inner).toHaveBeenCalledTimes(1);
    expect(outer).not.toHaveBeenCalled();
  });

  it('confirms a destructive action with its consequence and a counted reason', () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        title="Cancel Voucher DV-1"
        effect="The voucher is voided."
        confirmLabel="Cancel Voucher"
        reason="required"
        reasonMin={10}
        destructive
        onConfirm={onConfirm}
        onClose={vi.fn()}
      />,
    );
    expect(screen.getByText('The voucher is voided.')).toBeTruthy();
    expect(screen.getByText('This cannot be undone.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Go Back' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText(/Reason/), { target: { value: 'Typo' } });
    expect(screen.getByText('4 / 500')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel Voucher' }));
    expect(screen.getByRole('alert').textContent).toBe('Enter at least 10 characters.');
    expect(onConfirm).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText(/Reason/), { target: { value: 'Wrong payee entered' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cancel Voucher' }));
    expect(onConfirm).toHaveBeenCalledWith('Wrong payee entered');
  });
});

describe('comment field', () => {
  it('states the rules of a comment', () => {
    expect(commentProblem('', { required: true })).toBe('Enter the comment.');
    expect(commentProblem('', { required: false, min: 5 })).toBeUndefined();
    expect(commentProblem('abc', { min: 5, noun: 'reason' })).toBe('Enter at least 5 characters.');
    expect(commentProblem('x'.repeat(11), { max: 10 })).toBe(
      'Shorten the comment to 10 characters.',
    );
  });

  it('counts the characters', () => {
    function Host() {
      const [text, setText] = useState('');
      return <CommentField value={text} onChange={setText} max={20} />;
    }
    render(<Host />);
    expect(screen.getByText('Up to 20 characters')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Comment'), { target: { value: 'Noted' } });
    expect(screen.getByText('5 / 20')).toBeTruthy();
  });
});

describe('searchable select', () => {
  const OPTIONS = [
    { value: 'CBG-NCR', label: 'Consumer Banking Group - NCR' },
    { value: 'CORP-NCR', label: 'Corporate Banking - NCR' },
    { value: 'VIS', label: 'Visayas', hint: 'Cebu' },
  ];

  it('searches names, codes and hints', () => {
    expect(filterOptions(OPTIONS, 'ncr banking').map((o) => o.value)).toEqual([
      'CBG-NCR',
      'CORP-NCR',
    ]);
    expect(filterOptions(OPTIONS, 'cebu').map((o) => o.value)).toEqual(['VIS']);
    expect(filterOptions(OPTIONS, '')).toHaveLength(3);
  });

  it('shows the name, stores the code and offers All in a filter', () => {
    const onChange = vi.fn();
    render(
      <Combobox
        aria-label="Marketing Unit"
        value="CBG-NCR"
        emptyLabel="All"
        options={OPTIONS}
        onChange={onChange}
      />,
    );
    const input = screen.getByRole('combobox', { name: 'Marketing Unit' });
    expect(input).toHaveValue('Consumer Banking Group - NCR');
    fireEvent.change(input, { target: { value: 'visa' } });
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['VisayasCebu']);
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onChange).toHaveBeenCalledWith('VIS');
    fireEvent.click(screen.getByRole('button', { name: 'Clear' }));
    expect(onChange).toHaveBeenLastCalledWith('');
    fireEvent.click(input);
    expect(screen.getAllByRole('option')[0]?.textContent).toBe('All');
    fireEvent.keyDown(input, { key: 'Escape' });
    expect(screen.queryByRole('listbox')).toBeNull();
  });
});
