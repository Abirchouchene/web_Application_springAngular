import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';

export interface AgentWorkload {
  agentName: string;
  assignedRequests: number;
  resolvedRequests: number;
  contactRate: number;
  resolutionRate: number;
  status: string; // AVAILABLE, BUSY, OVERLOADED
}

export interface RecentActivity {
  action: string;
  description: string;
  timestamp: string;
}

export interface TimeSeriesPoint {
  date: string;
  count: number;
}

export interface AiRecommendation {
  icon: string;
  title: string;
  message: string;
  severity: string; // critical, warning, info, success
}

export interface PerformanceMetric {
  label: string;
  score: number;
  color: string;
}

export interface DashboardStats {
  totalRequests: number;
  pendingRequests: number;
  inProgressRequests: number;
  resolvedRequests: number;
  totalAgents: number;
  activeAgents: number;
  avgContactRate: number;

  // Advanced KPIs
  resolutionRate: number;
  slaComplianceRate: number;
  avgResolutionHours: number;
  urgentOpenRequests: number;
  overdueRequests: number;
  requestsToday: number;
  resolvedToday: number;
  weekOverWeekChange: number;

  requestsByStatus: { [key: string]: number };
  requestsByPriority: { [key: string]: number };
  requestsByType: { [key: string]: number };
  requestsByCategory: { [key: string]: number };
  contactStatusDistribution: { [key: string]: number };

  totalReports: number;
  pendingReports: number;
  approvedReports: number;

  agentWorkload: AgentWorkload[];
  recentActivity: RecentActivity[];
  aiInsights: string;
  aiRecommendations: AiRecommendation[];
  requestsTrend: TimeSeriesPoint[];
  resolutionTrend: TimeSeriesPoint[];
  hourlyActivity: { [key: number]: number };

  performanceScore: number;
  performanceBreakdown: PerformanceMetric[];
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService implements OnDestroy {
  private apiUrl = `${environment.apiUrl}/dashboard`;
  private rxStomp: RxStomp | null = null;
  private wsSub: Subscription | null = null;
  private _liveStats = new BehaviorSubject<DashboardStats | null>(null);
  public liveStats$ = this._liveStats.asObservable();

  constructor(private http: HttpClient) {}

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/stats`);
  }

  /** Connect to WebSocket for real-time dashboard updates */
  connectRealTime(): void {
    if (this.rxStomp) return;

    const config: RxStompConfig = {
      webSocketFactory: () => new SockJS(`${environment.gatewayUrl}/ws`),
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      reconnectDelay: 5000,
    };

    this.rxStomp = new RxStomp();
    this.rxStomp.configure(config);
    this.rxStomp.activate();

    this.wsSub = this.rxStomp
      .watch('/topic/dashboard')
      .subscribe((message) => {
        try {
          const stats: DashboardStats = JSON.parse(message.body);
          this._liveStats.next(stats);
        } catch (e) {
          console.error('Error parsing dashboard WS update:', e);
        }
      });
  }

  disconnectRealTime(): void {
    this.wsSub?.unsubscribe();
    this.wsSub = null;
    this.rxStomp?.deactivate();
    this.rxStomp = null;
  }

  ngOnDestroy(): void {
    this.disconnectRealTime();
  }
}
