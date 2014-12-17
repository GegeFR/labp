/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.core;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.cluster.Member;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import test.Constants;

/**
 *
 * @author Gwen
 */
public class ClusterServicesRegistry {

    private static ClusterServicesRegistry instance;

    public static void initInstance(ActorSystem system) {
        instance = new ClusterServicesRegistry(system);
    }

    public static ClusterServicesRegistry getInstance() {
        return instance;
    }

    private final LoggingAdapter log;
    private final Map<Address, Double> weightByMembers = new HashMap();
    private final WheightedMap<Address, ActorSelection> luaByMember = new WheightedMap(weightByMembers);
    private final WheightedMap<Address, ActorSelection> lfsByMember = new WheightedMap(weightByMembers);
    private final WheightedMap<Address, ActorSelection> wuiByMember = new WheightedMap(weightByMembers);
    private final Map<String, WheightedMap<Address, ActorSelection>> mapByRole = new HashMap();

    private ClusterServicesRegistry(ActorSystem system) {
        log = Logging.getLogger(system, this.getClass());
        mapByRole.put(Constants.ROLE_LFS, lfsByMember);
        mapByRole.put(Constants.ROLE_LUA, luaByMember);
        mapByRole.put(Constants.ROLE_WUI, wuiByMember);
    }

    public synchronized void registerMember(Address member, Double weight) {
        if (!weightByMembers.containsKey(member)) {
            log.info("register member " + member + " with weight " + weight);
            weightByMembers.put(member, weight);
        }
        else {
            log.warning("tried to update weight of non-registred member : ignored");
        }
    }

    public synchronized void updateMember(Address member, Double newWeight) {
        Double oldWeight = weightByMembers.get(member);
        if (null != oldWeight) {
            log.info("update member " + member + " with weight " + newWeight);
            weightByMembers.put(member, newWeight);
        }
        else {
            log.warning("tryed to update weight of non-registred member : ignored");
        }
    }
    
    public synchronized void recomputeAll() {
        luaByMember.recompute();
        lfsByMember.recompute();
        wuiByMember.recompute();
    }

    public synchronized void unregisterMember(Address member) {
        luaByMember.remove(member);
        lfsByMember.remove(member);
        wuiByMember.remove(member);
        weightByMembers.remove(member);
    }

    public synchronized void registerService(String role, Address member, ActorSelection service) {
        log.info("register service for member " + member + ", with role " + role + ": " + service);
        if (weightByMembers.containsKey(member)) {
            mapByRole.get(role).put(member, service);
        }
        else {
            throw new RuntimeException("unknown member");
        }
    }

    public synchronized ActorSelection getService(String role, Address member) {
        return mapByRole.get(role).get(member);
    }

    public synchronized ActorSelection getService(String role) {
        log.info("get for role " + role);
        return mapByRole.get(role).get();
    }

    public class WheightedMap<K, V> {

        private final Random random;
        private final Map<K, Double> weightByKey;
        private final Map<K, V> theMap;
        private Object[] keys;
        private double[] weights;

        public WheightedMap(Map<K, Double> weightsByKey) {
            random = new Random();
            weightByKey = weightsByKey;
            theMap = new HashMap();
            keys = new Object[0];
            weights = new double[0];
        }

        public synchronized void recompute() {
            keys = new Object[theMap.size()];
            weights = new double[theMap.size()];

            int i = 0;
            for (K key : theMap.keySet()) {
                keys[i] = key;
                if (i > 0) {
                    weights[i] = weights[i - 1] + weightByKey.get(key);
                }
                else {
                    weights[i] = weightByKey.get(key);
                }
                i++;
            }
        }

        public synchronized V get() {
            if (weights.length == 0) {
                return null;
            }

            double rand = random.nextDouble() * weights[weights.length - 1];

            log.debug("-- rand = " + rand);
            for (int i = 0; i < keys.length; i++) {
                log.debug("-- key = " + keys[i]);
                if (rand < weights[i]) {
                    return theMap.get((K) keys[i]);
                }
            }

            throw new RuntimeException("weight greater than maximum weight in array ... problem");
        }

        public V get(K key) {
            return theMap.get(key);
        }

        public V remove(K key) {
            V ret = theMap.remove(key);
            recompute();
            return ret;
        }

        public synchronized V put(K key, V value) {
            V ret = theMap.put(key, value);
            recompute();
            return ret;
        }

    }
}
