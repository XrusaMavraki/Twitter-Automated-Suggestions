package main;

import inputOutput.DailyExecutor;
import inputOutput.IncomingTweetAnalysis;
import inputOutput.IncomingTweetListener;
import training.WekaNaiveBayes;
import weka.classifiers.Classifier;

/**
 * Created by xrusa on 25/2/2018.
 */
public class FirstLaunchMain {

    public static void main(String[] args) throws Exception {

        DailyExecutor dailyExecutor= new DailyExecutor();
        dailyExecutor.startExecutor();
        Classifier nb=WekaNaiveBayes.getInstance().loadNaiveBayesModel();
        IncomingTweetAnalysis analysis= new IncomingTweetAnalysis(nb,0.8);
        IncomingTweetListener listener= new IncomingTweetListener(analysis);
    }

}
