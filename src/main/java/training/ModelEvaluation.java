package training;

import org.apache.commons.lang3.ArrayUtils;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import java.io.File;
import java.io.PrintWriter;
import java.util.stream.IntStream;

/**
 * Created by xrusa on 12/1/2018.
 */
public class ModelEvaluation {

    private static final String[] categories= TrainingPrep.getCategories();

    

    public static void evaluateModelWeka (String arffTraining, String arffEvaluation,Classifier classifier,String resultsFileName) throws Exception {
        ArffLoader trainLoader = new ArffLoader();
        trainLoader.setFile(new File(arffTraining));
        Instances train = trainLoader.getDataSet();
        train.setClassIndex(train.numAttributes() - 1);
        Instances trainlabeled = new Instances(train);
        ArffLoader testLoader= new ArffLoader();
        testLoader.setFile(new File(arffEvaluation));
        Instances test= testLoader.getDataSet();
        test.setClassIndex(test.numAttributes() - 1);
        Instances testlabeled = new Instances(test);


        Evaluation eval= new Evaluation(trainlabeled);
        eval.evaluateModel(classifier,testlabeled);
        PrintWriter writer= new PrintWriter(resultsFileName, "UTF-8");
        writer.write(eval.toSummaryString("\nResults\n======\n", false));
        writer.write("\n" +eval.toMatrixString("MatrixString results"));
        writer.flush();
    }

