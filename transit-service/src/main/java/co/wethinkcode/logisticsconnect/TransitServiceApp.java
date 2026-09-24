package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransitServiceApp {

    private static final String HUB_SERVICE_URL = "http://localhost:7051/hubs/";

    private static final int BASE_HOURS = 24;
    private static final int HOURS_PER_STAGE = 2;

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();


    private static final Map<String, Integer> latestStageByHubId = new ConcurrentHashMap<>();

    public static void main(String[] args) throws JMSException {
        setUpMqConsumer();

        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/transit/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();

            Hub hub = fetchHub(hubId);
            if (hub == null) {
                ctx.status(404).result("No hub found with id " + hubId);
                return;
            }

            // no REST call here anymore —> just read whatever the MQ
            // subscriber has most recently stored for this hub,
            // default to 0 if no update recieved
            int delayStage = latestStageByHubId.getOrDefault(hubId, 0);
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
     * Connects to the ActiveMQ broker and subscribes to package-status-topic.
     * Every time delay-stage-service publishes a change, the message listener
     * below fires and updates our local cache.
     */
    private static void setUpMqConsumer() throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(MqConfig.TOPIC);
        MessageConsumer consumer = session.createConsumer(topic);

        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage textMessage) {
                    String body = textMessage.getText();
                    StageUpdateMessage update = mapper.readValue(body, StageUpdateMessage.class);
                    latestStageByHubId.put(update.getHubId(), update.getStage());
                    System.out.println("[transit-service] received stage update: " + body);
                }
            } catch (Exception e) {
                System.out.println("Failed to process stage update message: " + e.getMessage());
            }
        });

        System.out.println("[transit-service] subscribed to topic '" + MqConfig.TOPIC
                + "' at " + MqConfig.BROKER_URL);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                consumer.close();
                session.close();
                connection.close();
            } catch (JMSException ignored) {
            }
        }));
    }

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

        // http code error handling
        if (response.statusCode() == 404) {
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
}