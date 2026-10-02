import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { setUserDirectory } from '@/api/users';
import type { DispositionRule } from './api';
import { detailFields } from './collectionsLogic';
import { describeRule } from './escalations/labels';
import { escalationRuleActions } from './escalations/ruleActions';
import { SOURCE_LABELS, installmentActions } from './plans/labels';
import type { Installment } from './plans/api';
import {
  assignmentRuleActions,
  changedFieldText,
  changedValueText,
  componentText,
  dispositionLabel,
  handOffLabel,
  lockText,
  roleLabel,
  ruleSummary,
  taggingOwnerLabel,
  templateText,
  thresholdText,
} from './presentation';
import { applicationFileText, paymentFacts } from './unapplied/labels';
import type { UnappliedRow } from './unapplied/api';
import { DetailInput, HandlerOptions, LockBanner } from './WorkInputs';

/** The screen standards on the Collections screens: names, labels, formats and row menus. */

const RESERVED: DispositionRule = {
  code: 'PR_2307_FOR_REVERSAL',
  label: 'PR 2307 for reversal',
  category: 'B',
  taggingOwner: 'OPERATIONS',
  opsAction: 'CWT2307_REVERSAL',
  allowedRoles: ['MKT_TL', 'CLX_TL'],
};

afterEach(() => setUserDirectory([]));

describe('labels instead of codes', () => {
  it('names the team that acts next and the roles a disposition is reserved to', () => {
    expect(taggingOwnerLabel('OPERATIONS')).toBe('Operations');
    expect(taggingOwnerLabel('MARKETING')).toBe('Marketing');
    expect(taggingOwnerLabel(undefined)).toBeUndefined();
    expect(roleLabel('CLX_TL')).toBe('Collection Team Lead');
    expect(ruleSummary(RESERVED)).toBe(
      'Category B · Operations action · Reserved to Marketing Team Lead, Collection Team Lead',
    );
    expect(ruleSummary(RESERVED)).not.toMatch(/OPERATIONS|MKT_TL|CLX_TL/);
  });

  it('names the basis of a plan in words', () => {
    expect(Object.values(SOURCE_LABELS)).toEqual(['Policy Years', 'Generated', 'Manual']);
  });

  it('shows the lock of the invoice by the name of the team', () => {
    expect(lockText('ADJUSTMENT')).toBe('Locked by Adjustment');
  });

  it('shows a disposition by its label, the code in words while the rules load', () => {
    expect(dispositionLabel([RESERVED], 'PR_2307_FOR_REVERSAL')).toBe('PR 2307 for reversal');
    expect(dispositionLabel(undefined, 'DP_PR_FOR_REVERSAL')).not.toContain('_');
    expect(dispositionLabel([RESERVED], undefined)).toBe('');
  });

  it('names the daily application file by its date, not by the stored file name', () => {
    expect(applicationFileText('FOR_APPLICATION_TO_INVOICE_20261001_20261001_224840.txt')).toBe(
      'File of 01-Oct-2026',
    );
  });

  it('offers the BIR 2307 path as a drop-down of labels', () => {
    const path = detailFields('CWT2307_REVERSAL').find((f) => f.key === 'path');
    expect(path?.type).toBe('select');
    expect(path?.label).not.toMatch(/CASH|CERTIFICATE/);
    render(<DetailInput id="p" field={path!} value="" onChange={vi.fn()} />);
    expect(screen.getByRole('option', { name: 'Paid in cash' })).toHaveValue('CASH');
    expect(screen.queryByRole('option', { name: 'CASH' })).toBeNull();
  });
});

describe('dates as dd-MMM-yyyy', () => {
  it('shows the pick-up date of a check pick-up on the BIBS date picker', () => {
    const date = detailFields('CHECK_PICKUP').find((f) => f.key === 'pickupDate');
    expect(date?.type).toBe('date');
    const onChange = vi.fn();
    render(<DetailInput id="pickup" field={date!} value="2026-10-04" onChange={onChange} />);
    const input = screen.getByDisplayValue('04-Oct-2026');
    expect(input).toHaveAttribute('id', 'pickup');
    expect(input).not.toHaveAttribute('type', 'date');
    expect(input).toHaveAttribute('placeholder', 'dd-MMM-yyyy');
    expect(screen.getByRole('button', { name: 'Choose date' })).toBeInTheDocument();
  });

  it('passes the date typed as dd-MMM-yyyy on as an ISO date', () => {
    const date = detailFields('CHECK_PICKUP').find((f) => f.key === 'pickupDate');
    const onChange = vi.fn();
    render(<DetailInput id="pickup" field={date!} value="" onChange={onChange} />);
    fireEvent.change(screen.getByPlaceholderText('dd-MMM-yyyy'), {
      target: { value: '04-Oct-2026' },
    });
    expect(onChange).toHaveBeenLastCalledWith('2026-10-04');
  });
});

