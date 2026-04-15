import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListeJobComponent } from './liste-job.component';

describe('ListeJobComponent', () => {
  let component: ListeJobComponent;
  let fixture: ComponentFixture<ListeJobComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListeJobComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListeJobComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
