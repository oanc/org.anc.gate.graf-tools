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

package ANC.gate;

import java.io.*;
import java.net.*;
import java.util.*;

//import org.xces.graf.io.*;
//import org.xces.graf.api.*;
//import org.xces.graf.impl.*;

import gate.*;
import gate.creole.ResourceInstantiationException;
import org.xml.sax.*;

import ANC.util.*;
import gate.creole.ExecutionException;
import gate.util.InvalidOffsetException;

/**
 *
 * @author Keith Suderman
 * @version 1.0
 */
public class LoadGrafStandoff extends LoadStandoff
{
   /*
	public static final String TYPES_PARAMETER = "types";
//   public static final String LABEL_PARAMETER = "labelType";

//   protected String labelType;
	protected List<String> types;
//   protected Set<String> typeSet = new HashSet<String>();

	protected GraphParser parser;
	protected AnnotationSet annotations;

	public LoadGrafStandoff()
	{
		super();
	}

//   public void setLabelType(String label) { this.labelType = label; }
	public void setTypes(List<String> types) { this.types = types; }

//   public String getLabelType() { return labelType; }
	public List<String> getTypes() { return types; }

	public Resource init() throws ResourceInstantiationException
	{
		try
		{
			super.init();
			parser = new GraphParser();
		}
		catch (SAXException ex)
		{
			throw new ResourceInstantiationException(ex);
		}
		return this;
	}

	public void execute() throws ExecutionException
	{
		annotations = getAnnotations(standoffASName);
		URL url = this.getSourceUrl();
		File file = new File(url.getPath());
		IGraph graph = null;
		try
		{
			graph = parser.parse(file);
			graph.sort();
			for (INode node : graph.nodes())
			{
//         String type = node.getFeature("ptb", "label")
				addAnnotation(node);
			}
		}
		catch (Exception ex)
		{
			throw new ExecutionException(ex);
		}
		System.out.println("Execution complete.");
	}

	protected void addAnnotation(INode node) throws InvalidOffsetException
	{
		FeatureMap features = Factory.newFeatureMap();
		Offset offset = getOffset(node);
		if (offset == null)
		{
			return;
		}

//      IFeatureStructure fs = node.getFeatures();
		for (String type : types)
		{
			IFeatureStructure fs = node.getFeatures().getFeatureStructure(type);
			if (fs == null)
			{
				continue;
			}
			String label = node.getType();
			if (label == null)
			{
				label = type;
			}
			addFeatures(fs, features, type);
			System.out.println("Adding annotation " + label + " from " + offset.getStart() +
						" to " + offset.getEnd());
			annotations.add(offset.getStart(), offset.getEnd(), label, features);
		}
//      addFeatures(node, features, null);
	}

	protected void addFeatures(IFeatureStructure fs, FeatureMap features, String type)
	{
//      IFeatureStructure fs = node.getFeatures().getFeatureStructure(type);
		if (fs == null)
		{
			return;
		}
		for (IFeatureStructureElement e : fs.features())
		{
			if (e instanceof IFeature)
			{
				IFeature f = (IFeature) e;
				System.out.println("Adding feature " + f.getName() + " = " +
										 f.getValue());
				if (type == null)
				{
					features.put(f.getName(), f.getValue());
				}
				else
				{
					features.put(type + "." + f.getName(), f.getValue());
				}
			}
			else
			{
				IFeatureStructure childFS = (IFeatureStructure) e;
				String childType = childFS.getType();
				if (childType == null)
				{
					childType = type;
				}
				else
				{
					if (type != null)
					{
						childType = type + "." + childType;
					}
				}
				addFeatures(childFS, features, childType);
			}
		}
	}

	protected Offset getOffset(INode node)
	{
		if (node instanceof ISpan)
		{
			return getOffset((ISpan)node);
		}

		Offset offset = (Offset) node.getUserObject();
		if (offset != null)
		{
//			System.out.print("Found offset for node " + node.getId());
//			System.out.println(" from " + offset.getStart() + " to " +
//			 offset.getEnd());
			return offset;
		}

		long start = Long.MAX_VALUE;
		long end = Long.MIN_VALUE;
		for (IEdge e : node.getOutEdges())
		{
			INode to = e.getTo();
			offset = getOffset(to);
			if (offset != null)
			{
				if (offset.getStart() < start)
				{
					start = offset.getStart();
				}
				if (offset.getEnd() > end)
				{
					end = offset.getEnd();
				}
			}
		}
		if (end <= start)
		{
			return null;
		}

		offset = new Offset(start, end);
//		System.out.print("Creating offset for node " + node.getId());
//		System.out.println(" from " + offset.getStart() + " to " +
//		 offset.getEnd());
		node.setUserObject(offset);
		return offset;
	}

	protected Offset getOffset(ISpan span)
	{
		Offset offset = new Offset(span.getStart(), span.getEnd());
//		System.out.print("Creating offset for span " + span.getId());
//		System.out.println(" from " + offset.getStart() + " to " +
//		 offset.getEnd());
		span.setUserObject(offset);
		return offset;
	}

	protected void test()
	{
		try
		{
			Gate.init();
			Document doc = Factory.newDocument(new URL(
			 "file:/e:/corpora2/masc/ptb/graf/written/110cyl067.txt"));
			System.out.println("Document loaded");
			Resource res = Factory.createResource("ANC.gate.LoadGrafStandoff");
			LoadGrafStandoff load = (LoadGrafStandoff) res;
			System.out.println("Resource created.");
			List<String> types = new Vector<String> ();
			types.add("ptb");
			load.setTypes(types);
			load.setDocument(doc);
			load.execute();
			System.out.println("Done");
		}
		catch (Exception ex)
		{
			System.out.println(ex);
			ex.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		LoadGrafStandoff app = new LoadGrafStandoff();
		app.test();
	}
}

class Offset extends Pair<Long, Long>
{
	public Offset(long start, long end)
	{
		super(start, end);
	}
	public Long getStart() { return first; }
	public Long getEnd() { return second; }
	public void setStart(long start) { setFirst(start); }
	public void setEnd(long end) { setEnd(end); }
	*/
}
