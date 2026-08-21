package edu.kit.ifbc.common.ifbcmodel.statements;

import edu.kit.ifbc.common.ifbcmodel.Lattice.Level;
import edu.kit.ifbc.common.ifbcmodel.LatticeResultContext;
import edu.kit.ifbc.common.ifbcmodel.Lattice;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.integrity.IntegrityLattice;
import edu.kit.ifbc.common.ifbcmodel.parsing.parser.VariableParsingException;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import edu.kit.cbc.common.corc.proof.ProofContext;

@Serdeable
public class ReturnStatement extends AbstractIFbCStatement {

    private String returnStatement;

    @Override
    public VariableState calculatePostVariableState(
        Lattice lattice, 
        Lattice.Level level,
        VariableState preVariableState,
        LatticeResultContext context
    ) throws VariableParsingException {
        // TODO: Implement confidentiality checkS
        throw new UnsupportedOperationException();
    }
}
