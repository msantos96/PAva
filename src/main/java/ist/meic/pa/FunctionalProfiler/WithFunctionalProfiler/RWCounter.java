package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.Skip;

@Skip
public class RWCounter {
    private HashMap<String, int[]> __rwCounters = new HashMap<String, int[]>();
    public void putIfAbsent(String key) {
        __rwCounters.putIfAbsent(key, new int[2]);
    }
    public void incRead(String key) {
        __rwCounters.get(key)[0]++;
    }
    public void incWrite(String key) {
        __rwCounters.get(key)[1]++;
    }

    public void printProfiles() {
        int[] counter = new int[2];
        for(int[] c : __rwCounters.values()) {
            counter[0] += c[0];
            counter[1] += c[1];
        }
        
        Map<String,int[]> orderedMap = new TreeMap<String,int[]>(__rwCounters);
        System.out.print("Total reads: " + counter[0] + " Total writes: " + counter[1]);
        for(String key : orderedMap.keySet())
        	if(orderedMap.get(key)[0]!=0 || orderedMap.get(key)[1]!=0) {
        		System.out.print("\nclass " + key + " -> reads: " + orderedMap.get(key)[0] + " writes: " + orderedMap.get(key)[1]);
        	}
    }
}