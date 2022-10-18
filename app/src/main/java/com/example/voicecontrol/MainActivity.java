package com.example.voicecontrol;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.content.Context;


import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.protocol.types.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;





/*
Notes:
use web api to search and get track and playlist uri
use android sdk to play them and control playback


if user asks for example High way to hell by acdc, search for hwth then search through results till artist acdc found

next thing to do:
    sort though json data to get best match, if user asks for song by someone find the song by artist within json
    if no artist requested take first result
    make voice rec repeating, and without the pop up
    think about moving the start stuff to oncreate


 */



public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "";
    private static final String REDIRECT_URI = "https://www.google.com/";
    public SpotifyAppRemote mSpotifyAppRemote;
    private String ACCESS_TOKEN;
    public TextView text;
    private String data ="";
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    private boolean AddtoQueue;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
    }

    private final Handler handler = new Handler();

    private final Runnable DisplaySong = new Runnable() {
        @Override
        public void run() {
            //call function
            //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
                final Track track = playerState.track;
                if (track != null) {
                    String message = "Playing "+track.name+" by "+track.artist.name;
                    text.setText(message);
                }
            });
            //run runnable again with delay
            //handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //            case 10:
        //                if(resultCode==RESULT_OK && data !=null){
        //                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        //                    TextView text = findViewById(R.id.text);
        //                    text.setText(result.get(0));
        //
        //                }
        //                break;
        if (requestCode == 20) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    //Toast.makeText(this,response.getAccessToken(),Toast.LENGTH_SHORT).show();
                    ACCESS_TOKEN = response.getAccessToken();
                    text = findViewById(R.id.text);
                    //text.setText(ACCESS_TOKEN);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Toast.makeText(this, "Error with auth", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this,response.getError(),Toast.LENGTH_LONG).show();
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }





