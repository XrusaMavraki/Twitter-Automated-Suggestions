package googlePlacesApi;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Map;

/**
 * Created by xrusa on 28/11/2017.
 */
@JsonTypeName("geometry")
public class GoogleGeometry {
    private Map<String,Double> location;

    public Map<String, Double> getLocation() {
        return location;
    }

    public void setLocation(Map<String, Double> location) {
        this.location = location;
    }
}
