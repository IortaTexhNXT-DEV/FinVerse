# BIBS UX readiness checklist

Prepared by the iorta TechNXT project team for the UAT readiness programme. Every pattern of the client feedback of
26-Sep-2026 is listed with the shared component that implements it, the screens verified and the screenshot
reference (a slug of `docs/design/screenshots`, whose README gives the file number). The rules themselves are in
`BDO_UX_GUIDELINES.md` sections 8 to 10; the fixed findings are in `ALIGNMENT_FINDINGS.md`.

Tick the last column during the UAT readiness review.

## 1. Structure and presentation

| # | Pattern | Shared component | Screens verified | Screenshot | Status | UAT |
|---|---|---|---|---|---|---|
| S1 | Workflow and status history as a table (Stage, From Stage, Action, By, Date and Time, Remarks, Duration in Stage; newest first, sort toggle) | `HistoryTable` in `WorkflowPanel` and the History tabs | Client, quotation, proposal, account, disbursement voucher, FRBS service fee run, adjustment and package requests | `client-record`, `quotation-record`, `disbursement-voucher`, `frbs-service-fee-run` | Done | ☐ |
| S2 | Current stage as a status strip (Stage, In Stage Since, Due, Assigned To), no "Since … by …" line | `WorkflowPanel` | Every record page with a workflow | `client-record`, `account-record` | Done | ☐ |
| S3 | Key-value blocks as aligned definition grids; "Not provided" for empty sections | `DefinitionGrid`, `.detail-list` | Client details; every `.detail-list` block | `client-record` | Done | ☐ |
| S4 | Audit trails and change logs as tables with display names | `DataTable`, `UserName` | Client audit history, audit trail, access change log | `client-record`, `message-log` | Done | ☐ |
| S5 | Uniform status pills (one height, font and minimum width; column-wide in tables; short forms with tooltip; never wrap) | `StatusBadge`, `statusTones` | All lists and headers | `quotations`, `collections-unapplied`, `disbursement-workbench` | Done | ☐ |
| S6 | Uniform flag chips in their own column or the header flag list | `Tag` | Client header, account flags, PR worklist | `client-record`, `collections-worklist` | Done | ☐ |
| S7 | Sticky Header Blue table header, zebra rows, hover and selected row | `DataTable`, `patterns.css` | All tables | any list | Done | ☐ |
| S8 | Column alignment by kind: text left, amounts right with the currency in the header, dates dd-MMM-yyyy, timestamps dd-MMM-yyyy HH:mm, fixed code columns | `Column.kind`, `formatDate`, `formatDateTime` | Unapplied payments, requests to Cashiering; every date on screen | `collections-unapplied`, `collections-unapplied-requests` | Done | ☐ |
| S9 | Sortable headers where the API sorts; pagination with "Showing x to y of n results" and Rows per Page | `DataTable` `sortKey` / `onSort`, `Pager` `onSize` | Available to every list; lists adopt it with their API sort | any list | Done (component) | ☐ |
| S10 | Skeleton rows while loading; empty state with the next action | `DataTable`, `EmptyState` | All tables | `notifications` | Done | ☐ |
| S11 | Totals row styling | `DataTable` `footer`, `.row-total` | Reports and statements with totals | `remittance-batch` | Done | ☐ |
| S12 | One cell = one primary value and at most one muted line; dash for empty; states as neutral pills | `CellStack`, `EmptyCell` | Unapplied payments, PR worklist | `collections-unapplied`, `collections-worklist` | Done | ☐ |
| S13 | Display names instead of login ids | `UserName`, `displayNameOf`, `/api/v1/users/directory` | Histories, headers, tables and summaries of every module | `client-record`, `collections-unapplied` | Done | ☐ |
| S14 | Record header: name, reference chip, status and labelled statuses, flags, completeness, key facts | `RecordHeader` | Client record (other records keep `RecordSummary` with the same chips and facts) | `client-record` | Done | ☐ |
| S15 | Page actions in one order; destructive action separated and last | `PageHeader` styles | Every record page | `client-record` | Done | ☐ |
| S16 | Blocking alert only | Client page | Client record | `client-record` | Done | ☐ |
| S17 | Tabs styled consistently, with counts and keyboard navigation | `Tabs` | Every tabbed page | `client-record`, `notifications` | Done | ☐ |
| S18 | LOV labels, never codes; Title Case labels | `LovSelect`, `humanize`, `titleCase` | PR worklist disposition, counters, stages | `collections-worklist`, `disbursement-workbench` | Done | ☐ |
| S19 | Forms: aligned labels, required markers, inline errors, 12-column grid, action bar bottom right | `Field`, `.form-grid`, `.form-actions` | New client, new quotation, e-policy upload, bulk uploads | `new-client`, `new-quotation`, `epolicy-upload` | Done | ☐ |
| S20 | Filter and parameter bars on one baseline, action aligned | `.filter-bar`, `.form-grid`, hints as tooltips | Remittance extraction, notifications, report parameters | `remittance-extraction`, `notifications` | Done | ☐ |
| S21 | Upload drop zone (drag and drop, Browse, types and size hint, file list with remove, progress) | `FileDropZone` | All 24 upload screens (bulk uploads, e-policy, statements, payment files, watchlists, SOA, attachments of KYC and cases) | `epolicy-upload`, `bulk-processing` | Done | ☐ |
| S22 | No internal references on screens (requirement ids, annex and question numbers, design notes) | `businessWording.test.ts`, `ReportCatalogueWordingIT`, `BusinessText`, `businessText` | Report Centre, Help Centre, subtitles, hints, empty states, errors, exported report headers | `report-centre` | Done | ☐ |
| S23 | No explanatory helper text under fields; short subtitles; short placeholders | `Field` (info tooltip), `PageHeader` | Every screen | `new-quotation`, `epolicy-upload` | Done | ☐ |
| S24 | Company selector shows the short name and code and truncates | Header | Every screen | any | Done | ☐ |
| S25 | Print in the BDO print layout | `patterns.css` `@media print` | Record pages and lists | print preview | Done | ☐ |
| S26 | Insurer suite hidden from BDOI users (menu, Help Centre, dashboard widgets) | `mayOpen` `requiresAll`, `showsInsurerWidgets` | Menu of every SIT/UAT persona; finance dashboard | `finance-dashboard` | Done | ☐ |

