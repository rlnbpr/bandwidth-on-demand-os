package nl.surfnet.bod.search;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;

import nl.surfnet.bod.util.BoDInitializer;
import nl.surfnet.bod.util.FullTextSearchContext;

public class AbstractIndexAndSearch<T> {

  protected Logger log = LoggerFactory.getLogger(getClass());

  private EntityManager entityManager;

  private EntityManagerFactory entityManagerFactory;

  private final Class<T> clazz;

  private final BoDInitializer boDInitializer = new BoDInitializer();

  public AbstractIndexAndSearch(final Class<T> clazz) {
    this.clazz = clazz;
  }

  protected void initEntityManager() {
    entityManagerFactory = Persistence.createEntityManagerFactory("hibernate-search-pu");
    entityManager = entityManagerFactory.createEntityManager();
  }

  protected void index() {
    boDInitializer.setEntityManager(entityManager);
    boDInitializer.init();
  }

  @SuppressWarnings("unchecked")
  protected List<T> getSearchQuery(String query) {
    return new FullTextSearchContext<T>(entityManager, clazz).getFullTextQueryForKeywordOnAllAnnotedFields(query,
        new Sort("id")).getResultList();
  }

  protected final void closeEntityManager() {
    entityManagerFactory.close();
  }

}
