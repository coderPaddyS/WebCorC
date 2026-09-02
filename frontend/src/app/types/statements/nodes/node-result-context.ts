import { ILatticeLevel } from "../../confidentiality/confidentiality";

type ResultContext = {
    postVariableState: { [variable: string]: ILatticeLevel };
    context: ILatticeLevel;
    compatibleWithPostState?: boolean;
}
export class NodeResultContext {
    confidentiality?: ResultContext;
    integrity?: ResultContext;
}