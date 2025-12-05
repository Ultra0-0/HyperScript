// In src/main/java/Interpreter.java

import java.util.ArrayList;
import java.util.List;

// NO 'package' or 'import static'. We are disciplined.

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // The environment for storing variables.
    public final static Environment globals = new Environment();
    private Environment environment = globals;

    void interpret(List<Stmt> statements) {
        try {
            // --- PASS 1: Find all function declarations first. ---
            for (Stmt statement : statements) {
                if (statement instanceof Stmt.Function) {
                    execute(statement);
                }
            }
        
            // --- PASS 2: Execute all other code. ---
            for (Stmt statement : statements) {
                if (!(statement instanceof Stmt.Function)) {
                    execute(statement);
                }
            }
        } catch (RuntimeError error) {
            HyperScript.runtimeError(error);
        }
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // A function declaration just puts the function object into the environment.
        HyperScriptFunction function = new HyperScriptFunction(stmt);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    // --- THE UPGRADE: visitCallExpr ---
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof HyperScriptCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        HyperScriptCallable function = (HyperScriptCallable)callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                function.arity() + " arguments but got " +
                arguments.size() + ".");
        }

        return function.call(this, arguments);
    }
    
    // --- THE UPGRADE: visitBlockStmt ---
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    // This helper method is now the heart of our scoping logic.
    void executeBlock(List<Stmt> statements, Environment blockEnvironment) {
        Environment previous = this.environment;
        try {
            this.environment = blockEnvironment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // CRITICAL: After the block is done, we restore the previous environment.
            // This is how variables "go out of scope".
            this.environment = previous;
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case BANG: return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            default:
                break;
        }
        return null; // Unreachable.
    }

    @Override 
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // --- Arithmetic Operations ---
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                // Handle division by zero
                if ((double)right == 0.0) {
                throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                // Allow number addition OR string concatenation
                if (left instanceof Double && right instanceof Double) {
                return (double)left + (double)right;
                }
                if (left instanceof String || right instanceof String) {
                return stringify(left) + stringify(right);
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or at least one string.");

            // --- THE UPGRADES ---
            case PERCENT:
                checkNumberOperands(expr.operator, left, right);
                return (double)left % (double)right;
            case CARET:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double)left, (double)right);
        
            // Your custom concatenation operator
            case DOT_DOT:
                return stringify(left) + stringify(right);

            // --- Comparison Operations ---
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            default:
                break;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    
    // --- (Logical, Call expressions will be implemented later) ---
    // --- Statement Visitor Methods ---

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }
    
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        // Defines the variable in the current scope.
        environment.define(stmt.name.lexeme, value);
        return null;
    }


    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            // If the left side is true, we don't even look at the right side.
            if (isTruthy(left)) return left;
        } else { // This must be an AND
            // If the left side is false, we don't even look at the right side.
            if (!isTruthy(left)) return left;
        }

        // Only evaluate the right side if necessary.
        return evaluate(expr.right);
    }

    public static class Return extends RuntimeException {
        final Object value;

        Return(Object value) {
            // We override a method to disable noisy JVM stack trace generation,
            // as this is a normal control flow mechanism, not a true error.
            super(null, null, false, false);
            this.value = value;
        }
    }
    
    // --- (Function, Return statements will be implemented later) ---
    // Add this method to Interpreter.java
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new Return(value);
    }
    // --- (Other visit methods for IF, WHILE, etc. will go here) ---

    // --- Helper Methods ---
    private Object evaluate(Expr expr) { return expr.accept(this); }
    private void execute(Stmt stmt) { stmt.accept(this); }
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
    private String stringify(Object object) {
        if (object == null) return "null";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    // RuntimeError class is the same
    static class RuntimeError extends RuntimeException {
        final Token token;
        RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
        }
    }
}

