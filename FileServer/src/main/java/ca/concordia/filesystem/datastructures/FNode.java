package ca.concordia.filesystem.datastructures;

//Import Serializable to allow saving/loading the object without it, every time server loads data would be lost
import java.io.Serializable;

//Each FNode hold the info about the data block and a link to the next block like a chain.
//blockIndex = which block this node uses
//next = index of the next FNode in the chain, -1 if last node

public class FNode implements Serializable{

    private static final long serialVersionUID = 1L;    //Serialization so if changes occur in code, it won't break deserialization.
    private int blockIndex;
    private int next;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    //--Getters and Setters--
    //Default Empty Node (unused block)
    public FNode() {
        this.blockIndex = -1;
        this.next = -1;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    //Check if node is used
    public boolean isUsed() {
        return this.blockIndex >= 0;
    }

    //Clear node data(delete option)
    public void clear() {
        this.blockIndex = -1;
        this.next = -1;
    }

    @Override
    public String toString() {
        return "FNode[blockIndex= " + blockIndex + ", next= " + next + "]";
    }

}
