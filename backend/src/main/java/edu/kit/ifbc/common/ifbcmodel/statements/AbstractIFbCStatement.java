package edu.kit.ifbc.common.ifbcmodel.statements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import edu.kit.ifbc.common.dto.VariableStateDTO;
import edu.kit.ifbc.common.ifbcmodel.StatementType;
import edu.kit.ifbc.common.ifbcmodel.VariableState;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.cbc.common.corc.cbcmodel.Condition;
import edu.kit.cbc.common.corc.parsing.parser.ast.Tree;
import edu.kit.cbc.common.corc.proof.ProofContext;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Statement.class, name = "STATEMENT"),
    @JsonSubTypes.Type(value = CompositionStatement.class, name = "COMPOSITION"),
    @JsonSubTypes.Type(value = ReturnStatement.class, name = "RETURN"),
    @JsonSubTypes.Type(value = SelectionStatement.class, name = "SELECTION"),
    @JsonSubTypes.Type(value = SkipStatement.class, name = "SKIP"),
    @JsonSubTypes.Type(value = SmallRepetitionStatement.class, name = "REPETITION")
})
public abstract class AbstractIFbCStatement {

    private String name;
    private StatementType type;
    private Condition preCondition;
    private Condition postCondition;

    protected boolean respectsConfidentiality;
    // protected boolean respectsIntegrity;

    public abstract VariableState calculatePostConfidentialityState(
        ConfidentialityLattice lattice, 
        ConfidentialityLevel level,
        VariableState preVariableState
    );

    public String[] getRelevantVariablesInCondition(Condition condition) {
        Tree tree = condition.getParsedCondition();
        
        // TODO: Implement this, so get all variables which are not contained in a declassify operation.
        throw new UnsupportedOperationException();
    }

}
