package com.azad.yearn.deployer.action;

import com.azad.yearn.deployer.model.XSource_Content;
import com.azad.yearn.deployer.utils.AppProperties;
import com.azad.yearn.deployer.utils.TwitterOauthHeaderGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

public class EmailSender implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private AppProperties applicationProperties;
    private Collection<XSource_Content> data;
    private Queue<String> fifo;
   // String[] email = {"muhdarif93@gmail.com", "hazrid93@hotmail.com", "Ahmadiwan14@gmail.com"};
    public EmailSender(Collection<XSource_Content> data, Queue<String> fifo , AppProperties applicationProperties) {
        this.data = data;
        this.fifo = fifo;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void run() {
        for (XSource_Content innerData: data) {
            for(int size = 0;size<1; size ++){
              //  log.info(Thread.currentThread().getName() + ", executing run() method!" + innerData.getResult().get(size).toString());
                Map<String,String> data = innerData.getResult().get(size);
                // Check for contract creation
                if(!fifo.contains(data.get("hash"))){
                     boolean boolSendDataCreation = decidingLogicContractCreation(data);
                     boolean boolSendOther = decidingLogicOther(data);
                     if(boolSendDataCreation){
                         sendTweet(data, "$YFI #yearnfinance #YFI @iearnfinance \n ### Possible contract creation event by Yearn deployer at: https://etherscan.io/tx/" + data.get("hash"));
                        // sendEmail(data); *require a separate fifo handling if this is enable
                     }
                    if(boolSendOther){
                        sendTweet(data, "$YFI #yearnfinance #YFI @iearnfinance \n ### Token transferred event to/from Yearn deployer at: https://etherscan.io/tx/" + data.get("hash"));
                        // sendEmail(data); *require a separate fifo handling if this is enable
                    }
                } else {
                    //skip
                }
            }
        }
    }

    private boolean decidingLogicContractCreation(Map<String,String> data){
        boolean decider = false;
        if (data.get("from").equalsIgnoreCase("0x2d407ddb06311396fe14d4b49da5f0471447d45c")
                && !data.get("contractAddress").isEmpty() && data.get("to").isEmpty()) {
            // log.info( data.get("from") );
            decider = true;
        }
        return decider;
    }

    private boolean decidingLogicOther(Map<String,String> data){
        boolean decider = false;
        if (data.get("contractAddress").isEmpty() && !data.get("to").isEmpty()) {
            // log.info( data.get("from") );
            decider = true;
        }
        return decider;
    }
    private void sendTweet(Map<String,String> data,String content){
        AppProperties.Twitter twitterProperties = applicationProperties.getTwitter();
        /*
        AppProperties.Etherscan etherscanProperties = applicationProperties.getEtherscan();
        String etherApiKey = etherscanProperties.getApikey();
        String etherDeployerId = etherscanProperties.getDeployer(); */
        String consumerKey = twitterProperties.getConsumerKey();
        String consumerSecret = twitterProperties.getConsumerSecret();
        String token = twitterProperties.getAccessToken();
        String tokenSecret = twitterProperties.getTokenSecret();

        try {
            TwitterOauthHeaderGenerator generator = new TwitterOauthHeaderGenerator(consumerKey,
                    consumerSecret,
                    token,
                    tokenSecret);

            String header = generator.generateHeader("POST", "https://api.twitter.com/1.1/statuses/update.json", null);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", header);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
            map.add("status", content);
            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<MultiValueMap<String, String>>(map, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> stringEntity= restTemplate.postForEntity("https://api.twitter.com/1.1/statuses/update.json", httpEntity, String.class);
            log.info("Request Sent Successfully: " + stringEntity.getBody());
            fifo.add(data.get("hash"));
        } catch (Exception e) {
            log.error("Error " + e);
        }
    }

    private void sendEmail(Map<String,String> data){
        // Recipient's email ID needs to be mentioned.
        try {
            final Address[] addresses = {new InternetAddress("example@hotmail.com")}; // recipient list
            final String fromEmail = "example@hotmail.com"; // the email that we use to send
            final String password = "<password>"; // correct password for gmail id

            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp-mail.outlook.com"); //SMTP Host
            props.put("mail.smtp.port", "587"); //TLS Port
            props.put("mail.smtp.auth", "true"); //enable authentication
            props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS

            //create Authenticator object to pass in Session.getInstance argument
            Authenticator auth = new Authenticator() {
                //override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            };
            Session session = Session.getInstance(props, auth);
            emailUtil(session, addresses,"[IMPORTANT] Yearn Deployer Notification", "Possible contract creation transaction at : https://etherscan.io/tx/" + data.get("hash"));
            fifo.add(data.get("hash"));
        } catch (Exception e) {
            log.error("Error " + e);
        }
    }

    private void emailUtil(Session session, Address[] toEmail, String subject, String body){
        try
        {
            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");
            msg.setFrom(new InternetAddress("example@hotmail.com"));
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO,toEmail);
            log.info("Message is ready");
            Transport.send(msg);
            log.info("Email Sent Successfully!!");
        }
        catch (Exception e) {
            log.error("Error " + e);
        }
    }
}
