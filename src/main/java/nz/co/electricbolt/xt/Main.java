// Main.java
// XT Copyright © 2025; Electric Bolt Limited.

package nz.co.electricbolt.xt;

import nz.co.electricbolt.xt.usermode.ProgramRunner;
import nz.co.electricbolt.xt.usermode.interrupts.Interrupts;
import java.util.List;
import java.util.ArrayList;

import java.io.File;

public class Main {

    private boolean traceCPU = false;
    private boolean traceInterrupt = false;
    private String traceFile = "";
    String emulatedProgramPath = "";
    String emulatedProgramArgs = "";
    String hostWorkingDir = "";
    private final CommandLineParser commandLine;
    private Long maxInstructions = null;
    private final List<Breakpoint> breakpoints = new ArrayList<>();
    private boolean traceMode = false;
    private final List<Watchpoint> watchpoints = new ArrayList<>();

    private Main(final CommandLineParser commandLine) {
        this.commandLine = commandLine;
    }

    private void printAppVersion() {
        System.out.println("XT/DW version 1.0.2; Copyright © 2026; DosWorld.");
        System.out.println("Copyright © 2025; Electric Bolt Limited.");
    }

    private void haltSyntax(final String message) {
        printAppVersion();
        System.out.println("Syntax:        xt help [run|int]");
        System.out.println("               xt run [options] program [program-args]");
        System.out.println("               xt trace [options] program [program-args]");
        System.out.println("               xt int");

        if (!message.isEmpty()) {
            System.out.println();
            System.out.println("error: " + message);
        }

        System.exit(255);
    }

    private void haltSyntaxRun(final String message) {
        printAppVersion();
        System.out.println("Syntax:        xt run [-tc -ti file] [-c dir] [--max=N] [--bp=SEG:OFS]... [--wp=SEG:OFS:type]... program [program-args]");
        System.out.println("               Run a .EXE or .COM command line MS-DOS app on your host system.");
        System.out.println("-tc -ti file = Trace CPU and/or interrupts to the tracing host file specified.");
        System.out.println("-c dir       = The host directory that will be the root of the emulated C: drive");
        System.out.println("               If not specified then the current working directory will be used.");
        System.out.println("--max=N      = Maximum number of instructions to execute before stopping");
        System.out.println("--bp=SEG:OFS = Set a breakpoint at the specified segment:offset (hex)");
        System.out.println("               Can be specified multiple times. Relative to EXE.");
        System.out.println("--wp=SEG:OFS:type = Set a watchpoint at the specified segment:offset (hex)");
        System.out.println("               type can be: r (read), w (write), a (access)");
        System.out.println("               Can be specified multiple times.");
        System.out.println("program      = The .EXE or .COM command line MS-DOS app you want to run. You can");
        System.out.println("               optionally prefix with emulated path.");
        System.out.println("program-args = Optional arguments for the MS-DOS app, max 127 characters.");
        System.out.println();
        System.out.println("The exit code will be 255 if XT terminates the program due to an error,");
        System.out.println("otherwise the exit code will be the exit code of the MS-DOS app.");

        if (!message.isEmpty()) {
            System.out.println();
            System.out.println("error: " + message);
        }

        System.exit(255);
    }

    private void haltSyntaxTrace(final String message) {
        printAppVersion();
        System.out.println("Syntax:        xt trace [--max=N] [--bp=SEG:OFS]... [--wp=SEG:OFS:type]... program [program-args]");
        System.out.println("               Trace execution of a .EXE or .COM command line MS-DOS app.");
        System.out.println("               For each instruction, displays CS:IP and all register values.");
        System.out.println("--max=N      = Maximum number of instructions to trace before stopping");
        System.out.println("--bp=SEG:OFS = Set a breakpoint at the specified segment:offset (hex)");
        System.out.println("               Can be specified multiple times. Relative to EXE.");
        System.out.println("--wp=SEG:OFS:type = Set a watchpoint at the specified segment:offset (hex)");
        System.out.println("               type can be: r (read), w (write), a (access)");
        System.out.println("               Can be specified multiple times.");
        System.out.println("program      = The .EXE or .COM command line MS-DOS app you want to trace.");
        System.out.println("program-args = Optional arguments for the MS-DOS app.");

        if (!message.isEmpty()) {
            System.out.println();
            System.out.println("error: " + message);
        }

        System.exit(255);
    }

    private void haltSyntaxInt() {
        printAppVersion();
        System.out.println("Syntax:        xt int");
        System.out.println("               Output the list of interrupts that are implemented by this");
        System.out.println("               version of XT for the run command.");
        System.exit(255);
    }

    private void run() {
        if (hostWorkingDir.isEmpty()) {
            hostWorkingDir = System.getProperty("user.dir");
        }
        if (!hostWorkingDir.endsWith(File.separator)) {
            hostWorkingDir += File.separator;
        }

        final ProgramRunner runner = new ProgramRunner(emulatedProgramPath, emulatedProgramArgs, hostWorkingDir,
                traceCPU, traceInterrupt, traceFile, breakpoints, watchpoints, maxInstructions, traceMode);
        printSettings();
        runner.loadAndExecute();
    }

