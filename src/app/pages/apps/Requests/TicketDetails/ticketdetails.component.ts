import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TablerIconsModule } from 'angular-tabler-icons';
import { MaterialModule } from 'src/app/material.module';
import { Request } from 'src/app/models/Request';
import { RequestType } from 'src/app/models/RequestType';
import { RequestService } from 'src/app/services/apps/ticket/ticket.service';

@Component({
  selector: 'app-requestdetails',
  templateUrl: './ticketdetails.component.html',
  styleUrls: ['./ticketdetails.component.scss'],
  imports: [
    MaterialModule,
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule,
]
})
export class TicketdetailsComponent implements OnInit {
  requestId: number = 0;
  requestData: Request = {} as Request;
  status: string = '';
  note: string = '';
  isLoading: boolean = false;
  requestTypeEnum = RequestType;
  
  constructor(
    private route: ActivatedRoute,
    private requestService: RequestService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.requestId = +params['id'];
      this.getRequestDetails();
    });
  }

  getRequestDetails(): void {
    this.requestService.getRequestById(this.requestId).subscribe({
      next: (data) => {
        this.requestData = data;
        this.status = data.status?.label || '';
        this.note = data.note || '';
      },
      error: (err) => {
        console.error('Failed to fetch request:', err);
      }
    });
  }

  updateRequest(): void {
    if (!this.requestData || !this.status.trim()) {
      alert('Please select a valid status before updating!');
      return;
    }

    this.isLoading = true;

    this.requestService.updateRequestStatus(this.requestData.idR, this.status).subscribe({
      next: () => {
        this.isLoading = false;
        alert('Request updated successfully!');
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error updating request:', error);
        alert('Failed to update the request. Please try again.');
      }
    });
  }
}