describe('names instead of logins', () => {
  it('lists the handlers of a drop-down by name', () => {
    setUserDirectory([{ username: 'clxhandler', displayName: 'Clara Collection Handler' }]);
    render(
      <select aria-label="Handler">
        <HandlerOptions handlers={['clxhandler']} />
      </select>,
    );
    const option = screen.getByRole('option', { name: 'Clara Collection Handler' });
    expect(option).toHaveValue('clxhandler');
  });

  it('names the designated user of an escalation rule and formats its amount', () => {
    const name = (login: string) => (login === 'mkttl' ? 'Marites Marketing Lead' : login);
    const rule = { basis: 'BROKEN_PROMISES_COUNT', threshold: 1, targetLevel: 'USER' } as const;
    expect(describeRule({ ...rule, targetUsername: 'mkttl' }, name)).toBe(
      'Broken promises ≥ 1 → Marites Marketing Lead',
    );
    const amount = {
      basis: 'AMOUNT_OVER',
      threshold: 1000000,
      targetLevel: 'SECTION_HEAD',
    } as const;
    expect(describeRule(amount)).toBe('Outstanding at or above 1,000,000.00 → Section Head');
    expect(thresholdText('AGING_FROM_BOOKING', 45)).toBe('45');
  });

  it('shows the account officer, unit head, handler and insurer of an unapplied payment by name', () => {
    setUserDirectory([
      { username: 'ao', displayName: 'Aileen Account Officer' },
      { username: 'mkttl', displayName: 'Marites Marketing Lead' },
      { username: 'clxhandler', displayName: 'Clara Collection Handler' },
    ]);
    const row = {
      unappliedRef: 'UNP-2026-000003',
      paymentDate: '2026-10-01',
      ageDays: 0,
      currency: 'PHP',
      amount: 1500,
      balance: 1000,
      payor: 'Castillo, Sofia',
      cashieringTab: 'UNAPPLIED',
      account: {
        assuredName: 'Castillo, Sofia',
        aoUsername: 'ao',
        unitHead: 'mkttl',
        handler: 'clxhandler',
        insurerCode: 'INS-MGIC',
      },
    } as unknown as UnappliedRow;
    const facts = Object.fromEntries(
      paymentFacts(row, (code) => (code === 'INS-MGIC' ? 'Malayan General' : code)),
    );
    expect(facts['Account Officer']).toBe('Aileen Account Officer');
    expect(facts['Unit Head']).toBe('Marites Marketing Lead');
    expect(facts['Collection Handler']).toBe('Clara Collection Handler');
    expect(facts.Insurer).toBe('Malayan General');
  });
});

