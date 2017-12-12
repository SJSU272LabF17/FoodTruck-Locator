import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.GeocodingResult;
import com.vdurmont.emoji.EmojiParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import edu.stanford.nlp.simple.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TwitterBot {
    public Twitter twitter;
    public TwitterStream twitterStream;
    public GeoApiContext gContext;

    public TwitterBot() {
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("MS7OhRzOnKTOM1cPVr1Q3LmZI")
                .setOAuthConsumerSecret("zL2JOVfMppwwfZb1POclssw8Iy3njZmKIdM7KSgwRsNBrLl4wF")
                .setOAuthAccessToken("927734664730525697-3NlBQVjlmyA6AGgW2GTIlp97FtaQ2uN")
                .setOAuthAccessTokenSecret("rZfqq3eAU8Rf7wT9cGFMhHBAlzJbVAEltXL6jHKJSD0Gv");

        Configuration config = cb.build();
        TwitterFactory tf = new TwitterFactory(config);
        TwitterStreamFactory tsf = new TwitterStreamFactory(config);
        twitter = tf.getInstance();
        twitterStream = tsf.getInstance();

        gContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyADP5ixTlchIaI1TpveOfSCcUnlRirH6J4")
                .build();
    }

    public void startListen(long... userIDs) {
        StatusListener listener = new StatusListener(){
            public void onStatus(Status status) {
                Event newEvent = processStatus(status);

                if (newEvent != null) {
                    postEvent(newEvent);
                }
            }
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {

            }

            @Override
            public void onStallWarning(StallWarning warning) {

            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };

        FilterQuery fq = new FilterQuery(userIDs);

        twitterStream.addListener(listener);
        twitterStream.filter(fq);
    }

    public boolean postEvent(Event event) {
        String postUrl = "http://52.53.249.123:3600/create";// put in your url
        Gson gson = new Gson();
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(postUrl);
        StringEntity postingString = null;
        String json = gson.toJson(event);
        try {
            postingString = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        post.setEntity(postingString);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");
        HttpResponse response = null;
        try {
            response = httpClient.execute(post);
        } catch (IOException e) {
            return false;
        }

        if (response == null) {
            return false;
        }

        return true;
    }

    public Event processStatus(Status status) {
        if (status.isRetweet()) {
            return null;
        }

        Event newEvent = processText(status.getText());
        if (newEvent == null) {
            return null;
        }

        newEvent.originalTweet = status.getText();
        newEvent.linkToTweet = "https://twitter.com/" + status.getUser().getScreenName()
                + "/status/" + status.getId();
        newEvent.twitterHandle = "@"+status.getUser().getScreenName();
        newEvent.twitterID = Long.toString(status.getUser().getId());
        newEvent.merchantName = status.getUser().getName();
        newEvent.merchantLogo = status.getUser().getBiggerProfileImageURL();

        return newEvent;
    }

    public Event processText(String text) {
        Sentence sentence = new Sentence(text);
        List<String> words = sentence.words();
        List<String> nerTags = sentence.nerTags();

        System.out.println("--------------------");
        System.out.println("Original text : "+ text);
        System.out.println("Words         : "+ words);
        System.out.println("NER Tags      : "+ nerTags);
        System.out.println("--------------------");

        GeocodingResult geocodingResult = extractLocationFromString(text, words, nerTags);

        if (geocodingResult == null) {
            System.out.println("No location information for this text");
            return null;
        }

        Date finalDate = extractDateFromString(text, words, nerTags);

        if (finalDate == null) {
            return null;
        }

        System.out.println("Processed Date: "+ finalDate+"\n");

        Event event = new Event();
        event.lat = Double.toString(geocodingResult.geometry.location.lat);
        event.lon = Double.toString(geocodingResult.geometry.location.lng);
        event.address = geocodingResult.formattedAddress;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        event.eventDate = format.format(finalDate);

        return event;
    }

    public GeocodingResult extractLocationFromString(String text, List<String> tokenizedWords, List<String> nerTags) {
        List<String> possibleBuffer = new ArrayList<String>();
        List<String> locationOnlyBuffer = new ArrayList<String>();

        for(int i = 0; i < nerTags.size(); i++) {
            String currentWord = tokenizedWords.get(i);
            String currentNer = nerTags.get(i);
            String nextNer = null;
            String nextNextNer = null;

            if (i < nerTags.size()-1 && nerTags.size()-1 >= 0) {
                nextNer = nerTags.get(i+1);
                if (i < nerTags.size()-2 && nerTags.size()-2 >= 0) {
                    nextNextNer = nerTags.get(i+2);
                }
            }

            if (currentNer.equals("O")) {
                continue;
            }

            if (EmojiParser.removeAllEmojis(currentWord).length() != currentWord.length()
                    || currentWord.contains("!")
                    || currentWord.contains("?")) {
                continue;
            }

            Boolean isCurrentNumberOnly = (currentNer.equals("DATE") || currentNer.equals("TIME") || currentNer.equals("NUMBER")) && currentWord.matches("[0-9]+");
            if (currentNer.equals("ORDINAL") || isCurrentNumberOnly) {
                if (nextNer != null && nextNer.equals("LOCATION")) {
                    possibleBuffer.add(currentWord);
                    continue;
                } else if(nextNextNer != null && nextNextNer.equals("LOCATION")) {
                    possibleBuffer.add(currentWord);
                    continue;
                }
            }

            if (currentNer.equals("LOCATION")) {
                possibleBuffer.add(currentWord);
                locationOnlyBuffer.add(currentWord);
                continue;
            }
        }


        String finalPossibleString = String.join(" ", possibleBuffer);
        String finalLocationOnlyString = String.join(" ", locationOnlyBuffer);

        System.out.println("finalPossibleString: " + finalPossibleString);
        System.out.println("finalLocationOnlyString: " + finalLocationOnlyString);

        if (finalPossibleString.length() > 0) {
            // First use possible string
            GeocodingApiRequest req = GeocodingApi.newRequest(gContext).address(finalPossibleString);
            GeocodingResult[] results = req.awaitIgnoreError();

            // Use location only string when there is no result from Google
            if (finalLocationOnlyString.length() > 0 && (results == null || results.length == 0)) {
                req = GeocodingApi.newRequest(gContext).address(finalLocationOnlyString);
                results = req.awaitIgnoreError();
            }

            if (results != null && results.length != 0) {
                return results[0];
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public Date extractDateFromString(String text, List<String> tokenizedWords, List<String> nerTags) {
        Date now = new Date();
        Boolean lunchFound = false;
        Boolean dinnerFound = false;
        Boolean todayFound = false;
        Boolean tomorrowFound = false;
        Boolean tonightFound = false;

        List<String> dateTimeBuffer = new ArrayList<String>();

        for(int i = 0; i < nerTags.size(); i++) {
            String currentWord = tokenizedWords.get(i);
            String currentNer = nerTags.get(i);
            String nextWord = null;
            if (i < tokenizedWords.size()-1 && tokenizedWords.size()-1 >= 0) {
                nextWord = tokenizedWords.get(i+1);
            }

            String lowCaseCurrentText = currentWord.toLowerCase();
            if (lowCaseCurrentText.equals("today")) {
                todayFound = true;
            } else if (lowCaseCurrentText.equals("lunch")) {
                lunchFound = true;
            } else if (lowCaseCurrentText.equals("dinner")) {
                dinnerFound = true;
            } else if (lowCaseCurrentText.equals("tomorrow")) {
                tomorrowFound = true;
            } else if (lowCaseCurrentText.equals("early")) {
                continue;
            } else if (lowCaseCurrentText.equals("late")) {
                continue;
            } else if (lowCaseCurrentText.equals("tonight")) {
                continue;
            }

            if (currentNer.equals("O")) {
                continue;
            }

            if (EmojiParser.removeAllEmojis(currentWord).length() != currentWord.length()
                    || currentWord.contains("!")
                    || currentWord.contains("?")) {
                continue;
            }

            if (currentWord.matches("[0-9]+") && (currentWord.length() == 3 || currentWord.length() == 4)) {
                if (nextWord != null) {
                    String lowCaseNextWord = nextWord.toLowerCase();
                    if (lowCaseNextWord.equals("am") || lowCaseNextWord.equals("pm")) {
                        if (currentWord.length() == 3) {
                            dateTimeBuffer.add(currentWord.substring(0,1)+":"+currentWord.substring(1));
                        } else {
                            dateTimeBuffer.add(currentWord.substring(0,2)+":"+currentWord.substring(2));
                        }
                        dateTimeBuffer.add(nextWord);
                        continue;
                    }
                }
                continue;
            }

            if (currentNer.equals("NUMBER")) {
                dateTimeBuffer.add(currentWord);
                continue;
            }

            if (currentNer.equals("DATE") || currentNer.equals("TIME")) {
                if (currentWord.contains("-")) {
                    String newText = currentWord.replace("-", " to ");
                    dateTimeBuffer.add(newText);
                } else {
                    dateTimeBuffer.add(currentWord);
                }
                continue;
            }
        }

        if (dateTimeBuffer == null || dateTimeBuffer.size() == 0) {
            return null;
        }

        String finalDateTimeString = String.join(" ", dateTimeBuffer);
        String lowCaseFinalString = finalDateTimeString.toLowerCase();
        if (lowCaseFinalString.equals("today") || lowCaseFinalString.equals("tonight") || lowCaseFinalString.equals("tomorrow")) {
            if (dinnerFound && todayFound || tonightFound) {
                finalDateTimeString = "6pm today";
            } else if (dinnerFound && tomorrowFound) {
                finalDateTimeString = "6pm tomorrow";
            } else if (lunchFound && todayFound) {
                finalDateTimeString = "12pm today";
            } else if (lunchFound && tomorrowFound) {
                finalDateTimeString = "12pm tomorrow";
            }
        }

        System.out.println("finalDateTimeString: "+finalDateTimeString);
        List<Date> dates = new ArrayList<Date>();
        if (finalDateTimeString.length() > 0) {
            dates = new PrettyTimeParser().parse(finalDateTimeString);
        }

        if (dates.size() == 0) {
            return null;
        } else {
            return dates.get(0);
        }
    }
}
