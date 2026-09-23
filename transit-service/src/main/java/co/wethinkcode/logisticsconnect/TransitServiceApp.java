package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransitServiceApp {

    private static final String HUB_SERVICE_URL = "http://localhost:7051/hubs/";
    private static final String DELAY_STAGE_SERVICE_URL = "http://localhost:7052/delay-stage/";

    // amount of hours in a day (constant)
    private static final int BASE_HOURS = 24;

    // each delay stage (0-8) adds this many extra hours to the estimate
    // eg stage 4 -> 24 + (4 * 2) = 32 hours
    private static final int HOURS_PER_STAGE = 2;

    // one shared HttpClient for the whole service,
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        // eg -> GET /transit/H-500
        app.get("/transit/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();

            Hub hub = fetchHub(hubId);
            if (hub == null) {
                ctx.status(404).result("No hub found with id " + hubId);
                return;
            }

            int delayStage = fetchDelayStage(hubId);
            int estimatedHours = BASE_HOURS + (delayStage * HOURS_PER_STAGE);

            TransitEstimate estimate = new TransitEstimate();
            estimate.setHubId(hub.getHubId());
            estimate.setSortingCenter(hub.getSortingCenter());
            estimate.setProvince(hub.getProvince());
            estimate.setDelayStage(delayStage);
            estimate.setEstimatedHours(estimatedHours);

            ctx.json(estimate);
        });
    }

    /**
     * Calls hub-service to look up one hub by ID.
     * Returns null if the hub doesn't exist (hub-service returned 404) or if
     * hub-service couldn't be reached at all — either way, the caller above
     * treats "null" as "this hub isn't available."
     */
    private static Hub fetchHub(String hubId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HUB_SERVICE_URL + hubId))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.out.println("Could not reach hub-service: " + e.getMessage());
            return null;
        }

        if (response.statusCode() == 404) {
            // "hub" genuinely doesn't exist —> not an error, just "not found"
            return null;
        }

        if (response.statusCode() != 200) {
            System.out.println("hub-service returned unexpected status " + response.statusCode());
            return null;
        }

        try {
            return mapper.readValue(response.body(), Hub.class);
        } catch (Exception e) {
            System.out.println("Could not parse hub-service response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calls delay-stage-service to look up the current delay stage for a hub.
     * Defaults to 0 (no delay) if delay-stage-service can't be reached or the
     * response can't be parsed, rather than failing the whole /transit request
     * just because this one piece of information wasn't available.
     */
    private static int fetchDelayStage(String hubId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DELAY_STAGE_SERVICE_URL + hubId))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.out.println("Could not reach delay-stage-service: " + e.getMessage());
            return 0;
        }

        if (response.statusCode() != 200) {
            System.out.println("delay-stage-service returned unexpected status " + response.statusCode());
            return 0;
        }

        try {
            StageResponse stageResponse = mapper.readValue(response.body(), StageResponse.class);
            return stageResponse.getStage();
        } catch (Exception e) {
            System.out.println("Could not parse delay-stage-service response: " + e.getMessage());
            return 0;
        }
    }
}
