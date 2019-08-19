package edu.ucsc.dbtune.advisor.wfit;

import edu.ucsc.dbtune.advisor.wfit.IndexPartitions.Subset;
import edu.ucsc.dbtune.metadata.Index;

//CHECKSTYLE:OFF
public class InteractionSelector {
    
    /*
     * Note that when this is called, hotSet and hotPartitions are out of sync!
     */
    public static IndexPartitions choosePartitions(
            StaticIndexSet newHotSet, IndexPartitions oldPartitions,
            DoiFunction doiFunc, int maxNumStates, int numPartitionIterations, int minId)
    {
        java.util.Random rand = new java.util.Random();
        IndexPartitions bestPartitions;
        double bestCost;
    
        /* initialize bestPartitions with singleton sets */ 
        bestPartitions = new IndexPartitions(newHotSet, minId);

        /* nothing to do if merging is disabled by maxNumStates = 0 */
        if (maxNumStates <= 0)
            return bestPartitions;

        /* construct initial guess, which put indexes together that were previously together */
        for (int s = 0; s < oldPartitions.subsetCount(); s++) {
            IndexPartitions.Subset subset = oldPartitions.get(s);
            for (Index i1 : subset) {
                if (!newHotSet.contains(i1))
                    continue;
                for (Index i2 : subset) {
                    if (i1 == i2 || !newHotSet.contains(i2))
                        continue;
                    bestPartitions.merge(i1, i2);
                }
            }
        }
        bestCost = partitionCost(bestPartitions, doiFunc);
        
        for (int attempts = 0; attempts < numPartitionIterations; attempts++) {
            IndexPartitions currentPartitions = new IndexPartitions(newHotSet, minId);
            while (true) {
                double currentSubsetCount = currentPartitions.subsetCount();
                double currentStateCount = currentPartitions.wfaStateCount();
                double totalWeightSingletons = 0;
                double totalWeightOthers = 0;
                boolean foundSingletonPair = false;
                for (int s1 = 0; s1 < currentSubsetCount; s1++) {
                    IndexPartitions.Subset subset1 = currentPartitions.get(s1);
                    double size1 = subset1.size();
                    for (int s2 = s1+1; s2 < currentSubsetCount; s2++) { 
                        IndexPartitions.Subset subset2 = currentPartitions.get(s2);
                        double size2 = subset2.size();
                        double weight = interactionWeight(subset1, subset2, doiFunc);
                        if (weight == 0)
                            continue;
                        
                        if (size1 == 1 && size2 == 1) {
                            foundSingletonPair = true;
                            totalWeightSingletons += weight;
                        }
                        else if (!foundSingletonPair) {
                            double addedStates = Math.pow(2, size1+size2) - Math.pow(2, size1) - Math.pow(2, size2);
                            if (addedStates + currentStateCount > maxNumStates)
                                continue;
                            totalWeightOthers += weight / addedStates;
                        }
                    }
                }
                
                double weightThreshold;
                if (foundSingletonPair)
                    weightThreshold = rand.nextDouble() * totalWeightSingletons;
                else if (totalWeightOthers > 0)
                    weightThreshold = rand.nextDouble() * totalWeightOthers;
                else
                    break;
                
                double accumWeight = 0;
                for (int s1 = 0; s1 < currentSubsetCount && accumWeight <= weightThreshold; s1++) {
                    IndexPartitions.Subset subset1 = currentPartitions.get(s1);
                    double size1 = subset1.size();
                    for (int s2 = s1+1; s2 < currentSubsetCount && accumWeight <= weightThreshold; s2++) { 
                        IndexPartitions.Subset subset2 = currentPartitions.get(s2);
                        double size2 = subset2.size();
                        double weight = interactionWeight(subset1, subset2, doiFunc);
                        if (weight == 0)
                            continue;
                        
                        if (size1 == 1 && size2 == 1) {
                            accumWeight += weight;
                        }
                        else if (!foundSingletonPair) {
                            double addedStates = Math.pow(2, size1+size2) - Math.pow(2, size1) - Math.pow(2, size2);
                            if (addedStates + currentStateCount > maxNumStates)
                                continue;
                            accumWeight += weight / addedStates;
                        }
                        
                        if (accumWeight > weightThreshold) 
                            currentPartitions.merge(s1, s2); // for loops will exit due to threshold
                    }
                }
            } // end of while(true)
            
            // currentPartitions is our new candidate, now compare it
            double currentCost = partitionCost(currentPartitions, doiFunc);
            if (currentCost < bestCost) { 
                bestCost = currentCost;
                bestPartitions = currentPartitions;
            }
        }
        
        return bestPartitions;
    }
    
    private static double partitionCost(IndexPartitions partitions, DoiFunction doiFunc) {
        double cost = 0;
        for (int s1 = 0; s1 < partitions.subsetCount(); s1++) {
            IndexPartitions.Subset subset1 = partitions.get(s1);
            for (int s2 = s1+1; s2 < partitions.subsetCount(); s2++) { 
                IndexPartitions.Subset subset2 = partitions.get(s2);
                cost += interactionWeight(subset1, subset2, doiFunc);
            }
        }
        return cost;
    }

    private static double interactionWeight(Subset s1, Subset s2, DoiFunction doiFunc) {
        double weight = 0;
        for (Index i1 : s1)
            for (Index i2 : s2) 
                weight += doiFunc.doi(i1, i2);
        return weight;
    }
}
//CHECKSTYLE:ON
