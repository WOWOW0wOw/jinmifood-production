package com.jinmifood.shop.web;
import java.io.Serializable;
import java.util.*;
public class Cart implements Serializable {
    private final Map<Long,Integer> quantities=new LinkedHashMap<>();
    public void add(Long id,int quantity){
        int safeQuantity=Math.min(Math.max(1,quantity),99);
        quantities.compute(id,(key,current)->Math.min(99,(current==null?0:current)+safeQuantity));
    }
    public void update(Long id,int quantity){if(quantity<=0)quantities.remove(id);else quantities.put(id,Math.min(quantity,99));}
    public void clear(){quantities.clear();} public Map<Long,Integer> getQuantities(){return Collections.unmodifiableMap(quantities);}
    public int getCount(){return quantities.values().stream().mapToInt(Integer::intValue).sum();} public boolean isEmpty(){return quantities.isEmpty();}
}
