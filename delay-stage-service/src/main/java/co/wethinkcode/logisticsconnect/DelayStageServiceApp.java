package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DelayStageServiceApp {

    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;

    private static final Map<String, Integer> stagesByHubId = new HashMap<>();

    private static Session mqSession;
    private static MessageProducer mqProducer;

    public static void main(String[] args) throws JMSException {
        setUpMqProducer();

        Javalin app = Javalin.create().start(7052);

        app.get("/health", ctx -> ctx.result("OK"));

        // read the current stage for one hub
        // eg -> GET /delay-stage/H-500
        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();
            int currentStage = stagesByHubId.getOrDefault(hubId, MIN_STAGE);

            StageUpdateRequest response = new StageUpdateRequest();
            response.setStage(currentStage);
            ctx.json(response);
        });

        // update the stage for one hub
        // eg -> POST /delay-stage/H-500  --- {"stage": 4}
        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();

            StageUpdateRequest requestBody = ctx.bodyAsClass(StageUpdateRequest.class);
            int newStage = requestBody.getStage();

            if (newStage < MIN_STAGE || newStage > MAX_STAGE) {
                ctx.status(400).result("Stage must be between " + MIN_STAGE + " and " + MAX_STAGE
                        + ", got " + newStage);
                return;
            }

            stagesByHubId.put(hubId, newStage);
            System.out.println("[delay-stage-service] hub " + hubId + " stage set to " + newStage);

            // update latest stage
            publishStageChange(hubId, newStage);

            ctx.status(200).result("Hub " + hubId + " stage updated to " + newStage);
        });
    }

    /**
     * Connects to the ActiveMQ broker once at startup and creates a producer
     * pointed at package-status-topic, ready to publish messages whenever the
     * POST handler above needs to.
     */
    private static void setUpMqProducer() throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        mqSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = mqSession.createTopic(MqConfig.TOPIC);
        mqProducer = mqSession.createProducer(topic);

        System.out.println("[delay-stage-service] ready to publish to topic '" + MqConfig.TOPIC
                + "' at " + MqConfig.BROKER_URL);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mqProducer.close();
                mqSession.close();
                connection.close();
            } catch (JMSException ignored) {
            }
        }));
    }

    /**
     * Builds a small JSON message and sends it to package-status-topic.
     */
    private static void publishStageChange(String hubId, int newStage) {
        String payload = "{\"hubId\":\"" + hubId + "\",\"stage\":" + newStage
                + ",\"timestamp\":\"" + Instant.now() + "\"}";

        try {
            TextMessage message = mqSession.createTextMessage(payload);
            mqProducer.send(message);
            System.out.println("[delay-stage-service] published to topic: " + payload);
        } catch (JMSException e) {
            System.out.println("Failed to publish stage change: " + e.getMessage());
        }
    }
}