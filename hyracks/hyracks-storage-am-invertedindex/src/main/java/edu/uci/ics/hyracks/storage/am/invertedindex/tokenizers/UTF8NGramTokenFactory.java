/**
 * Copyright 2010-2011 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS"; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under
 * the License.
 * 
 * Author: Alexander Behm <abehm (at) ics.uci.edu>
 */

package edu.uci.ics.hyracks.storage.am.invertedindex.tokenizers;

public class UTF8NGramTokenFactory extends AbstractUTF8TokenFactory {

	private static final long serialVersionUID = 1L;

	public UTF8NGramTokenFactory() {
		super();
	}

	public UTF8NGramTokenFactory(byte tokenTypeTag, byte countTypeTag) {
		super(tokenTypeTag, countTypeTag);
	}

	@Override
	public IToken createToken() {
		return new UTF8NGramToken(tokenTypeTag, countTypeTag);
	}

}
