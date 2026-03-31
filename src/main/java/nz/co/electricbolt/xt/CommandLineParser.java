// CommandLineParser.java
// XT Copyright © 2025; Electric Bolt Limited.

package nz.co.electricbolt.xt;

import java.util.List;
import java.util.ArrayList;

public class CommandLineParser {

    private final String[] args;
    private int index;

    public CommandLineParser(final String[] args) {
        this.args = args;
        this.index = 0;
    }

    public boolean hasNext() {
        return index < args.length;
    }

    public String peek() {
        if (hasNext()) {
            return args[index];
        } else {
            return null;
        }
    }

    public String next() {
        if (hasNext()) {
            return args[index++];
        } else {
            return null;
        }
    }

   public List<Breakpoint> parseBreakpoints() {
        List<Breakpoint> breakpoints = new ArrayList<>();
        
        while (hasNext()) {
            String arg = peek();
            if (arg.startsWith("--bp=")) {
                next(); // consume the argument
                String bpStr = arg.substring(5); // remove "--bp="
                String[] parts = bpStr.split(":");
                if (parts.length == 2) {
                    try {
                        int segment = Integer.parseInt(parts[0], 16);
                        int offset = Integer.parseInt(parts[1], 16);
                        breakpoints.add(new Breakpoint((short) segment, (short) offset));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid breakpoint format: " + bpStr + ". Expected format: SEG:OFFSET (hex)");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Invalid breakpoint format: " + bpStr + ". Expected format: SEG:OFFSET");
                    System.exit(1);
                }
            } else {
                break;
            }
        }
        
        return breakpoints;
    }

    public Long parseMaxInstructions() {
        if (hasNext()) {
            String arg = peek();
            if (arg.startsWith("--max=")) {
                next(); // consume the argument
                try {
                    return Long.parseLong(arg.substring(6));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid max instructions value: " + arg.substring(6));
                    System.exit(1);
                }
            }
        }
        return null;
    }
}
