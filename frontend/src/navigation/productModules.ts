import type { ModulesInUse } from '@/api/modules';
import type { NavGroup, ScreenDef } from './types';

/**
 * Prefix of the pseudo permission that asks whether a product module is switched off: `can` of the
 * signed-in user answers true for "MODULE_OFF:<code>" of a module switched off in the deployment.
 */
export const MODULE_OFF = 'MODULE_OFF:';

/**
 * The screens of each product module by path, mirroring the module catalogue of the server
 * (system.domain.ProductModule). Screens outside every list belong to the platform, which is
 * always on. The longest matching prefix wins (the insurer tax schedules inside Tax and Statutory).
 */
export const PRODUCT_MODULE_PATHS: Readonly<Record<string, readonly string[]>> = {
  NEW_BUSINESS: [
    '/crm',
    '/quotations',
    '/accounts',
    '/proposals',
    '/placement',
    '/issuance',
    '/booking',
    '/nb',
  ],
  PRODUCT_MAINTENANCE: ['/product-maintenance'],
  OPERATIONS: [
    '/operations',
    '/cashiering',
    '/remittance',
    '/adjustment',
    '/prodrecon',
    '/commission',
  ],
  COLLECTIONS: ['/collections'],
  ACCOUNTING_DISBURSEMENT: ['/disbursement', '/payment-requests', '/acsl', '/frbs'],
  RENEWAL: ['/renewal'],
  CLAIMS_HANDLING: ['/claims-handling'],
  EMPLOYEE_BENEFITS: ['/eb'],
  CUSTOMER_SERVICING: ['/csf'],
  SANCTION_SCREENING: ['/screening', '/screening-setup'],
  SUBMITTED_POLICIES: ['/submitted'],
  DATA_MIGRATION: ['/migration', '/legacy-inquiry'],
  PAYABLES: ['/payables'],
  RECEIVABLES: ['/receivables'],
  ASSETS_INVESTMENTS: ['/assets', '/investments'],
  BUDGET: ['/planning/budgets', '/planning/budget-vs-actual'],
  TAX_STATUTORY: ['/tax'],
  UNDERWRITING: ['/underwriting'],
  INSURER_CLAIMS: ['/claims'],
  REINSURANCE: ['/reinsurance'],
  ACTUARIAL_RESERVES: ['/reserves'],
  CONSOLIDATION: ['/planning/consolidation', '/planning/intercompany'],
  INSURER_TAX: ['/tax/premium-tax', '/tax/dst', '/tax/ic-schedules'],
};

function matches(path: string, prefix: string): boolean {
  return path === prefix || path.startsWith(`${prefix}/`);
}

/**
 * The product module of a screen path.
 *
 * @returns the module code, undefined for a platform screen
 */
export function productModuleOf(path: string): string | undefined {
  let best: { code: string; length: number } | undefined;
  for (const [code, prefixes] of Object.entries(PRODUCT_MODULE_PATHS)) {
    for (const prefix of prefixes) {
      if (matches(path, prefix) && (best === undefined || prefix.length > best.length)) {
        best = { code, length: prefix.length };
      }
    }
  }
  return best?.code;
}

/** A screen tied to its product module. */
function withModule(screen: ScreenDef): ScreenDef {
  const code = productModuleOf(screen.path);
  return code === undefined ? screen : { ...screen, productModule: code };
}

/** The sidebar groups with every screen tied to its product module (menus and routes follow). */
export function withProductModules(groups: NavGroup[]): NavGroup[] {
  return groups.map((g) => ({
    ...g,
    modules: g.modules.map((m) => ({ ...m, screens: m.screens.map(withModule) })),
  }));
}

/**
 * Whether a permission check passes with the product modules in use: "MODULE_OFF:<code>" passes
 * when the module is switched off; a permission of switched-off modules grants nothing; any other
 * permission is decided by `holds`.
 */
export function mayUse(
  permission: string,
  modules: ModulesInUse,
  holds: (permission: string) => boolean,
): boolean {
  if (permission.startsWith(MODULE_OFF)) {
    return modules.switchedOff.includes(permission.slice(MODULE_OFF.length));
  }
  return !modules.inactivePermissions.includes(permission) && holds(permission);
}
