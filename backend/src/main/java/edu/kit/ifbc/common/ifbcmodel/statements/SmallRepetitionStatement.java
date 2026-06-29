package edu.kit.ifbc.common.ifbcmodel.statements;

import edu.kit.cbc.common.corc.cbcmodel.Condition;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.parsing.condition.ConditionPrinter;
import edu.kit.cbc.common.corc.proof.KeYProof;
import edu.kit.cbc.common.corc.proof.KeYProofGenerator;
import edu.kit.cbc.common.corc.proof.ProofContext;
import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmallRepetitionStatement extends AbstractIFbCStatement {

    private AbstractIFbCStatement loopStatement;
    private Condition guard;

    @Override
    public VariableState calculatePostConfidentialityState(
        ConfidentialityLattice lattice, 
        ConfidentialityLevel level,
        VariableState preVariableState
    ) {
        String[] usedVariables = getRelevantVariablesInCondition(guard);
        ConfidentialityLevel lub = lattice.leastUpperBound(preVariableState.confidentialityOf(lattice.getMinimalLevel(), usedVariables));
        ConfidentialityLevel context = lattice.leastUpperBound(lub, level);
        
        Logger.getGlobal().info("Condition: \t" + this.getPreCondition().getParsedCondition());
        return loopStatement.calculatePostConfidentialityState(lattice, context, preVariableState);
    }
}
