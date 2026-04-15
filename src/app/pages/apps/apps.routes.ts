import { Routes } from '@angular/router';
import { AuthGuard } from '../../guard/auth.guard';

import { AppChatComponent } from './chat/chat.component';
import { AppEmailComponent } from './email/email.component';
import { DetailComponent } from './email/detail/detail.component';
import { AppCoursesComponent } from './courses/courses.component';
import { AppCourseDetailComponent } from './courses/course-detail/course-detail.component';
import { AppEmployeeComponent } from './employee/employee.component';
import { AppBlogsComponent } from './blogs/blogs.component';
import { AppBlogDetailsComponent } from './blogs/details/details.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { ContactManagementComponent } from './contact/contact-management/contact-management.component';
import { AppNotesComponent } from './notes/notes.component';
import { AppTodoComponent } from './todo/todo.component';
import { AppPermissionComponent } from './permission/permission.component';
import { AppKanbanComponent } from './kanban/kanban.component';
import { AppFullcalendarComponent } from './fullcalendar/fullcalendar.component';
import { AppInvoiceListComponent } from './invoice/invoice-list/invoice-list.component';
import { AppAddRequestComponent } from './invoice/add-invoice/add-request.component';
import { AppInvoiceViewComponent } from './invoice/invoice-view/invoice-view.component';
import { AppEditInvoiceComponent } from './invoice/edit-invoice/edit-invoice.component';
/*import { AppContactListComponent } from './contact-list/contact-list.component';*/
import { AppTicketlistComponent } from './Requests/TicketList/tickets.component';
import { TicketdetailsComponent } from './Requests/TicketDetails/ticketdetails.component';
import { RequestManagerViewComponent } from './RequestManager/request-manager-view/request-manager-view.component';
import { RequestManagerListComponent } from './RequestManager/request-manager-view/request-manager-list/request-manager-list.component';
import { CallbacksComponent } from './Callbacks/callbacks.component';
import { ReportListComponent } from './Reports/report-list/report-list.component';
import { ReportDetailsComponent } from './Reports/report-details/report-details.component';
import { NotificationsComponent } from './Notifications/notifications.component';
import { LogsComponent } from './Logs/logs.component';
import { CallsComponent } from './Calls/calls.component';


