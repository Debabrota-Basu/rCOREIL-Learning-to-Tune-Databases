package sg.edu.nus.autotune;

import java.util.Comparator;
import java.util.List;

public class CustomComparator implements Comparator<List<Integer>> {
    @Override
    public int compare(List<Integer> o1, List<Integer> o2) {
        return o1.size() - o2.size();
    }
}
