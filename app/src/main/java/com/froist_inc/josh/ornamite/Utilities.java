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
import java.util.HashMap;

public class Utilities
{
    public static HashMap<String, TvSeriesData> AllSeries;
    public static int Success = 1;
//    public static int Error = 0;

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

    public static class TvSeriesData
    {
        public String series_name;
        private Long   series_id;

        public boolean IsSubscribed() {
            return is_subscribed;
        }

        public String GetSeriesName() {
            return series_name;
        }

        public void SetIsSubscribed( boolean is_subscribed ) {
            this.is_subscribed = is_subscribed;
        }

        private boolean is_subscribed;

        public TvSeriesData( final String title, final long id, final boolean subscribed )
        {
            series_name = title;
            series_id = id;
            is_subscribed = subscribed;
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
}
