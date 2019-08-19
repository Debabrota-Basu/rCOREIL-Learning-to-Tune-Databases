/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sg.edu.nus.autotune;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author Chen Weidong
 */
public class AbsQuery {

    //private int[] _QArray;
    private HashMap<Integer, Integer> _select;
    private HashMap<Integer, Integer> _update;
    
    public AbsQuery() {
        _select = new HashMap<Integer, Integer>();
        _update = new HashMap<Integer, Integer>();
    }

    public AbsQuery(HashMap<Integer, Integer> select, HashMap<Integer, Integer> update) {
        _select = new HashMap<Integer, Integer>(select);
        _update = new HashMap<Integer, Integer>(update);
    }

    public int getSelectInfo(int i) {
        if (_select.containsKey(i)) {
            return _select.get(i);
        }
        return 0;
    }

    public int getUpdateInfo(int i) {
        if (_update.containsKey(i)) {
            return _update.get(i);
        }
        return 0;
    }

    public void print() {
        Iterator<Integer> keySetIterator = _select.keySet().iterator();
        System.out.print("Select: ");
        while (keySetIterator.hasNext()) {
            Integer key = keySetIterator.next();
            System.out.print(key + "(" + _select.get(key) + ")" + ";");
        }
        keySetIterator = _update.keySet().iterator();
        System.out.print("\tUpdate: ");
        while (keySetIterator.hasNext()) {
            Integer key = keySetIterator.next();
            System.out.print(key + "(" + _update.get(key) + ")" + ";");
        }
        System.out.println("\n====");
    }
}
