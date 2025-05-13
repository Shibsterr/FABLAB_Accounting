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

// Klase, kas atbild par e-pasta ziņojumu sūtīšanu, izmantojot Gmail SMTP
public class EmailSender {
    private String email;    // Sūtītāja e-pasta adrese
    private String password; // Sūtītāja paroles mainīgais (netiek izmantots tieši, parole ir hardcodēta iekšā)

    // Konstruktors, kas inicializē e-pasta adresi un paroli
    public EmailSender(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Publiskā metode e-pasta nosūtīšanai vairākiem saņēmējiem
    public void sendEmail(List<String> recipients, String subject, String message) {
        new SendEmailTask().execute(recipients, subject, message);
    }

    // Iekšēja klase, kas veic e-pasta sūtīšanu fonā (AsyncTask)
    private class SendEmailTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            // Izņem parametrus
            List<String> recipients = (List<String>) params[0];
            String subject = (String) params[1];
            String message = (String) params[2];

            // Nosūta e-pastu katram saņēmējam atsevišķi
            for (String recipient : recipients) {
                try {
                    sendEmail(recipient, subject, message);
                } catch (MessagingException e) {
                    Log.e("EmailSender", "Kļūda sūtot e-pastu: " + e.getMessage());
                }
            }
            return null;
        }

        // Metode, kas sagatavo un nosūta e-pastu konkrētam saņēmējam
        private void sendEmail(String recipient, String subject, String message) throws MessagingException {
            // SMTP konfigurācijas uzstādījumi priekš Gmail
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            // Izveido sesiju ar autentifikāciju (norāda e-pastu un paroli)
            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    // Šeit ir fiksēts noreply konta e-pasts un lietotnes parole.
                    return new PasswordAuthentication("fablabappnoreply@gmail.com", "xllk wqet dulg xabp");
                }
            });

            // Izveido e-pasta ziņojumu
            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(email)); // No: adrese (dinamiski padota konstrukturā)
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient)); // Kam:
            mimeMessage.setSubject(subject); // Tēma
            mimeMessage.setText(message);    // Ziņojuma saturs

            // Nosūta ziņojumu
            Transport.send(mimeMessage);
        }
    }
}
