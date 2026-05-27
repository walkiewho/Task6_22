package Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class SequenceGenerator {
    private static final int RANDOM_VALUE_START = -100_000;
    private static final int RANDOM_VALUE_END = 100_000;
    private static int RANDOM_KEY_LENGTH = 10;

    private static Action.actionTypes randomType() {
        Random random = new Random();
        Action.actionTypes[] actiontypes = Action.actionTypes.values();
        return actiontypes[random.nextInt(actiontypes.length)];
    }

    private static String randomString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(RANDOM_KEY_LENGTH);

        for (int i = 0; i < RANDOM_KEY_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static Integer randomInteger() {
        Random random = new Random();
        return random.nextInt(RANDOM_VALUE_END - RANDOM_VALUE_START + 1) + RANDOM_VALUE_START;
    }

    public static <K> K getRandomKey(ArrayList<K> keys) {
        Random random = new Random();
        return keys.get(random.nextInt(keys.size()));
    }

    public static LinkedList<Action> generateCombinedActionSequence(int length) {
        LinkedList<Action> result = new LinkedList<>();
        for (int index = 0; index < length; index++) {
            Action.actionTypes type = randomType();
            switch (type) {
                case PUT:
                    result.add(new Action(type, randomString(), randomInteger()));
                    break;
                case GET, REMOVE, CONTAINS_KEY:
                    result.add(new Action(type, randomString(), null));
                    break;
                case ENTRY:
                    result.add(new Action(type, null, null));
                    break;
            }
        }
        return result;
    }

    public static LinkedList<Action> generateGetActionSequence(Map<String, Integer> map, int length) {
        LinkedList<Action> result = new LinkedList<>();
        ArrayList<String> keys = new ArrayList<>(map.keySet());

        for (int index = 0; index < length; index++) {
            result.add(new Action(Action.actionTypes.GET, getRandomKey(keys), null));
        }

        return result;
    }

    public static LinkedList<Action> generatePutActionSequence(int length) {
        LinkedList<Action> result = new LinkedList<>();

        for (int index = 0; index < length; index++) {
            result.add(new Action(Action.actionTypes.PUT, randomString(), randomInteger()));
        }

        return result;
    }

    public static int getRandomKeyLength() {
        return RANDOM_KEY_LENGTH;
    }

    public static void setRandomKeyLength(int randomKeyLength) {
        RANDOM_KEY_LENGTH = randomKeyLength;
    }

    public static void randomFillMap(Map<String, Integer> map, int length){
        for (int index = 0; index < length; index++) {
            map.put(randomString(), randomInteger());
        }
    }
}
