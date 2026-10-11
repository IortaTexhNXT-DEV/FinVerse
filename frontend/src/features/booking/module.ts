import { BookCheck, FilePen, History, ReceiptText, Settings2, Upload } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const VIEW = 'BOOKING_PROCESS';
const ADJUST = ['BOOKING_ADJUST'];

/**
 * Booking: booked invoices, batches, endorsements, cancellations and service invoices (booking
 * module, docs/architecture/BROKING_ARCHITECTURE.md). Self-contained screens so the section can
 * later join Placement & Booking.
 */
export const bookingModule: FeatureModule = {
  id: 'booking',
  section: 'Booking',
  screens: [
    {
      path: '/booking',
      label: 'Booking Workbench',
      icon: BookCheck,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./BookingWorkbenchPage')),
    },
    {
      path: '/booking/book/:arn',
      label: 'Pre-booking Confirmation',
      icon: BookCheck,
      permission: VIEW,
      component: lazy(() => import('./BookingConfirmPage')),
      hidden: true,
    },
    {
      path: '/booking/invoices/:id',
      label: 'Booked Invoice',
      icon: ReceiptText,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./InvoiceDetailPage')),
      hidden: true,
    },
    {
      path: '/booking/invoices/no/:invoiceNo',
      label: 'Booked Invoice by Number',
      icon: ReceiptText,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./InvoiceByNoPage')),
      hidden: true,
    },
    {
      path: '/booking/endorsements',
      label: 'Endorsements',
      icon: FilePen,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./EndorsementsPage')),
    },
    {
      path: '/booking/endorsements/new',
      label: 'New Endorsement',
      icon: FilePen,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./EndorsementEntryPage')),
      hidden: true,
    },
    {
      path: '/booking/service-invoices',
      label: 'Service Invoices',
      icon: ReceiptText,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./ServiceInvoicesPage')),
    },
    {
      path: '/booking/service-invoices/:id',
      label: 'Service Invoice',
      icon: ReceiptText,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./ServiceInvoiceDetailPage')),
      hidden: true,
    },
    {
      path: '/booking/batch-runs',
      label: 'Batch Runs',
      icon: History,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./BatchRunsPage')),
    },
    {
      path: '/booking/batch-runs/:runNo',
      label: 'Batch Run',
      icon: History,
      permission: VIEW,
      alsoPermissions: ADJUST,
      component: lazy(() => import('./BatchRunDetailPage')),
      hidden: true,
    },
    {
      path: '/booking/setup',
      label: 'Booking Setup',
      icon: Settings2,
      permission: VIEW,
      alsoPermissions: ['BOOKING_ADJUST', 'MASTER_VIEW'],
      component: lazy(() => import('./BookingSetupPage')),
    },
    {
      // More specific than /bulk/:handler so the menu keeps the Booking section open.
      path: '/bulk/BOOKING_UPLOAD',
      label: 'Upload Bookings',
      icon: Upload,
      permission: 'BULK_PROCESS',
      component: lazy(() => import('./BookingUploadPage')),
      hidden: true,
    },
  ],
};
