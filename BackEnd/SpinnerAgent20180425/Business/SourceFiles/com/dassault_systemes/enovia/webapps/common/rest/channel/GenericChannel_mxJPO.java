package com.dassault_systemes.enovia.webapps.common.rest.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.File;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.util.StringList;

import com.dassault_systemes.enovia.webapps.common.rest.entity.ImageSelectable;
import com.dassault_systemes.enovia.webapps.common.rest.entity.ProgramSelectable;
import com.dassault_systemes.enovia.webapps.common.rest.entity.RowItem;
import com.dassault_systemes.enovia.webapps.common.rest.entity.RowSet;
import com.dassault_systemes.enovia.webapps.common.rest.entity.Selectable;
import com.dassault_systemes.enovia.webapps.common.rest.entity.SortDirection;
import com.dassault_systemes.enovia.webapps.common.rest.entity.SortType;
import com.dassault_systemes.enovia.webapps.common.rest.util.JPOUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.fcs.common.ImageRequestData;

public class GenericChannel_mxJPO implements ChannelService {

    private String channelName;
    
    public GenericChannel_mxJPO() {
    }

//    public String getChannelName() {
//        return channelName;
//    }

//    public void setChannelName(String name) {
//        this.channelName = name;
//    }

    public List<RowSet> query(Context context, String[] args)
        throws Exception {

            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) JPO.unpackArgs(args);
            
            String baseURL = (String) map.get("baseURL");
            String itemHref = (String) map.get("itemHref");
            String typePattern = (String) map.get("typePattern");
            String where = (String) map.get("where");
            @SuppressWarnings("unchecked")
            List<Selectable> selects = (List<Selectable>) map.get("selects");
            List<ProgramSelectable> programSelects = (List<ProgramSelectable>) map.get("programSelects");
            ImageSelectable imageSelect = (ImageSelectable) map.get("imageSelect");
            String overrideSort = (String) map.get("sort");
            String overrideSortDirection = (String) map.get("sortDirection");
            
