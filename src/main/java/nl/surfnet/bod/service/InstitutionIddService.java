package nl.surfnet.bod.service;

import java.util.Collection;
import java.util.List;

import nl.surfnet.bod.domain.Institution;
import nl.surfnet.bod.extern.IddClient;
import nl.surfnet.bod.idd.Klanten;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Service
public class InstitutionIddService implements InstitutionService {

    private IddClient iddClient;

    @Autowired
    public InstitutionIddService(@Qualifier("iddStaticClient") IddClient iddClient) {
        this.iddClient = iddClient;
    }

    @Override
    public Collection<Institution> getInstitutions() {
        Collection<Klanten> klanten = iddClient.getKlanten();

        return toInstitutions(klanten);
    }

    private Collection<Institution> toInstitutions(Collection<Klanten> klantnamen) {
        List<Institution> institutions = Lists.newArrayList();
        for (Klanten klant : klantnamen) {
            String klantnaam = klant.getKlantnaam().trim();
            if (Strings.isNullOrEmpty(klantnaam)) {
                continue;
            }
            institutions.add(new Institution(klantnaam));
        }

        return institutions;
    }
}
