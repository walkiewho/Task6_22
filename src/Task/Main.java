package Task;

import java.util.*;


public class Main {
    public static void main(String[] args) {
        testPut(100_000);

        testGet(100_000, 100_000);

        testCombined(10_000);
    }


    public static long testSequence(Map<String, Integer> map, LinkedList<Action> sequence) {
        long start = System.nanoTime();

        for (Action act : sequence){
            act.doAction(map);
        }

        return System.nanoTime() - start;
    }

    public static void testGet(int mapLength, int seqLength){
        System.out.println("=== GET TEST ===");
        System.out.println("Map length: " + mapLength);
        System.out.println("Sequence length: " + seqLength);

        HashMap<String, Integer> hash = new HashMap<>();
        SequenceGenerator.randomFillMap(hash, mapLength);
        TreeMap<String, Integer> tree = new TreeMap<>(hash);
        LinkedHashMap<String, Integer> linked = new LinkedHashMap<>(hash);

        LinkedList<Action> sequence = SequenceGenerator.generateGetActionSequence(hash, seqLength);

        double hashTime = testSequence(hash, sequence) / 1_000_000_000.0;
        System.out.println("HashMap: " + hashTime + "s");

        double treeTime = testSequence(tree, sequence) / 1_000_000_000.0;
        System.out.println("TreeMap: " + treeTime + "s");

        double linkedTime = testSequence(linked, sequence) / 1_000_000_000.0;
        System.out.println("LinkedHashMap: " + linkedTime + "s");
    }

    public static void testPut(int seqLength){
        System.out.println("=== PUT TEST ===");
        System.out.println("Sequence length: " + seqLength);


        LinkedList<Action> sequence = SequenceGenerator.generatePutActionSequence(seqLength);

        HashMap<String, Integer> hash = new HashMap<>();
        double hashTime = testSequence(hash, sequence) / 1_000_000_000.0;
        System.out.println("HashMap: " + hashTime + "s");

        TreeMap<String, Integer> tree = new TreeMap<>();
        double treeTime = testSequence(tree, sequence) / 1_000_000_000.0;
        System.out.println("TreeMap: " + treeTime + "s");

        LinkedHashMap<String, Integer> linked = new LinkedHashMap<>();
        double linkedTime = testSequence(linked, sequence) / 1_000_000_000.0;
        System.out.println("LinkedHashMap: " + linkedTime + "s");
    }

    public static void testCombined(int seqLength){
        System.out.println("=== COMBINED TEST ===");
        System.out.println("Sequence length: " + seqLength);


        LinkedList<Action> sequence = SequenceGenerator.generateCombinedActionSequence(seqLength);

        TreeMap<String, Integer> tree = new TreeMap<>();
        double treeTime = testSequence(tree, sequence) / 1_000_000_000.0;
        System.out.println("TreeMap: " + treeTime + "s");

        HashMap<String, Integer> hash = new HashMap<>();
        double hashTime = testSequence(hash, sequence) / 1_000_000_000.0;
        System.out.println("HashMap: " + hashTime + "s");

        LinkedHashMap<String, Integer> linked = new LinkedHashMap<>();
        double linkedTime = testSequence(linked, sequence) / 1_000_000_000.0;
        System.out.println("LinkedHashMap: " + linkedTime + "s");
    }

}