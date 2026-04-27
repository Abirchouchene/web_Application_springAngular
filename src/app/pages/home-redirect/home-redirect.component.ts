import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RoleService } from 'src/app/services/role.service';

@Component({
  selector: 'app-home-redirect',
  standalone: true,
  template: '',
})
export class HomeRedirectComponent implements OnInit {
  constructor(private roleService: RoleService, private router: Router) {}

  ngOnInit(): void {
    this.roleService.ensureLoaded().then(() => {
      this.router.navigate([this.roleService.getHomeRoute()], { replaceUrl: true });
    });
  }
}
