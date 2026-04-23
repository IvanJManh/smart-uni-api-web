package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.getRooms().values());
        return Response.ok(roomList).build();
    }

    
    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildError("id is required"))
                    .build();
        }
        if (room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildError("name is required"))
                    .build();
        }

        synchronized (DataStore.getRooms()) {
            if (DataStore.roomExists(room.getId())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(buildError("Room with id '" + room.getId() + "' already exists"))
                        .build();
            }
            DataStore.putRoom(room);
        }

        URI location = UriBuilder.fromResource(RoomResource.class)
                .path(room.getId())
                .build();

        return Response.created(location).entity(room).build();
    }

    
    @GET
    @Path("{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildError("Room '" + roomId + "' not found"))
                    .build();
        }
        return Response.ok(room).build();
    }

    
    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildError("Room '" + roomId + "' not found"))
                    .build();
        }

        
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted. It still has " +
                    room.getSensorIds().size() + " active sensor(s) assigned to it. " +
                    "Please remove all sensors before decommissioning this room."
            );
        }

        DataStore.deleteRoom(roomId);
        return Response.noContent().build();
    }

    private java.util.Map<String, String> buildError(String message) {
        java.util.Map<String, String> body = new java.util.LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}
