package io.github.coderodde.util;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This class is the unit test for the 
 * {@link io.github.coderodde.util.AvlTreeList}.
 */
public class TreeListTest {
    
    public TreeListTest() {
    }

    @Test
    public void collectionAppend() {
        List<Integer> lst = List.of(1, 2, 3, 4, 5, 6, 7);
        List<Integer> trl = new AvlTreeList<>();
        trl.addAll(lst);
        System.out.println("yes");
    }
}
