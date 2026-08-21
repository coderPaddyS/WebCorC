package edu.kit.ifbc.common.ifbcmodel.statements;

import java.util.logging.Logger;

import edu.kit.cbc.common.corc.cbcmodel.Condition;
import edu.kit.ifbc.common.dto.VariableStateDTO;
import edu.kit.ifbc.common.ifbcmodel.Lattice.Level;
import edu.kit.ifbc.common.ifbcmodel.LatticeResultContext;
import edu.kit.ifbc.common.ifbcmodel.Lattice;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.integrity.IntegrityLattice;
import edu.kit.ifbc.common.ifbcmodel.parsing.parser.VariableParsingException;
import io.micronaut.serde.annotation.Serdeable;
import edu.kit.cbc.common.corc.proof.ProofContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class CompositionStatement extends AbstractIFbCStatement {

    private AbstractIFbCStatement firstStatement;
    private AbstractIFbCStatement secondStatement;

    @Override
    public VariableState calculatePostVariableState(
        Lattice lattice, 
        Lattice.Level level,
        VariableState preVariableState,
        LatticeResultContext context
    ) throws VariableParsingException {
        Logger.getGlobal().info("Condition: \t" + this.getPreCondition().getParsedCondition());
        context.handleChild(firstStatement.getId());
        VariableState postVariableStateS1 = firstStatement.calculatePostVariableState(lattice, level, preVariableState, context);
        context.finishChild();
        context.handleChild(secondStatement.getId());
        VariableState postVariableState = secondStatement.calculatePostVariableState(lattice, level, postVariableStateS1, context);
        context.finishChild();
        context.setInfo(postVariableState, level);
        return postVariableState;
    }
}
