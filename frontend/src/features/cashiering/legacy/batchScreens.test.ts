import { describe, expect, it } from 'vitest';
import type { LegacyBatch } from '@/api/legacyBatches';
import {
  approverPermission,
  approvers,
  cashScreenOf,
  DPPR_SCREEN,
  INCOME_SCREEN,
  PR2307_SCREEN,
} from './batchScreens';

const batch = (over: Partial<LegacyBatch>): LegacyBatch => ({
  batchNo: 'UIR-2026-000001',
  status: 'DRAFT',
  reason: 'Unclaimed',
  total: 900,
  lineCount: 1,
  createdBy: 'cashier',
  postedCount: 0,
  failedCount: 0,
  ...over,
});

describe('legacy batch screens', () => {
  it('asks the team lead first and top management second for an income batch', () => {
    expect(approverPermission(INCOME_SCREEN, batch({ status: 'FOR_APPROVAL' }))).toBe(
      'CASH_DISPOSITION_APPROVE',
    );
    expect(approverPermission(INCOME_SCREEN, batch({ status: 'FOR_TOP_MANAGEMENT' }))).toBe(
      'CASH_UPP_INCOME_APPROVE',
    );
    expect(approverPermission(INCOME_SCREEN, batch({ status: 'DRAFT' }))).toBeUndefined();
  });

  it('has one approval level for the reversal batches', () => {
    expect(approverPermission(PR2307_SCREEN, batch({ status: 'FOR_APPROVAL' }))).toBe(
      'LEGACY_REVERSAL_APPROVE',
    );
    expect(
      approverPermission(DPPR_SCREEN, batch({ status: 'FOR_TOP_MANAGEMENT' })),
    ).toBeUndefined();
    expect(approverPermission(DPPR_SCREEN, batch({ status: 'EXECUTED' }))).toBeUndefined();
  });

  it('picks the Cashiering screen from the kind of the batch', () => {
    expect(cashScreenOf(batch({ kind: 'PR2307_REVERSAL' }))).toBe(PR2307_SCREEN);
    expect(cashScreenOf(batch({ kind: 'INCOME_RECLASS' }))).toBe(INCOME_SCREEN);
    expect(cashScreenOf(undefined)).toBe(INCOME_SCREEN);
  });

  it('lists the approvers in order', () => {
    expect(approvers(batch({ firstApprovedBy: 'cashtl', finalApprovedBy: 'topmgmt' }))).toBe(
      'cashtl / topmgmt',
    );
    expect(approvers(batch({ approvedBy: 'commtl' }))).toBe('commtl');
    expect(approvers(batch({}))).toBe('');
  });

  it('names the approvers when given the display names', () => {
    const names: Record<string, string> = { cashtl: 'Carla Cash Lead', topmgmt: 'Tomas Top' };
    const name = (login: string) => names[login] ?? login;
    expect(approvers(batch({ firstApprovedBy: 'cashtl', finalApprovedBy: 'topmgmt' }), name)).toBe(
      'Carla Cash Lead / Tomas Top',
    );
  });
});
