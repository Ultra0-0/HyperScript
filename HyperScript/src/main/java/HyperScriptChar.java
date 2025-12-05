// In src/main/java/HyperScriptChar.java

public class HyperScriptChar {
    private final char value;

    public HyperScriptChar(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    // --- The "Push, Not Pull" Law in action ---
    // A character operation that PROMOTES a character to a string.
    public HyperScriptString asString() {
        return new HyperScriptString(String.valueOf(this.value));
    }
}