            return query(context, baseURL, itemHref, typePattern, where, selects, programSelects, imageSelect, overrideSort, overrideSortDirection);
    }

    @Override
    public List<RowSet> query(Context context, String baseURL, String itemHref, String typePattern, String where,
            List<Selectable> selects, List<ProgramSelectable> programSelects, ImageSelectable imageSelect,
            String overrideSortId, String overrideSortDirection) throws Exception {
        List<RowSet> items = new ArrayList<RowSet>();
        try {
//            setChannelName(name);
            
            HashMap idMap = new HashMap();
            
            // Set up select list.
            StringList selectList = new StringList(DomainObject.SELECT_ID);
            
            idMap.put(DomainObject.SELECT_ID, "object" + DomainObject.SELECT_ID.toUpperCase());

            for (Selectable select : selects) {
                selectList.add(select.getExpression());
                idMap.put(select.getExpression(), select.getId());
            }
            
            // Search for Channel members.
            // May want to include vault pattern in channel definition.
            // For now we search all vaults (performance issue).
            MapList ml = DomainObject.findObjects(context, typePattern, "*", where, selectList);
            translateRangeValues(context, ml);
            evaluateProgramSelects(context, ml, programSelects);
            evaluateImageSelect(context, ml, imageSelect, baseURL);

            // if using the override sort
            if (overrideSortId != null && overrideSortDirection != null) {
            	boolean sortFound = false;
            	for (Selectable select : selects) {
            		if (overrideSortId.equalsIgnoreCase(select.getId())) {
                    	String sortDirection = overrideSortDirection;
                    	String sortType = SortType.STRING.value();
            			if (select.getSort() != null && select.getSort().getType() != null) {
    	                	sortType = select.getSort().getType().value();
            			}
            		
            			ml.addSortKey(select.getExpression(), sortDirection, sortType);
            			sortFound = true;
            			break;
            		}
            	}
            	
            	if (sortFound == false) {
            		for (ProgramSelectable select : programSelects) {
	            		if (overrideSortId.equalsIgnoreCase(select.getId())) {
	                    	String sortDirection = overrideSortDirection;
	                    	String sortType = SortType.STRING.value();
	            			if (select.getSort() != null && select.getSort().getType() != null) {
	    	                	sortType = select.getSort().getType().value();
	            			}
		                	
	            			ml.addSortKey(select.getId(), sortDirection, sortType);
	            			break;
	            		}
            		}
            	}
            }
            // otherwise use the sort specified in the xml definition
            else {
	            for (Selectable select : selects) {
	                String sortDirection = null;
	                String sortType = null;
        			if (select.getSort() != null && select.getSort().getDirection() != null) {
	                	sortDirection = select.getSort().getDirection().value();
	                	sortType = SortType.STRING.value();
	                }
        			if (select.getSort() != null && select.getSort().getType() != null) {
	                	sortType = select.getSort().getType().value();
	                	if (sortDirection == null) {
	                		sortDirection = SortDirection.ASCENDING.value();
	                	}
	                }
	
	                if (sortDirection != null) {
	                	ml.addSortKey(select.getExpression(), sortDirection, sortType);
	                }
	            }
	            
	            // add filters and sorts for program selectables
	            for (ProgramSelectable programSelect : programSelects) {
	                String sortDirection = null;
	                String sortType = null;
        			if (programSelect.getSort() != null && programSelect.getSort().getDirection() != null) {
	                	sortDirection = programSelect.getSort().getDirection().value();
	                	sortType = SortType.STRING.value();
	                }
        			if (programSelect.getSort() != null && programSelect.getSort().getType() != null) {
	                	sortType = programSelect.getSort().getType().value();
	                	if (sortDirection == null) {
	                		sortDirection = SortDirection.ASCENDING.value();
	                	}
	                }
	
	                if (sortDirection != null) {
	                	ml.addSortKey(programSelect.getId(), sortDirection, sortType);
	                }
	            }
            }
            
            // sort the list for good measure
            ml.sort();
            
            for (int index=0; index <ml.size(); index++) {
                @SuppressWarnings("unchecked")
                Map<String,String> map = (Map<String,String>) ml.get(index);
                String memberId = map.get(DomainObject.SELECT_ID);
                RowSet item = new RowSet();
                
                // Each member is given a link so it can be selected by the user
                // to get more details.
                RowItem linkProperty = new RowItem();
                linkProperty.setName("link");
                linkProperty.setValue(itemHref + memberId);
                item.getRowItem().add(linkProperty);
                
                // Now we add the select information to the member.
                for (Iterator<String> itr = map.keySet().iterator(); itr.hasNext(); ) {
                    RowItem property = new RowItem();
                    String key = itr.next();
                    String id = (String) idMap.get(key);
                    if (id == null) id = key;
                    property.setName(id);
                    property.setValue((String) map.get(key));
                    item.getRowItem().add(property);
                }
                
                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw(e);
        }
        return items;
    }

	private MapList evaluateProgramSelects(Context context, MapList ml, List<ProgramSelectable> programSelects)
	{
        if (programSelects != null && programSelects.size() > 0) {
    		// retrieve object ids from map list
        	List<String> objectIds = new ArrayList<String>();
    		Iterator<Map<String, String>> itr = ml.iterator();
        	while (itr.hasNext()) {
        		objectIds.add(itr.next().get(DomainObject.SELECT_ID));
        	}
        	
	        for (ProgramSelectable select : programSelects) {
	        	Map<String,Object> info = new HashMap<String,Object>();
	        	info.put("maplist", ml);
		        info.put("id", select.getId());

	        	try {
			        // Pack arguments into string array
			        String[] args = JPO.packArgs(info);
			        
			        // invoke JPO
                    String className = JPOUtil.getJPOClassName(context, select.getProgram());
		        	ml = (MapList) JPO.invokeLocal(context, className, new String[0], select.getMethod(), args, MapList.class);

	        	} catch (Throwable e) {
	                System.out.println("exception invoking channel select JPO program: " + select.getProgram() + " method: " + select.getMethod() + " exception: " + e.toString());
	            }
	        }
        }
        	
        return (ml);
    }

	private MapList evaluateImageSelect(Context context, MapList ml, ImageSelectable imageSelect, String baseURL)
	{
        // set the default values 
		String selectExpression = DomainObject.SELECT_IMAGE_HOLDER_ID;
        String imageId = "image";
        String imageFormat = DomainObject.FORMAT_MX_MEDIUM_IMAGE;
        String imageFile = null;
        String imageDefault = null;
        
        if (imageSelect != null) {
    		// if there is an ImageSelectable then use those values
        	imageId = imageSelect.getId();
    		imageFormat = imageSelect.getFormat();
    		imageFile = imageSelect.getFile();
    		imageDefault = imageSelect.getDefault();
    		
    		if (imageSelect.getExpression() != null && imageSelect.getExpression().length() > 0) {
    			selectExpression = imageSelect.getExpression();
    		}
        }
        
		// retrieve object ids from map list
    	List<String> objectIds = new ArrayList<String>();
		Iterator<Map<String, String>> itr = ml.iterator();
    	while (itr.hasNext()) {
    		objectIds.add(itr.next().get(DomainObject.SELECT_ID));
    	}
	            
    	StringList selects = new StringList(selectExpression);
    
        // Retrieve info for the list of objects
        try {
        	itr = DomainObject.getInfo(context, objectIds.toArray(new String[objectIds.size()]), selects).iterator();
        	
            String imageHolderId;
            String imageURL;
            for (int i = 0; itr.hasNext(); i++) {
                imageHolderId = (String) ((Map) itr.next()).get(selectExpression);
                try {
                	imageURL = getImageURL(context, baseURL, imageHolderId, imageFormat, imageFile);
                	((Map) ml.get(i)).put(imageId, imageURL);
                }
                catch (Exception e) {
            		if (imageDefault != null) {
            			((Map) ml.get(i)).put(imageId, imageDefault);
            		}
                }
            }
        }
        catch (Exception e) {
        	// just continue if we have trouble
        	// getting image holders
        }
	            
        return (ml);
    }

	private String getImageURL(Context context, String baseURL, String imageHolderId, String format, String file)
		throws Exception {

		if (imageHolderId == null || imageHolderId.length() == 0) {
        	throw new Exception("Invalid image holder id " + imageHolderId);
		}
			
		BusinessObject bo = new BusinessObject(imageHolderId);
        FormatList formatList = bo.getFormats(context);

        Format boFormat = null;
        File boFile = null;
        if (format == null || format.length() == 0) {
            boFormat = (Format) formatList.get(0);
        } else {
            for (Format f : new ArrayList<Format>(formatList)) {
                if (f.getName().equals(format)) {
                    boFormat = f;
                    break;
                }
            }
        }

        if (boFormat == null) {
        	throw new Exception("Missing format specified as " + format);
        }
        
        FileList fileList = bo.getFiles(context, boFormat.getName());
        if (file == null || file.length() == 0) {
            boFile = (File) fileList.get(0);
        } else {
            for (File f : new ArrayList<File>(fileList)) {
                if (f.getName().equals(format)) {
                    boFile = f;
                    break;
                }
            }
        }

        if (boFile == null) {
	        throw new Exception("Missing file specified as " + file);
        }

        ArrayList<BusinessObjectProxy> bops = new ArrayList<BusinessObjectProxy>();
        BusinessObjectProxy bproxy = 
        	new BusinessObjectProxy(imageHolderId, boFormat.getName(), boFile.getName(), false, false);
        bops.add(bproxy);
        // LKD - hack
        baseURL = baseURL.substring(0,baseURL.length() - "/rest".length());
        String[] urls = ImageRequestData.getImageURLS(context, baseURL, bops);

        return(urls[0]);
	}

	private MapList translateRangeValues(Context context, MapList ml)
	{
		String selectable;
		String attributeName;
		String value;
		int index;
		
		for (int i = 0; i < ml.size(); i++) {
			Map map = (Map) ml.get(i);
            for (Iterator<String> itr = map.keySet().iterator(); itr.hasNext(); ) {
                selectable = itr.next();
                index = selectable.indexOf("attribute[");
                if (index != -1) {
                	attributeName = selectable.substring(index + "attribute[".length(), selectable.indexOf("]", index)).trim();
                	try {
                		value = UINavigatorUtil.getAttrRangeI18NString(attributeName, 
                											(String) map.get(selectable), 
                											context.getSession().getLanguage());
                		map.put(selectable, value);
                	}
                	catch (Exception e) {
                		// do nothing on purpose
                	}
                }
            }
		}
			
		return (ml);
	}
}

