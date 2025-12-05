// In src/main/java/BlueprintParser.java

import java.util.ArrayList;
import java.util.List;

// We use the explicit, non-static import for architectural purity.
// Note: You must add all the needed keywords to TokenType.java,
// for example: SECTOR, CLASS, FRAGMENT, ATTRIBUTES, ATTACHMENTS,
// PROPERTIES, COMPONENT, ROLE, IS, CRUNCH, FLOW.

class BlueprintParser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    BlueprintParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<BlueprintStmt> parse() {
        List<BlueprintStmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            if (match(TokenType.EOL)) continue;
            // The top level is a list of declarations.
            statements.add(declaration());
        }
        return statements;
    }

    // =========================================================================
    // == GRAMMAR RULES - TOP LEVEL
    // =========================================================================

    // --- THE FINAL, CORRECT sectorDeclaration ---
    private BlueprintStmt.SectorStmt sectorDeclaration() {
        consume(TokenType.SECTOR, "Expect 'Sector' keyword.");
        Token name = consume(TokenType.IDENTIFIER, "Expect Sector name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' after Sector name.");
        
        List<BlueprintStmt> declarations = new ArrayList<>();
        
        // --- THE FIX: An intelligent loop ---
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            // If we see any empty lines, just skip them and continue the loop.
            if (match(TokenType.EOL)) continue;
            
            // Once we're past the empty lines, we parse the next real declaration.
            declarations.add(declaration());
        }
        
        consume(TokenType.RIGHT_BRACE, "Expect '}' after Sector body.");
        return new BlueprintStmt.SectorStmt(name, declarations);
    }
    

    private BlueprintStmt declaration() {
        try {
            if (check(TokenType.SECTOR)) return sectorDeclaration();
            if (match(TokenType.COMPONENT)) return componentDeclaration();
            if (match(TokenType.ROLE)) return roleDeclaration();
            if (match(TokenType.CLASS)) return classDeclaration();
        } catch (ParseError error) {
            synchronize(); // Attempt to recover and continue parsing
            return null;
        }
        throw error(peek(), "Expect a component, role, or class declaration.");
    }

    // =========================================================================
    // == GRAMMAR RULES - DECLARATIONS
    // =========================================================================

    private BlueprintStmt.ComponentStmt componentDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect component name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' after component name.");
        
        List<BlueprintStmt.Field> fields = new ArrayList<>();
        
        // The new, intelligent loop.
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            // First, handle all empty lines.
            if (match(TokenType.EOL)) continue;
    
            // Now, check for our special blocks.
            if (match(TokenType.CRUNCH)) {
                consume(TokenType.LEFT_PAREN, "Expect '(' after 'crunch'.");
                // Consume everything until we find the closing parenthesis.
                while (!check(TokenType.RIGHT_PAREN) && !isAtEnd()) {
                    advance(); // Just eat the tokens for now.
                }
                consume(TokenType.RIGHT_PAREN, "Expect ')' after crunch block.");
                continue; // Go to the next item in the component body.
            }
    
            if (match(TokenType.PROPERTIES, TokenType.FLOW)) {
                consume(TokenType.LEFT_BRACE, "Expect '{' after block keyword.");
                // Consume everything until we find the closing brace.
                while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    advance(); // Just eat the tokens for now.
                }
                consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
                continue;
            }
    
            // If it's not a special block, it MUST be a field declaration.
            Token type = consume(TokenType.IDENTIFIER, "Expect type name for field.");
            consume(TokenType.COLON, "Expect ':' after type name.");
            Token fieldName = consume(TokenType.IDENTIFIER, "Expect field name.");
            fields.add(new BlueprintStmt.Field(type, fieldName));
    
            // Fields can be separated by commas or newlines.
            match(TokenType.COMMA);
        }
        
        consume(TokenType.RIGHT_BRACE, "Expect '}' after component body.");
        return new BlueprintStmt.ComponentStmt(name, fields);
    }
    
    private BlueprintStmt.RoleStmt roleDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect role name.");
        Token isKeyword = consume(TokenType.IS, "Expect 'is' after role name.");
        Token underlyingType = consume(TokenType.IDENTIFIER, "Expect component type for role.");
        return new BlueprintStmt.RoleStmt(name, isKeyword, underlyingType);
    }

    private BlueprintStmt.ClassStmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' after class name.");
        
        List<BlueprintStmt.Field> attributes = new ArrayList<>();
        List<BlueprintStmt.Attachment> attachments = new ArrayList<>();
        List<BlueprintStmt.FragmentStmt> fragments = new ArrayList<>();
        match(TokenType.EOL);
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.ATTRIBUTES)) attributes = attributesBlock();
            else if (match(TokenType.ATTACHMENTS)) attachments = attachmentsBlock();
            else if (match(TokenType.FRAGMENT)) fragments.add(fragmentDeclaration());
            else if (match(TokenType.EOL)) continue;
            else throw error(peek(), "Expect 'Attributes', 'Attachments', or 'Fragment' inside Class.");
        }
        
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
        return new BlueprintStmt.ClassStmt(name, attributes, attachments, fragments);
    }

    private BlueprintStmt.FragmentStmt fragmentDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect fragment name.");
        consume(TokenType.LEFT_BRACE, "Expect '{' after fragment name.");

        List<BlueprintStmt.Field> attributes = new ArrayList<>();
        List<BlueprintStmt.Attachment> attachments = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.ATTRIBUTES)) attributes = attributesBlock();
            else if (match(TokenType.ATTACHMENTS)) attachments = attachmentsBlock();
            else if (match(TokenType.EOL)) continue;
            else throw error(peek(), "Expect 'Attributes' or 'Attachments' inside Fragment.");
        }
        
        consume(TokenType.RIGHT_BRACE, "Expect '}' after fragment body.");
        return new BlueprintStmt.FragmentStmt(name, attributes, attachments);
    }

    // =========================================================================
    // == GRAMMAR RULES - BLOCKS
    // =========================================================================

    private List<BlueprintStmt.Field> attributesBlock() {
        consume(TokenType.LEFT_BRACE, "Expect '{' after 'Attributes'.");
        List<BlueprintStmt.Field> attributes = new ArrayList<>();
        match(TokenType.EOL);
        if (!check(TokenType.RIGHT_BRACE)) {
            do {
                if (check(TokenType.RIGHT_BRACE)) break;
                Token type = consume(TokenType.IDENTIFIER, "Expect a type name.");
                consume(TokenType.COLON, "Expect ':' after type.");
                Token name = consume(TokenType.IDENTIFIER, "Expect attribute instance name.");
                attributes.add(new BlueprintStmt.Field(type, name));
                
            } while (match(TokenType.COMMA) || match(TokenType.EOL));
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after attributes list.");
        return attributes;
    }

    private Token parsePath() {
        Token name = consume(TokenType.IDENTIFIER, "Expect a filename.");
        
        // This is a simple lookahead. If we see a '.', we know there's an extension.
        if (match(TokenType.DOT)) {
            Token extension = consume(TokenType.IDENTIFIER, "Expect a file extension.");
            // For now, we will just return the full name as the lexeme.
            // A more advanced parser would create a special Path object.
            String fullPath = name.lexeme + "." + extension.lexeme;
            return new Token(TokenType.STRING, fullPath, fullPath, name.line);
        }
        
        // If there was no dot, just return the original identifier.
        return name;
    }
    
    private List<BlueprintStmt.Attachment> attachmentsBlock() {
        consume(TokenType.LEFT_BRACE, "Expect '{' after 'Attachments'.");
        List<BlueprintStmt.Attachment> attachments = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.EOL)) continue;
    
            // It can be MANIFEST, FLOW, or BLUEPRINT
            Token type;
            if (match(TokenType.MANIFEST, TokenType.FLOW, TokenType.BLUEPRINT)) {
                type = previous();
            } else {
                throw error(peek(), "Expect attachment type ('Manifest', 'Flow', 'Blueprint').");
            }
            
            consume(TokenType.COLON, "Expect ':' after attachment type.");
            
            // --- THE FIX: Call our new path parser ---
            Token path = parsePath();
            
            attachments.add(new BlueprintStmt.Attachment(type, path));
        }
        
        consume(TokenType.RIGHT_BRACE, "Expect '}' after attachments list.");
        return attachments;
    }

    // =========================================================================
    // == PARSER HELPERS
    // =========================================================================

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
            // For Blueprint, a good synchronization point is the start of a new declaration.
            switch (peek().type) {
                case CLASS:
                case COMPONENT:
                case ROLE:
                case SECTOR:
                    return;
                default:
                    break;
            }
            advance();
        }
    }
}