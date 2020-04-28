package edu.berkeley.nlp.assignments.assign1.student;

public class LongKeyHashWrapper {
    private int total = 0;
    private int size = 0;
    private LongKeyOpenHashMap lmMap;
    private int totalFerts = 0;

    public LongKeyHashWrapper(int initCap, double loadFactor){
        lmMap = new LongKeyOpenHashMap(initCap, loadFactor);
    }

    private boolean putKey(long k, int v){

        boolean done = lmMap.put(k, v);
        if(done){
            total++;
        }
        return done;
    }

    public boolean incrementAt(long k){
        int val = lmMap.get(k);
        if(val == -1){
            return putKey(k, 1);
        }
        else{
            return putKey(k, val+1);
        }
    }

    public boolean keyExists(long k){
        int val = lmMap.get(k);
        return val != -1;
    }

    public int get(long k){
        return lmMap.get(k);
    }

    public int getTotal(){
        return total;
    }

    public int getKeyCount(){
        return lmMap.size();
    }

    public long[] getKeyList() { return lmMap.getKeyArray();}

    public boolean incrementFerts(long k){ return lmMap.incrementFertsAt(k);}

    public int size(){
        if(size == 0){
            size = lmMap.size();
        }
        return size;
    }

    public int getFerts(long k){ return lmMap.getFerts(k);}

    public int getAlphas(long k){ return lmMap.getAlphas(k);}

    public int getAtemp(long k){ return lmMap.getAtemp(k);}

    public boolean incrementAlpha(long k){ return lmMap.incrementAlphasAt(k);}

    public boolean incrementAtemp(long k){ return lmMap.incrementAtempAt(k);}

//    public boolean getTotalFerts(){
//        if(totalFerts == 0){
//            totalFerts = lmMap.totalFerts();
//        }
//    }

}