export const AppsRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: 'chat',
        component: AppChatComponent,
        data: {
          title: 'Chat',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Chat' },
          ],
        },
      },
      {
        path: 'calendar',
        component: AppFullcalendarComponent,
        data: {
          title: 'Calendar',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Calendar' },
          ],
        },
      },
      {
        path: 'notes',
        component: AppNotesComponent,
        data: {
          title: 'Notes',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Notes' },
          ],
        },
      },
      { path: 'email', redirectTo: 'email/inbox', pathMatch: 'full' },
      {
        path: 'email/:type',
        component: AppEmailComponent,
        data: {
          title: 'Email',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Email' },
          ],
        },
        children: [
          {
            path: ':id',
            component: DetailComponent,
            data: {
              title: 'Email Detail',
              urls: [
                { title: 'Dashboard', url: '/dashboards/dashboard1' },
                { title: 'Email Detail' },
              ],
            },
          },
        ],
      },
      {
        path: 'permission',
        component: AppPermissionComponent,
        data: {
          title: 'Roll Base Access',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Roll Base Access' },
          ],
        },
      },
      {
        path: 'todo',
        component: AppTodoComponent,
        data: {
          title: 'Todo App',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Todo App' },
          ],
        },
      },
      {
        path: 'kanban',
        component: AppKanbanComponent,
        data: {
          title: 'Kanban',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Kanban' },
          ],
        },
      },
      
      {
        path: 'tickets',
        component: AppTicketlistComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Tickets',
          roles: ['AGENT', 'MANAGER', 'ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Tickets' },
          ],
        },
      },
      {
        path: 'ticket/:id',
        component: TicketdetailsComponent,
        data: {
          title: 'Ticket Details',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Tickets', url: '/apps/tickets' },
            { title: 'Ticket Details' },
          ],
        },
      },
      {
        path: 'callbacks',
        component: CallbacksComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Callbacks',
          roles: ['AGENT', 'MANAGER', 'ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Callbacks' },
          ],
        },
      },
      {
        path: 'notifications',
        component: NotificationsComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Notifications',
          roles: ['AGENT', 'MANAGER', 'ADMIN', 'SURVEY_REQUESTER'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Notifications' },
          ],
        },
      },
      {
        path: 'contacts',
        component: ContactManagementComponent,
        data: {
          title: 'Gestion des Contacts',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Gestion des Contacts' },
          ],
        },
      },
      {
        path: 'courses',
        component: AppCoursesComponent,
        data: {
          title: 'Courses',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Courses' },
          ],
        },
      },
      /*{
        path: 'contact-list',
        component: AppContactListComponent,
        data: {
          title: 'Contact List',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Contact List' },
          ],
        },
      },*/
      {
        path: 'courses/coursesdetail/:id',
        component: AppCourseDetailComponent,
        data: {
          title: 'Course Detail',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Course Detail' },
          ],
        },
      },
      {
        path: 'blog/post',
        component: AppBlogsComponent,
        data: {
          title: 'Posts',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Posts' },
          ],
        },
      },
      {
        path: 'blog/detail/:id',
        component: AppBlogDetailsComponent,
        data: {
          title: 'Blog Detail',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Blog Detail' },
          ],
        },
      },
      {
        path: 'employee',
        component: AppEmployeeComponent,
        data: {
          title: 'Employee',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Employee' },
          ],
        },
      },
      {
        path: 'request-manager',
        component: RequestManagerListComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Request Manager',
          roles: ['MANAGER', 'ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Request Manager' },
          ],
        },
      },
      {
        path: 'request-manager/viewRequest/:id',
        component: RequestManagerViewComponent,
        data: {
          title: 'View Request',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Request Manager', url: '/apps/request-manager' },
            { title: 'View Request' },
          ],
        },
      },
      {
        path: 'invoice',
        component: AppInvoiceListComponent,
        data: {
          title: 'Request List',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Request List' },
          ],
        },
      },
      {
        path: 'addInvoice',
        component: AppAddRequestComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Ajouter une Demande',
          roles: ['SURVEY_REQUESTER', 'MANAGER', 'ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Ajouter une Demande' },
          ],
        },
      },
      {
        path: 'viewInvoice/:id',
        component: AppInvoiceViewComponent,
        data: {
          title: 'View Invoice',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'View Invoice' },
          ],
        },
      },
      {
        path: 'editinvoice/:id',
        component: AppEditInvoiceComponent,
        data: {
          title: 'Edit Invoice',
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Edit Invoice' },
          ],
        },
      },
      {
        path: 'reports',
        canActivate: [AuthGuard],
        data: { roles: ['AGENT', 'MANAGER', 'ADMIN', 'SURVEY_REQUESTER'] },
        children: [
          {
            path: 'list',
            component: ReportListComponent,
            data: {
              title: 'Report List',
              urls: [
                { title: 'Dashboard', url: '/dashboards/dashboard1' },
                { title: 'Reports', url: '/apps/reports/list' },
                { title: 'Report List' },
              ],
            },
          },
          {
            path: 'details/:id',
            component: ReportDetailsComponent,
            data: {
              title: 'Report Details',
              urls: [
                { title: 'Dashboard', url: '/dashboards/dashboard1' },
                { title: 'Reports', url: '/apps/reports/list' },
                { title: 'Report Details' },
              ],
            },
          },
        ],
      },
      {
        path: 'logs',
        component: LogsComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Journal d\'Activités',
          roles: ['AGENT', 'MANAGER', 'ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Journal d\'Activités' },
          ],
        },
      },
      {
        path: 'calls',
        component: CallsComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Gestion des Appels',
          roles: ['AGENT', 'MANAGER', 'ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Gestion des Appels' },
          ],
        },
      },
      {
        path: 'user-management',
        component: UserManagementComponent,
        canActivate: [AuthGuard],
        data: {
          title: 'Gestion des Utilisateurs',
          roles: ['ADMIN'],
          urls: [
            { title: 'Dashboard', url: '/dashboards/dashboard1' },
            { title: 'Gestion des Utilisateurs' },
          ],
        },
      },
    ],
  },
];
