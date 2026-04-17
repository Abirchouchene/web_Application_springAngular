import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { NgApexchartsModule } from 'ng-apexcharts';
import { UserService, KeycloakUser } from 'src/app/services/apps/user/user.service';
import { DashboardService, DashboardStats } from 'src/app/services/apps/dashboard.service';
import { AiChatService } from 'src/app/services/apps/ai-chat.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MaterialModule, TablerIconsModule, NgApexchartsModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
})
export class AdminDashboardComponent implements OnInit {
  isLoading = true;
  stats: DashboardStats | null = null;
  users: KeycloakUser[] = [];

  // KPIs
  totalUsers = 0;
  activeUsers = 0;
  roleCounts: { [key: string]: number } = {};

  // AI
  aiSummary = '';
  aiSecurityAlert = '';
  aiWorkforceInsight = '';
  aiLoading = false;

  // Charts
  roleChart: any = {};
  trendChart: any = {};

  constructor(
    private userService: UserService,
    private dashboardService: DashboardService,
    private aiService: AiChatService,
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.isLoading = true;

    this.userService.getAllUsers(0, 500).subscribe({
      next: (users) => {
        this.users = users;
        this.totalUsers = users.length;
        this.activeUsers = users.filter((u) => u.enabled).length;
        this.roleCounts = {};
        for (const u of users) {
          const role = u.role || 'UNDEFINED';
          this.roleCounts[role] = (this.roleCounts[role] || 0) + 1;
        }
        this.buildRoleChart();
      },
      error: () => {},
    });

    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.buildTrendChart(data);
        this.isLoading = false;
        this.loadAiInsights(data);
      },
      error: () => { this.isLoading = false; },
    });
  }

  private loadAiInsights(data: DashboardStats): void {
    this.aiLoading = true;

    // AI summary from existing insights
    if (data.aiInsights) {
      this.aiSummary = data.aiInsights;
    }

    // AI recommendations -> extract security and workforce
    if (data.aiRecommendations?.length) {
      const secRec = data.aiRecommendations.find((r) => r.severity === 'critical' || r.severity === 'warning');
      this.aiSecurityAlert = secRec ? secRec.title + ' — ' + secRec.message : 'Aucune alerte de sécurité détectée.';
      const wfRec = data.aiRecommendations.find((r) => r.severity === 'info' || r.severity === 'success');
      this.aiWorkforceInsight = wfRec ? wfRec.title + ' — ' + wfRec.message : '';
    }

    // Try getting a fresh AI analysis
    this.aiService.sendMessage('Donne un résumé court (3 phrases max) de l\'état du système admin: ' +
      this.totalUsers + ' utilisateurs, ' + this.activeUsers + ' actifs, ' +
      (data.totalRequests || 0) + ' demandes, taux résolution ' + (data.resolutionRate || 0) + '%, ' +
      (data.urgentOpenRequests || 0) + ' urgences, ' + (data.overdueRequests || 0) + ' en retard.'
    ).subscribe({
      next: (res) => {
        if (res?.message) this.aiSummary = res.message;
        this.aiLoading = false;
      },
      error: () => { this.aiLoading = false; },
    });
  }

  private buildRoleChart(): void {
    const labels = Object.keys(this.roleCounts).map((r) => this.getRoleLabel(r));
    const values = Object.values(this.roleCounts);
    this.roleChart = {
      series: values,
      chart: { type: 'donut', height: 220, fontFamily: 'inherit' },
      labels,
      colors: ['#5d87ff', '#ffb22b', '#13deb9', '#7b1fa2', '#adb0bb'],
      plotOptions: { pie: { donut: { size: '72%', labels: { show: true, total: { show: true, label: 'Total', fontSize: '13px' } } } } },
      legend: { position: 'bottom', fontSize: '11px' },
      dataLabels: { enabled: false },
      tooltip: { theme: 'dark' },
    };
  }

  private buildTrendChart(s: DashboardStats): void {
    this.trendChart = {
      series: [
        { name: 'Créées', data: s.requestsTrend?.map((p) => p.count) || [] },
        { name: 'Résolues', data: s.resolutionTrend?.map((p) => p.count) || [] },
      ],
      chart: { type: 'area', height: 200, fontFamily: 'inherit', foreColor: '#adb0bb', toolbar: { show: false }, sparkline: { enabled: false } },
      colors: ['#5d87ff', '#13deb9'],
      fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.3, opacityTo: 0.05 } },
      stroke: { curve: 'smooth', width: 2 },
      xaxis: { categories: s.requestsTrend?.map((p) => p.date) || [], labels: { show: false }, axisBorder: { show: false } },
      yaxis: { labels: { style: { fontSize: '10px' } } },
      dataLabels: { enabled: false },
      tooltip: { theme: 'dark' },
      legend: { show: false },
      grid: { borderColor: '#f0f0f0', strokeDashArray: 3 },
    };
  }

  getRoleLabel(role: string): string {
    const map: { [key: string]: string } = { ADMIN: 'Admin', MANAGER: 'Manager', AGENT: 'Agent', SURVEY_REQUESTER: 'Demandeur', UNDEFINED: 'Non défini' };
    return map[role] || role;
  }

  getSeverityIcon(sev: string): string {
    const m: { [k: string]: string } = { critical: 'alert-octagon', warning: 'alert-triangle', info: 'info-circle', success: 'circle-check' };
    return m[sev] || 'info-circle';
  }

  getSeverityColor(sev: string): string {
    const m: { [k: string]: string } = { critical: '#fa896b', warning: '#ffb22b', info: '#49beff', success: '#13deb9' };
    return m[sev] || '#5d87ff';
  }
}
