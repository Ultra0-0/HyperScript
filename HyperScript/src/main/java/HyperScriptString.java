// In src/main/java/HyperScriptString.java

public class HyperScriptString {
    private final String value;

    public HyperScriptString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        // We add quotes to make it clear it's a string when printed.
        return "\"" + value + "\"";
    }
    
    // We will later add "manager" methods here like:
    // public HyperScriptString substring(...) { ... }
}
