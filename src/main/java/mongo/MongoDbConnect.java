package mongo; /**
 * Created by xrusa on 20/6/2017.
 */

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.HashMap;
import java.util.Map;


public  class MongoDbConnect {

    private static MongoDatabase db;


    private static Map<String,MongoCollection<Document>> allConectedCollections = new HashMap<>();
    private MongoDbConnect(String collectionName){
        DbConnect(collectionName);
    }
    private  static void DbConnect(String collectionName){


        try{
            // To connect to mongodb server
            MongoClient mongoClient = new MongoClient( "localhost" , 27017 );

            // Now connect to your databases
            db = mongoClient.getDatabase("local");
            MongoCollection<Document> collection=db.getCollection(collectionName);
            allConectedCollections.put(collectionName,collection);
            System.out.println("Connect to database successfully");

        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }
    public static MongoDatabase getDb(){return db;}

    public static MongoCollection<Document> getCollection(String collectionName) {
        if(allConectedCollections.get(collectionName)== null){
            new MongoDbConnect(collectionName);
        }
        return allConectedCollections.get(collectionName);
    }


}