    private void printSettings() {
        if (!breakpoints.isEmpty()) {
            System.out.println("Breakpoints set:");
            for (Breakpoint bp : breakpoints) {
                System.out.println("  " + bp);
            }
        }
        
        if (!watchpoints.isEmpty()) {
            System.out.println("Watchpoints set:");
            for (Watchpoint wp : watchpoints) {
                System.out.println("  " + wp);
            }
        }
        
        if (maxInstructions != null) {
            System.out.println("Maximum instructions limit: " + maxInstructions);
        }
    }

    private void parseInt() {
        final Interrupts interrupts = new Interrupts();
        interrupts.printInterrupts();
        System.exit(255);
    }

    private void parseTraceInterrupt() {
        traceInterrupt = true;
        final String argument = commandLine.next();
        if (argument == null) {
            haltSyntaxRun("expecting tracing host file argument");
        } else if (argument.startsWith("-")) {
            haltSyntaxRun(argument + " argument not recognized");
        } else {
            traceFile = argument;
        }
    }

    private void parseTraceCPU() {
        traceCPU = true;
        final String argument = commandLine.next();
        if (argument == null) {
            haltSyntaxRun("expecting -ti or tracing host file argument");
        } else if (argument.equals("-ti")) {
            parseTraceInterrupt();
        } else if (argument.startsWith("-")) {
            haltSyntaxRun(argument + " argument not recognized");
        } else {
            traceFile = argument;
        }
    }

    private void parseTraceOptions() {
        final String argument = commandLine.next();
        if (argument.equals("-tc")) {
            parseTraceCPU();
        } else if (argument.equals("-ti")) {
            parseTraceInterrupt();
        } else {
            haltSyntaxRun(argument + " argument not recognized");
        }
    }

    private void parseRootDirectory() {
        commandLine.next(); // skip over -c argument.
        String argument = commandLine.next();
        if (argument == null) {
            haltSyntaxRun("expecting host root directory argument");
        } else if (argument.startsWith("-")) {
            haltSyntaxRun("expecting host root directory argument");
        } else {
            hostWorkingDir = argument;
        }
    }

    private void parseRun() {
        if (!commandLine.hasNext()) {
            haltSyntaxRun("expecting program argument");
        }

        // Parse double dash options (--max, --bp) first
        parseDoubleDashOptions();

        // Parse trace options (-tc, -ti)
        if (commandLine.hasNext()) {
            String argument = commandLine.peek();
            if (argument != null && argument.startsWith("-t")) {
                parseTraceOptions();
            }
        }

        // Parse root directory option (-c)
        if (commandLine.hasNext()) {
            String argument = commandLine.peek();
            if (argument != null && argument.equals("-c")) {
                parseRootDirectory();
            }
        }

        // Parse any remaining double dash options that might appear after -c
        parseDoubleDashOptions();

        // Program (mandatory)
        if (!commandLine.hasNext()) {
            haltSyntaxRun("expecting program argument");
        }
        
        String argument = commandLine.next();
        if (argument.startsWith("-")) {
            haltSyntaxRun(argument + " not recognized");
        } else {
            emulatedProgramPath = argument;
        }

        // Program args (optional)
        final StringBuilder buf = new StringBuilder();
        while (commandLine.hasNext()) {
            if (!buf.isEmpty()) {
                buf.append(' ');
            }
            buf.append(commandLine.next());
        }
        emulatedProgramArgs = buf.toString();

        run();

        System.exit(255);
    }

    private boolean parseWatchpointOption() {
        String argument = commandLine.peek();
        if (argument != null && argument.startsWith("--wp=")) {
            commandLine.next(); // consume the argument
            String wpStr = argument.substring(5); // remove "--wp="
            String[] parts = wpStr.split(":");
            if (parts.length == 3) {
                try {
                    int segment = Integer.parseInt(parts[0], 16);
                    int offset = Integer.parseInt(parts[1], 16);
                    String typeStr = parts[2].toLowerCase();
                    
                    if (segment < 0 || segment > 0xFFFF) {
                        haltSyntaxRun("Segment must be between 0 and 0xFFFF");
                    }
                    if (offset < 0 || offset > 0xFFFF) {
                        haltSyntaxRun("Offset must be between 0 and 0xFFFF");
                    }
                    
                    Watchpoint.Type type;
                    switch (typeStr) {
                        case "r":
                        case "read":
                            type = Watchpoint.Type.READ;
                            break;
                        case "w":
                        case "write":
                            type = Watchpoint.Type.WRITE;
                            break;
                        case "a":
                        case "access":
                            type = Watchpoint.Type.ACCESS;
                            break;
                        default:
                            haltSyntaxRun("Invalid watchpoint type: " + typeStr + ". Use r/w/a");
                            return false;
                    }
                    
                    watchpoints.add(new Watchpoint((short) segment, (short) offset, type));
                    return true;
                } catch (NumberFormatException e) {
                    haltSyntaxRun("Invalid watchpoint format: " + wpStr + ". Expected format: SEG:OFS:type");
                }
            } else {
                haltSyntaxRun("Invalid watchpoint format: " + wpStr + ". Expected format: SEG:OFS:type (r/w/a)");
            }
        }
        return false;
    }

