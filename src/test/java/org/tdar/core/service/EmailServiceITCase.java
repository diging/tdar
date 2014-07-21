package org.tdar.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.mail.SimpleMailMessage;
import org.tdar.core.bean.AbstractIntegrationTestCase;
import org.tdar.core.bean.entity.Person;
import org.tdar.core.bean.notification.Email;
import org.tdar.core.service.external.MockMailSender;

public class EmailServiceITCase extends AbstractIntegrationTestCase {

    
    @Test
    public void testMockMailSender() {
        Person to = new Person(null, null, "toguy@tdar.info");
        String mailBody = "this is a message body";
        String subject = "this is a subject";
        Email email = new Email();
        email.addToAddress(to.getEmail());
        email.setMessage(mailBody);
        email.setSubject(subject);
        emailService.send(email);

        SimpleMailMessage received = checkMailAndGetLatest();

        assertEquals(received.getSubject(), subject);
        assertEquals(received.getText(), mailBody);
        assertEquals(received.getFrom(), emailService.getFromEmail());
        assertEquals(received.getTo()[0], to.getEmail());
    }


    @Test
    public void testSendTemplate() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "Hieronymous");
        map.put("bar", "Basho");
        Email email = new Email();
        email.addToAddress("toguy@tdar.info");
        email.setSubject("test");
        emailService.queueWithFreemarkerTemplate("test-email.ftl", map, email);
        sendEmailProcess.execute();
        assertTrue("expecting a mail in in the inbox", ((MockMailSender)emailService.getMailSender()).getMessages().size() > 0);

    }

}
