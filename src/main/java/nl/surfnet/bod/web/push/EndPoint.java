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
package nl.surfnet.bod.web.push;

import java.io.IOException;

import nl.surfnet.bod.web.security.RichUserDetails;

import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndPoint implements WebSocket.OnTextMessage {

  private final Logger logger = LoggerFactory.getLogger(EndPoint.class);

  private final EndPoints endPoints;
  private final RichUserDetails user;

  private Connection connection;

  public EndPoint(RichUserDetails user, EndPoints endPoints) {
    this.endPoints = endPoints;
    this.user = user;
  }

  @Override
  public void onOpen(Connection con) {
    this.connection = con;
    endPoints.add(this);
  }

  @Override
  public void onClose(int closeCode, String message) {
    endPoints.remove(this);
  }

  @Override
  public void onMessage(String data) {
  }

  public RichUserDetails getUser() {
    return user;
  }

  public void sendMessage(String message) {
    try {
      connection.sendMessage(message);
    }
    catch (IOException e) {
      logger.warn("Could not send a message over websocket to client", e);
    }
  }

}
