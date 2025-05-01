import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common'; // <-- Add this
import { MatTableDataSource, MatTableModule } from '@angular/material/table'; // <-- Add MatTableModule
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator'; // <-- Add MatPaginatorModule
import { RequestService } from 'src/app/services/apps/ticket/ticket.service';
import { MatButtonModule } from '@angular/material/button'; // If you're using buttons
import { MatIconModule } from '@angular/material/icon'; // If you're using icons
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-request-manager-list',
  standalone: true, // If you are using standalone components
  imports: [
    RouterModule,
    MatTableModule,
  MatPaginatorModule,
  MatIconModule,
  MatButtonModule,
  MatFormFieldModule,
  MatInputModule,
  MatCardModule,
  DatePipe
  ],
  templateUrl: './request-manager-list.component.html',
  styleUrls: ['./request-manager-list.component.scss'] // <-- fix styleUrls
})
export class RequestManagerListComponent implements OnInit {
  displayedColumns: string[] = [
    'idR',
    'requestType',
    'requester',
    'description',
    'status',
    'priority',
    'categoryRequest',
    'note',
    'deadline',
    'action'
  ];

  dataSource = new MatTableDataSource<Request>([]);
  totalCount: number = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(private requestService: RequestService) {}

  ngOnInit(): void {
    this.getRequests();
  }

  getRequests(): void {
    this.requestService.getAllRequests().subscribe({
      next: (data: Request[]) => {
        this.dataSource.data = data;
        this.totalCount = data.length;
        this.dataSource.paginator = this.paginator;
      },
      error: (error) => {
        console.error('Error fetching requests', error);
      }
    });
  }

  

  btnCategoryClick(status: string): void {
    if (status) {
      this.dataSource.filter = status.trim().toLowerCase();
    } else {
      this.dataSource.filter = '';
    }
  }

  onKeyup(event: any): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  openDialog(action: string, element: Request): void {
    console.log(`${action} clicked for`, element);
  }
}
