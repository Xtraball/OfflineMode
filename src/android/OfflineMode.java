package dev.siberiancms.www.test;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebSettings;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class OfflineMode extends CordovaPlugin {

    public static final List<String> CACHED_EXTENSIONS = new ArrayList<String>();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        CACHED_EXTENSIONS.add("gif");
        CACHED_EXTENSIONS.add("png");
        CACHED_EXTENSIONS.add("jpg");
        CACHED_EXTENSIONS.add("js");
        CACHED_EXTENSIONS.add("css");
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	if (action.equals("useCache")) {
            this.useCache(args.getString(0), callbackContext);
            return true;
        }

        return false;
    }

    @Override
    public Uri remapUri(Uri uri) {
        if(uri.getScheme().startsWith("http")) {
            String filename = uri.getLastPathSegment();

            if(filename != null) {
                int lastDot = filename.lastIndexOf(".");
                if(lastDot > 0 && lastDot+1 < filename.length()) {
                    String ext = filename.substring(lastDot+1);
                    if(CACHED_EXTENSIONS.contains(ext)) {
                        return toPluginUri(uri);
                    }
                }
            }
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public CordovaResourceApi.OpenForReadResult handleOpenForRead(Uri uri) throws IOException {
        Uri orig = fromPluginUri(uri);

        try {
            URL url = new URL(orig.toString());
            URLConnection connection = url.openConnection();
            connection.connect();

            long lengthOfFile = connection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            String cachePath = this.cachePathForUri(orig);

            // Output stream
            OutputStream output = new FileOutputStream(cachePath);

            byte data[] = new byte[1024];

            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.reset();

            JSONObject meta = new JSONObject();
            meta.put("Content-Type", connection.getContentType());
            meta.put("Content-Length", connection.getContentLength());

            FileWriter file = new FileWriter(cachePath+".meta");
            file.write(meta.toString(4));
            file.flush();
            file.close();

            return new CordovaResourceApi.OpenForReadResult(orig, input, connection.getContentType(), lengthOfFile, null);
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

        return new CordovaResourceApi.OpenForReadResult(uri, null, "text/plain", 0, null);
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    private void useCache(String use_cache, CallbackContext callbackContext) {
        if(use_cache == "1") {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        } else {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        }

        callbackContext.success();
        Log.e("OFFLINEMODE", "useCache: "+use_cache);
    }

    private String cachePathForUri(Uri originalURI) {
        String URI = originalURI.toString();

        URI = URI.replaceAll("\\.css\\?t=\\d+$", ".css");

        return new File(this.cordova.getActivity().getApplicationContext().getCacheDir(), this.makeSHA1Hash(URI)).getAbsolutePath();
    }

    private String makeSHA1Hash(String input)
    {
        String hexStr = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            byte[] buffer = input.getBytes("UTF-8");
            md.update(buffer);
            byte[] digest = md.digest();

            for (int i = 0; i < digest.length; i++) {
                hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        return hexStr;
    }


}