    public static void CreatePrecissionRecallStatistics(String arffFileName,Classifier classifier,String resultsFileName) throws Exception{

        PrintWriter writer= new PrintWriter(resultsFileName, "UTF-8");
        ArffLoader testLoader= new ArffLoader();
        testLoader.setFile(new File(arffFileName));
        Instances test= testLoader.getDataSet();
        test.setClassIndex(test.numAttributes() - 1);
        Instances testlabeled = new Instances(test);

        int[] categoryArffCounter= new int[6];
        int[] correctlyClassified= new int[6];
        int[] correctlyClassifiedOver90= new int[6];
        int[] correctlyClassifiedOver80= new int[6];
        int[] correctlyClassifiedOver70= new int[6];
        int[] inCorrectlyClassified= new int[6];
        int[] inCorrectlyClassifiedOver90= new int[6];
        int[] inCorrectlyClassifiedOver80= new int[6];
        int[] inCorrectlyClassifiedOver70= new int[6];
        for(Instance instance: testlabeled){
            double[] distrib=classifier.distributionForInstance(instance);
            double maxProb=0;
            int maxIndex=-1;
            for(int i=0; i<distrib.length;i++){
                if (distrib[i]>maxProb) {
                    maxProb=distrib[i];
                    maxIndex=i;
                }
            }
            categoryArffCounter[(int)instance.classValue()]++;
            if(categories[maxIndex].equals(categories[(int)instance.classValue()])){
                correctlyClassified[maxIndex]++;
                if(maxProb>0.9) correctlyClassifiedOver90[maxIndex]++;
                if(maxProb>0.8) correctlyClassifiedOver80[maxIndex]++;
                if(maxProb>0.7) correctlyClassifiedOver70[maxIndex]++;

            }else{
                inCorrectlyClassified[maxIndex]++;
                if(maxProb>0.9) inCorrectlyClassifiedOver90[maxIndex]++;
                if(maxProb>0.8) inCorrectlyClassifiedOver80[maxIndex]++;
                if(maxProb>0.7) inCorrectlyClassifiedOver70[maxIndex]++;
            }
        }

        printHelper(writer,categoryArffCounter,correctlyClassified,inCorrectlyClassified,0);
        printHelper(writer,categoryArffCounter,correctlyClassifiedOver90,inCorrectlyClassifiedOver90,90);
        printHelper(writer,categoryArffCounter,correctlyClassifiedOver80,inCorrectlyClassifiedOver80,80);
        printHelper(writer,categoryArffCounter,correctlyClassifiedOver70,inCorrectlyClassifiedOver70,70);
        writer.flush();
//        int totalArffInstances= IntStream.of(categoryArffCounter).sum();
//        int sumCorrectlyClassifiedInstances= IntStream.of(correctlyClassified).sum();
//        int sumInccorectlyClassifiedInstances= IntStream.of(inCorrectlyClassified).sum();
//        writer.write("Total correctly classified instances : " +sumCorrectlyClassifiedInstances+"\n");
//        writer.write("Out of total + "+totalArffInstances +"\n");
//        writer.write("Overall Precision: "+ (sumCorrectlyClassifiedInstances/((double)sumInccorectlyClassifiedInstances+sumCorrectlyClassifiedInstances))+"\n");
//        writer.write("Overall Recall: "+sumCorrectlyClassifiedInstances/(double)totalArffInstances +"\n");
//        writer.write("-----------------------------------------------------"+"\n"+"\n");
//        writer.write("Total correctly classified instances Airport : "+correctlyClassified[0]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[0]+"\n");
//        writer.write("Airport precision : "+correctlyClassified[0]/((double)inCorrectlyClassified[0]+correctlyClassified[0])+"\n");
//        writer.write("Airport recall : "+correctlyClassified[0]/(double)categoryArffCounter[0]+"\n");
//        writer.write("Total correctly classified instances Culture : "+correctlyClassified[1]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[1]+"\n");
//        writer.write("Culture precision : "+correctlyClassified[1]/((double)inCorrectlyClassified[1]+correctlyClassified[1])+"\n");
//        writer.write("Culture recall : "+correctlyClassified[1]/(double)categoryArffCounter[1]+"\n");
//        writer.write("Total correctly classified instances Fun-Nature-Sport : "+correctlyClassified[2]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[2]+"\n");
//        writer.write("Fun-Nature-Sport precision : "+correctlyClassified[2]/((double)inCorrectlyClassified[2]+correctlyClassified[2])+"\n");
//        writer.write("Fun-Nature-Sport recall : "+correctlyClassified[2]/(double)categoryArffCounter[2]+"\n");
//        writer.write("Total correctly classified instances Coffee-Nighttime-Food : "+correctlyClassified[3]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[3]+"\n");
//        writer.write("Coffee-Nighttime-Food precision : "+correctlyClassified[3]/((double)inCorrectlyClassified[3]+correctlyClassified[3])+"\n");
//        writer.write("Coffee-Nighttime-Food recall : "+correctlyClassified[3]/(double)categoryArffCounter[3]+"\n");
//        writer.write("Total correctly classified instances Shopping-Beauty : "+correctlyClassified[4]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[4]+"\n");
//        writer.write("Shopping-Beauty precision : "+correctlyClassified[4]/((double)inCorrectlyClassified[4]+correctlyClassified[4])+"\n");
//        writer.write("Shopping-Beauty recall : "+correctlyClassified[4]/(double)categoryArffCounter[4]+"\n");
//        writer.write("Total correctly classified instances Irrelevant : "+correctlyClassified[5]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[5]+"\n");
//        writer.write("Irrelevant precision : "+correctlyClassified[5]/((double)inCorrectlyClassified[5]+correctlyClassified[5])+"\n");
//        writer.write("Irrelevant recall : "+correctlyClassified[5]/(double)categoryArffCounter[5]+"\n");
//        writer.write("-----------------------------------------------------"+"\n"+"\n");
//
//
//        sumCorrectlyClassifiedInstances= IntStream.of(correctlyClassifiedOver90).sum();
//        sumInccorectlyClassifiedInstances=IntStream.of(inCorrectlyClassifiedOver90).sum();
//        writer.write("When required certainty is ove 90%: "+"\n");
//        writer.write("Total correctly classified instances : " +sumCorrectlyClassifiedInstances+"\n");
//        writer.write("Overall Precision: "+ (sumCorrectlyClassifiedInstances/((double)sumInccorectlyClassifiedInstances+sumCorrectlyClassifiedInstances)+"\n"));
//        writer.write("-----------------------------------------------------"+"\n");
//        writer.write("Total correctly classified instances Airport : "+correctlyClassifiedOver90[0]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[0]+"\n");
//        writer.write("Airport precision : "+correctlyClassifiedOver90[0]/((double)inCorrectlyClassifiedOver90[0]+correctlyClassifiedOver90[0])+"\n");
//        writer.write("Airport recall : "+correctlyClassifiedOver90[0]/(double)categoryArffCounter[0]+"\n");
//        writer.write("Total correctly classified instances Culture : "+correctlyClassifiedOver90[1]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[1]+"\n");
//        writer.write("Culture precision : "+correctlyClassifiedOver90[1]/((double)inCorrectlyClassifiedOver90[1]+correctlyClassifiedOver90[1])+"\n");
//        writer.write("Culture recall : "+correctlyClassifiedOver90[1]/(double)categoryArffCounter[1]+"\n");
//        writer.write("Total correctly classified instances Fun-Nature-Sport : "+correctlyClassifiedOver90[2]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[2]+"\n");
//        writer.write("Fun-Nature-Sport precision : "+correctlyClassifiedOver90[2]/((double)inCorrectlyClassifiedOver90[2]+correctlyClassifiedOver90[2])+"\n");
//        writer.write("Fun-Nature-Sport recall : "+correctlyClassifiedOver90[2]/(double)categoryArffCounter[2]+"\n");
//        writer.write("Total correctly classified instances Coffee-Nighttime-Food : "+correctlyClassifiedOver90[3]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[3]+"\n");
//        writer.write("Coffee-Nighttime-Food precision : "+correctlyClassifiedOver90[3]/((double)inCorrectlyClassifiedOver90[3]+correctlyClassifiedOver90[3])+"\n");
//        writer.write("Coffee-Nighttime-Food recall : "+correctlyClassifiedOver90[3]/(double)categoryArffCounter[3]+"\n");
//        writer.write("Total correctly classified instances Shopping-Beauty : "+correctlyClassifiedOver90[4]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[4]+"\n");
//        writer.write("Shopping-Beauty precision : "+correctlyClassifiedOver90[4]/((double)inCorrectlyClassifiedOver90[4]+correctlyClassifiedOver90[4])+"\n");
//        writer.write("Shopping-Beauty recall : "+correctlyClassifiedOver90[4]/(double)categoryArffCounter[4]+"\n");
//        writer.write("Total correctly classified instances Irrelevant : "+correctlyClassifiedOver90[5]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[5]);
//        writer.write("Irrelevant precision : "+correctlyClassifiedOver90[5]/((double)inCorrectlyClassifiedOver90[5]+correctlyClassifiedOver90[5])+"\n");
//        writer.write("Irrelevant recall : "+correctlyClassified[5]/(double)categoryArffCounter[5]+"\n");
//        writer.write("-----------------------------------------------------"+"\n"+"\n");
//
//        sumCorrectlyClassifiedInstances= IntStream.of(correctlyClassifiedOver90).sum();
//        sumInccorectlyClassifiedInstances=IntStream.of(inCorrectlyClassifiedOver80).sum();
//        writer.write("When required certainty is over 80%" +"\n");
//        writer.write("Total correctly classified instances : " +sumCorrectlyClassifiedInstances+"\n");
//        writer.write("Overall Precision: "+ (sumCorrectlyClassifiedInstances/((double)sumInccorectlyClassifiedInstances)+sumCorrectlyClassifiedInstances)+"\n");
//        writer.write("-----------------------------------------------------"+"\n");
//        writer.write("Total correctly classified instances Airport : "+correctlyClassifiedOver80[0]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[0]+"\n");
//        writer.write("Airport precision : "+correctlyClassifiedOver80[0]/((double)inCorrectlyClassifiedOver80[0]+correctlyClassifiedOver80[0])+"\n");
//        writer.write("Airport recall : "+correctlyClassifiedOver80[0]/(double)categoryArffCounter[0]+"\n");
//        writer.write("Total correctly classified instances Culture : "+correctlyClassifiedOver80[1]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[1]+"\n");
//        writer.write("Culture precision : "+correctlyClassifiedOver80[1]/((double)inCorrectlyClassifiedOver80[1]+correctlyClassifiedOver80[1])+"\n");
//        writer.write("Culture recall : "+correctlyClassifiedOver80[1]/(double)categoryArffCounter[1]+"\n");
//        writer.write("Total correctly classified instances Fun-Nature-Sport : "+correctlyClassifiedOver80[2]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[2]+"\n");
//        writer.write("Fun-Nature-Sport precision : "+correctlyClassifiedOver80[2]/((double)inCorrectlyClassifiedOver80[2]+correctlyClassifiedOver80[2])+"\n");
//        writer.write("Fun-Nature-Sport recall : "+correctlyClassifiedOver80[2]/(double)categoryArffCounter[2]+"\n");
//        writer.write("Total correctly classified instances Coffee-Nighttime-Food : "+correctlyClassifiedOver80[3]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[3]+"\n");
//        writer.write("Coffee-Nighttime-Food precision : "+correctlyClassifiedOver80[3]/((double)inCorrectlyClassifiedOver80[3]+correctlyClassifiedOver80[3])+"\n");
//        writer.write("Coffee-Nighttime-Food recall : "+correctlyClassifiedOver80[3]/(double)categoryArffCounter[3]+"\n");
//        writer.write("Total correctly classified instances Shopping-Beauty : "+correctlyClassifiedOver80[4]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[4]+"\n");
//        writer.write("Shopping-Beauty precision : "+correctlyClassifiedOver80[4]/((double)inCorrectlyClassifiedOver80[4]+correctlyClassifiedOver80[4])+"\n");
//        writer.write("Shopping-Beauty recall : "+correctlyClassifiedOver80[4]/(double)categoryArffCounter[4]+"\n");
//        writer.write("Total correctly classified instances Irrelevant : "+correctlyClassifiedOver80[5]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[5]+"\n");
//        writer.write("Irrelevant precision : "+correctlyClassifiedOver80[5]/((double)inCorrectlyClassifiedOver80[5]+correctlyClassifiedOver80[5])+"\n");
//        writer.write("Irrelevant recall : "+correctlyClassified[5]/(double)categoryArffCounter[5]+"\n");
//        writer.write("-----------------------------------------------------"+"\n");
//
//
//        sumCorrectlyClassifiedInstances= IntStream.of(correctlyClassifiedOver70).sum();
//        sumInccorectlyClassifiedInstances=IntStream.of(inCorrectlyClassifiedOver70).sum();
//        writer.write("When required certainty is over 70%" +"\n");
//        writer.write("Total correctly classified instances : " +sumCorrectlyClassifiedInstances+"\n");
//        writer.write("Overall Precision: "+ (sumCorrectlyClassifiedInstances/((double)sumInccorectlyClassifiedInstances)+sumCorrectlyClassifiedInstances)+"\n");
//        writer.write("-----------------------------------------------------"+"\n");
//        writer.write("Total correctly classified instances Airport : "+correctlyClassifiedOver70[0]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[0]+"\n");
//        writer.write("Airport precision : "+correctlyClassifiedOver70[0]/((double)inCorrectlyClassifiedOver70[0]+correctlyClassifiedOver70[0])+"\n");
//        writer.write("Airport recall : "+correctlyClassifiedOver70[0]/(double)categoryArffCounter[0]+"\n");
//        writer.write("Total correctly classified instances Culture : "+correctlyClassifiedOver70[1]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[1]+"\n");
//        writer.write("Culture precision : "+correctlyClassifiedOver70[1]/((double)inCorrectlyClassifiedOver70[1]+correctlyClassifiedOver70[1])+"\n");
//        writer.write("Culture recall : "+correctlyClassifiedOver70[1]/(double)categoryArffCounter[1]+"\n");
//        writer.write("Total correctly classified instances Fun-Nature-Sport : "+correctlyClassifiedOver70[2]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[2]+"\n");
//        writer.write("Fun-Nature-Sport precision : "+correctlyClassifiedOver70[2]/((double)inCorrectlyClassifiedOver70[2]+correctlyClassifiedOver70[2])+"\n");
//        writer.write("Fun-Nature-Sport recall : "+correctlyClassifiedOver70[2]/(double)categoryArffCounter[2]+"\n");
//        writer.write("Total correctly classified instances Coffee-Nighttime-Food : "+correctlyClassifiedOver70[3]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[3]+"\n");
//        writer.write("Coffee-Nighttime-Food precision : "+correctlyClassifiedOver70[3]/((double)inCorrectlyClassifiedOver70[3]+correctlyClassifiedOver70[3])+"\n");
//        writer.write("Coffee-Nighttime-Food recall : "+correctlyClassifiedOver70[3]/(double)categoryArffCounter[3]+"\n");
//        writer.write("Total correctly classified instances Shopping-Beauty : "+correctlyClassifiedOver70[4]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[4]+"\n");
//        writer.write("Shopping-Beauty precision : "+correctlyClassifiedOver70[4]/((double)inCorrectlyClassifiedOver70[4]+correctlyClassifiedOver70[4])+"\n");
//        writer.write("Shopping-Beauty recall : "+correctlyClassifiedOver70[4]/(double)categoryArffCounter[4]+"\n");
//        writer.write("Total correctly classified instances Irrelevant : "+correctlyClassifiedOver70[5]+"\n");
//        writer.write("Out of total + "+categoryArffCounter[5]+"\n");
//        writer.write("Irrelevant precision : "+correctlyClassifiedOver70[5]/((double)inCorrectlyClassifiedOver70[5]+correctlyClassifiedOver70[5])+"\n");
//        writer.write("Irrelevant recall : "+correctlyClassified[5]/(double)categoryArffCounter[5]+"\n");
//        writer.write("-----------------------------------------------------"+"\n");
//        writer.flush();

    }

