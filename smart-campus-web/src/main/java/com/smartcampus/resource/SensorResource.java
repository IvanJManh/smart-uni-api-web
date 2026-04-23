package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(DataStore.getSensors().values());

        if (type != null && !type.isBlank()) {
            sensorList = sensorList.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensorList).build();
    }

    
    @GET
    @Path("{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildError("Sensor '" + sensorId + "' not found"))
                    .build();
        }
        return Response.ok(sensor).build();
    }

  
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildError("id is required"))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildError("roomId is required"))
                    .build();
        }

      
        synchronized (DataStore.getSensors()) {
            if (!DataStore.roomExists(sensor.getRoomId())) {
                throw new LinkedResourceNotFoundException(
                        "The roomId '" + sensor.getRoomId() + "' does not exist. " +
                        "Please create the room before registering a sensor to it."
                );
            }

            if (DataStore.sensorExists(sensor.getId())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(buildError("Sensor with id '" + sensor.getId() + "' already exists"))
                        .build();
            }

            
            if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
                sensor.setStatus("ACTIVE");
            }

            DataStore.putSensor(sensor);

            // Register sensorId on the parent room
            DataStore.getRoom(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        }

        URI location = UriBuilder.fromResource(SensorResource.class)
                .path(sensor.getId())
                .build();

        return Response.created(location).entity(sensor).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Map<String, String> buildError(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}
