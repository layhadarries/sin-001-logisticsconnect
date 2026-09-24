package co.wethinkcode.logisticsconnect;

/**
 * Same shape as hub-service's Hub class — duplicated here because
 * transit-service is its own independent Maven module and needs to parse the
 * JSON that hub-service's GET /hubs/{hubId} endpoint returns.
 */
public class Hub {

    private String hubId;
    private String province;
    private String sortingCenter;
    private Boolean active;

    public Hub() {
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getSortingCenter() {
        return sortingCenter;
    }

    public void setSortingCenter(String sortingCenter) {
        this.sortingCenter = sortingCenter;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}