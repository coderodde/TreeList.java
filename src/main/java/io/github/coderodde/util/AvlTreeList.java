package io.github.coderodde.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
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
public class AvlTreeList<E> implements List<E> {
    
    private static final int INCORRECT_HEIGHT_SENTINEL = Integer.MAX_VALUE - 1;
    
    private static final class TreeListNode<E> {
        E element;
        TreeListNode<E> left;
        TreeListNode<E> right;
        int height;
        int treeSize = 1;
        
        private TreeListNode(E element) {
            this.element = element;
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof TreeListNode<?> other 
                    && Objects.equals(this.element, other.element)
                    && left     == other.left
                    && right    == other.right
                    && height   == other.height
                    && treeSize == other.treeSize;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + Objects.hashCode(element);
            hash = 53 * hash + Objects.hashCode(left);
            hash = 53 * hash + Objects.hashCode(right);
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
     * The number of modifications made into this list.
     */
    private int modCount;
    
    public AvlTreeList() {
        
    }
    
    public AvlTreeList(Collection<? extends E> elements) {
        Object[] array = Objects.requireNonNull(elements).toArray();
        root = treeify(array, 0, array.length);
    }
    
    @Override
    public int size() {
        return treeSize(root);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return containsImpl(root, o);
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }
    
    @Override
    public Object[] toArray() {
        Object[] array = new Object[size()];
        int index = 0;
        
        for (E e : this) {
            array[index++] = e;
        }
        
        return array;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean add(E e) {
        add(size(), e);
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
    public boolean addAll(Collection<? extends E> elements) {
        return addAll(size(), elements);
    }
    
    @Override
    public boolean addAll(int index, Collection<? extends E> elements) {
        checkPosition(index);
        
        Object[] array = Objects.requireNonNull(elements).toArray();
        
        if (array.length == 0) {
            return false;
        }
            
        if ((long) size() + array.length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "List is to large to accommodate the collection.");
        }
        
        TreeListNode<E> incoming = treeify(array, 0, array.length);
        Split<E> parts           = split(root, index);
        root                     = join(
                                       join(
                                           parts.left(), 
                                           incoming), 
                                       parts.right());
    
        ++modCount;
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        if (root != null) {
            root = null;
            ++modCount;
        }
    }

    @Override
    public E get(int index) {
        return nodeAt(Objects.checkIndex(index, size())).element;
    }

    @Override
    public E set(int index, E element) {
        TreeListNode<E> node = nodeAt(Objects.checkIndex(index, size()));
        
        E old        = node.element;
        node.element = element;
        
        return old;
    }

    @Override
    public void add(int index, E element) {
        checkPosition(index);
        
        if (size() == Integer.MAX_VALUE) {
            throw new IllegalStateException("List is too large.");
        }
        
        root = insert(root, index, element);
        ++modCount;
    }
    
    @Override
    public E remove(int index) {
        E old = get(index);
        root = delete(root, index);
        ++modCount;
        return old;
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        checkPosition(index);
        return new TreeIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<E> spliterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addFirst(E e) {
        add(0, e);
    }

    @Override
    public void addLast(E e) {
        add(size(), e);
    }

    @Override
    public E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return nodeAt(0).element;
    }

    @Override
    public E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return nodeAt(size() - 1).element;
    }

    @Override
    public E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return remove(0);
    }

    @Override
    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        
        return remove(size() - 1);
    }

