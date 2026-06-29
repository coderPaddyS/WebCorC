package edu.kit.ifbc.common.ifbcmodel.confidentiality;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.serde.annotation.Serdeable;
@Serdeable
public record ConfidentialityLevel(
    int id,
    String name,
    List<Integer> parentIDs
) implements Cloneable {
    public ConfidentialityLevel(int id, String name) {
        this(id, name, new ArrayList<>());
    }

    @Override
    public ConfidentialityLevel clone() {
        try {
            return (ConfidentialityLevel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
