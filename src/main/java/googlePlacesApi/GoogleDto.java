package googlePlacesApi;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Created by xrusa on 28/11/2017.
 */
@JsonTypeName("result")
public class GoogleDto {
    private String name;
    private Double rating;
    private GoogleGeometry geometry;

    public GoogleGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(GoogleGeometry geometry) {
        this.geometry = geometry;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
