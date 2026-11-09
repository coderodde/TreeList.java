package io.github.coderodde.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * This class implements an AVL-tree based list data structure.
 * 
 * @param <E>
 */
public class TreeList<E> implements List<E> {
    
    private static final int INCORRECT_HEIGHT_SENTINEL = 1_000_000;

    private static final class TreeListNode<E> {
        E element;
        TreeListNode<E> left;
        TreeListNode<E> right;
        TreeListNode<E> parent;
        int height;
        int treeSize;
        
        private TreeListNode(E element) {
            this.element = element;
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof TreeListNode<?> other 
                    && Objects.equals(this, other)
                    && left     == other.left
                    && right    == other.right
                    && parent   == other.parent
                    && height   == other.height
                    && treeSize == other.treeSize;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(element);
            hash = 53 * hash + Objects.hashCode(left);
            hash = 53 * hash + Objects.hashCode(right);
            hash = 53 * hash + Objects.hashCode(parent);
            hash = 53 * hash + height;
            hash = 53 * hash + treeSize;
            return hash;
        }
        
        @Override
        public String toString() {
            return "[Element: %s, h: %d, ts: %d]".formatted(element, 
                                                            height, 
                                                            treeSize);
        }
    }
    
    /**
     * The root node of this tree list.
     */
    private TreeListNode<E> root;
    
    /**
     * The number of elements in this list.
     */
    private int size;
    
    /**
     * The number of modifications made into this list.
     */
    private int modCount;
    
    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return containsImpl(root, (E) o);
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean add(E e) {
        ++modCount;
        
        if (root == null) {
            root = new TreeListNode<>(e);
            size = 1;
            return true;
        }
        
        TreeListNode<E> node = root;
        
        while (node.right != null) {
            node = node.right;
        }
        
        TreeListNode<E> newNode = new TreeListNode<>(e);
        node.right              = newNode;
        newNode.parent          = node;
        
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return false;
        }
        
        ++modCount;
        
        TreeListNode<E> newTreeRoot = treeify(c);
        
        if (size == 0) {
            size = c.size();
            root = newTreeRoot;
        } else {
            appendSubTree(newTreeRoot);
        }
        
