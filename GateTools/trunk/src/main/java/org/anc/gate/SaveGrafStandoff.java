package org.anc.gate;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.anc.Sys;
import org.anc.util.IDGenerator;
import org.xces.*;
import org.xces.graf.util.GraphUtils;

////import org.xces.Constants;
////import org.xces.util.AnnotationComparer;
////import ANC.creole.ANCLanguageAnalyser;

//import gate.resources.*;
import gate.*;
import gate.creole.*;
import gate.util.*;

import org.xces.graf.api.*;
import org.xces.graf.io.*;
import org.xces.graf.impl.Factory;

/**
 */
public class SaveGrafStandoff extends ANCLanguageAnalyzer
{
	// Parameters passed by Gate.
	public static final String DESTINATION_PARAMETER_NAME = "destination";
	public static final String INPUTASNAME_PARAMETER_NAME = "inputASName";
	public static final String STANDOFFTAGS_PARAMETER_NAME = "standoffTags";
	public static final String ENCODING_PARAMETER_NAME = "encoding";
	public static final String ANNOTYPE_PARAMETER_NAME = "annotationType";
	public static final String VERSION_PARAMETER_NAME = "version";
//	public static final String NAMESPACE_PARAMETER_NAME = "namespace";
//	public static final String SCHEMALOCATION_PARAMETER_NAME = "schemaLocation";

	// Properties to hold parameter values.
	private java.net.URL destination = null;
	private String inputASName = null;
	private java.util.List<String> standoffTags = null;
	private String annotationType = "standoff";
	private String version = null;
	private String encoding = null;
//	private String namespace = null;
//	private String schemaLocation = null;

	public SaveGrafStandoff()
	{
	}

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
		if (null == document)
			throw new ExecutionException("Parameter document has not been set.");

		if (null == destination)
			throw new ExecutionException("Parameter destination has not been set.");

		if (null == encoding)
		{
			encoding = "UTF-8";
		}

		try
		{
			File f = new File(destination.getPath());
			if (f.isDirectory())
			{
				f = new File(f, document.getName() + "-" + annotationType + ".xml");
			}

			IGraph graph = createGraph();
			FileOutputStream stream = new FileOutputStream(f);
			OutputStreamWriter writer = new OutputStreamWriter(stream, encoding);
			GrafRenderer graf = new GrafRenderer(writer);
			graf.render(graph);
			writer.close();
		}
		catch (Exception ex)
		{
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			throw new ExecutionException(ex);
		}
	}

	public IGraph createGraph() throws IOException, GrafException
	{
		IGraph graph = Factory.newGraph();
		IDGenerator id = new IDGenerator();
		IAnchorFactory anchorFactory = Factory.newCharacterAnchorFactory();
		
		AnnotationSet annotations = null;
		try
		{
			AnnotationSet originals = this.getAnnotations(inputASName);
			if (originals != null && originals.size() > 0)
			{
				if (standoffTags == null || standoffTags.size() == 0)
				{
					annotations = originals;
				}
				else
				{
					annotations = originals.get(new HashSet<String>(standoffTags));
				}
			}
		}
		catch (Exception ex)
		{
			Out.prln(ex.getMessage());
			failed = true;
			return null;
		}

		if (annotations == null || annotations.size() == 0)
		{
			System.out.println(this.getClass().getName() + 
			      		": No standoff annotations found.");
			failed = true;
			return null;
		}

		Comparator<Annotation> comp = org.anc.gate.AnnotationComparer
		      .createStartComparator();
		ArrayList<Annotation> sortedAnnotations = new ArrayList<Annotation>(
		      annotations);
		Collections.sort(sortedAnnotations, comp);
		Iterator<Annotation> it = sortedAnnotations.iterator();
		File f = new File(document.getSourceUrl().getPath());

		while (it.hasNext())
		{
			Annotation gateAnnotation = (Annotation) it.next();
			long start = gateAnnotation.getStartNode().getOffset().longValue();
			long end = gateAnnotation.getEndNode().getOffset().longValue();
			
			IAnchor startAnchor = anchorFactory.newAnchor(start);
			IAnchor endAnchor = anchorFactory.newAnchor(end);
			
			IRegion region = graph.getRegion(startAnchor, endAnchor);
			if (region == null)
			{
				region = Factory.newRegion(id.generate("r"), startAnchor, endAnchor);
				graph.addRegion(region);
			}
			
			List<INode> nodes = region.getNodes();
			INode node = null;
			if (nodes.size() > 0)
			{
				node = nodes.get(0);
			}
			else
			{
				node = Factory.newNode(id.generate("n"));
				graph.addNode(node);
				node.addRegion(region);
			}
			IAnnotation grafAnnotation = Factory.newAnnotation(gateAnnotation.getType());
			node.addAnnotation(grafAnnotation);
			FeatureMap fm = gateAnnotation.getFeatures();
			if (fm != null && fm.size() > 0)
			{
				Set attSet = fm.entrySet();
				Iterator asIt = attSet.iterator();
				while (asIt.hasNext())
				{
					Map.Entry att = (Map.Entry) asIt.next();
					if (!"isEmptyAndSpan".equals(att.getKey()))
					{
						String key = (String) att.getKey();
						String value = GraphUtils.encode((String)att.getValue());
						grafAnnotation.addFeature(key, value);
					}
				}
			}

		}
		return graph;
	}

	// Property getters and setters.
	public void setDestination(java.net.URL destination)
	{
		this.destination = destination;
	}

	public java.net.URL getDestination()
	{
		return destination;
	}

	public void setInputASName(String inputASName)
	{
		this.inputASName = inputASName;
	}

	public String getInputASName()
	{
		return inputASName;
	}

	public void setStandoffTags(java.util.List standoffTags)
	{
		this.standoffTags = standoffTags;
	}

	public java.util.List getStandoffTags()
	{
		return standoffTags;
	}

	public void setEncoding(String encoding)
	{
		this.encoding = encoding;
	}

	public String getEncoding()
	{
		return encoding;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getVersion()
	{
		return version;
	}

	public void setAnnotationType(String type)
	{
		this.annotationType = type;
	}

	public String getAnnotationType()
	{
		return annotationType;
	}
}
