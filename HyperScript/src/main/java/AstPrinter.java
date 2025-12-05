// In src/main/java/AstPrinter.java

import java.util.List;

// The AstPrinter is now a visitor for BOTH expressions AND statements.
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    String print(List<Stmt> statements) { // <-- NEW entry point
        StringBuilder builder = new StringBuilder();
        for (Stmt statement : statements) {
            builder.append(statement.accept(this)).append("\n");
        }
        return builder.toString();
    }
    
    // --- Visitor Methods for Statements ---

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(block ");
        for (Stmt statement : stmt.statements) {
            builder.append(statement.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return parenthesize("expr_stmt", stmt.expression);
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(fun " + stmt.name.lexeme + "(");
        for (Token param : stmt.params) {
            builder.append(" " + param.lexeme);
        }
        builder.append(") ");
        for (Stmt bodyStmt : stmt.body) {
            builder.append(bodyStmt.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        if (stmt.elseBranch == null) {
            return parenthesize("if", stmt.condition, stmt.thenBranch);
        }
        return parenthesize("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value == null) return "(return)";
        return parenthesize("return", stmt.value);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer == null) {
            return parenthesize("var " + stmt.name.lexeme);
        }
        return parenthesize("var " + stmt.name.lexeme + " =", stmt.initializer);
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return parenthesize("while", stmt.condition, stmt.body);
    }

    // --- Visitor Methods for Expressions ---

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }
    
    @Override
    public String visitCallExpr(Expr.Call expr) {
        return parenthesize("call", expr.callee); // Simplified for now
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "null";
        return expr.value.toString();
    }
    
    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }
    
    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    // --- Helper Method ---

    private String parenthesize(String name, Object... parts) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Object part : parts) {
            builder.append(" ");
            if (part instanceof Expr) {
                builder.append(((Expr)part).accept(this));
            } else if (part instanceof Stmt) {
                builder.append(((Stmt)part).accept(this));
            } else if (part instanceof Token) {
                builder.append(((Token)part).lexeme);
            } else {
                builder.append(part);
            }
        }
        builder.append(")");
        return builder.toString();
    }
}