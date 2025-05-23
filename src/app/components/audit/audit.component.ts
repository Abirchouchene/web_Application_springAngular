import {Component, OnInit} from '@angular/core';
import {HistoriqueAction} from "../../models/HistoriqueAction";
import {AuditService} from "../../services/audit.service";
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-audit',
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.scss']
})
export class AuditComponent implements OnInit {

  logs: HistoriqueAction[] = [];
  loading = false;
  error = '';
  dataSource = new MatTableDataSource<HistoriqueAction>([]);
  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs() {
    this.loading = true;
    this.auditService.getAllLogs().subscribe({
      next: (data) => {
        this.logs = data;
        this.dataSource.data = this.logs;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des historiques';
        this.loading = false;
        console.error(err);
      }
    });
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.dataSource.filter = filterValue;
  }
}
