package training;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by xrusa on 13/12/2017.
 */
public class NaiveBayesResultsVisualiser extends JFrame {

    public static void main(String[] args) {

        NaiveBayesResultsVisualiser v = new NaiveBayesResultsVisualiser();
        try {
            v.createClassifiedTweetsFiles("resultsParallel.txt","testTweetIndexToText.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private List<Pair> points = new ArrayList<>();

    public NaiveBayesResultsVisualiser() {
        super("Scatterplot");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try (Stream<String> stream = Files.lines(Paths.get("resultsParallel.txt"))) {
            AtomicInteger counter = new AtomicInteger(0);
            long[] counts = new long[6];
            stream.map(line -> handleLine(line, counter, counts)).forEach(points::add);
            for (int i = 0; i < counts.length; i++) {
                System.out.println(counts[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //adding points
//            for(int a = 0; a < c.myList.size(); a++)
//            {
//                points.add(new Point2D.Float(a, c.myList.get(a).getSteps()));
//            }
        //end adding points


        JPanel panel = new JPanel() {
            public void paintComponent(Graphics g) {
                Color[] colors= {Color.BLACK,Color.red, Color.green,Color.blue,Color.yellow,Color.cyan};
                for (Iterator<Pair> i = points.iterator(); i.hasNext(); ) {
                    Pair pair = i.next();
                    Point2D.Float pt = (Point2D.Float) pair.getPoint();
                    g.setColor(colors[pair.getCategory()]);
                    g.drawString("*", (int) (pt.x * (1000)) + 40,
                            (int) (-pt.y + getHeight()) - 40);
                }
                int width = getWidth();
                int height = getHeight();
                setVisible(true);
                //axises (axes?)
                g.drawLine(0, height - 40, width, height - 40);
                g.drawLine(40, height - 270, 40, height);

                //y-axis labels below
                for (int a = 1; a < 5; a++) {
                    String temp = 20 * a + "--";
                    g.drawString(temp, 20, height - (36 + 20 * (a)));
                }
                for (int a = 5; a < 11; a++) {
                    String temp = 20 * a + "--";
                    g.drawString(temp, 11, height - (36 + 20 * (a)));
                }
                //y-axis labels above

                //x-axis labels below
                for (int a = 1; a < 11; a++) {
                    g.drawString("|", 40 + 100 * a, height - 30);
                    int x = 10 * a;
                    String temp = x + " ";
                    g.drawString(temp, 30 + 100 * a, height - 18);
                }
                g.drawString("The Collatz Conjecture: Number vs. Stopping Time", 400, 60);
                //x-axis labels above

            }
        };

        setContentPane(panel);
        //last two numbers below change the initial size of the graph.
        setBounds(20, 20, 4000, 3000);
        setVisible(true);
    }

    private Pair handleLine(String line, AtomicInteger counter){
        String[] results = line.split("\\[");
        results[1] = results[1].replaceAll("]", "");
        String resultNums[] = results[1].split(" ");
        float max = 0;
        int maxCategory=-1;
        for (int i=0;i<resultNums.length;i++) {
            resultNums[i] = resultNums[i].replaceAll(",", "").trim();
            float n = Float.valueOf(resultNums[i]);
            if (max <= n) {
                max = n;
                maxCategory=i;
            }
        }
        Pair pair= new Pair(max, counter.getAndIncrement(),maxCategory);
        return pair;
    }
    private Pair handleLine(String line, AtomicInteger counter, long[] counts) {
        Pair pair= handleLine(line,counter);
        float max= (float)pair.getPoint().getX();
        int maxCategory= pair.getCategory();
        if (max >= .75f)
            counts[maxCategory]++;
        return pair;
    }
    public void createClassifiedTweetsFiles(String resultsFileName,String testIndexToTextFileName) throws IOException {
        FileInputStream resultsfile = new FileInputStream(resultsFileName);
        FileInputStream tweetsfile = new FileInputStream(testIndexToTextFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(resultsfile));
        BufferedReader br1= new BufferedReader(new InputStreamReader(tweetsfile));
        PrintWriter[] writers = {new PrintWriter("airportResults", "UTF-8"),
                new PrintWriter("cultureResults", "UTF-8"),
                new PrintWriter("fun-nature-sportResults", "UTF-8"),
                new PrintWriter("coffee-nighttime-foodResults", "UTF-8"),
                new PrintWriter("shopping-beautyResults", "UTF-8"),
                new PrintWriter("irrelevantResults", "UTF-8")
        };
        String resultsline,tweetLine;
        AtomicInteger integer= new AtomicInteger(0);
        while(((resultsline = br.readLine()) != null)&&(tweetLine = br1.readLine()) != null){

            Pair pair = handleLine(resultsline,integer);
            float probability=(float)pair.getPoint().getX();
            int category=pair.getCategory();
            writers[category].println(" Probability: "+probability+": Tweet: "+tweetLine);
        }
        for(PrintWriter writer: writers){
            writer.flush();
        }

    }
    private static class Pair {
        private Point2D point;
        private int category;

        private Pair(float value, float counter, int category) {
            this.point = new Point2D.Float(value, counter);
            this.category = category;
        }

        public Point2D getPoint() {
            return point;
        }

        public int getCategory() {
            return category;
        }
    }
}
