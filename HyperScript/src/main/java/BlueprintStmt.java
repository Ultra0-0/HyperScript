// In src/main/java/BlueprintStmt.java

import java.util.List;

/**
 * This file defines the Abstract Syntax Tree (AST) nodes for the Blueprint language.
 * Each class represents a specific grammatical construct, like a Class or a Component.
 * The entire system uses the Visitor pattern for processing, which allows us to
 * cleanly separate the tree structure from the operations we perform on it (like
 * printing, semantic analysis, or code generation).
 */
abstract class BlueprintStmt {
    // The Visitor interface. Any class that wants to process the Blueprint AST
    // must implement this interface, providing a method for every type of statement.
    interface Visitor<R> {
        R visitSectorStmt(SectorStmt stmt);
        R visitClassStmt(ClassStmt stmt);
        R visitFragmentStmt(FragmentStmt stmt);
        R visitComponentStmt(ComponentStmt stmt);
        R visitRoleStmt(RoleStmt stmt);
    }

    // The 'accept' method is the entry point for the Visitor pattern.
    abstract <R> R accept(Visitor<R> visitor);

    // =========================================================================
    // == HELPER / SUB-NODE CLASSES
    // =========================================================================

    /** Represents a single field declaration, like 'health: int' or 'Position: transform'. */
    static class Field {
        final Token type;
        final Token name;

        Field(Token type, Token name) {
            this.type = type;
            this.name = name;
        }
    }

    /** Represents a single attachment, like 'Flow: Controls'. */
    static class Attachment {
        final Token type; // e.g., 'Flow', 'Manifest'
        final Token path; // e.g., 'Controls'

        Attachment(Token type, Token path) {
            this.type = type;
            this.path = path;
        }
    }

    // =========================================================================
    // == PRIMARY AST NODE CLASSES (STATEMENTS)
    // =========================================================================

    /** Represents a top-level 'Sector Main { ... }' block. */
    static class SectorStmt extends BlueprintStmt {
        final Token name;
        final List<BlueprintStmt> declarations;

        SectorStmt(Token name, List<BlueprintStmt> declarations) {
            this.name = name;
            this.declarations = declarations;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSectorStmt(this);
        }
    }

    /** Represents a 'Class Clock { ... }' declaration. */
    static class ClassStmt extends BlueprintStmt {
        final Token name;
        final List<Field> attributes;
        final List<Attachment> attachments;
        final List<FragmentStmt> fragments;

        ClassStmt(Token name, List<Field> attributes, List<Attachment> attachments, List<FragmentStmt> fragments) {
            this.name = name;
            this.attributes = attributes;
            this.attachments = attachments;
            this.fragments = fragments;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }
    }

    /** Represents a nested 'Fragment Hand { ... }' declaration inside a Class. */
    static class FragmentStmt extends BlueprintStmt {
        final Token name;
        final List<Field> attributes;
        final List<Attachment> attachments;
        
        FragmentStmt(Token name, List<Field> attributes, List<Attachment> attachments) {
            this.name = name;
            this.attributes = attributes;
            this.attachments = attachments;
        }
        
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFragmentStmt(this);
        }
    }

    /** Represents a 'component Vector3 { ... }' declaration. */
    static class ComponentStmt extends BlueprintStmt {
        final Token name;
        final List<Field> fields;
        // Properties and inline Flow attachments will be added here later.

        ComponentStmt(Token name, List<Field> fields) {
            this.name = name;
            this.fields = fields;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitComponentStmt(this);
        }
    }

    /** Represents a 'role Position is Vector3' declaration. */
    static class RoleStmt extends BlueprintStmt {
        final Token name;
        final Token isKeyword; // The 'is' token, useful for error reporting
        final Token underlyingType;

        RoleStmt(Token name, Token isKeyword, Token underlyingType) {
            this.name = name;
            this.isKeyword = isKeyword;
            this.underlyingType = underlyingType;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRoleStmt(this);
        }
    }
}