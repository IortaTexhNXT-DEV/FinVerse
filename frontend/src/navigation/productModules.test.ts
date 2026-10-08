import { mayOpen } from './access';
import { MODULES } from './modules';
import { MODULE_OFF, PRODUCT_MODULE_PATHS, mayUse, productModuleOf } from './productModules';

const SERVER_MODULES = [
  'NEW_BUSINESS',
  'PRODUCT_MAINTENANCE',
  'OPERATIONS',
  'COLLECTIONS',
  'ACCOUNTING_DISBURSEMENT',
  'RENEWAL',
  'CLAIMS_HANDLING',
  'EMPLOYEE_BENEFITS',
  'CUSTOMER_SERVICING',
  'SANCTION_SCREENING',
  'SUBMITTED_POLICIES',
  'DATA_MIGRATION',
  'PAYABLES',
  'RECEIVABLES',
  'ASSETS_INVESTMENTS',
  'BUDGET',
  'TAX_STATUTORY',
];

const screens = MODULES.flatMap((m) => m.screens);

describe('product module switches in the menu', () => {
  it('knows exactly the modules of the server catalogue', () => {
    const byName = (a: string, b: string) => a.localeCompare(b);
    expect(Object.keys(PRODUCT_MODULE_PATHS).sort(byName)).toEqual(
      [...SERVER_MODULES].sort(byName),
    );
  });

  it('ties screens to their module by path, the longest prefix first', () => {
    expect(productModuleOf('/claims-handling/worklist')).toBe('CLAIMS_HANDLING');
    expect(productModuleOf('/claims')).toBeUndefined();
    expect(productModuleOf('/tax/vat')).toBe('TAX_STATUTORY');
    expect(productModuleOf('/planning/budgets/12')).toBe('BUDGET');
    expect(productModuleOf('/planning/fx-revaluation')).toBeUndefined();
    expect(productModuleOf('/admin/users')).toBeUndefined();
  });

  it('shows no screen of a switched-off module and keeps the others', () => {
    const modules = {
      switchedOff: ['RENEWAL', 'PAYABLES'],
      inactivePermissions: ['RNW_VIEW'],
    };
    const can = (p: string) => mayUse(p, modules, () => true);
    const renewal = screens.filter((s) => s.path.startsWith('/renewal'));
    expect(renewal.length).toBeGreaterThan(0);
    renewal.forEach((s) => expect(mayOpen(s, can), s.path).toBe(false));
    screens
      .filter((s) => s.path.startsWith('/payables'))
      .forEach((s) => expect(mayOpen(s, can), s.path).toBe(false));
    const collections = screens.find((s) => s.path === '/collections');
    expect(collections && mayOpen(collections, can)).toBe(true);
    expect(screens.find((s) => s.path === '/admin/modules')?.productModule).toBeUndefined();
    expect(screens.find((s) => s.path === '/renewal')?.productModule).toBe('RENEWAL');
  });

  it('refuses the permissions of switched-off modules', () => {
    const modules = { switchedOff: ['RENEWAL'], inactivePermissions: ['RNW_VIEW'] };
    expect(mayUse('RNW_VIEW', modules, () => true)).toBe(false);
    expect(mayUse('JOURNAL_VIEW', modules, () => true)).toBe(true);
    expect(mayUse('JOURNAL_VIEW', modules, () => false)).toBe(false);
    expect(mayUse(`${MODULE_OFF}RENEWAL`, modules, () => false)).toBe(true);
    expect(mayUse(`${MODULE_OFF}PAYABLES`, modules, () => true)).toBe(false);
  });
});
