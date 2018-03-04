package inputOutput;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import googlePlacesApi.GoogleResults;
import mongo.MongoDbConnect;
import org.bson.Document;
import twitter4j.*;
/**
 * Created by xrusa on 13/1/2018.
 */
public class TwitterOutboundService {

    private Map<Long, ContactedUserInfo> contactedUsers;
    private static TwitterOutboundService instance;
    private static final ObjectMapper objectmapper= new ObjectMapper();
    private MongoCollection<Document> contactedUsersCollection= MongoDbConnect.getCollection("Contacted-Users");
    private MongoCollection<Document> contactedUsersCollectionDataToTest= MongoDbConnect.getCollection("Contacted-UsersTEST");
    private MongoCollection<Document> blackListedUsersCollection= MongoDbConnect.getCollection("BlackListed-Users");

    private TwitterOutboundService() {
        try {
            createContactedUsers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createContactedUsers() throws IOException {
        BasicDBObject query= new BasicDBObject("user_id", new BasicDBObject("$exists",true));
        FindIterable<Document> contactedUsersInfo = contactedUsersCollection.find(query);
        contactedUsers=new ConcurrentHashMap<>();
        for(Document doc:contactedUsersInfo){
            Long id= doc.getLong("user_id");
            GoogleResults googleResults= objectmapper.readValue(doc.getString("googleResults"),GoogleResults.class);
            Date createdAt= doc.getDate("createdAt");
            ContactedUserInfo info= new ContactedUserInfo(googleResults,createdAt);
            contactedUsers.put(id,info);
        }
    }

    public static TwitterOutboundService getInstance() {
        if(instance == null) {
            instance = new TwitterOutboundService();
        }
        return instance;

    }
    public  void sendSuggestion(Status status, String statusAsString, IncomingTweetAnalysis.SuggestionPair suggestionPair) throws JsonProcessingException {
        Twitter twitter = new TwitterFactory().getInstance();
        try {

            if(contactedUsers.get(status.getUser().getId())!=null){return;}
            Status reply = twitter.updateStatus(new StatusUpdate(" @" + status.getUser().getScreenName() + " "+ suggestionPair.getSuggestion()).inReplyToStatusId(status.getId()));
            ContactedUserInfo info= new ContactedUserInfo(suggestionPair.getGoogleResults(),reply.getCreatedAt());
            contactedUsers.put(status.getUser().getId(),info);
            Document user= new Document();
            user.put("user_id",status.getUser().getId());
            user.put("googleResults",objectmapper.writeValueAsString(info.getResults()));
            user.put("createdAt",(info.getBotSentTweetAt()));
            contactedUsersCollection.insertOne(user);
            Document userTest= new Document();
            userTest.put("user_id",status.getUser().getId());
            userTest.put("googleResults",objectmapper.writeValueAsString(info.getResults()));
            userTest.put("createdAt",(info.getBotSentTweetAt()));
           // userTest.put("statusRepliedTo", TwitterObjectFactory.getRawJSON(status));
            ObjectMapper objectMapper= new ObjectMapper();
            Map<String,Object> map=objectMapper.readValue(statusAsString, new TypeReference<HashMap<String,Object>>() {});
            userTest.put("statusRepliedTo",map);
            userTest.put("repliedText",reply.getText());
            contactedUsersCollectionDataToTest.insertOne(userTest);
            System.out.println("Successfully updated the status to [" + reply.getText() + "].");

        } catch (TwitterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public void removeOldUsers(){
        BasicDBObject query= new BasicDBObject("createdAt", new BasicDBObject("$lt", new Date(LocalDateTime.now().minusDays(1L).toInstant(ZoneOffset.UTC).toEpochMilli())));
        contactedUsersCollection.deleteMany(query);
    }


    public static class ContactedUserInfo{

        private GoogleResults results;
        private Date botSentTweetAt;

        public ContactedUserInfo(GoogleResults results, Date botSentTweetAt) {
            this.results = results;
            this.botSentTweetAt = botSentTweetAt;
        }

        public GoogleResults getResults() {
            return results;
        }

        public Date getBotSentTweetAt() {
            return botSentTweetAt;
        }
    }
}
