package nl.surfnet.bod.nbi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import javax.xml.bind.JAXBException;

import nl.surfnet.bod.nbi.generated.TerminationPoint;

import org.junit.Before;
import org.junit.Test;

public class NbiOfflineClientTest {

  private NbiOfflineClient subject = new NbiOfflineClient();

  @Before
  public void initOfflineClient() throws JAXBException {
    subject = new NbiOfflineClient();
    subject.init();
  }

  @Test
  public void shouldGivePortsBackWithDummyInName() {
    List<TerminationPoint> allPorts = subject.findAllPorts();

    assertThat(allPorts, hasSize(greaterThan(0)));

    assertThat(allPorts.get(0).getPortDetail().getName(), endsWith("_dummy"));
  }

}
