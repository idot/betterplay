import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UserSpecialbetsComponent } from './user-specialbets.component';

describe('UserSpecialbetsComponent', () => {
  let component: UserSpecialbetsComponent;
  let fixture: ComponentFixture<UserSpecialbetsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UserSpecialbetsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UserSpecialbetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
