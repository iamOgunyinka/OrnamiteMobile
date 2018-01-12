package com.froist_inc.josh.ornamite;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

public class Utilities
{
    public static HashMap<String, TvSeriesData> AllSeries;
    public static int Success = 1;
    public static HashMap<String, ArrayList<EpisodeData>> AllUpdates;

    public static class JsonParser
    {
        JsonParser(){}

        JSONArray ParseListOfObjects( final String data )
        {
            if( data == null ) return null;
            try {
                return ( JSONArray ) new JSONTokener( data ).nextValue();
            } catch ( JSONException except ){
                except.printStackTrace();
                return null;
            }
        }
    }

    public static class EpisodeData
    {
        public String episode_name;
        public ArrayList<DownloadsData> download_links;

        public EpisodeData( final String episode_name )
        {
            this.episode_name = episode_name;
            download_links = new ArrayList<>();
        }

        public void AddDownloadsData( final DownloadsData data )
        {
            download_links.add( data );
        }
    }

    public static class DownloadsData
    {
        public String download_type;
        public String download_link;

        public DownloadsData( final String download_type, final String download_link )
        {
            this.download_type = download_type;
            this.download_link = download_link;
        }
    }

    public static class TvSeriesData
    {
        public String series_name;
        private Long   series_id;
        private boolean is_subscribed;

        public TvSeriesData( final String title, final long id, final boolean subscribed )
        {
            series_name = title;
            series_id = id;
            is_subscribed = subscribed;
        }

        public boolean IsSubscribed() {
            return is_subscribed;
        }

        public String GetSeriesName() {
            return series_name;
        }

        public void SetIsSubscribed( boolean is_subscribed ) {
            this.is_subscribed = is_subscribed;
        }
        public long GetSeriesID() {
            return series_id;
        }
        public JSONObject ToJson() throws JSONException
        {
            JSONObject data = new JSONObject();
            data.put( "name", series_name );
            data.put( "id", series_id );
            data.put( "subscribed", is_subscribed );

            return data;
        }
    }

    public static String ReadDataFile( final Context context, final String filename ) throws IOException
    {
        BufferedReader reader = null;
        try {
            InputStream input_stream = context.openFileInput( filename );
            reader = new BufferedReader( new InputStreamReader( input_stream ) );
            StringBuilder buffer = new StringBuilder();
            String each_line;
            while ( ( each_line = reader.readLine() ) != null) {
                buffer.append(each_line);
            }
            return buffer.toString();
        } finally {
            if( reader != null ) reader.close();
        }
    }

    public static HashMap<String, TvSeriesData> ReadTvSeriesData( final Context context, final String filename )
            throws IOException, JSONException
    {
        HashMap<String, TvSeriesData> data = new HashMap<>();
        String data_string;
        try {
            data_string = ReadDataFile( context, filename );
        } catch (IOException except ){
            FileOutputStream output_stream = context.openFileOutput( filename, Context.MODE_PRIVATE );
            Writer writer = new OutputStreamWriter( output_stream );
            data_string = "[]";
            writer.write( data_string );
            writer.close();
        }
        JSONArray root_object = new JsonParser().ParseListOfObjects( data_string );
        for( int i = 0; i != root_object.length(); ++i )
        {
            JSONObject series = root_object.getJSONObject( i );
            final String series_name = series.getString( "name" );
            final long   series_id = series.getLong( "id" );
            final boolean is_subscribed = series.getBoolean( "subscribed" );
            data.put( series_name, new TvSeriesData(series_name, series_id, is_subscribed ) );
        }
        return data;
    }

    public static void WriteTvSeriesData( final Context context,
                                          final String filename,
                                          final HashMap<String, TvSeriesData> data_map )
            throws JSONException, IOException
    {
        if( data_map.size() == 0 ) return;
        JSONArray list_of_tv_series = new JSONArray();
        for ( HashMap.Entry pair : data_map.entrySet() ) {
            final TvSeriesData data_item = ( TvSeriesData ) pair.getValue();
            list_of_tv_series.put( data_item.ToJson() );
        }
        WriteDataToDisk( context, filename, list_of_tv_series.toString() );
    }

    private static void WriteDataToDisk( final Context context, final String filename, final String buffer )
            throws IOException
    {
        Writer writer = null;
        try {
            FileOutputStream output_stream = context.openFileOutput( filename, Context.MODE_PRIVATE );
            writer = new OutputStreamWriter(output_stream);
            writer.write( buffer );
        } finally {
            if( writer != null ) writer.close();
        }
    }

    public static HashMap<String, ArrayList<EpisodeData>> ReadTvUpdates( final Context context, final String filename )
            throws JSONException, IOException
    {
        HashMap<String, ArrayList<EpisodeData>> data_map = new HashMap<>();
        String data_string;
        try {
            data_string = ReadDataFile( context, filename );
        } catch (IOException except ){
            FileOutputStream output_stream = context.openFileOutput( filename, Context.MODE_PRIVATE );
            Writer writer = new OutputStreamWriter( output_stream );
            data_string = "[]";
            writer.write( data_string );
            writer.close();
        }

        JSONArray root_data = new JsonParser().ParseListOfObjects( data_string );
        for( int i = 0; i != root_data.length(); ++i ){
            final JSONObject data_item = root_data.getJSONObject( i );
            final String date = data_item.getString( "date" );
            final JSONArray detail = data_item.getJSONArray( "detail" );
            ArrayList<EpisodeData> episodes = new ArrayList<>();

            for( int j = 0 ; j != detail.length(); ++j ){
                final JSONObject tv_data = detail.getJSONObject( j );
                final String episode_name = tv_data.getString( "name" );
                final JSONArray dl_links = tv_data.getJSONArray( "dl_links" );
                EpisodeData episode = new EpisodeData( episode_name );

                for( int x = 0; x != dl_links.length(); ++x ){
                    final JSONObject download_data = dl_links.getJSONObject( x );
                    final String download_type = download_data.getString( "type" );
                    final String download_link = download_data.getString( "link" );
                    episode.AddDownloadsData( new DownloadsData( download_type, download_link ) );
                }
                episodes.add( episode );
            }
            data_map.put( date, episodes );
        }
        return data_map;
    }
}
