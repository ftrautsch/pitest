/*
 * Copyright 2010 Henry Coles
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.instrument;

import java.io.IOException;
import java.io.OutputStream;

import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.instrument.ResultsReader.DetectionStatus;
import org.pitest.mutationtest.instrument.protocol.Id;
import org.pitest.util.SafeDataOutputStream;

public class DefaultReporter implements Reporter {

  private final SafeDataOutputStream w;

  DefaultReporter(final OutputStream w) {
    this.w = new SafeDataOutputStream(w);
  }

  public synchronized void describe(final MutationIdentifier i)
      throws IOException {
    this.w.writeByte(Id.DESCRIBE);
    this.w.write(i);
    this.w.flush();
  }

  public synchronized void report(final MutationIdentifier i,
      final DetectionStatus mutationDetected) throws IOException {
    this.w.writeByte(Id.REPORT);
    this.w.write(i);
    this.w.write(mutationDetected);
    this.w.flush();
    System.err.println("Mutation " + i + " -> " + mutationDetected);
  }

  public synchronized void done() {
    this.w.writeByte(Id.DONE);
    this.w.flush();
  }

}
