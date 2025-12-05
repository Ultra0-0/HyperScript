// In src/main/java/Environment.java
import java.util.HashMap;
import java.util.Map;

class Environment {
    // A link to the parent scope. It's 'final' because it never changes.
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    // Constructor for the global scope (has no parent)
    Environment() {
        this.enclosing = null;
    }

    // Constructor for local scopes (takes its parent as an argument)
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // Define a variable in the CURRENT scope.
    void define(String name, Object value) {
        values.put(name, value);
    }

    // Get a variable. If not found here, check the parent.
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // If not in this scope, recursively search up the chain.
        if (enclosing != null) return enclosing.get(name);

        throw new Interpreter.RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    // Assign a value to an EXISTING variable. If not found here, check the parent.
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // If not in this scope, recursively try to assign to the parent.
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new Interpreter.RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}