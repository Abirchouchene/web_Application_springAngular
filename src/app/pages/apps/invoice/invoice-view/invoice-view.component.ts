import { Component, signal } from '@angular/core';
import { InvoiceService } from 'src/app/services/apps/invoice/invoice.service';
import { InvoiceList } from '../invoice';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MaterialModule } from 'src/app/material.module';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TablerIconsModule } from 'angular-tabler-icons';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { AgentAvailabilityDTO } from 'src/app/models/AgentAvailabilityDTO';

@Component({
    selector: 'app-invoice-view',
    templateUrl: './invoice-view.component.html',
    imports: [
        MaterialModule,
        CommonModule,
        RouterLink,
        FormsModule,
        ReactiveFormsModule,
        TablerIconsModule,
    ]
})
export class AppInvoiceViewComponent {
  id = signal<number>(0);
  requestDetail = signal<any | null>(null);
  agents = signal<any[]>([]);
  selectedAgentId = signal<number | null>(null);
  availableAgents = signal<AgentAvailabilityDTO[]>([]);
  showAllAgents = signal<boolean>(false);


  constructor(
    private activatedRouter: ActivatedRoute,
    private requestService: RequestService,
  ) {}
  ngOnInit(): void {
    // Subscribe to the route params
    this.activatedRouter.params.subscribe((params) => {
      this.id.set(+params['id']); 
  
      this.loadRequestDetail();
      this.availableAgents();
    });
  }
  

  public loadRequestDetail(): void {
    const requestId = this.id();
    this.requestService.getRequestById(requestId).subscribe({
      next: (res) => this.requestDetail.set(res),
      error: (err) => console.error('Error fetching request', err),
    });
  }

   public loadAgents(): void {
    this.requestService.getAgents().subscribe({
      next: (res) => this.agents.set(res),
      error: (err) => console.error('Error fetching agents', err),
    });
  }

  assignAgent() {
    const agentId = this.selectedAgentId();
    const requestId = this.id();
    if (agentId) {
      this.requestService.assignAgentToRequest(requestId, agentId).subscribe({
        next: () => {
          alert('Agent assigned successfully!');
          this.loadRequestDetail(); // Refresh details if needed
        },
        error: (err) => console.error('Error assigning agent', err),
      });
    }
  }
   loadAvailableAgents(): void {
    this.requestService.getAvailableAgents()
      .subscribe({
        next: (agents) => {
          this.availableAgents.set(agents);
          this.agents.set(agents); // <-- update agents list as well
        },
        error: (err) => console.error('Error fetching agents', err)
      });
  }
  

  toggleAgentView() {
    this.showAllAgents.update(value => !value);  // Flip true/false
    if (this.showAllAgents()) {
      this.loadAgents();
    } else {
      this.loadAvailableAgents();
    }
  }
  
}