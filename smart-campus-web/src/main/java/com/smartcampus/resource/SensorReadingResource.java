package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

   
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

   
    @GET
    public Response getReadings() {
        if (!DataStore.sensorExists(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildError("Sensor '" + sensorId + "' not found"))
                    .build();
        }

        List<SensorReading> history = DataStore.getReadings(sensorId);
        return Response.ok(history).build();
    }

 
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.getSensor(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildError("Sensor '" + sensorId + "' not found"))
                    .build();
        }

        
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                    "and cannot accept new readings. Please try again after maintenance is complete."
            );
        }

       
        SensorReading newReading = new SensorReading(reading.getValue());
        DataStore.addReading(sensorId, newReading);

        
        sensor.setCurrentValue(newReading.getValue());

        return Response.status(Response.Status.CREATED).entity(newReading).build();
    }

    private Map<String, String> buildError(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}
