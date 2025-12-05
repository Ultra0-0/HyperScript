// In src/main/java/Scanner.java

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// NO 'package' declaration. We live in the default package.

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",     TokenType.AND);
        keywords.put("Class",   TokenType.CLASS);
        keywords.put("Attributes", TokenType.ATTRIBUTES);
        keywords.put("Attachments", TokenType.ATTACHMENTS);
        keywords.put("else",    TokenType.ELSE);
        keywords.put("end",     TokenType.END);
        keywords.put("false",   TokenType.FALSE);
        keywords.put("for",     TokenType.FOR);
        keywords.put("function",TokenType.FUNCTION);
        keywords.put("if",      TokenType.IF);
        keywords.put("then",    TokenType.THEN);
        keywords.put("null",    TokenType.NULL);
        keywords.put("or",      TokenType.OR);
        keywords.put("print",   TokenType.PRINT);
        keywords.put("return",  TokenType.RETURN);
        keywords.put("super",   TokenType.SUPER);
        keywords.put("this",    TokenType.THIS);
        keywords.put("true",    TokenType.TRUE);
        keywords.put("let",     TokenType.VAR);
        keywords.put("while",   TokenType.WHILE);
        keywords.put("do",      TokenType.DO);

        // --- Blueprint Keywords (CAPITALIZED & lowercase) ---
        keywords.put("Sector",      TokenType.SECTOR);
        keywords.put("Class",       TokenType.CLASS);
        keywords.put("Fragment",    TokenType.FRAGMENT);
        keywords.put("Attributes",  TokenType.ATTRIBUTES);
        keywords.put("Attachments", TokenType.ATTACHMENTS);
        keywords.put("properties",  TokenType.PROPERTIES);
        keywords.put("Flow",        TokenType.FLOW); // The keyword 'Flow'
        keywords.put("Manifest",    TokenType.MANIFEST); // The keyword 'Manifest'
        keywords.put("Blueprint",   TokenType.BLUEPRINT); // The keyword 'Blueprint'
        
        keywords.put("component",   TokenType.COMPONENT);
        keywords.put("role",        TokenType.ROLE);
        keywords.put("is",          TokenType.IS);
        keywords.put("crunch",      TokenType.CRUNCH);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(match('.') ? TokenType.DOT_DOT : TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENT); break;
            case '^': addToken(TokenType.CARET); break;
            case ':': addToken(TokenType.COLON); break;
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case ' ': case '\r': case '\t': break;
            case '\n': 
                line++;
                addToken(TokenType.EOL);
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    HyperScript.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            // HyperScript.error(line, "Unterminated string.");
            return;
        }
        advance(); // The closing ".
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}