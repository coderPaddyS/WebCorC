package edu.kit.ifbc.common.ifbcmodel.confidentiality;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Introspected
@Serdeable
@AllArgsConstructor
public class ConfidentialityLattice {
    @Getter
    private ConfidentialityLevel minimalLevel;
    @Getter
    private ConfidentialityLevel maximalLevel;
    
    private List<ConfidentialityLevel> topologicalOrder;
    private HashMap<Integer, Integer> orderMap;

    public ConfidentialityLattice(List<ConfidentialityLevel> levels) {
        if (levels.stream().filter(l -> l.parentIDs().isEmpty()).count() != 1) {
            throw new IllegalArgumentException("Exactly one level may have no parents and be the maximal level");
        }

        // Create a mapping from id to index in the levels list
        this.orderMap = new HashMap<>();
        IntStream.range(0, levels.size())
            .forEach((i) -> this.orderMap.put(levels.get(i).id(), i));

        /* 
            Calculate the topological order of the lattice.
            Uses a DFS to identify roots and to determine the level in the topological sort.

            This algorithm is based on the classical topological sort found in:
            @article{cormen2001section,
                title={Section 22.4: topological sort},
                author={Cormen, Thomas H and Leiserson, Charles E and Rivest, Ronald L and Stein, Clifford},
                journal={Introduction to Algorithms (2nd ed.), MIT Press and McGraw-Hill},
                pages={549--552},
                year={2001}
            }
        */
        
        this.topologicalOrder = new LinkedList<>();
        boolean[] tempMarkings = new boolean[levels.size()];
        boolean[] permMarkings = new boolean[levels.size()];
        boolean[] isParent = new boolean[levels.size()];
        int leftMostNonPermanentMarking = 0;

        RecursiveConsumer<Integer> visitVertex = (visit, vertex) -> {
            if (permMarkings[vertex]) {
                return;
            }
            if (tempMarkings[vertex]) {
                throw new IllegalArgumentException("Given lattice is not a DAG!");
            }

            tempMarkings[vertex] = true;
            for (Integer parent : levels.get(vertex).parentIDs()) {
                int index = orderMap.get(parent);
                isParent[index] = true;
                visit.apply(visit, index);
            }
            permMarkings[vertex] = true;
            this.topologicalOrder.addFirst(levels.get(vertex));
        };

        while (leftMostNonPermanentMarking < levels.size()) {
            visitVertex.apply(visitVertex, leftMostNonPermanentMarking);
            do { leftMostNonPermanentMarking += 1; } 
            while (leftMostNonPermanentMarking < levels.size() && permMarkings[leftMostNonPermanentMarking]);
        }

        int roots = 0;
        for (int i = 0; i < levels.size(); ++i) {
            if (!isParent[i]) {
                roots += 1;
            }
        }

        if (roots != 1) {
            throw new IllegalArgumentException("Given lattice has no global minimum");
        }

        this.minimalLevel = this.topologicalOrder.getFirst();
        this.maximalLevel = this.topologicalOrder.getLast();

        orderMap.clear();
        IntStream.range(0, this.topologicalOrder.size())
            .forEach((i) -> this.orderMap.put(this.topologicalOrder.get(i).id(), i));
    }


    public ConfidentialityLevel leastUpperBound(ConfidentialityLevel lhs, ConfidentialityLevel rhs) {
        if (lhs.equals(rhs)) {
            return lhs; 
        }
        int lhsIndex = this.orderMap.get(lhs.id());
        int rhsIndex = this.orderMap.get(rhs.id());
        ConfidentialityLevel smaller = this.topologicalOrder.get(Math.min(lhsIndex, rhsIndex));
        ConfidentialityLevel bigger = this.topologicalOrder.get(Math.max(lhsIndex, rhsIndex));
        if (smaller.equals(this.minimalLevel)) {
            return rhs;
        } else if (bigger.equals(this.maximalLevel)) {
            return this.maximalLevel;
        }


        PriorityQueue<Integer> smallerQueue = new PriorityQueue<>();
        PriorityQueue<Integer> biggerQueue = new PriorityQueue<>();
        smallerQueue.addAll(smaller.parentIDs().stream().map((c) -> this.orderMap.get(c)).toList());
        biggerQueue.addAll(bigger.parentIDs().stream().map((c) -> this.orderMap.get(c)).toList());

        // Determine the first index which occurs in both queues
        while(!smallerQueue.isEmpty() && !biggerQueue.isEmpty()) {
            int index;
            if (smallerQueue.peek() <= biggerQueue.peek()) {
                index = smallerQueue.remove();
            } else {
                index = biggerQueue.remove();
            }

            ConfidentialityLevel level = this.topologicalOrder.get(index);
            for (Integer parent : level.parentIDs()) {
                index = this.orderMap.get(parent);
                if (smallerQueue.contains(index) || biggerQueue.contains(index)) {
                    return this.topologicalOrder.get(index);
                }
            }

        };

        if (smallerQueue.isEmpty()) {
            return this.topologicalOrder.get(biggerQueue.remove());
        } else {
            return this.topologicalOrder.get(smallerQueue.remove());
        }
    }

    public ConfidentialityLevel leastUpperBound(ConfidentialityLevel... levels) {
        ConfidentialityLevel lub = levels[0];
        Logger.getGlobal().info("lub: " + lub.name() + ", " + lub.id());
        for (int i = 1; i < levels.length; i++) {
            Logger.getGlobal().info("i: " + i + " prev lub: " + lub.name() + ", " + lub.id() + " level: " + levels[i].name() + ", " + levels[i].id());
            lub = leastUpperBound(lub, levels[i]);
            Logger.getGlobal().info("i: " + i + " lub: " + lub.name() + ", " + lub.id());
        }
        return lub;
    }

    public ConfidentialityLevel confidentialityById(Integer id) {
        return this.topologicalOrder.get(this.orderMap.get(id));
    }

    public static ConfidentialityLattice defaultLConfidentialityLattice() {
        return new ConfidentialityLattice(List.of(
            new ConfidentialityLevel(0, "public", List.of(1)),
            new ConfidentialityLevel(1, "private", List.of(2)),
            new ConfidentialityLevel(2, "secret")
        ));

    }

    @Override
    public String toString() {
        return this.topologicalOrder.stream().map(l -> l.toString()).collect(Collectors.joining(","));
    }

    interface RecursiveConsumer<T> {
        void apply(RecursiveConsumer<T> self, T t);
    }
}
