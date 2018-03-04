package googlePlacesApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xrusa on 28/11/2017.
 */
public class GooglePlacesRequestHandler {

    public static void main(String[] args) throws IOException {
        double []coordinates={-33.8670522,151.1957362};
        GoogleResults results = executeRequest(coordinates,500);
        System.out.println(results.getResults().get(0).getName());
        System.out.println(results.getResults().get(0).getRating());
        System.out.println(results.getResults().get(0).getGeometry().getLocation().get("lat"));
        System.out.println(results.getResults().get(0).getGeometry().getLocation().get("lng"));
        //worth looking at vicinity attribute
    }
//    String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362" +
//                  "&rankby=distance&type=cafe&keyword=cruise&key=AIzaSyCXkgWPkSbF4-yozHFghuozZYHi5O0GSUs";

    //deserialises Java object to JSON or reversed.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static GoogleResults executeRequest(double[] coordinates, String[]categories) throws IOException {
        String url= null;
        try {
            url = buildUrl(coordinates,categories);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return httprequestResults(url);
    }
    public static GoogleResults executeRequest(double [] coordinates, int radius) throws IOException {
        String url= buildUrl(coordinates,radius);
        return httprequestResults(url);
    }
    public static GoogleResults executeRequest(String[]categories)throws IOException{
        double[] AthensCoordinates={37.983810,23.727539};
        int radius=(int)(442.5696001690403*1000);
        String url= null;
        try {
            url = buildUrl(AthensCoordinates,categories,radius);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return httprequestResults(url);

    }
    private static GoogleResults httprequestResults(String url) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        HttpResponse response = client.execute(request);
        GoogleResults results = OBJECT_MAPPER.readValue(response.getEntity().getContent(), GoogleResults.class);
        return results;
    }


    private static String buildUrl(double[]coordinates, int radius){
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+
                coordinates[0]+","+coordinates[1] +"&radius="+radius+ "&key=AIzaSyCXkgWPkSbF4-yozHFghuozZYHi5O0GSUs";
        return url;
    }
    private static String buildUrl(double[]coordinates, String[]categories) throws URISyntaxException {

        URIBuilder uriBuilder=new URIBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json");
        uriBuilder.addParameter("location", coordinates[0]+","+coordinates[1]);
        //uriBuilder.addParameter("rankby","distance");
        uriBuilder.addParameter("radius","1000");
        uriBuilder.addParameter("opennow","true");
        uriBuilder.addParameter("type",categoryBuilderHelp(categories));
        uriBuilder.addParameter("key", "AIzaSyCXkgWPkSbF4-yozHFghuozZYHi5O0GSUs");

        return uriBuilder.toString();
    }

    private static String buildUrl(double[]coordinates,String[]categories,int radius) throws MalformedURLException, URISyntaxException {

        URIBuilder uriBuilder=new URIBuilder ("https://maps.googleapis.com/maps/api/place/nearbysearch/json");
        uriBuilder.addParameter("location", coordinates[0]+","+coordinates[1]);
        uriBuilder.addParameter("radius", Integer.toString(radius));
        uriBuilder.addParameter("type", categoryBuilderHelp(categories));
        uriBuilder.addParameter("key", "AIzaSyCXkgWPkSbF4-yozHFghuozZYHi5O0GSUs");
        return uriBuilder.toString();
    }
    private static String categoryBuilderHelp(String[]categories){
        StringBuilder builder= new StringBuilder();
        for(int i=0;i<categories.length;i++){
            if(i==(categories.length-1)){
                builder.append(categories[i]);
            }
            else {
                builder.append( categories[i] + "|");
            }
        }
        return builder.toString();
    }
}
