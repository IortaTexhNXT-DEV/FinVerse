import { WorksheetView } from './WorksheetView';

/** VAT worksheet (BIR 2550Q) with the Summary Lists of Sales and Purchases. */
export default function VatWorksheetPage() {
  return (
    <WorksheetView
      kind="VAT"
      title="VAT Worksheet (2550Q)"
      description="Output VAT on premiums, input VAT on supplier invoices, carry-over and VAT payable. SLS / SLP CSV per quarter."
      granularity="QUARTER"
    />
  );
}
