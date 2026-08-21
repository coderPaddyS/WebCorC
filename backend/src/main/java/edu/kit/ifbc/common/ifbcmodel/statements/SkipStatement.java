package edu.kit.ifbc.common.ifbcmodel.statements;

import edu.kit.ifbc.common.ifbcmodel.Lattice.Level;
import edu.kit.ifbc.common.ifbcmodel.LatticeResultContext;
import edu.kit.ifbc.common.ifbcmodel.Lattice;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.integrity.IntegrityLattice;
import edu.kit.ifbc.common.ifbcmodel.parsing.parser.VariableParsingException;
import io.micronaut.serde.annotation.Serdeable;


@Serdeable
public class SkipStatement extends AbstractIFbCStatement {
    @Override
    public VariableState calculatePostVariableState(
        Lattice lattice, 
        Lattice.Level level,
        VariableState preVariableState,
        LatticeResultContext context
    ) { 
        context.setInfo(preVariableState, level);
        return preVariableState;
    }
}
