import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Customer Service Facility screens (BRD-9). */
export const CSF_HELP: HelpSection = {
  id: 'csf',
  module: 'Customer Service Facility',
  intro:
    'The Customer Service Facility is the workspace of the contact centre: find the client of a caller, see the accounts with their status, the payments, renewal advices, e-policies and documents, verify the caller before changing the contact details, resend documents and upload what the client sends. Every search, view, download, resend, upload and change is logged for the supervisors.',
  screens: [
    {
      name: 'Customer Search',
      path: '/csf',
      summary:
        'One search box by key: name, client ID (client code or ID number), account number (ARN or policy number), PN number or loan application number.',
      workflow: [
        'Choose the key, enter the value and select Search.',
        'Open a client to see the Servicing View; when one client matches, it opens directly.',
      ],
      controls: [
        'A name search needs at least 3 characters; refine the search when more than 50 clients match.',
        'Every search is logged with its criteria.',
      ],
    },
    {
      name: 'Servicing View',
      summary:
        'The client summary with the contact details, then the tabs Accounts (status, stage, policy number, period, balance), Payments, Renewal Advice, E-policies, Documents and Contact History, each loaded on its own.',
      workflow: [
        'Update Contact: record the verification checklist of the caller, then change the e-mail, mobile, phone or address with a reason.',
        'Refer to Fulfilment Unit: other changes, such as the name or civil status, go to the fulfilment unit.',
        'Resend a renewal advice or a confirmed e-policy to the registered e-mail; the password follows in a separate e-mail.',
        'Upload a document to the client or one of its accounts; download one document or several as a ZIP file.',
      ],
      controls: [
        'A change needs a passed verification of the caller, valid for 30 minutes.',
        'Only a supervisor may send to another address, with a reason.',
        'Repeated failed verifications of one client in a day alert the supervisors.',
      ],
    },
    {
      name: 'Cases',
      path: '/csf/cases',
      summary:
        'The work queues of the cases: My Cases, Returned to Me, CSR Action Required, the Team Queue of the supervisors, the Fulfilment Unit Queue, All Open and Closed, with the status, due date and SLA indicator of each case.',
      workflow: [
        'Log Case: on the Servicing View of a client, or here for a caller who is not a client; choose the channel, case type, nature of transaction, product type, market segment and fulfilment unit, then Submit to Fulfilment Unit or Resolve and Close.',
        'Open a case to add notes and documents; the fulfilment unit resolves and closes it, returns it to the agent or asks the agent to act with the client.',
        'The supervisor reassigns, escalates, changes the category or closes an open case; a closed case gives a linked follow-up case.',
      ],
      controls: [
        'The due date counts the turnaround time of the case type in working days from the creation of the case.',
        'A second open case for the same client, account and nature of transaction is refused.',
        'Every action is in the History tab of the case and the audit trail.',
      ],
    },
    {
      name: 'Case Dashboard',
      path: '/csf/case-dashboard',
      summary:
        'Case volume, ageing, SLA compliance rate, breached and approaching cases and the distribution of the cases, from the same cases as the case reports.',
      workflow: ['Set the period and the filters; open a case from the list of open cases.'],
      controls: [
        'The Case Register, Case Ageing and SLA and Case Summary reports give the same figures in PDF, Excel and CSV.',
      ],
    },
    {
      name: 'Contact Changes',
      path: '/csf/changes',
      summary:
        'Contact changes, refused changes and referrals to the fulfilment unit with the values before and after, the verification, the agent and the sending to the legacy systems.',
      workflow: ['Filter by status, agent, dates or client and open the client from a row.'],
      controls: ['Changes cannot be edited; the audit trail keeps every change.'],
    },
    {
      name: 'Customer Service Reports',
      path: '/csf/reports',
      summary:
        'Contact Changes and Agent Activity (counts per agent and day, or the detail), in PDF, Excel or CSV.',
      workflow: ['Open a report, set the dates and filters and run or export it.'],
      controls: ['The audit trail of client records is on the Audit Trail screen.'],
    },
  ],
};
