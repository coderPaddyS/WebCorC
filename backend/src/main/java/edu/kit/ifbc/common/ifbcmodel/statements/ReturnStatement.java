package edu.kit.ifbc.common.ifbcmodel.statements;

import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.proof.ProofContext;

public class ReturnStatement extends AbstractIFbCStatement {

    private String returnStatement;

    @Override
    public boolean proveConfidentiality(ConfidentialityLattice lattice, ConfidentialityLevel level) {
        // TODO: Implement confidentiality check
        throw new UnsupportedOperationException();
    }
}