## 2. Behaviour

| # | Pattern | Shared component | Screens verified | Screenshot | Status | UAT |
|---|---|---|---|---|---|---|
| B1 | Notification panel grouped by day with icon, title, summary, reference chip and relative time; unread styling; mark one or all read; View All | `NotificationBell`, `NotificationItem` | Header of every screen | `notification-panel` | Done | ☐ |
| B2 | Notifications page with filters kept in the URL | `NotificationsPage` | `/notifications` | `notifications` | Done | ☐ |
| B3 | A notification opens its record (the link sent by the server) | `NotificationBell` | Header | `notification-panel` | Done | ☐ |
| B4 | Inbox and approvals badge counts from the same service as the lists | `HeaderTools` | Header | any | Done | ☐ |
| B5 | Field errors inline under the field, field outlined | `Field` | All forms | `new-client` | Done | ☐ |
| B6 | Form error banner listing the errors, each linked to its field | `ErrorAlert` | All forms | — | Done | ☐ |
| B7 | Business rule refusals: business message with error code and correlation id; no stack trace, raw JSON or HTTP code | `ErrorAlert`, `ApiError`, `GlobalExceptionHandler` | All screens | — | Done | ☐ |
| B8 | Success toast | `useToast` | All actions | — | Done | ☐ |
| B9 | Network and server errors: friendly banner with Retry | `ErrorAlert` `onRetry` | Client record, notifications (others show the banner) | — | Done | ☐ |
| B10 | Session expiry warning; signing in again returns to the same page | `SessionTimeoutGuard`, `RequireAuth`, `LoginPage` | All screens | — | Done | ☐ |
| B11 | Themed 403 and 404 pages with a way back | `StatusPage` | Any unknown address; screens without access | `page-not-found` | Done | ☐ |
| B12 | Uploads validated before commit with a summary (rows read, valid, rejected) and preview | `BulkUploadWizard` | Bulk centre and every bulk upload | `bulk-processing` | Done | ☐ |
| B13 | Download Error File (template layout, Error column, offending cells highlighted) | `BulkWorkbooks.errorFile`, `/bulk/jobs/{id}/error-file` | Every bulk upload | — | Done | ☐ |
| B14 | Processing rule stated (valid rows processed, rejected rows in the error file) | `BulkUploadWizard` | Every bulk upload | `bulk-processing` | Done | ☐ |
| B15 | Re-upload of the corrected file linked to the previous upload | `BulkUploadWizard` (Upload Corrected File) | Every bulk upload | — | Done | ☐ |
| B16 | Duplicate file detection message | `BulkService` (`BULK_DUPLICATE_FILE`) | Handlers that refuse duplicates | — | Done | ☐ |
| B17 | Upload progress | `FileDropZone` `busy` | Every upload | — | Done | ☐ |
| B18 | Download Template next to every bulk upload | `BulkUploadWizard` | Every bulk upload | `bulk-processing` | Done | ☐ |
| B19 | Themed confirmation naming the record and the effect, with a reason where needed; danger button for destructive actions | `ConfirmDialog`, `ActionDialog` | Workflow actions of every record, client deactivation, year-end close | — | Done | ☐ |
| B20 | Unsaved-changes guard | `useUnsavedChangesGuard` | New and edit client, new quotation | — | Done | ☐ |
| B21 | Buttons disabled while submitting (no double submit) | `Button` `busy` | All actions | — | Done | ☐ |
| B22 | Active filters as removable chips with Clear All; search kept in the URL | `FilterChips`, `WorklistToolbar` | Every work list with the toolbar; notifications | `notifications` | Done | ☐ |
| B23 | One date picker, dd-MMM-yyyy input and display | `DateInput`, `TypedInput` | Every date field (all modules) | `new-account` | Done | ☐ |
| B24 | Amounts with two decimals, negatives in brackets, currency code shown | `formatAmount`, `Amount` | All amounts | `collections-unapplied` | Done | ☐ |
| B25 | Breadcrumbs; back arrow returns to the list with its filters; browser tab title | `PageHeader` | Every screen | any | Done | ☐ |
| B26 | Keyboard use and focus: tabs with arrow keys, rows with Enter, visible focus rings | `Tabs`, `DataTable`, `base.css` | Every screen | — | Done | ☐ |
| B27 | AA contrast of pills, tags and text | tokens | Every screen | — | Done | ☐ |
| B28 | Layouts at 1366×768 and 1920×1080 without horizontal page scroll (wide tables scroll inside their card) | `patterns.css`, `.table-wrap` | Screenshots at 1600×1000; spot checks at 1366×768 | all | Done | ☐ |

## 3. Automated checks

- `frontend`: `npm run verify` (format, lint, type check, Vitest with coverage) runs the component tests of the patterns
  (`patterns.test.tsx`, `behaviour.test.tsx`, `FileDropZone.test.tsx`, `DateInput.test.tsx`, `businessText.test.ts`)
  and the wording check `businessWording.test.ts`.
- `backend`: `mvn verify` runs `BusinessTextTest`, `ReportCatalogueWordingIT`, `UserDirectoryIT` and the error file
  assertions of `BulkServiceIT`.
