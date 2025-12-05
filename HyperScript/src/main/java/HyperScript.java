// In src/main/java/HyperScript.java

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// In HyperScript.java
public class HyperScript {
    static final Interpreter interpreter = new Interpreter(); // One interpreter instance
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        // In main()
        runProject("project", "Main.hbp");
        System.out.println("\n--- Next File ---\n");
        runProject("project", "Clock.hbp");
    }

    // --- The Hunter Method ---
    private static void runProject(String dir, String entryFile) throws IOException {
        System.out.println("--- [HyperScript Engine Started] ---");
        System.out.println("Scanning directory: " + dir);

        // This is a modern Java way to find all files in a directory.
        List<Path> files;
        try (Stream<Path> stream = Files.walk(Paths.get(dir))) {
            files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        System.out.println("Found " + files.size() + " files.");
        
        // Find our specific entry point file.
        Path entryPointPath = Paths.get(dir, entryFile);

        if (!files.contains(entryPointPath)) {
            System.out.println("FATAL: Entry point file not found: " + entryFile);
            System.exit(1);
        }

        System.out.println("Executing entry point: " + entryPointPath);
        runFile(entryPointPath.toString());
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        
        // --- THE DISPATCHER ---
        if (path.endsWith(".hfl")) {
            runFlow(new String(bytes, Charset.defaultCharset()));
        } else if (path.endsWith(".hbp")) {
            runBlueprint(new String(bytes, Charset.defaultCharset()));
        }

        // Check if a syntax error was found and exit.
        if (hadError) System.exit(65);
        // Check if a runtime error was found and exit.
        if (hadRuntimeError) System.exit(70);
    }

    // =========================================================================
    // == FLOW PIPELINE
    // =========================================================================
    private static void runFlow(String source) {
        // --- STAGE 1: LEXER ---
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        // --- PIPE WINDOW 1: THE TOKEN STREAM ---
        System.out.println("--- Tokens ---");
        for (Token token : tokens) {
            System.out.println(token);
        }
        
        // --- STAGE 2: PARSER ---
        FlowParser parser = new FlowParser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        // --- PIPE WINDOW 2: THE AST ---
        System.out.println("\n--- AST ---");
        System.out.println(new AstPrinter().print(statements));

        // --- STAGE 3: INTERPRETER ---
        System.out.println("\n--- Execution ---");
        interpreter.interpret(statements);
    }
    
    private static void runBlueprint(String source) {
        // --- STAGE 1: LEXER ---
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // --- PIPE WINDOW 1: THE TOKEN STREAM ---
        System.out.println("--- Blueprint Tokens ---");
        for (Token token : tokens) {
            System.out.println(token);
        }

        // --- STAGE 2: PARSER ---
        BlueprintParser parser = new BlueprintParser(tokens);
        List<BlueprintStmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        // --- PIPE WINDOW 2: THE AST ---
        System.out.println("\n--- Blueprint AST ---");
        System.out.println(new BlueprintAstPrinter().print(statements));
    }
    
    // The old, simple error reporter (still useful for the Scanner)
    static void error(int line, String message) {
        report(line, "", message);
    }

    // --- THE FIX ---
    // The new, powerful, token-aware error reporter that the Parser needs.
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(Interpreter.RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        // Using System.err to separate error messages from normal output.
        System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}

