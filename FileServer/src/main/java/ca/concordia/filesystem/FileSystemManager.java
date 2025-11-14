package ca.concordia.filesystem;

//Operations: CREATE, WRITE, READ, DELETE files
//Synchronization so multiple clients can access the file system without data corruption

import ca.concordia.filesystem.datastructures.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class FileSystemManager {

    private static final int BLOCKSIZE = 128;
    private static final int MAXFILES = 16;
    private static final int MAXBLOCKS = 64;

    private final FEntry[] fentries = new FEntry[MAXFILES];
    private final FNode[] fnodes = new FNode[MAXBLOCKS];

    private final ReadWriteLock fsLock = new ReentrantReadWriteLock(true);

    private RandomAccessFile disk;
    private int dataStartBlock;
    private int metadataBytes;
    private int metadataBlocks;

    public FileSystemManager(String filename, int totalSize) throws Exception {

        for (int i = 0; i < MAXFILES; i++) fentries[i] = new FEntry();
        for (int i = 0; i < MAXBLOCKS; i++) fnodes[i] = new FNode();

        int sizeFEntry = 15;
        int sizeFNode = 4;
        metadataBytes = MAXFILES * sizeFEntry + MAXBLOCKS * sizeFNode;
        metadataBlocks = (int) Math.ceil((double) metadataBytes / BLOCKSIZE);
        dataStartBlock = metadataBlocks;

        disk = new RandomAccessFile(filename, "rw");

        if (disk.length() == 0) {
            disk.setLength(totalSize);
            saveMetadata();
        } else {
            loadMetadata();
        }
    }

    // MetaData I/O 

    private void saveMetadata() throws IOException {
        fsLock.writeLock().lock();
        try {
            disk.seek(0);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            for (FEntry e : fentries) writeFEntry(bout, e);
            for (FNode n : fnodes) writeFNode(bout, n);

            byte[] meta = bout.toByteArray();
            disk.write(meta, 0, meta.length);

            int padding = (metadataBlocks * BLOCKSIZE) - meta.length;
            if (padding > 0) disk.write(new byte[padding]);

        } finally {
            fsLock.writeLock().unlock();
        }
    }

    private void loadMetadata() throws IOException {
        fsLock.writeLock().lock();
        try {
            disk.seek(0);
            byte[] meta = new byte[metadataBlocks * BLOCKSIZE];
            disk.readFully(meta);

            ByteArrayInputStream bin = new ByteArrayInputStream(meta);

            for (int i = 0; i < MAXFILES; i++) fentries[i] = readFEntry(bin);
            for (int i = 0; i < MAXBLOCKS; i++) fnodes[i] = readFNode(bin);

        } finally {
            fsLock.writeLock().unlock();
        }
    }

    // Metadata Serialization Helpers 

    private void writeFEntry(OutputStream out, FEntry e) throws IOException {
        byte[] name = new byte[11];
        if (e.getFilename() != null)
            System.arraycopy(e.getFilename().getBytes(), 0, name, 0, e.getFilename().length());

        out.write(name);
        writeShort(out, e.getFilesize());
        writeShort(out, e.getFirstBlock());
    }

    private FEntry readFEntry(InputStream in) throws IOException {
        byte[] name = in.readNBytes(11);
        short size = readShort(in);
        short first = readShort(in);

        FEntry e = new FEntry();
        String n = new String(name).trim();
        if (!n.isEmpty()) e.setFilename(n);
        e.setFilesize(size);
        e.setFirstBlock(first);
        return e;
    }

    private void writeFNode(OutputStream out, FNode n) throws IOException {
        writeShort(out, n.getBlockIndex());
        writeShort(out, n.getNext());
    }

    private FNode readFNode(InputStream in) throws IOException {
        short blk = readShort(in);
        short nxt = readShort(in);
        FNode n = new FNode();
        n.setBlockIndex(blk);
        n.setNext(nxt);
        return n;
    }

    private void writeShort(OutputStream out, short v) throws IOException {
        out.write((v >> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private short readShort(InputStream in) throws IOException {
        int hi = in.read();
        int lo = in.read();
        return (short) ((hi << 8) | lo);
    }

    //  File Operations

    public void createFile(String name) throws Exception {
        fsLock.writeLock().lock();
        try {
            if (name == null || name.length() > 11)
                throw new Exception("ERROR: filename too large");

            for (FEntry e : fentries)
                if (e.isUsed() && name.equals(e.getFilename()))
                    throw new Exception("ERROR: file already exists");

            int free = -1;
            for (int i = 0; i < MAXFILES; i++)
                if (!fentries[i].isUsed()) { free = i; break; }

            if (free == -1)
                throw new Exception("ERROR: maximum file limit reached");

            fentries[free] = new FEntry(name);
            saveMetadata();

        } finally {
            fsLock.writeLock().unlock();
        }
    }

    public byte[] readFile(String name) throws Exception {
        fsLock.readLock().lock();
        try {
            FEntry fe = findEntry(name);
            if (fe == null)
                throw new Exception("ERROR: file " + name + " does not exist");

            byte[] data = new byte[fe.getFilesize()];
            int offset = 0;
            short node = fe.getFirstBlock();

            while (node != -1) {
                FNode fn = fnodes[node];
                int realBlock = dataStartBlock + fn.getBlockIndex();

                disk.seek(realBlock * BLOCKSIZE);

                int toread = Math.min(BLOCKSIZE, data.length - offset);
                disk.readFully(data, offset, toread);

                offset += toread;
                node = fn.getNext();
            }

            return data;
        } finally {
            fsLock.readLock().unlock();
        }
    }

    public void deleteFile(String name) throws Exception {
        fsLock.writeLock().lock();
        try {
            FEntry fe = findEntry(name);
            if (fe == null)
                throw new Exception("ERROR: file " + name + " does not exist");

            short node = fe.getFirstBlock();
            byte[] zeros = new byte[BLOCKSIZE];

            while (node != -1) {
                FNode fn = fnodes[node];

                int realBlock = dataStartBlock + fn.getBlockIndex();
                disk.seek(realBlock * BLOCKSIZE);
                disk.write(zeros);

                short next = fn.getNext();
                fn.clear();
                node = next;
            }

            fe.clear();
            saveMetadata();

        } finally {
            fsLock.writeLock().unlock();
        }
    }

    public void writeFile(String name, byte[] data) throws Exception {
        fsLock.writeLock().lock();
        try {
            FEntry fe = findEntry(name);
            if (fe == null)
                throw new Exception("ERROR: file " + name + " does not exist");

            int needed = (int) Math.ceil(data.length / (double) BLOCKSIZE);
            if (needed == 0) needed = 1;

            List<Integer> freeNodes = new ArrayList<>();
            for (int i = 0; i < MAXBLOCKS; i++)
                if (!fnodes[i].isUsed()) freeNodes.add(i);

            if (freeNodes.size() < needed)
                throw new Exception("ERROR: file too large");

            int[] allocated = new int[needed];
            for (int i = 0; i < needed; i++) allocated[i] = freeNodes.get(i);

            for (int i = 0; i < needed; i++) {
                int idx = allocated[i];
                fnodes[idx].setBlockIndex((short) idx);
                fnodes[idx].setNext(i == needed - 1 ? (short)-1 : (short) allocated[i + 1]);
            }

            int offset = 0;
            for (int i = 0; i < needed; i++) {
                int blk = dataStartBlock + allocated[i];
                disk.seek(blk * BLOCKSIZE);

                int towrite = Math.min(BLOCKSIZE, data.length - offset);
                disk.write(data, offset, towrite);

                if (towrite < BLOCKSIZE)
                    disk.write(new byte[BLOCKSIZE - towrite]);

                offset += towrite;
            }

            short old = fe.getFirstBlock();
            byte[] zeros = new byte[BLOCKSIZE];
            while (old != -1) {
                FNode fn = fnodes[old];
                int blk = dataStartBlock + fn.getBlockIndex();
                disk.seek(blk * BLOCKSIZE);
                disk.write(zeros);

                short next = fn.getNext();
                fn.clear();
                old = next;
            }

            fe.setFilesize((short) data.length);
            fe.setFirstBlock((short) allocated[0]);

            saveMetadata();

        } finally {
            fsLock.writeLock().unlock();
        }
    }

    public String[] listFiles() {
        fsLock.readLock().lock();
        try {
            List<String> names = new ArrayList<>();
            for (FEntry e : fentries)
                if (e.isUsed()) names.add(e.getFilename());
            return names.toArray(new String[0]);
        } finally {
            fsLock.readLock().unlock();
        }
    }

    private FEntry findEntry(String name) {
        for (FEntry e : fentries)
            if (e.isUsed() && name.equals(e.getFilename()))
                return e;
        return null;
    }
}
