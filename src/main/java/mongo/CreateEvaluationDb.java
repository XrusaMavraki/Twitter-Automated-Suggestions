package mongo;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.*;
import java.util.*;

/**
 * Created by xrusa on 10/2/2018.
 */
public class CreateEvaluationDb {

//    public static void main(String[] args){
//        parseEvaluationFile("evaluationTweets.txt","Tweets_evaluation");
//    }
    public static void parseEvaluationFile(String filename,String dbName){
        FileInputStream evaluationStream = null;
        try {
            evaluationStream = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader= new BufferedReader(new InputStreamReader(evaluationStream));
        String line= null;
        String[] tweetData= {"",""};
        int counter=0;
        try {
            line = reader.readLine();

            while(line!=null) {
                if(line.isEmpty()) {
                    line=reader.readLine();
                    continue;
                }

                if(line.equals("***")){
                    counter++;
                line=reader.readLine();
                continue;
                }
                //index

                //tweet
                if(counter==1){
                    tweetData[0]+=line;

                }
                //lang
                if(counter==2){
                    tweetData[1]=line;


                }
                if(counter==3){
                    List<Double>geo= new ArrayList<>();
                    if(line.equals("null")) {
                        geo=null;
                    }
                    else {
                        line = line.replace("[", "");
                        line = line.replace("]", "");
                        String[] splitString = line.split(",");
                        geo.add(Double.parseDouble(splitString[0]));
                        geo.add(Double.parseDouble(splitString[1]));
                    }
                   getJsonForTweet(tweetData,geo,dbName);
                   counter=0;
                   tweetData=new String[]{"",""};
                }
                if(line.equals("***")){counter++;}
                line=reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void getJsonForTweet(String[]tweetData, List<Double> geo, String dbName){
        Map<String,Object> tweetDbEntry= new HashMap<>();
//        String[] tweetwords=CleanTweets.getInstance().cleanWords(tweetData[0],tweetData[1]);
//        String newText= Arrays.stream(tweetwords).collect(Collectors.joining(" "));
        tweetDbEntry.put("text",tweetData[0]);
        tweetDbEntry.put("lang",tweetData[1]);
        tweetDbEntry.put("geo",geo);
        insertToDb(tweetDbEntry,dbName);
    }
    private static void insertToDb(Map<String,Object> tweetDbEntry,String dbName){
        Document doc= new Document(tweetDbEntry);
        MongoCollection<Document> collection= MongoDbConnect.getCollection(dbName);
        collection.insertOne(doc);
    }
}
