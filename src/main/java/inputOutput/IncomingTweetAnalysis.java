package inputOutput;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import googlePlacesApi.GoogleDto;
import googlePlacesApi.GooglePlacesRequestHandler;
import googlePlacesApi.GoogleResults;
import cleanAndModel.CleanTweets;
import cleanAndModel.ModelData;
import mongo.MongoDbConnect;
import org.bson.Document;
import training.ArffCreator;
import training.TrainingPrep;
import training.WekaNaiveBayes;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import weka.classifiers.Classifier;
import weka.core.Instance;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xrusa on 29/11/2017.
 */
public class IncomingTweetAnalysis {

    private static final String ARFF_TEMPLATE_PATH = "arffTemplate.arff";
    private final Classifier classifier;
    private final long myID=960484154029826048L;
    private final double classificationThreshold;
    private final TwitterOutboundService twitterOutboundService;
    private final String[] categories=TrainingPrep.getCategories();
    private final List<ModelData.HashMapData> locations= ModelData.getLocations();
    private MongoCollection<Document> blackListedUsersCollection= MongoDbConnect.getCollection("BlackListed-Users");
    private final Map<Integer,List<String>> categoryIndexToGoogleTypeNames= new HashMap<Integer,List<String>>(){

        {
            //airport
            put(0,new ArrayList<String>(){
                {
                    add("airport");
                    add("bus_station");
                    add("taxi_stand");

                }
            });
            //culture
            put(1,new ArrayList<String>(){
                {
                    add("library");
                    add("museum");
                    add("art_gallery");
                }
            });
            //fun-nature-sport
            put(2,new ArrayList<String>(){
                {
                    add("stadium");
                    add("park");
                    add("amusement_park");
                    add("movie_theater");

                }
            });
            //coffee-nighttime-food
            put(3,new ArrayList<String>(){
                {
                    add("restaurant");
                    add("bar");
                    add("cafe");
                    add("night_club");

                }
            });
            //shopping-beauty
            put(4,new ArrayList<String>(){
                {
                    add("beauty_salon");
                    add("clothing_store");
                    add("hair_care");
                    add("shopping_mall");
                    add("spa");
                    add("store");
                }
            });
        }

    };

    public IncomingTweetAnalysis( Classifier classifier, double classificationThreshold) {
        this.classifier = classifier;
        this.classificationThreshold = classificationThreshold;
        twitterOutboundService = TwitterOutboundService.getInstance();

    }

    /**
     * Creates a template to be used by every incoming tweet in order to build its own arff file.
     * The template will be copied before being populated by each tweet.
     */

