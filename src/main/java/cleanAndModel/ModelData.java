package cleanAndModel;

import mongo.MongoDbConnect;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * Created by xrusa on 25/9/2017.
 *
 * Google Places Api key : AIzaSyCXkgWPkSbF4-yozHFghuozZYHi5O0GSUs
 */

public class ModelData {


    //    private Map<ArrayList<String>,double[]> locationNamesToCoordinates= new HashMap<>();
    private static List<HashMapData> locations = new ArrayList<>();
    private Map<Integer,String> tweetIndexToText = new HashMap<>();
    private Map<String,Integer> uniqueWordsToIndex;
    private static ModelData instance= new ModelData();
    private ModelData() {
        readGrLocationsData("GR.txt");
        uniqueWordsToIndex = retrieveUniqueWordsToIndex();
    }

    public static ModelData getInstance() {
        return instance;
    }

    private void readGrLocationsData(String txtName) {
        //need to get the lines ignore the first number get all the string names add to arraylist
        //get lat long and put them in a HashMap
        Path path = Paths.get(txtName);
        try {
            Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
            lines.map(this::readDatafromLine).forEach(locations::add);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //find common words is crrently in cleanAndModel.CleanTweets. maybe move here?

    /**
     * used during Map, takes each line from the text document and returns a HashMapData object
     * which contains the names and the lat,long of the location
     * */
    private HashMapData readDatafromLine(String line){
        /*
        geonameid         : integer id of record in geonames database
        name              : name of geographical point (utf8) varchar(200)
        asciiname         : name of geographical point in plain ascii characters, varchar(200)
        alternatenames    : alternatenames, comma separated, ascii names automatically transliterated, convenience attribute from alternatename table, varchar(10000)
        latitude          : latitude in decimal degrees (wgs84)
        longitude         : longitude in decimal degrees (wgs84)
         */
        String[] splitLine=line.split("\t");
        double lat= Double.parseDouble(splitLine[4]);
        double lon= Double.parseDouble(splitLine[5]);
        Set<String> names= new HashSet<>();
        names.add(splitLine[1]);
        names.add(splitLine[2]);
        names.addAll(Arrays.asList(splitLine[3].split(",")));
        return new HashMapData(names,lat,lon);

    }

    public Map<Integer, Map<Integer,Double>> createModelIndexToValues(Iterable<Document> docs) {
        Map<Integer, Map<Integer, Double>> tweetIndexToModelIndexToValues = new HashMap<>();
//        double[][] data = new double[docsSize][uniqueWordsToIndex.size()];

        int tweetIndexCounter=0;
        for (Document doc : docs) {
            Map<Integer, Double> indexToValue = createIndexToValueForTweet(doc.getString("text"));
            tweetIndexToModelIndexToValues.put(tweetIndexCounter, indexToValue);
            tweetIndexToText.put(tweetIndexCounter, doc.getString("text"));

            tweetIndexCounter++;
        }

        return tweetIndexToModelIndexToValues;
    }

    public Map<Integer, Double> createIndexToValueForTweet(String text) {
        int i = 0;
        Map<Integer, Double> indexToValue = new TreeMap<>();
        String[] words = text.split(" ");
        Map<String, Long> wordsToCount =
                Arrays.stream(words).collect(groupingBy(Function.identity(), counting()));
        double[] data = new double[wordsToCount.size()];
        for (Long count : wordsToCount.values()) {
            data[i] = (double) count;
            i++;
        }
        //double normalisationFactor = Math.sqrt(dotProductSameArray(data));
        i = 0;
        for (Map.Entry<String, Long> entry : wordsToCount.entrySet()) {
            if (uniqueWordsToIndex.get(entry.getKey()) == null) continue;
            if (data[i] != 0) {
                //  data[i]=data[i]/normalisationFactor;
                indexToValue.put(uniqueWordsToIndex.get(entry.getKey()), data[i]);
            }
            i++;
        }
        return indexToValue;
    }


//

    //    private Set<Integer> createBannedIndexSet(String bannedFileName){
//        Set<Integer> bannedWords=new HashSet<>();
//        try (Stream<String> stream = Files.lines(Paths.get(bannedFileName+".txt"))) {
//            stream.map(this::handleLine).forEach(bannedWords::addAll);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return bannedWords;
//    }
    public List<Integer> handleLine(String line){
        String[] lines= line.split(",");
        List<Integer>indexes= new ArrayList<>();
        for(String l:lines){
            l= l.replaceAll("\\D+","");
            if(l==null||!(l.equals(""))) indexes.add( Integer.parseInt(l));
        }
        return indexes;

    }
    private FindIterable<Document> getCleanWordsFromDB(){
        return MongoDbConnect.getCollection("unique_words").find();
    }

    public static List<HashMapData> getLocations() {
        return locations;
    }
    public static class HashMapData{

        private Set<String> names;
        private double[] coordinates=new double[2];

        HashMapData(Set<String> names, double lat, double lon){
            this.names=names;
            this.coordinates[0]=lat;
            this.coordinates[1]=lon;
        }
        public boolean containsName(String name) {return names.stream().anyMatch(s -> s.equalsIgnoreCase(name));}

        public double[] getCoordinates() {
            return coordinates;
        }
    }

    private Map<String,Integer> retrieveUniqueWordsToIndex(){
        Map<String, Integer> uniqueWordsToIndex = new HashMap<>();
        int index = 0;
        FindIterable<Document> cleanWordsFromDb = getCleanWordsFromDB();
        for (Document doc : cleanWordsFromDb) {
            String text = doc.getString("text");
            uniqueWordsToIndex.put(text, index);
            index++;
        }
        return uniqueWordsToIndex;
    }

    public Map<Integer,String> getTweetIndexToText() {
        return tweetIndexToText;
    }

    public Map<Integer,String> getIndexToText(Iterable<Document> docs){
        Map<Integer,String> indexToText= new HashMap<>();
        int i=0;
        for (Document doc : docs) {
            indexToText.put(i++,doc.getString("text"));
        }
        return  indexToText;
    }

    public Map<String, Integer> getUniqueWordsToIndex() {
        return uniqueWordsToIndex;
    }

}
//in db executed:
    /*
        var m = function() {
        var words = this.text.split(" ");
        if (words) {
            for(var i=0; i<words.length; i++) {
                var words1=words[i].split("#");
                for(var j=0;j<words1.length;j++){
                    emit(words1[j].toLowerCase(), 1);
                }

            }
        }
    }

    var r = function(k, v) {
        return v.length;
    };

    db.TweetsCopy.mapReduce(
        m, r, { out: { merge: "words_count" } }
    );

     */