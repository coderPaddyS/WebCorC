import { Injectable, signal, Signal } from "@angular/core";
import { LocalCBCFormula } from "../../types/CBCFormula";
import { ProjectService } from "../project/project.service";
import { IRootStatement, RootStatement } from "../../types/statements/root-statement";
import { ConsoleService } from "../console/console.service";
import { IAbstractStatement } from "../../types/statements/abstract-statement";
import { AbstractStatementNode } from "../../types/statements/nodes/abstract-statement-node";
import { GlobalSettingsService } from "../global-settings.service";
import { IFBCFormula, IFBCVerificationResult } from "../../types/IFBCFormula";
import { TreeService } from "../tree/tree.service";
import { ConfidentialityLattice, ILattice, ILatticeLevel, IntegrityLattice, LatticeLevel } from "../../types/confidentiality/confidentiality";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { APIVariableIFbCState, toVariableConfidentialityIDMapping, VariableIFbCState } from "../../types/confidentiality/variableConfidentialityState";
import { BehaviorSubject, filter, map, Observable, of, tap } from "rxjs";
import { RootStatementComponent } from "../../components/editor/statements/root-statement/root-statement.component";
import { NetworkJobService } from "../tree/network/network-job.service";

type LatticeGetResponse = { 
    lattice: {
        minimalLevel: {
            id: number,
            name: string,
            parentIDs: number[]
        },
        levels: {
            id: number,
            name: string,
            parentIDs: number[]
        }[]
    }, 
    preConditions: APIVariableIFbCState, 
    postConditions: APIVariableIFbCState
}

/**
 * Service to distribute the verification result from the http response to the tree service.
 * @see TreeService
 */
@Injectable({
  providedIn: "root",
})
export class IFbCService {

    constructor(
        private readonly http: HttpClient,
        private projectService: ProjectService,
        private treeService: TreeService,
        private networkTreeService: NetworkJobService,
        private consoleService: ConsoleService,
        private globalSettingsService: GlobalSettingsService,
    ) {
        this.treeService.variableUpdateNotifier.subscribe(() => {
            this.variables = this.treeService.variables
            this.preVariableState$.next({
                confidentiality: this.adjustVariables(this.preVariableState$.value.confidentiality, this.variables, this.confidentialityLattice$.value),
                integrity: this.adjustVariables(this.preVariableState$.value.integrity, this.variables, this.integrityLattice$.value),
            })
            this.postVariableState$.next({
                confidentiality: this.adjustVariables(this.postVariableState$.value.confidentiality, this.variables, this.confidentialityLattice$.value),
                integrity: this.adjustVariables(this.postVariableState$.value.integrity, this.variables, this.integrityLattice$.value),
            })
            console.log("update variables", this.variables, this.preVariableState$.value, this.postVariableState$.value)
        })

    }

    private rootStatement: RootStatementComponent | undefined = undefined
    public readonly isCheckingConfidentiality = signal(false);
    
    private variables: string[] = [];

    private readonly confidentialityLattice$ = new BehaviorSubject<ConfidentialityLattice | undefined>(undefined);
    private readonly integrityLattice$ = new BehaviorSubject<IntegrityLattice | undefined>(undefined);
    private readonly preVariableState$ = new BehaviorSubject<VariableIFbCState>({ confidentiality: {}, integrity: {} });
    private readonly postVariableState$ = new BehaviorSubject<VariableIFbCState>({ confidentiality: {}, integrity: {} });

    private confidentialityLatticeLoaded = false;
    private integrityLatticeLoaded = false;

    private withDefaultLatticeLevel(state: { [variable: string]: ILatticeLevel | undefined } | undefined, variables: string[], lattice: ILattice | undefined): { [variable: string]: ILatticeLevel } {
        const entries = state ? Object.entries(state) : variables.map(v => [v, undefined])
        return Object.fromEntries(
            entries.map(([k, v]) => [k, v ?? lattice?.minimalLevel])
        )
    }

    private adjustVariables(current: { [variable: string]: ILatticeLevel }, variables: string[], lattice: ILattice | undefined) {
        const adjustedLevels = { ...this.withDefaultLatticeLevel(undefined, variables, lattice), ...current };
        console.log("adjust", current, variables, adjustedLevels, Object.fromEntries(Object.entries(adjustedLevels).filter(([k,v]) => variables.includes(k))))
        return Object.fromEntries(Object.entries(adjustedLevels).filter(([k,v]) => variables.includes(k)));
    }

    private mapVariableStateResponse(variableState: APIVariableIFbCState | undefined, lattice: ILattice): { [variable: string]: ILatticeLevel } {
        if (variableState !== undefined) {
            return Object.fromEntries(Object.entries(variableState).map(([k, v]) => [k, !!v ? lattice.levelById(v) : lattice.minimalLevel]))
        }
        return {}
    }

    private correctApiConfidentialityLattice(lattice: LatticeGetResponse['lattice']): ConfidentialityLattice {
        return new ConfidentialityLattice(
            lattice.levels.map(({ id, name, parentIDs }) => new LatticeLevel(id, name, parentIDs ?? []), lattice.minimalLevel)
        )
    }

    private correctApiIntegrityLattice(lattice: LatticeGetResponse['lattice']): IntegrityLattice {
        return new IntegrityLattice(
            lattice.levels.map(({ id, name, parentIDs }) => new LatticeLevel(id, name, parentIDs), lattice.minimalLevel)
        )
    }

