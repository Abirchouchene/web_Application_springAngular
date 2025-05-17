import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { RequestService } from 'src/app/services/apps/ticket/request.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { RouterModule } from '@angular/router';
import { MatSort, MatSortModule } from '@angular/material/sort';

interface RequestData {
  idR: number;
  requestType: string;
  description: string;
  status: string;
  priority: string;
  categoryRequest: string;
  createdAt: string | Date;
  deadline: string | Date | null;
  note: string;
  user?: { name: string };
}

@Component({
  selector: 'app-request-manager-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    RouterModule,
    MatSortModule,
    DatePipe
  ],
  templateUrl: './request-manager-list.component.html',
  styleUrls: ['./request-manager-list.component.scss']
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
    'createdAt',
    'deadline',
    'note',
    'action'
  ];
  
  dataSource = new MatTableDataSource<RequestData>();
  totalCount = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(private requestService: RequestService) {}

  ngOnInit() {
    this.loadRequests();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadRequests() {
    this.requestService.getAllRequests().subscribe({
      next: (data: RequestData[]) => {
        // Convert string dates to Date objects
        data = data.map(item => ({
          ...item,
          createdAt: new Date(item.createdAt),
          deadline: item.deadline ? new Date(item.deadline) : null
        }));

        this.dataSource.data = data;
        this.totalCount = data.length;

        // Set default sorting
        if (this.sort) {
          this.sort.active = 'createdAt';
          this.sort.direction = 'desc';
          this.dataSource.sort = this.sort;
        }
      },
      error: (error) => {
        console.error('Error loading requests:', error);
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

  onKeyup(event: KeyboardEvent): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  openDialog(action: string, element: RequestData): void {
    console.log(`${action} clicked for request ${element.idR}`);
  }
}