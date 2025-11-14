package ca.concordia.filesystem.datastructures;

//Import Serializable to allow saving/loading the object without it, every time server loads data would be lost
import java.io.Serializable;

public class FNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private short blockIndex;    
    private short next;          

    public FNode() {
        this.blockIndex = -1;
        this.next = -1;
    }

    //Check if node is used
    public boolean isUsed() {
        return blockIndex >= 0;
    }

    public short getBlockIndex() { return blockIndex; }
    public void setBlockIndex(short blockIndex) { this.blockIndex = blockIndex; }

    public short getNext() { return next; }
    public void setNext(short next) { this.next = next; }

    //Clear node data(delete option)
    public void clear() {
        this.blockIndex = -1;
        this.next = -1;
    }

    @Override
    public String toString() {
        return "[FNode block=" + blockIndex + " next=" + next + "]";
    }
}
