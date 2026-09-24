package co.wethinkcode.logisticsconnect;

/**
 * Represents the JSON body of a POST /delay-stage/{hubId} request
 * eg. {"stage": 4}
 */
public class StageUpdateRequest {

    private int stage;

    public StageUpdateRequest() {
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }
}