    @Override
    public List<E> reversed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<E> stream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<E> parallelStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        throw new UnsupportedOperationException();
    }
    
    protected void removeRange(int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, size());
        
        if (fromIndex == toIndex) {
            return;
        }
        
        if (fromIndex == 0 && toIndex == size()) {
            clear();
            return;
        }
        
        Split<E> prefix = split(root, fromIndex);
        Split<E> suffix = split(prefix.right(), toIndex - fromIndex);
        root            = join(prefix.left(), suffix.right());
        
        ++modCount;
    }
    
    private TreeListNode<E> nodeAt(int index) {
        TreeListNode<E> node = root;
        
        while (true) {
            int leftSize = treeSize(node.left);
            
            if (index < leftSize) {
                node = node.left;
            } else if (index == leftSize) {
                return node;
            } else {
                index -= leftSize + 1;
                node = node.right;
            }
        }
    }
    
    private static <E> int height(TreeListNode<E> node) {
        return node == null ? -1 : node.height;
    }
    
    private static <E> int treeSize(TreeListNode<E> node) {
        return node == null ? 0 : node.treeSize;
    }
    
    private static <E> void update(TreeListNode<E> node) {
        node.height   = 1 + Math.max(height(node.left), height(node.right));
        node.treeSize = 1 + treeSize(node.left) + treeSize(node.right); 
    }
    
    private static <E> TreeListNode<E> rotateLeft(TreeListNode<E> node) {
        TreeListNode<E> top = node.right;
        node.right          = top.left;
        top.left            = node;
        
        update(node);
        update(top);
        
        return top;
    }
    
    private static <E> TreeListNode<E> rotateRight(TreeListNode<E> node) {
        TreeListNode<E> top = node.left;
        node.left           = top.right;
        top.right           = node;
        
        update(node);
        update(top);
        
        return top;
    }
    
    private static <E> TreeListNode<E> balance(TreeListNode<E> node) {
        update(node);
        
        int factor = height(node.left) - height(node.right);;
        
        if (factor > 1) {
            if (height(node.left.left) < height(node.left.right)) {
                node.left = rotateLeft(node.left);
            }
            
            return rotateRight(node);
        }
        
        if (factor < -1) {
            if (height(node.right.right) < height(node.right.left)) {
                node.right = rotateRight(node.right);
            }
            
            return rotateLeft(node);
        }
        
        return node;
    }
    
    private static <E> TreeListNode<E> insert(TreeListNode<E> node,
                                              int index,
                                              E element) {
        if (node == null) {
            return new TreeListNode<>(element);
        }
        
        int leftSize = treeSize(node.left);
        
        if (index <= leftSize) {
            node.left = insert(node.left, index, element);
        } else {
            node.right = insert(node.right, index - leftSize - 1, element);
        }
        
        return balance(node);
    }
    
    private static <E> TreeListNode<E> delete(TreeListNode<E> node, int index) {
        int leftSize = treeSize(node.left);
        
        if (index < leftSize) {
            node.left = delete(node.left, index);
        } else if (index > leftSize) {
            node.right = delete(node.right, index - leftSize - 1);
        } else {
            if (node.left == null || node.right == null) {
                TreeListNode<E> child = node.left == null 
                                      ? node.right 
                                      : node.left;
                
                return child;
            }
            
            node.element = minimum(node.right).element;
            node.right   = delete(node.right, 0);
        }
        
        return balance(node);
    }
    
    private static <E> Split<E> split(TreeListNode<E> node, int index) {
        if (node == null) {
            return new Split<>(null, null);
        }
        
        if (index == 0) {
            return new Split<>(null, node);
        }
        
        if (index == treeSize(node)) {
            return new Split<>(node, null);
        }
        
        TreeListNode<E> left  = node.left;
        TreeListNode<E> right = node.right;
        
        int leftSize = treeSize(left);
        
        node.left  = null;
        node.right = null;
        
        update(node);
        
        if (index <= leftSize) {
            Split<E> parts = split(left, index);
            return new Split<>(parts.left(), join(parts.right(), node, right));
        }
        
        Split<E> parts = split(right, index - leftSize - 1);
        return new Split<>(join(left, node, parts.left()), parts.right());
    }
    
    private static <E> TreeListNode<E> join(TreeListNode<E> left,
                                            TreeListNode<E> pivot,
                                            TreeListNode<E> right) {
        if (height(left) > height(right) + 1) {
            left.right = join(left.right, pivot, right);
            return balance(left);
        }
        
        if (height(right) > height(left) + 1) {
            right.left = join(left, pivot, right.left);
            return balance(right);
        }
        
        pivot.left  = left;
        pivot.right = right;
        
        update(pivot);
        
        return pivot;
    }
    
    private static <E> TreeListNode<E> join(TreeListNode<E> left,
                                            TreeListNode<E> right) {
        if (left == null) {
            return right;
        }
        
        if (right == null) {
            return left;
        }
        
        TreeListNode<E> pivot = minimum(right);
        right = delete(right, 0);
        
        pivot.left  = null;
        pivot.right = null;
        
        update(pivot);
        
        return join(left, pivot, right);
    }
    
    private static <E> TreeListNode<E> treeify(Object[] array,
                                               int from, 
                                               int to) {
        if (from == to) {
            return null;
        }
        
        int mid              = from + (to - from) / 2;
        E element            = (E) array[mid];
        TreeListNode<E> node = new TreeListNode<>(element);
        
        node.left  = treeify(array, from, mid);
        node.right = treeify(array, mid + 1, to);
        
        update(node);
        
        return node;
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
        
        if (visited.contains(node)) {
            return false;
        }
        
        visited.add(node);
        
        if (!treeHasNoCycles(visited, node.left) ||
            !treeHasNoCycles(visited, node.right)) {
            return false;
        }
        
        visited.remove(node);
        
        return true;
    }
    
    private static <E> boolean containsImpl(TreeListNode<E> node, 
                                            Object element) {
        if (node == null) {
            return false;
        }
        
        if (Objects.equals(node.element, element)) {
            return true;
        }
        
        return containsImpl(node.left, element) ||
               containsImpl(node.right, element);
    }
    
    private static final record Split<E>(TreeListNode<E> left, 
                                         TreeListNode<E> right) {}

    private class SubList extends AbstractList<E> {

        private final int fromIndex;
        private final int toIndex;
        
        public SubList(int fromIndex, int toIndex) {
            Objects.checkFromToIndex(fromIndex, toIndex, size());
            
            this.fromIndex = fromIndex;
            this.toIndex   = toIndex;
        }

        @Override
        public void clear() {
            AvlTreeList.this.removeRange(fromIndex, toIndex);
        }

        @Override
        public E get(int index) {
            return AvlTreeList.this.get(fromIndex + index);
        }

        @Override
        public int size() {
            return toIndex - fromIndex;
        }
    }
    
    private final class TreeIterator implements ListIterator<E> {

        private int cursor;
        private int lastIndex        = -1;
        private int expectedModCount = modCount;
        
        private final List<TreeListNode<E>> path = new ArrayList<>();
        private TreeListNode<E> last;
        
        TreeIterator(int index) {
            cursor = index;
            rebuildPath();
        }
        
        private void checkModification() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }    
        
        private void rebuildPath() {
            path.clear();
            
            if (cursor == size()) {
                return;
            }
            
            int index = cursor;
            TreeListNode<E> node = root;
            
            while (node != null) {
                path.add(node);
                
                int leftSize = treeSize(node.left);
                
                if (index == leftSize) {
                    return;
                }
                
                if (index < leftSize) {
                    node = node.left;
                } else {
                    index -= leftSize + 1;
                    node = node.right;
                }
            }
        }
        
        private void advancePath() {
            TreeListNode<E> node = path.getLast();
            
            if (node.right != null) {
                node = node.right;
                
                while (node != null) {
                    path.add(node);
                    node = node.left;
                }
            } else {
                path.removeLast();
                
                while (!path.isEmpty()) {
                    TreeListNode<E> ancestor = path.getLast();
                    
                    if (ancestor.left == node) {
                        return;
                    }
                    
                    node = ancestor;
                    path.removeLast();
                }
            }
        }
        
        private void retreatPath() {
            if (path.isEmpty()) {
                TreeListNode<E> node = root;
                
                while (node != null) {
                    path.add(node);
                    node = node.right;
                }
                
                return;
            }
            
            TreeListNode<E> node = path.getLast();
            
            if (node.left != null) {
                node = node.left;
                
                while (node != null) {
                    path.add(node);
                    node = node.right;
                }
            } else {
                path.removeLast();
                
                while (!path.isEmpty()) {
                    TreeListNode<E> ancestor = path.getLast();
                    
                    if (ancestor.right == node) {
                        return;
                    }
                    
                    node = ancestor;
                    path.removeLast();
                }
            }
        }
        
        @Override
        public boolean hasNext() {
            return cursor < size();
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public E next() {
            checkModification();
            
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            last = path.getLast();
            lastIndex = cursor++;
            advancePath();
            return last.element;
        }

        @Override
        public E previous() {
            checkModification();
            
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            
            retreatPath();
            last = path.getLast();
            lastIndex = --cursor;
            return last.element;
        }

        @Override
        public void remove() {
            checkModification();
            
            if (lastIndex < 0) {
                throw new IllegalStateException();
            }
            
            AvlTreeList.this.remove(lastIndex);
            
            if (lastIndex < cursor) {
                --cursor;
            }
            
            rebuildPath();
            last = null;
            lastIndex = -1;
            expectedModCount = modCount;
        }

        @Override
        public void set(E e) {
            checkModification();
            
            if (lastIndex < 0) {
                throw new IllegalStateException();
            }
            
            last.element = e;
        }

        @Override
        public void add(E e) {
            checkModification();
            
            AvlTreeList.this.add(cursor, e);
            ++cursor;
            rebuildPath();
            last = null;
            lastIndex = -1;
            expectedModCount = modCount;
        }
    }
    
    private void checkPosition(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException(
                    "Index: " + index + ", size: " + size());
        }
    }
}