    private void parseTrace() {
        if (!commandLine.hasNext()) {
            haltSyntaxTrace("expecting program argument");
        }
        
        traceMode = true;
        
        // Parse double dash options (--max, --bp) for trace mode
        parseDoubleDashOptions();

        // Program (mandatory)
        if (!commandLine.hasNext()) {
            haltSyntaxTrace("expecting program argument");
        }
        
        String argument = commandLine.next();
        if (argument.startsWith("-")) {
            haltSyntaxTrace(argument + " not recognized");
        } else {
            emulatedProgramPath = argument;
        }

        // Program args (optional)
        final StringBuilder buf = new StringBuilder();
        while (commandLine.hasNext()) {
            if (!buf.isEmpty()) {
                buf.append(' ');
            }
            buf.append(commandLine.next());
        }
        emulatedProgramArgs = buf.toString();

        
        run();
        
        System.exit(255);
    }

    private void parseHelp() {
        if (commandLine.hasNext()) {
            final String argument = commandLine.next();
            switch (argument) {
                case "run" -> haltSyntaxRun("");
                case "trace" -> haltSyntaxTrace("");
                case "int" -> haltSyntaxInt();
                default -> haltSyntax(argument + " not recognized");
            }
        }
        haltSyntax("");
    }

    private boolean parseMaxOption() {
        String argument = commandLine.peek();
        if (argument != null && argument.startsWith("--max=")) {
            commandLine.next(); // consume the argument
            try {
                String value = argument.substring(6); // remove "--max="
                maxInstructions = Long.parseLong(value);
                if (maxInstructions <= 0) {
                    if (traceMode) {
                        haltSyntaxTrace("--max value must be positive");
                    } else {
                        haltSyntaxRun("--max value must be positive");
                    }
                }
                return true;
            } catch (NumberFormatException e) {
                if (traceMode) {
                    haltSyntaxTrace("Invalid --max value: " + argument.substring(6));
                } else {
                    haltSyntaxRun("Invalid --max value: " + argument.substring(6));
                }
            }
        }
        return false;
    }

    private boolean parseBreakpointOption() {
        String argument = commandLine.peek();
        if (argument != null && argument.startsWith("--bp=")) {
            commandLine.next(); // consume the argument
            String bpStr = argument.substring(5); // remove "--bp="
            String[] parts = bpStr.split(":");
            if (parts.length == 2) {
                try {
                    int segment = Integer.parseInt(parts[0], 16);
                    int offset = Integer.parseInt(parts[1], 16);
                    
                    if (segment < 0 || segment > 0xFFFF) {
                        haltSyntaxRun("Segment must be between 0 and 0xFFFF");
                    }
                    if (offset < 0 || offset > 0xFFFF) {
                        haltSyntaxRun("Offset must be between 0 and 0xFFFF");
                    }
                    
                    breakpoints.add(new Breakpoint((short) segment, (short) offset));
                    return true;
                } catch (NumberFormatException e) {
                    haltSyntaxRun("Invalid breakpoint format: " + bpStr + ". Expected format: SEG:OFFSET in hex");
                }
            } else {
                haltSyntaxRun("Invalid breakpoint format: " + bpStr + ". Expected format: SEG:OFFSET");
            }
        }
        return false;
    }

    private void parseDoubleDashOptions() {
        while (commandLine.hasNext()) {
            String argument = commandLine.peek();
            if (argument == null || !argument.startsWith("--")) {
                break;
            }
            if (argument.startsWith("--max=")) {
                parseMaxOption();
            } else if (argument.startsWith("--bp=")) {
                parseBreakpointOption();
            } else if (argument.startsWith("--wp=")) {
                parseWatchpointOption();
            } else {
                if (traceMode) {
                    haltSyntaxTrace(argument + " option not recognized");
                } else {
                    haltSyntaxRun(argument + " option not recognized");
                }
            }
        }
    }

    private void parse() {
        if (commandLine.hasNext()) {
            final String argument = commandLine.next();
            switch (argument) {
                case "help" -> parseHelp();
                case "int" -> parseInt();
                case "run" -> parseRun();
                case "trace" -> parseTrace();
                default -> haltSyntax(argument + " not recognized");
            }
        }
        haltSyntax("");
    }

    public static void main(String[] args) {
        final Main main = new Main(new CommandLineParser(args));
        main.parse();
    }
}
