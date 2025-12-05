// In src/main/java/HyperScriptBool.java

// We might not build out the full functionality now, but we define the class.
// For now, it's a simple wrapper. We'll add methods later.
public class HyperScriptBool {
    private final boolean value;

    public HyperScriptBool(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    // We will later add methods here like:
    // public HyperScriptInteger toInt() { ... }
}
