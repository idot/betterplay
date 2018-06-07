import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialbetPlayerComponent } from './specialbet-player.component';

describe('SpecialbetPlayerComponent', () => {
  let component: SpecialbetPlayerComponent;
  let fixture: ComponentFixture<SpecialbetPlayerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SpecialbetPlayerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SpecialbetPlayerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
