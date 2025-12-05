import java.util.List;

class HyperScriptFunction implements HyperScriptCallable {
    private final Stmt.Function declaration; // The AST node for the function

    HyperScriptFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    // In HyperScriptFunction.java
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Create a new environment for the function's scope.
        Environment environment = new Environment(Interpreter.globals); // Or a more complex closure environment
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            // Execute the function body in the new, local environment.
            interpreter.executeBlock(declaration.body, environment);
        } catch (Interpreter.Return returnValue) {
            // --- THE CATCH ---
            // If a 'return' is thrown, we catch it here and return its value.
            // This is how we escape the function call.
            return returnValue.value;
        }

        // If the function finishes without a return statement, it returns null.
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
