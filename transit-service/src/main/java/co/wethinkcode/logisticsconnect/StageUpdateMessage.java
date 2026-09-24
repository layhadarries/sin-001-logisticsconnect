package co.wethinkcode.logisticsconnect;

/**
 * matches the JSON delay-stage-service publishes to package-status-topic,
 * eg -> {"hubId": "H-500", "stage": 67, "timestamp": "blebleble"}.
 */
public class StageUpdateMessage {

    private String hubId;
    private int stage;
    private String timestamp;

    public StageUpdateMessage() {
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
