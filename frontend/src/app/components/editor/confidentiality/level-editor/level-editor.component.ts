import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Condition, } from '../../../../types/condition/condition';
import { AiChatService } from '../../../../services/ai-chat/ai-chat.service';
import { FloatLabelModule } from 'primeng/floatlabel';
import { $dt } from '@primeuix/themes';
import { FormsModule } from '@angular/forms';
import {  defaultConfidentialityLattice, defaultIntegrityLattice, ILattice, ILatticeLevel } from '../../../../types/confidentiality/confidentiality';
import { Select } from 'primeng/select';
import { VariableIFbCState } from '../../../../types/confidentiality/variableConfidentialityState';
import { Tag } from 'primeng/tag';

/**
 * Editor in the statements for the {@link Condition}
 * @link https://material.angular.io/components/form-field/overview
 * @link https://angular.dev/guide/forms/reactive-forms
 */
@Component({
  selector: 'level-editor',
  imports: [FloatLabelModule, FormsModule, Select, Tag],
  templateUrl: './level-editor.component.html',
  standalone: true,
  styleUrl: './level-editor.component.css',
})
export class LevelEditorComponent {
  @Input({ required: true }) public variables!: { [variable: string]: ILatticeLevel };
  @Input({ required: true }) public lattice!: ILattice;
  @Input() public readonly: boolean = false;


  @Output() public variablesChange = new EventEmitter<VariableIFbCState>();

  public constructor(
  ) {}

  public get levels(): ILatticeLevel[] {
    return this.lattice.levels
  }

  // public set variables(value: VariableConfidentialityState) {
  //   this._variables = value;
  //   this.variablesChange.emit(value)
  // }
  // public get variables(): VariableConfidentialityState {
  //   return this._variables
  // }

  public onConditionChange(newConditionString: string): void {
    // const currentCondition = this.condition.getValue();
    // // Create a new condition object or update existing one?
    // // Assuming we should update the existing one or create a new one if it doesn't exist.
    // // However, since we are passing ICondition objects around, let's update the property.
    // // But to trigger updates properly with BehaviorSubject, we might want to emit a new object reference if immutability is desired.
    // // Based on previous code: this.condition.condition = event; this.conditionChange.emit(this.condition);
    // // It seems mutation was used.
    
    // if (currentCondition) {
    //     currentCondition.condition = newConditionString;
    //     this.condition.next(currentCondition);
    // } else {
    //     // Should not happen if initialized correctly, but as a fallback
    //     this.condition.next(new Condition(newConditionString));
    // }
  }

  public get items(): { variable: string, level: ILatticeLevel }[] {
    console.log("variable level", this.variables)
    return Object.keys(this.variables).map((variable) => ({ variable, level: this.variables[variable] }))
  }

  public onVariableLevelChanged(variable: string, level: ILatticeLevel): void {
    this.variables[variable] = level
  }

  protected readonly $dt = $dt;
}
