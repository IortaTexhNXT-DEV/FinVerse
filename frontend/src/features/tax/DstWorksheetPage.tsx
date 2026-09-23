import { WorksheetView } from './WorksheetView';

/** Documentary stamp tax worksheet (BIR 2000, monthly). */
export default function DstWorksheetPage() {
  return (
    <WorksheetView
      kind="DST"
      title="Documentary Stamp Tax (2000)"
      description="DST on the policies and endorsements approved in the month, by line of business."
      granularity="MONTH"
    />
  );
}
