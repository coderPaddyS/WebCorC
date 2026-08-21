import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfidentialityEditorComponent } from './confidentiality-editor.component';
import { Condition } from '../../../../types/condition/condition';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { defaultConfidentialityLattice } from '../../../../types/confidentiality/confidentiality';

describe('ConditionEditorComponent', () => {
  let component: ConfidentialityEditorComponent;
  let fixture: ComponentFixture<ConfidentialityEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfidentialityEditorComponent],
      providers: [provideHttpClient(),provideAnimations()]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ConfidentialityEditorComponent);
    component = fixture.componentInstance;
    component.variables = { 'i': defaultConfidentialityLattice.levelById(0) }
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
