/**
 * Copyright 2011-2012 @WalmartLabs, a division of Wal-Mart Stores, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.walmartlabs.mupd8

import scala.actors.Actor
import grizzled.slf4j.Logging

class HeartBeat(appRuntime: AppRuntime) extends Actor with Logging {
  private var keepRunning = true
  private val HEARTBEAT_INTERVAL = 2

  def act {
    while (keepRunning) {
      if (appRuntime.ring != null && appRuntime.messageServerHost != null) {
        // ping message server
        val lmsClient = new LocalMessageServerClient(appRuntime.messageServerHost, appRuntime.messageServerPort + 1)
        if (lmsClient.sendMessage(PING())) {
          // if ping okay, calculate sleep time and sleep
          val sleepTime = appRuntime.rand.nextInt(HEARTBEAT_INTERVAL * appRuntime.ring.ips.size)
          Thread.sleep(sleepTime)
        } else {
          error("Heart Beat: ping " + appRuntime.messageServerHost + " fails")
          // if ping failed, check if message server changed?
          if (lmsClient.serverHost.compareTo(appRuntime.messageServerHost) != 0) {
            // if message server is changed sleep again
            val sleepTime = appRuntime.rand.nextInt(HEARTBEAT_INTERVAL * appRuntime.ring.ips.size)
            Thread.sleep(sleepTime)
          } else {
            // if message server is not changed, pick next message server
            if (!appRuntime.nextMessageServer().isDefined) {
              error("HeartBeat: cannot find node to be next message server, exit...")
              System.exit(-1)
            }
          }
        }
      } else {
        Thread.sleep(5000)
      }
    }
  }

  def stop() {
    keepRunning = false;
  }

}