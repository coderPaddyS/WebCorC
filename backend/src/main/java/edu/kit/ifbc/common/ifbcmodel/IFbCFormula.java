package edu.kit.ifbc.common.ifbcmodel;

import edu.kit.cbc.common.corc.cbcmodel.JavaVariable;
import edu.kit.cbc.common.corc.cbcmodel.Renaming;
import edu.kit.ifbc.common.dto.VariableStateDTO;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import edu.kit.ifbc.common.ifbcmodel.statements.AbstractIFbCStatement;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Serdeable
public class IFbCFormula {
    private String name;
    private AbstractIFbCStatement statement;
    private List<JavaVariable> javaVariables;
    private List<Renaming> renamings;

    private ConfidentialityLattice lattice = ConfidentialityLattice.defaultLConfidentialityLattice();

    private Integer level;

    private boolean respectsConfidentiality;
    private VariableStateDTO preVariableState;
    private VariableStateDTO postVariableState;

    public boolean proveConfidentiality() {
        // Add this defensive check as somehow due to Jackson lattice is null if not provided in the request
        if (lattice == null) {
            lattice = ConfidentialityLattice.defaultLConfidentialityLattice();
        }
        
        Logger.getGlobal().info("lattice: \n" + lattice.toString());

        Logger.getGlobal().info("variableState: \t" + this.preVariableState.confidentiality() + "\t" + this.postVariableState);
        VariableState calculatedState = statement.calculatePostConfidentialityState(
            lattice, 
            lattice.confidentialityById(level),
            VariableState.fromIDs(this.preVariableState.confidentiality(), lattice)
        );
        Logger.getGlobal().info("calculated: \t" + calculatedState + "\n post: \t" + VariableState.fromIDs(this.postVariableState.confidentiality(), lattice) + "\n equal: \t" + calculatedState.equals(VariableState.fromIDs(this.postVariableState.confidentiality(), lattice)));
        return calculatedState.equals(VariableState.fromIDs(this.postVariableState.confidentiality(), lattice));
    }
}
