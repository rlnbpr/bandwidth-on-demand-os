/**
 * The owner of the original code is SURFnet BV.
 *
 * Portions created by the original owner are Copyright (C) 2011-2012 the
 * original owner. All Rights Reserved.
 *
 * Portions created by other contributors are Copyright (C) the contributor.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   (Contributors insert name & email here)
 *
 * This file is part of the SURFnet7 Bandwidth on Demand software.
 *
 * The SURFnet7 Bandwidth on Demand software is free software: you can
 * redistribute it and/or modify it under the terms of the BSD license
 * included with this distribution.
 *
 * If the BSD license cannot be found with this distribution, it is available
 * at the following location <http://www.opensource.org/licenses/BSD-3-Clause>
 */
package nl.surfnet.bod.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nl.surfnet.bod.domain.UserGroup;
import nl.surfnet.bod.util.Environment;

import org.opensocial.Client;
import org.opensocial.RequestException;
import org.opensocial.auth.AuthScheme;
import org.opensocial.auth.OAuth2LeggedScheme;
import org.opensocial.models.Group;
import org.opensocial.providers.Provider;
import org.opensocial.providers.ShindigProvider;
import org.opensocial.services.GroupsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

@Service
public class GroupOpenSocialService implements GroupService {

  private Logger logger = LoggerFactory.getLogger(GroupOpenSocialService.class);

  @Autowired
  private Environment env;

  @Override
  public Collection<UserGroup> getGroups(String nameId) {
    try {
      List<Group> osGroups = getClient(nameId).send(GroupsService.getGroups()).getEntries();

      return Lists.newArrayList(Lists.transform(osGroups, new Function<Group, UserGroup>() {
        @Override
        public UserGroup apply(Group input) {
          return new UserGroup(input.getId(), input.getTitle(), input.getDescription());
        }
      }));
    }
    catch (RequestException e) {
      logger.error("Could not retreive groups from open social server", e);
      return Collections.emptyList();
    }
    catch (IOException e) {
      logger.error("Could not retreive groups from open social server", e);
      return Collections.emptyList();
    }
  }

  private Client getClient(String loggedInUser) {
    Provider provider = new ShindigProvider(true);

    provider.setRestEndpoint(env.getOpenSocialUrl() + "/rest/");
    provider.setRpcEndpoint(null);
    provider.setVersion("0.9");

    AuthScheme scheme = new OAuth2LeggedScheme(env.getOpenSocialOAuthKey(), env.getOpenSocialOAuthSecret(),
        loggedInUser);

    return new Client(provider, scheme);
  }

  protected void setEnvironment(Environment environment) {
    this.env = environment;
  }
}
