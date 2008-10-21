/*-
 * Copyright (c) 2008 American National Corpus
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

package org.anc.gate;

import java.io.*;
import java.util.*;

import gate.*;
import gate.creole.*;
import gate.util.*;
import java.net.*;

//import ANC.creole.ANCProcessingResource;
//import ANC.creole.ANCLanguageAnalyser;

/**  */
public class SaveContent extends org.anc.gate.ANCLanguageAnalyzer
{
	// Parameters passed by Gate.
	public static final String DESTINATION_PARAMETER_NAME = "destination";
	public static final String ENCODING_PARAMETER_NAME = "encoding";

	// Properties to hold parameter values.
	private java.net.URL destination = null;
	private java.lang.String encoding = null;

	public SaveContent() { }

	public Resource init() throws ResourceInstantiationException
	{
		super.init();
		return this;
	}

	public void reInit() throws ResourceInstantiationException
	{
		this.init();
	}

	public void execute() throws ExecutionException
	{
	  failed = true;
		if(null == destination)
			throw new ExecutionException("Parameter destination has not been set.");

		if(null == encoding)
		  encoding = "UTF-8";

		try
		{
		  FileOutputStream ofstream = new FileOutputStream(destination.getPath());
		  OutputStreamWriter writer = new OutputStreamWriter(ofstream, encoding);
//        System.out.println("Set encoding to " + writer.getEncoding());
//        FileWriter writer = new FileWriter(destination.getPath());

		  String content = document.getContent().toString();
		  writer.write(content);
		  writer.close();
		  failed = false;
		}
		catch (IOException ex)
		{
		  throw new ExecutionException(
				"SaveContent I/O Exception: " + ex.getMessage());
		}
	}

	// Property getters and setters.
	public void setDestination(java.net.URL destination) { this.destination = destination; }
	public java.net.URL getDestination() { return destination; }

	public void setEncoding(java.lang.String encoding) { this.encoding = encoding; }
	public java.lang.String getEncoding() { return encoding; }

}
