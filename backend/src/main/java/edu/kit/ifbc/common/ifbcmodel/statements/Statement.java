package edu.kit.ifbc.common.ifbcmodel.statements;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.kit.cbc.common.corc.parsing.TokenSource;
import edu.kit.cbc.common.corc.parsing.lexer.Lexer;
import edu.kit.cbc.common.corc.parsing.program.ProgramLexer;
import edu.kit.cbc.common.corc.parsing.program.ProgramParser;
import edu.kit.ifbc.common.ifbcmodel.Lattice.Level;
import edu.kit.ifbc.common.ifbcmodel.LatticeResultContext;
import edu.kit.ifbc.common.ifbcmodel.Lattice;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.integrity.IntegrityLattice;
import edu.kit.ifbc.common.ifbcmodel.parsing.parser.VariableParsing;
import edu.kit.ifbc.common.ifbcmodel.parsing.parser.VariableParsingException;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class Statement extends AbstractIFbCStatement {

    private String variable;
    private String programStatement;

    public String[] getAssignmentLHSVariables(VariableState variables) throws VariableParsingException {
        Lexer lexer = ProgramLexer.forString(this.programStatement);
        TokenSource source = new TokenSource(lexer);
        ProgramParser parser = new ProgramParser(source);
        return VariableParsing.getRelevantVariables(parser.parse(), variables.getVariableSet());
    }

    @Override
    public VariableState calculatePostVariableState(
        Lattice lattice, 
        Lattice.Level level,
        VariableState preVariableState,
        LatticeResultContext context
    ) throws VariableParsingException {
        Logger.getGlobal().info("Condition: \t" + this.getPreCondition().getParsedCondition());
        Logger.getGlobal().info(programStatement);
        String[] usedVariables = getRelevantVariablesInStatement(programStatement, preVariableState);
        if (usedVariables == null) {
            return preVariableState;
        }

        String[] assignedVariables = getAssignmentLHSVariables(preVariableState);
        if (assignedVariables == null) {
            return preVariableState;
        }
        this.variable = assignedVariables[0];
        
        Logger.getGlobal().warning("used variables: \t" + String.join(",", usedVariables) + " \t variable: " + this.variable);
        Logger.getGlobal().warning("used confstates: \t" + Arrays.toString(preVariableState.levelOf(lattice.getMinimalLevel(), usedVariables)));
        Lattice.Level lub = lattice.leastUpperBound(preVariableState.levelOf(lattice.getMinimalLevel(), usedVariables));
        Logger.getGlobal().info("lub of preVariableStates: " + lub.name());
        lub = lattice.leastUpperBound(lub, preVariableState.levelOf(variable, lattice.getMinimalLevel()), level);

        Logger.getGlobal().info("level: " + level.name());
        Logger.getGlobal().info("lub afterwards: " + lub.name());
        VariableState postVariableState = preVariableState.with(variable, lub);
        context.setInfo(postVariableState, level);
        return postVariableState;
    }
}
