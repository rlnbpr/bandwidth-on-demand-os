package nl.surfnet.bod.service;

import java.util.List;

import nl.surfnet.bod.domain.PhysicalPort;
import nl.surfnet.bod.repo.PhysicalPortRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("physicalPortServiceRepoImpl")
@Transactional
public class PhysicalPortServiceRepoImpl implements PhysicalPortService {

    @Autowired
    private PhysicalPortRepo physicalPortRepo;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PhysicalPort> findAll() {
        return physicalPortRepo.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PhysicalPort> findEntries(final int firstResult, final int sizeNo) {
        return findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() {
        return physicalPortRepo.count();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final PhysicalPort physicalPort) {
        physicalPortRepo.delete(physicalPort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PhysicalPort find(final Long id) {
        return physicalPortRepo.findOne(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final PhysicalPort physicalPort) {
        physicalPortRepo.save(physicalPort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PhysicalPort update(final PhysicalPort physicalPort) {
        return physicalPortRepo.save(physicalPort);
    }

    @Override
    public PhysicalPort find(final String portId) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
