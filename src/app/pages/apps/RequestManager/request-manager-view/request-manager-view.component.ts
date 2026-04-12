import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { AgentAvailabilityDTO } from 'src/app/models/AgentAvailabilityDTO';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';



@Component({
  selector: 'app-request-manager-view',
  standalone: true,
  imports: [
    MaterialModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule
  ],
  templateUrl: './request-manager-view.component.html',
  styleUrl: './request-manager-view.component.scss'
})
  export class RequestManagerViewComponent implements OnInit {
    id = signal<number>(0);
    requestDetail = signal<any | null>(null);
    agents = signal<AgentAvailabilityDTO[]>([]);
    selectedAgentId = signal<number | null>(null);
    selectedDate = signal<string>(new Date().toISOString().split('T')[0]);
  
    constructor(
      private activatedRouter: ActivatedRoute,
      private requestService: RequestService,
      private snackBar: MatSnackBar,
      private router: Router,
    ) {}
  
    ngOnInit(): void {
      this.activatedRouter.params.subscribe((params) => {
        this.id.set(+params['id']);
        this.loadRequestDetail();
        this.loadAgentAvailability(this.selectedDate());
      });
    }
  
    loadRequestDetail(): void {
      this.requestService.getRequestById(this.id()).subscribe({
        next: (res) => this.requestDetail.set(res),
        error: (err) => console.error('Error fetching request', err),
      });
    }
  
    loadAgentAvailability(date?: string): void {
      this.requestService.getAvailableAgents(date).subscribe({
        next: (res: AgentAvailabilityDTO[]) => this.agents.set(res),
        error: (err) => console.error('Error fetching available agents', err),
      });
    }
  
    onDateChange(event: Event): void {
      const input = event.target as HTMLInputElement;
      this.selectedDate.set(input.value);
      this.loadAgentAvailability(input.value);
    }
    
    assignAgent(): void {
      const agentId = this.selectedAgentId();
      if (agentId) {
        this.requestService.assignAgentToRequest(this.id(), agentId).subscribe({
          next: () => {
            this.snackBar.open('Agent assigné avec succès!', 'OK', { duration: 3000 });
            this.loadRequestDetail();
          },
          error: (err) => console.error('Error assigning agent', err),
        });
      }
    }

    approveRequest(): void {
      this.requestService.approveRequest(this.id(), 'APPROVED').subscribe({
        next: () => {
          this.snackBar.open('Demande approuvée avec succès', 'OK', { duration: 3000 });
          this.loadRequestDetail();
        },
        error: (err) => {
          console.error('Error approving request:', err);
          this.snackBar.open('Erreur lors de l\'approbation', 'OK', { duration: 3000 });
        }
      });
    }

    rejectRequest(): void {
      this.requestService.approveRequest(this.id(), 'REJECTED').subscribe({
        next: () => {
          this.snackBar.open('Demande rejetée', 'OK', { duration: 3000 });
          this.loadRequestDetail();
        },
        error: (err) => {
          console.error('Error rejecting request:', err);
          this.snackBar.open('Erreur lors du rejet', 'OK', { duration: 3000 });
        }
      });
    }

    getStatusClass(status: string): string {
      switch (status) {
        case 'PENDING': return 'bg-warning text-dark';
        case 'APPROVED': return 'bg-success text-white';
        case 'REJECTED': return 'bg-danger text-white';
        case 'IN_PROGRESS': return 'bg-info text-white';
        case 'ASSIGNED': return 'bg-primary text-white';
        case 'RESOLVED': return 'bg-success text-white';
        default: return 'bg-light text-dark';
      }
    }
  
    formatFullDate(date: any): string {
      let parsedDate: Date;
      if (Array.isArray(date) && date.length >= 6) {
        const [year, month, day, hour, minute, second] = date;
        parsedDate = new Date(year, month - 1, day, hour, minute, second);
      } else if (typeof date === 'string' || typeof date === 'number') {
        parsedDate = new Date(date);
      } else if (date instanceof Date) {
        parsedDate = date;
      } else {
        return 'Invalid date';
      }
  
      if (isNaN(parsedDate.getTime())) return 'Invalid date';
  
      const dayStr = parsedDate.getDate().toString().padStart(2, '0');
      const monthStr = (parsedDate.getMonth() + 1).toString().padStart(2, '0');
      const year = parsedDate.getFullYear();
      const hourStr = parsedDate.getHours().toString().padStart(2, '0');
      const minuteStr = parsedDate.getMinutes().toString().padStart(2, '0');
  
      return `${dayStr}/${monthStr}/${year} ${hourStr}:${minuteStr}`;
    }
  
    getAvailabilityLabel(isAvailable: boolean): string {
      return isAvailable ? 'Oui' : 'Non';
    }
  }