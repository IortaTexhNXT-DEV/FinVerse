import { mayOpen } from './access';
import { NAV_GROUPS } from './modules';
import { MODULE_OFF } from './productModules';

/** Every menu path a user with these permissions sees. */
function menuOf(can: (permission: string) => boolean): string[] {
  return NAV_GROUPS.flatMap((group) =>
    group.modules.flatMap((module) =>
      module.screens.filter((s) => s.hidden !== true && mayOpen(s, can)).map((s) => s.path),
    ),
  );
}

/**
 * Client decisions of 17-Oct-2026: reinsurance and the partner portal are deferred to Phase 2 and
 * hidden from the BDOI personas; the super user Brokerverse, who holds every permission, keeps the
 * Data Migration console. Neither Phase 2 function has a menu of its own in the platform.
 */
describe('Phase 2 functions and the Data Migration menu', () => {
  it('shows the Data Migration console and the legacy inquiry to the super user', () => {
    const menu = menuOf((p) => !p.startsWith(MODULE_OFF));
    expect(menu).toEqual(expect.arrayContaining(['/migration', '/legacy-inquiry']));
  });

  it('has no reinsurance or partner portal menu for a BDOI persona', () => {
    const menu = menuOf(
      (p) => !p.startsWith(MODULE_OFF) && !p.startsWith('MIG_') && p !== 'RI_BROKING_TRANSACT',
    );
    expect(menu.filter((p) => /reinsurance|portal/.test(p))).toEqual([]);
    expect(menu.filter((p) => p.startsWith('/migration'))).toEqual([]);
  });
});
