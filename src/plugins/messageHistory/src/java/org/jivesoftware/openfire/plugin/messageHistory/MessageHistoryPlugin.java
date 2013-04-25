package org.jivesoftware.openfire.plugin.messageHistory;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.plugin.messageHistory.interceptor.MessageInterceptor;
import org.jivesoftware.openfire.plugin.messageHistory.util.DbUtil;
import org.slf4j.LoggerFactory;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.JID;

import java.io.File;

/**
 * Class MessageHistoryPlugin provides custom function to intercept incoming packets and insert them into the db
 */
public class MessageHistoryPlugin implements Plugin {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MessageHistoryPlugin.class);
    //Private members
    private XMPPServer server;
    private JID serverAddress;

    /**
     * Plugin initializer
     */
    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        logger("Starting Message History Handler");

        server = XMPPServer.getInstance();
        serverAddress = new JID(server.getServerInfo().getXMPPDomain());
        logger("The server address is @ = " + serverAddress.getDomain());
        //add the interceptor
        InterceptorManager.getInstance().addInterceptor(new MessageInterceptor());
        logger(DbUtil.retriveMessageOnSubjectForiOS("dealer2(at)rides.ca@rides.openfire", "john.iphone@rides.openfire", "31044").toString());
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

    public void destroyPlugin() {
    }


}
