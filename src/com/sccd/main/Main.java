package com.sccd.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;

public class Main {

	public static void main(String[] args) {
		Properties props = new Properties();
		
        try{
        	props = new Properties();
            InputStream in = new FileInputStream(new File(ClassLoader.getSystemResource("").getPath() + "properties.properties"));
        	props.load(in);
        	in.close();
        	
        	File dir = new File(props.getProperty("in.dir"));
    		
    		SpeechToText service = new SpeechToText();
    		service.setUsernameAndPassword(props.getProperty("watson.speech2text.username"), props.getProperty("watson.speech2text.password"));
    		RecognizeOptions options = new RecognizeOptions.Builder()
    				.contentType("audio/wav")
    				.model("pt-BR_NarrowbandModel")
    				.timestamps(true).build();
    		
    		for (File file : dir.listFiles()) {
    			System.out.println("Identificando audio do arquivo " + file.getName());
    			SpeechResults results = service.recognize(file, options).execute();
    			//System.out.println(results);
    			String audioTranscription = results.getResults().get(0).getAlternatives().get(0).getTranscript();
    			
    			if (results.getResults().get(0).getAlternatives().get(0).getConfidence() >= 0.60) {
    				System.out.println("E-mail enviado.");
    				System.out.println("Transcrição do áudio: " + audioTranscription);
    				//sendMail(audioTranscription);
    				sendMail(props.getProperty("mail.server.sender"),
    						props.getProperty("mail.server.receiver"),
    						"Abertura de chamado via URA",
    						audioTranscription,
    						props.getProperty("mail.server.host"),
    						props.getProperty("mail.server.port"));
    				moveFile(file, props.getProperty("out.dir"));
    			} else {
    				System.out.println("E-mail não enviado devido grau de confiança do reconhecimento estar abaixo de 60%.");
    				System.out.println("Transcrição do áudio: " + audioTranscription);
    				moveFile(file, props.getProperty("error.dir"));
    			}
    		}
    		
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
		} catch (IOException e) {
        	e.printStackTrace();
        }
	}
	
	private static void sendMail(String from, String to, String subject, String desc, String mailServer, String mailPort) {
		Properties props = new Properties();
        
        props.put("mail.smtp.host", mailServer);
        props.put("mail.smtp.port", mailPort); //port 25 or 2525 or 587 - Secure SMTP (SSL / TLS) - port 465 or 25 or 587, 2526 (Elastic Email)

        Session session = Session.getDefaultInstance(props, null);
        
        session.setDebug(false);
        
        try {

			Message message = new MimeMessage(session);

			Address[] toUser = InternetAddress.parse(to); 
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, toUser);
			message.setSubject(subject);
			message.setText(desc);
			
			Transport.send(message);
			
        } catch (MessagingException e) {
        	throw new RuntimeException(e);
        }
        
	}
	
	private static void moveFile(File file, String dir) {
		try {
			InputStream in = new FileInputStream(file);
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss"); 
			Date date = new Date();  
			
			File fileDir = new File(dir);
			
			if (!fileDir.exists()) {
				fileDir.mkdir();
			}
			
			OutputStream out = new FileOutputStream(dir + dateFormat.format(date) + "-" + file.getName());
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			
			in.close();
			out.close();
			file.delete();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
