package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class DataStore {

    private DataStore() {}

   
    private static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    
    private static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

  
    private static final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

  

    public static ConcurrentHashMap<String, Room> getRooms() {
        return rooms;
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static void putRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public static boolean deleteRoom(String id) {
        return rooms.remove(id) != null;
    }

    public static boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

   
    public static ConcurrentHashMap<String, Sensor> getSensors() {
        return sensors;
    }

    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public static void putSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.putIfAbsent(sensor.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    public static boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

   

    public static List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, Collections.emptyList());
    }

    public static void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(reading);
    }
}
