// Steps of shots_v15.cjs: storyboards first (wt-*), then the screen states of the UX design (ux-*). The people and
// records are fictitious. UIDM-ISC events are sent through the provisioning simulator of the test environment, which
// feeds the same intake as the SCIM interface.
module.exports = ({ api, one, settle, go, click, tab, BASE, API }) => {
  const SIM = '/admin/identity/simulator/accounts';
  const isabel = { windowsId: 'BDO\\inavarro', userId: 'a013000196', email: 'isabel.navarro@uat.brokerverse.cloud',
    firstName: 'Isabel', lastName: 'Navarro', displayName: 'Isabel Navarro', adGroup: 'BIBS Users', status: 'ACTIVE',
    teamLeaderName: 'Teresa Lopez', unitSegment: 'Retail Marketing', department: 'Marketing', location: 'Makati',
    uidmRequestNo: 'UIDM-2026-005196' };
  const carlo = { windowsId: 'BDO\\cmendoza', userId: 'a013000197', email: 'carlo.mendoza@uat.brokerverse.cloud',
    firstName: 'Carlo', lastName: 'Mendoza', displayName: 'Carlo Mendoza', adGroup: 'BIBS Users', status: 'ACTIVE',
    teamLeaderName: 'Ramon Cruz', unitSegment: 'Corporate Marketing', department: 'Marketing', location: 'Ortigas',
    uidmRequestNo: 'UIDM-2026-005197' };
  const ev = (w, type) => `${SIM}/events/${type}?windowsId=${encodeURIComponent(w)}`;
  const account = async (a) => {
    await api('uamadmin', 'POST', SIM, a).catch(() => api('uamadmin', 'PUT', `${SIM}?windowsId=${encodeURIComponent(a.windowsId)}`, a));
  };
  const userId = (u) => one(`select id from sec_user where username = '${u}'`);
  const roleReq = (code) => one(`select id from nba_access_request where role_code = '${code}' and status not in ('IMPLEMENTED','REJECTED','CANCELLED','APPROVED') order by id desc limit 1`);
  const setting = async (key, value) => {
    const cur = one(`select param_value from sys_parameter where param_key = '${key}'`);
    if (cur !== value) {
      await api('uamadmin', 'PUT', `/nbadmin/uam-settings/${key}`, { value });
      await api('infosec', 'POST', `/nbadmin/uam-settings/${key}/approve`);
    }
  };
  const fill = (label, value) => async (page) => { await page.getByLabel(label, { exact: false }).first().fill(value); };
  const search = (text) => async (page) => {
    await page.getByPlaceholder(/search/i).first().fill(text);
    await page.getByRole('button', { name: /^search$/i }).first().click();
    await settle(page, 600);
  };
  const openUser = (u, t) => async (page) => { await go(`/access/users/${userId(u)}${t ? `?tab=${t}` : ''}`)(page); };
  const ssoSignIn = (windowsId) => async (page) => {
    // The test environment's identity provider stands in for Microsoft Entra ID; its page is not captured.
    await page.goto(`${BASE}/login`);
    await settle(page, 400);
    await page.getByRole('button', { name: /microsoft entra id/i }).click();
    await page.waitForURL(/eiam-simulator/);
    await page.selectOption('select[name=windowsId]', windowsId);
    const back = page.waitForResponse((r) => r.url().startsWith(`${API}/sso/callback`));
    await page.click('button[type=submit]');
    const url = (await back).url();
    await page.waitForTimeout(1500);
    // The web client of the test environment runs on its own port: the ticket is handed over to it.
    await page.goto(url.replace(API, BASE));
    await page.waitForURL((u) => !u.pathname.startsWith('/sso') && !u.pathname.startsWith('/login'), { timeout: 30000 });
    await settle(page, 1500);
  };

  return [
    // ------------------------------------------------ preparation (no capture)
    { file: 'prep', user: 'uamadmin', noShot: true, setup: async () => {
      await setting('UAM_PROVISIONING_ROLES', 'true');
      await account(carlo);
      await api('uamadmin', 'POST', ev(carlo.windowsId, 'JOINER'));
      await api('uamadmin', 'POST', ev(carlo.windowsId, 'MEMBERSHIP'), { groups: ['MKT_AO'], uidmRequestNo: 'UIDM-2026-005197' });
    } },

    // ------------------------------------------------ WT-1 joiner to first sign-in
    { file: 'wt1-01-events', user: 'uamadmin', setup: async () => {
      await account(isabel);
      await api('uamadmin', 'POST', ev(isabel.windowsId, 'JOINER'));
      await api('uamadmin', 'POST', ev(isabel.windowsId, 'MEMBERSHIP'), { groups: ['MKT_AO'], uidmRequestNo: 'UIDM-2026-005196' });
    }, do: go('/access/identity-sync?tab=events') },
    { file: 'wt1-02-users', user: 'uamadmin', do: [go('/access/users'), search('a01300019')] },
    { file: 'wt1-03-user-profile', user: 'uamadmin', h: 1100, do: openUser('a013000196') },
    { file: 'wt1-04-user-groups', user: 'uamadmin', do: openUser('a013000196', 'profiles') },
    { file: 'wt1-05-signin', noShot: true, // same image as the UX design state
      user: '-', do: [go('/login')] },
    { file: 'wt1-06-first-home', user: 'sso-isabel', do: ssoSignIn(isabel.windowsId) },
    { file: 'wt1-07-user-sessions', noShot: true, // same image as the UX design state
      user: 'uamadmin', do: openUser('a013000196', 'sessions') },

    // ------------------------------------------------ WT-2 mover and leaver
    { file: 'wt2-01-mover-event', user: 'uamadmin', setup: async () => {
      await account({ ...isabel, unitSegment: 'Corporate Marketing', location: 'Ortigas', uidmRequestNo: 'UIDM-2026-005240' });
      await api('uamadmin', 'POST', ev(isabel.windowsId, 'MOVER'));
      await api('uamadmin', 'POST', ev(isabel.windowsId, 'MEMBERSHIP'), { groups: ['MKT_TL'], uidmRequestNo: 'UIDM-2026-005240' });
    }, do: go('/access/identity-sync?tab=events') },
    { file: 'wt2-02-mover-groups', noShot: true, // same image as the UX design state
      user: 'uamadmin', do: openUser('a013000196', 'profiles') },
    { file: 'wt2-03-mover-history', noShot: true, // same image as the UX design state
      user: 'uamadmin', h: 1100, do: openUser('a013000196', 'history') },
    { file: 'wt2-04-leaver-user', user: 'uamadmin', h: 1100, setup: async () => {
      await account({ ...carlo, status: 'DEACTIVATED', uidmRequestNo: 'UIDM-2026-005251' });
      await api('uamadmin', 'POST', ev(carlo.windowsId, 'LEAVER'));
    }, do: openUser('a013000197') },
    { file: 'wt2-05-leaver-history', user: 'uamadmin', h: 1100, do: openUser('a013000197', 'history') },

    // ------------------------------------------------ WT-3 group-profile change under maker-checker
    { file: 'wt3-01-profile', user: 'requestor', h: 1100, do: go('/access/group-profiles/MKT_AO') },
    { file: 'wt3-02-propose', user: 'requestor', h: 1300, do: [go('/access/group-profiles/MKT_AO'), click('Propose Change'),
      fill('Reason', 'Account officers need the client enquiry report for the monthly portfolio review')] ,
      after: async (page) => { await page.keyboard.press('Escape'); } },
    { file: 'wt3-03-pending-maker', user: 'requestor', h: 1100, setup: async () => {
      const roles = await api('requestor', 'GET', '/nbadmin/roles');
      const perms = roles.find((r) => r.code === 'MKT_AO').permissions;
      const add = roles.find((r) => r.code === 'MKT_TL').permissions.find((c) => !perms.includes(c));
      await api('requestor', 'POST', '/nbadmin/access-requests', { type: 'MODIFY_ROLE_PERMISSIONS', roleCode: 'MKT_AO',
        permissionsAdded: [add], justification: 'Account officers need the client enquiry report for the monthly portfolio review' });
    }, do: go('/access/group-profiles/MKT_AO?tab=pending') },
    { file: 'wt3-04-checker', user: 'uamapprover', h: 1100, do: go('/access/group-profiles/MKT_AO?tab=pending') },
    { file: 'wt3-05-approve-dialog', user: 'uamapprover', h: 1100, do: [go('/access/group-profiles/MKT_AO?tab=pending'), click('Approve'),
      async (page) => { const t = page.getByRole('dialog').locator('textarea'); if (await t.count()) await t.first().fill('Checked against the access matrix'); }],
      after: async (page) => { await page.getByRole('dialog').getByRole('button', { name: /^approve$/i }).click(); await settle(page); } },
    { file: 'wt3-06-implement', user: 'admin', h: 1100, do: go('/access/group-profiles/MKT_AO?tab=pending'),
      after: async () => { await api('admin', 'POST', `/nbadmin/access-requests/${roleReq('MKT_AO')}/implement`); } },
    { file: 'wt3-07-history', user: 'uamauditor', h: 1100, do: go('/access/group-profiles/MKT_AO?tab=history') },

    // ------------------------------------------------ WT-4 refused event and reconciliation
    { file: 'wt4-01-refused', user: 'uamadmin', setup: async () => {
      await api('uamadmin', 'POST', ev(isabel.windowsId, 'MEMBERSHIP'), { groups: ['MKT_TL', 'MKT_SUPERVISOR'], uidmRequestNo: 'UIDM-2026-005262' }).catch(() => {});
    }, do: go('/access/identity-sync?tab=refused') },
    { file: 'wt4-02-dismiss', user: 'uamadmin', do: [go('/access/identity-sync?tab=refused'),
      async (page) => { await page.getByRole('button', { name: /actions|more/i }).first().click().catch(() => {}); await page.getByRole('menuitem', { name: /dismiss/i }).first().click().catch(async () => page.getByRole('button', { name: /^dismiss$/i }).first().click()); await settle(page, 500); },
      async (page) => { await page.getByRole('dialog').locator('textarea, input').first().fill('MKT_SUPERVISOR is not a group profile of BIBS; UIDM-ISC request UIDM-2026-005262 corrected to MKT_TL only'); }],
      after: async (page) => { await page.getByRole('dialog').getByRole('button', { name: /dismiss/i }).last().click(); await settle(page); } },
    { file: 'wt4-03-reconciliation', noShot: true, // same image as the UX design state
      user: 'uamadmin', setup: async () => {
      await account({ ...isabel, status: 'INACTIVE', unitSegment: 'Corporate Marketing', location: 'Ortigas' });
      await api('uamadmin', 'POST', '/nbadmin/identity/reconciliation/run').catch(() => api('uamadmin', 'POST', '/access/identity/reconciliation/run').catch(() => {}));
    }, do: [go('/access/identity-sync'), click('Synchronise Now'), async (page) => { const d = page.getByRole('dialog'); if (await d.count()) await d.getByRole('button', { name: /synchronise/i }).last().click(); await settle(page, 2500); }, go('/access/identity-sync?tab=reconciliation')] },
    { file: 'wt4-04-status', noShot: true, // same image as the UX design state
      user: 'uamadmin', do: go('/access/identity-sync') },

    // ------------------------------------------------ WT-5 evidence for the auditor and certification
    { file: 'wt5-01-reports', user: 'uamauditor', h: 1300, do: go('/access/reports') },
    { file: 'wt5-02-extract', user: 'uamauditor', h: 1300, do: [go('/access/reports'), click(/take extract|certification extract|xlsx/i)] },
    { file: 'wt5-03-audit', user: 'uamauditor', h: 1300, do: go('/access/audit') },
    { file: 'wt5-04-matrix', noShot: true, // same image as the UX design state
      user: 'uamauditor', h: 1100, do: go('/access/matrix') },

    // ------------------------------------------------ UX design: one state per screen
    { file: 'ux-01-signin', user: '-', do: go('/login') },
    { file: 'ux-02-users', user: 'uamadmin', h: 1300, do: go('/access/users') },
    { file: 'ux-03-user-profile', user: 'uamadmin', h: 1100, do: openUser('a013000196') },
    { file: 'ux-04-user-groups', user: 'uamadmin', do: openUser('a013000196', 'profiles') },
    { file: 'ux-05-user-permissions', user: 'uamadmin', h: 1300, do: openUser('a013000196', 'permissions') },
    { file: 'ux-06-user-data', user: 'uamadmin', h: 1100, do: openUser('a013000196', 'data') },
    { file: 'ux-07-user-sessions', user: 'uamadmin', do: openUser('a013000196', 'sessions') },
    { file: 'ux-08-user-history', user: 'uamadmin', h: 1100, do: openUser('a013000196', 'history') },
    { file: 'ux-09-end-sessions', user: 'uamadmin', do: [openUser('a013000196'), async (page) => { await page.getByText('End Sessions', { exact: true }).first().click(); await settle(page, 600); }],
      after: async (page) => { await page.keyboard.press('Escape'); } },
    { file: 'ux-10-user-auditor', user: 'uamauditor', h: 1100, do: openUser('a013000196') },
    { file: 'ux-11-group-profiles', user: 'requestor', h: 1300, do: go('/access/group-profiles') },
    { file: 'ux-12-gp-definition', user: 'uamadmin', h: 1100, do: go('/access/group-profiles/MKT_AO') },
    { file: 'ux-13-gp-permissions', user: 'uamadmin', h: 1300, do: go('/access/group-profiles/MKT_AO?tab=permissions') },
    { file: 'ux-14-gp-members', user: 'uamadmin', do: go('/access/group-profiles/MKT_AO?tab=members') },
    { file: 'ux-15-gp-new', user: 'requestor', h: 1300, do: [go('/access/group-profiles'), click('New Group Profile'), click(/submit for approval/i)],
      after: async (page) => { await page.keyboard.press('Escape'); } },
    { file: 'ux-16-gp-pending-empty', user: 'uamapprover', do: go('/access/group-profiles/MKT_AO?tab=pending') },
    { file: 'ux-17-idsync-status', user: 'uamadmin', do: go('/access/identity-sync') },
    { file: 'ux-18-idsync-events', user: 'uamadmin', h: 1100, do: go('/access/identity-sync?tab=events') },
    { file: 'ux-19-idsync-refused', user: 'uamadmin', do: go('/access/identity-sync?tab=refused') },
    { file: 'ux-20-idsync-recon', user: 'uamadmin', do: go('/access/identity-sync?tab=reconciliation') },
    { file: 'ux-21-sod-rules', user: 'infosec', h: 1100, do: go('/access/separation-of-duties') },
    { file: 'ux-23-sod-violations', user: 'infosec', h: 1100, do: go('/access/separation-of-duties?tab=violations') },
    { file: 'ux-24-matrix-permission', user: 'uamauditor', h: 1100, do: go('/access/matrix') },
    { file: 'ux-25-matrix-action', user: 'uamauditor', h: 1100, do: [go('/access/matrix'), tab('By Action')] },
    { file: 'ux-26-reports', user: 'uamadmin', h: 1300, do: go('/access/reports') },
    { file: 'ux-27-audit', user: 'uamadmin', h: 1300, do: go('/access/audit') },
    { file: 'ux-28-settings', user: 'uamadmin', h: 1800, do: go('/access/settings') },
    { file: 'ux-29-settings-change', user: 'uamadmin', h: 1100, do: [go('/access/settings'), click('Change')],
      after: async (page) => { await page.keyboard.press('Escape'); } },
    { file: 'ux-30-settings-approve', user: 'infosec', h: 1800, setup: async () => {
      await api('uamadmin', 'PUT', '/nbadmin/uam-settings/UAM_DORMANT_NOTICE_DAYS', { value: '10' }).catch(() => {});
    }, do: go('/access/settings') },
  ];
};
