package training;

/**
 * Created by xrusa on 30/11/2017.
 */

import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import java.io.*;
import java.util.*;


public class WekaNaiveBayes {
    private static final WekaNaiveBayes instance= new WekaNaiveBayes();
    public static WekaNaiveBayes getInstance(){
        return instance;
    }

    public void naiveBayesClassifyParallel(String fileName) {
        ParallelWekaClassification parallelWekaClassification = new ParallelWekaClassification();
        parallelWekaClassification.classify(this::loadNaiveBayesModel, fileName, "resultsParallel.txt");
    }

    public List<List<CategoryHelper>> naiveBayesClassify(String arffFileName) {
        List<List<CategoryHelper>> classifiedCategories= new ArrayList<>();
        ArffLoader testLoader = new ArffLoader();
        NaiveBayesUpdateable nb;
        try {
            nb = loadNaiveBayesModel();

            System.out.println("Starting classification");
            testLoader.setFile(new File(arffFileName));
            Instances test = testLoader.getDataSet();
            test.setClassIndex(test.numAttributes() - 1);
            Instances testlabeled = new Instances(test);
            for (int i = 0; i < test.numInstances(); i++) {
                double[] nbLabel = nb.distributionForInstance(test.instance(i));
                double[] nblabelcopy= Arrays.copyOf(nbLabel,nbLabel.length);
                Arrays.sort(nbLabel);
               List<CategoryHelper> valuesAndCategoryIndexes=new ArrayList<>();
                for(int j=nbLabel.length-1;j>=0;j--){
                    for(int z=0; z<nblabelcopy.length;z++){
                        if(nblabelcopy[z]==nbLabel[j]){
                            CategoryHelper helper= new CategoryHelper(nblabelcopy[z],z);
                           valuesAndCategoryIndexes.add(helper);
                            break;
                        }
                    }

                }
                classifiedCategories.add(valuesAndCategoryIndexes);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return classifiedCategories;
    }
    public void naiveBayesClassify(String arffFileName,String resultFileName){
        ArffLoader testLoader = new ArffLoader();
        NaiveBayesUpdateable nb;
        try {
            nb = loadNaiveBayesModel();

            System.out.println("Starting classification");
            testLoader.setFile(new File(arffFileName));
            Instances test = testLoader.getDataSet();
            test.setClassIndex(test.numAttributes() - 1);
            Instances testlabeled = new Instances(test);

            PrintWriter writer = new PrintWriter(resultFileName, "UTF-8");
            for (int i = 0; i < test.numInstances(); i++) {
                if (i % 1000 == 0) {
                    System.out.println(i + " " + 100* (double) i / test.numInstances() + "%" );
                }
                double[] nbLabel = nb.distributionForInstance(test.instance(i));
                writer.println(testlabeled.instance(i).toString() +" "+ Arrays.toString(nbLabel));
            }
            System.out.println("Finished classification");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void createandSaveNaiveBayesModel() throws Exception {
        ArffLoader trainLoader = new ArffLoader();
        trainLoader.setFile(new File("arffTrainingFile.arff"));
        Instances train = trainLoader.getDataSet();
        train.setClassIndex(train.numAttributes() - 1);

        // train NaiveBayes
        NaiveBayesUpdateable nb = new NaiveBayesUpdateable();
        // Evaluation eval = new Evaluation(train);
        //eval.crossValidateModel(nb, train, 10, new Random(1));
        nb.buildClassifier(train);
        System.out.println("Finished training");
        ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("naiveBayes.model"));
        oos.writeObject(nb);

    }

    public NaiveBayesUpdateable loadNaiveBayesModel() {
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream("naiveBayes.model"));
            NaiveBayesUpdateable nb = (NaiveBayesUpdateable) ois.readObject();
            ois.close();

            return nb;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class CategoryHelper{

        private double probabilityValueResult;
        private int categoryIndex;

        public CategoryHelper(double probabilityValueResult,int categoryIndex){
            this.probabilityValueResult=probabilityValueResult;
            this.categoryIndex=categoryIndex;
        }

        public double getProbabilityValueResult() {
            return probabilityValueResult;
        }

        public int getCategoryIndex() {
            return categoryIndex;
        }
    }

}


