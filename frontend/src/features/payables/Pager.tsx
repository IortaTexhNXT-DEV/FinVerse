import type { PageResponse } from '@/api/types';
import { PageFooter } from '@/components/ui/Pager';

interface PagerProps {
  data: PageResponse<unknown> | undefined;
  noun: string;
  onPage: (page: number) => void;
}

/** Pager of the payables lists: the shared BDO pager ("Showing 1 to n of N results"). */
export function Pager({ data, noun, onPage }: Readonly<PagerProps>) {
  return <PageFooter data={data} noun={noun} onPage={onPage} />;
}
