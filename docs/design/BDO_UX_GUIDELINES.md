# BDO Insure UX guidelines for iNXT BrokerVerse

Source: the BDOI UX Design pack (BDO Style Guide, prepared by ITG – User Experience Design, and the
"BDO Insure – Quotation & Renewal" screen designs, 2026). A copy is kept at
`docs/source-documents/BDOI_UXD.docx`.

This page is binding for every BrokerVerse screen. Components read the design tokens in
`frontend/src/styles/tokens.css`; do not hard-code colours, fonts or spacing in screens.

## 1. Brand

- **Product name.** Users see **BIBS – BDOI Broker System**, "Powered by iorta TechNXT". The platform is
  iNXT BrokerVerse (`frontend/src/branding.ts`).
- **Logo.** BDO Insure on white or Header Blue, with clear space of half the logo height and a minimum
  height of 24 px. Never redrawn, recoloured or deconstructed.
  - The sidebar, sign-in and About dialog use the lockup from the BDOI UX pack
    (`frontend/src/assets/brand/bdo-insure.png`). Replace it with the master file from BDO Marketing
    Communications, preferably SVG, once supplied.
  - The iorta TechNXT logo appears only as "Powered by" on sign-in and in About.
- **Colours.** Blue is always the dominant colour; yellow is used sparingly.

| Token | Hex | Style guide name | Use |
|---|---|---|---|
| `--brand-navy` | #004EA8 | Header Blue | Table headers, section bars, navigation text |
| `--brand-blue` | #0072D8 | CTA Blue | Primary buttons, links, icons, active states |
| `--brand-gold` | #FDB913 | Yellow | Accents: active tab top bar, highlights |
| `--brand-field-blue` | #99C1E7 | Text Field Blue | Input borders |
| `--brand-blue-050` | #E5F5FF | Background Blue | Alternate table rows, active menu item, info panels |
| `--color-heading` | #2E2E2E | Near Black | Headings and highlights (never pure black #000000) |
| `--color-text` | #4B4B4B | Dark Grey | Body text |
| `--color-text-muted` | #656565 | Base Grey | Support copy, sub-labels |
| `--color-placeholder` | #919191 | Medium Grey | Placeholder, disabled and inactive text |
| `--color-border-strong` | #C2C2C1 | Light Grey | Separators, disabled button fill |
| `--color-border` | #E4E4E4 | Dark Grey (elements) | Dividers and borders |
| `--color-bg` | #F6F6F6 | Dirty White | Page background behind white cards |

- **Contrast.** Never put dark text on a dark fill or light text on a light fill. This applies to charts too: labels on dark bars are white.

## 2. Typography

- **Fonts.** Nunito everywhere, bundled with the app (`@fontsource/nunito`). Arial is the approved fallback.
- **Headings.** Bold and Near Black.
  - The page title (h1) is 28/36.
  - Section titles are 18/28 and group titles 16/24.
  - The display sizes of the guide (72–24) are reserved for dashboards and landing panels.
- **Paragraphs.** Large 18/28 for leads, Medium 16/24 for content, Small 14/20 as the application default, and 12/16 for fine print and helper text.
- **Buttons.** Title Case: "Generate Quotation", "Send via Email", "Back to Homepage".

## 3. Spacing and layout

- **Grid.** An 8 px baseline grid; spacing tokens are `--space-*` (4, 8, 12, 16, 24, 32). Desktop web uses a 12-column grid.
- **Shell.** Screens follow the BDOI UX design:
  - a white sidebar with the logo on top and collapsible groups;
  - a white header with context selectors, Notifications (with a red count), and the user's name with the date and time;
  - page content on Dirty White with white cards (radius 12).
- **Navigation groups.** These follow the BDOI UX design (`frontend/src/navigation/modules.ts`):
  - Dashboard and My Work (always open);
  - **Client & Policy**: Client Management, Sanction Screening, Quotation / Proposal, Accounts, Non-Package Management, Employee Benefits, Placement & Booking, Renewal, Submitted Policies, Product Reconciliation, Adjustment, Customer Service Facility, Product Maintenance, Bulk Processing;
  - **Finance**: Collections, the operations cash modules (cashiering, remittance, commission), Disbursement, Refund & Cash Advance Requests, ACSL and Accounting Reports, then the general ledger, receivables, payables, assets, planning, tax and accounting engine;
  - **Claims & Insurance**: Claims Handling (broking claims, listed first), then the insurer-side modules;
  - **Reports**;
  - **Setup & Administration**: includes Compliance Setup (Sanction Screening configuration and watchlist), User Access (access requests, including portal users, and the User Access Matrix, before Administration) and Portal Users (Employee Benefits portal).

  The sections of the later BRDs (Employee Benefits, Renewal, Submitted Policies, Customer Service Facility, Claims Handling, Portal Users) are designed, not built; Sanction Screening, Compliance Setup and User Access are registered with their landing screens (foundation S0 / U0), Compliance Setup after Broking Setup. Their places follow the designs: Sanction Screening after Client Management, Employee Benefits after Non-Package Management, Renewal right after Placement & Booking, Submitted Policies after Renewal and before Product Reconciliation, and Customer Service Facility after Renewal.

  The group holding the current page opens automatically.

