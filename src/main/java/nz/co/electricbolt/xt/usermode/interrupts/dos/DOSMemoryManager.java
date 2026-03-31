// DOSMemoryManager.java
package nz.co.electricbolt.xt.usermode.interrupts.dos;

import nz.co.electricbolt.xt.cpu.CPU;
import nz.co.electricbolt.xt.cpu.Memory;
import nz.co.electricbolt.xt.cpu.SegOfs;

/**
 * DOS Memory Manager using MCB (Memory Control Block) chain.
 * MCB structure (16 bytes):
 * - Offset 0: byte - marker ('M' = middle block, 'Z' = last block)
 * - Offset 1-2: word - PID of owner (0 = free block)
 * - Offset 3-4: word - size in paragraphs (16-byte units)
 * - Offset 5-15: reserved (11 bytes)
 */
public class DOSMemoryManager {
    
    // MCB structure offsets
    private static final short MCB_MARKER = 0x00;
    private static final short MCB_PID = 0x01;
    private static final short MCB_SIZE = 0x03;
    private static final short MCB_RESERVED_START = 0x05;
    private static final short MCB_RESERVED_END = 0x0F;
    
    // MCB marker values
    private static final byte MCB_MIDDLE = 'M';
    private static final byte MCB_LAST = 'Z';
    
    // DOS error codes
    public static final byte ERROR_NO_ERROR = 0x00;
    public static final byte ERROR_INVALID_FUNCTION = 0x01;
    public static final byte ERROR_FILE_NOT_FOUND = 0x02;
    public static final byte ERROR_PATH_NOT_FOUND = 0x03;
    public static final byte ERROR_NO_HANDLES = 0x04;
    public static final byte ERROR_ACCESS_DENIED = 0x05;
    public static final byte ERROR_INVALID_HANDLE = 0x06;
    public static final byte ERROR_MEMORY_CONTROL_BLOCKS_DESTROYED = 0x07;
    public static final byte ERROR_INSUFFICIENT_MEMORY = 0x08;
    public static final byte ERROR_INVALID_MEMORY_BLOCK_ADDRESS = 0x09;
    public static final byte ERROR_INVALID_ENVIRONMENT = 0x0A;
    public static final byte ERROR_INVALID_FORMAT = 0x0B;
    public static final byte ERROR_INVALID_ACCESS = 0x0C;
    public static final byte ERROR_INVALID_DATA = 0x0D;
    
    private final CPU cpu;
    private short firstMCBSegment;
    private boolean initialized = false;
    
    public DOSMemoryManager(CPU cpu) {
        this.cpu = cpu;
    }
    
    /**
     * Initialize the DOS memory chain with a single free block covering all available memory
     * after the PSP up to 0xFFFFF.
     * @param pspSegment The segment of the Program Segment Prefix
     */
    public void initialize(short pspSegment) {
        // Get the memory size (1MB = 0xFFFFF+1 = 0x100000 bytes = 0x10000 paragraphs)
        int totalMemoryParagraphs = 0x10000; // 1MB in paragraphs (16-byte units)
        
        // Calculate where the PSP ends (PSP is 256 bytes = 0x10 paragraphs)
        int pspEndParagraph = (pspSegment & 0xFFFF) + 0x10;
        
        // Calculate total available paragraphs after PSP
        int availableParagraphs = totalMemoryParagraphs - pspEndParagraph;
        
        // Create the first MCB at PSP end
        firstMCBSegment = (short) pspEndParagraph;
        
        // Write MCB structure
        SegOfs mcbSegOfs = new SegOfs(firstMCBSegment, (short) 0);
        
        // Mark as last block (will be updated if we split later)
        cpu.getMemory().writeByte(new SegOfs(firstMCBSegment, MCB_MARKER), MCB_LAST);
        // PID = 0 (free block)
        cpu.getMemory().writeWord(new SegOfs(firstMCBSegment, MCB_PID), (short) 0);
        // Size in paragraphs (excluding the MCB itself)
        cpu.getMemory().writeWord(new SegOfs(firstMCBSegment, MCB_SIZE), (short) (availableParagraphs - 1));
        
        // Initialize reserved area to 0
        for (short i = MCB_RESERVED_START; i <= MCB_RESERVED_END; i++) {
            cpu.getMemory().writeByte(new SegOfs(firstMCBSegment, i), (byte) 0);
        }
        
        initialized = true;
    }
    
