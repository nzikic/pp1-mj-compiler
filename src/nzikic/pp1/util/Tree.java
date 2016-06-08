package nzikic.pp1.util;

import java.util.Iterator;

public class Tree<T> 
{
    private Node<T> root;
    
    public Tree(T rootNode)
    {
        this.root = new Node<T>(rootNode);
    }
    
    public Node<T> search(T node)
    {
        return searchNode(root, node);
    }

    public Node<T> searchNode(Node<T> node, T value) 
    {
        // Is this the node?
        if (node.getValue() == value)
        {
            return node;
        }
        
        // Search children then
        Node<T> result = null;
        Iterator<Node<T>> it = node.getChildren().iterator();
        while (it.hasNext())
        {
            result = searchNode(it.next(), value);
            if (result != null) break;
        }
        
        return result;
    }
}
