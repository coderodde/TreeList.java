package io.github.coderodde.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Jupiter 5 tests for AvlTreeList; requires Java 21.
 * Place in src/test/java/io/github/coderodde/util/AvlTreeListTest.java.
 * Random tests are reproducible; failures report the seed and step.
 * Unsupported operations and unimplemented list equality/hashCode are excluded.
 * Supported-operation correctness assertions are retained, even if they expose bugs.
 * Reflection is restricted to tests of the diagnostic invariant checker.
 */
@Timeout(30)
class AvlTreeListTest {
    private static List<Integer> sequence(int size) {
        return IntStream.range(0, size).boxed().toList();
    }

    private static void verify(List<Integer> expected, AvlTreeList<Integer> actual) {
        assertTrue(actual.invariantHolds(), "AVL invariant");
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        assertArrayEquals(expected.toArray(), actual.toArray());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), "index " + i);
        }
        ListIterator<Integer> it = actual.listIterator(actual.size());
        for (int i = expected.size() - 1; i >= 0; i--) {
            assertTrue(it.hasPrevious());
            assertEquals(expected.get(i), it.previous());
        }
        assertFalse(it.hasPrevious());
    }

    @Test void emptyList() {
        AvlTreeList<Integer> t = new AvlTreeList<>();
        verify(List.of(), t);
        assertFalse(t.contains(null));
        assertThrows(NoSuchElementException.class, t::getFirst);
        assertThrows(NoSuchElementException.class, t::getLast);
        assertThrows(NoSuchElementException.class, t::removeFirst);
        assertThrows(NoSuchElementException.class, t::removeLast);
        assertThrows(NoSuchElementException.class, t.iterator()::next);
        assertThrows(NoSuchElementException.class, t.listIterator()::previous);
        t.clear();
        t.removeRange(0, 0);
        verify(List.of(), t);
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 7, 8, 15, 16, 31, 32, 1000})
    void collectionConstructor(int size) {
        ArrayList<Integer> source = new ArrayList<>(sequence(size));
        AvlTreeList<Integer> t = new AvlTreeList<>(source);
        source.clear();
        verify(sequence(size), t);
    }

    @Test void nullConstructorAndArguments() {
        assertThrows(NullPointerException.class, () -> new AvlTreeList<Integer>(null));
        AvlTreeList<Integer> t = new AvlTreeList<>(List.of(1, 2));
        assertThrows(NullPointerException.class, () -> t.addAll(null));
        assertThrows(NullPointerException.class, () -> t.addAll(1, null));
        verify(List.of(1, 2), t);
    }

    @Test void nullsDuplicatesAndSearch() {
        AvlTreeList<Integer> t = new AvlTreeList<>(Arrays.asList(null, 2, 1, 2, null));
        assertTrue(t.contains(null)); assertTrue(t.contains(1)); assertTrue(t.contains(2));
        assertFalse(t.contains("2")); assertFalse(t.contains(99));
        assertEquals(Integer.valueOf(2), t.remove(1));
        assertNull(t.remove(0));
        verify(Arrays.asList(1, 2, null), t);
        assertEquals(Integer.valueOf(1), t.set(0, null));
        verify(Arrays.asList(null, 2, null), t);
    }

    @Test void indexBoundsAndRangeBounds() {
        AvlTreeList<Integer> t = new AvlTreeList<>(List.of(1, 2, 3));
        for (int i : new int[]{-1, 3, 4, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> t.get(i));
            assertThrows(IndexOutOfBoundsException.class, () -> t.set(i, 7));
            assertThrows(IndexOutOfBoundsException.class, () -> t.remove(i));
        }
        for (int i : new int[]{-1, 4, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> t.add(i, 7));
            assertThrows(IndexOutOfBoundsException.class, () -> t.addAll(i, List.of()));
            assertThrows(IndexOutOfBoundsException.class, () -> t.listIterator(i));
        }
        for (int[] range : new int[][]{{-1, 2}, {0, 4}, {2, 1}}) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> t.removeRange(range[0], range[1]));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> t.subList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> t.subList(0, 4));
        assertThrows(IllegalArgumentException.class, () -> t.subList(2, 1));
        verify(List.of(1, 2, 3), t);
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void rotationAndDeletionPatterns(int mode) {
        AvlTreeList<Integer> t = new AvlTreeList<>();
        ArrayList<Integer> a = new ArrayList<>();
        Random r = new Random(42);
        for (int i = 0; i < 256; i++) {
            int p = switch(mode) {case 0 -> 0; case 1 -> a.size();
                case 2 -> a.size()/2; default -> r.nextInt(a.size()+1);};
            t.add(p, i); a.add(p, i); verify(a, t);
        }
        while (!a.isEmpty()) {
            int p = switch(mode) {case 0 -> 0; case 1 -> a.size()-1;
                case 2 -> a.size()/2; default -> r.nextInt(a.size());};
            assertEquals(a.remove(p), t.remove(p)); verify(a, t);
        }
    }

    @Test void sequencedOperationsAndReuseAfterClear() {
        AvlTreeList<Integer> t = new AvlTreeList<>();
        t.addFirst(2); t.addFirst(1); t.addLast(3);
        assertEquals(Integer.valueOf(1), t.getFirst());
        assertEquals(Integer.valueOf(3), t.getLast());
        assertEquals(Integer.valueOf(1), t.removeFirst());
        assertEquals(Integer.valueOf(3), t.removeLast());
        assertEquals(Integer.valueOf(2), t.removeLast());
        t.add(null); t.clear(); t.add(4); verify(List.of(4), t);
    }

    @Test void bulkInsertionAtEveryPositionAndDifferentHeights() {
        for (int size : new int[]{0, 1, 2, 7, 32, 100}) {
            for (int count : new int[]{0, 1, 2, 17, 128}) {
                for (int p = 0; p <= size; p++) {
                    AvlTreeList<Integer> t = new AvlTreeList<>(sequence(size));
                    ArrayList<Integer> a = new ArrayList<>(sequence(size));
                    assertEquals(a.addAll(p, sequence(count)), t.addAll(p, sequence(count)));
                    verify(a, t);
                }
            }
        }
    }

    @Test void selfInsertionAndBackedSourceSnapshot() {
        for (int p = 0; p <= 5; p++) {
            AvlTreeList<Integer> t = new AvlTreeList<>(sequence(5));
            ArrayList<Integer> a = new ArrayList<>(sequence(5));
            a.addAll(p, new ArrayList<>(a)); t.addAll(p, t); verify(a, t);
            a.addAll(2, new ArrayList<>(a.subList(1, 4)));
            t.addAll(2, t.subList(1, 4)); verify(a, t);
        }
        AvlTreeList<Integer> t = new AvlTreeList<>(List.of(1, 2));
        assertTrue(t.addAll(t)); verify(List.of(1, 2, 1, 2), t);
    }

    @Test void everySmallRemovalRange() {
        for (int size = 0; size <= 35; size++) {
            for (int from = 0; from <= size; from++) {
                for (int to = from; to <= size; to++) {
                    AvlTreeList<Integer> t = new AvlTreeList<>(sequence(size));
                    ArrayList<Integer> a = new ArrayList<>(sequence(size));
                    t.removeRange(from, to); a.subList(from, to).clear(); verify(a, t);
                    t.add(from, -1); a.add(from, -1); verify(a, t);
                }
            }
        }
    }







    @Test void iteratorStartsAndAlternatingDirections() {
        AvlTreeList<Integer> t = new AvlTreeList<>(sequence(40));
        for (int p = 0; p <= t.size(); p++) {
            ListIterator<Integer> it = t.listIterator(p);
            assertEquals(p, it.nextIndex()); assertEquals(p-1, it.previousIndex());
            if (p < t.size()) {
                assertEquals(Integer.valueOf(p), it.next());
                assertEquals(Integer.valueOf(p), it.previous());
                assertEquals(Integer.valueOf(p), it.next());
                it.previous();
            }
            for (int i=p-1; i>=0; i--) assertEquals(Integer.valueOf(i), it.previous());
            assertThrows(NoSuchElementException.class, it::previous);
            for (int i=0; i<t.size(); i++) assertEquals(Integer.valueOf(i), it.next());
            assertThrows(NoSuchElementException.class, it::next);
        }
    }

    @Test void iteratorEditStateMachine() {
        AvlTreeList<Integer> t = new AvlTreeList<>(List.of(1, 2, 3));
        ListIterator<Integer> it = t.listIterator();
        assertThrows(IllegalStateException.class, it::remove);
        assertThrows(IllegalStateException.class, () -> it.set(9));
        assertEquals(Integer.valueOf(1), it.next()); it.set(10); it.set(11);
        it.remove(); assertEquals(0, it.nextIndex());
        assertThrows(IllegalStateException.class, it::remove);
        assertThrows(IllegalStateException.class, () -> it.set(9));
        it.add(7); assertEquals(1, it.nextIndex());
        assertThrows(IllegalStateException.class, it::remove);
        assertThrows(IllegalStateException.class, () -> it.set(9));
        assertEquals(Integer.valueOf(7), it.previous()); it.remove();
        assertEquals(0, it.nextIndex()); assertEquals(Integer.valueOf(2), it.next());
        verify(List.of(2, 3), t);
        ListIterator<Integer> backwards = t.listIterator(t.size());
        while (backwards.hasPrevious()) {backwards.previous(); backwards.remove(); assertTrue(t.invariantHolds());}
        verify(List.of(), t); backwards.add(null); assertNull(backwards.previous());
    }

    @Test void deletingTwoChildNodeThroughIteratorPreservesCursor() {
        AvlTreeList<Integer> t = new AvlTreeList<>(sequence(15));
        ListIterator<Integer> it = t.listIterator(7);
        assertEquals(Integer.valueOf(7), it.next()); it.remove();
        assertEquals(Integer.valueOf(8), it.next()); it.set(80);
        assertEquals(Integer.valueOf(80), it.previous());
        ArrayList<Integer> a = new ArrayList<>(sequence(15));a.remove(7);a.set(7,80);
        verify(a,t);
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4})
    void structuralEditsInvalidateIterators(int operation) {
        AvlTreeList<Integer> t = new AvlTreeList<>(sequence(8));
        ListIterator<Integer> it = t.listIterator(); it.next();
        switch(operation) {
            case 0 -> t.add(99); case 1 -> t.remove(4); case 2 -> t.clear();
            case 3 -> t.addAll(3, List.of(8,9)); case 4 -> t.removeRange(2,5);
        }
        assertThrows(ConcurrentModificationException.class, it::next);
        assertThrows(ConcurrentModificationException.class, it::previous);
        assertThrows(ConcurrentModificationException.class, it::remove);
        assertThrows(ConcurrentModificationException.class, () -> it.set(8));
        assertThrows(ConcurrentModificationException.class, () -> it.add(8));
        // hasNext/hasPrevious need not throw: fail-fast is best effort.
    }

    @Test void setAndNoOpEditsPreserveIterator() {
        AvlTreeList<Integer> t = new AvlTreeList<>(List.of(1,2));
        ListIterator<Integer> it = t.listIterator();
        t.set(0,9); t.addAll(List.of()); t.removeRange(1,1);
        assertEquals(Integer.valueOf(9),it.next());
        assertEquals(Integer.valueOf(2),it.next());
        t.clear(); ListIterator<Integer> empty = t.listIterator(); t.clear();
        empty.add(7); verify(List.of(7),t);
    }

    @Test void sublistReadBoundsAndClear() {
        AvlTreeList<Integer> t = new AvlTreeList<>(sequence(10));
        List<Integer> sub = t.subList(2, 8);
        assertEquals(6, sub.size());
        for (int i = 0; i < 6; i++) assertEquals(Integer.valueOf(i + 2), sub.get(i));
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sub.get(sub.size()));
        t.set(2, 99); assertEquals(Integer.valueOf(99), sub.get(0));
        sub.clear(); verify(List.of(0, 1, 8, 9), t);
        assertTrue(sub.isEmpty(), "A cleared sublist must report size zero");
    }



    private record Outcome(Object value, Class<?> exception) {}
    private static Outcome outcome(Supplier<?> action) {
        try {return new Outcome(action.get(),null);}
        catch (RuntimeException ex) {return new Outcome(null,ex.getClass());}
    }
    private static void compare(Supplier<?> expected, Supplier<?> actual) {
        assertEquals(outcome(expected),outcome(actual));
    }

    @ParameterizedTest @ValueSource(longs = {0, 1, 2, 3, 7, 13, 42, 100, 999, 20261111})
    void randomizedOperationsAgainstArrayList(long seed) {
        Random r=new Random(seed);AvlTreeList<Integer> t=new AvlTreeList<>();
        ArrayList<Integer> a=new ArrayList<>();
        for (int step=0;step<3000;step++) {
            try {
                int p=r.nextInt(a.size()+1);
                Integer v=r.nextInt(8)==0?null:r.nextInt(30);
                switch(r.nextInt(a.size()>200?4:12)) {
                    case 0 -> {if(!a.isEmpty()) {int i=r.nextInt(a.size());assertEquals(a.remove(i),t.remove(i));}}
                    case 1 -> {int q=p+r.nextInt(a.size()-p+1);a.subList(p,q).clear();t.removeRange(p,q);}
                    case 2 -> { if (!a.isEmpty()) { assertEquals(a.remove(0), t.remove(0)); } }
                    case 3 -> {if(!a.isEmpty()) {int i=r.nextInt(a.size());assertEquals(a.set(i,v),t.set(i,v));}}
                    case 4 -> {a.add(p,v);t.add(p,v);}
                    case 5 -> {List<Integer> b=new ArrayList<>();for(int k=r.nextInt(20);k>0;k--)b.add(r.nextInt(30));assertEquals(a.addAll(p,b),t.addAll(p,b));}
                    case 6 -> {int q=p+r.nextInt(a.size()-p+1);List<Integer> b=new ArrayList<>(a.subList(p,q));a.addAll(p,b);t.addAll(p,t.subList(p,q));}
                    case 7 -> {ListIterator<Integer> x=a.listIterator(p),y=t.listIterator(p);
                        for(int k=0;k<25;k++) {
                            switch(r.nextInt(5)) {
                                case 0 -> compare(x::next,y::next);
                                case 1 -> compare(x::previous,y::previous);
                                case 2 -> compare(()->{x.remove();return null;},()->{y.remove();return null;});
                                case 3 -> compare(()->{x.set(v);return null;},()->{y.set(v);return null;});
                                case 4 -> {x.add(v);y.add(v);}
                            }
                            assertEquals(x.nextIndex(),y.nextIndex());assertEquals(x.previousIndex(),y.previousIndex());
                            assertEquals(x.hasNext(),y.hasNext());assertEquals(x.hasPrevious(),y.hasPrevious());
                        }
                    }
                    case 8 -> {a.add(0,v);t.addFirst(v);}
                    case 9 -> { if (!a.isEmpty()) { assertEquals(a.removeLast(), t.removeLast()); } }
                    case 10 -> {a.add(v);t.addLast(v);}
                    case 11 -> {a.add(v);t.add(v);}
                }
                verify(a,t);
            } catch (AssertionError | RuntimeException ex) {
                throw new AssertionError("seed="+seed+", step="+step,ex);
            }
        }
    }

    private static Field field(Class<?> type,String name) throws ReflectiveOperationException {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }

    @ParameterizedTest @ValueSource(strings = {"cycle", "shared", "leftSize", "rightSize", "height", "balance"})
    void invariantCheckerRejectsCorruption(String kind) throws ReflectiveOperationException {
        AvlTreeList<Integer> t=new AvlTreeList<>(sequence(7));
        Object root=field(AvlTreeList.class,"root").get(t);
        Field left=field(root.getClass(),"left"),right=field(root.getClass(),"right");
        Field height=field(root.getClass(),"height"),size=field(root.getClass(),"treeSize");
        switch(kind) {
            case "cycle" -> left.set(root,root);
            case "shared" -> right.set(root,left.get(root));
            case "leftSize" -> size.setInt(left.get(root),99);
            case "rightSize" -> size.setInt(right.get(root),99);
            case "height" -> height.setInt(root,99);
            case "balance" -> {right.set(root,null);size.setInt(root,4);}
        }
        assertFalse(t.invariantHolds());
    }
}
