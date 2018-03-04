package training;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import cleanAndModel.CleanTweets;
import mongo.MongoDbConnect;
import org.bson.Document;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by xrusa on 13/2/2018.
 */
public class CreateEvaluationTweets
{
    private static  MongoCollection<Document> evaluationTweetsDB = MongoDbConnect.getCollection("Tweets_Valuation");
    //evaluationTweets.txt

    private static final String[] airportKeywords={
            " αεροπλάνo "," airplane "," αεροδρόμιο ", " aegeanairlines "," departure "," airspace "," airlines "
            ," landed "," plane ","egyptair","πτήσεις","Alitalia","flights"," air ", "airport"
    };
    private static final String[] cultureKeywords={
            " parthenon "," ακρόπολη "," ancient "," μουσεί", " cultur"," αρχαί","museum"
    };

    private static final String[] funNatureSportKeywords={
            " ταινί"," film "," μπάσκετ "," διακοπές "," stadium ", " beach "," θάλασσ"," sunset "
    };
    private static final String[] coffeeNighttimeFoodKeywords={
            " καφ"," φαγη", " εστιατόρ"," restaur", " nightlife ","instafood ", "ouzo","food"
    };
    private static final String[] shoppingBeautyKeywords={
            " fashion", " shopping ", " μαλλ", " hair ", " mall ", "boho"," price ", " buy ", "fashion", " ψων",
            " shoes", " spa ", " massage", " pamper", " price "
    };
    private static final String[] irrelevantKeywords={
            "μνημονί", "νοσοκομ", "nurse", "σεισμ", " οφειλ", "αλλοδ", " minist"," debt ", " earthquake "
    };
    private static final String[][] categoryKeywords= {airportKeywords,cultureKeywords,funNatureSportKeywords,coffeeNighttimeFoodKeywords,
            shoppingBeautyKeywords,irrelevantKeywords};

    /**
     * will get 222 tweets for each category so 1332 in total to evaluate the model.
     * The tweets were collected during phase 1 of the project and were not used in training or test sets.
     * */
    public static void createEvaluationFile(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        AtomicInteger counter= new AtomicInteger(0);
        AtomicInteger oopscounter= new AtomicInteger(0);
        Set<TweetInfo> allCategoryTweets= new HashSet<>();
        IntStream.of(4, 0, 1, 2, 3, 5).forEach(i -> {
            int maxCounter=184;
            Set<TweetInfo> categoryTweets= createCategory(i);
            System.out.println("category "+i+" size "+categoryTweets.size());
            for(TweetInfo tweetInfo: categoryTweets){
                if (allCategoryTweets.contains(tweetInfo)) {
                    oopscounter.incrementAndGet();
                    continue;
                }
                allCategoryTweets.add(tweetInfo);
                // System.out.println("category "+i+" maxcounter: "+ maxCounter);
                if(maxCounter==0) break;
                writer.println(counter.get());
                writer.println("***");
                writer.println(tweetInfo.getTweet());
                writer.println("***");
                writer.println(tweetInfo.getLang());
                writer.println("***");
                writer.println(tweetInfo.geo);

                counter.incrementAndGet();
                maxCounter--;
            }
        });
        System.out.println("oops counter "+ (oopscounter));

        writer.flush();

    }

    private static Set<TweetInfo> createCategory(int category) {
        Set<TweetInfo> categoryTweets = new HashSet<>();
        for (int i = 0; i < categoryKeywords[category].length; i++) {
            BasicDBObject query = new BasicDBObject("text", new BasicDBObject("$regex", ".*" + categoryKeywords[category][i] + ".*"));
            FindIterable<Document> docs = evaluationTweetsDB.find(query);

            for (Document doc : docs) {
                String tweet = doc.getString("text");
                String lang = doc.getString("lang");
                if (!lang.equals("en") && !lang.equals("el")) continue;
                String[] tweetwords= CleanTweets.getInstance().cleanWords(tweet,lang);
                tweet= Arrays.stream(tweetwords).filter(Objects::nonNull).collect(Collectors.joining(" "));

                List<Double> geo;
                if (doc.get("geo") != null) {
                    geo = ((ArrayList<Double>) ((Document) doc.get("geo")).get("coordinates"));
                } else {
                    geo = null;
                }
                TweetInfo tweetInfo = new TweetInfo(tweet, lang, geo);
                if (categoryTweets.contains(tweetInfo)) continue;


                categoryTweets.add(tweetInfo);
            }
        }
        return categoryTweets;
    }

    private static class TweetInfo{
        private String tweet;
        private String lang;
        private List<Double> geo;
        TweetInfo (String tweet,String lang,List<Double> geo){
            this.tweet=tweet;
            this.lang=lang;
            this.geo=geo;
        }

        public String getTweet() {
            return tweet;
        }

        public String getLang() {
            return lang;
        }
        public List<Double> getGeo(){
            return geo;
        }

        @Override
        public boolean equals(Object o) {
//             if (this == o) return true;
//             if (o == null || getClass() != o.getClass()) return false;
//
//             TweetInfo tweetInfo = (TweetInfo) o;
//
//             if (tweet != null ? !tweet.equals(tweetInfo.tweet) : tweetInfo.tweet != null) return false;
//             return lang != null ? lang.equals(tweetInfo.lang) : tweetInfo.lang == null;

            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TweetInfo tweetInfo = (TweetInfo) o;
            return tweet!=null&& tweet.equals(tweetInfo.getTweet());
        }

        @Override
        public int hashCode() {
            int result = tweet != null ? tweet.hashCode() : 0;
            result = 31 * result + (lang != null ? lang.hashCode() : 0);
            return result;
        }
    }
}