describe('row action menus', () => {
  it('puts the actions of an assignment rule in one menu, deactivation last in red', () => {
    const on = { change: vi.fn(), activate: vi.fn() };
    const active = assignmentRuleActions({ active: true }, on);
    expect(active.map((a) => a.label)).toEqual(['Change Rule', 'Deactivate']);
    expect(active[1]?.danger).toBe(true);
    expect(assignmentRuleActions({ active: false }, on).map((a) => a.label)).toEqual([
      'Change Rule',
      'Activate',
    ]);
  });

  it('offers the escalation rule actions by permission and status', () => {
    const on = { preview: vi.fn(), change: vi.fn(), authorize: vi.fn(), deactivate: vi.fn() };
    const all = { preview: true, setup: true, authorize: true };
    expect(
      escalationRuleActions({ recordStatus: 'PENDING_AUTHORIZATION' }, all, on).map((a) => a.label),
    ).toEqual(['Preview', 'Change', 'Authorize']);
    const active = escalationRuleActions({ recordStatus: 'ACTIVE' }, all, on);
    expect(active.at(-1)).toMatchObject({ label: 'Deactivate', danger: true });
    const reader = { preview: false, setup: false, authorize: false };
    expect(escalationRuleActions({ recordStatus: 'ACTIVE' }, reader, on)).toEqual([]);
  });

  it('offers Generate SOA and Record Promise of an installment in its row menu', () => {
    const installment = { seq: 2, invoiceNo: 'BI-HO-2026-000002', status: 'DUE' } as Installment;
    const actions = {
      billed: new Set<number>(),
      canBill: true,
      canPromise: true,
      busy: false,
      onBill: vi.fn(),
      onPromise: vi.fn(),
    };
    const menu = installmentActions(installment, actions);
    expect(menu.map((a) => a.label)).toEqual(['Generate SOA', 'Record Promise']);
    menu[0]?.onSelect();
    expect(actions.onBill).toHaveBeenCalledWith(2);
    expect(installmentActions(installment, { ...actions, billed: new Set([2]) })).toHaveLength(1);
  });
});

describe('the account record in words', () => {
  it('names the premium components, never their codes in words', () => {
    expect(componentText('FST')).toBe('FST');
    expect(componentText('PR2307')).toBe('PR 2307');
    expect(componentText('BASIC')).toBe('Basic Premium');
    expect(componentText('PREMIUM_TAX_VAT')).toBe('Premium Tax / VAT');
    expect(componentText('OTHER')).toBe('Other Charges');
  });

  it('shows the change log by field name, the handler by name and the codes by label', () => {
    const look = {
      name: (login: string) => (login === 'clxhandler' ? 'Clara Collection Handler' : login),
      disposition: (code: string) => (code === 'FOR_CHECK_PICKUP' ? 'For check pick-up' : code),
      effort: (code: string) => (code === 'CALL' ? 'Phone call' : code),
    };
    expect(changedFieldText('taggingOwner')).toBe('Tagging Owner');
    expect(changedFieldText('dispositionCode')).toBe('Disposition');
    expect(changedFieldText('lastEffortCode')).toBe('Last Effort');
    expect(changedFieldText('currentHandler')).toBe('Handler');
    expect(changedFieldText('someNewField')).toBe('Some New Field');
    expect(changedValueText('currentHandler', 'clxhandler', look)).toBe('Clara Collection Handler');
    expect(changedValueText('dispositionCode', 'FOR_CHECK_PICKUP', look)).toBe('For check pick-up');
    expect(changedValueText('lastEffortCode', 'CALL', look)).toBe('Phone call');
    expect(changedValueText('taggingOwner', 'MARKETING', look)).toBe('Marketing');
    expect(changedValueText('status', 'OPEN', look)).toBe('Open');
    expect(changedValueText('category', 'B', look)).toBe('Category B');
    expect(changedValueText('remarks', null, look)).toBe('—');
  });

  it('names the hand-off to Operations and its feed, never the code in words', () => {
    expect(handOffLabel('CHECK_PICKUP')).toBe('Check pick-up');
    expect(handOffLabel('COLLECTION_CHECK_PICKUP')).toBe('Check pick-up');
    expect(handOffLabel('COLLECTION_CWT')).toBe('BIR 2307 reversal');
    expect(handOffLabel('DP_REVERSAL')).toBe('DP reversal');
    expect(handOffLabel('CHECK_PICKUP')).not.toBe('Check Pickup');
  });

  it('names the statement template by its version, never its code', () => {
    expect(templateText('CLX_SOA v1')).toBe('Statement template version 1.');
    expect(templateText(undefined)).toBe('');
  });

  it('says who is editing an account in one sentence with single spaces', () => {
    setUserDirectory([{ username: 'clxhandler', displayName: 'Clara Collection Handler' }]);
    render(<LockBanner login="clxhandler" since="2026-10-01T17:53:00Z" />);
    const banner = screen.getByRole('status');
    expect(banner.children).toHaveLength(2);
    expect(banner.textContent).toMatch(
      /^Clara Collection Handler is editing since \d{2}-[A-Z][a-z]{2}-2026 \d{2}:\d{2}$/,
    );
    expect(banner.textContent).not.toMatch(/clxhandler| {2}/);
  });
});
