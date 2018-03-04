package mongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xrusa on 5/8/2017.
 */
public class TxtToMongoMigration {

    public static void main_(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        try {
            Files.lines(Paths.get("tweets.json"))
                    .forEach(l -> {
                        try {
                            // creating an anonymous object of an anonymous clsss to make sure the map will be of type String,Object
                            Map<String, Object> map= objectMapper.readValue(l, new TypeReference<HashMap<String,Object>>() {});
                            Document doc= new Document(map);
                            success.incrementAndGet();
                            insertToDb(doc,"Tweets");

                        } catch (IOException e) {
                            System.err.println(l);
                            e.printStackTrace();
                            fail.incrementAndGet();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(success + " " + fail);
    }

    private static void insertToDb(Document doc,String collectionName){
        MongoCollection<Document> collection= MongoDbConnect.getCollection(collectionName);
        collection.insertOne(doc);
    }
}
