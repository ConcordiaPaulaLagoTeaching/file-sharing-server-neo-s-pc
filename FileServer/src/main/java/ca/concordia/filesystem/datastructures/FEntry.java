package ca.concordia.filesystem.datastructures;

import java.util.LinkedList;
import java.io.Serializable;

public class FEntry implements Serializable {
    private static final long serialVersionUID = 1L;    //Serialization so if changes occur in code, it won't break deserialization.

    private String filename;    // Max 11 characters
    private short filesize;     // Size in bytes
    private short firstBlock;   // Pointer to the first data block

      public FEntry() {
        this.filename = null;   // no filename yet = unused slot
        this.filesize = 0;      // no data written yet
        this.firstBlock = -1;   // -1 = no block assigned
    }

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException{
        
        //Check filename is max 11 bytes long
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    //Check if entry is used
    public boolean isUsed() {
        return filename != null && !filename.isEmpty();
    }

    //Clear entry data(delete option)
    public void clear() {
        this.filename = null;
        this.filesize = 0;
        this.firstBlock = -1;
    }

    @Override
    public String toString() {
        return "FEntry[filename= " + filename + ", filesize= " + filesize + ", firstBlock= " + firstBlock + "]";
    }
}
