package edu.kit.ifbc.common.ifbcmodel.statements;

import edu.kit.cbc.common.corc.cbcmodel.Condition;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.parsing.condition.ConditionPrinter;
import edu.kit.cbc.common.corc.proof.KeYProof;
import edu.kit.cbc.common.corc.proof.KeYProofGenerator;
import edu.kit.cbc.common.corc.proof.ProofContext;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectionStatement extends AbstractIFbCStatement {
    private List<Condition> guards;
    private List<AbstractIFbCStatement> commands;
    private boolean isPreProven;

    @Override
    public VariableState calculatePostConfidentialityState(
        ConfidentialityLattice lattice, 
        ConfidentialityLevel level,
        VariableState preVariableState
    ) {
        // TODO: Determine correct usedVariables in guards
        List<String[]> usedVariables = guards
            .stream()
            .map(guard -> getRelevantVariablesInCondition(guard))
            .toList();
        Set<String> usedVariablesSet = new HashSet<>();
        for (String[] variables : usedVariables) {
            for (String variable : variables) {
                usedVariablesSet.add(variable);
            }
        } 
        ConfidentialityLevel lub = lattice.leastUpperBound(preVariableState.confidentialityOf(lattice.getMinimalLevel(), (String[]) usedVariablesSet.toArray()));
        ConfidentialityLevel context = lattice.leastUpperBound(lub, level);
        List<VariableState> states = this.commands.stream().map(statement -> statement.calculatePostConfidentialityState(lattice, context, preVariableState)).toList();
        VariableState finalPostState = states.stream().reduce((a,b) -> a.withEachLub(lattice, b)).get();
        return finalPostState;
    }
}
