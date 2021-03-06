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
package nl.surfnet.bod.web.appmanager;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.List;

import nl.surfnet.bod.nbi.mtosi.NotificationConsumerHttp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.tmforum.mtop.fmw.xsd.hbt.v1.HeartbeatType;
import org.tmforum.mtop.nra.xsd.alm.v1.AlarmType;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class MtosiNotificationsControllerTest {

  @InjectMocks
  private MtosiNotificationsController subject;

  @Mock
  private NotificationConsumerHttp notificationConsumerHttpMock;

  private MockMvc mockMvc;

  @Before
  public void setup() {
    mockMvc = standaloneSetup(subject).build();
  }

  @Test
  public void notificationsShouldAddAlamsAndHeartbeatsToModel() throws Exception {
    when(notificationConsumerHttpMock.getAlarms()).thenReturn(Lists.newArrayList(new AlarmType()));
    when(notificationConsumerHttpMock.getHeartbeats()).thenReturn(Lists.newArrayList(new HeartbeatType(), new HeartbeatType()));

    mockMvc.perform(get("/appmanager/mtosi/notifications"))
      .andExpect(status().isOk())
      .andExpect(model().attribute("alarms", hasSize(1)))
      .andExpect(model().attribute("heartbeats", hasSize(2)));
  }

  @Test
  public void indexPageShouldBeOk() throws Exception {
    mockMvc.perform(get("/appmanager/mtosi"))
      .andExpect(status().isOk());
  }

  @Test
  public void lastShouldReturnLastFewElements() {
    List<String> input = Lists.newArrayList("one", "two", "three", "four");

    List<String> output = subject.last(input, 2);

    assertThat(output, contains("three", "four"));
  }

  @Test
  public void lastShouldReturnTheWholeList() {
    List<String> input = Lists.newArrayList("one", "two");

    List<String> output = subject.last(input, 20);

    assertThat(output, contains("one", "two"));
  }
}
