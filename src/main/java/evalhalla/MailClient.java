package evalhalla;

import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

import mjson.Json;

public class MailClient
{
    private Properties configurationProperties;
    private Session session = null;
    private volatile boolean initialized = false;

    private static MailClient instance = new MailClient();
    
    private static Json config = evalhalla.package$.MODULE$.config();
    
    public static MailClient getInstance()
    {
        return instance;
    }

    private synchronized void ensureInit()
    {
        if (initialized)
            return;
        Authenticator authenticator = null;
        if (configurationProperties == null)
        {
            // throw new
            // RuntimeException("Attempt to use MailClient without configuration.");
            configurationProperties = new Properties();
            configurationProperties.put("mail.smtp.host",
                                        config.at("smtp-host").asString());
            if (config.has("smtp-port"))
                configurationProperties.setProperty("mail.smtp.port", config.at("smtp-port").asString());
            if (config.has("smtp-user"))
            {
                String username = config.at("smtp-user").asString();
                String pwd = "";
                if (config.has("smtp-password"))
                    pwd = config.at("smtp-password").asString();
                authenticator = new Authenticator(username, pwd);            
                configurationProperties.setProperty("mail.smtp.submitter",
                                                    authenticator.getPasswordAuthentication()
                                                            .getUserName());
                configurationProperties.setProperty("mail.smtp.auth", "true");
            }
        }
        session = Session.getDefaultInstance(configurationProperties,
                                             authenticator);
        initialized = true;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public void setInitialized(boolean initialized)
    {
        this.initialized = initialized;
    }

    public void sendEmail(String from, String to, String subject, String body)
    {
        ensureInit();
        try
        {
            if (from != null && to != null)
            {
                MimeMessage msg = new MimeMessage(session);
                InternetAddress sender = new InternetAddress(from);
                String[] tos = to.trim().split(";");
                InternetAddress[] recipients = new InternetAddress[tos.length];
                for (int i = 0; i < tos.length; i++)
                    recipients[i] = new InternetAddress(tos[i]);
                msg.setFrom(sender);
                msg.setRecipients(Message.RecipientType.TO, recipients);
                msg.setSentDate(new java.util.Date());
                msg.setSubject(subject);
                msg.setContent(body, "text/html");
                Transport.send(msg);
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public void sendEmail(String from, String to, String subject, Multipart body)
    {
        ensureInit();
        try
        {
            if (from != null && to != null)
            {
                MimeMessage msg = new MimeMessage(session);
                InternetAddress sender = new InternetAddress(from);
                String[] tos = to.trim().split(";");
                InternetAddress[] recipients = new InternetAddress[tos.length];
                for (int i = 0; i < tos.length; i++)
                    recipients[i] = new InternetAddress(tos[i]);
                msg.setFrom(sender);
                msg.setRecipients(Message.RecipientType.TO, recipients);
                msg.setSentDate(new java.util.Date());
                msg.setSubject(subject);
                msg.setContent(body);
                Transport.send(msg);
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public Properties getConfiguration()
    {
        return configurationProperties;
    }

    public void setConfiguration(Properties configurationProperties)
    {
        this.configurationProperties = configurationProperties;
    }

    private class Authenticator extends javax.mail.Authenticator
    {
        PasswordAuthentication authentication;

        public Authenticator(String username, String password)
        {
            authentication = new PasswordAuthentication(username, password);
        }

        protected PasswordAuthentication getPasswordAuthentication()
        {
            return authentication;
        }
    }
}
