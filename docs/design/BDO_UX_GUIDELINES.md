# BDO Insure UX guidelines for iNXT BrokerVerse

Source: the BDOI UX Design pack (BDO Style Guide, prepared by ITG – User Experience Design, and the
"BDO Insure – Quotation & Renewal" prototype screens, 2026). A copy is kept at
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
- **Shell.** Screens follow the prototype:
  - a white sidebar with the logo on top and collapsible groups;
  - a white header with context selectors, Notifications (with a red count), and the user's name with the date and time;
  - page content on Dirty White with white cards (radius 12).
- **Navigation groups.** These follow the BDOI prototype (`frontend/src/navigation/modules.ts`):
  - Dashboard and My Work (always open);
  - **Client & Policy**: Client Management, Quotation / Proposal, Accounts, Non-Package Management, Placement & Booking, Product Reconciliation, Adjustment, Renewal (later BRD), Customer Service Facility (later BRD), Product Maintenance, Bulk Processing;
  - **Finance**: Collections, the operations cash modules (cashiering, remittance, commission), Disbursement, Refund & Cash Advance Requests, ACSL and Accounting Reports, then the general ledger, receivables, payables, assets, planning, tax and accounting engine;
  - **Claims & Insurance**, **Reports**, **Setup & Administration**.

  The group holding the current page opens automatically.

## 4. Components

| Component | Rule |
|---|---|
| Action button | Height 44–48 px, radius 8 px, padding 16 px outside and 8 px between icon and label. Primary = CTA Blue filled; secondary = white with a CTA Blue outline; disabled = Light Grey fill with white text. The size does not change with the breakpoint |
| Link | Large 18 px or small 16 px in CTA Blue, with an optional 24 px leading icon and 8 px gap |
| Input text | Label above (14 px, Near Black) and a 44 px field with a Text Field Blue border, radius 8. Placeholder is Medium Grey. Disabled and read-only fields are Dirty White with a Light Grey border. Helper text goes under the field; the error text is red, above the helper text, and the border turns red |
| Dropdown | Opens under the field. Shows 5 options (188 px) and scrolls beyond 5 (200 px) |
| Form builder set | Input text, text area, dropdown, date picker (range picker with a calendar icon, MM/DD/YYYY), radio button, checkbox, select-search combo box, password, readable fields, upload file |
| Table | Header row in Header Blue with white bold text and sort chevrons, and alternating white / Background Blue rows. A checkbox column for bulk actions. "Showing 1 to n of N results" with numbered pages on the right |
| Flag chip | Record flags such as FFY and Direct Payment go in their own column as small gold-tinted chips, never mixed with the status pill |
| Status pill | One per record in the Status column. Outlined, radius 8: green = Clean / Approved, yellow = Review / Pending, red = Exception / Rejected, blue = in-process (e.g. "Account for Placement") |
| Tabs | Boxed tabs; the active tab is white with a 4 px yellow top bar and CTA Blue text, inactive tabs have a light blue gradient |
| Empty state | Table-and-magnifier illustration with "No items to display" (`EmptyState`) |
| Modal | Title with a close ✕, body, and a footer with Cancel (secondary) and the action (primary) on the right |
| Icons | BDO outline icons in CTA Blue at 16 / 24 / 32 / 48 dp with the same stroke weight. The Lucide outline set is used as the equivalent |

## 5. Screen patterns (from the prototype)

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
  - Windows ID login needs BDO SSO / Active Directory (BRD-1 Q42, parked).
  - Confirm with BDOI that the photo is licensed for the application.

## 6. Observations to confirm with BDOI

| # | Observation | Current BrokerVerse design |
|---|---|---|
| UX-1 | The prototype's proposal number is `MI-06192026-001` (line prefix + MMDDYYYY + sequence) and its client code is `CC01234567890` | ARN-yyyy-nnnnnn (BRNB.102), PRF-/QS-/PS- per document, client code CL-yyyy-nnnnnn. Numbering formats are configurable, so BDOI to confirm the format per document |
| UX-2 | Renewal screens (disposition assignment, expiry list, Clean / Review / Exception classification) | Renewal is outside BRD-1 (OOS-1); it waits for its BRD |
| UX-3 | Customer Service Facility and Employee Benefits menu entries | Not in BRD-1/2; they wait for their BRDs |
| UX-4 | Windows ID sign-in | BDO SSO / AD parked (Q42) |
| UX-5 | "Send to TSU" directly from the client record | BrokerVerse routes to TSU from the PRF / quotation (BRNB.098 rules). To confirm whether a client-level TSU request is needed |

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
