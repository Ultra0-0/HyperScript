// In src/main/java/HyperScriptCallable.java
import java.util.List;

interface HyperScriptCallable {
    // How many arguments does the function expect?
    int arity();
    // The actual code to execute when the function is called.
    Object call(Interpreter interpreter, List<Object> arguments);
}
