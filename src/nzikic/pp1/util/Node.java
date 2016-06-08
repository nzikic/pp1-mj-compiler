package nzikic.pp1.util;

import java.util.LinkedList;
import java.util.List;

import rs.etf.pp1.symboltable.Tab;

public class Node<T> 
{
    private T value;
    private List<Node<T>> children;
    
    public Node(T value)
    {
        this.setValue(value);
        this.setChildren(new LinkedList<Node<T>>());
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public List<Node<T>> getChildren() {
        return children;
    }

    public void setChildren(List<Node<T>> children) {
        this.children = children;
    }
    
    public boolean insertChild(T child)
    {
        if (child == Tab.noObj) // Da li se ikada dodaje noObj?!
            return false;
        
        children.add(new Node<T>(child));
        return true;
    }
    
}
