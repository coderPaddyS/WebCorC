package edu.kit.ifbc.common.ifbcmodel;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLattice;
import edu.kit.ifbc.common.ifbcmodel.confidentiality.ConfidentialityLevel;
import io.micronaut.http.annotation.RequestAttribute;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class VariableState {
    private HashMap<String, ConfidentialityLevel> confidentiality;

    public VariableState(HashMap<String, ConfidentialityLevel> confidentiality) {
        this.confidentiality = confidentiality;
    }

    public VariableState(VariableState other) {
        HashMap<String, ConfidentialityLevel> map = new HashMap<>();
        for (var entry : other.confidentiality.entrySet()) {
            map.put(entry.getKey(), entry.getValue().clone());
        }
        this.confidentiality = map;
    }

    public VariableState without(String variable) {
        VariableState copy = new VariableState(this);
        copy.confidentiality.remove(variable);
        Logger.getGlobal().info("Variable without " + (variable == null ? "null" : variable));
        for (var entry : copy.confidentiality.entrySet()) {
            Logger.getGlobal().info(entry.getKey() + ": " + (entry == null ? "null" : entry.getValue().name()));;

        }
        return copy;
    }

    public VariableState with(String variable, ConfidentialityLevel level) {
        VariableState copy = new VariableState(this);
        copy.confidentiality.put(variable, level);
        return copy;
    }

    public ConfidentialityLevel confidentialityOf(String variable, ConfidentialityLevel def) {
        ConfidentialityLevel level = this.confidentiality.get(variable);
        if (level == null) {
            return def;
        } else {
            return level;
        }
    }

    public ConfidentialityLevel[] confidentialityOf(ConfidentialityLevel def, String... variables) {
        ConfidentialityLevel[] levels = new ConfidentialityLevel[variables.length];
        
        for (int i = 0; i < variables.length; i++) {
            levels[i] = this.confidentialityOf(variables[i], def);
        }
        
        return levels;
    }

    public VariableState withEachLub(ConfidentialityLattice lattice, VariableState other) {
        HashMap<String, ConfidentialityLevel> newMap = new HashMap<>();
        for (String variable : this.confidentiality.keySet()) {
            ConfidentialityLevel otherLevel = other.confidentialityOf(variable, this.confidentiality.get(variable));
            newMap.put(variable, lattice.leastUpperBound(this.confidentiality.get(variable), otherLevel));
        }
        for (String variable : other.confidentiality.keySet()) {
            if (this.confidentiality.containsKey(variable)) {
                continue;
            }
            newMap.put(variable, other.confidentiality.get(variable));
        }
        return new VariableState(newMap);
    }

    @JsonCreator
    public static VariableState fromIDs(
        @JsonProperty("confidentiality") Map<String, Integer> ids,
        ConfidentialityLattice lattice
    ) {
        Logger.getGlobal().info(lattice.confidentialityById(0).name());
        HashMap<String, ConfidentialityLevel> levels = new HashMap<>();
        for (var entry : ids.entrySet()) {
            levels.put(entry.getKey(), lattice.confidentialityById(entry.getValue()));
        }
        return new VariableState(levels);
    }
}
