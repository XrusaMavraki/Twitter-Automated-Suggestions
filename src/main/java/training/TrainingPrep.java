package training;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import cleanAndModel.CleanTweets;
import cleanAndModel.ModelData;
import mongo.MongoDbConnect;
import org.bson.Document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by xrusa on 29/11/2017.
 */
public class TrainingPrep {
    private Map<String,Set<Integer>> categoryToIndexes;
    private Map<Integer,String> tweetIndexToCategoryTraining;
    private Map<Integer,String> tweetIndexToCategoryTest;
    private Map<Integer,String> tweetIndexToCategoryEvaluation;
    private Map<String,Integer> tweetCategoryToCount;
    private CleanTweets c;
    private ModelData d;
    private Iterable<Document> docs;
    private  Map<Integer, Map<Integer,Double>> tweetIndexToModelIndexToValues;
    private static final String[] categories= {"airport","culture","fun-nature-sport","coffee-nighttime-food","shopping-beauty","irrelevant"};
    private static TrainingPrep instance=null;
    private TrainingPrep(){
        categoryToIndexes=new HashMap<>();
        tweetIndexToCategoryTraining = new HashMap<>();
        tweetIndexToCategoryTest = new HashMap<>();
        tweetIndexToCategoryEvaluation= new HashMap<>();
        tweetCategoryToCount=new HashMap<>();
        c= CleanTweets.getInstance();
        d= ModelData.getInstance();
        docs=c.getDocumentsWords("clean_tweets");
        tweetIndexToModelIndexToValues = d.createModelIndexToValues(docs);
    }

    public static TrainingPrep getInstance() {
        if(instance == null) {
            instance = new TrainingPrep();
        }
        return instance;

    }

    public static  void main(String args[]){
        TrainingPrep t = getInstance();

    }


    public void trainingpreparation(boolean retraining,int retrainingType,double threshold, double lowThresholdToRetrain) throws IOException {

        initialiseTweetCategoryToCounter();
        Iterable<Document> valDocs= c.cleanTweetDocuments("Tweets_evaluation");
        Map<Integer,String> valTweetIndexToText=d.getIndexToText(valDocs);
        printIndexToText(valTweetIndexToText,"valTweetIndexToText.txt");
        Map<Integer,Map<Integer,Double>> tweetIndexToModelIndexToValuesEvaluation= d.createModelIndexToValues(valDocs);
        System.out.println("evaluation size: "+tweetIndexToModelIndexToValuesEvaluation.size());
        System.out.println("TweetToIndexToValue size= "+tweetIndexToModelIndexToValues.size());
        createCategoryToIndexes();
//
       // createEvaluationCategories("IndexToCategoryValuation.txt");
        createEvaluationCategories("Tweets_evaluation");
        if (retraining){
            createTrainingCategoriesRetraining(retrainingType,threshold,lowThresholdToRetrain);
        }
        createTrainingCategories();
        System.out.println("TweetIndexToCategoryTest size= "+tweetIndexToCategoryTest.size());
        System.out.println("TweetIndexToCategoryTraining size= "+tweetIndexToCategoryTraining.size());
        printCategoryTrainingSizes();
        ArffCreator arffEvaluationCreator= new ArffCreator(categories,d.getUniqueWordsToIndex(),
                tweetIndexToModelIndexToValuesEvaluation,tweetIndexToCategoryEvaluation);
        arffEvaluationCreator.createArrfFile("arffEvaluationFile.arff","TweetsEvaluationDatabase");
        ArffCreator arffTrainingCreator= new ArffCreator(categories, d.getUniqueWordsToIndex(),
                tweetIndexToModelIndexToValues, tweetIndexToCategoryTraining);
        arffTrainingCreator.createArrfFile("arffTrainingFile.arff","TweetsTrainingDatabase");
        ArffCreator arffTestCreator= new ArffCreator(categories,d.getUniqueWordsToIndex(),
                tweetIndexToModelIndexToValues,tweetIndexToCategoryTest);
        arffTestCreator.createArrfFile("arffTestFile.arff","TweetsTestDatabase");
        System.out.println("modelData tweetIndexToText size= "+ d.getTweetIndexToText().size());
        printIndexToText(d.getTweetIndexToText());
    }



