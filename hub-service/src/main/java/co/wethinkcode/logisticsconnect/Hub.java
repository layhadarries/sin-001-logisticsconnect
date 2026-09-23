package co.wethinkcode.logisticsconnect;

// hub record
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

    @Override
    public String toString() {
        return "Hub{hubId='" + hubId + "', province='" + province
                + "', sortingCenter='" + sortingCenter + "', active=" + active + "}";
    }
}