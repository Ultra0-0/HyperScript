// It's good practice to put your code into a package.
// This is like a 'module' in our Blueprint language.

// We can define the TokenType enum in the same file for simplicity.

// This is the main Token class.
public class Token {
    // We use 'final' to make these properties immutable. Once a token is created, it never changes.
    final TokenType type;
    final String lexeme; // The raw text of the token, e.g., "123" or "var"
    final Object literal; // The actual value, e.g., the number 123
    final int line;

    // The constructor
    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        // We will build the new, beautiful format.
        String literalString;
        if (literal == null) {
            literalString = "null";
        } else {
            // Special handling for strings to add quotes
            if (type == TokenType.STRING) {
                literalString = "\"" + literal.toString() + "\"";
            } else {
                literalString = literal.toString();
            }
        }
        
        // This format is a little different from your proposal,
        // but it's a common and very readable professional standard.
        // TOKEN[Type=NUMBER, Lexeme='17', Literal=17.0]
        return "TOKEN[Type=" + type + ", Lexeme='" + lexeme + "', Literal=" + literalString + "]";
    }
}