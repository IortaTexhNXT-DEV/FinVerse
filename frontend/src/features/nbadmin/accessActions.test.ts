import type { AccessRequest } from '@/api/nbadmin';
import { requestActions } from './accessActions';
import type { Viewer } from './accessActions';

const request = (patch: Partial<AccessRequest>): AccessRequest => ({
  id: 1,
  requestNo: 'AR-2026-000001',
  type: 'CREATE_USER',
  userType: 'INTERNAL',
  summary: '',
  username: 'a013000196',
  roleCodes: ['MKT_AO'],
  status: 'PENDING',
  requestedBy: 'requestor',
  requestedAt: '2026-09-25T01:00:00Z',
  permissionsAdded: [],
  permissionsRemoved: [],
  returnedCount: 0,
  details: { unlock: false },
  lifecycle: {
    assignedApprover: 'uamapprover',
    approvers: [{ sequence: 1, approver: 'uamapprover', decision: 'PENDING' }],
    riskFlags: [],
    secondApprovalRequired: false,
  },
  ...patch,
});

const viewer = (username: string, permissions: string[], anyApprover = false): Viewer => ({
  username,
  can: (p) => permissions.includes(p),
  anyApprover,
});

const requestor = viewer('requestor', ['UAM_ENROLL', 'UAM_CANCEL', 'UAM_CORRECT']);
const approver = viewer('uamapprover', ['ACCESS_APPROVE']);
const other = viewer('approver', ['ACCESS_APPROVE']);

describe('actions on an access request', () => {
  it('lets the creator edit, correct and cancel', () => {
    expect(requestActions(request({ status: 'DRAFT' }), requestor)).toEqual(['edit', 'cancel']);
    expect(requestActions(request({ status: 'RETURNED' }), requestor)).toEqual(['edit', 'cancel']);
    expect(requestActions(request({}), requestor)).toEqual(['cancel']);
    expect(requestActions(request({ status: 'APPROVED' }), requestor)).toEqual([]);
    expect(
      requestActions(request({ status: 'RETURNED' }), viewer('requestor', ['UAM_ENROLL'])),
    ).toEqual([]);
  });

  it('lets only the chosen approver decide, unless any approver may', () => {
    expect(requestActions(request({}), approver)).toEqual(['approve', 'return', 'reject']);
    expect(requestActions(request({}), other)).toEqual([]);
    expect(requestActions(request({}), viewer('approver', ['ACCESS_APPROVE'], true))).toEqual([
      'approve',
      'return',
      'reject',
    ]);
    expect(
      requestActions(request({ username: 'uamapprover', type: 'MODIFY_USER' }), approver),
    ).toEqual([]);
  });

  it('keeps the second approval and the implementation apart', () => {
    const second = request({ status: 'PENDING_SECOND', decidedBy: 'uamapprover' });
    expect(requestActions(second, viewer('secapprover', ['UAM_SECOND_APPROVE']))).toEqual([
      'secondApprove',
      'return',
      'reject',
    ]);
    expect(
      requestActions(second, viewer('uamapprover', ['ACCESS_APPROVE', 'UAM_SECOND_APPROVE'])),
    ).toEqual([]);
    const implement = request({
      status: 'FOR_IMPLEMENTATION',
      type: 'CREATE_ROLE',
      username: undefined,
      requestedBy: 'badmin',
    });
    expect(requestActions(implement, viewer('admin', ['ROLE_MANAGE']))).toEqual(['implement']);
    expect(requestActions(implement, viewer('badmin', ['ROLE_MANAGE', 'ACCESS_REQUEST']))).toEqual(
      [],
    );
    expect(
      requestActions(request({ status: 'SCHEDULED', decidedBy: 'uamapprover' }), approver),
    ).toEqual(['cancel']);
  });
});
