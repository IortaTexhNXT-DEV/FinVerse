import { describe, expect, it } from 'vitest';
import {
  allocationList,
  bankCurrency,
  canSave,
  emptyReceiptForm,
  showsAllocations,
  toPdcInput,
  toReceiptInput,
} from './receiptForm';
import type { ReceiptForm } from './receiptForm';

const ctx = { companyId: 1, branchId: 2, currency: 'PHP' };

function form(patch: Partial<ReceiptForm>): ReceiptForm {
  return { ...emptyReceiptForm(), ...patch };
}

describe('receipt entry form', () => {
  it('saves only a complete receipt without allocation errors', () => {
    const cheque = form({
      partyCode: 'PH1',
      bankAccountCode: '1111',
      amount: 100,
      instrumentNo: '123',
      draweeBank: 'BDO',
    });
    expect(canSave(cheque, 0)).toBe(true);
    expect(canSave(cheque, 1)).toBe(false);
    expect(canSave({ ...cheque, draweeBank: '' }, 0)).toBe(false);
    expect(canSave({ ...cheque, amount: 0 }, 0)).toBe(false);
    const other = form({ payerType: 'OTHER', mode: 'CASH', bankAccountCode: '1111', amount: 5 });
    expect(canSave(other, 0)).toBe(false);
    expect(canSave({ ...other, payerName: 'Walk-in' }, 0)).toBe(true);
  });

  it('maps a party receipt with manual allocations', () => {
    const input = toReceiptInput(
      form({ partyCode: 'PH1', bankAccountCode: '1111', amount: 100 }),
      ctx,
      { 7: 60 },
    );
    expect(input.partyCode).toBe('PH1');
    expect(input.payerName).toBeUndefined();
    expect(input.incomeAccountCode).toBeUndefined();
    expect(input.instrumentNo).toBeUndefined();
    expect(input.allocations).toEqual([{ debitItemId: 7, amount: 60 }]);
  });

  it('maps a miscellaneous receipt to the income account without allocations', () => {
    const input = toReceiptInput(
      form({ payerType: 'OTHER', payerName: 'Tenant', mode: 'CASH', instrumentNo: 'X1' }),
      ctx,
      { 7: 60 },
    );
    expect(input.partyCode).toBeUndefined();
    expect(input.incomeAccountCode).toBe('4700');
    expect(input.allocationMethod).toBe('NONE');
    expect(input.instrumentDate).toBeUndefined();
    expect(input.instrumentNo).toBe('X1');
    expect(input.allocations).toEqual([]);
  });

  it('maps a post-dated cheque to the first selected debit note', () => {
    const pdc = form({ mode: 'PDC', partyCode: 'PH1', instrumentNo: '9', narration: 'n' });
    expect(toPdcInput(pdc, ctx, { 3: 10 }).debitItemId).toBe(3);
    expect(toPdcInput(pdc, ctx, {}).debitItemId).toBeUndefined();
    expect(toPdcInput(pdc, ctx, {}).narration).toBe('n');
  });

  it('shows allocations for manual party receipts and PDCs', () => {
    expect(showsAllocations(form({ partyCode: 'PH1' }))).toBe(true);
    expect(showsAllocations(form({ partyCode: 'PH1', method: 'FIFO' }))).toBe(false);
    expect(showsAllocations(form({ partyCode: 'PH1', method: 'FIFO', mode: 'PDC' }))).toBe(true);
    expect(showsAllocations(form({ partyCode: '' }))).toBe(false);
    expect(showsAllocations(form({ payerType: 'OTHER', partyCode: 'X' }))).toBe(false);
  });

  it('resolves the bank currency and allocation list', () => {
    const banks = [{ code: '1120', currency: 'USD' }];
    expect(bankCurrency(banks, '1120')).toBe('USD');
    expect(bankCurrency(banks, '')).toBe('PHP');
    expect(allocationList({ 1: 5, 2: 6 })).toHaveLength(2);
  });
});
