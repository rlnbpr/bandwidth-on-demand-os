package nl.surfnet.bod.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import nl.surfnet.bod.domain.VirtualPortRequestLink;
import nl.surfnet.bod.support.RichUserDetailsFactory;
import nl.surfnet.bod.support.VirtualPortRequestLinkFactory;
import nl.surfnet.bod.web.security.RichUserDetails;

import org.junit.Test;

public class EmailsTest {

  @Test
  public void requestVirtualPortMailWithEmail() {
    RichUserDetails from = new RichUserDetailsFactory().setDisplayname("Henk").setEmail("henk@henk.nl").create();
    VirtualPortRequestLink requestLink = new VirtualPortRequestLinkFactory().create();
    String link = "http://localhost";

    String body = Emails.VirtualPortRequestMail.body(from, requestLink, link);

    assertThat(body, containsString("From: Henk (henk@henk.nl)"));
  }

  @Test
  public void requestVirtualPortMailWithoutEmail() {
    RichUserDetails from = new RichUserDetailsFactory().setDisplayname("Henk").setEmail(null).create();
    VirtualPortRequestLink requestLink = new VirtualPortRequestLinkFactory().create();
    String link = "http://localhost";

    String body = Emails.VirtualPortRequestMail.body(from, requestLink, link);

    assertThat(body, containsString("From: Henk (Unknown Email)"));
  }


  @Test
  public void errorMailWithEmail() {
    RichUserDetails user = new RichUserDetailsFactory().setDisplayname("Henk").setEmail("henk@henk.nl").create();
    RuntimeException throwable = new RuntimeException();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString()).thenReturn("");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/wrong"));

    String body = Emails.ErrorMail.body(user, throwable, request);

    assertThat(body, containsString("User: Henk (henk@henk.nl)"));
  }

  @Test
  public void errorMailWithoutEmail() {
    RichUserDetails user = new RichUserDetailsFactory().setDisplayname("Henk").setEmail(null).create();
    RuntimeException throwable = new RuntimeException();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getQueryString()).thenReturn("");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/wrong"));

    String body = Emails.ErrorMail.body(user, throwable, request);

    assertThat(body, containsString("User: Henk (Email not known)"));
  }
}
