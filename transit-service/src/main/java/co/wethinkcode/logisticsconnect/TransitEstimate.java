package co.wethinkcode.logisticsconnect;

// transit-service GET /transit/{hubId} endpoint
public class TransitEstimate {

    private String hubId;
    private String sortingCenter;
    private String province;
    private int delayStage;
    private int estimatedHours;

    public TransitEstimate() {
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public String getSortingCenter() {
        return sortingCenter;
    }

    public void setSortingCenter(String sortingCenter) {
        this.sortingCenter = sortingCenter;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public int getDelayStage() {
        return delayStage;
    }

    public void setDelayStage(int delayStage) {
        this.delayStage = delayStage;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
}