// In src/main/java/FlowParser.java
import java.util.ArrayList;
import java.util.List;

class FlowParser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    FlowParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        statements.removeIf(stmt -> stmt == null);
        return statements;
    }

    // =========================================================================
    // == GRAMMAR RULES - STATEMENTS
    // =========================================================================

    private Stmt declaration() {
        try {
            // Check for empty lines first.
            while (match(TokenType.EOL));
            if (isAtEnd()) return null;

            if (match(TokenType.FUNCTION)) return function("function");
            if (match(TokenType.VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) error(peek(), "Can't have more than 255 parameters.");
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        
        List<Stmt> body = block(TokenType.END);
        consume(TokenType.END, "Expect 'end' after " + kind + " body.");
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consumeTerminator("Expect newline or ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.PRINT)) return printStatement();
        // --- THE FIX for Bug #2 ---
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        // ...
        return expressionStatement();
    }
    
    // NEW METHOD
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        // A return can have a value or be empty.
        if (!isAtEndOfStatement()) {
            value = expression();
        }
        consumeTerminator("Expect newline or ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        consume(TokenType.DO, "Expect 'do' after while.");
        List<Stmt> body = block(TokenType.END);
        consume(TokenType.END, "Expect 'end' after while body.");
        return new Stmt.While(condition, new Stmt.Block(body));
    }
    
    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        if(match(TokenType.RIGHT_PAREN)){
            consume(TokenType.THEN, "Expect 'then' after if condition.");
        
            List<Stmt> thenBody = block(TokenType.ELSE, TokenType.END);
            Stmt elseBranch = null;
        
            if (match(TokenType.ELSE)) {
                List<Stmt> elseBody = block(TokenType.END);
                elseBranch = new Stmt.Block(elseBody);
            }
        
            consume(TokenType.END, "Expect 'end' after if statement.");
            return new Stmt.If(condition, new Stmt.Block(thenBody), elseBranch);
        }
        throw error(peek(), "Expect if statement or the parenthesis wasn't closed.");
    }
    
    private List<Stmt> block(TokenType... terminators) {
        List<Stmt> statements = new ArrayList<>();
        while (!check(terminators) && !isAtEnd()) {
            statements.add(declaration());
        }
       return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consumeTerminator("Expect newline or ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consumeTerminator("Expect newline or ';' after expression.");
        return new Stmt.Expression(expr);
    }
    
    // =========================================================================
    // == GRAMMAR RULES - EXPRESSIONS (THE FINAL, LAWFUL, CORRECT HIERARCHY)
    // =========================================================================
    
    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        // Assignment has the lowest precedence. It calls the next rule.
        Expr expr = logic_or();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment(); // Right-associative
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }
    
    private Expr logic_or() {
        Expr expr = logic_and(); // OR calls AND
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = logic_and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr logic_and() {
        Expr expr = equality(); // AND calls EQUALITY
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison(); // EQUALITY calls COMPARISON
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = concatenation(); // COMPARISON calls CONCATENATION
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = concatenation();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }
    
    private Expr concatenation() {
        Expr expr = addition(); // CONCATENATION calls ADDITION
        while (match(TokenType.DOT_DOT)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication(); // ADDITION calls MULTIPLICATION
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr multiplication() {
        Expr expr = exponentiation(); // MULTIPLICATION calls EXPONENTIATION
        while (match(TokenType.SLASH, TokenType.STAR, TokenType.PERCENT)) {
            Token operator = previous();
            Expr right = exponentiation();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr exponentiation() {
        Expr expr = unary(); // EXPONENTIATION calls UNARY
        while (match(TokenType.CARET)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        // Handle 'not' as a prefix operator
        if (check(TokenType.IDENTIFIER) && peek().lexeme.equals("not")) {
             advance(); // Consume the 'not'
             Token operator = new Token(TokenType.BANG, "not", null, previous().line);
             Expr right = unary();
             return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);

            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) error(peek(), "Can't have more than 255 arguments.");
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }
    
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NULL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }
    
    // =========================================================================
    // == PARSER HELPERS
    // =========================================================================

    private boolean isAtEndOfStatement() {
        return peek().type == TokenType.EOL || peek().type == TokenType.SEMICOLON || peek().type == TokenType.EOF;
    }
    private void consumeTerminator(String message) {
        if (isAtEndOfStatement()) {
            while(match(TokenType.EOL, TokenType.SEMICOLON)); // Consume all of them
            return;
        }
        throw error(peek(), message);
    }
    private boolean check(TokenType... types) {
        for (TokenType type : types) {
            if (isAtEnd()) return false;
            if (peek().type == type) return true;
        }
        return false;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        HyperScript.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUNCTION:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }
            advance();
        }
    }
}