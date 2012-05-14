package nl.surfnet.bod.web.noc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import nl.surfnet.bod.service.PhysicalPortService;
import nl.surfnet.bod.service.ReservationService;
import nl.surfnet.bod.support.ModelStub;
import nl.surfnet.bod.support.ReservationFilterViewFactory;
import nl.surfnet.bod.support.RichUserDetailsFactory;
import nl.surfnet.bod.web.WebUtils;
import nl.surfnet.bod.web.security.RichUserDetails;
import nl.surfnet.bod.web.security.Security;
import nl.surfnet.bod.web.view.NocStatisticsView;
import nl.surfnet.bod.web.view.ReservationFilterView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RunWith(MockitoJUnitRunner.class)
public class DashboardControllerTest {

  @InjectMocks
  private DashboardController subject;

  @Mock
  private PhysicalPortService physicalPortServiceMock;

  @Mock
  private ReservationService reservationServiceMock;

  @Test
  public void shouldAddNullPrgToModel() {
    RichUserDetails user = new RichUserDetailsFactory().addUserRole().create();

    Security.setUserDetails(user);

    RedirectAttributes model = new ModelStub();

    String page = subject.index(model);

    assertThat(WebUtils.getAttributeFromModel("stats", model), notNullValue());
    assertThat(page, is("noc/index"));
  }

  @Test
  public void shouldAddStatisticsToModel() {
    ReservationFilterView elapsedFilter = new ReservationFilterViewFactory()
        .create(ReservationFilterViewFactory.ELAPSED);
    ReservationFilterView activeFilter = new ReservationFilterViewFactory().create(ReservationFilterViewFactory.ACTIVE);
    ReservationFilterView comingFilter = new ReservationFilterViewFactory().create(ReservationFilterViewFactory.COMING);

    RichUserDetails noc = new RichUserDetailsFactory().addNocRole().create();
    Security.setUserDetails(noc);
    Security.switchToNocEngineer();

    when(physicalPortServiceMock.countAllocated()).thenReturn(2L);
    when(reservationServiceMock.countAllEntriesUsingFilter(elapsedFilter)).thenReturn(3L);
    when(reservationServiceMock.countAllEntriesUsingFilter(activeFilter)).thenReturn(4L);
    when(reservationServiceMock.countAllEntriesUsingFilter(comingFilter)).thenReturn(5L);

    NocStatisticsView statistics = subject.determineStatistics();
    assertThat(statistics.getPhysicalPortsAmount(), is(2L));
    assertThat(statistics.getElapsedReservationsAmount(), is(3L));
    assertThat(statistics.getActiveReservationsAmount(), is(4L));
    assertThat(statistics.getComingReservationsAmount(), is(5L));
  }
}