package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import java.util.HashMap;

import ist.meic.pa.FunctionalProfilerExtended.Skip;

@Skip
public class RWCounter {
    private HashMap<String, int[]> __rwCounters = new HashMap<String, int[]>();
    public void putIfAbsent(String key, int[] value) {
        __rwCounters.putIfAbsent(key, value);
    }
    public void printProfiles() {
        int[] counter = new int[2];
        for(int[] c : __rwCounters.values()) {
            counter[0] += c[0];
            counter[1] += c[1];
        }
        
        System.out.print("Total reads: " + counter[0] + " Total writes: " + counter[1]);
        for(String key : __rwCounters.keySet())
        	if(__rwCounters.get(key)[0]!=0 && __rwCounters.get(key)[1]!=0) {
        		System.out.print("\nclass " + key + " -> reads: " + __rwCounters.get(key)[0] + " write: " + __rwCounters.get(key)[1]);
        	}
    }
}