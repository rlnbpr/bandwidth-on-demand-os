/**
 * Copyright (c) 2012, SURFnet BV
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *     disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the SURFnet BV nor the names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.surfnet.bod.nbi.mtosi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.StringUtils;
import org.tmforum.mtop.fmw.xsd.nam.v1.RelativeDistinguishNameType;
import org.tmforum.mtop.sb.xsd.svc.v1.ServiceCharacteristicValueType;

public final class MtosiUtils {

  private static final String PTP_FORMAT = "/rack=%s/shelf=%s/slot=%s/port=%s";
  private static final String PTP_WITH_SUB_SLOT_FORMAT = "/rack=%s/shelf=%s/slot=%s/sub_slot=%s/port=%s";

  private MtosiUtils() {
  }

  public static String physicalTerminationPointToNmsPortId(String ptp) {
    return ptp.replace("rack=", "").replace("shelf=", "").replace("sub_slot=", "").replace("slot=", "")
        .replace("port=", "").replaceFirst("/", "").replaceAll("/", "-");
  }

  public static String nmsPortIdToPhysicalTerminationPoint(String nmsPortId) {
    Object[] parts = nmsPortId.split("-");
    if (parts.length == 4) {
      return String.format(PTP_FORMAT, parts);
    }
    else if (parts.length == 5) {
      return String.format(PTP_WITH_SUB_SLOT_FORMAT, parts);
    }
    else {
      throw new IllegalArgumentException("The nmsPortId can not be converted to a ptp");
    }
  }

  public static void printDescribedByList(List<ServiceCharacteristicValueType> describedByList) {

    Iterator<ServiceCharacteristicValueType> iterator = describedByList.iterator();
    while (iterator.hasNext()) {
      ServiceCharacteristicValueType type = iterator.next();
      System.err
          .println(String.format("Type: %s, SscRef %s", getRdnString(type.getSscRef().getRdn()), type.getValue()));
    }
  }

  public static String getRdnString(List<RelativeDistinguishNameType> rdn) {
    List<String> contents = new ArrayList<>();
    Iterator<RelativeDistinguishNameType> iterator = rdn.iterator();
    while (iterator.hasNext()) {
      RelativeDistinguishNameType item = iterator.next();
      contents.add(item.getType() + " " + item.getValue());
    }
    return StringUtils.collectionToCommaDelimitedString(contents);
  }

}
