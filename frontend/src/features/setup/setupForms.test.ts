import {
  isEmail,
  employeeInput,
  employeeProblems,
  layoutProblems,
  newEmployee,
  ruleCriteria,
  ruleProblems,
} from './setupForms';

describe('setup forms of BRD-5', () => {
  it('validates employees', () => {
    const empty = newEmployee(1, 2);
    expect(Object.keys(employeeProblems(empty)).sort((a, b) => a.localeCompare(b))).toEqual([
      'costCenter',
      'employeeNo',
      'fullName',
    ]);
    const ok = { ...empty, employeeNo: 'E-001', fullName: 'Ana Cruz', costCenter: 'FIN' };
    expect(employeeProblems(ok)).toEqual({});
    expect(employeeProblems({ ...ok, email: 'nope' }).email).toBeDefined();
    expect(
      employeeProblems({ ...ok, hiredOn: '2026-02-01', separatedOn: '2026-01-01' }).separatedOn,
    ).toBeDefined();
    const back = employeeInput(1, {
      id: 9,
      employeeNo: 'E-9',
      fullName: 'X',
      branchId: 3,
      costCenter: 'UW',
      active: true,
    });
    expect(back.branchId).toBe(3);
  });

  it('checks e-mail addresses', () => {
    expect(isEmail('ana@bdo.com.ph')).toBe(true);
    expect(isEmail('ana@bdo')).toBe(false);
    expect(isEmail('a b@bdo.ph')).toBe(false);
    expect(isEmail('@bdo.ph')).toBe(false);
    expect(isEmail('a@@bdo.ph')).toBe(false);
  });

  it('validates cost-centre rules and describes their criteria', () => {
    const rule = { companyId: 1, priority: 0, costCenter: '', active: true };
    expect(Object.keys(ruleProblems(rule)).sort((a, b) => a.localeCompare(b))).toEqual([
      'costCenter',
      'priority',
    ]);
    expect(ruleProblems({ ...rule, priority: 10, costCenter: 'FIN' })).toEqual({});
    expect(ruleCriteria({ priority: 1, costCenter: 'FIN', active: true })).toBe('Any posting');
    expect(
      ruleCriteria({
        priority: 1,
        costCenter: 'FIN',
        active: true,
        sourceModule: 'FRBS',
        accountCode: '5614',
        partyCode: '',
      }),
    ).toBe('FRBS · account 5614');
  });

  it('requires amounts in a statement layout', () => {
    const layout = {
      bankAccountCode: '1101',
      name: 'Export',
      dateColumn: 'Date',
      datePattern: 'yyyy-MM-dd',
      chequeNumberFirst: true,
    };
    expect(layoutProblems(layout).amountColumn).toBeDefined();
    expect(layoutProblems({ ...layout, amountColumn: 'Amount' })).toEqual({});
    expect(layoutProblems({ ...layout, debitColumn: 'Dr', creditColumn: 'Cr' })).toEqual({});
    expect(
      Object.keys(layoutProblems({ ...layout, bankAccountCode: '', name: '', dateColumn: '' })),
    ).toEqual(expect.arrayContaining(['bankAccountCode', 'name', 'dateColumn']));
  });
});
