package com.appsmobilecompany.base;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebSettings;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class OfflineMode extends CordovaPlugin {

    public static final List<String> CACHED_EXTENSIONS = new ArrayList<String>();
    static {
        CACHED_EXTENSIONS.add("gif");
        CACHED_EXTENSIONS.add("png");
        CACHED_EXTENSIONS.add("jpg");
        CACHED_EXTENSIONS.add("js");
        CACHED_EXTENSIONS.add("css");
    }

    public boolean isOnline = true;
    public boolean isAwareOfReachability = false;
    public boolean postNotifications = false;
    public String checkConnectionUrl;
    public CallbackContext icb;

    protected void pluginInitialize() {
        checkConnection();
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("setCheckConnectionURL")) {
            if(Uri.parse(args.getString(0)) != null && args.getString(0).trim().startsWith("http")) {
                checkConnectionUrl = args.getString(0);
                callbackContext.success();
                return true;
            }
        }

        if (action.equals("setInternalCallback")) {
            icb = callbackContext;
            return true;
        }

        if (action.equals("cacheURL")) {
            final Uri uri = Uri.parse(args.getString(0));
            if(uri != null && args.getString(0).trim().startsWith("http")) {
                Runnable r = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try {
                            CordovaResourceApi.OpenForReadResult result = handleOpenForRead(toPluginUri(uri));
                            if(result.length > 0) {
                                callbackContext.success();
                                return;
                            }
                        } catch(IOException e) {
                            Log.e("Error: ", e.getMessage());
                        }
                        callbackContext.error("error");
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }
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
        String cachePath = this.cachePathForUri(orig);

        try {
            URL url = new URL(orig.toString());
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            long lengthOfFile = connection.getContentLength();

            if(connection.getResponseCode() >= 300 && connection.getResponseCode() <= 310) {
                String newUrl = connection.getHeaderField("Location");
                if(newUrl != url.toString()) {
                    return this.handleOpenForRead(toPluginUri(Uri.parse(newUrl)));
                }
            } else if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 400) {
                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

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
            }

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

        File cached = new File(cachePath);
        if(cached.exists()) {
            InputStream stream = new FileInputStream(cached);
            int contentLength = stream.available();
            String contentType = "application/octet-stream";

            File meta_file = new File(cachePath+".meta");
            if(meta_file.exists()) {
                try {
                    InputStream meta = new FileInputStream(meta_file);
                    int metasize = meta.available();
                    byte[] buffer = new byte[metasize];
                    meta.read(buffer);
                    meta.close();
                    JSONObject metadata = new JSONObject(new String(buffer, "UTF-8"));
                    String jsonContentType = metadata.getString("Content-Type");
                    int jsonContentLength = metadata.getInt("Content-Length");

                    if(jsonContentType != null && !jsonContentType.isEmpty())
                        contentType = jsonContentType;
                    if(jsonContentLength > 0)
                        contentLength = jsonContentLength;

                } catch(Exception e) {
                    Log.e("Error: ", e.getMessage());
                }
            }

            return new CordovaResourceApi.OpenForReadResult(uri, stream, contentType, contentLength, null);
        }

        return new CordovaResourceApi.OpenForReadResult(uri, null, "text/plain", 0, null);
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

    private void setReachable() {
        isAwareOfReachability = true;
        isOnline = true;

        this.postNotification();
    }

    private void setUnreachable() {
        isAwareOfReachability = true;
        isOnline = false;

        this.postNotification();
    }

    private void postNotification() {
        if(isAwareOfReachability && postNotifications) {
            try {
                JSONObject json_result = new JSONObject();
                json_result.put("isOnline", isOnline);
                PluginResult presult = new PluginResult(PluginResult.Status.OK, json_result);
                presult.setKeepCallback(true);
                icb.sendPluginResult(presult);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkConnection() {
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                HttpURLConnection connection = null;
                if(checkConnectionUrl != null && !checkConnectionUrl.trim().equals("")) {
                    postNotifications = true;

                    try {
                        URL url = new URL(checkConnectionUrl);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.connect();

                        if (connection.getResponseCode() == 200) {
                            String result = "";
                            InputStream in = new BufferedInputStream(connection.getInputStream());
                            if (in != null) {
                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                                String line = "";

                                while ((line = bufferedReader.readLine()) != null)
                                    result += line;
                            }
                            in.close();
                            result = result.trim();
                            if (result.equals("1")) {
                                setReachable();
                            } else {
                                setUnreachable();
                            }
                        }

                    } catch(UnknownHostException e) {
                        setUnreachable();
                    } catch (Exception e) {
                        Log.e("Error: ", e.getMessage());
                    } finally {
                        if (connection != null)
                            connection.disconnect();
                    }
                }

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        checkConnection();
                    }
                }, 3000);
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

}
