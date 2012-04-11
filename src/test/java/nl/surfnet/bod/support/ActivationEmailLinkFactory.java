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
package nl.surfnet.bod.support;

import java.util.concurrent.atomic.AtomicLong;

import nl.surfnet.bod.domain.ActivationEmailLink;
import nl.surfnet.bod.domain.PhysicalResourceGroup;

public class ActivationEmailLinkFactory<T> {

  private static final AtomicLong COUNTER = new AtomicLong();
  private Long id = COUNTER.incrementAndGet();

  private PhysicalResourceGroup physicalResourceGroup = new PhysicalResourceGroupFactory().create();
  private boolean activate = false;
  private boolean emailSent = true;

  public ActivationEmailLink<T> create() {
    ActivationEmailLink<T> link = new ActivationEmailLink<T>(physicalResourceGroup);
    link.setId(id);

    if (activate) {
      link.activate();
    }

    if (emailSent) {
      link.emailWasSent();
    }

    return link;
  }

  public ActivationEmailLinkFactory<T> setActivate(boolean active) {
    this.activate = active;
    return this;
  }

  public ActivationEmailLinkFactory<T> setEmailSent(boolean sent) {
    this.emailSent = sent;
    return this;
  }

  public ActivationEmailLinkFactory<T> setPhysicalResourceGroup(PhysicalResourceGroup prg) {
    this.physicalResourceGroup = prg;
    return this;
  }

}
