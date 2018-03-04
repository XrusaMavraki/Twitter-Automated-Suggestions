package inputOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import twitter4j.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xrusa on 22/2/2018.
 */
public class IncomingTweetListener {

    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<String> KEYWORDS_OPTIONAL = new HashSet<>();
    private static final Set<String>  KEYWORDS_NECESSARY = new HashSet<>();
    private static IncomingTweetAnalysis incomingTweetAnalysis;
    static {
        KEYWORDS.addAll(Arrays.asList("athens","greece","hellas","santorini","mykonos","rhodes","crete",
                "thessaloniki","Αθήνα","Σαντορίνη","Μύκονος","Ρόδος","Κρήτη","SuggestionsGR","culture","summer", "beach","travel","ταξίδι","θάλασσα","καλοκαίρι"));
        KEYWORDS_OPTIONAL.addAll(Arrays.asList("culture","summer", "beach","travel","ταξίδι","θάλασσα","καλοκαίρι"));
        KEYWORDS_NECESSARY.addAll(Arrays.asList("athens","greece","hellas","santorini","mykonos","rhodes","crete",
                "thessaloniki","Αθήνα","Σαντορίνη","Μύκονος","Ρόδος","Κρήτη","SuggestionsGR"));
    }

    public IncomingTweetListener(IncomingTweetAnalysis incomingTweetAnalysis){
        this.incomingTweetAnalysis=incomingTweetAnalysis;
        startListener();
    }

    private void startListener() {
        StatusListener listener = new StatusListener() {

            @Override
            public void onStatus(Status status) {
                try {
                    if(containsKeyword(status)) {
                        String statusAsJSON= TwitterObjectFactory.getRawJSON(status);
                        if(!status.isRetweet()) {
                            incomingTweetAnalysis.handleIncomingTweet(status, statusAsJSON);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            @Override
            public void onTrackLimitationNotice(int i) {

            }

            @Override
            public void onScrubGeo(long l, long l1) {

            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            @Override
            public void onException(Exception e) {

            }
        };
        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);
        FilterQuery filterQuery = new FilterQuery();


        double[][] locations = {{19.380,34.800}, {29.600,41.740}}; //covers greece according to Worldatlas.com
        filterQuery.locations(locations);
        String[] arr = new String[KEYWORDS.size()];
        arr = KEYWORDS.toArray(arr);
        filterQuery.track(arr);
        twitterStream.filter(filterQuery);

    }
    public boolean containsKeyword(Status status) {
        boolean flag = false;
        String text = status.getText().toLowerCase();
        for (HashtagEntity hashtagEntity : status.getHashtagEntities()) {
            String lowerHashtag = hashtagEntity.getText().toLowerCase();
            if (KEYWORDS.contains(lowerHashtag)) {
                if (KEYWORDS_OPTIONAL.contains(lowerHashtag)) {
                    for (String keyword : KEYWORDS_NECESSARY) {
                        if (text.contains(keyword)) {
                            flag = true;
                            break;
                        }
                    }
                } else {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }
}
