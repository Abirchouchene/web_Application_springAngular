import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestManagerListComponent } from './request-manager-list.component';

describe('RequestManagerListComponent', () => {
  let component: RequestManagerListComponent;
  let fixture: ComponentFixture<RequestManagerListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequestManagerListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RequestManagerListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
