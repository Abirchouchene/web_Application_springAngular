import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { NgApexchartsModule } from 'ng-apexcharts';
import { DashboardService, DashboardStats } from 'src/app/services/apps/dashboard.service';
import { AiChatComponent } from './ai-chat/ai-chat.component';
import { Subscription, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard1',
  standalone: true,
  imports: [
    CommonModule,
    MaterialModule,
    TablerIconsModule,
    NgApexchartsModule,
    AiChatComponent,
  ],
  templateUrl: './dashboard1.component.html',
  styleUrls: ['./dashboard1.component.scss'],
})
export class AppDashboard1Component implements OnInit, OnDestroy {
  stats: DashboardStats | null = null;
  isLoading = true;
  lastUpdated: Date | null = null;
  private pollSub: Subscription | null = null;
  private liveSub: Subscription | null = null;

  // Chart configs
  trendChart: any = {};
  resolutionTrendChart: any = {};
  statusChart: any = {};
  priorityChart: any = {};
  contactStatusChart: any = {};
  categoryChart: any = {};
  agentChart: any = {};
  hourlyChart: any = {};
  performanceChart: any = {};

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    // Initial load
    this.loadStats();

    // Real-time via WebSocket
    this.dashboardService.connectRealTime();
    this.liveSub = this.dashboardService.liveStats$.subscribe((data) => {
      if (data) {
        this.stats = data;
        this.buildCharts(data);
        this.lastUpdated = new Date();
      }
    });

