package edu.kit.ifbc.common.ifbcmodel.statements;

import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.proof.KeYProof;
import edu.kit.cbc.common.corc.proof.KeYProofGenerator;
import edu.kit.cbc.common.corc.proof.ProofContext;

public class SkipStatement extends AbstractIFbCStatement {
    @Override
    public VariableState calculatePostConfidentialityState(
        ConfidentialityLattice lattice, 
        ConfidentialityLevel level,
        VariableState preVariableState
    ) { 
        return preVariableState;
    }
}
