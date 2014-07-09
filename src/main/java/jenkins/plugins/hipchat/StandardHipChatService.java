package jenkins.plugins.hipchat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

public class StandardHipChatService implements HipChatService {

    private static final Logger logger = Logger.getLogger(StandardHipChatService.class.getName());

    private String host = "api.hipchat.com";
    private String token;
    private String[] roomIds;
    private String from;

    public StandardHipChatService(String token, String roomId, String from) {
        super();
        this.token = token;
        this.roomIds = roomId.split(",");
        this.from = from;
    }

    public void publish(String message) {
        publish(message, "yellow");
    }

    public void publish(String message, String color) {
        for (String roomId : roomIds) {
            logger.info("Posting: " + from + " to " + roomId + ": " + message + " " + color);
            HttpClient client = getHttpClient();
            String url = "https://" + host + "/v2/room/" + roomId.replace(" ", "%20") + "/notification?auth_token=" + token;
            PostMethod post = new PostMethod(url);

            try {
                JSONObject obj = new JSONObject();
                obj.put("message", message);
                obj.put("color", color);
                obj.put("notify", shouldNotify(color));

                post.setRequestEntity(new StringRequestEntity(obj.toString(), "application/json", "UTF-8"));
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if(responseCode != HttpStatus.SC_OK || ! response.contains("\"sent\"")) {
                    logger.log(Level.WARNING, "HipChat post may have failed. Response: " + response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to HipChat", e);
            } finally {
                post.releaseConnection();
            }
        }
    }
    
    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            }
        }
        return client;
    }

    private boolean shouldNotify(String color) {
        return color.equalsIgnoreCase("green") ? false : true;
    }

    void setHost(String host) {
        this.host = host;
    }
}