    private static void printHelper(PrintWriter writer,int[]categoryArffCounter,int[] correctlyClassified,int[]inCorrectlyClassified,int percentageOver){
        int totalArffInstances= IntStream.of(categoryArffCounter).sum();
        int sumCorrectlyClassifiedInstances= IntStream.of(correctlyClassified).sum();
        int sumInccorectlyClassifiedInstances= IntStream.of(inCorrectlyClassified).sum();
        if(percentageOver!=0){
            writer.write("-----------------------------------------------------"+"\n"+"\n");
            writer.write("When required certainty is over "+ percentageOver+"%" +"\n");
        }
        writer.write("Total correctly classified instances : " +sumCorrectlyClassifiedInstances+"\n");
        writer.write("Out of total + "+totalArffInstances +"\n");
        writer.write("Overall Precision: "+ (sumCorrectlyClassifiedInstances/((double)sumInccorectlyClassifiedInstances+sumCorrectlyClassifiedInstances))+"\n");
        writer.write("Overall Recall: "+sumCorrectlyClassifiedInstances/(double)totalArffInstances +"\n");

        for(int i=0;i<correctlyClassified.length;i++){
            writer.write("-----------------------------------------------------"+"\n"+"\n");
            writer.write("Total correctly classified instances "+categories[i]+" : "+correctlyClassified[i]+"\n");
            writer.write("Out of total + "+categoryArffCounter[i]+"\n");
            writer.write(categories[i]+ " precision : "+correctlyClassified[i]/((double)inCorrectlyClassified[i]+correctlyClassified[i])+"\n");
            writer.write(categories[i]+" recall : "+correctlyClassified[i]/(double)categoryArffCounter[i]+"\n");
        }

    }



}




