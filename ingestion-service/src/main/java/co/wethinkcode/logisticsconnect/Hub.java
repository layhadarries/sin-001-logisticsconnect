package co.wethinkcode.logisticsconnect;

// hub construct to be sent over REST api
public class Hub {

    private final String hubId;
    private final String province;
    private final String sortingCenter;
    private final Boolean active;

    public Hub(String hubId, String province, String sortingCenter, Boolean active) {
        this.hubId = hubId;
        this.province = province;
        this.sortingCenter = sortingCenter;
        this.active = active;
    }

    public String getHubId() {
        return hubId;
    }

    public String getProvince() {
        return province;
    }

    public String getSortingCenter() {
        return sortingCenter;
    }

    public Boolean getActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Hub{hubId='%s', province='%s', sortingCenter='%s', active=%s}"
                .formatted(hubId, province, sortingCenter, active);
    }
}