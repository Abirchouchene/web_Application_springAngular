import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from 'src/app/material.module';
import { TablerIconsModule } from 'angular-tabler-icons';
import { AppSettings } from 'src/app/config';

@Component({
  selector: 'app-header',
  template: `
    <mat-toolbar class="mat-toolbar topbar gap-8 mat-toolbar-single-row">
      <button mat-icon-button="" class="d-flex justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="" (click)="toggleMobileNav.emit()">
        <i-tabler name="align-left" class="icon-20 d-flex"></i-tabler>
      </button>

      <div class="d-none d-lg-flex">
        <button mat-button="" aria-label="Notifications" class="mat-mdc-menu-trigger mdc-button mat-mdc-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple">
          <div class="d-flex align-items-center"> Apps <i-tabler name="chevron-down" class="icon-16 m-l-4"></i-tabler></div>
        </button>
        <a mat-button="" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" class="mdc-button mat-mdc-button mat-unthemed mat-mdc-button-base" ng-reflect-router-link="/apps/chat" href="/apps/chat">Chat</a>
        <a mat-button="" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" class="mdc-button mat-mdc-button mat-unthemed mat-mdc-button-base" ng-reflect-router-link="/apps/calendar" href="/apps/calendar">Calendar</a>
        <a mat-button="" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" class="mdc-button mat-mdc-button mat-unthemed mat-mdc-button-base" ng-reflect-router-link="/apps/email/inbox" href="/apps/email/inbox">Email</a>
      </div>

      <span class="flex-1-auto"></span>

      <button mat-icon-button="" class="d-flex d-lg-none justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="">
        <i-tabler name="grid-dots" class="icon-20 d-flex"></i-tabler>
      </button>

      <button mat-stroked-button="" class="d-none d-lg-flex custom-outline-btn mdc-button mdc-button--outlined mat-mdc-outlined-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple">
        <div class="d-flex align-items-center">
          <i-tabler name="search" class="icon-20 d-flex m-r-10"></i-tabler>
          Try to Searching...
        </div>
      </button>

      <button mat-icon-button="" class="m-l-10 d-none d-lg-flex align-items-center justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="">
        <img class="rounded-circle object-cover icon-20" src="/assets/images/flag/icon-flag-en.svg">
      </button>

      <button mat-icon-button="" class="d-flex justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="">
        <i-tabler class="d-flex icon-22" name="moon"></i-tabler>
      </button>

      <button mat-icon-button="" class="d-flex d-lg-none justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-uninitialized="" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="">
        <i-tabler name="dots" class="icon-20 d-flex"></i-tabler>
      </button>

      <button mat-icon-button="" aria-label="Messages" class="mat-mdc-menu-trigger d-none d-lg-block align-items-center justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="">
        <i-tabler name="message-2" class="d-flex"></i-tabler>
        <div class="pulse">
          <span class="heartbit border-primary"></span>
          <span class="point bg-primary"></span>
        </div>
      </button>

      <button mat-icon-button="" aria-label="Notifications" class="mat-mdc-menu-trigger d-none d-lg-block align-items-center justify-content-center mdc-icon-button mat-mdc-icon-button mat-unthemed mat-mdc-button-base" mat-ripple-loader-class-name="mat-mdc-button-ripple" mat-ripple-loader-centered="">
        <i-tabler name="bell" class="d-flex"></i-tabler>
        <div class="pulse">
          <span class="heartbit border-primary"></span>
          <span class="point bg-primary"></span>
        </div>
      </button>

      <button mat-fab="" extended="" color="inherit" aria-label="Notifications" class="profile-btn d-none d-lg-flex align-items-center justify-content-center mdc-fab mat-mdc-fab-base mat-mdc-fab mat-inherit mat-mdc-button-base mdc-fab--extended mat-mdc-extended-fab">
        <div class="d-flex align-items-center gap-12">
          <img src="/assets/images/profile/user5.jpg" width="40" class="rounded-circle object-cover">
          <div class="d-none d-lg-flex text-left flex-col">
            <h5 class="f-s-16 f-w-600">hadil</h5>
            <span class="f-s-14">Admin</span>
          </div>
        </div>
      </button>
    </mat-toolbar>
  `,
  styles: [`
    .topbar {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1000;
      background: white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .gap-8 {
      gap: 8px;
    }
    .m-l-10 {
      margin-left: 10px;
    }
    .m-r-10 {
      margin-right: 10px;
    }
    .flex-1-auto {
      flex: 1 1 auto;
    }
  `],
  imports: [
    CommonModule,
    MaterialModule,
    TablerIconsModule
  ],
  standalone: true
})
export class HeaderComponent {
  @Input() showToggle = true;
  @Output() toggleCollapsed = new EventEmitter<void>();
  @Output() toggleMobileNav = new EventEmitter<void>();
  @Output() toggleMobileFilterNav = new EventEmitter<void>();
  @Output() optionsChange = new EventEmitter<AppSettings>();
} 