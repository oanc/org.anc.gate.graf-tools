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
import java.net.*;
import java.util.*;

import gate.*;
import gate.creole.ResourceInstantiationException;

import org.xces.graf.api.*;
import org.xces.graf.impl.CharacterAnchor;
import org.xces.graf.io.*;
import org.xces.graf.util.IFunction;
import org.xml.sax.*;

import org.anc.util.*;
import gate.creole.ExecutionException;
import gate.util.InvalidOffsetException;

/**
 *
 * @author Keith Suderman
 * @version 1.0
 */
public class LoadGrafStandoff extends ANCLanguageAnalyzer
{
	public static final String STANDOFFASNAME_PARAMETER = "standoffASName";
	public static final String SOURCE_URL_PARAMETER = "sourceUrl";

	protected String standoffASName = null;
	protected URL sourceUrl = null;

	protected GraphParser parser;
	protected AnnotationSet annotations;
	protected GetRangeFunction getRangeFn = new GetRangeFunction();

	public LoadGrafStandoff()
	{
		super();
	}

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
//		URL url = this.getSourceUrl();
		File file = new File(sourceUrl.getPath());
		IGraph graph = null;
		try
		{
			graph = parser.parse(file);
//			graph.sort();
			for (INode node : graph.nodes())
			{
//         String type = node.getFeature("ptb", "label")
				addAnnotation(node);
			}
		}
		catch (Exception ex)
		{
			System.out.println(ex.getMessage());
			throw new ExecutionException(ex);
		}
		System.out.println("Execution complete.");
	}

	public String getStandoffASName() { return standoffASName; }
	public void setStandoffASName(String name) { standoffASName = name; }

	public URL getSourceUrl() { return sourceUrl; }
	public void setSourceUrl(URL url) { sourceUrl = url; }

	protected void addAnnotation(INode node) throws InvalidOffsetException
	{
		getRangeFn.reset();
		Offset offset = getRangeFn.apply(node);
		if (offset.getEnd() < offset.getStart())
		{
			return;
		}
		
		for (IAnnotationSet aSet : node.annotationSets())
		{
			String aSetName = aSet.getType();
			for (IAnnotation a : aSet.annotations())
			{
				FeatureMap features = Factory.newFeatureMap();
				features.put("graf:set", aSetName);
				String label = a.getLabel();
				addFeatures(a.getFeatures(), features, null);
//				for (IFeatureStructureElement fse : a.features())
//				{
//					addFeatures(fse, features, null);
//				}
//				System.out.println("Adding annotation " + label + " from " + offset.getStart() +
//						" to " + offset.getEnd());
				try
				{
					annotations.add(offset.getStart(), offset.getEnd(), label, features);
				}
				catch (InvalidOffsetException e)
				{
					throw new InvalidOffsetException("Invalid offsets for " + label + " from " + offset.getStart() +
							" to " + offset.getEnd());
				}
			}
		}
	}

	private void addFeatures(IFeatureStructureElement fse, FeatureMap features, String prefix)
	{
		if (fse.isFeature())
		{
			IFeature f = (IFeature) fse;
			String name = f.getName();
			if (prefix != null)
			{
				name = prefix + "/" + name; 
			}
			features.put(name, f.getValue());	   	
		}
		else
		{
			IFeatureStructure fs = (IFeatureStructure) fse;
			String type = fs.getType();
			if (prefix != null)
			{
				type = prefix + "/" + type;
			}
			addFeatures(fs, features, type);
		}
	}

	protected void addFeatures(IFeatureStructure fs, FeatureMap features, String type)
	{
//      IFeatureStructure fs = node.getFeatures().getFeatureStructure(type);
		if (fs == null)
		{
			return;
		}
		for (IFeature f : fs.features())
		{
		   if (f.isAtomic())
		   {
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
		      IFeatureStructure childFS = (IFeatureStructure) f.getValue();
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
//			if (e instanceof IFeature)
//			{
//				IFeature f = (IFeature) e;
//				if (type == null)
//				{
//					features.put(f.getName(), f.getValue());
//				}
//				else
//				{
//					features.put(type + "." + f.getName(), f.getValue());
//				}
//			}
//			else
//			{
//				IFeatureStructure childFS = (IFeatureStructure) e;
//				String childType = childFS.getType();
//				if (childType == null)
//				{
//					childType = type;
//				}
//				else
//				{
//					if (type != null)
//					{
//						childType = type + "." + childType;
//					}
//				}
//				addFeatures(childFS, features, childType);
//			}
		}
	}

	@Deprecated
	protected Offset getOffset(INode node)
	{
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

	protected void test()
	{
		try
		{
			System.setProperty("gate.home", "d:/Applications/Gate-5.0");
			Gate.init();
			Document doc = Factory.newDocument(new URL(
			"file:/D:/corpora/masc/ptb/graf/110cyl067-ptb.xml"));
			System.out.println("Document loaded");
			Resource res = Factory.createResource("org.anc.gate.LoadGrafStandoff");
			LoadGrafStandoff load = (LoadGrafStandoff) res;
			System.out.println("Resource created.");
//			List<String> types = new Vector<String> ();
//			types.add("ptb");
//			load.setTypes(types);
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
	public Offset()
	{
		super(Long.MAX_VALUE, Long.MIN_VALUE);
	}

	public Offset(long start, long end)
	{
		super(start, end);
	}
	public Long getStart() { return first; }
	public Long getEnd() { return second; }
	public void setStart(long start) { setFirst(start); }
	public void setEnd(long end) { setSecond(end); }

}

class GetRangeFunction implements IFunction<INode, Offset>
{
	protected Offset offset = new Offset();

	public Offset apply(INode item)
	{
		for (IRegion region : item.getRegions())
		{
			getRange(region);
		}
		for (IEdge e : item.getOutEdges())
		{
			apply(e.getTo());
		}
		return offset;
	}

	private void getRange(IRegion region)
	{
		IAnchor startAnchor = region.getStart();
		IAnchor endAnchor = region.getEnd();
		if (!(startAnchor instanceof CharacterAnchor) || !(endAnchor instanceof CharacterAnchor))
		{
			return;
		}

		CharacterAnchor start = (CharacterAnchor) startAnchor;
		CharacterAnchor end = (CharacterAnchor) endAnchor;
		if (start.getOffset() < offset.getStart())
		{
			offset.setStart(start.getOffset());
		}
		if (end.getOffset() > offset.getEnd())
		{
			offset.setEnd(end.getOffset());
		}
	}

	public void reset() 
	{ 
		offset.setStart(Long.MAX_VALUE);
		offset.setEnd(Long.MIN_VALUE);
	}
}
