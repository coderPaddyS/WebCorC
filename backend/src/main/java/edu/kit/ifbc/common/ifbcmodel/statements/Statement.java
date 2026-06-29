package edu.kit.ifbc.common.ifbcmodel.statements;

import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.parsing.TokenSource;
import edu.kit.cbc.common.corc.parsing.lexer.Lexer;
import edu.kit.cbc.common.corc.parsing.parser.ast.Tree;
import edu.kit.cbc.common.corc.parsing.program.ProgramLexer;
import edu.kit.cbc.common.corc.parsing.program.ProgramParser;
import edu.kit.cbc.common.corc.parsing.program.ProgramPrinter;
import edu.kit.cbc.common.corc.proof.KeYProof;
import edu.kit.cbc.common.corc.proof.KeYProofGenerator;
import edu.kit.cbc.common.corc.proof.ProofContext;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class Statement extends AbstractIFbCStatement {

    private String variable;
    private String[] usedVariables;

    @Override
    public VariableState calculatePostConfidentialityState(
        ConfidentialityLattice lattice, 
        ConfidentialityLevel level,
        VariableState preVariableState
    ) {
        Logger.getGlobal().info("Condition: \t" + this.getPreCondition().getParsedCondition());
        // TODO: Replace by actual variable definitions;
        this.usedVariables = new String[1];
        this.usedVariables[0] = "int i";
        this.variable = "int i";
        ConfidentialityLevel lub = lattice.leastUpperBound(preVariableState.confidentialityOf(lattice.getMinimalLevel(), usedVariables));
        Logger.getGlobal().info("lub of preVariableStates: " + lub.name());
        lub = lattice.leastUpperBound(lub, preVariableState.confidentialityOf(variable, lattice.getMinimalLevel()), level);

        Logger.getGlobal().info("level: " + level.name());
        Logger.getGlobal().info("lub afterwards: " + lub.name());
        return preVariableState.with(variable, lub);
    }
}