## 4. Components

| Component | Rule |
|---|---|
| Action button | Height 44–48 px, radius 8 px, padding 16 px outside and 8 px between icon and label. Primary = CTA Blue filled; secondary = white with a CTA Blue outline; disabled = Light Grey fill with white text. The size does not change with the breakpoint |
| Link | Large 18 px or small 16 px in CTA Blue, with an optional 24 px leading icon and 8 px gap |
| Input text | Label above (14 px, Near Black) and a 44 px field with a Text Field Blue border, radius 8. Placeholder is Medium Grey and short ("Search client code or name"). Disabled and read-only fields are Dirty White with a Light Grey border. Only a short format hint goes under the field ("dd-MMM-yyyy", "Max 10 MB, PDF"); other guidance is an info icon tooltip on the label (section 9). The error text is red under the field and the border turns red |
| Dropdown | Opens under the field. Shows 5 options (188 px) and scrolls beyond 5 (200 px) |
| Form builder set | Input text, text area, dropdown, date picker (`DateInput`: typed and shown as dd-MMM-yyyy, with a calendar icon; BIBS uses dd-MMM-yyyy instead of the pack's MM/DD/YYYY, section 8), radio button, checkbox, select-search combo box, password, readable fields, upload file (`FileDropZone`) |
| Table | Header row in Header Blue with white bold text and sort chevrons, and alternating white / Background Blue rows. A checkbox column for bulk actions. "Showing 1 to n of N results" with numbered pages on the right (the Renewal Expiry List asks for one scrollable view instead: UX-6) |
| Flag chip | Record flags such as FFY and Direct Payment go in their own column as small gold-tinted chips (`Tag`), never mixed with the status pill. One height (22 px), one font, a minimum width of 88 px, centred text; never wraps |
| Status pill | One per record in the Status column. Outlined and tinted, radius 8: green = Clean / Approved, yellow = Review / Pending, red = Exception / Rejected, blue = in-process (e.g. "Account for Placement"), grey = draft or inactive. One size everywhere: 24 px high, 12 px bold, centred, 112–176 px wide; in a table cell every pill of the column takes the column width. Long labels use the agreed short form with the full label in the tooltip (section 8.2); a pill never wraps |
| Tabs | Boxed tabs; the active tab is white with a 4 px yellow top bar and CTA Blue text, inactive tabs have a light blue gradient |
| Empty state | Table-and-magnifier illustration with "No items to display" (`EmptyState`) |
| Modal | Title with a close ✕, body, and a footer with Cancel (secondary) and the action (primary) on the right |
| Icons | BDO outline icons in CTA Blue at 16 / 24 / 32 / 48 dp with the same stroke weight. The Lucide outline set is used as the equivalent |

## 5. Screen patterns (from the BDOI UX design)

- **Work lists.** Examples are Renewal and Placement & Booking.
  - Page title, then a card with status tabs (e.g. For Placement & Booking | Booked Account; Unassigned Disposition | For Renewal | For Quotation | Not for Renewal).
  - Toolbar: search box ("Search Proposal No."), Search button, Filters link.
  - Bulk actions on the right, enabled by row selection (For Booking, For Placement, Assign Disposition, Re-assign Officer, Generate Expiry List).
  - Then the table.
- **Record details.** The example is Client Record Details.
  - A back arrow with the module breadcrumb above the title, and page actions on the right (Send to TSU, Generate Quotation).
  - A summary card: code, name, record status pill and risk rating, then icon + label/value pairs (basic details, address, mobile, e-mail) with an Update link and "View All Details".
  - Then tabs for the related records (Quotation | Confirmed Proposals | Renewal).
- **Dialogs** for a single parameterised action, e.g. Generate Expiry List with a date range.
- **Sign in.** A split screen: the photo from the UX pack on the left, and on the right the BDO Insure logo, "Welcome to BIBS" with "BDOI Broker System" below, User ID and Password, a full-width Login button, and "Powered by iorta TechNXT".
  - Windows ID login is required (BRD-1 Q42, answered by BRD-11). It is built as the parked port `DirectoryAuthenticator` (USER_ACCESS_DESIGN section 10); the User ID and Password sign-in stays until BDO supplies the EUA interface (UX-4).
  - Confirm with BDOI that the photo is licensed for the application.

## 6. Observations to confirm with BDOI

| # | Observation | Current BrokerVerse design |
|---|---|---|
| UX-1 | The BDOI UX design's proposal number is `MI-06192026-001` (line prefix + MMDDYYYY + sequence) and its client code is `CC01234567890` | ARN-yyyy-nnnnnn (BRNB.102), PRF-/QS-/PS- per document, client code CL-yyyy-nnnnnn. Numbering formats are configurable, so BDOI to confirm the format per document |
| UX-2 | Renewal screens (disposition assignment, expiry list, Clean / Review / Exception classification) | **Closed by BRD-6 (Renewal).** The Expiry List with Generate Expiry List, Assign Disposition, Re-assign Officer and the Classification pill Clean / Review / Exception are in the BRD; the BDOI UX design tabs are kept, with For Proposal / Lost Business / Exceptions added (RENEWAL_DESIGN section 12) |
| UX-3 | Customer Service Facility and Employee Benefits menu entries | **Closed by BRD-8 (EB) and BRD-9 (CSF).** Sections Employee Benefits and Customer Service Facility in Client & Policy (section 3; EMPLOYEE_BENEFITS_DESIGN section 10, CUSTOMER_SERVICING_DESIGN section 10) |
| UX-4 | Windows ID sign-in | **Closed by BRD-11 (User Access, Q42).** Directory sign-in (BDO EUA / Windows ID, LDAP / AD / SSO) is required. It is built as a parked port; the User ID and Password sign-in stays until BDO supplies the interface (UQ04) |
| UX-5 | "Send to TSU" directly from the client record | BrokerVerse routes to TSU from the PRF / quotation (BRNB.098 rules). To confirm whether a client-level TSU request is needed |
| UX-6 | Renewal lists: "no pagination, one scrollable view" (BRRN.004 / 011) against the table rule "Showing 1 to n of N results" with numbered pages (section 4) | Open (Renewal RQ05). The Renewal design uses a new virtualised grid (`GridTable`, keyset chunks, server-side filters) with a row count and no pager for its Expiry List grid; `DataTable` and its pager are unchanged for every other list. BDOI to confirm |

## 7. Shared components that implement the patterns

Use these instead of building a screen-specific variant.

| Pattern | Component |
|---|---|
| Work list toolbar: search box with Search, Filters toggle, bulk actions on the right | `components/broking/WorklistToolbar` (CSS `.worklist-toolbar`, filters panel `.worklist-filters`) |
| Status tabs of a work list card | `Tabs` as the first child of a `Card flush` (tabs stand on the card's separator line) |
| Checkbox column with a select-all box in the header | `components/broking/rowSelection` (`selectionColumn`) |
| "Showing 1 to n of N results" with numbered pages | `components/ui/Pager` (`Pager`, `PageFooter` for a paged API response) |
| Record summary card: name, reference chip, status pill, flags, key facts with icons | `components/broking/RecordSummary` (`flags` renders `.tag` chips apart from the pill) |
| Status pill colours | `StatusBadge`: green done / approved, yellow waiting for review or approval, blue (`info`) in process, red exception, grey draft or inactive |
| Record flags (FFY, Direct Payment, Information Incomplete, TSU) | `<span className="tag">` inside `.tag-list`, never a status pill |
| Empty list | `EmptyState` (the `DataTable` default message is "No items to display") |
| Buttons | one filled primary (`accent`) per area; secondary actions outlined; destructive actions (`danger`) outlined red; labels in Title Case |
| Workflow status and history | `components/broking/WorkflowPanel` (status strip: Stage, In Stage Since, Due, Assigned To; actions on the right) and `HistoryTable` (the history as a table) |
| Key-value detail blocks | `components/ui/DefinitionGrid` (aligned label / value rows, dash for empty, "Not provided" collapse); existing `.detail-list` blocks share the look |
| Record page header | `components/broking/RecordHeader` (name, reference chips, status and labelled statuses, flags, completeness, key facts) |
| Table cell with a secondary line | `components/ui/CellStack` (`EmptyCell` for the muted dash) |
| Flag and label chips | `components/ui/Tag` |
| User names | `components/ui/UserName` (display name, login id in the tooltip; `displayNameOf` for texts) |
| File upload | `components/ui/FileDropZone` |
| Date entry | `components/ui/DateInput` (`TypedInput` in form wrappers that take the input type) |
| Confirmation of a destructive action | `components/ui/ConfirmDialog`; workflow actions use `ActionDialog` (record and effect named, danger button for cancel / void / reverse / deactivate / delete / reject) |
| Unsaved changes | `useUnsavedChangesGuard(dirty)` |
| Active filters | `components/ui/FilterChips`; `WorklistToolbar` keeps the search in the URL (`?q=`) |
| Errors | `components/ui/ErrorAlert` (business message, field list linked to the fields, reference = error code and correlation id, Retry) |
| 403 and 404 | `components/ui/StatusPage` |
| Notifications | `NotificationBell` panel and the `/notifications` page |

## 8. Presentation rules (client feedback, 26-Sep-2026)

### 8.1 Structured data is never free text

- **Histories are tables.** Workflow and status histories use `HistoryTable`: Stage (status pill), From Stage, Action, By (display name and role), Date and Time, Remarks, Duration in Stage. Header Blue header row, zebra rows, right-aligned dates in one format, newest first with a sort toggle on the date column.
- The same applies to audit trails and change logs (from and to values in their own columns), e-mail and notification logs, status histories, remarks and comment lists and activity feeds: a table when rows repeat.
- **No "Since … by …" lines.** The current stage is a labelled status strip (`WorkflowPanel`): Stage, In Stage Since, Due, Assigned To.
- **Key-value blocks** use `DefinitionGrid` or `.detail-list`: one label width (168 px), ruled rows, a muted dash for an empty value. Full-width sections use two label / value pairs per row (four columns).

### 8.2 Status pills and tags

- One height, one font size and weight, centred text, a fixed minimum width; in a table cell the pill takes the column width so every pill of a column is the same size. A pill never wraps.
- Agreed short forms (full label in the tooltip): Returned to Mktg (Returned to Marketing), Insurer Returned (Returned by Insurer), Accepted as Req. (Accepted as Requested), Appr. with Changes (Approved with Changes), Pending Auth. (Pending Authorization), Plcmt Cancelled (Placement Cancelled), Excl. Cancelled (Excluded Cancelled), Reqts Prep (Requirements Prep), Unit Head Appr. (Unit Head Approval), For Mktg Approval (For MKT Approval), For Placement (Ready for Placement), Skipped (Skipped Locked). Other labels longer than 18 characters are cut with an ellipsis and keep the tooltip.
- One colour tone per state group (section 4). Two statuses of one record (e.g. Unapplied and a Queued request) are two columns, or two aligned pills (`.badge-pair`), never a word above a pill.
- Flags (FFY, Direct Payment, Information Incomplete) are `Tag` chips in their own column or in the record header's flag list.

### 8.3 Tables

- Sticky Header Blue header row; the table scrolls inside its card (70% of the window height).
- Alignment by column kind (`Column.kind`): text left; amounts right with thousand separators and two decimals, the currency code in the header ("Paid (PHP)"); dates dd-MMM-yyyy and timestamps dd-MMM-yyyy HH:mm (Philippine time); codes and references (ARN, CL, PAY… numbers) in a fixed 168 px column that never wraps, with the copy button aligned; status pills centred.
- Every cell has one primary value and at most one muted secondary line under it (`CellStack`). Empty values are a single muted dash; a state such as "Not Matched" is a neutral pill, never prose ("None yet").
- Users are shown by display name, never by login id (`UserName`).
- Sortable headers (`sortKey` with `sort` / `onSort`) where the API sorts; the pagination bar shows "Showing x to y of n results", numbered pages and, where the API takes a page size, Rows per Page.
- Loading shows skeleton rows; an empty list shows the illustration with the message and, where there is one, the next action. Row hover and the selected row are highlighted; clickable rows open with Enter. Totals rows use the gold-ruled totals style (`footer`).

### 8.4 Record pages

- One header layout: back arrow and breadcrumb, title, then `RecordHeader` (name, reference chips, status pill and labelled statuses such as KYC, flag tags, completeness indicator, key facts in four columns). The page actions are on the right in one order: secondary, primary, then the destructive action separated and last (enforced by the page header styles).
- An alert banner only for a blocking issue (e.g. "Complete Birth Date, TIN before submitting the KYC").
- The workflow is the status strip and the history table (section 8.1), never a free-text card.
- Detail sections are cards in one grid (`.detail-grid`) with aligned label / value rows; a section with nothing filled shows "Not provided", a mostly empty one shows its filled fields with "Show All Fields".
- Tabs are boxed with optional counts (`Tabs` `count`) and move with the arrow keys, Home and End.
- LOV labels, never codes; Title Case labels.

### 8.5 Forms, filter bars and uploads

- Labels above the fields on one baseline, inputs of equal height on one baseline, fields on the 12-column grid (`.form-grid`), required markers, inline errors under the field, and the action bar at the bottom right (`.form-actions`: Cancel, then the primary action).
- Filter, parameter and search bars share one layout (`.filter-bar`, `.form-grid`): no hints pushing fields out of line; the action button aligned to the input line at the right end.
- Uploads use `FileDropZone` (drag and drop plus Browse, the accepted types and maximum size as one hint, the files listed with size and a remove icon, a progress bar). The upload button sits in the action bar; disabled buttons use the theme's disabled style.

## 9. Business-user wording

- **No internal references on screens.** Requirement and traceability ids (BRD ids such as ADJID.021 or BRNB.110, "Annex II #14", open-question numbers such as OQ42), and design notes ("layout to confirm", "draft", "parked", "to be confirmed by BDOI") never appear in what a user sees: report catalogue descriptions, subtitles, help text, tooltips, error messages, empty states and exported report headers. They stay in code comments, hidden `requirementRefs` metadata and the documents.
  - Enforced by `frontend/src/test/businessWording.test.ts` (every user-facing string of `frontend/src`) and the backend `ReportCatalogueWordingIT` (report titles and descriptions); `BusinessText` (server) and `businessText` (web) remove any reference left in report texts and error messages.
- **Report cards** show the title, the report code and a one-line business description.
- **No explanatory helper text under fields.** Only short format hints stay; other guidance is an info icon tooltip on the label (`Field` does this for every `hint`) or goes to the Help Centre.
- **Page subtitles** are at most one short business line (cut with an ellipsis and the full text in the tooltip), or none.
- **Placeholders** are short ("Search client code or name").
- **Header context**: the company selector shows the company short name and code and truncates gracefully.

## 10. Behaviour patterns

| Pattern | Rule | Shared implementation |
|---|---|---|
| Notifications | The bell opens a panel grouped by day (Today, Yesterday, date); each item has an icon, title, one-line summary, record reference chip and relative time; unread items are highlighted with Mark Read; Mark All Read; View All opens `/notifications` with filters kept in the URL | `NotificationBell`, `NotificationsPage`, `notificationLogic` |
| Field errors | Inline under the field in the error colour, field outlined | `Field` `error`, `aria-invalid` |
| Form errors | Banner at the top listing the errors, each linked to its field | `ErrorAlert` |
| Business rule refusals | Business message plus a reference for support (error code and correlation id); no stack trace, raw JSON or HTTP code | `ErrorAlert`, `ApiError`, server `GlobalExceptionHandler` (`correlationId`) |
| Success | Toast | `useToast` |
| Network and server errors | Friendly banner with Retry | `ErrorAlert onRetry` |
| Session expiry | Warning before the time-out; signing in again returns to the same page | `SessionTimeoutGuard`, `RequireAuth` (remembers the page) |
| 403 and 404 | Themed page with Go Back and Back to Homepage | `StatusPage` |
| Uploads | Validate before commit, summary (rows read, valid, rejected) and a preview of the rows; Download Error File (the template layout with an Error column and the offending cells highlighted); the rule stated on screen (valid rows are processed, rejected rows are returned in the error file); Upload Corrected File keeps the link to the previous upload; duplicate files refused with the earlier upload named; progress; Download Template next to the upload | `BulkUploadWizard`, `FileDropZone`, `/bulk/jobs/{id}/error-file` |
| Confirmations | Themed dialog naming the record and the effect, with a reason where the process needs one; destructive actions confirm with the danger button | `ConfirmDialog`, `ActionDialog` |
| Unsaved changes | Leaving a changed form asks first | `useUnsavedChangesGuard` |
| Loading and empty | Skeletons for tables, cards and panels; empty states with the next action; buttons disabled while submitting | `DataTable`, `EmptyState`, `Button busy` |
| Search and filters | Active filters as removable chips with Clear All; the search kept in the URL | `FilterChips`, `WorklistToolbar` |
| Dates, numbers, currency | One date picker, dd-MMM-yyyy input and display; amounts with two decimals and negatives in brackets; the currency code in the header or a column | `DateInput`, `formatAmount`, `Amount` |
| Navigation | Breadcrumb above the title; the back arrow returns to the list with its filters; the browser tab shows the screen title | `PageHeader` |
| Accessibility | Keyboard use of tabs (arrow keys) and tables (Enter on a row), visible focus rings, AA contrast of every pill and tag tone | `Tabs`, `DataTable`, tokens |
| Print | Record pages and lists print in the BDO print layout (A4 landscape, Header Blue table heads, no navigation or buttons) | `patterns.css` `@media print` |