    /**
     * DOS Function 0x48 - Allocate Memory
     * Allocates a block of memory of specified size in paragraphs.
     * 
     * Input:
     *   BX = number of paragraphs requested
     * 
     * Output:
     *   AX = segment of allocated block (on success)
     *   BX = size of largest available block in paragraphs (on failure)
     *   Carry flag = 0 on success, 1 on failure
     *   On failure, AX = error code (0x07 or 0x08)
     */
    public void allocateMemory(CPU cpu, short paragraphsRequested) {
        if (!initialized) {
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((byte) ERROR_INVALID_FUNCTION);
            return;
        }
        
        if (paragraphsRequested <= 0) {
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((byte) ERROR_INVALID_FUNCTION);
            return;
        }
        
        // Find first free block large enough
        short currentSegment = firstMCBSegment;
        short largestBlockSize = 0;
        short bestBlockSegment = 0;
        short bestBlockSize = 0;
        
        while (true) {
            // Read MCB marker
            byte marker = cpu.getMemory().readByte(new SegOfs(currentSegment, MCB_MARKER));
            // Read PID
            short pid = cpu.getMemory().readWord(new SegOfs(currentSegment, MCB_PID));
            // Read block size (in paragraphs)
            short blockSize = cpu.getMemory().readWord(new SegOfs(currentSegment, MCB_SIZE));
            
            // If this is a free block (PID == 0)
            if (pid == 0) {
                if (blockSize >= paragraphsRequested) {
                    // Found a suitable block
                    if (bestBlockSegment == 0 || blockSize < bestBlockSize) {
                        bestBlockSegment = currentSegment;
                        bestBlockSize = blockSize;
                    }
                }
                
                // Track largest block for error reporting
                if (blockSize > largestBlockSize) {
                    largestBlockSize = blockSize;
                }
            }
            
            // Move to next MCB if not last
            if (marker == MCB_LAST) {
                break;
            }
            
            // Next MCB starts after current block + MCB
            currentSegment = (short) (currentSegment + blockSize + 1);
        }
        
        if (bestBlockSegment == 0) {
            // No block large enough
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((byte) ERROR_INSUFFICIENT_MEMORY);
            cpu.getReg().BX.setValue(largestBlockSize);
            return;
        }
        
        // Allocate the block
        short allocatedSegment = bestBlockSegment;
        short allocatedSize = bestBlockSize;
        
        if (allocatedSize > paragraphsRequested) {
            // Split the block: create a new free block after the allocated one
            short newFreeBlockSegment = (short) (allocatedSegment + paragraphsRequested + 1);
            short newFreeBlockSize = (short) (allocatedSize - paragraphsRequested - 1);
            
            // Get current marker of the original block (to determine if it was last)
            byte originalMarker = cpu.getMemory().readByte(new SegOfs(allocatedSegment, MCB_MARKER));
            
            // Update allocated block
            cpu.getMemory().writeByte(new SegOfs(allocatedSegment, MCB_MARKER), 
                (newFreeBlockSize == 0) ? originalMarker : MCB_MIDDLE);
            cpu.getMemory().writeWord(new SegOfs(allocatedSegment, MCB_SIZE), paragraphsRequested);
            
            if (newFreeBlockSize > 0) {
                // Create new MCB for free block
                cpu.getMemory().writeByte(new SegOfs(newFreeBlockSegment, MCB_MARKER), originalMarker);
                cpu.getMemory().writeWord(new SegOfs(newFreeBlockSegment, MCB_PID), (short) 0);
                cpu.getMemory().writeWord(new SegOfs(newFreeBlockSegment, MCB_SIZE), newFreeBlockSize);
                
                // Initialize reserved area
                for (short i = MCB_RESERVED_START; i <= MCB_RESERVED_END; i++) {
                    cpu.getMemory().writeByte(new SegOfs(newFreeBlockSegment, i), (byte) 0);
                }
            }
        } else {
            // Exact fit - just mark as allocated
            // Keep the same marker (M or Z)
            // No need to change marker
        }
        
        // Set owner PID (get current PSP segment - from DS or ES)
        short currentPSP = cpu.getReg().DS.getValue(); // DS typically points to PSP
        cpu.getMemory().writeWord(new SegOfs(allocatedSegment, MCB_PID), currentPSP);
        
        // Success
        cpu.getReg().flags.setCarry(false);
        cpu.getReg().AX.setValue(allocatedSegment);
    }
    
