/*   emxInterfaceLineage.
 **
 **   Copyright (c) 2002-2015 Dassault Systemes.
 **   All Rights Reserved.
 **   This program contains proprietary and trade secret information of MatrixOne,
 **   Inc.  Copyright notice is precautionary only
 **   and does not evidence any actual or intended publication of such program.
 **
 **   This JPO contains the implementation of emxInterfaceLineage.
 **
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.search.index.Indexer;

import matrix.db.*;
import matrix.util.*;

/**
 * The <code>emxInterfaceLineage</code> class contains implementation code for
 * emxInterfaceLineage.
 *
 * @version AEF 2013
 */

public class emxInterfaceLineage_mxJPO
{
  // a cache of interface->lineage map.
  // Note this does not support dynamic addition or modification of types, unlike the kernel..
  private static Map <String, String>lineage;

  public emxInterfaceLineage_mxJPO(Context ctx, String[] args)
  {
  }

  static private Map<String, String> buildInterfaceHierarchy(Context context) throws Exception
  {
      Map<String, String> interfaces = new HashMap<String, String>();
      Map<String, String> firstPass = new HashMap<String, String>();

      String result = MqlUtil.mqlCommand(context, "list interface select $1 derived dump $2","name","|");      

      BufferedReader in = new BufferedReader(new StringReader(result));
      StringTokenizer tokenizer;
      boolean hasParent;
      String interfaceName;
      String parentName;
      String line;

      try
      {
          while ((line = in.readLine()) != null)
          {
              hasParent = false;
              tokenizer = new StringTokenizer(line, "|");
              if (tokenizer.countTokens() > 1)
              {
                  hasParent = true;
              }

              interfaceName = tokenizer.nextToken().trim();
              parentName = (hasParent == true) ? tokenizer.nextToken().trim() : null;
              firstPass.put(interfaceName, parentName);
          }
      }
      catch (IOException ex)
      {
          throw new MatrixException(ex.getMessage());
      }

      String interfaceLineage;
      Iterator<String> itr = firstPass.keySet().iterator();
      while (itr.hasNext())
      {
          interfaceLineage = null;
          interfaceName = itr.next();
          interfaceLineage = interfaceName;
          parentName = firstPass.get(interfaceName);
          while (parentName != null)
          {
              interfaceLineage = parentName + SelectConstants.cSelectDelimiter + interfaceLineage;
              parentName = firstPass.get(parentName);
          }

          interfaces.put(interfaceName, interfaceLineage);
      }

      return (Collections.unmodifiableMap(interfaces));

  }

  public String getLineage(Context ctx, String [] args)
  {
      synchronized (emxInterfaceLineage_mxJPO.class)
      {
          if (lineage == null)
          {
              try{
                  lineage = buildInterfaceHierarchy(ctx);
              }
              catch (Exception e) {
                  System.out.println("Exception building Interface hierarchy: " + e.toString());
              }
          }
      }

      String [] interfaces = args[0].split(SelectConstants.cSelectDelimiter);
      String ret = "";
      for(String interfaceName : interfaces ) {
          final String val = lineage.get(interfaceName);
          if (ret.length() > 0)
              ret += Indexer.cTaxonomyDelimiterRegexp;
          ret += (val == null) ? "" : val.toString();
      }
      return ((ret == null) ? "" : ret.toString());
  }

}
