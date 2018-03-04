package training;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * This class is responsible for creating ARRF files which are input to WEKA trainers.
 * An ARFF (Attribute-Relation File Format) file is an ASCII text file that describes a list of instances sharing a set of attributes.
 *
 * http://weka.wikispaces.com/ARFF+%28stable+version%29
 * Created by xrusa on 19/11/2017.
 */
public class ArffCreator {

    //private final int argumentsLength;
    private final String[] categories;
    private final Map<String, Integer> wordToIndex;
    private final Map<Integer, Map<Integer,Double>> tweetIndexToModelIndexToValues;
    private final Map<Integer,String> tweetIndexToCategory;

    public ArffCreator(String[] categories, Map<String, Integer> wordToIndex, Map<Integer, Map<Integer, Double>> tweetIndexToModelIndexToValues, Map<Integer, String> tweetIndexToCategory) {
        this.categories = categories;
        this.wordToIndex = wordToIndex;
        this.tweetIndexToModelIndexToValues = tweetIndexToModelIndexToValues;
        this.tweetIndexToCategory = tweetIndexToCategory;
    }



    //    public ArffCreator(String[] categories,Map<String, Integer> wordToIndex, Map<Integer,Map<Integer,Double>> tweetIndexToModelIndexToValues,Map<Integer,String> indexToCategory) {
//        // this.argumentsLength = argumentsLength;
//        this.categories = categories;
//        //this.wordToIndex = wordToIndex;
//        this.indexToValue= indexToValue;
//        this.indexToCategory=indexToCategory;
//    }

    public void createArrfFile(String filename,String title) throws IOException {
        PrintWriter writer = new PrintWriter(filename, "UTF-8");
        writer.println(" % 1. Title: "+title);
        writer.println("@RELATION tweets");
        int j=0;
        for ( Map.Entry<String,Integer> entry : wordToIndex.entrySet()){
           // writer.println("@ATTRIBUTE "+ entry.getKey()+ " NUMERIC");
            writer.println("@ATTRIBUTE attribute"+j +" NUMERIC");
            j++;
        }
        writer.print("@ATTRIBUTE class {");
        for (int z=0;z<categories.length;z++) {
            if(z==categories.length-1){
                writer.print(categories[z] );
            }else {
                writer.print(categories[z] + ",");
            }
        }
        writer.print("} \n" );
        writer.println("@DATA");
        for (Map.Entry<Integer, Map<Integer, Double>> entry : tweetIndexToModelIndexToValues.entrySet()) {
            if(tweetIndexToCategory.get(entry.getKey())==null) continue;
            Map<Integer, Double> currentMap = entry.getValue();
            writer.print("{");
            for (Map.Entry<Integer,Double> modelEntry: currentMap.entrySet()){
                writer.print(modelEntry.getKey()+" "+modelEntry.getValue()+",");
            }
            writer.print(wordToIndex.size()+" "+tweetIndexToCategory.get(entry.getKey())+"}"+"\n");
        }
//        for(Map<Integer,Double> tweetIndexToModelIndexToValues: tweetIndexToModelIndexToValues){
//
//        }

        writer.close();
    }
}