    private void printCategoryTrainingSizes() {
        for(int i=0;i<categories.length;i++){
            System.out.println(  categories[i]+" training size: "+ tweetCategoryToCount.get(categories[i]));
        }
    }

    private void initialiseTweetCategoryToCounter() {
        for(int i=0;i<categories.length;i++){
            tweetCategoryToCount.put(categories[i],0);
        }
    }


    private void createCategoryToIndexes(){
        for(String category:categories){
            Set<Integer> indexes=readTaggedWords(category);
            categoryToIndexes.put(category,indexes);
        }
    }

    private Set<Integer> readTaggedWords(String filename){
        Set<Integer> taggedWords= new HashSet<>();
        try (Stream<String> stream = Files.lines(Paths.get(filename+".txt"))) {
            stream.map(this::handleLine).forEach(taggedWords::addAll);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return taggedWords;
    }
    public List<Integer> handleLine(String line){
        String[] lines= line.split(",");
        List<Integer>indexes= new ArrayList<>();
        for(String l:lines){
            l= l.replaceAll("\\D+","");
            if(l==null||!(l.equals(""))) indexes.add( Integer.parseInt(l));
        }
        return indexes;

    }
    public void createTrainingCategories( ){
        for(Map.Entry<Integer, Map<Integer,Double>> entry : tweetIndexToModelIndexToValues.entrySet()){
            List<String> foundCategories= new ArrayList<>();
            for (Map.Entry<String,Set<Integer>> category: categoryToIndexes.entrySet()){

                //true when the taggedWordIndexes set and the tweetIndexes have at least one common element.
                if(!Collections.disjoint(category.getValue(), entry.getValue().keySet())) {
                    foundCategories.add(category.getKey());
                }
            }
            if (foundCategories.size() == 1 && !tweetIndexToCategoryTraining.containsKey(entry.getKey())) {
                tweetIndexToCategoryTraining.put(entry.getKey(), foundCategories.get(0));
                tweetCategoryToCount.computeIfPresent(foundCategories.get(0),(k,v)->v+1);
            } else if(!tweetIndexToCategoryTraining.containsKey(entry.getKey())){
                tweetIndexToCategoryTest.put(entry.getKey(),"?");
            }
        }
    }


    /**
     * Checks the result files that were created by the NaiveBayesVisualiser after the classification
     * @param  retrainType If the retrainType is 1: retraining will take place for the classified instances with
     * probability above the threshold. If the retrainType is 2: retraining will tkae place for the classified
     * instances with probability above the threshold as well as deem irrelevant all classified instances
     * with probability below lowThresholdToRetrain
     * @param threshold probability above which data will be considered for retraining
     * @param lowThresholdToRetrain probability below which data will be categorised as irrelevant.
     */
    private void createTrainingCategoriesRetraining(int retrainType,double threshold,double lowThresholdToRetrain){
        for(String category: categories) {
            try (Stream<String> stream = Files.lines(Paths.get(category + "Results"))) {
                stream.map(k->handleResultsLine(k,threshold)).filter(index->index!=-1).forEach(k->{
                    tweetIndexToCategoryTraining.put(k,category);
                    tweetCategoryToCount.computeIfPresent(category,(z,v)->v+1);

                });



            } catch (IOException e) {
                e.printStackTrace();
            }
            if(retrainType==2){
                try (Stream<String> stream = Files.lines(Paths.get(category + "Results"))) {
                    stream.map(k->handleResultsLineRetraining2(k,lowThresholdToRetrain)).filter(index->index!=-1).forEach(k->{
                        tweetIndexToCategoryTraining.put(k,categories[5]);
                        tweetCategoryToCount.computeIfPresent(categories[5],(z,v)->v+1);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private int  handleResultsLineRetraining2(String k, double lowThresholdToRetrain) {
        String[] split = k.split(":");
        if(Double.parseDouble(split[1].trim())<=lowThresholdToRetrain){
            String[] tweetSplit = split[4].trim().split(" ");
            return Integer.parseInt(tweetSplit[0].trim());
        }
        return -1;
    }


    private int handleResultsLine(String s,double threshold) {
        String[] split = s.split(":");

        if(Double.parseDouble(split[1].trim())>threshold){
            String[] tweetSplit = split[4].trim().split(" ");
            return Integer.parseInt(tweetSplit[0].trim());

        }
        return -1;
    }

    //    public void createEvaluationCategories(String evaluationIndexToCategoryFileName) throws IOException {
//        FileInputStream evaluationStream = new FileInputStream(evaluationIndexToCategoryFileName);
//        BufferedReader reader= new BufferedReader(new InputStreamReader(evaluationStream));
//        String line= reader.readLine();
//        while(line!=null){
//            String[] indexAndCategory= line.split(" ");
//            tweetIndexToCategoryEvaluation.put(Integer.parseInt(indexAndCategory[1]),categories[Integer.parseInt(indexAndCategory[2])]);
//            line=reader.readLine();
//        }
//    }
    public void createEvaluationCategories(String evaluationDbName)throws IOException{
        MongoCollection<Document> collection= MongoDbConnect.getCollection(evaluationDbName);
        FindIterable<Document> docs=collection.find();
        int counter=0;
        for (Document doc:docs) {
            doc.getString("text");
            if (counter < 184) {
                tweetIndexToCategoryEvaluation.put(counter, categories[4]);
                counter++;
            } else if (counter < (184 * 2)) {
                tweetIndexToCategoryEvaluation.put(counter, categories[0]);
                counter++;
            } else if (counter < (184 * 3)) {
                tweetIndexToCategoryEvaluation.put(counter, categories[1]);
                counter++;
            } else if (counter < (184 * 4)) {
                tweetIndexToCategoryEvaluation.put(counter, categories[2]);
                counter++;
            } else if (counter < ((184 * 5))) {
                tweetIndexToCategoryEvaluation.put(counter, categories[3]);
                counter++;
            } else if (counter < (184 * 6)) {
                tweetIndexToCategoryEvaluation.put(counter, categories[5]);
                counter++;
            }
        }
    }
    public void printIndexToText(Map<Integer,String> tweetIndexToText) throws FileNotFoundException, UnsupportedEncodingException {

        printIndexToText(tweetIndexToText,"tweetIndexToText.txt");
        PrintWriter writer = new PrintWriter("testTweetIndexToText.txt", "UTF-8");
        for(Map.Entry<Integer,String> tweet: tweetIndexToText.entrySet()){
            if (tweetIndexToCategoryTest.containsKey(tweet.getKey()))
                writer.println("Tweet Index: "+tweet.getKey()+ " Tweet Text "+ tweet.getValue());
        }
        writer.flush();
    }
    public void printIndexToText(Map<Integer,String> tweetIndexToText,String filename) throws FileNotFoundException, UnsupportedEncodingException{
        PrintWriter writer = new PrintWriter(filename, "UTF-8");
        for(Map.Entry<Integer,String> tweet: tweetIndexToText.entrySet()){
            writer.println("Tweet Index: "+tweet.getKey()+ " Tweet Text "+ tweet.getValue());
        }
        writer.flush();
    }

    public static String[] getCategories() {
        return categories;
    }

    public Map<String, Set<Integer>> getCategoryToIndexes() {
        return categoryToIndexes;
    }

    public Map<Integer, String> getTweetIndexToCategoryTraining() {
        return tweetIndexToCategoryTraining;
    }
}
