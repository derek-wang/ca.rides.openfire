package org.jivesoftware.openfire.plugin.messageHistory.interceptor;

import flexjson.JSONDeserializer;
import javapns.Push;
import javapns.communication.exceptions.KeystoreException;
import javapns.notification.PushNotificationPayload;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.plugin.messageHistory.util.DbUtil;
import org.jivesoftware.openfire.plugin.messageHistory.util.MessageStatus;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.json.JSONException;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * User: John Kuo
 * Date: 5/9/12
 * Time: 11:40 AM
 * Class MessageInterceptor intercepts incoming Packets.  In this particular scenario, Message type is our interest.
 * At the same time it inserts the Message into the DB for persistence.
 */
public class MessageInterceptor implements PacketInterceptor {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MessageInterceptor.class);
    final private static String HISTORY_MESSAGE = "HistoryMessage";
    final private static String HISTORY_MESSAGE_iOS = "HistoryMessageiOS";
    final private static String MESSAGE_STATUS = "MessageStatus";
    final private static int TWO_SECS = 1;
    Timer timer = new Timer();

    private UserManager userManager;
    private PresenceManager presenceManager;
    private XMPPServer server;

    //For push notification
    final static String DEV_P12_LOC = "/opt/openfire/plugins/rides_prod_push.p12";
    final static String LOC_P12_LOC = "C:\\Users\\HeavyArms\\Desktop\\WORK\\Bignition_Projects\\Rides\\ca.rides.messenger.openfire\\src\\plugins\\messageHistory\\resources\\rides_prod_push.p12";
    private String DEV_URL_FOR_DEVICE_INFO = "http://localhost:8080/ca.rides.web/mobile/webservice/ios/getDeviceInfoOnEmail?userEmail=";
    private String LOC_URL_FOR_DEVICE_INFO = "http://localhost:8080/mobile/webservice/ios/getDeviceInfoOnEmail?userEmail=";
    private String DEV_URL_FOR_USER_DISPLAY_INFO = "http://localhost:8080/ca.rides.web/mobile/webservice/ios/getUserDisplayOnEmail?userEmail=";
    private String LOC_URL_FOR_USER_DISPLAY_INFO = "http://localhost:8080/mobile/webservice/ios/getUserDisplayOnEmail?userEmail=";
    final private HttpClient httpClient = new HttpClient();
    private GetMethod getMethod;


    public MessageInterceptor(){
    }

    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {

        logger("XML = " + packet.toXML());

        //IQ request from User for HistoryMessage
        if(packet instanceof IQ && incoming == true && processed == true){

            final IQ iq = (IQ) packet;

            if(iq.getID() != null && iq.getID().contains(MESSAGE_STATUS)){
                logger("*********ToSetMessageStatus*************");
            }

            //Need to check here if the IQ packet ID is null
            if(iq.getID() != null && iq.getID().contains(HISTORY_MESSAGE)){
                final String fromJID = iq.getFrom().toBareJID();
                final DefaultElement targetElement = (DefaultElement)iq.getElement().content().get(1);
                final DefaultElement targetiOSElement = (DefaultElement)iq.getElement().content().get(2);
                final String targetJID = targetElement.getText();

                logger("*********RequestForHistoryMessage*************");
                logger("Request for the target JID = " + targetJID);
                logger("Request for the from JID = " + iq.getFrom().toBareJID());
                logger("Full JID = " + iq.getFrom().toFullJID());
                logger("Bare JID = " + iq.getFrom().toBareJID());
                logger("From JID = " + fromJID);
                logger("Target JID = " + targetJID);
                logger("Target iOS Element = " + targetiOSElement.getText());

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {

                        //From should be the target of query
                        //To should be the requester
                        DefaultElement subjectElement = null;
                        String subject = null;
                        DefaultElement windowIdElement = null;
                        String windowId = null;
                        try{
                            if(iq.getID().contains(HISTORY_MESSAGE_iOS)){
                                subjectElement = (DefaultElement)iq.getElement().content().get(3);
                            }else{
                                subjectElement = (DefaultElement)iq.getElement().content().get(2);
                            }

                            subject = subjectElement.getText();
                            if(iq.getElement().content().size() > 2)
                            {
                                windowIdElement = (DefaultElement)iq.getElement().content().get(3);
                                windowId = windowIdElement.getText();
                            }
                            else
                            {
                                windowId = "0";
                            }
                        }catch (Exception e){
                            logger.error("Not able to retrieve subject line of IQ packet");
                        }

                        if(subject != null && iq.getID().contains(HISTORY_MESSAGE_iOS)){
                            //For iOS specifically
                            this.userHistoryMessageDispatcher(DbUtil.retriveMessageOnSubjectForiOS(targetiOSElement.getText(), targetJID, subject));
                            logger("^^^^^^^^^^^^^^^^^^ retrieve history message for iOS got called ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                        }else if(subject != null && iq.getID().contains(HISTORY_MESSAGE)){
                            //For Web, has WindowID
                            this.userHistoryMessageDispatcher(DbUtil.retrieveMessageOnSubject(targetJID, fromJID, subject, windowId));
                            logger("^^^^^^^^^^^^^^^^^^ retrieve history message for Web got called ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                        }
                    }

                    private void userHistoryMessageDispatcher(Message historyMessage){
                                logger("HistoryMessage = " + historyMessage.toXML());
                                SessionManager.getInstance().sendServerMessage(historyMessage, new JID(fromJID));
                    }
                }, TWO_SECS);
            }
        }

        if (packet instanceof Message) {
            Message message = (Message) packet;
                logger("message type = " + message.getType());
                if(!message.getType().equals(Message.Type.normal))
                {

                    String vehicleId = message.getSubject();
                    String to = message.getTo().toString().substring(0,message.getTo().toString().lastIndexOf("@"));
                    String from = "";
                    if(message.getFrom() != null)
                        from = message.getFrom().toString().substring(0,message.getFrom().toString().lastIndexOf("@"));

                    logger("------------------Message Packet-------------------");

                    logger("message packet = " + message.toXML());


                    //Ken's bidLead
                    try
                    {
                        URL url = new URL("http://localhost:8080/ca.rides.web/bidLead/makeLead");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput(true);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "text/plain");

                        String data = vehicleId+","+to+","+from+","+message.getBody();
                        logger("Posting "+data+"to: http://localhost:8080/ca.rides.web/bidLead/makeLead");
                        OutputStream os = conn.getOutputStream();
                        os.write(data.getBytes());
                        os.flush();

                        String line;
                        StringBuilder results = new StringBuilder();

                        if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                            throw new RuntimeException("Failed : HTTP error code : "
                                    + conn.getResponseCode());
                        }

                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                (conn.getInputStream())));

                        String output;
                        logger("Output from Server .... \n");
                        while ((output = br.readLine()) != null) {
                            logger(output);
                        }
                        conn.disconnect();
                    }

                    catch (Exception e) {
                        logger(e.getMessage());
                    }

                    //only store when incoming and processed are TRUE
                    if (incoming == true && processed == true ) {

                        //Make sure it's not for History message
                        String subject = null;
                        if(message.getSubject() != null){
                            subject = message.getSubject();
                        }

                        server = XMPPServer.getInstance();
                        userManager = server.getUserManager();
                        presenceManager = server.getPresenceManager();
                        try
                        {
                            User user = userManager.getUser(message.getTo().toString());
                            boolean isAvailable = presenceManager.isAvailable(user);

                            if(isAvailable)
                                DbUtil.insertMessage(1, message.getFrom().toString(), message.getTo().toString(), subject,
                                    DbUtil.getCurrentDBTimestamp(), message.getBody(), MessageStatus.UNREAD, false);
                            else
                                DbUtil.insertMessage(1, message.getFrom().toString(), message.getTo().toString(), subject,
                                    DbUtil.getCurrentDBTimestamp(), message.getBody(), MessageStatus.UNREAD, true);
                        }
                        catch ( UserNotFoundException e)
                        {
                            logger("User not found");
                        }

                        //For Push notification
                        String vehicleSubjectId = message.getSubject();
                        //User To Push
                        String userToPushWithRidesFormat = message.getTo().toString();
                        String userToPushForOpenfireUsage = userToPushWithRidesFormat;
                        userToPushForOpenfireUsage = userToPushForOpenfireUsage.substring(0, userToPushForOpenfireUsage.indexOf("@"));
                        userToPushForOpenfireUsage = userToPushForOpenfireUsage.replace("(at)", "@");
                        //From User
                        String fromUserWithRidesFormat = message.getFrom().toString();
                        String fromUserForOpenfireUsage = fromUserWithRidesFormat;
                        fromUserForOpenfireUsage = fromUserForOpenfireUsage.substring(0, fromUserForOpenfireUsage.indexOf("@"));
                        fromUserForOpenfireUsage = fromUserForOpenfireUsage.replace("(at)", "@");
                        StringBuilder stringBuilder = new StringBuilder(DEV_URL_FOR_DEVICE_INFO);

                        getMethod = new GetMethod(stringBuilder.append(userToPushForOpenfireUsage).toString());

                       //The actual push
                try {
                    java.io.InputStream inputStream = new FileInputStream(DEV_P12_LOC);

                    KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");
                    char password[] = "1234".toCharArray();
                    keyStore.load(inputStream, password);

                    httpClient.executeMethod(getMethod);
                    logger("Request URL = " + stringBuilder.toString());
                    logger("Response = " + getMethod.getResponseBodyAsString());



                    //Once execute we try to deserialize the obj
                    ArrayList<HashMap<String, String>> apnsEntities = new JSONDeserializer<ArrayList<HashMap<String, String>>>()
                            .deserialize(getMethod.getResponseBodyAsString());

//                    {"aps":
//                        {"alert": "Instant Message request from Pete Robinson",
//                                "instantMessage":{"name":"Pete Robinson",
//                                "id":501673,"jid":"dealer8(at)rides.ca@rides.openfire"}},
//                        "device_info": [ "devidcetype":"iPhone 4s",devidtoketn:"D71AFBAA2F1B3B75D9891258CEE45C12A33E74131B52CE4825EB5949A73D9740"]}

                    //"d71afbaa2f1b3b75d9891258cee45c12a33e74131b52ce4825eb5949a73d9740"
                    if(apnsEntities.size() != 0){
                        //At this point, we are sure that the user has iOS devices
                        //Try to get their display name
                        StringBuilder imRequestBuilder = new StringBuilder("Instant Message Request from ");
                        stringBuilder = new StringBuilder(DEV_URL_FOR_USER_DISPLAY_INFO);
                        getMethod = new GetMethod(stringBuilder.append(fromUserForOpenfireUsage).toString());
                        httpClient.executeMethod(getMethod);
                        logger("Request URL trying to get Dealer Display = " + stringBuilder.toString());
                        logger("Response = " + getMethod.getResponseBodyAsString());

                        String dealerDisplayName = getMethod.getResponseBodyAsString();
                        imRequestBuilder.append(dealerDisplayName);

                        for(int i = 0 ; i < apnsEntities.size(); i++){

                            //Custom payload
                            PushNotificationPayload pushNotificationPayload = PushNotificationPayload.complex();
                            pushNotificationPayload.addAlert(imRequestBuilder.toString());

                            Map<String, String> instantMsgUserInfoMap = new HashMap<String, String>();
                            instantMsgUserInfoMap.put("vehicleId", vehicleSubjectId);
                            instantMsgUserInfoMap.put("jid", fromUserForOpenfireUsage);
                            instantMsgUserInfoMap.put("name", dealerDisplayName);

                            List<Map<String, String>> instantMsgUserInfo = new ArrayList<Map<String, String>>();
                            instantMsgUserInfo.add(instantMsgUserInfoMap);
                            List<Map<String, String>> userDeviceInfo = new ArrayList<Map<String, String>>();

                            pushNotificationPayload.addCustomDictionary("instantMessage", instantMsgUserInfo);

                            Push.payload(pushNotificationPayload, keyStore, "1234", true, apnsEntities.get(i).get("devicetoken"));
                        }
                    }

                } catch (FileNotFoundException e) {
                    logger("Having issue Getting the P12 location");
                } catch (CertificateException e) {
                    logger("cant find the certificate");
                } catch (NoSuchAlgorithmException e) {
                    logger("cant find the algorithm");
                } catch (KeyStoreException e) {
                    logger("Can't find the KeyStore");
                } catch (IOException e) {
                    logger("Something is wrong with the IO");
                } catch (KeystoreException e) {
                    logger("Something is wrong with the Keystore in javaPNS");
                } catch (JSONException e) {
                    logger("Something is wrong with the JSON prep");
                }  catch (javapns.communication.exceptions.CommunicationException e) {
                    logger("Something is wrong with the communication");
                }



                logger("Thread : " + message.getThread());
                logger("XML format : " + message.toXML());
                logger("------------------END of Message Packet-------------------");
                }
            }
        logger("------------------PACKET-HIT-------------------");
        }
    }



    /**
     * class logger
     *
     * @param message message to log
     */
    private void logger(String message) {
        logger.info(message);
        System.out.println(message);
    }

}