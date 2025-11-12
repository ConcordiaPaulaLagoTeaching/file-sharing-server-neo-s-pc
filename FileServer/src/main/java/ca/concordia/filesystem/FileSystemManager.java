package ca.concordia.filesystem;

//Operations: CREATE, WRITE, READ, DELETE files
//Synchronization so multiple clients can access the file system without data corruption

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager implements Serializable{
    private static final long serialVersionUID = 1L;    //Serialization so if changes occur in code, it won't break deserialization.

    private final int MAXFILES = 5;     // Max number of files
    private final int MAXBLOCKS = 10;   // Max number of blocks
    private final static FileSystemManager instance = null;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();   // Global lock for synchronizing access

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        if(instance == null) {
                                            //TODO Initialize the file system 
            //Initialize file system
            this.inodeTable = new FEntry[MAXFILES];
            this.freeBlockList = new boolean[MAXBLOCKS];

            //Initialize inodes and all free blocks
            for (int i = 0; i < MAXFILES; i++) {
                inodeTable[i] = new FEntry();
            }
            for (int i = 0; i < MAXBLOCKS; i++) {
                freeBlockList[i] = true;    // true means block is free
            }
            
            //Create or open the disk file
            this.disk = new RandomAccessFile(filename, "rw");

            //Set the file size
            disk.setLength(totalSize);

            System.out.println("File System initialized with: " + MAXFILES + " files and " + MAXBLOCKS + " blocks.");

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    //--CREATE FILE--

    public void createFile(String fileName) throws Exception {

        globalLock.lock();  // Lock to prevent concurent modsifications
        try {
            //Check filename validity
            if(fileName == null || fileName.isEmpty() || fileName.length() > 11) {
                throw new IllegalArgumentException("Filename cannot be null, empty, or longer than 11 characters.");
            }

            //Check file name exists(no copy)
            for (FEntry entry : inodeTable) {
                if (entry.isUsed() && fileName.equals(entry.getFilename())) {
                    throw new Exception("ERROR: File with the same name already exists!");
                }
            }

            //Find free entry slot
            int freeIndex = -1;
            for (int i =0; i < MAXFILES; i++) {
                if (!inodeTable[i].isUsed()) {
                    freeIndex = i;
                    break;
                }
            }

            if (freeIndex == -1) {
                throw new Exception("ERROR: Maximum file limit reached. Cannot create more files!");
            }

            //Create new file entry
            inodeTable[freeIndex] = new FEntry(fileName, (short)0, (short)-1);
            System.out.println("File '" + fileName + "' created successfully.");
        } finally {
            globalLock.unlock();    //Unlock
        }
    }

    //--WRITE FILE--
    public void writeFile(String fileName, String data) throws Exception {

        globalLock.lock();  // Lock to prevent concurent modifications
        try {
            //Find file entry
            FEntry fileEntry = null;
            for(FEntry entry : inodeTable) {
                if(entry.isUsed() && fileName.equals(entry.getFilename())) {
                    fileEntry = entry;
                    break;
                }
            }

            if(fileEntry == null) {
                throw new Exception("ERROR: File not found!");
            }

            //Convert data to bytes
            byte[] dataBytes = data.getBytes();
            int requiredBlocks = (int) Math.ceil((double)dataBytes.length / BLOCK_SIZE);

            //Check for free blocks
            int freeBlocks = 0;
            for(boolean block : freeBlockList) {
                if(block) freeBlocks++;
            }
            if(requiredBlocks > freeBlocks) {
                throw new Exception("ERROR: Not enough free blocks to write data!");
            }

            //Write data to blocks
            int offset = 0;
            for(int i = 0; i < MAXBLOCKS && offset < dataBytes.length; i++) {
                if(freeBlockList[i]) {
                    freeBlockList[i] = false;   // Mark block as used
                    disk.seek(i * BLOCK_SIZE);
                    int bytesToWrite = Math.min(BLOCK_SIZE, dataBytes.length - offset);
                    disk.write(dataBytes, offset, bytesToWrite);
                    offset += bytesToWrite;
                }
            }

            //Update file entry
            fileEntry.setFilesize((short)dataBytes.length);
            System.out.println("Data written to file '" + fileName + "' successfully.");
        } finally {
            globalLock.unlock();    //Unlock
        }
    }

     //--READ FILE--

     public String readFile(String fileName) throws Exception {
        globalLock.lock();      // Lock to prevent concurent modifications
        try {
            //Find file entry
            FEntry fileEntry = null;
            for(FEntry entry : inodeTable) {
                if(entry.isUsed() && fileName.equals(entry.getFilename())) {
                    fileEntry = entry;
                    break;
                }
            }

            if(fileEntry == null) {
                throw new Exception("ERROR: File not found!");
            }

            //Read data from blocks
            byte[] readbytes = new byte[fileEntry.getFilesize()];
            disk.seek(0);
            disk.read(readbytes);
            return new String(readbytes).trim();
        } finally {
            globalLock.unlock();    //Unlock
        }
    }

    //--DELETE FILE--
    public void deleteFile(String fileName) throws Exception {
        globalLock.lock();      // Lock to prevent concurent modifications
        try {
            for (FEntry entry : inodeTable) {
                if (entry.isUsed() && fileName.equals(entry.getFilename())) {
                    entry.clear();
                    System.out.println("File '" + fileName + "' deleted successfully.");
                    return;
                }
            }
            throw new Exception("ERROR: File not found!");
        } finally {
            globalLock.unlock();    //Unlock
        }
    }

     //--LIST FILE--
    
     public void listFiles() {
        globalLock.lock();      // Lock to prevent concurent modifications
        try {
            System.out.println("List of files: ");
            for (FEntry entry : inodeTable) {
                if (entry.isUsed()) {
                    System.out.println("- " + entry.getFilename() + " (Size: " + entry.getFilesize() + " bytes)");
                }
            }
        } finally {
            globalLock.unlock();    //Unlock
        }
    }
}   
