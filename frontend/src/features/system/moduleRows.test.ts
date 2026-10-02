import type { ModuleSwitch } from '@/api/modules';
import { isPending, moduleActions, moduleStatus } from './moduleRows';

const base: ModuleSwitch = {
  code: 'EB',
  name: 'Employee Benefits',
  enabled: true,
} as ModuleSwitch;

describe('module rows', () => {
  it('shows On, Off or the change waiting for approval', () => {
    expect(moduleStatus(base)).toEqual({ status: 'ACTIVE', label: 'On' });
    expect(moduleStatus({ ...base, enabled: false })).toEqual({ status: 'INACTIVE', label: 'Off' });
    expect(moduleStatus({ ...base, pendingEnabled: false, pendingBy: 'sysadmin' })).toEqual({
      status: 'PENDING_APPROVAL',
      label: 'Switch Off Pending',
    });
    expect(isPending(base)).toBe(false);
  });

  it('offers the switch to the maker and the decision to another checker', () => {
    const choose = vi.fn();
    const maker = { username: 'sysadmin', mayManage: true, mayApprove: true };
    expect(moduleActions(base, maker, choose).map((a) => a.label)).toEqual(['Switch Off']);
    const waiting = { ...base, pendingEnabled: false, pendingBy: 'sysadmin' };
    expect(moduleActions(waiting, maker, choose).map((a) => a.label)).toEqual(['Withdraw Request']);
    const checker = { username: 'checker', mayManage: false, mayApprove: true };
    expect(moduleActions(waiting, checker, choose).map((a) => a.label)).toEqual([
      'Approve',
      'Reject',
    ]);
    expect(moduleActions(base, { ...checker, mayApprove: false }, choose)).toEqual([]);
  });
});
