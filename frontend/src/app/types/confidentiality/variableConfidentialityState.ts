import { LatticeLevel, ILatticeLevel } from "./confidentiality";

function omitType(variable: string): string {
    const index = variable.lastIndexOf(' ');
    if (index < 0) {
        return variable
    } else {
        return variable.substring(index + 1)
    }
}

export type VariableIFbCState = { 
    confidentiality: {
        [variable: string]: ILatticeLevel
    };
    integrity: {
        [variable: string]: ILatticeLevel
    };
}

export type APIVariableIFbCState = { [variable: string]: number }

export function toVariableConfidentialityIDMapping(state: VariableIFbCState) {
    return {
        confidentiality: Object.fromEntries(Object.entries(state.confidentiality).map(([k,v]) => ([omitType(k),v.id]))),
        integrity: Object.fromEntries(Object.entries(state.integrity).map(([k,v]) => ([omitType(k),v.id])))
    }
}