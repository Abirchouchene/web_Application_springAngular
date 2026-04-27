import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import {
  NgApexchartsModule,
  ApexChart, ApexNonAxisChartSeries, ApexAxisChartSeries,
  ApexXAxis, ApexStroke, ApexFill, ApexDataLabels, ApexGrid, ApexTooltip,
} from 'ng-apexcharts';
import { forkJoin, Subscription, interval, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { RoleService } from 'src/app/services/role.service';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { CallbackService } from 'src/app/services/apps/callback.service';
import { NotificationService } from 'src/app/services/apps/notification.service';
import { DashboardService, DashboardStats } from 'src/app/services/apps/dashboard.service';
import { Callback } from 'src/app/models/Callback';
import { Notification } from 'src/app/models/Notification';

export interface AgentTask {
  title: string;
  subtitle: string;
  priority: 'Urgent' | 'Important' | 'Normal';
  time: string;
  actionIcon: string;
  actionRoute: string;
}

export type SparkOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  stroke: ApexStroke;
  fill: ApexFill;
  colors: string[];
  dataLabels: ApexDataLabels;
  grid: ApexGrid;
  xaxis: ApexXAxis;
  tooltip: ApexTooltip;
};

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, TablerIconsModule, NgApexchartsModule],
  templateUrl: './agent-dashboard.component.html',
  styleUrls: ['./agent-dashboard.component.scss'],
})
export class AgentDashboardComponent implements OnInit, OnDestroy {

  // Identity
  agentId: number | null = null;
  agentName = '';
  agentInitials = '';
  currentDate = new Date();

  // KPIs
  contactRate = 0;
  contactRateTrend = 0;
  inProgress = 0;
  urgentCount = 0;
  resolvedToday = 0;
  totalAssigned = 0;
  pendingCallbacks = 0;

  // IA Banner data
  iaInsights: { icon: string; color: string; bg: string; title: string; detail: string }[] = [];
  aiChatExample = 'Montre-moi les demandes à risque';

  // Tasks
  priorityTasks: AgentTask[] = [];

  // AI recommendations
  aiRecs: any[] = [];

  // Recent activity
  recentActivity: any[] = [];

  // Notifications (unread count)
  unreadNotifications = 0;

  // Sparklines (one per KPI card)
  sparkContact!: Partial<SparkOptions>;
  sparkProgress!: Partial<SparkOptions>;
  sparkUrgent!: Partial<SparkOptions>;
  sparkResolved!: Partial<SparkOptions>;

  isLoading = true;
  aiQuestion = '';

  private refreshSub: Subscription | null = null;

  readonly priorityColors: Record<string, string> = {
    HIGH: '#ef4444', MEDIUM: '#f59e0b', LOW: '#10b981', CRITICAL: '#7c3aed',
  };

  constructor(
    private roleService: RoleService,
    private requestService: RequestService,
    private callbackService: CallbackService,
    private notificationService: NotificationService,
    private dashboardService: DashboardService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.roleService.ensureLoaded().then(info => {
      this.agentId = info.id;
      this.agentName = info.fullName || info.username;
      this.agentInitials = this.buildInitials(this.agentName);
      this.loadAll();
      this.refreshSub = interval(60000).subscribe(() => this.loadAll());
    });
  }

  ngOnDestroy(): void { this.refreshSub?.unsubscribe(); }

  loadAll(): void {
    const agentId = this.agentId;
    if (!agentId) return;
    forkJoin({
      requests:      this.requestService.getAssignedRequests(agentId).pipe(catchError(() => of<any[]>([]))),
      callbacks:     this.callbackService.getUpcomingCallbacks(agentId).pipe(catchError(() => of<any[]>([]))),
      notifications: this.notificationService.getNotifications(agentId).pipe(catchError(() => of<any[]>([]))),
      stats:         this.dashboardService.getStats().pipe(catchError(() => of(null))),
    }).subscribe(({ requests, callbacks, notifications, stats }) => {
      this.processRequests(requests, callbacks);
      this.processNotifications(notifications);
      this.processStats(stats, requests);
      this.buildSparks(stats);
      this.isLoading = false;
    });
  }

