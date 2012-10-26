/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.asterix.metadata.api;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.uci.ics.asterix.metadata.bootstrap.AsterixProperties;

/**
 * Interface for setting/getting distributed state of Asterix.
 */
public interface IAsterixStateProxy extends Remote, Serializable {
    public void setMetadataNode(IMetadataNode metadataNode) throws RemoteException;

    public void setAsterixProperties(AsterixProperties asterixProperties) throws RemoteException;

    public IMetadataNode getMetadataNode() throws RemoteException;

    public AsterixProperties getAsterixProperties() throws RemoteException;
}