        return true;
    }
    
    private void appendSubTree(TreeListNode<E> subTreeRoot) {
        TreeListNode<E> node = root;
        
        while (node.right != null) {
            node = node.right;
        }
        
        node.right = subTreeRoot;
    }

    private static <E> TreeListNode<E> treeify(Collection<? extends E> c) {
        E[] array       = c.toArray((E[]) new Object[c.size()]);
        int middleIndex = array.length / 2;
        
        TreeListNode<E> subTreeRoot = new TreeListNode<>(array[middleIndex]);
        
        return treeify(array, 
                       0,
                       array.length, 
                       middleIndex, 
                       subTreeRoot);
    }
    
    private static <E> TreeListNode<E> treeify(E[] array, 
                                               int from, 
                                               int to,
                                               int leftCount,
                                               TreeListNode<E> parent) {
        
        int rangeLength = to - from;
        int middleIndex = from + rangeLength / 2;
        
        if (rangeLength == 0) {
            System.out.println("yesss");
        }
        
        if (rangeLength == 1) {
            TreeListNode<E> node = new TreeListNode<>(array[middleIndex]);
            node.parent          = parent;
            return node;
        }
        
        TreeListNode<E> middleNode = new TreeListNode<>(array[middleIndex]);
        
        TreeListNode<E> leftChild = treeify(array, 
                                            from, 
                                            middleIndex, 
                                            leftCount,
                                            middleNode);
        
        TreeListNode<E> rightChild = treeify(array, 
                                             middleIndex + 1,
                                             to, 
                                             rangeLength - middleIndex, 
                                             middleNode);
        middleNode.left  = leftChild;
        middleNode.right = rightChild;
        
        return middleNode;
    }
    
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        List.super.replaceAll(operator); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public void sort(Comparator<? super E> c) {
        List.super.sort(c); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public E get(int index) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Spliterator<E> spliterator() {
        return List.super.spliterator(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public void addFirst(E e) {
        List.super.addFirst(e); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public void addLast(E e) {
        List.super.addLast(e); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public E getFirst() {
        return List.super.getFirst(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public E getLast() {
        return List.super.getLast(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public E removeFirst() {
        return List.super.removeFirst(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public E removeLast() {
        return List.super.removeLast(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public List<E> reversed() {
        return List.super.reversed(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return List.super.toArray(generator); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return List.super.removeIf(filter); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public Stream<E> stream() {
        return List.super.stream(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public Stream<E> parallelStream() {
        return List.super.parallelStream(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        List.super.forEach(action); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
    
    private static <E> TreeListNode<E> successor(TreeListNode<E> node) {
        if (node.right != null) {
            return minimum(node.right);
        }
        
        TreeListNode<E> parent = node.parent;
        
        while (parent != null && parent.right == node) {
            node   = parent;
            parent = parent.parent;
        }
        
        return parent;
    }
    
    private static <E> TreeListNode<E> minimum(TreeListNode<E> node) {
        while (node.left != null) {
            node = node.left;
        }
        
        return node;
    }
    
    public boolean invariantHolds() {
        return treeHasNoCycles()
            && treeHasCorrectHeightFields()
            && treeIsBalanced();
    }
    
    private boolean treeIsBalanced() {
        return treeIsBalanced(root);
    }
    
    private boolean treeIsBalanced(TreeListNode<E> node) {
        if (node == null) {
            return true;
        }
        
        int leftHeight  = getHeight(node.left);
        int rightHeight = getHeight(node.right);
        int balance     = Math.abs(leftHeight - rightHeight);
        
        return balance < 2;
    }
    
    private static <E> int getHeight(TreeListNode<E> node) {
        return node == null ? -1 : node.height;
    }
    
    private boolean treeHasCorrectHeightFields() {
        if (root == null) {
            return true;
        }
        
        return root.height == countHeight(root);
    }
    
    private int countHeight(TreeListNode<E> node) {
        if (node == null) {
            return -1;
        }
        
        int expectedHeight = Math.max(countHeight(node.left),
                                      countHeight(node.right)) + 1;
        
        return node.height != expectedHeight 
                           ? INCORRECT_HEIGHT_SENTINEL 
                           : expectedHeight;
    }
    
    private boolean treeHasNoCycles() {
        return treeHasNoCycles(new HashSet<>(), root);
    }
    
    private boolean treeHasNoCycles(Set<TreeListNode<E>> visited, 
                                    TreeListNode<E> node) {
        
        if (node == null) {
            return true;
        }
        
        visited.add(node);
        
        if (!treeHasNoCycles(visited, node.left) ||
            !treeHasNoCycles(visited, node.right)) {
            return false;
        }
        
        visited.remove(node);
        
        return true;
    }
    
    private static <E> boolean
         treeNodesHaveCorrectHeightsImpl(TreeListNode<E> node) {
             
        if (node.left == null && node.right == null) {
            return node.height == 0;
        }
        
        if (node.left != null 
                && !treeNodesHaveCorrectHeightsImpl(node.left)) {
            
            return false;
        }
        
        if (node.right != null 
            && !treeNodesHaveCorrectHeightsImpl(node.right)) {
            
            return false;
        }
        
        int expectedNodeHeight = Math.max(node.left.height,
                                          node.right.height) + 1;
        
        return node.height == expectedNodeHeight;
    }
         
    
    
    private static <E> boolean containsImpl(TreeListNode<E> node, E element) {
        if (node == null) {
            return false;
        }
        
        if (Objects.equals(node.element, element)) {
            return true;
        }
        
        return containsImpl(node.left, element) && 
               containsImpl(node.right, element);
    }
}
