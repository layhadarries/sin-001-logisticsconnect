package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class HubServiceApp {
    private static final String INGESTION_SERVICE_URL = "http://localhost:7050/hubs";
    private static List<Hub> hubs = new ArrayList<>();

    public static void main(String[] args) throws  Exception {
        hubs = fetchHubsFromIngestionService();
//        System.out.println("hubs size: " + hubs.size());

        // start web server
        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));

        // return full list of hubs as JSON
        app.get("/hubs", ctx -> ctx.json(hubs));

        // return SINGLE hub by hubID -> http://localhost:7050/hubs/H-500
        app.get("/hubs/{hubId}", ctx -> {
            String requestHubId = ctx.pathParam("hubId").toUpperCase();
            Hub foundId = findHubId(requestHubId);

            if (foundId == null) {
                ctx.status(404).result("No hub found with id " + requestHubId);
            } else {
                ctx.json(foundId);
            }
        });
    }

    // calls ingestion service GET /hubs endpoint over http and turns json response
    // into List of Hub objects
    private static List<Hub> fetchHubsFromIngestionService() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_SERVICE_URL))
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.out.println("Could not reach ingestion-service at " + INGESTION_SERVICE_URL
                    + " (" + e.getMessage() + "). Starting with an empty hub list.");
            return new ArrayList<>();
        }

        if (response.statusCode() != 200 ) {
            System.out.println("Ingestion-service returned status (" + response.statusCode() + "). " +
                    "Starting with an empty hub list.");
            return new ArrayList<>();
        }

        ObjectMapper mapper = new ObjectMapper();
        Hub[] hubsArray = mapper.readValue(response.body(), Hub[].class);

        List<Hub> hubList = new ArrayList<>();
        for (Hub hub : hubsArray) {
            hubList.add(hub);
        }
        return hubList;
    }


    // iterate through hub list for correct id, else return null
    private static Hub findHubId(String hubId) {
        for (Hub hub : hubs) {
            if (hub.getHubId().equals(hubId)) {
                return hub;
            }
        }
        return null;
    }
}
