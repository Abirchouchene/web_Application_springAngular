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
    route: '/dashboards/dashboard1',
    roles: ['ADMIN', 'MANAGER', 'AGENT', 'SURVEY_REQUESTER'],
  },

  // ===================== AGENT =====================
  {
    navCap: 'Espace Agent',
    roles: ['AGENT'],
  },
  {
    displayName: 'Enquêtes Assignées',
    iconName: 'file-invoice',
    bgcolor: 'primary',
    route: '/apps/invoice',
    roles: ['AGENT'],
  },
  {
    displayName: 'Tickets',
    iconName: 'ticket',
    bgcolor: 'error',
    route: 'apps/tickets',
    roles: ['AGENT'],
  },
  {
    displayName: 'Contacts',
    iconName: 'phone',
    bgcolor: 'success',
    route: 'apps/contacts',
    roles: ['AGENT'],
  },
  {
    displayName: 'Rappels',
    iconName: 'phone-calling',
    bgcolor: 'secondary',
    route: 'apps/callbacks',
    roles: ['AGENT'],
  },
  {
    displayName: 'Gestion des Appels',
    iconName: 'headset',
    bgcolor: 'accent',
    route: 'apps/calls',
    roles: ['AGENT'],
  },
  {
    displayName: 'Rapports',
    iconName: 'file-report',
    bgcolor: 'info',
    route: 'apps/reports',
    roles: ['AGENT'],
    children: [
      {
        displayName: 'Liste des Rapports',
        iconName: 'point',
        bgcolor: 'tranparent',
        route: 'apps/reports/list',
      },
    ],
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
    navCap: 'Gestion',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Demandes',
    iconName: 'file-invoice',
    bgcolor: 'primary',
    route: '',
    roles: ['MANAGER'],
    children: [
      {
        displayName: 'Toutes les Demandes',
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
    displayName: 'Gestion des Demandes',
    iconName: 'brand-ctemplar',
    bgcolor: 'warning',
    route: 'apps/request-manager',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Tickets',
    iconName: 'ticket',
    bgcolor: 'error',
    route: 'apps/tickets',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Contacts',
    iconName: 'phone',
    bgcolor: 'success',
    route: 'apps/contacts',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Rappels',
    iconName: 'phone-calling',
    bgcolor: 'secondary',
    route: 'apps/callbacks',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Gestion des Appels',
    iconName: 'headset',
    bgcolor: 'accent',
    route: 'apps/calls',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Rapports',
    iconName: 'file-report',
    bgcolor: 'info',
    route: 'apps/reports',
    roles: ['MANAGER'],
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
    navCap: 'Audit',
    roles: ['MANAGER'],
  },
  {
    displayName: 'Journal d\'Activités',
    iconName: 'history',
    bgcolor: 'accent',
    route: 'apps/logs',
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
    route: '',
    roles: ['ADMIN'],
    children: [
      {
        displayName: 'Toutes les Demandes',
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
    displayName: 'Gestion des Appels',
    iconName: 'headset',
    bgcolor: 'accent',
    route: 'apps/calls',
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

