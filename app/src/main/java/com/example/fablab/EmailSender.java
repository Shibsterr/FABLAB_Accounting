package com.example.fablab;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
    private String email;
    private String password;

    public EmailSender(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void sendEmail(List<String> recipients, String subject, String message) {
        new SendEmailTask().execute(recipients, subject, message);
    }

    private class SendEmailTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            List<String> recipients = (List<String>) params[0];
            String subject = (String) params[1];
            String message = (String) params[2];

            for (String recipient : recipients) {
                try {
                    sendEmail(recipient, subject, message);
                } catch (MessagingException e) {
                    Log.e("EmailSender", "Error sending email: " + e.getMessage());
                }
            }
            return null;
        }

        private void sendEmail(String recipient, String subject, String message) throws MessagingException {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("fablabappnoreply@gmail.com", "xllk wqet dulg xabp");
                }
            });

            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(email));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);
            Transport.send(mimeMessage);
        }
    }
}
