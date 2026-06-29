package edu.kit.ifbc.common.ifbcmodel.statements;

import java.util.logging.Logger;

import edu.kit.cbc.common.corc.cbcmodel.Condition;
import edu.kit.ifbc.common.dto.VariableStateDTO;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.proof.ProofContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompositionStatement extends AbstractIFbCStatement {

    private AbstractIFbCStatement firstStatement;
    private AbstractIFbCStatement secondStatement;
    @Getter(AccessLevel.NONE)
    private VariableStateDTO intermediateVariableState;

    public VariableState getIntermediateVariableState(ConfidentialityLattice lattice) {
        return VariableState.fromIDs(intermediateVariableState.confidentiality(), lattice);
    }

    @Override
    public VariableState calculatePostConfidentialityState(
        ConfidentialityLattice lattice, 
        ConfidentialityLevel level,
        VariableState preVariableState
    ) {
        Logger.getGlobal().info("Condition: \t" + this.getPreCondition().getParsedCondition());
        VariableState postVariableStateS1 = firstStatement.calculatePostConfidentialityState(lattice, level, preVariableState);
        return secondStatement.calculatePostConfidentialityState(lattice, level, postVariableStateS1);
    }
}
