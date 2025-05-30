package org.prac.korailreserve;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        // If you want to set a 'from' address different from the username in properties
        // message.setFrom("your_sender_name@example.com");

        try {
            mailSender.send(message);
            System.out.println("Email sent successfully to " + to);
        } catch (MailException e) {
            System.err.println("Error sending email: " + e.getMessage());
            // Log the exception properly in a real application
        }
    }

    // You can add more complex methods here for HTML emails, attachments, etc.
    /*
    public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // true for multipart, UTF-8 for encoding
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true for HTML content
        // helper.addAttachment("attachment.pdf", new FileSystemResource("path/to/attachment.pdf"));

        try {
            mailSender.send(message);
            System.out.println("HTML Email sent successfully to " + to);
        } catch (MailException e) {
            System.err.println("Error sending HTML email: " + e.getMessage());
            // Log the exception properly in a real application
        }
    }
    */
}