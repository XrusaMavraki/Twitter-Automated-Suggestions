import inputOutput.IncomingTweetAnalysis;
import org.junit.Assert;
import org.junit.Test;
import training.WekaNaiveBayes;
import twitter4j.*;
import weka.classifiers.Classifier;

/**
 * Created by xrusa on 6/2/2018.
 */
public class NewIncomingTweetTests {

//    @Mock
//    private Status status;

//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//    }

    @Test
    public void testNewIncomingTweet() {
//        when(status.getText()).thenReturn("Parthenon is amazing! Ancient Greece");
//        when(status.getLang()).thenReturn("en");
//
//        Classifier classifier = WekaNaiveBayes.getInstance().loadNaiveBayesModel();
//        IncomingTweetAnalysis incomingTweetAnalysis = new IncomingTweetAnalysis(classifier, .75f);
//        incomingTweetAnalysis.handleIncomingTweet(status);
    }

    @Test
    public void testGetTweetStatusFromId() throws TwitterException {
        Twitter twitter = new TwitterFactory().getInstance();
        Status status = twitter.showStatus(967082876058701830L);
        Classifier classifier = WekaNaiveBayes.getInstance().loadNaiveBayesModel();
        IncomingTweetAnalysis incomingTweetAnalysis = new IncomingTweetAnalysis(classifier, .75f);
       // incomingTweetAnalysis.handleIncomingTweet(status,status.);
       // Assert.assertTrue(status.getText().contains("δελτίο ειδήσεων του"));
    }

    @Test
    public void testToJson() throws TwitterException {
        Twitter twitter = new TwitterFactory().getInstance();
        Status status = twitter.showStatus(967082876058701830L);
        String json = TwitterObjectFactory.getRawJSON(status);
        Assert.assertNotNull(json);
    }
}
