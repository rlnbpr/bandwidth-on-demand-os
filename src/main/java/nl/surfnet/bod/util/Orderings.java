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
package nl.surfnet.bod.util;

import nl.surfnet.bod.domain.*;

import com.google.common.collect.Ordering;

public final class Orderings {

  private static final Ordering<VirtualPort> VP_ORDERING = new Ordering<VirtualPort>() {
    @Override
    public int compare(VirtualPort left, VirtualPort right) {
      return left.getUserLabel().compareTo(right.getUserLabel());
    }
  };

  private static final Ordering<VirtualResourceGroup> VRG_ORDERING = new Ordering<VirtualResourceGroup>() {
    @Override
    public int compare(VirtualResourceGroup left, VirtualResourceGroup right) {
      return left.getName().compareTo(right.getName());
    }
  };

  private static final Ordering<PhysicalResourceGroup> PRG_ORDERING = new Ordering<PhysicalResourceGroup>() {
    @Override
    public int compare(PhysicalResourceGroup left, PhysicalResourceGroup right) {
      return left.getName().compareTo(right.getName());
    }
  };

  private static final Ordering<BodRole> ROLE_ORDERING = new Ordering<BodRole>() {
    @Override
    public int compare(BodRole role1, BodRole role2) {
      return (role1.getRole().getSortOrder() + role1.getInstituteName().or("")).compareTo(role2.getRole().getSortOrder()
          + role2.getInstituteName().or(""));
    }
  };

  private static final Ordering<VirtualPortRequestLink> VP_REQUEST_LINK_ORDERING = new Ordering<VirtualPortRequestLink>() {
    @Override
    public int compare(VirtualPortRequestLink left, VirtualPortRequestLink right) {
      return left.getRequestDateTime().compareTo(right.getRequestDateTime());
    }
  };

  private Orderings() {
  }

  public static Ordering<VirtualPort> vpUserLabelOrdering() {
    return VP_ORDERING;
  }

  public static Ordering<VirtualResourceGroup> vrgNameOrdering() {
    return VRG_ORDERING;
  }

  public static Ordering<PhysicalResourceGroup> prgNameOrdering() {
    return PRG_ORDERING;
  }

  public static Ordering<BodRole> bodRoleOrdering() {
    return ROLE_ORDERING;
  }

  public static Ordering<VirtualPortRequestLink> vpRequestLinkOrdering() {
    return VP_REQUEST_LINK_ORDERING;
  }
}
