package nl.surfnet.bod.repo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import nl.surfnet.bod.domain.Reservation;

@Repository
public class CustomReservationRepo {

  @SuppressWarnings("unused")
  private final Logger log = LoggerFactory.getLogger(getClass());

  @PersistenceContext
  private EntityManager entityManager;

  private static final Function<Tuple, Long> FROM_TUPLE_TO_LONG = new Function<Tuple, Long>() {
    @Override
    public Long apply(final Tuple reservation) {
      return (Long) reservation.get(0);
    }
  };

  public List<Long> findIdsWithWhereClause(final Specification<Reservation> whereClause) {
    final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    final CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
    final Root<Reservation> root = criteriaQuery.from(Reservation.class);

    criteriaQuery.select(criteriaBuilder.tuple(root.get("id"))).where(
        whereClause.toPredicate(root, criteriaQuery, criteriaBuilder));

    final List<Tuple> results = entityManager.createQuery(criteriaQuery).getResultList();
    return new ArrayList<Long>(Collections2.transform(results, FROM_TUPLE_TO_LONG));
  }
}