//    public void TestSearch(View view){
//
//        Toast.makeText(this,data,Toast.LENGTH_SHORT).show();
//
//
//        //text.setText(data);
//        if (data !=null) {
//            try {
//                JSONObject json = new JSONObject(data);
//                if (json.length()==0){
//                    text.setText("error converting json");
//                }
//                JSONObject tracks = new JSONObject(json.get("tracks").toString());
//                JSONArray items = new JSONArray(tracks.get("items").toString());
//                JSONObject item = items.getJSONObject(0);
//                String uri = item.get("uri").toString();
//                text.setText(uri);
//            } catch (JSONException e) {
//                e.printStackTrace();
//                text.setText(e.getMessage());
//            }
//        }
//
//
//    }

    public void StartSpeech(View view){
        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }




    @Override
    protected void onStart() {
        super.onStart();
        //handler.postDelayed(runnable, 5000);
        //getSpeechInput();
        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        //connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);
                        Context context = getApplicationContext();
                        CharSequence text = throwable.getMessage();
                        int duration = Toast.LENGTH_LONG;


                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });

        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming","playlist-read-private"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, 20, request);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.ENGLISH);





        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle bundle) {
                text.setText("Ready");
//                if (!WasPaused){
//                    mSpotifyAppRemote.getPlayerApi().resume();
//                }

//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        boolean paused;
//                        paused  = playerState.isPaused;
//                        if (!paused){
//                            mSpotifyAppRemote.getPlayerApi().resume();
//                        }
//                    });
//                }

            }

            @Override
            public void onBeginningOfSpeech() {
                text.setText("BeginningofSpeech");
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }

            }

            @Override
            public void onRmsChanged(float v) {
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }

            }

            @Override
            public void onBufferReceived(byte[] bytes) {
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }

            }

            @Override
            public void onEndOfSpeech() {
                text.setText("EndofSpeech");
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }
                handler.postDelayed(DisplaySong,2000);

            }

            @Override
            public void onError(int i) {
                text.setText("Error "+ i);
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }
                mSpeechRecognizer.cancel();

                handler.postDelayed(DisplaySong,2000);


            }

            @Override
            public void onResults(Bundle results) {
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if ((matches==null)||(matches.isEmpty())) {
                    text.setText("Don't know what you said sorry");
                } else{
                    String command = matches.get(0);
                    command = command.toLowerCase();
                    if (command.equals("resume")) {
                        Resume();
                    } else if (command.equals("pause")) {
                        Pause();
                    } else if (command.equals("next song")) {
                        Next();
                    } else if (command.equals("previous song")) {
                        Previous();
                    } else if (command.equals("toggle shuffle")) {
                        ToggleShuffle();
                    } else if (command.equals("toggle repeat")) {
                        ToggleRepeat();
                    } else if (command.contains("play")) {
                        AddtoQueue = false;
                        command = command.replace("play ", "");
                        if (command.contains("by")) {
                            int index = command.indexOf("by");
                            String artist = command.substring(index);
                            artist = artist.replace("by ", "");
                            artist = artist.replace(" ", "+");
                            artist = artist.replace("'", "%27");
                            if (command.contains("song")) {
                                command = command.substring(0, index - 1);
                                String song = command.replace("song ", "");
                                song = song.replace(" ", "+");
                                song = song.replace("'", "%27");
                                PlaySong(song, artist);
                            } else if (command.contains("playlist")) {
                                command = command.substring(0, index - 1);
                                String playlist = command.replace("playlist ", "");
                                playlist = playlist.replace(" ", "+");
                                playlist = playlist.replace("'", "%27");
                                PlayPlaylist(playlist, artist);
                            } else if (command.contains("album")) {
                                command = command.substring(0, index - 1);
                                String album = command.replace("album ", "");
                                album = album.replace(" ", "+");
                                album = album.replace("'", "%27");
                                PlayAlbum(album, artist);
                            }
                        } else if (command.contains("my")) {
                            command = command.replace("my ", "");
                            if (command.contains("playlist")) {
                                command = command.replace("playlist ", "");
                                String playlist = command;
                                playlist = playlist.replace(" ", "+");
                                playlist = playlist.replace("'", "%27");
                                PlayMyPlaylist(playlist);
                            }
                        } else {
                            if (command.contains("song")) {
                                command = command.replace("song ", "");
                                String song = command.replace(" ", "+");
                                song = song.replace("'", "%27");
                                PlaySong(song);
                            } else if (command.contains("playlist")) {
                                command = command.replace("playlist ", "");
                                String playlist = command.replace(" ", "+");
                                playlist = playlist.replace("'", "%27");
                                PlayPlaylist(playlist);
                            } else if (command.contains("album")) {
                                command = command.replace("album ", "");
                                String album = command.replace(" ", "+");
                                album = album.replace("'", "%27");
                                PlayAlbum(album);
                            }
                        }
                    } else if (command.contains("add song")) {
                        AddtoQueue = true;
                        command = command.replace("add song ", "");
                        command = command.replace(" to the queue", "");
                        if (command.contains("by")) {
                            int index = command.indexOf("by");
                            String artist = command.substring(index);
                            artist = artist.replace("by ", "");
                            artist = artist.replace(" ", "+");
                            artist = artist.replace("'", "%27");
                            command = command.substring(0, index - 1);
                            String song = command.replace("song ", "");
                            song = song.replace(" ", "+");
                            song = song.replace("'", "%27");
                            PlaySong(song, artist);
                        } else {
                            command = command.replace("song ", "");
                            String song = command.replace(" ", "+");
                            song = song.replace("'", "%27");
                            PlaySong(song);
                        }
                    }
                    //Toast.makeText(context,matches.get(0),Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onPartialResults(Bundle bundle) {
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }

            }

            @Override
            public void onEvent(int i, Bundle bundle) {
//                if (mSpotifyAppRemote!=null) { // add see if playing otherwies impossible to pause // also stop beeps if possible
//                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
//                        WasPaused  = playerState.isPaused;
//                    });
//
//                }

            }
        });
    }

    public void Resume(){
        mSpotifyAppRemote.getPlayerApi().resume();
    }
    public void Pause(){
        mSpotifyAppRemote.getPlayerApi().pause();
    }
    public void Next(){
        mSpotifyAppRemote.getPlayerApi().skipNext();
    }
    public void Previous(){
        mSpotifyAppRemote.getPlayerApi().seekTo(0);
        mSpotifyAppRemote.getPlayerApi().skipPrevious();
    }
    public void ToggleShuffle(){
        mSpotifyAppRemote.getPlayerApi().toggleShuffle();
    }
    public void ToggleRepeat(){
        mSpotifyAppRemote.getPlayerApi().toggleRepeat();
    }

    public void PlaySong(String song, String artist){
        String urlstring = "https://api.spotify.com/v1/search?q="+song+"&type=track&limit=3";
        String jsondata = SearchSpotify(urlstring);
        //Toast.makeText(this,jsondata,Toast.LENGTH_SHORT).show();
        if (jsondata !=null) {
            try {
                boolean found = false;
                int i=0;
                JSONObject json = new JSONObject(jsondata);
                JSONObject tracks = new JSONObject(json.get("tracks").toString());
                JSONArray items = new JSONArray(tracks.get("items").toString());
                int lengthofarray = items.length();
                JSONObject item;
                do {
                    item = items.getJSONObject(i);
                    JSONArray artists = new JSONArray(item.get("artists").toString());
                    JSONObject artistsJSONObject = artists.getJSONObject(0);
                    //if (artistsJSONObject.get("name").toString().contains("/")){
                    String tempartist = artistsJSONObject.get("name").toString();
                    tempartist = tempartist.replace("/","");
                    tempartist = tempartist.replace(".","");
                    tempartist = tempartist.replace(" ","+");
                    if(tempartist.equals(artist.toLowerCase())){
                        found=true;
                    }
//                    }else {
//                        String tempartist = artistsJSONObject.get("name").toString().replace(" ","+");
//                        if(tempartist.toLowerCase().equals(artist.toLowerCase())){
//                            found=true;
//                        }/home/alex/Documents/Other Programming/AndroidApps/FileViewer/home/alex/Documents/Other Programming/AndroidApps/FileViewer
//                    }
                    i+=1;
                } while ((!found)&&(i!=lengthofarray));
                if ((!found)){
                    item = items.getJSONObject(0); // if artist not found play first result
                }

                String uri = item.get("uri").toString();
                mSpotifyAppRemote.getPlayerApi().queue(uri);
                Thread play = new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        mSpotifyAppRemote.getPlayerApi().skipNext();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }

                });
                if (!AddtoQueue){
                    play.start();
                }


                //Toast.makeText(this,uri,Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }


    }
    public void PlaySong(String song){ // when no artist is provided, play first result
        String urlstring = "https://api.spotify.com/v1/search?q="+song+"&type=track&limit=3";
        String jsondata = SearchSpotify(urlstring);
        if (jsondata !=null) {
            try {
                JSONObject json = new JSONObject(jsondata);
                JSONObject tracks = new JSONObject(json.get("tracks").toString());
                JSONArray items = new JSONArray(tracks.get("items").toString());
                JSONObject item = items.getJSONObject(0);
                String uri = item.get("uri").toString();
                mSpotifyAppRemote.getPlayerApi().queue(uri);
                Thread play = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        mSpotifyAppRemote.getPlayerApi().skipNext();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }

                });
                if (!AddtoQueue) {
                    play.start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }
    }


    public void PlayPlaylist(String playlist, String artist){
        String urlstring = "https://api.spotify.com/v1/search?q="+playlist+"&type=playlist&limit=3";
        String jsondata = SearchSpotify(urlstring);
        //Toast.makeText(this,jsondata,Toast.LENGTH_SHORT).show();
        if (jsondata !=null) {
            try {
                boolean found = false;
                int i=0;
                JSONObject json = new JSONObject(jsondata);
                JSONObject tracks = new JSONObject(json.get("playlists").toString());
                JSONArray items = new JSONArray(tracks.get("items").toString());
                int lengthofarray = items.length();
                JSONObject item;
                do {
                    item = items.getJSONObject(i);
                    //JSONArray artists = new JSONArray(item.get("artists").toString());
                    JSONObject artistsJSONObject = new JSONObject(item.get("owner").toString());
                    if (artistsJSONObject.get("id").toString().contains("/")){
                        String tempartist = artistsJSONObject.get("name").toString();
                        tempartist = tempartist.replace("/","");
                        tempartist = tempartist.replace(" ","+");
                        if(tempartist.equals(artist.toLowerCase())){
                            found=true;
                        }
                    }else {
                        String tempartist = artistsJSONObject.get("id").toString().replace(" ","+");
                        if(tempartist.equals(artist.toLowerCase())){
                            found=true;
                        }
                    }
                    i+=1;
                } while ((!found)&&(i!=lengthofarray));
                if (!found){
                    item = items.getJSONObject(0); // if artist not found play first result

                }

                String uri = item.get("uri").toString();
                Thread play = new Thread(() -> mSpotifyAppRemote.getPlayerApi().play(uri));
                play.start();

                //Toast.makeText(this,uri,Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }


    }
    public void PlayPlaylist(String playlist){
        String urlstring = "https://api.spotify.com/v1/search?q="+playlist+"&type=playlist&limit=3";
        String jsondata = SearchSpotify(urlstring);
        //Toast.makeText(this,jsondata,Toast.LENGTH_SHORT).show();
        if (jsondata !=null) {
            try {
                JSONObject json = new JSONObject(jsondata);
                JSONObject tracks = new JSONObject(json.get("playlists").toString());
                JSONArray items = new JSONArray(tracks.get("items").toString());
                JSONObject item = items.getJSONObject(0);
                String uri = item.get("uri").toString();
                Thread play = new Thread(() -> mSpotifyAppRemote.getPlayerApi().play(uri));
                play.start();
            } catch (JSONException e) {
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }

    }
    public void PlayMyPlaylist(String playlist){
        String urlstring = "https://api.spotify.com/v1/me/playlists";
        String jsondata = SearchSpotify(urlstring);
        if (jsondata!=null){
            try{
                boolean found = false;
                int i=0;
                JSONObject json = new JSONObject(jsondata);
                JSONArray items = new JSONArray(json.get("items").toString());
                int lengthofarray = items.length();
                JSONObject item;
                do {
                    item = items.getJSONObject(i);
                    String name = item.get("name").toString();
                    name = name.replace(".","");
                    playlist = playlist.replace("+"," ");
                    if(playlist.equals(name.toLowerCase())){
                        found=true;
                    }
                    i+=1;
                } while ((!found)&&(i!=lengthofarray));
                if (!found){
                    item = items.getJSONObject(0); // if artist not found play first result
                }

                String uri = item.get("uri").toString();
                Thread play = new Thread(() -> mSpotifyAppRemote.getPlayerApi().play(uri));
                play.start();

            }catch (JSONException e){
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }

    }

    public void PlayAlbum(String album, String artist){
        String urlstring ="https://api.spotify.com/v1/search?q="+album+"&type=album&limit=3";
        String jsondata = SearchSpotify(urlstring);
        if (jsondata!=null){
            try{
                boolean found =false;
                int i=0;
                JSONObject json = new JSONObject(jsondata);
                JSONObject albums = new JSONObject(json.get("albums").toString());
                JSONArray items = new JSONArray(albums.get("items").toString());
                int lengthofarray = items.length();
                JSONObject item;
                do {
                    item = items.getJSONObject(i);
                    JSONArray artists = new JSONArray(item.get("artists").toString());
                    JSONObject artistsJSONObject = artists.getJSONObject(0);
                    //if (artistsJSONObject.get("name").toString().contains("/")){
                    String tempartist = artistsJSONObject.get("name").toString();
                    tempartist = tempartist.replace("/","");
                    tempartist = tempartist.replace(".","");
                    tempartist = tempartist.replace(" ","+");

                    if(tempartist.equals(artist.toLowerCase())){
                        found=true;
                    }
//                    }else {
//                        String tempartist = artistsJSONObject.get("name").toString().replace(" ","+");
//                        if(tempartist.toLowerCase().equals(artist.toLowerCase())){
//                            found=true;
//                        }
//                    }
                    i+=1;
                } while ((!found)&&(i!=lengthofarray));
                if (!found){
                    item = items.getJSONObject(0); // if artist not found play first result
                }

                String uri = item.get("uri").toString();
                Thread play = new Thread(() -> mSpotifyAppRemote.getPlayerApi().play(uri));
                play.start();

            } catch (JSONException e){
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }

    }
    public void PlayAlbum(String album){
        String urlstring ="https://api.spotify.com/v1/search?q="+album+"&type=album&limit=3";
        String jsondata = SearchSpotify(urlstring);
        if (jsondata!=null){
            try{
                JSONObject json = new JSONObject(jsondata);
                JSONObject tracks = new JSONObject(json.get("albums").toString());
                JSONArray items = new JSONArray(tracks.get("items").toString());
                JSONObject item = items.getJSONObject(0);
                String uri = item.get("uri").toString();
                Thread play = new Thread(() -> mSpotifyAppRemote.getPlayerApi().play(uri));
                play.start();

            }catch (JSONException e){
                e.printStackTrace();
                text.setText(e.getMessage());
            }
        }

    }

    public String SearchSpotify(String urlstring){
        data="";
        StringBuilder datastr = new StringBuilder();
        Thread Network = new Thread(() -> {
            //Do whatever
            HttpURLConnection urlConnection = null;
            try {
                //String urlstring = "https://api.spotify.com/v1/search?q=highway+to+hell&type=track&limit=1&market=US"; //Authorization: Bearer "+ACCESS_TOKEN;
                URL url = new URL(urlstring);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+ACCESS_TOKEN);
                urlConnection.setRequestProperty("Accept","application/json");
                urlConnection.setRequestProperty("Content-Type","application/json");
                InputStream stream = urlConnection.getInputStream();
                BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
                //temp string to hold each line
                String inputLine="";

                while(inputLine !=null){
                    inputLine=bin.readLine();
                    datastr.append(inputLine);
                    //data = data + inputLine;
                }
                data = datastr.toString();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if (urlConnection!=null) {
                    urlConnection.disconnect();
                }
            }

        });
        Network.start();
        try {
            Network.join();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }


        return data;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("MainActivity","app closed");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("MainActivity","app minimised");
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        AuthorizationClient.clearCookies(this);
    }


}
