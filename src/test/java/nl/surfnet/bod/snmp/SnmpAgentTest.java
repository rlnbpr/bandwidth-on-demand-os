package nl.surfnet.bod.snmp;

import static nl.surfnet.bod.snmp.SnmpAgent.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

public class SnmpAgentTest {

  private final SnmpOfflineManager snmpOfflineManager = new SnmpOfflineManager();
  private final SnmpAgent snmpAgent = new SnmpAgent();
  private final PDU pdu = new PDU();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    snmpOfflineManager.startup();
    // need to specify the system up time
    pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
    pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(DEFAULT_OID)));
    pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(DEFAULT_IP_ADDRESS)));
    pdu.add(new VariableBinding(new OID(DEFAULT_OID), new OctetString("Major")));
    pdu.setType(PDU.NOTIFICATION);
  }

  @After
  public void tearDown() throws Exception {
    snmpOfflineManager.shutdown();
  }

  @Test
  public void should_send_and_receive_pdu() {
    snmpAgent.sendTrap(pdu);
    final PDU lastTrap = snmpOfflineManager.getOrWaitForLastTrap(10);
    assertThat(lastTrap.getType(), is(PDU.TRAP));
    final String lastVariableBindingsAsString = lastTrap.getVariableBindings().toString();
    assertThat(lastVariableBindingsAsString, containsString("1.3.6.1.2.1.1.3.0"));
    assertThat(
        lastVariableBindingsAsString,
        containsString("1.3.6.1.6.3.1.1.4.1.0 = 1.3.6.1.2.1.1.8, 1.3.6.1.6.3.18.1.3.0 = 127.0.0.1, 1.3.6.1.2.1.1.8 = Major"));
    assertThat(lastVariableBindingsAsString, containsString(SnmpAgent.DEFAULT_OID.replaceFirst(".", "")));
  }

}
