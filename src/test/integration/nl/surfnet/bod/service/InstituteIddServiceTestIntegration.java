package nl.surfnet.bod.service;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import nl.surfnet.bod.domain.Institute;

import org.hibernate.internal.SessionImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring/appCtx.xml", "/spring/appCtx-jpa-test.xml", "/spring/appCtx-nbi-client.xml",
    "/spring/appCtx-idd-client.xml" })
@Transactional
public class InstituteIddServiceTestIntegration {

  @PersistenceContext
  private EntityManager em;

  @Autowired
  private InstituteIddService instituteService;

  @Test
  public void shouldInsertInstitutes() {
    instituteService.refreshInstitutes();

    assertThat(instituteService.findAll(), hasSize(218));
  }

  @Test
  public void shouldUpdateInstitutesThisIsWhyTheVersionAttributeIsRemovedInInstitute() {
    instituteService.refreshInstitutes();
    Collection<Institute> institutes = instituteService.findAll();

    for (Institute institute : instituteService.findAll()) {
      ((SessionImpl) em.getDelegate()).evict(institute);
    }

    instituteService.refreshInstitutes();
    Collection<Institute> foundInstitutes = instituteService.findAll();

    // Size should be the same, not doubled or so due to additional inserts
    assertThat(foundInstitutes.size(), is(institutes.size()));
  }

}
