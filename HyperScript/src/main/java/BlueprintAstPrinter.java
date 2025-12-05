// In src/main/java/BlueprintAstPrinter.java

import java.util.List;

// The AstPrinter is now a complete visitor for our entire Blueprint AST.
class BlueprintAstPrinter implements BlueprintStmt.Visitor<String> {

    String print(List<BlueprintStmt> statements) {
        StringBuilder builder = new StringBuilder();
        for (BlueprintStmt statement : statements) {
            if (statement != null) {
                builder.append(statement.accept(this)).append("\n");
            }
        }
        return builder.toString();
    }

    // --- Visitor Methods ---

    @Override
    public String visitSectorStmt(BlueprintStmt.SectorStmt stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(Sector " + stmt.name.lexeme);
        for (BlueprintStmt declaration : stmt.declarations) {
            builder.append("\n  ").append(declaration.accept(this).replaceAll("\n", "\n  "));
        }
        builder.append("\n)");
        return builder.toString();
    }

    @Override
    public String visitClassStmt(BlueprintStmt.ClassStmt stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(Class " + stmt.name.lexeme);
        
        if (stmt.attributes != null && !stmt.attributes.isEmpty()) {
            builder.append("\n  (Attributes");
            for (BlueprintStmt.Field attr : stmt.attributes) {
                builder.append("\n    (" + attr.type.lexeme + ": " + attr.name.lexeme + ")");
            }
            builder.append(" )");
        }

        if (stmt.attachments != null && !stmt.attachments.isEmpty()) {
            builder.append("\n  (Attachments");
            for (BlueprintStmt.Attachment attach : stmt.attachments) {
                builder.append("\n    (" + attach.type.lexeme + ": " + attach.path.lexeme + ")");
            }
            builder.append(" )");
        }

        if (stmt.fragments != null && !stmt.fragments.isEmpty()) {
            for (BlueprintStmt.FragmentStmt fragment : stmt.fragments) {
                builder.append("\n  ").append(fragment.accept(this).replaceAll("\n", "\n  "));
            }
        }

        builder.append("\n)");
        return builder.toString();
    }
    
    @Override
    public String visitFragmentStmt(BlueprintStmt.FragmentStmt stmt) {
        // This is almost identical to visitClassStmt, just with a different keyword.
        StringBuilder builder = new StringBuilder();
        builder.append("(Fragment " + stmt.name.lexeme);

        if (stmt.attributes != null && !stmt.attributes.isEmpty()) {
            builder.append("\n  (Attributes");
            for (BlueprintStmt.Field attr : stmt.attributes) {
                builder.append("\n    (" + attr.type.lexeme + ": " + attr.name.lexeme + ")");
            }
            builder.append(" )");
        }

        if (stmt.attachments != null && !stmt.attachments.isEmpty()) {
            builder.append("\n  (Attachments");
            for (BlueprintStmt.Attachment attach : stmt.attachments) {
                builder.append("\n    (" + attach.type.lexeme + ": " + attach.path.lexeme + ")");
            }
            builder.append(" )");
        }

        builder.append("\n)");
        return builder.toString();
    }

    @Override
    public String visitComponentStmt(BlueprintStmt.ComponentStmt stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(component " + stmt.name.lexeme);
        if (stmt.fields != null && !stmt.fields.isEmpty()) {
            for (BlueprintStmt.Field field : stmt.fields) {
                builder.append("\n  (" + field.type.lexeme + ": " + field.name.lexeme + ")");
            }
        }
        builder.append(" )");
        return builder.toString();
    }
    
    @Override
    public String visitRoleStmt(BlueprintStmt.RoleStmt stmt) {
        return "(role " + stmt.name.lexeme + " is " + stmt.underlyingType.lexeme + ")";
    }
}