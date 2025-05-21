import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { AgentAvailabilityDTO } from 'src/app/models/AgentAvailabilityDTO';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { signal } from '@angular/core'; // Or another correct import for signals



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
    agents = signal<AgentAvailabilityDTO[]>([]); // Directly using your backend DTO
    selectedAgentId = signal<number | null>(null);
    selectedDate = signal<string>(new Date().toISOString().split('T')[0]); // Default to today's date
  
    constructor(
      private activatedRouter: ActivatedRoute,
      private requestService: RequestService,
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
      console.log('Fetching agents for date:', date);
      this.requestService.getAvailableAgents(date).subscribe({
        next: (res: AgentAvailabilityDTO[]) => {
          console.log('Fetched agents:', res);  // Log the response to see if availability is correct
          this.agents.set(res);
        },
        error: (err) => console.error('Error fetching available agents', err),
      });
    }
    
    
  
    onDateChange(event: Event): void {
      const input = event.target as HTMLInputElement;
      const newDate = input.value;
      this.selectedDate.set(newDate);  // Update the signal value
      this.loadAgentAvailability(newDate);  // Fetch agent availability for the selected date
    }
    
    
    
    assignAgent(): void {
      const agentId = this.selectedAgentId();
      if (agentId) {
        this.requestService.assignAgentToRequest(this.id(), agentId).subscribe({
          next: () => {
            alert('Agent assigned successfully!');
            this.loadRequestDetail();
          },
          error: (err) => console.error('Error assigning agent', err),
        });
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
      return isAvailable ? 'Yes' : 'No';
    }
    
  }