  private processRequests(requests: any[], callbacks: Callback[]): void {
    this.totalAssigned  = requests.length;
    this.inProgress     = requests.filter(r => r.status === 'IN_PROGRESS').length;
    this.resolvedToday  = requests.filter(r =>
      (r.status === 'RESOLVED' || r.status === 'CLOSED') &&
      new Date(r.updatedAt).toDateString() === new Date().toDateString()
    ).length;
    this.urgentCount    = requests.filter(r =>
      r.priority === 'HIGH' || r.priority === 'CRITICAL'
    ).length;
    this.pendingCallbacks = callbacks.filter(c => (c.status as string) === 'SCHEDULED').length;

    // Build task list: high-priority requests + callbacks + report placeholder
    const tasks: AgentTask[] = [];

    requests
      .filter(r => r.status !== 'RESOLVED' && r.status !== 'CLOSED')
      .sort((a, b) => {
        const order: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
        return (order[a.priority] ?? 4) - (order[b.priority] ?? 4);
      })
      .slice(0, 4)
      .forEach(r => {
        tasks.push({
          title:       r.title,
          subtitle:    r.description ? r.description.substring(0, 50) + '...' : (r.categoryRequest || r.requestType || ''),
          priority:    r.priority === 'HIGH' || r.priority === 'CRITICAL' ? 'Urgent'
                     : r.priority === 'MEDIUM' ? 'Important' : 'Normal',
          time:        r.deadline ? new Date(r.deadline).toLocaleTimeString('fr-FR', { hour:'2-digit', minute:'2-digit' }) : '--:--',
          actionIcon:  'phone',
          actionRoute: '/apps/calls',
        });
      });

    // Add upcoming callbacks as tasks
    callbacks.filter(c => (c.status as string) === 'SCHEDULED').slice(0, 2).forEach(cb => {
      tasks.push({
        title:       'Rappel automatique',
        subtitle:    cb.notes || 'Contact à relancer',
        priority:    'Normal',
        time:        new Date(cb.scheduledDate).toLocaleTimeString('fr-FR', { hour:'2-digit', minute:'2-digit' }),
        actionIcon:  'refresh',
        actionRoute: '/apps/callbacks',
      });
    });

    // Report task
    if (tasks.length < 6) {
      tasks.push({
        title:       'Rapport quotidien IA',
        subtitle:    'Génération automatique',
        priority:    'Normal',
        time:        '--:--',
        actionIcon:  'sparkles',
        actionRoute: '/apps/reports/list',
      });
    }

    this.priorityTasks = tasks.slice(0, 6);
  }

  private processNotifications(notifications: Notification[]): void {
    this.unreadNotifications = notifications.filter(n => !n.isRead).length;
  }

  private processStats(stats: DashboardStats | null, requests: any[]): void {
    if (!stats) {
      this.buildDefaultIA();
      return;
    }

    const myWorkload = stats.agentWorkload?.find(
      w => w.agentName?.toLowerCase() === this.agentName?.toLowerCase()
    );
    this.contactRate      = Math.round(myWorkload?.contactRate ?? stats.avgContactRate ?? 0);
    this.contactRateTrend = Math.round(stats.weekOverWeekChange ?? 0);
    this.resolvedToday    = stats.resolvedToday ?? this.resolvedToday;
    this.aiRecs           = (stats.aiRecommendations || []).slice(0, 4);
    this.recentActivity   = (stats.recentActivity || []).slice(0, 5);

    // Build IA banner insights from stats + requests
    this.iaInsights = [];
    if (this.urgentCount > 0) {
      this.iaInsights.push({
        icon: 'alert-circle', color: '#ef4444', bg: '#fef2f2',
        title: `${this.urgentCount} demande${this.urgentCount > 1 ? 's' : ''} urgente${this.urgentCount > 1 ? 's' : ''}`,
        detail: 'Nécessitent votre attention immédiate',
      });
    }
    if (this.contactRate < 60) {
      this.iaInsights.push({
        icon: 'trending-down', color: '#f59e0b', bg: '#fffbeb',
        title: 'Taux de contact faible',
        detail: `${this.contactRate}% aujourd'hui · Essayez de relancer vos contacts`,
      });
    } else {
      this.iaInsights.push({
        icon: 'trending-up', color: '#10b981', bg: '#f0fdf4',
        title: 'Bon taux de contact',
        detail: `${this.contactRate}% aujourd'hui · Continuez comme ça !`,
      });
    }
    if (this.resolvedToday > 0) {
      this.iaInsights.push({
        icon: 'circle-check', color: '#10b981', bg: '#f0fdf4',
        title: `${this.resolvedToday} résolue${this.resolvedToday > 1 ? 's' : ''} aujourd'hui`,
        detail: 'Belle progression ! 🎉',
      });
    } else {
      this.iaInsights.push({
        icon: 'clock', color: '#3b82f6', bg: '#eff6ff',
        title: `${this.inProgress} en cours de traitement`,
        detail: 'Finalisez vos demandes actives',
      });
    }

    if (stats.aiInsights) {
      this.aiChatExample = stats.aiInsights.substring(0, 60).split('.')[0];
    }
  }

