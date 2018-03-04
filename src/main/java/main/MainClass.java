package main;

import com.sun.org.apache.xpath.internal.operations.Mod;
import mongo.CreateEvaluationDb;
import training.ModelEvaluation;
import training.NaiveBayesResultsVisualiser;
import training.TrainingPrep;
import training.WekaNaiveBayes;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;

import static com.mongodb.client.model.Projections.include;

/**
 * Created by xrusa on 22/6/2017.
 */
public class MainClass {

    public static void main(String[] args) throws Exception {
////        mongo.TwitterListener listener= new mongo.TwitterListener();
//      //  ModelData modelData = new ModelData();
////        FindIterable<Document> iterable = MongoDbConnect.getCollection("Tweets_copy").find();
//       // modelData.incomingTweetAnalysis(iterable);
//        MongoCollection<Document> source = MongoDbConnect.getCollection("Tweets");
//        MongoCollection<Document> existing = MongoDbConnect.getCollection("Tweets_copy");
//        MongoCollection<Document> target = MongoDbConnect.getCollection("Tweets_Valuation");
////        MongoDatabase db = MongoDbConnect.getDb();
////        BasicDBObject command = new BasicDBObject();
////        command.put("eval", "(function() {vals = db.Tweets_copy.find({}).map(function(a){return a._id;});\n" +
////                "return db.Tweets.find({_id: {$nin: vals}}).toArray();\n" +
////                "})();");
////        FindIterable<Document> asd = db.runCommand(command, FindIterable.class);
//        Set<String> existingIds = new HashSet<>();
//        FindIterable<Document> existingDocs = existing.find().projection(include("_id"));
//        for (Document doc : existingDocs) {
//            existingIds.add(doc.getObjectId("_id").toString());
//        }
//        System.out.println("Found existing: " + existingIds.size());
//        FindIterable<Document> allDocs = source.find();
//        int counter = 0;
//        for (Document doc : allDocs) {
//            if (counter % 1000 == 0) {
//                System.out.println("Counter: " + counter);
//            }
//            if (!existingIds.contains(doc.getObjectId("_id").toString())) {
//                target.insertOne(doc);
//                counter++;
//            }


        //CreateEvaluationTweets.createEvaluationFile("evaluationTweets.txt");
        // CreateEvaluationDb.parseEvaluationFile("evaluationTweets.txt","Tweets_evaluation");

        //  Statistics.computeStatistics();

        TrainingPrep p= TrainingPrep.getInstance();
        p.trainingpreparation(true,1,0.9,0.4);
//        p.trainingpreparation(false,2,0.9,0.4);
////        p.trainingpreparation(false,0.99);
      WekaNaiveBayes nb= WekaNaiveBayes.getInstance();
        nb.createandSaveNaiveBayesModel();
//        //    nb.loadNaiveBayesModel();
//        nb.naiveBayesClassifyParallel("arffTestFile.arff");
////

        ModelEvaluation.evaluateModelWeka("arffTrainingFile.arff","arffEvaluationFile.arff",nb.loadNaiveBayesModel(),"EvaluationResults.txt");
        ModelEvaluation.CreatePrecissionRecallStatistics("arffEvaluationFile.arff",nb.loadNaiveBayesModel(),"EvaluationPrecRecall.txt");
        NaiveBayesResultsVisualiser v =new NaiveBayesResultsVisualiser();
        v.createClassifiedTweetsFiles("resultsParallel.txt","testTweetIndexToText.txt");
    }
}


