package cleanAndModel;

import mongo.MongoDbConnect;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.index.analysis.SkroutzGreekStemmer;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Projections.include;


/**
 * Created by xrusa on 22/9/2017.
 */
public class CleanTweets {
    private  MongoCollection<Document> collection=MongoDbConnect.getCollection("Tweets_copy");
    private  Set<String> stopWordList;
    private  Map<String, Long> uniqueWords= new LinkedHashMap<>();

    private static final CleanTweets instance= new CleanTweets();
    private CleanTweets(){}
    public static CleanTweets getInstance(){
        return instance;
    }
    public static  void main(String args[]){

        CleanTweets c= getInstance();
     //   c.computeStatistics();
        System.out.println("Statistics Computed");
        c.stopWordList=c.createStopWordsList("./StopWords.txt");
        System.out.println("StopWord List created");

        for (String aStopWordList : c.stopWordList) {
            System.out.println(aStopWordList);
        }
        Map<String, Long> cleanWords=c.createUniqueWordsToCount();
        System.out.println("text cleaned");
        c.createUniqueWordDb(cleanWords);
        System.out.println("Unique words db created");
     //   c.showStats();
        c.createUniqueWordsFile();

    }


    private void createUniqueWordsFile() {
        try {
            Files.deleteIfExists(Paths.get("unique_words.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("unique_words.txt"), Charset.forName("UTF-8"))) {
            int i=0;
            for (Document doc : MongoDbConnect.getCollection(("unique_words")).find().projection(include("text"))) {
                writer.write("Id: "+i+ " " +doc.getString("text") + ", ");
                i++;
            }
        }catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Reads a stopWords comma separated file and adds the stopwords
     * in a TreeSet which is returned
     * */
    private Set<String> createStopWordsList(String path){
        try {
            FileInputStream file = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(file));
            String line;
            stopWordList= new TreeSet<>();
            while ((line = br.readLine()) != null)
            {
                String[] split=line.split(",");
                stopWordList.addAll(Arrays.asList(split));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWordList;
    }

    /**
     * for every tweet text words are separated,Url words are removed
     * and then all numeric and non word characters are removed. Finally, stopWords are removed.
     * A TreeSet of the unique words of the collection is returned.
     * */
//    public Iterable<Document> cleanTweetDocuments(String collectionName){
//        BasicDBObject query= new BasicDBObject("text", new BasicDBObject("$exists",true));
//        FindIterable<Document> findIterableTweets = MongoDbConnect.getCollection(collectionName).find(query).projection(include("text","lang"));
//        String text="";
//        String lang="";
//        List<Document> cleanDocs = new LinkedList<>();
//        Set<String> uniqueTweets= new HashSet<>();
//        for(Document f: findIterableTweets){
//            text=f.getString("text");
//            lang= f.getString("lang");
//            String [] words=cleanWords(text,lang);
//            if(words==null) continue;
//            String newText= Arrays.stream(words).collect(Collectors.joining(" ")); //giati edo se antithesi me to  cleanTweetsDataSet den koitai an einai sta unique words
//            if(!uniqueTweets.contains(newText)){
//                uniqueTweets.add(newText);
//                Document cleanDoc = new Document(f);
//                cleanDoc.put("text", newText);
//                cleanDocs.add(cleanDoc);
//            }
//
//        }
//        return cleanDocs;
//    }
    public Iterable<Document> cleanTweetDocuments(String collectionName){
        BasicDBObject query= new BasicDBObject("text", new BasicDBObject("$exists",true));
        FindIterable<Document> findIterableTweets = MongoDbConnect.getCollection(collectionName).find(query).projection(include("text","lang"));
        return cleanTweetsDataset(findIterableTweets,true);
    }
    private List<Document> cleanTweetsDataset(Iterable<Document> docs,boolean cleanTweetDocumentsCall){
        String lang;
        String text;
        List<Document> cleanTweets= new ArrayList<>();
        Set<String> uniqueTweets= new HashSet<>();
        for (Document f: docs){
            lang= f.getString("lang");
            text=f.getString("text");
            String[] words=cleanWords(text,lang);
            if(words==null) continue;
            String cleanTweet;
            if(cleanTweetDocumentsCall) {
                cleanTweet = Arrays.stream(words).collect(Collectors.joining(" "));
            }
            else{
                cleanTweet=Arrays.stream(words).filter(Objects::nonNull).filter(uniqueWords::containsKey).collect(Collectors.joining(" "));
            }
           // String cleanTweet=Arrays.stream(words).filter(Objects::nonNull).filter(uniqueWords::containsKey).collect(Collectors.joining(" "));
            if(!uniqueTweets.contains(cleanTweet)) {
                uniqueTweets.add(cleanTweet);
                Document tw = new Document(f);
                tw.put("text", cleanTweet);
                cleanTweets.add(tw);
            }
//          else{
//                System.out.println("dgdh");
//            }

        }
        return  cleanTweets;
    }


    private Map<String, Long> createUniqueWordsToCount(){
        BasicDBObject query= new BasicDBObject("text", new BasicDBObject("$exists",true));
        FindIterable<Document> findIterable = collection.find(query).projection(include("text", "lang"));
        String text="";
        String lang="";

        for ( Document f: findIterable){
            lang= f.getString("lang");
            text=f.getString("text");
            String[] words=cleanWords(text,lang);
            if(words==null) continue;
            Arrays.stream(words).filter(Objects::nonNull).forEach( word ->
                    uniqueWords.compute(word, (k, v) -> {
                        // k is same as word (the key of the map)
                        // v is the existing value (count) of this key
                        if (v == null) {
                            // if this is the first occurence, count is 1.
                            return 1L;
                        }
                        return v + 1;
                    })
            );

        }

        uniqueWords.remove("");
        stopWordList.forEach(uniqueWords::remove);
        uniqueWords.entrySet().removeIf(entry -> entry.getValue() == 1L);
        addToCleanTweetDB(findIterable);
        return uniqueWords;
    }


    public String[] cleanWords(String text,String lang){

        String[] words=text.split(" ");

        if(!("el".equals(lang) || "en".equals(lang))){
            return null;
        }
        for(int i=0;i<words.length;i++){
            words[i]=removeUrl(words[i]);
            if (!words[i].equals("")) {

                words[i] = words[i].replaceAll("[^A-Za-z\\u0388-\\u03EF]", "").toLowerCase();
            }

        }
        if(lang.equals("en")){
            words = stemEnglish(words);
        }
        else if(lang.equals("el")){
            words = stemGreek(words);
        }
        return words != null ? Arrays.stream(words).filter(w -> w != null && w.length() > 1).collect(Collectors.toList()).toArray(new String[0]) : null;
    }
    private String[] stemEnglish(String[]words){
        SnowballStemmer snowballStemmer = new englishStemmer();
        String[] stemmedWords= new String[words.length];
        for (int i=0;i<words.length;i++) {
            if(words[i].equals("")) continue;
            snowballStemmer.setCurrent(words[i]);
            snowballStemmer.stem();
            stemmedWords[i]= snowballStemmer.getCurrent();
        }
        return stemmedWords;

    }
    public String[] stemGreek(String[]words){
        SkroutzGreekStemmer stemmer= new SkroutzGreekStemmer();
        String[] stemmedWords= new String[words.length];
        char[] token;
        int tokenLength;
        int stemLength;
        for (int i=0;i<words.length;i++) {
            if(words[i].equals("")) continue;
            token = words[i].toCharArray();
            tokenLength=words[i].length();
            stemLength=stemmer.stem(token,tokenLength);
            stemmedWords[i]= new String(token, 0, stemLength);
        }
        return stemmedWords;
    }

    private void addToCleanTweetDB(FindIterable<Document> docs){
        MongoCollection<Document> cleanTweetCollection = MongoDbConnect.getCollection("clean_tweets");
        cleanTweetCollection.insertMany(cleanTweetsDataset(docs,false));
    }
    /**
     * Creates a database Collection which contains all unique words
     * */
    private void createUniqueWordDb(Map<String, Long> uniqueWords){
        MongoCollection<Document> wordCollection = MongoDbConnect.getCollection("unique_words");
        for(Map.Entry<String, Long> entry: uniqueWords.entrySet()) {
            Document uniqueWord = new Document("text", entry.getKey());
            uniqueWord.put("count", entry.getValue());
            wordCollection.insertOne(uniqueWord);
        }
    }

    /**
     * Removes Urls from a String and returns it.
     * */
    private String removeUrl(String commentstr)
    {
        String urlPattern = "(((http|ftp|https):\\/\\/)?[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?)";
        String altered= commentstr.replaceAll(urlPattern,"");
        if(!altered.equals(commentstr)){
            commentstr="";
        }
        return commentstr;
    }


    /**
     * Statistics such as number of Tweets collected, number of retweets, number of Fcated Tweets
     * and their averages are computed.
     * */


    //
    public FindIterable<Document> getDocumentsWords(String collectionName){
        BasicDBObject query= new BasicDBObject("text", new BasicDBObject("$exists",true));
        FindIterable<Document> docs=MongoDbConnect.getCollection(collectionName).find(query).projection(include("text"));
        return docs;
    }
    // public long getWordCount(){return wordCollection.count();}
    public long getDocumentCount(){
        return MongoDbConnect.getCollection("clean_tweets").count();
    }
    //  public  Map<String, Long> getUniqueWords(){ return uniqueWords;}

}


