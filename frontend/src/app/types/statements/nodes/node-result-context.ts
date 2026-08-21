import { ILatticeLevel } from "../../confidentiality/confidentiality";

type ResultContext = {
    postVariableState: { [variable: string]: ILatticeLevel };
    context: ILatticeLevel;
}
export class NodeResultContext {
    confidentiality?: ResultContext;
    integrity?: ResultContext;
}