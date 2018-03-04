import com.mongodb.client.MongoCollection;
import inputOutput.TwitterOutboundService;
import mongo.MongoDbConnect;
import org.bson.Document;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Created by xrusa on 25/2/2018.
 */
public class TwitterOutboundServiceTests {

    private TwitterOutboundService service = TwitterOutboundService.getInstance();

    @Test
    public void testOldUsersAreDeleted() {
        // insert some old users
        MongoCollection<Document> contactedUsersCollection= MongoDbConnect.getCollection("Contacted-Users");

        Document user= new Document();
        user.put("user_id", 1);
        user.put("createdAt", new Date(LocalDateTime.now().minusDays(2L).toInstant(ZoneOffset.UTC).toEpochMilli()));
        contactedUsersCollection.insertOne(user);

        user= new Document();
        user.put("user_id", 2);
        user.put("createdAt", new Date(LocalDateTime.now().minusDays(3L).toInstant(ZoneOffset.UTC).toEpochMilli()));
        contactedUsersCollection.insertOne(user);

        user= new Document();
        user.put("user_id", 3);
        user.put("createdAt", new Date(LocalDateTime.now().minusHours(2L).toInstant(ZoneOffset.UTC).toEpochMilli()));
        contactedUsersCollection.insertOne(user);

        user= new Document();
        user.put("user_id", 4);
        user.put("createdAt", new Date(LocalDateTime.now().minusHours(1L).toInstant(ZoneOffset.UTC).toEpochMilli()));
        contactedUsersCollection.insertOne(user);

        service.removeOldUsers();
    }
}
