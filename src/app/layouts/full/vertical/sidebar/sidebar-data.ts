import { NavItem } from './nav-item/nav-item';

export const navItems: NavItem[] = [
  // ===================== ACCUEIL (tous) =====================
  {
    navCap: 'Accueil',
  },
  {
    displayName: 'Tableau de bord',
    iconName: 'layout-dashboard',
    bgcolor: 'primary',
    route: '/dashboards/admin',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Tableau de bord',
    iconName: 'layout-dashboard',
    bgcolor: 'primary',
    route: '/dashboards/dashboard1',
    roles: ['MANAGER', 'AGENT', 'SURVEY_REQUESTER'],
  },

  // ===================== AGENT =====================
  {
    navCap: 'CALL CENTER MODULES',
    roles: ['AGENT'],
  },
  {
    displayName: 'Appels à traiter',
    iconName: 'headset',
    bgcolor: 'primary',
    route: 'apps/calls',
    roles: ['AGENT'],
  },
  {
    displayName: 'Rappels',
    iconName: 'phone-calling',
    bgcolor: 'warning',
    route: 'apps/callbacks',
    roles: ['AGENT'],
  },
  {
    displayName: 'Journalisation',
    iconName: 'history',
    bgcolor: 'accent',
    route: 'apps/logs',
    roles: ['AGENT'],
  },
  {
    displayName: 'Mes Rapports Générés',
    iconName: 'file-report',
    bgcolor: 'success',
    route: 'apps/reports/list',
    roles: ['AGENT'],
  },

  // ===================== DEMANDEUR =====================
  {
    navCap: 'Mes Demandes',
    roles: ['SURVEY_REQUESTER'],
  },
  {
    displayName: 'Demandes',
    iconName: 'file-invoice',
    bgcolor: 'primary',
    route: '',
    roles: ['SURVEY_REQUESTER'],
    children: [
      {
        displayName: 'Mes Demandes',
        iconName: 'point',
        bgcolor: 'tranparent',
        route: '/apps/invoice',
      },
      {
        displayName: 'Créer une Demande',
        iconName: 'point',
        bgcolor: 'tranparent',
        route: '/apps/addInvoice',
      },
    ],
  },
  {
    displayName: 'Contacts',
    iconName: 'phone',
    bgcolor: 'success',
    route: '',
    roles: ['SURVEY_REQUESTER'],
    children: [
      {
        displayName: 'Gestion des Contacts',
        iconName: 'point',
        bgcolor: 'tranparent',
        route: 'apps/contacts',
      },
    ],
  },
  {
    displayName: 'Rapports Reçus',
    iconName: 'file-report',
    bgcolor: 'info',
    route: 'apps/reports/list',
    roles: ['SURVEY_REQUESTER'],
  },

  // ===================== MANAGER =====================
  {
    navCap: 'CALL CENTER MODULES',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Rapports',
    iconName: 'file-report',
    bgcolor: 'info',
    route: 'apps/reports/list',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Demandes du Manager',
    iconName: 'file-invoice',
    bgcolor: 'warning',
    route: 'apps/request-manager',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Journalisation',
    iconName: 'history',
    bgcolor: 'accent',
    route: 'apps/logs',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Mes Rapports Générés',
    iconName: 'report-analytics',
    bgcolor: 'success',
    route: 'apps/reports/list',
    roles: ['MANAGER'],
  },

  // ===================== ADMIN =====================
  {
    navCap: 'Centre d\'appels',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Demandes',
    iconName: 'file-invoice',
    bgcolor: 'primary',
    route: '/apps/invoice',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Gestion des Demandes',
    iconName: 'brand-ctemplar',
    bgcolor: 'warning',
    route: 'apps/request-manager',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Tickets',
    iconName: 'ticket',
    bgcolor: 'error',
    route: 'apps/tickets',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Contacts',
    iconName: 'phone',
    bgcolor: 'success',
    route: 'apps/contacts',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Rappels',
    iconName: 'phone-calling',
    bgcolor: 'secondary',
    route: 'apps/callbacks',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Rapports',
    iconName: 'file-report',
    bgcolor: 'info',
    route: 'apps/reports',
    roles: ['ADMIN'],
    children: [
      {
        displayName: 'Liste des Rapports',
        iconName: 'point',
        bgcolor: 'tranparent',
        route: 'apps/reports/list',
      },
    ],
  },
  {
    navCap: 'Administration',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Utilisateurs',
    iconName: 'users',
    bgcolor: 'accent',
    route: 'apps/user-management',
    roles: ['ADMIN'],
  },
  {
    displayName: 'Journal d\'Activités',
    iconName: 'history',
    bgcolor: 'info',
    route: 'apps/logs',
    roles: ['ADMIN'],
  },

  // ===================== PARAMETRES (tous) =====================
  {
    navCap: 'Paramètres',
  },
  {
    displayName: 'Mon Compte',
    iconName: 'user-circle',
    bgcolor: 'warning',
    route: 'theme-pages/account-setting',
    roles: ['ADMIN', 'MANAGER', 'AGENT', 'SURVEY_REQUESTER'],
  },
];