  private buildDefaultIA(): void {
    this.iaInsights = [
      { icon: 'clipboard-list', color: '#667eea', bg: '#f0f4ff',
        title: `${this.totalAssigned} demandes assignées`, detail: 'Gérez vos demandes actives' },
      { icon: 'phone', color: '#3b82f6', bg: '#eff6ff',
        title: `${this.pendingCallbacks} rappels planifiés`, detail: 'Consultez votre planning' },
      { icon: 'chart-bar', color: '#10b981', bg: '#f0fdf4',
        title: 'Tableau de bord actif', detail: 'Toutes vos données en temps réel' },
    ];
  }

  private buildSparks(stats: DashboardStats | null): void {
    const trend = stats?.requestsTrend?.slice(-7).map(p => p.count) || [3,5,4,6,5,7,6];
    const mk = (data: number[], color: string): Partial<SparkOptions> => ({
      series: [{ data }],
      chart: { type: 'area', height: 45, sparkline: { enabled: true }, toolbar: { show: false } },
      stroke: { curve: 'smooth', width: 2 },
      fill: { type: 'gradient', gradient: {
        shadeIntensity: 1, opacityFrom: 0.3, opacityTo: 0,
        colorStops: [{ offset: 0, color, opacity: 0.3 }, { offset: 100, color, opacity: 0 }],
      }},
      colors: [color],
      dataLabels: { enabled: false },
      grid: { show: false },
      xaxis: { labels: { show: false }, axisBorder: { show: false }, axisTicks: { show: false } },
      tooltip: { enabled: false },
    });

    this.sparkContact  = mk([48,52,55,51,56,54,this.contactRate], '#667eea');
    this.sparkProgress = mk([...trend], '#f59e0b');
    this.sparkUrgent   = mk([2,4,3,5,4,6,this.urgentCount], '#ef4444');
    this.sparkResolved = mk([...trend].reverse(), '#10b981');
  }

  getActivityIcon(action: string): string {
    if (!action) return 'activity';
    const a = action.toLowerCase();
    if (a.includes('call') || a.includes('appel')) return 'phone';
    if (a.includes('email')) return 'mail';
    if (a.includes('status') || a.includes('statut')) return 'refresh';
    if (a.includes('note')) return 'pencil';
    if (a.includes('assign') || a.includes('assigné')) return 'user-check';
    return 'activity';
  }

  getActivityColor(action: string): string {
    if (!action) return '#667eea';
    const a = action.toLowerCase();
    if (a.includes('call')) return '#3b82f6';
    if (a.includes('email')) return '#f59e0b';
    if (a.includes('resolve') || a.includes('résol')) return '#10b981';
    return '#667eea';
  }

  getSeverityColor(s: string): string {
    return s === 'critical' ? '#ef4444' : s === 'warning' ? '#f59e0b' : s === 'success' ? '#10b981' : '#3b82f6';
  }
  getSeverityBg(s: string): string {
    return s === 'critical' ? '#fef2f2' : s === 'warning' ? '#fffbeb' : s === 'success' ? '#f0fdf4' : '#eff6ff';
  }
  getSeverityIcon(s: string): string {
    return s === 'critical' ? 'alert-circle' : s === 'warning' ? 'alert-triangle' : s === 'success' ? 'circle-check' : 'info-circle';
  }

  navigate(path: string): void { this.router.navigate([path]); }

  askAI(): void { this.navigate('/apps/calls'); }

  private buildInitials(name: string): string {
    if (!name) return 'A';
    const parts = name.trim().split(/\s+/);
    return parts.length >= 2
      ? (parts[0][0] + parts[1][0]).toUpperCase()
      : name.substring(0, 2).toUpperCase();
  }
}
