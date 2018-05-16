import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialbetTeamComponent } from './specialbet-team.component';

describe('SpecialbetTeamComponent', () => {
  let component: SpecialbetTeamComponent;
  let fixture: ComponentFixture<SpecialbetTeamComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SpecialbetTeamComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SpecialbetTeamComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