    // Auto-refresh every 30 seconds as fallback
    this.pollSub = interval(30000)
      .pipe(switchMap(() => this.dashboardService.getStats()))
      .subscribe({
        next: (data) => {
          this.stats = data;
          this.buildCharts(data);
          this.lastUpdated = new Date();
        },
      });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.liveSub?.unsubscribe();
    this.dashboardService.disconnectRealTime();
  }

  loadStats(): void {
    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.buildCharts(data);
        this.isLoading = false;
        this.lastUpdated = new Date();
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  refreshDashboard(): void {
    this.isLoading = true;
    this.loadStats();
  }

  private buildCharts(s: DashboardStats): void {
    // 1. Dual trend chart (Created + Resolved)
    this.trendChart = {
      series: [
        { name: 'Créées', data: s.requestsTrend.map((p) => p.count) },
        { name: 'Résolues', data: s.resolutionTrend.map((p) => p.count) },
      ],
      chart: { type: 'area', height: 280, fontFamily: 'inherit', foreColor: '#adb0bb', toolbar: { show: false } },
      colors: ['#5d87ff', '#13deb9'],
      fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05 } },
      stroke: { curve: 'smooth', width: 2 },
      xaxis: { categories: s.requestsTrend.map((p) => p.date), labels: { style: { fontSize: '10px' }, rotate: -45 }, axisBorder: { show: false } },
      yaxis: { labels: { style: { fontSize: '11px' } } },
      dataLabels: { enabled: false },
      tooltip: { theme: 'dark' },
      legend: { position: 'top', fontSize: '12px' },
      grid: { borderColor: '#e7ecf0', strokeDashArray: 3 },
    };

    // 2. Status donut
    const statusLabels = Object.keys(s.requestsByStatus);
    const statusValues = Object.values(s.requestsByStatus);
    this.statusChart = {
      series: statusValues,
      chart: { type: 'donut', height: 260, fontFamily: 'inherit' },
      labels: statusLabels.map((l) => this.translateStatus(l)),
      colors: ['#ffb22b', '#5d87ff', '#fa896b', '#13deb9', '#49beff', '#6610f2', '#e7ecf0', '#539BFF'],
      plotOptions: { pie: { donut: { size: '70%', labels: { show: true, total: { show: true, label: 'Total', fontSize: '14px' } } } } },
      legend: { position: 'bottom', fontSize: '12px' },
      dataLabels: { enabled: false },
      tooltip: { theme: 'dark' },
    };

    // 3. Priority bar chart
    const prioLabels = Object.keys(s.requestsByPriority);
    const prioValues = Object.values(s.requestsByPriority);
    this.priorityChart = {
      series: [{ name: 'Demandes', data: prioValues }],
      chart: { type: 'bar', height: 260, fontFamily: 'inherit', foreColor: '#adb0bb', toolbar: { show: false } },
      colors: ['#13deb9', '#5d87ff', '#ffb22b', '#fa896b', '#e7168c'],
      plotOptions: { bar: { distributed: true, columnWidth: '45%', borderRadius: 5 } },
      xaxis: { categories: prioLabels },
      yaxis: { labels: { style: { fontSize: '11px' } } },
      dataLabels: { enabled: false },
      legend: { show: false },
      tooltip: { theme: 'dark' },
      grid: { borderColor: '#e7ecf0', strokeDashArray: 3 },
    };

    // 4. Contact status pie
    const csLabels = Object.keys(s.contactStatusDistribution || {});
    const csValues = Object.values(s.contactStatusDistribution || {});
    this.contactStatusChart = {
      series: csValues.length > 0 ? csValues : [1],
      chart: { type: 'pie', height: 260, fontFamily: 'inherit' },
      labels: csLabels.length > 0 ? csLabels.map((l) => this.translateContactStatus(l)) : ['Aucun'],
      colors: ['#e7ecf0', '#13deb9', '#fa896b', '#ffb22b', '#49beff', '#6610f2'],
      legend: { position: 'bottom', fontSize: '11px' },
      dataLabels: { enabled: true, style: { fontSize: '11px' } },
      tooltip: { theme: 'dark' },
    };

    // 5. Category bar chart
    const catLabels = Object.keys(s.requestsByCategory || {});
    const catValues = Object.values(s.requestsByCategory || {});
    this.categoryChart = {
      series: [{ name: 'Demandes', data: catValues }],
      chart: { type: 'bar', height: 260, fontFamily: 'inherit', foreColor: '#adb0bb', toolbar: { show: false } },
      colors: ['#5d87ff'],
      plotOptions: { bar: { horizontal: true, barHeight: '50%', borderRadius: 4 } },
      xaxis: { categories: catLabels },
      dataLabels: { enabled: false },
      tooltip: { theme: 'dark' },
      grid: { borderColor: '#e7ecf0', strokeDashArray: 3 },
    };

    // 6. Agent workload chart
    if (s.agentWorkload && s.agentWorkload.length > 0) {
      this.agentChart = {
        series: [
          { name: 'Assignées', data: s.agentWorkload.map((a) => a.assignedRequests) },
          { name: 'Résolues', data: s.agentWorkload.map((a) => a.resolvedRequests) },
        ],
        chart: { type: 'bar', height: 280, fontFamily: 'inherit', foreColor: '#adb0bb', toolbar: { show: false } },
        colors: ['#5d87ff', '#13deb9'],
        plotOptions: { bar: { columnWidth: '40%', borderRadius: 4 } },
        xaxis: { categories: s.agentWorkload.map((a) => a.agentName) },
        yaxis: { labels: { style: { fontSize: '11px' } } },
        dataLabels: { enabled: false },
        legend: { position: 'top', fontSize: '12px' },
        tooltip: { theme: 'dark' },
        grid: { borderColor: '#e7ecf0', strokeDashArray: 3 },
      };
    }

    // 7. Hourly activity heatmap bar
    if (s.hourlyActivity) {
      const hours = Object.keys(s.hourlyActivity).map(Number).sort((a, b) => a - b);
      this.hourlyChart = {
        series: [{ name: 'Activité', data: hours.map((h) => s.hourlyActivity[h]) }],
        chart: { type: 'bar', height: 200, fontFamily: 'inherit', foreColor: '#adb0bb', toolbar: { show: false }, sparkline: { enabled: false } },
        colors: ['#49beff'],
        plotOptions: { bar: { columnWidth: '60%', borderRadius: 3 } },
        xaxis: { categories: hours.map((h) => h + 'h'), labels: { style: { fontSize: '9px' } } },
        yaxis: { show: false },
        dataLabels: { enabled: false },
        tooltip: { theme: 'dark' },
        grid: { show: false },
      };
    }

    // 8. Performance radial
    if (s.performanceBreakdown) {
      this.performanceChart = {
        series: s.performanceBreakdown.map((m) => m.score),
        chart: { type: 'radialBar', height: 300, fontFamily: 'inherit' },
        labels: s.performanceBreakdown.map((m) => m.label),
        colors: s.performanceBreakdown.map((m) => m.color),
        plotOptions: {
          radialBar: {
            dataLabels: {
              name: { fontSize: '12px' },
              value: { fontSize: '16px', formatter: (val: number) => val + '%' },
              total: { show: true, label: 'Score', formatter: () => s.performanceScore + '' },
            },
            hollow: { size: '35%' },
            track: { background: '#f2f2f2' },
          },
        },
        legend: { show: true, position: 'bottom', fontSize: '12px' },
      };
    }
  }

  // ========= Translation helpers =========
  translateStatus(s: string): string {
    const map: { [key: string]: string } = {
      PENDING: 'En attente', APPROVED: 'Approuvée', REJECTED: 'Rejetée',
      IN_PROGRESS: 'En cours', ASSIGNED: 'Assignée', RESOLVED: 'Résolue',
      CLOSED: 'Fermée', AUTO_GENERATED: 'Auto-générée',
    };
    return map[s] || s;
  }

  translateContactStatus(s: string): string {
    const map: { [key: string]: string } = {
      NOT_CONTACTED: 'Non contacté', CONTACTED_AVAILABLE: 'Contacté - Disponible',
      CONTACTED_UNAVAILABLE: 'Contacté - Indisponible', NO_ANSWER: 'Pas de réponse',
      CALL_BACK_LATER: 'Rappeler', WRONG_NUMBER: 'Mauvais numéro',
    };
    return map[s] || s;
  }

  getActionIcon(action: string): string {
    const map: { [key: string]: string } = {
      REQUEST_CREATED: 'file-plus', STATUS_CHANGED: 'refresh',
      AGENT_ASSIGNED: 'user-check', PRIORITY_CHANGED: 'arrow-up', REQUEST_UPDATED: 'edit',
    };
    return map[action] || 'activity';
  }

  getActionColor(action: string): string {
    const map: { [key: string]: string } = {
      REQUEST_CREATED: 'primary', STATUS_CHANGED: 'accent',
      AGENT_ASSIGNED: 'warn', PRIORITY_CHANGED: 'warn', REQUEST_UPDATED: 'primary',
    };
    return map[action] || 'primary';
  }

  getSeverityIcon(sev: string): string {
    const map: { [key: string]: string } = { critical: 'alert-octagon', warning: 'alert-triangle', info: 'info-circle', success: 'circle-check' };
    return map[sev] || 'info-circle';
  }

  getSeverityColor(sev: string): string {
    const map: { [key: string]: string } = { critical: 'warn', warning: 'warning', info: 'accent', success: 'success' };
    return map[sev] || 'primary';
  }

  getAgentStatusColor(status: string): string {
    const map: { [key: string]: string } = { AVAILABLE: '#13deb9', BUSY: '#ffb22b', OVERLOADED: '#fa896b' };
    return map[status] || '#adb0bb';
  }

  getAgentStatusLabel(status: string): string {
    const map: { [key: string]: string } = { AVAILABLE: 'Disponible', BUSY: 'Occupé', OVERLOADED: 'Surchargé' };
    return map[status] || status;
  }
}
