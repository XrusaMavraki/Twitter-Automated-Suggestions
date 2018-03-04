package mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import twitter4j.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;


/**
 * Created by xrusa on 11/6/2016.
 */
public class TwitterListener {
    //    public static MongoDatabase db= mongo.MongoDbConnect.getDb();
    private static final String OUTPUT_FILE_NAME = "tweets.json";
    private final BufferedWriter bufferedWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<String> KEYWORDS_OPTIONAL = new HashSet<>();
    private static final Set<String>  KEYWORDS_NECESSARY = new HashSet<>();
    static {
        KEYWORDS.addAll(Arrays.asList("athens","greece","hellas","culture","summer", "beach","travel"));
        KEYWORDS_OPTIONAL.addAll(Arrays.asList("culture","summer", "beach","travel"));
        KEYWORDS_NECESSARY.addAll(Arrays.asList("athens","greece","hellas"));
    }


    public TwitterListener() {
        try {
            if(!Files.exists(Paths.get(OUTPUT_FILE_NAME))) {
                Files.createFile(Paths.get(OUTPUT_FILE_NAME));
            }
            bufferedWriter = Files.newBufferedWriter(Paths.get(OUTPUT_FILE_NAME), Charset.forName("UTF-8"), StandardOpenOption.APPEND );
            startListener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    private void startListener() {

        StatusListener listener = new StatusListener() {

            @Override
            public void onStatus(Status status) {
                try {
                    saveTweets(status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            @Override
            public void onTrackLimitationNotice(int i) {

            }

            @Override
            public void onScrubGeo(long l, long l1) {

            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            @Override
            public void onException(Exception e) {

            }
        };
        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);
        FilterQuery filterQuery = new FilterQuery();


        double[][] locations = {{19.380,34.800}, {29.600,41.740}}; //covers greece according to Worldatlas.com
        filterQuery.locations(locations);
        String[] arr = new String[KEYWORDS.size()];
        arr = KEYWORDS.toArray(arr);
        filterQuery.track(arr);
        twitterStream.filter(filterQuery);

    }
    private void saveTweets(Status status) throws Exception {
        boolean flag = false;
        String text = status.getText().toLowerCase();
        for (HashtagEntity hashtagEntity : status.getHashtagEntities()) {
            String lowerHashtag = hashtagEntity.getText().toLowerCase();
            if (KEYWORDS.contains(lowerHashtag)) {
                if (KEYWORDS_OPTIONAL.contains(lowerHashtag)) {
                    for (String keyword : KEYWORDS_NECESSARY) {
                        if (text.contains(keyword)) {
                            flag = true;
                            break;
                        }
                    }
                } else {
                    flag = true;
                    break;
                }
            }
        }


        String tweet = TwitterObjectFactory.getRawJSON(status) + "\n";

        if (flag) {
            insertToDb(tweet);
//            bufferedWriter.write(tweet);
//            bufferedWriter.flush();
            System.out.println(status.getText());
        }
    }
    public void insertToDb(String tweet) throws IOException {

        Map<String,Object> map=objectMapper.readValue(tweet, new TypeReference<HashMap<String,Object>>() {});
        Document doc= new Document(map);
        MongoCollection<Document> collection= MongoDbConnect.getCollection("Tweets");
        collection.insertOne(doc);
    }
}