    public void handleIncomingTweet(Status tweetStatus,String statusAsString) {
        UserMentionEntity [] entities=tweetStatus.getUserMentionEntities();
        boolean flag=false;
        for(UserMentionEntity e:entities){
            if (e.getId()==myID) flag=true;
        }
        if(myID==tweetStatus.getInReplyToUserId()||flag ){
            String words[]= tweetStatus.getText().split(" ");
            for(String word:words){
                if ("STOP".equalsIgnoreCase(word)){
                    blackListUser(tweetStatus.getUser());
                }
            }
            return;
        }
        boolean blackListed= checkBlackListed(tweetStatus.getUser().getId());
        if(blackListed) return;

        GeoInfo tweetGeolocation = retrieveTweetGeolocation(tweetStatus);
        if(tweetGeolocation.getGeo()==null){
            tweetGeolocation=retrieveTweetGeolocationFromKnownGRLocations(tweetStatus.getText());
        }
        Map<Integer, Double> tweetModelData = prepareTweetForClassification(tweetStatus);
        if(tweetModelData.isEmpty()) return;
        String arffPath = createArffFileForTweet(tweetModelData);
        List<Integer> detectedCategoryList = detectTweetCategory(arffPath,0);
        try {
            Files.delete(Paths.get(arffPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] detectedGoogleTypeNames=detectedCategoriesToGoogleTypeNames(detectedCategoryList);
        try {
            SuggestionPair suggestion=null;
//            if((tweetGeolocation==null)&&(detectedGoogleTypeNames!=null)){
//                suggestion=constructSuggestion(detectedGoogleTypeNames,3);
//            }
//            else if((tweetGeolocation!=null)&&(detectedGoogleTypeNames==null)){
//                suggestion=constructSuggestion(tweetGeolocation,1000,3);
//            }
            if(tweetGeolocation==null||tweetGeolocation.getGeo()==null){
                System.out.println("Location not found continuing.");
            }
            else if((tweetGeolocation.getGeo()!=null && detectedGoogleTypeNames!=null)){
                suggestion = constructSuggestion(detectedGoogleTypeNames, tweetGeolocation,detectedCategoryList, 3);
            }

            if(suggestion!=null){
                System.out.println("Responded to tweet: "+tweetStatus.getText());
                twitterOutboundService.sendSuggestion(tweetStatus,statusAsString,suggestion);
            }
            else{
                System.out.println("Suggestion was null continuing.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void blackListUser(User user) {
        BasicDBObject query= new BasicDBObject("user_id", user.getId());
        FindIterable<Document> res= blackListedUsersCollection.find(query);
        if(res==null){
            Document blackListedUser= new Document();
            blackListedUser.put("user_id",user.getId());
            blackListedUsersCollection.insertOne(blackListedUser);
        }
    }
    private boolean checkBlackListed(long id) {
        BasicDBObject query= new BasicDBObject("user_id", id);
        FindIterable<Document> res= blackListedUsersCollection.find(query);
        return (res!=null);
    }

    private String[] detectedCategoriesToGoogleTypeNames(List<Integer> detectedCategories){
        List<String> googleTypeNames= new ArrayList<>();
        if(detectedCategories.contains(5)||detectedCategories.isEmpty()){
            return null;
        }
        for(Integer i:detectedCategories){
            if(categoryIndexToGoogleTypeNames.containsKey(i)){
                googleTypeNames.addAll(categoryIndexToGoogleTypeNames.get(i));
            }//i use If so that if something goes wrong and the category index is wrong it won't be affected.

        }
        if(googleTypeNames.isEmpty()){
            return null;
        }
        return googleTypeNames.toArray(new String[0]);
    }
    private GeoInfo retrieveTweetGeolocation(Status tweetStatus) {
        double []geolocation;
        if( tweetStatus.getGeoLocation()!=null) {
            double lat = tweetStatus.getGeoLocation().getLatitude();
            double lon = tweetStatus.getGeoLocation().getLongitude();
            geolocation= new double[] {lat,lon};
        }else{
            geolocation= null;

        }
        return new GeoInfo(geolocation,"GeolocatedTweet");
    }
    private GeoInfo retrieveTweetGeolocationFromKnownGRLocations(String tweetText) {
        String [] tweetWords= tweetText.split(" ");
        for(int i=0; i<tweetWords.length;i++) {
            tweetWords[i] = tweetWords[i].replaceAll("[^A-Za-z\\u0388-\\u03EF]", "").toLowerCase();

        }
        tweetWords= Arrays.stream(tweetWords).filter(s->!s.isEmpty()).collect(Collectors.toList()).toArray(tweetWords);

        for(int window=3;window>0;window--) {
            for (int i = 0; i < tweetWords.length - window; i++) {
                String wordsExamined = "";
                for (int j = 0; j < window; j++) {
                    wordsExamined += tweetWords[j + i] + " ";
                }
                wordsExamined = wordsExamined.trim();

                for (ModelData.HashMapData data : locations) {
                    if (data.containsName(wordsExamined)) {
                        return new GeoInfo(data.getCoordinates(),wordsExamined);
                    }
                }
            }
        }
        return null;
    }
    private Map<Integer, Double> prepareTweetForClassification(Status tweetStatus) {
        CleanTweets c= CleanTweets.getInstance();
        String[]words=c.cleanWords(tweetStatus.getText(),tweetStatus.getLang());
        ModelData d= ModelData.getInstance();
        if(words==null) return Collections.emptyMap();
        String newText= Arrays.stream(words).collect(Collectors.joining(" "));
        return d.createIndexToValueForTweet(newText);


    }

    private String createArffFileForTweet(Map<Integer, Double> modelIndexToValues) {
        Map<Integer,Map<Integer,Double>>  tweetIndexToModelIndexToValues= new HashMap<>();
        tweetIndexToModelIndexToValues.put(1,modelIndexToValues);
        Map<Integer,String> tweetIndexToCategory=new HashMap<>();
        tweetIndexToCategory.put(1,"?");
        ArffCreator arffCreator= new ArffCreator(categories,ModelData.getInstance().getUniqueWordsToIndex(),
                tweetIndexToModelIndexToValues,tweetIndexToCategory);
        String path="IncomingTweetArff"+ UUID.randomUUID()+".arff";
        try {
            arffCreator.createArrfFile(path,"IncomingTweetArff");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;

    }

    /**
     * Calls {@link Classifier#distributionForInstance(Instance)} in order to find the most probable category.
     * It returns the most probable category if it exceeds the passed {@link IncomingTweetAnalysis#classificationThreshold}
     *
     * Otherwise it will return "UNCERTAIN"
     *
     * @param path the path of the arff file
     * @return The detected tweet category.
     */
    private List<List<Integer>> detectTweetCategoryMultiple(String path) {
        WekaNaiveBayes nb=WekaNaiveBayes.getInstance();
        List<List<WekaNaiveBayes.CategoryHelper>> categories=nb.naiveBayesClassify(path);
        List<List<Integer>> detectedTweetCategoriesforAllTweets= new ArrayList<>();

        if(categories.size()!=0) {
            for (int i = 0; i < categories.size(); i++) {
                detectedTweetCategoriesforAllTweets.add(detectTweetCategory(path,i));
            }
            return detectedTweetCategoriesforAllTweets;
        }
        else{
            return null;
        }
    }
    private List<Integer> detectTweetCategory(String path,int counter){
        WekaNaiveBayes nb=WekaNaiveBayes.getInstance();
        List<List<WekaNaiveBayes.CategoryHelper>> categories=nb.naiveBayesClassify(path);

        List<WekaNaiveBayes.CategoryHelper> tweetDetectedCategories = categories.get(counter);
        List<Integer> detectedTweetCategoriesSingle = new ArrayList<>();
        for (WekaNaiveBayes.CategoryHelper helper : tweetDetectedCategories) {
            if (helper.getProbabilityValueResult() >= classificationThreshold) {
                detectedTweetCategoriesSingle.add(helper.getCategoryIndex());
            }
        }
        return detectedTweetCategoriesSingle;

    }

    private SuggestionPair constructSuggestion(String[] detectedCategories, GeoInfo geoInfo,List<Integer> detectedCategorList, int nResults) throws IOException {
        GoogleResults results= GooglePlacesRequestHandler.executeRequest(geoInfo.getGeo(),detectedCategories);
        if (results != null && results.getResults().isEmpty()) return null;
        return new SuggestionPair(constructSuggestionHelper(results,nResults,geoInfo.getGeoName(),detectedCategorList),results);

    }
//    private SuggestionPair constructSuggestion( double[] tweetGeolocation,int radius,int nResults)throws IOException{
//        GoogleResults results= GooglePlacesRequestHandler.executeRequest(tweetGeolocation,radius);
//        return new SuggestionPair(constructSuggestionHelper(results,nResults,detectedCategories,geoInfo.getGeoName()),results);
//    }
//    private SuggestionPair constructSuggestion(String[] detectedCategories,int nResults)throws IOException{
//        GoogleResults results= GooglePlacesRequestHandler.executeRequest(detectedCategories);
//        return new SuggestionPair(constructSuggestionHelper(results,nResults),results);
//    }

    private String constructSuggestionHelper( GoogleResults results,int nResults,String geoName,List<Integer>detectedCategoryList){
        List<GoogleDto> resList=results.getResults();
        String str;
        if(geoName.equals("GeolocatedTweet")){
            str="Do you like "+categories[detectedCategoryList.get(0)]+" ? We suggest: ";
        }
        else {
            str = "Do you like " + categories[detectedCategoryList.get(0)] + " in " + geoName + " ? We suggest: ";
        }
        String strBeforeResultAdded=str;
        for(int i=0;i<Math.min(nResults,resList.size());i++){

            str+= resList.get(i).getName();
            Double rating= resList.get(i).getRating();
            if(rating!=null) {
                str += " with rating " + resList.get(i).getRating();
            }
            str+=", ";
            if(str.length()>140){
                str=strBeforeResultAdded;
            }else{
                strBeforeResultAdded=str;
            }
        }
        return str;
    }

    protected static class SuggestionPair{
        private String suggestion;
        private GoogleResults googleResults;

        SuggestionPair(String suggestion, GoogleResults googleResults) {
            this.suggestion = suggestion;
            this.googleResults = googleResults;
        }

        String getSuggestion() {
            return suggestion;
        }

        GoogleResults getGoogleResults() {
            return googleResults;
        }
    }
    protected static class GeoInfo{

        private double[] geo;
        private String geoName;

        public GeoInfo(double[] geo, String geoName) {
            this.geo = geo;
            this.geoName = geoName;
        }

        public double[] getGeo() {
            return geo;
        }

        public String getGeoName() {
            return geoName;
        }
    }

}