    /**
     * DOS Function 0x49 - Free Allocated Memory Block
     * Frees a previously allocated memory block.
     * 
     * Input:
     *   ES = segment of block to free
     * 
     * Output:
     *   Carry flag = 0 on success, 1 on failure
     *   On failure, AX = error code (0x07 or 0x09)
     */
    public void freeMemory(CPU cpu, short segmentToFree) {
        if (!initialized) {
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((byte) ERROR_INVALID_FUNCTION);
            return;
        }
        
        // Validate that the segment points to a valid MCB
        short currentSegment = firstMCBSegment;
        boolean found = false;
        boolean mcbChainValid = true;
        
        while (true) {
            // Check if this is the MCB we're looking for
            if (currentSegment == segmentToFree) {
                found = true;
                break;
            }
            
            byte marker = cpu.getMemory().readByte(new SegOfs(currentSegment, MCB_MARKER));
            if (marker == MCB_LAST) {
                break;
            }
            
            short blockSize = cpu.getMemory().readWord(new SegOfs(currentSegment, MCB_SIZE));
            currentSegment = (short) (currentSegment + blockSize + 1);
            
            // Check for invalid MCB chain
            if (currentSegment < 0 || currentSegment >= 0x10000) {
                mcbChainValid = false;
                break;
            }
        }
        
        if (!found || !mcbChainValid) {
            // Invalid memory block address
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((byte) ERROR_INVALID_MEMORY_BLOCK_ADDRESS);
            return;
        }
        
        // Verify that the block is owned by the current process
        short currentPSP = cpu.getReg().DS.getValue(); // DS typically points to PSP
        short blockOwner = cpu.getMemory().readWord(new SegOfs(segmentToFree, MCB_PID));
        
        if (blockOwner != currentPSP && blockOwner != 0) {
            // Block belongs to another process - MCB destroyed error
            cpu.getReg().flags.setCarry(true);
            cpu.getReg().AX.setValue((byte) ERROR_MEMORY_CONTROL_BLOCKS_DESTROYED);
            return;
        }
        
        // Mark as free
        cpu.getMemory().writeWord(new SegOfs(segmentToFree, MCB_PID), (short) 0);
        
        // Try to merge with adjacent free blocks
        mergeFreeBlocks();
        
        cpu.getReg().flags.setCarry(false);
    }
    
    /**
     * Merge adjacent free blocks to reduce fragmentation
     */
    private void mergeFreeBlocks() {
        short currentSegment = firstMCBSegment;
        
        while (true) {
            byte marker = cpu.getMemory().readByte(new SegOfs(currentSegment, MCB_MARKER));
            short pid = cpu.getMemory().readWord(new SegOfs(currentSegment, MCB_PID));
            short blockSize = cpu.getMemory().readWord(new SegOfs(currentSegment, MCB_SIZE));
            
            // Calculate next block segment
            short nextSegment = (short) (currentSegment + blockSize + 1);
            
            // Check if next block exists and is within bounds
            if (nextSegment >= 0x10000) {
                break;
            }
            
            // Read next block's PID
            short nextPid = cpu.getMemory().readWord(new SegOfs(nextSegment, MCB_PID));
            
            // If both current and next are free, merge them
            if (pid == 0 && nextPid == 0) {
                short nextBlockSize = cpu.getMemory().readWord(new SegOfs(nextSegment, MCB_SIZE));
                byte nextMarker = cpu.getMemory().readByte(new SegOfs(nextSegment, MCB_MARKER));
                
                // Merge sizes (current block size + next block size + MCB of next block)
                short newSize = (short) (blockSize + nextBlockSize + 1);
                cpu.getMemory().writeWord(new SegOfs(currentSegment, MCB_SIZE), newSize);
                cpu.getMemory().writeByte(new SegOfs(currentSegment, MCB_MARKER), nextMarker);
                
                // Continue checking from current segment (in case we can merge more)
                continue;
            }
            
            // Move to next block
            if (marker == MCB_LAST) {
                break;
            }
            
            currentSegment = nextSegment;
        }
    }
    
    /**
     * Get the segment of the first MCB
     */
    public short getFirstMCBSegment() {
        return firstMCBSegment;
    }
    
    /**
     * Check if the memory manager is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}
