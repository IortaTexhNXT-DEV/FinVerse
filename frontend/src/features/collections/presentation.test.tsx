import { render, screen } from '@testing-library/react';
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
  dispositionLabel,
  lockText,
  roleLabel,
  ruleSummary,
  taggingOwnerLabel,
  thresholdText,
} from './presentation';
import { applicationFileText, paymentFacts } from './unapplied/labels';
import type { UnappliedRow } from './unapplied/api';
import { DetailInput, HandlerOptions } from './WorkInputs';

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
