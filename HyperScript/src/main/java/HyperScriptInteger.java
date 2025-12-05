// In src/main/java/HyperScriptInteger.java

public class HyperScriptInteger {
    private final long value; // Using 'long' for 64-bit integers

    public HyperScriptInteger(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    // --- The "Push, Not Pull" Law in action ---
    // An integer operation that PROMOTES an integer to a character.
    public HyperScriptChar toChar() {
        // This is the 'int_as_char' (casting) operation.
        return new HyperScriptChar((char)this.value);
    }
}
