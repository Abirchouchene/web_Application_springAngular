import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestManagerViewComponent } from './request-manager-view.component';

describe('RequestManagerViewComponent', () => {
  let component: RequestManagerViewComponent;
  let fixture: ComponentFixture<RequestManagerViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequestManagerViewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RequestManagerViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
