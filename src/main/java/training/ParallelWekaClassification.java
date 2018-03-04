package training;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by xrusa on 16/12/2017.
 */
public class ParallelWekaClassification {

    private static final int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(NUM_PROCESSORS);
    private static final String PREFIX_ARRF_PARTIAL = "arrf-partial_";
    private static final String PREFIX_RESULTS_PARTIAL = "results-partial_";

    public void classify(Supplier<Classifier> classifierSupplier, String testArrfFile, String resultsFileName) {
        List<String> fileNames = splitArrfToPartialArrfs(testArrfFile);
        try {
            List<String> partialFiles = forkJoinPool.submit( () ->
                    fileNames.stream()
                            .parallel()
                            .map(partialPath -> classifyPartialArrf(classifierSupplier, partialPath))
                            .collect(Collectors.toList())
            ).get();

            Files.deleteIfExists(Paths.get(resultsFileName));
            partialFiles.stream()
                    .sorted()
                    .forEach(partial -> combineToSingleResultsFile(partial, resultsFileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> splitArrfToPartialArrfs(String fullTestArrfFile) {
        // read full Arrf File and keep the headers which will be copied to every partial file
        StringBuilder header = new StringBuilder();
        long dataLines = 0;
        try (BufferedReader br = Files.newBufferedReader(Paths.get(fullTestArrfFile))) {
            boolean readingHeader = true;
            String line;
            while ((line = br.readLine()) != null) {
                if (readingHeader) {
                    header.append(line).append(System.lineSeparator());
                    if (line.toUpperCase().startsWith("@DATA")) {
                        readingHeader = false;
                    }
                } else {
                    if (!line.trim().isEmpty()) {
                        dataLines++;
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // write the partial arrf files
        // the capacity of each file is the result of the division of the data lines and the number of files (processors)
        // the last file may contain 1 less line
        int partialFilesCapacity = (int) Math.ceil(dataLines / (double) NUM_PROCESSORS);
        BufferedWriter[] bufferedWriters = new BufferedWriter[NUM_PROCESSORS];
        try (BufferedReader br = Files.newBufferedReader(Paths.get(fullTestArrfFile))) {
            for (int i = 0; i < bufferedWriters.length; i++) {
                bufferedWriters[i] = new BufferedWriter(new FileWriter(PREFIX_ARRF_PARTIAL + i));
                bufferedWriters[i].write(header.toString());
            }
            long curDataLine = 0;
            boolean readingHeader = true;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toUpperCase().startsWith("@DATA")) {
                    readingHeader = false;
                    continue;
                }
                if (readingHeader) {
                    continue;
                }
                int partialFileIndex = (int) (curDataLine / partialFilesCapacity);
                bufferedWriters[partialFileIndex].write(line);
                bufferedWriters[partialFileIndex].write(System.lineSeparator());
                curDataLine++;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            for (BufferedWriter bufferedWriter : bufferedWriters) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Each partial file should end with _i, e.g. partial_0, partial_1, partial_2, to denote the order.
        return IntStream.range(0, NUM_PROCESSORS)
                .mapToObj(i -> PREFIX_ARRF_PARTIAL + i)
                .collect(Collectors.toList());
    }

    private String classifyPartialArrf(Supplier<Classifier> classifierSupplier, String partialArrfPath) {
        String[] partialSplit = partialArrfPath.split("_");
        int partialIndex = Integer.parseInt(partialSplit[partialSplit.length - 1]);
        String partialResultsFileName = PREFIX_RESULTS_PARTIAL + partialIndex;
        Classifier classifier = classifierSupplier.get();
        ArffLoader testLoader = new ArffLoader();
        try {
            testLoader.setFile(new File(partialArrfPath));
            Instances test = testLoader.getDataSet();
            test.setClassIndex(test.numAttributes() - 1);
            PrintWriter writer = new PrintWriter(partialResultsFileName, "UTF-8");
            Instant lastElapsed = Instant.now();
            for (int i = 0; i < test.numInstances(); i++) {
                if (i % 1000 == 0) {
                    System.out.println(LocalDateTime.now() + " - Partial " + partialIndex + ": " + i + " " + 100 * (double) i / test.numInstances() + "% Elapsed since last report in seconds: " + lastElapsed.until(Instant.now(), ChronoUnit.SECONDS));
                    lastElapsed = Instant.now();
                }
                double[] nbLabel = classifier.distributionForInstance(test.instance(i));
                writer.println(test.instance(i).toString() + " " + Arrays.toString(nbLabel));
            }
            System.out.println("Finished classification");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return partialResultsFileName;
    }

    private void combineToSingleResultsFile(String partialResults, String targetFile) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(partialResults)));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile, true)))) {
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                bw.write(inputLine);
                bw.newLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
