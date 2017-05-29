package com.sccd.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.PasswordAuthentication;
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
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MoveAction;

import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;

public class Main {

	public static void main(String[] args) {
		File dir = new File("C:\\SCCD\\in\\");
		
		SpeechToText service = new SpeechToText();
		service.setUsernameAndPassword("d8b1177e-85fb-40dd-8be5-b39dca3d2864", "g2nYKtmFaIIn");
		RecognizeOptions options = new RecognizeOptions.Builder()
				.contentType("audio/wav")
				.model("pt-BR_BroadbandModel")
				.timestamps(true).build();
		
		for (File file : dir.listFiles()) {
			System.out.println("Identificando audio do arquivo " + file.getName());
			SpeechResults results = service.recognize(file, options).execute();
			//System.out.println(results);
			
			if (results.getResults().get(0).getAlternatives().get(0).getConfidence() >= 0.80) {
				String audioTranscription = results.getResults().get(0).getAlternatives().get(0).getTranscript();
				System.out.println("TRANSCRICAO DO AUDIO: " + audioTranscription);
				sendMail(audioTranscription);
				moveFile(file, "C:\\SCCD\\out\\");
			} else {
				System.out.println("Não foi possível identificar a fala do áudio.");
				moveFile(file, "C:\\SCCD\\error\\");
			}
		}
	}
	
	private static void sendMail(String desc) {
		Properties props = new Properties();
        
        props.put("mail.smtp.host", "192.168.2.19");
        //props.put("mail.smtp.socketFactory.port", "465");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "25"); //port 25 or 2525 or 587 - Secure SMTP (SSL / TLS) - port 465 or 25 or 587, 2526 (Elastic Email)

        Session session = Session.getDefaultInstance(props, null);
        
        session.setDebug(true);
        
        try {

			Message message = new MimeMessage(session);

			Address[] toUser = InternetAddress.parse("msollitari@magnasistemas.com.br"); 
			message.setFrom(new InternetAddress("olima@magnasistemas.com.br"));
			message.setRecipients(Message.RecipientType.TO, toUser);
			message.setSubject("Chamado");
			message.setText(desc);
			
			Transport.send(message);
			System.out.println("Feito!!!");
			
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