    public get confidentialityLattice(): Observable<ConfidentialityLattice> {
        if (!this.confidentialityLatticeLoaded) {
            this.confidentialityLatticeLoaded = true;
            const params = this.projectService.projectId ? { projectId: this.projectService.projectId } : undefined
            this.http.get<LatticeGetResponse>(environment.apiUrl + "/ifbc/editor/lattice/confidentiality", { params })
                .pipe(map(resp => {
                    const lattice = this.correctApiConfidentialityLattice(resp.lattice);
                    return {
                        lattice,
                        preVariableState: this.mapVariableStateResponse(resp.preConditions, lattice),
                        postVariableState: this.mapVariableStateResponse(resp.postConditions, lattice),
                    }
                }))
                .subscribe(({lattice, preVariableState, postVariableState}) => {
                    console.log("trying to update variable confidentiality", preVariableState, postVariableState)
                    this.confidentialityLattice$.next(lattice)
                    this.preVariableState$.next({
                        confidentiality: this.adjustVariables(preVariableState, this.variables, lattice),
                        integrity: this.preVariableState$.value.integrity
                    });
                    this.postVariableState$.next({
                        confidentiality: this.adjustVariables(postVariableState, this.variables, lattice),
                        integrity: this.postVariableState$.value.integrity
                    });
                });
        }
        return this.confidentialityLattice$.pipe(filter((lattice) => lattice !== undefined));
    }

    public get preVariableState(): Observable<VariableIFbCState> {
        return this.preVariableState$.asObservable()
    }

    public get postVariableState(): Observable<VariableIFbCState> {
        return this.postVariableState$.asObservable()
    }

    public get integrityLattice(): Observable<IntegrityLattice> {
        if (!this.integrityLatticeLoaded) {
            this.integrityLatticeLoaded = true
            const params = this.projectService.projectId ? { projectId: this.projectService.projectId } : undefined
            this.http.get<LatticeGetResponse>(environment.apiUrl + "/ifbc/editor/lattice/integrity", { params })
                .pipe(map(resp => {
                    const lattice = this.correctApiIntegrityLattice(resp.lattice);
                    return {
                        lattice,
                        preVariableState: this.mapVariableStateResponse(resp.preConditions, lattice),
                        postVariableState: this.mapVariableStateResponse(resp.postConditions, lattice),
                    }
                }))
                .subscribe(({lattice, preVariableState, postVariableState}) => {
                    this.integrityLattice$.next(lattice)
                    this.preVariableState$.next({
                        integrity: this.adjustVariables(preVariableState, this.variables, lattice),
                        confidentiality: this.preVariableState$.value.confidentiality
                    });
                    this.postVariableState$.next({
                        integrity: this.adjustVariables(postVariableState, this.variables, lattice),
                        confidentiality: this.postVariableState$.value.confidentiality
                    });
                });
        }

        return this.integrityLattice$.pipe(filter((lattice) => lattice !== undefined));
    }

    public validateLattice(levels: ILatticeLevel[]) {
        return this.http.post<LatticeGetResponse['lattice']>(environment.apiUrl + "/ifbc/editor/lattice/validate", {levels})
    }

    public saveConfidentialityLattice(levels: ILatticeLevel[]) {
        console.log("save", levels, this.preVariableState$.value, this.postVariableState$.value)
        if (this.projectService.projectId === undefined) {
            // reuse the validation to get the correct lattice
            // as we cannot save without a project.
            return this.validateLattice(levels)
                    .pipe(map(lattice => this.correctApiIntegrityLattice(lattice)))
                    .subscribe(lattice => {
                        this.confidentialityLattice$.next(lattice);
                    });
        }
        const params = this.projectService.projectId ? { projectId: this.projectService.projectId } : undefined
        return this.http.post<LatticeGetResponse>(environment.apiUrl + "/ifbc/editor/lattice/confidentiality", {
            levels,
            preVariableState: toVariableConfidentialityIDMapping(this.preVariableState$.value),
            postVariableState: toVariableConfidentialityIDMapping(this.postVariableState$.value),
        }, { params })
            .pipe(map(resp => this.correctApiIntegrityLattice(resp.lattice)))
            .subscribe(lattice => {
                this.confidentialityLattice$.next(lattice);
            });
    }

    public saveIntegrityLattice(levels: ILatticeLevel[]) {
        return this.http.post<IntegrityLattice>(environment.apiUrl + "/ifbc/editor/lattice/integrity", {levels})
    }

    public registerRootStatement(statement: RootStatementComponent) {
        this.rootStatement = statement;
    }

    public verifyConfidentiality(
        checkConfidentiality: boolean,
        checkIntegrity: boolean,
    ): void {
        if (this.rootStatement === undefined || this.isCheckingConfidentiality()) {
          return;
        }
        this.isCheckingConfidentiality.set(true);
    
        // Finalize statements first
        this.treeService.finalizeStatements();
    
        // Create temporary formula from this node
        const tempFormula = this.treeService.createTempFormulaFromNode(this.rootStatement!._node);
          
        const tempIFBCFormula = IFBCFormula.fromCBCFormula({
            ...tempFormula, 
            preCondition:  this.rootStatement!._node.precondition.getValue(), 
            postCondition:  this.rootStatement._node.postcondition.getValue()
        }, this.confidentialityLattice$.value!)
        tempIFBCFormula.preVariables = this.preVariableState$.value
        tempIFBCFormula.postVariables = this.postVariableState$.value
    
        // Verify the statement
        this.networkTreeService.checkConfidentialityStatement(
          tempIFBCFormula,
          this.rootStatement._node,
          this.projectService.projectId,
          checkConfidentiality,
          checkIntegrity,
          this.treeService.urn,
          () => {
            console.log("is finished")
            this.isCheckingConfidentiality.set(false);
          },
        );
      }

}
