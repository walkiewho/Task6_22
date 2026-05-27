package Task;

import java.util.Map;

public class Action {
    private actionTypes type;
    private String key;
    private Integer value;
    public Action(actionTypes type, String key, Integer value) {
        setType(type);
        setKey(key);
        setValue(value);
    }

    public actionTypes getType() {
        return type;
    }

    public void setType(actionTypes type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void doAction(Map<String, Integer> map) {
        switch (type) {
            case PUT:
                map.put(key, value);
                break;
            case GET:
                map.get(key);
                break;
            case REMOVE:
                map.remove(key);
                break;
            case CONTAINS_KEY:
                map.containsKey(key);
                break;
            case ENTRY:
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                }
                break;
        }
    }

    @Override
    public String toString() {
        return getType() + " (" + getKey() + ", " + getValue() + ")";
    }

    public enum actionTypes {
        PUT,
        GET,
        REMOVE,
        ENTRY,
        CONTAINS_KEY,
    }
}
