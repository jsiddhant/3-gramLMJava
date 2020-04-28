package edu.berkeley.nlp.assignments.assign1.student;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Open address hash map with linear probing. Maps Longs to int's. Note that
 * int's are assumed to be non-negative, and -1 is returned when a key is not
 * present.
 *
 */
public class LongKeyOpenHashMap{

    private long[] keys;

    private int[] values;

    private int[] ferts;

    private int[] alphas;

    private int[] atemp;

    private int size = 0;

    private final float incrAmount = 2;

    private final long EMPTY_KEY = -1;

    private final double MAX_LOAD_FACTOR;

    public boolean put(long k, int v) {
        if(k < 0){
            throw new Error("Negative Key Recieved");
        }
        if(size / (double) keys.length > MAX_LOAD_FACTOR){
            rehash();
        }
        return putHelper(k, v, keys, values);
    }

    public LongKeyOpenHashMap(){this(10);}

    public LongKeyOpenHashMap(int initialCapacity){this(initialCapacity, 0.7);}

    public LongKeyOpenHashMap(int initalCapacity, double loadFactor){
        int cap = (int) (initalCapacity / loadFactor);
        MAX_LOAD_FACTOR = loadFactor;
        values = new int[cap];
        Arrays.fill(values, -1);
        keys = new long[cap];
        Arrays.fill(keys, -1);
    }

    private void rehash(){
        long[] newKeys = new long[(int) (keys.length * incrAmount)];
        int[] newVals = new int[(int) (keys.length * incrAmount)];

        Arrays.fill(newKeys, -1);
        Arrays.fill(newVals, -1);

        size = 0;

        for(int i=0; i < keys.length; ++i){
            long curr = keys[i];
            if(curr != -1){
                int val = values[i];
                putHelper(curr, val, newKeys, newVals);
            }
        }

        keys = newKeys;
        values = newVals;
    }

    private boolean putHelper(long k, int v, long[] keyArr, int[] valArr){
        int pos = getInitialPos(k, keyArr);
        long curr = keyArr[pos];
        while(curr != EMPTY_KEY && curr != k){
            pos++;
            if(pos == keyArr.length) pos = 0;
            curr = keyArr[pos];
        }

        valArr[pos] = v;

        if(curr == EMPTY_KEY){
            size++;
            keyArr[pos] = k;
            return true;
        }
        if(curr == k){
            return true;
        }
        return false;
    }

    private int getInitialPos(long k, long[] keyArr){
        int hash = (int)(k ^ (k >>> 32)) * 3875239;
        int pos = hash % keyArr.length;
        if(pos<0) pos += keyArr.length;

        return pos;
    }

    public int get(long k){
        int pos = find(k);
        // Returns -1 if key not found
        return values[pos];
    }

    public int getFerts(long k){
        int pos = find(k);
        // Returns -1 if key not found
        return ferts[pos];
    }

    public int getAlphas(long k){
        int pos = find(k);
        return alphas[pos];
    }

    public int getAtemp(long k){
        int pos = find(k);
        return atemp[pos];
    }

    private int find(long k) {
        int pos = getInitialPos(k, keys);
        long curr = keys[pos];

        while (curr != EMPTY_KEY && curr != k) {
            pos++;
            if (pos == keys.length) pos = 0;
            curr = keys[pos];
        }
        return pos;
    }

    public int size() {
        return size;
    }

    public long[] getKeyArray(){
        return keys;
    }

    public boolean incrementFertsAt(long k){
        if(ferts == null){
            ferts = new int[keys.length];
        }

        int pos = find(k);
        if(values[pos] == -1){
            throw new Error("Key not found when adding fertility");
        }
        else{
            int f = ferts[pos];
            ferts[pos] = f == -1 ? 1 : f +1;
            return true;
        }

    }

    public boolean incrementAlphasAt(long k){
        if(alphas == null){
            alphas = new int[keys.length];
        }

        int pos = find(k);
        if(values[pos] == -1){
            throw new Error("Key not found when adding fertility");
        }
        else{
            int a = alphas[pos];
            alphas[pos] = a == -1 ? 1 : a +1;
            return true;
        }

    }

    public boolean incrementAtempAt(long k){
        if(atemp == null){
            atemp = new int[keys.length];
        }

        int pos = find(k);
        if(values[pos] == -1){
            throw new Error("Key not found when adding fertility");
        }
        else{
            int a = atemp[pos];
            atemp[pos] = a == -1 ? 1 : a +1;
            return true;
        }

    }


//    public boolean totalFerts(){
//        int sum = 0;
//
//    }

}
