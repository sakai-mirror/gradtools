/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/tool/src/java/org/sakaiproject/tool/dissertation/DissertationUploadAction.java,v 1.2 2005/05/12 23:49:05 ggolden.umich.edu Exp $
*
***********************************************************************************
*
* Copyright (c) 2003, 2004 The Regents of the University of Michigan, Trustees of Indiana University,
*                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
* 
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
* 
*      http://cvs.sakaiproject.org/licenses/license_1_0.html
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/

// package
package org.sakaiproject.tool.dissertation;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import org.sakaiproject.api.app.dissertation.BlockGrantGroup;
import org.sakaiproject.api.app.dissertation.BlockGrantGroupEdit;
import org.sakaiproject.api.app.dissertation.CandidateInfo;
import org.sakaiproject.api.app.dissertation.CandidateInfoEdit;
import org.sakaiproject.api.app.dissertation.CandidatePath;
import org.sakaiproject.api.app.dissertation.CandidatePathEdit;
import org.sakaiproject.api.app.dissertation.DissertationStep;
import org.sakaiproject.api.app.dissertation.StepStatus;
import org.sakaiproject.api.app.dissertation.StepStatusEdit;
import org.sakaiproject.api.app.dissertation.cover.DissertationService;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
import org.sakaiproject.cheftool.api.Menu;
import org.sakaiproject.cheftool.api.MenuItem;
import org.sakaiproject.cheftool.menu.MenuEntry;
import org.sakaiproject.cheftool.menu.MenuImpl;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.thread_local.cover.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.FileItem;
import org.sakaiproject.util.ParameterParser;
import org.sakaiproject.util.SortedIterator;
import org.sakaiproject.util.StringUtil;

/**
* <p>DissertationUploadAction is the U-M Rackham Graduate School OARD/MP data loader.</p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision:
*
*/
public class DissertationUploadAction extends VelocityPortletPaneledAction
{
	/** The state attributes */
	private final static String  STATE_INITIALIZED = "initialized";
	private final static String  STATE_ACTION = "DisserationUploadAction";
	private final static String  STATE_OARDFILE = "oardext";
	private final static String  STATE_OARD_CONTENT_STRING = "oard_content_string";
	private final static String  STATE_MPFILE = "mpext";
	private final static String  STATE_MP_CONTENT_STRING = "mp_content_string";
	private final static String  STATE_LOAD_ERRORS = "load_errors";
	private final static String  STATE_MAX_LOAD_MESSAGES_TO_DISPLAY = "max_load_errors";
	private final static String  STATE_FIELD_TO_ADD = "field_to_add";
	private final static String  STATE_FIELD_TO_EDIT = "field_to_edit";
	private final static String  STATE_FIELDS_TO_REMOVE = "fields_to_remove";
	private final static String  STATE_CODES_LIST = "codes_list";
	private final static String  STATE_STUDENT_REMOVAL_MESSAGES = "student_removal_messages";
	private final static String  STATE_STUDENTS_TO_REMOVE = "students_to_remove";
	private final static String  STATE_STUDENTS_CANNOT_REMOVE = "students_cannot_remove";
	
	/** New or edit code form values */
	private final static String  STATE_FOS_CODE = "fos_code";
	private final static String  STATE_FOS_NAME = "fos_name";
	private final static String  STATE_BGG_CODE = "bgg_code";
	private final static String  STATE_BGG_NAME = "bgg_name";
	private final static String  STATE_BGG_GROUP = "bgg_group";
	
	/** The tool modes */
	private final static String  MODE_UPLOAD = "upload";
	private final static String  MODE_CONFIRM_UPLOAD = "confirm_upload";
	private final static String  MODE_LOAD_ERRORS = "load_errors";
	private final static String  MODE_SITEID_NOT_RACKHAM = "siteid_not_rackham";
	private final static String  MODE_NO_UPLOAD_PERMISSION = "no_upload_permission";
	private final static String  MODE_CUSTOMIZE = "customize";
	private final static String  MODE_LIST_CODES = "list_codes";
	private final static String  MODE_NEW_CODE = "new_code";
	private final static String  MODE_PREVIEW_CODE = "preview_code";
	private final static String  MODE_EDIT_NAMES = "edit_names";
	private final static String  MODE_REVISE_CODE = "revise_code";
	private final static String  MODE_CONFIRM_REMOVE_STUDENTS = "confirm_remove_students";
	private final static String  MODE_CONFIRM_REMOVE_CODES = "confirm_remove_codes";
	private final static String  MODE_REMOVE_STUDENTS = "remove_students";
	
	/** The templates */
	private final static String  TEMPLATE_UPLOAD = "_upload";
	private final static String  TEMPLATE_CONFIRM_UPLOAD = "_confirm_upload";
	private final static String  TEMPLATE_CONFIRM_REMOVE_STUDENTS = "_confirm_remove_students";
	private final static String  TEMPLATE_LOAD_ERRORS = "_load_errors";
	private final static String  TEMPLATE_SITEID_NOT_RACKHAM = "_siteid_not_rackham";
	private final static String  TEMPLATE_NO_UPLOAD_PERMISSION = "_no_upload_permission";
	private final static String  TEMPLATE_CUSTOMIZE = "_upload-customize";
	private final static String  TEMPLATE_LIST_CODES = "_list_codes";
	private final static String  TEMPLATE_NEW_CODE = "_new_code";
	private final static String  TEMPLATE_REVISE_CODE = "_revise_code";
	private final static String  TEMPLATE_PREVIEW_CODE = "_preview_code";
	private final static String  TEMPLATE_EDIT_NAMES = "_edit_names";
	private final static String  TEMPLATE_REMOVE_CODES = "_remove_codes";
	private final static String  TEMPLATE_REMOVE_STUDENTS = "_remove_students";
	
	private static final String SORTED_BY_FIELD_CODE = "field_code";
	private static final String SORTED_BY_FIELD_NAME = "field_name";
	private static final String SORTED_BY_GROUP_CODE = "group_code";
	private static final String SORTED_BY_GROUP_NAME = "group_name";
	private static final String SORTED_BY = SORTED_BY_FIELD_CODE;
	private static final String SORTED_ASC = "sort_asc";
	
	/** The configuration parameters */
	private final static Integer  MAX_LOAD_MESSAGES_TO_DISPLAY = new Integer(50);
	
	/**
	*  init
	* 
	*/
	private void init (VelocityPortlet portlet, RunData data, SessionState state)
	{
		state.setAttribute(STATE_MAX_LOAD_MESSAGES_TO_DISPLAY, MAX_LOAD_MESSAGES_TO_DISPLAY);
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		state.setAttribute (STATE_INITIALIZED, Boolean.TRUE.toString());
		return;

	} // init
	
	/**
	*
	* Populate the state object, if needed.
	*/
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData rundata)
	{
		super.initState(state, portlet, rundata);
		String mode = (String)state.getAttribute(STATE_MODE);
		if(mode != null && mode.equals(MODE_LIST_CODES))
		{
			List codes = DissertationService.getBlockGrantGroups();
			state.setAttribute(STATE_CODES_LIST, codes);
		}

	}   // initState

	/** 
	* Set the tool mode and build the context
	*/
	public String buildMainPanelContext(VelocityPortlet portlet, 
										Context context,
										RunData rundata,
										SessionState state)
	{
		if (state.getAttribute (STATE_INITIALIZED)== null)
		{
			init (portlet, rundata, state);
		}
		
		context.put("action", state.getAttribute(STATE_ACTION));
		
		String template = null;
		String mode = (String) state.getAttribute(STATE_MODE);

		//is this the Rackham site?
		//if(!DissertationService.getSchoolSite().equals(PortalService.getCurrentSiteId()))
		if(!DissertationService.getSchoolSite().equals(ToolManager.getCurrentPlacement().getContext()))
		{
			mode = MODE_SITEID_NOT_RACKHAM;
		}
		
		//does this user have permission to update the Rackham site?
		if(!SiteService.allowUpdateSite(DissertationService.getSchoolSite()))
		{
			mode = MODE_NO_UPLOAD_PERMISSION;
		}
		
		// check mode and dispatch
		if (mode == null || mode.equals(MODE_UPLOAD))
		{
			template = buildUploadContext(portlet, rundata, state, context);
		}
		else if (mode.equals(MODE_CONFIRM_UPLOAD))
		{
			template = buildConfirmUploadContext(state, context);
		}
		else if (mode.equals(MODE_LOAD_ERRORS))
		{
			template = buildLoadErrorsContext(state, context);
		}
		else if (mode.equals(MODE_SITEID_NOT_RACKHAM))
		{
			template = buildNotRackhamContext(state, context);
		}
		else if (mode.equals(MODE_NO_UPLOAD_PERMISSION))
		{
			template = buildPermissionContext(state, context);
		}
		else if (mode.equals(MODE_CUSTOMIZE))
		{
			template = buildCustomizeContext(state, context);
		}
		else if (mode.equals(MODE_LIST_CODES))
		{
			template = buildListCodesContext(portlet, rundata, state, context);
		}
		else if (mode.equals(MODE_NEW_CODE))
		{
			template = buildNewCodeContext(state, context, rundata);
		}
		else if (mode.equals(MODE_PREVIEW_CODE))
		{
			template = buildPreviewCodeContext(state, context);
		}
		else if (mode.equals(MODE_REVISE_CODE))
		{
			template = buildReviseCodeContext(state, context, rundata);
		}
		else if (mode.equals(MODE_EDIT_NAMES))
		{
			template = buildEditNamesContext(state, context);
		}
		else if (mode.equals(MODE_CONFIRM_REMOVE_CODES))
		{
			template = buildConfirmRemoveCodesContext(state, context);
		}
		else if (mode.equals(MODE_REMOVE_STUDENTS))
		{
			template = buildRemoveStudentsContext(state, context);
		}
		else if (mode.equals(MODE_CONFIRM_REMOVE_STUDENTS))
		{
			template = buildConfirmRemoveStudentsContext(state, context);
		}
		else
		{
			Log.warn("chef", this + ".buildMainPanelContext: unexpected mode: " + mode);
			template = buildUploadContext(portlet, rundata, state, context);
		}

		String templateRoot = (String) getContext(rundata).get("template");
		return templateRoot + template;

	}	// buildMainPanelContext
	
	
	/**
	* Build the context for the FOS and BGG codes list form.
	*/
	private String buildListCodesContext(VelocityPortlet portlet, RunData rundata, SessionState state, Context context)
	{
		// menu bar
		Menu bar = new MenuImpl(portlet, rundata, (String) state.getAttribute(STATE_ACTION));
		bar.add( new MenuEntry("Done", "doDone_edit_codes"));
		bar.add( new MenuEntry("New...", "doNew_code"));
		bar.add ( new MenuEntry ("Edit", null, true, MenuItem.CHECKED_NA, "doEdit_names", "listCodes") );
		bar.add ( new MenuEntry ("Remove", null, true, MenuItem.CHECKED_NA, "doRemove_codes", "listCodes") );
		context.put("menu", bar);
		
		// get current Block Grant Groups from state
		List codes = (List)state.getAttribute(STATE_CODES_LIST);
		
		// get the Fields of Study belonging to the Block Grant Groups
		BlockGrantGroup group = null;
		Hashtable fields = null;
		Enumeration keys = null;
		String fieldCode = null;
		String fieldName = null;
		List listOfFields = new Vector();

		//build the list of fields
		for (ListIterator i = codes.listIterator(); i.hasNext(); )
		{
			group = (BlockGrantGroup) i.next();
			fields = group.getFieldsOfStudy();
			keys = fields.keys();
			
			// get each Field of Study from the associated Block Grant Group
			while(keys.hasMoreElements())
			{
				fieldCode = (String)keys.nextElement();
				fieldName = (String)fields.get((String)fieldCode);
				if(fieldCode!=null && fieldName!=null)
				{
					//add a field item for display
					Field field = new Field();
					field.setGroupCode(group.getCode());
					field.setGroupName(group.getDescription());
					field.setFieldCode(fieldCode);
					field.setFieldName(fieldName );
					listOfFields.add(field);
				}
			}
		}
		
		//sort the field items for display
		List sorted = new Vector();
		String sortedBy = (String) state.getAttribute (SORTED_BY);
		String sortedAsc = (String) state.getAttribute (SORTED_ASC);
		SortedIterator sortedFields = new SortedIterator(listOfFields.iterator(), new FieldComparator (sortedBy, sortedAsc));
		while (sortedFields.hasNext())
		{
			Field field = (Field) sortedFields.next();
			sorted.add(field);
		}
		
		//put the field items in context
		context.put("listOfFields", sorted);
		return TEMPLATE_LIST_CODES;
		
	}//buildListCodesContext
	
	/**
	* Build the context for the new matching FOS and BGG codes form.
	*/
	private String buildNewCodeContext(SessionState state, Context context, RunData rundata)
	{
		//catch form values in onchange submit from BGG Group select list
		ParameterParser params = rundata.getParameters();
		String FOS_code = params.getString ("FOS_code");
		String FOS_name = params.getString ("FOS_name");
		String BGG_code = params.getString ("BGG_code");
		String BGG_name = params.getString ("BGG_name");
		String BGG_group = params.getString ("BGG_group");

		/*
		 * if BGG_group is not null, use it to set BGGC and BGGD
		 */
		
		if(BGG_group != null)
		{
			BGG_code = BGG_group;
			BGG_name = groupName(BGG_code);
		}
		
		//keep the current form values
		state.setAttribute(STATE_FOS_CODE, FOS_code);
		state.setAttribute(STATE_FOS_NAME, FOS_name);
		state.setAttribute(STATE_BGG_CODE, BGG_code);
		state.setAttribute(STATE_BGG_NAME, BGG_name);
		state.setAttribute(STATE_BGG_GROUP, BGG_group);
		
		// sort the group items for display
		List sorted = (List)sortGroups(state);
		
		context.put("FOS_code", FOS_code);
		context.put("FOS_name", FOS_name);
		context.put("BGG_code", BGG_code);
		context.put("BGG_name", BGG_name);
		context.put("BGG_group", BGG_group);
		context.put("groups", sorted);
		return TEMPLATE_NEW_CODE;
		
	}//buildNewCodeContext
	
	/**
	* Access the BGG name corresponding to the BGG code
	*/
	private String groupName(String BGG_code)
	{
		String retVal = null;
		List codes = DissertationService.getBlockGrantGroups();
		if(BGG_code != null)
		{
			BlockGrantGroup group = null;	
			for (ListIterator i = codes.listIterator(); i.hasNext(); )
			{
				group = (BlockGrantGroup) i.next();
				if(BGG_code.equals(group.getCode()))
				{
					retVal = group.getDescription();
				}
			}
		}
		return retVal;
	}
	
	
	/**
	* Check for the existence of a site with specified id.
	*/
	private boolean existsSite(String id)
	{
		if(id != null)
		{
			try
			{
				Site site = SiteService.getSite(id);
			}
			catch(IdUnusedException e)
			{
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	* Build the context for the preview of new matching codes form.
	*/
	private String buildPreviewCodeContext(SessionState state, Context context)
	{
		if(state.getAttribute(STATE_FIELD_TO_ADD)!= null)
		{
			Field field = (Field)state.getAttribute(STATE_FIELD_TO_ADD);
			context.put("field", field);
		}
		return TEMPLATE_PREVIEW_CODE;
		
	}
	
	/**
	* Build the context for the edit FOS and BGG names form.
	*/
	private String buildEditNamesContext(SessionState state, Context context)
	{
		String fieldCode = (String)state.getAttribute(STATE_FIELD_TO_EDIT);
		try
		{
			BlockGrantGroup group = DissertationService.getBlockGrantGroupForFieldOfStudy(fieldCode);
			Field field = new Field();
			field.setFieldCode(fieldCode);
			field.setFieldName(getFieldName(fieldCode, group));
			field.setGroupCode(group.getCode());
			field.setGroupName(group.getDescription());
			context.put("field",field);
		}
		catch(Exception e)
		{
			if(Log.isWarnEnabled())
			{
				Log.warn("chef", this + ".buildEditNamesContext getBlockGrantGroupForFieldOfStudy " + e);
			}
		}

		return TEMPLATE_EDIT_NAMES;
	}
	
	/**
	* Get field of study name for field code in Block Grant Group
	*/
	private String getFieldName(String field, BlockGrantGroup group)
	{
		String fieldCode = null;
		String fieldName = null;
		Hashtable fields = group.getFieldsOfStudy();
		Enumeration keys = fields.keys();
		while(keys.hasMoreElements())
		{
			fieldCode = (String)keys.nextElement();
			if(fieldCode.equals(field))
			{
				return (String)fields.get((String)fieldCode);
			}
		}
		return fieldName;
	}
	
	/**
	* Build the context for the form to enter student(s) to remove.
	*/
	private String buildRemoveStudentsContext(SessionState state, Context context)
	{
		return TEMPLATE_REMOVE_STUDENTS;
		
	}//buildRemoveStudentsContext
	
	/**
	* Build the context for the form to edit new matching FOS and BGG codes.
	*/
	private String buildReviseCodeContext(SessionState state, Context context, RunData rundata)
	{
		//catch form values in onchange submit from BGG Group select list
		ParameterParser params = rundata.getParameters();
		String FOS_code = params.getString ("FOS_code");
		String FOS_name = params.getString ("FOS_name");
		String BGG_code = params.getString ("BGG_code");
		String BGG_name = params.getString ("BGG_name");
		String BGG_group = params.getString ("BGG_group");

		/*
		 * if BGG_group is not null, use it to set BGGC and BGGD
		 */
		
		if(BGG_group != null)
		{
			BGG_code = BGG_group;
			BGG_name = groupName(BGG_code);
		}
		
		Field field = (Field)state.getAttribute(STATE_FIELD_TO_ADD);
		
		//set field to current values and if any missing addAlert
		if(FOS_code != null && !FOS_code.equals(""))
			field.setFieldCode(FOS_code);
		if(FOS_name != null && !FOS_code.equals(""))
			field.setFieldName(FOS_name);
		if(BGG_code != null && !BGG_code.equals(""))
			field.setGroupCode(BGG_code);
		if(BGG_name != null && !BGG_name.equals(""))
			field.setGroupName(BGG_name);
			
		// sort the group items for display
		List sorted = (List)sortGroups(state);
		
		context.put("groups", sorted);
		context.put("field", field);
		context.put("BGG_group", BGG_group);
		return TEMPLATE_REVISE_CODE;
		
	}//buildReviseCodeContext
	
	/**
	* Sort the Block Grant Groups for display.
	*/
	private List sortGroups(SessionState state)
	{
		List retVal = new Vector();
		List blockGrantGroups = DissertationService.getBlockGrantGroups();
		String sortedAsc = (String) state.getAttribute (SORTED_ASC);
		SortedIterator sortedGroups = new SortedIterator(blockGrantGroups.iterator(), new GroupComparator (SORTED_BY_GROUP_NAME, sortedAsc));
		while (sortedGroups.hasNext())
		{
			BlockGrantGroup group = (BlockGrantGroup) sortedGroups.next();
			retVal.add(group);
		}
		return retVal;
	}
	
	/**
	* Build the context for the form.
	*/
	private String buildConfirmRemoveCodesContext(SessionState state, Context context)
	{
		String[] fields = (String[])state.getAttribute(STATE_FIELDS_TO_REMOVE);
		List listOfFields = new Vector();
		for(int i = 0; i < fields.length; i++)
		{
			Field field = new Field();
			field.setFieldCode(fields[i]);
			try
			{
				BlockGrantGroup group = DissertationService.getBlockGrantGroupForFieldOfStudy((String)fields[i]);
				field.setFieldName(getFieldName((String)fields[i], group));
				field.setGroupCode(group.getCode());
				field.setGroupName(group.getDescription());	
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
				{
					Log.warn("chef", this + ".buildConfirmRemoveCodesContext " + e);
				}
			}
			listOfFields.add(field);
		}
		context.put("fields", listOfFields);
		return TEMPLATE_REMOVE_CODES;
		
	}//buildConfirmRemoveCodesContext
	
	/**
	* Build the context for the confirm student(s) removal form.
	*/
	private String buildConfirmRemoveStudentsContext(SessionState state, Context context)
	{
		//get list of uniqnames with paths from state
		List namesToRemove = (Vector)state.getAttribute(STATE_STUDENTS_TO_REMOVE);
		List namesCannotRemove = (Vector)state.getAttribute(STATE_STUDENTS_CANNOT_REMOVE);
		
		//put number of students to remove in context
		context.put("n", new Integer(namesToRemove.size()));
		
		//put names to remove in context
		context.put("uniqnames", namesToRemove);
		context.put("badnames", namesCannotRemove);
		return TEMPLATE_CONFIRM_REMOVE_STUDENTS;
		
	}//buildConfirmRemoveStudentsContext
	
	/**
	* Build the context for the file upload form.
	*/
	private String buildUploadContext(VelocityPortlet portlet, RunData rundata, SessionState state, Context context)
	{
		// menu bar
		Menu bar = new MenuImpl(portlet, rundata, (String) state.getAttribute(STATE_ACTION));
		bar.add( new MenuEntry("Show...", "doShow_setting"));
		bar.add( new MenuEntry("Edit Codes", "doList_codes"));
		bar.add(new MenuEntry("Remove Students", "doRemove_students"));
		
		/** a utility **
		bar.add(new MenuEntry("Check for duplicate steps", "doCheck_duplicate_steps"));
		*/
		
		context.put("menu", bar);
		return TEMPLATE_UPLOAD;

	}	// buildUpLoadContext


	/**
	* Build the context for file upload confirmation.
	*/
	private String buildConfirmUploadContext(SessionState state, Context context)
	{
		//get both FileItems from state
		FileItem oard = (FileItem)state.getAttribute(STATE_OARDFILE);
		FileItem mp = (FileItem)state.getAttribute(STATE_MPFILE);
		
		//this should have been caught earlier
		if(oard.getFileName().equals("") && mp.getFileName().equals(""))
		{
			addAlert(state, "Both extract files are missing names. Please contact GradTools Support if you have questions.");
			return TEMPLATE_UPLOAD;
		}
		else if(oard.getFileName().equals(""))
		{
			addAlert(state, "Did you mean to omit the OARD extract file?");
		}
		else if (mp.getFileName().equals(""))
		{
			addAlert(state, "Did you mean to omit the MP extract file?");
		}
		
		//display properties of the data files for confirmation
		try
		{
			if(!oard.getFileName().equals(""))
			{
				String OARDContentString = oard.getString();
				state.setAttribute(STATE_OARD_CONTENT_STRING, OARDContentString);
				
				Hashtable OARDProps = DissertationService.getDataFileProperties(OARDContentString.getBytes());
				context.put("OARDLines", ((Integer)OARDProps.get(DissertationService.DATAFILE_LINES)).toString());
				context.put("OARDFileName", (String)oard.getFileName());
				context.put("OARDContentType", (String)oard.getContentType());
			}
			if(!mp.getFileName().equals(""))
			{
				String MPContentString = mp.getString();
				state.setAttribute(STATE_MP_CONTENT_STRING, MPContentString);
				
				Hashtable MPProps = DissertationService.getDataFileProperties(MPContentString.getBytes());
				context.put("MPLines", ((Integer)MPProps.get(DissertationService.DATAFILE_LINES)).toString());
				context.put("MPFileName", (String)mp.getFileName());
				context.put("MPContentType", (String)mp.getContentType());
			}
		}
		catch (Exception e)
		{
			Log.warn("chef", this + ".buildConfirmUpLoadContext Exception caught displaying properties of the data files: " + e);
		}
		
		//remove FileItems from state
		state.removeAttribute(STATE_OARDFILE);
		state.removeAttribute(STATE_MPFILE);
		
		return TEMPLATE_CONFIRM_UPLOAD;

	}	// buildConfirmUpLoadContext
	
	/**
	* Build the context for the load error messages display
	*/
	private String buildLoadErrorsContext(SessionState state, Context context)
	{
		List loadErrors = (Vector) state.getAttribute(STATE_LOAD_ERRORS);
		state.removeAttribute(STATE_LOAD_ERRORS);
		
		int max = ((Integer)state.getAttribute(STATE_MAX_LOAD_MESSAGES_TO_DISPLAY)).intValue();
		int totalErrors = 0;
		if(loadErrors != null && loadErrors.size() > 0)
			totalErrors = loadErrors.size();
		boolean header = false;

		//see if there is a header included with the error msg's
		for (ListIterator i = loadErrors.listIterator(); i.hasNext(); )
		{
			String msg = (String) i.next();
			if((msg.equals("During loading of data:")) || (msg.equals("During validation of data:")))
			{
				context.put("header", Boolean.TRUE.toString());
				header = true;
				max = max + 1;
			}
		}
		 
		 //if there are more msg's than the show error max value, show max number (+ 1 if header is included)
		if(loadErrors.size() < max)
		{
			context.put("loadErrors", loadErrors);
		}
		else
		{
			List lessErrors = new Vector();
			for (int i = 0; i < max; i++)
			{
				lessErrors.add(loadErrors.get(i));
			}
			context.put("loadErrors", lessErrors);
			context.put("limit", (Integer)state.getAttribute(STATE_MAX_LOAD_MESSAGES_TO_DISPLAY));
			context.put ("displayLimit", Boolean.TRUE.toString());
		}
		if(!header)
		{
			totalErrors = totalErrors + 1;
		}
		context.put("totalErrors", new Integer(totalErrors));
		
		return TEMPLATE_LOAD_ERRORS;
	}
	
	/**
	* Build the context for the error message, Upload is only available to Rackham
	*/
	private String buildNotRackhamContext(SessionState state, Context context)
	{
		return TEMPLATE_SITEID_NOT_RACKHAM;
		
	}	// buildSiteidNotRackhamContext
	
	/**
	* Build the context for the error message, You do not have permission to update Rackham
	*/
	private String buildPermissionContext(SessionState state, Context context)
	{
		return TEMPLATE_NO_UPLOAD_PERMISSION;
		
	}	// buildNoUploadPermissionContext
	
	/** 
	* Setup for the options panel.
	*/
	public String buildCustomizeContext(SessionState state, Context context)
	{
		// provide option to change the default number of messages to display during a session
		String messages = ((Integer)state.getAttribute(STATE_MAX_LOAD_MESSAGES_TO_DISPLAY)).toString();
		context.put("messages", messages);
		return TEMPLATE_CUSTOMIZE;
		
	}//buildCustomizeContext
	
	/**
	* Make substitutions in template
	*/
	public String getStaticChecklist(SessionState state, CandidatePath path, String template)
	{
		int last = 0;
		int indexOfHead = 0;
		String order = null;
		String line = null;
		String fname = null;
		String lname = null;
		StringBuffer buf = null;
		Time now = TimeService.newTime();
		List sections = getSectionHeads();
		try
		{
			//path.getCandidate() returns User id
			User candidate = UserDirectoryService.getUser(path.getCandidate());
			if(candidate != null)
			{
				fname = candidate.getFirstName();
				lname = candidate.getLastName();
			}
		}
		catch(Exception e)
		{
			if(Log.isWarnEnabled())
				Log.warn("chef", this + ".getStaticChecklist getUser " + e);
		}
		
		//TODO import class TemplateStep with method to getTemplateSteps
		TemplateStep step = null;
		TemplateStep[] steps = getTemplateSteps(path, state, true);
		List lines = new Vector();
		
		//TODO bufSize get from config
		int bufSize = 32000;
		buf = new StringBuffer(bufSize);
		try
		{
			//TODO get HTML by merging objects into template
			buf.append("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>\n");
			buf.append("<html xmlns='http://www.w3.org/1999/xhtml' lang='en' xml:lang='en'>\n");
			buf.append("<head>\n");
			buf.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n");
			buf.append("<meta http-equiv='Content-Style-Type' content='text/css' />\n");
			buf.append("<title>Dissertation Checklist</title>\n");
			buf.append("<link href='/ctlib/skin/tool_base.css' type='text/css' rel='stylesheet' media='all' />\n");
			buf.append("<link href='/ctlib/skin/ctools/tool.css' type='text/css' rel='stylesheet' media='all' />\n");
			buf.append("</head>\n");
			buf.append("<body>\n");
			buf.append("<br/><br/>\n");
			buf.append(fname + " " + lname + " Dissertation Checklist, as of " + now.toStringLocalDate() + "\n");
			buf.append("<br/><br/>\n");
			
			//legend
			buf.append("<table border='0' cellspacing='0' cellpadding='5' width='70%'>\n");
			buf.append("<tr>\n");
			buf.append("<td align='center'>LEGEND&nbsp;:&nbsp;</td>\n");
			buf.append("<td><center><img alt='Student' src='/library/image/sakai/diss_validate1.gif' BORDER='0' /></center></td>\n");
			buf.append("<td><center><img alt='Committee Chair' src='/library/image/sakai/diss_validate2.gif' BORDER='0' /></center></td>\n");
			buf.append("<td><center><img alt='Committee' src='/library/image/sakai/diss_validate3.gif' BORDER='0' /></center></td>\n");
			buf.append("<td><center><img alt='Department' src='/library/image/sakai/diss_validate4.gif' BORDER='0' /></center></td>\n");
			buf.append("<td><center><img alt='Rackham' src='/library/image/sakai/diss_validate5.gif' BORDER='0' /></center></td>\n");
			buf.append("</tr>\n");
			buf.append("<tr>\n");
			buf.append("<td></td>\n");
			buf.append("<td><center>Student</center></td>\n");
			buf.append("<td><center>Committee Chair</center></td>\n");
			buf.append("<td><center>Committee</center></td>\n");
			buf.append("<td><center>Department</center></td>\n");
			buf.append("<td><center>Rackham</center></td>\n");
			buf.append("</tr>\n");
			buf.append("</table>\n");
			
			//steps
			buf.append("<table class='listHier' summary='All Steps in the Candidates Path' border='0' cellspacing='0' cellpadding='5' width='100%'>\n");
			buf.append("<tr>");
			buf.append("<th id='empty1'>&nbsp;&nbsp;</th>");
			buf.append("<th id='actors' align='left' valign='bottom' nowrap='nowrap'>Actor(s)</th>");
			buf.append("<th id='empty2'>&nbsp;&nbsp;</th>");
			buf.append("<th id='step' align='left' valign='bottom'>Step</th>");
			buf.append("<th id='prerequisites' width='10%' align='left' valign='bottom' nowrap='nowrap'>Prerequisites</th>");
			buf.append("<th id='approved' align='left' valign='bottom' nowrap='nowrap'>Approved On</th>");
			buf.append("</tr>");
			if(steps.length > 0)
			{
				//first section header
				last = 1;
				buf.append("<tr>\n");
				buf.append("<td colspan='5' style='font-weight:bold;color:#616161; padding-bottom:15px; padding-top:15px'>\n");
				buf.append(sections.get(last));
				buf.append("</td>\n");
				buf.append("</tr>\n");
				for(int i = 0; i < steps.length; i++)
				{
					try
					{
						step = steps[i];
						order = (new Integer(i+1)).toString();
						indexOfHead = step.getSection();
						if(indexOfHead != 0)
						{
							if(indexOfHead != last)
							{
								//others section headers
								buf.append("<tr>\n");
								buf.append("<td colspan='5' style='font-weight:bold;color:#616161; padding-bottom:15px; padding-top:15px'>\n");
								buf.append(sections.get(indexOfHead));
								buf.append("</td>\n");
								buf.append("</tr>\n");
							}
						}
						
						//prerequisites not completed
						buf.append("<tr ");
						if(step.getStatus().equals("Prerequisites not completed."))
							buf.append("text='#CCCCCC'");
						buf.append(">\n");
						
						//col 1 checkmark or checkbox
						if(step.getStatus().equals("Step completed."))
						{
							//TimeCompleted
							buf.append("<td headers='empty1' width='3%'><img alt='Completed' title='Completed : ");
							buf.append(step.getTimeCompleted());
							buf.append("' src = '/library/image/sakai/checkon.gif' BORDER='0' /></td>\n");
						}
						else if(step.showCheckbox())
						{
							//StatusReference
							buf.append("<td headers='empty1' width='3%'><input type='checkbox' name='selectedstatus' ");
							buf.append("id='selectedstatus' value='");
							buf.append(step.getStatusReference());
							buf.append("' ></input></td>\n");
						}
						else
						{
							buf.append("<td headers='empty1' width='3%'></td>\n");
						}
						
						//col 2 actor(s)
						buf.append("<td headers='empty2' width='3%'>\n");
						buf.append("<center><img  src='/library/image/" + step.getValidationImage() + "'"); 
						buf.append("alt='ValidationType'");
						buf.append("' BORDER='0'");
						buf.append("/></center></td>\n");
						if(step.getStatus().equals("Prerequisites not completed."))
						{
							//cols 3-5 order, instructions, prerequisites
							buf.append("<td headers='step' width='3%'><span style='color:#777777'>\n");
							buf.append(order);
							buf.append(".</span></td>\n");
							buf.append("<td headers='empty3' width='61%' ><span style='color:#777777'>\n");
							buf.append(step.getInstructions());
							buf.append("</span></td>\n");
							buf.append("<td headers='prerequisites' width='10%'><span style='color:#777777'>\n");
							buf.append(step.getPrereqs());
							buf.append("</span></td>\n");
						}
						else
						{
							//cols 3-5 order, instructions, prerequisites
							buf.append("<td headers='step' width='3%'>\n");
							buf.append(order);
							buf.append(".</td>\n");
							buf.append("<td headers='empty3' width='71%'>\n");
							buf.append(step.getInstructions());
							
							//committee members
							lines = step.getAuxiliaryText();
							if(lines != null && lines.size() > 0)
							{
								buf.append("<br/><i>Evaluations required from:</i><br/>\n");
								for(int j = 0; j < lines.size(); j++)
								{
									line = (String)lines.get(j);
									buf.append(line);
									buf.append("<br/>\n");
								}
							}
							buf.append("</td>\n");
							buf.append("<td headers='prerequisites' width='10%'>\n");
							buf.append(step.getPrereqs() + "</td>\n");
						}
						buf.append("<td headers='approved' width='20%'>" + step.getTimeCompleted() + "</td>");
						buf.append("</tr>\n");
						
						//if a department or personal step this will be 0
						if(indexOfHead != 0)
							last = indexOfHead;
					}
					catch(Exception e)
					{
						if(Log.isWarnEnabled())
							Log.warn("chef", this + ".getStaticChecklist " + e);
						continue;
					}
				}
			}
			else
			{
				buf.append("No steps are defined for this checklist.");
			}
			buf.append("</table></body></html>\n");
		}
		catch(Exception e)
		{
			if(Log.isWarnEnabled())
				Log.warn("chef", this + ".getStaticChecklist " +e);
		}
		
		//this should trim the underlying char[]
		return new String(buf.toString());
		
	}//getStaticChecklist

	/**
	* Get the template for creating a static copy of a checklist
	*/
	public String getStaticTemplate()
	{
		//TODO implement creation of HTML by merging template and objects
		String body = "";
		String home = null;
		
		//get the static checklist template
		try
		{
			home = ContentHostingService.getSiteCollection(ToolManager.getCurrentPlacement().getContext());
			try
			{
				ContentHostingService.checkCollection(home);
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
					Log.warn("chef", this + ".getStaticTemplate checkCollection " + e);
				return body;
			}
			ContentResource checklist = ContentHostingService.getResource(home + DissertationService.STATIC_CHECKLIST_TEMPLATE);
			
			// read the body
			if (checklist.getContent () != null)
			{
				body = new String (checklist.getContent ());
			}
		}
		catch(Exception e)
		{
			if(Log.isWarnEnabled())
				Log.warn("chef", this + ".getStaticTemplate getSiteCollection " + e);
		}
		return body;
		
	}//getStaticTemplate
	
	/**
	* Get the candidates to be removed
	*/
	public List getCandidatesToRemove(List paths)
	{
		CandidatePath path = null;
		String candidate = null;
		List candidates = new Vector();
		for (ListIterator i = paths.listIterator(); i.hasNext(); )
		{
			path = (CandidatePath)i.next();
			if(path != null)
			{
				candidate = path.getCandidate();
				if(candidate != null && !candidate.equals(""))
					candidates.add(candidate);
			}
		}
		return candidates;
		
	}//getCandidatesToRemove
	
	/**
	* Get the student paths that can be removed
	*/
	public List getPathsToRemove(SessionState state, String template)
	{
		//get the student(s) selected for removal
		List uniqnames = (Vector)state.getAttribute(STATE_STUDENTS_TO_REMOVE);
		List paths = new Vector();
		String uniqname = null;
		String siteId = null;
		CandidatePath path = null;
		for (ListIterator i = uniqnames.listIterator(); i.hasNext(); )
		{
			uniqname = (String)i.next();
			try
			{
				path = DissertationService.getCandidatePathForCandidate(uniqname);
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
					Log.warn("chef", this + ".getPathsToRemove " + e);
			}
			if(path != null)
			{
				siteId = path.getSite();
				if(siteId != null && !siteId.equalsIgnoreCase(uniqname))
				{
					//if we have a GradToolsStudent site id
					try
					{
						//if a copy was saved, remove path
						if(takeSnapshot(state, path, template))
							paths.add(path);
					}
					catch(Exception e)
					{
						continue;
					}
				}
				else
				{
					//if there is no GradToolsStudent site (or tool never used on it), remove path
					paths.add(path);
				}
			}
		}
		return paths;
		
	}//getPathsToRemove
	
	/**
	* Create a static copy of the student's checklist under Resources
	*/
	public boolean takeSnapshot(SessionState state, CandidatePath path, String template)
	{
		boolean retVal = false;
		
		//TODO develop a template into which dynamic data is inserted
		//get the a static copy of the checklist
		String checklist = getStaticChecklist(state, path, template);
		
		//take a snapshot of the path
		try
		{
			Snapshot s = new Snapshot(path.getSite(), checklist);
			Thread t = new Thread(s);
			t.start();
			retVal = true;
		}
		catch(Exception e)
		{
			retVal = false;
		}
		
		return retVal;
		
	}//takeSnapshot
	
	/**
	* Remove student(s)' path, step status, and info from db
	*/
	public void removeStudents(SessionState state, List paths, List candidates)
	{
		//remove path, status and candidate info
		CandidatePath path = null;
		CandidatePathEdit pathEdit= null;
		CandidateInfo info = null;
		CandidateInfoEdit infoEdit = null;
		StepStatusEdit statusEdit = null;
		Hashtable statuses = new Hashtable();
		String candidate = null;
		String statusRef = null;
		String key = null;
		String msg = "";
		
		//remove path and status
		for (ListIterator i = paths.listIterator(); i.hasNext(); )
		{
			path = (CandidatePath)i.next();
			if(path != null)
			{
				statuses = path.getOrderedStatus();
				try
				{
					pathEdit = DissertationService.editCandidatePath(path.getReference());
				}
				catch(Exception e)
				{
					if(pathEdit != null && pathEdit.isActiveEdit())
					{
						DissertationService.cancelEdit(pathEdit);
						msg = msg + " " + path.getCandidate();
					}
				}
				try
				{
					if(pathEdit != null)
						DissertationService.removeCandidatePath(pathEdit);
				}
				catch(Exception e)
				{
					msg = msg + " " + path.getCandidate();
					
					//if unable to remove path, leave step statuses
					continue;
				}
				
				//remove status
				if(statuses != null)
				{
					for (int j = 1; j < statuses.size(); j++)
					{
						try
						{
							key = "" + j;
							statusRef = (String)statuses.get(key);
							statusEdit = DissertationService.editStepStatus(statusRef);
							if(statusEdit != null)
								DissertationService.removeStepStatus(statusEdit);
						}
						catch(Exception e)
						{
							if(statusEdit != null && statusEdit.isActiveEdit())
								DissertationService.cancelEdit(statusEdit);
						}
					}
				}
			}
		}

		//remove candidate info
		for (ListIterator i = candidates.listIterator(); i.hasNext(); )
		{
			try
			{
				candidate = (String)i.next();
				info = DissertationService.getInfoForCandidate(candidate);
				if(info != null)
					infoEdit = DissertationService.editCandidateInfo(info.getReference());
				if(infoEdit != null)
					DissertationService.removeCandidateInfo(infoEdit);
			}
			catch(Exception e)
			{
				if(infoEdit != null && infoEdit.isActiveEdit())
				{
					DissertationService.cancelEdit(infoEdit);
					msg = msg + " " + candidate;
				}
				continue;
			}
		}
		if(!msg.equals(""))
			state.setAttribute(STATE_STUDENT_REMOVAL_MESSAGES, msg);

	}//removeStudents
	
	/**
	* Handle a menu request to customize Messages displayed setting.
	*/
	public void doShow_setting(RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_MODE, MODE_CUSTOMIZE);
		
	}//doShow_setting
	
	/**
	* Handle a request to Save code name changes.
	*/
	public void doSave_edited_names (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String FOS_code = params.getString("FOS_code");
		String FOS_name = params.getString("FOS_name");
		String BGG_name = params.getString("BGG_name");
		String fieldCode = null;
		
		//get edit and set names
		try
		{
			BlockGrantGroup group = DissertationService.getBlockGrantGroupForFieldOfStudy(FOS_code);
			try
			{
				BlockGrantGroupEdit edit = DissertationService.editBlockGrantGroup(group.getReference());
				
				//update Block Grant Group Name
				edit.setDescription(BGG_name);
				Hashtable fields = group.getFieldsOfStudy();
				Enumeration keys = fields.keys();
				while(keys.hasMoreElements())
				{	
					fieldCode = (String)keys.nextElement();
					if(fieldCode.equals(FOS_code))
					{
						fields.remove(fieldCode);
						
						//update Field of Study name
						fields.put(FOS_code, FOS_name);
					}
				}
				DissertationService.commitEdit(edit);
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
				{
					Log.warn("chef", this + ".doSave_edited_names editBlockGrantGroup(" + group.getReference() + ") " + e);
				}
			}
		}
		catch(Exception e)
		{
			if(Log.isWarnEnabled())
			{
				Log.warn("chef", this + ".doSave_edited_names getBlockGrantGroupForFieldOfStudy(" + FOS_code + ") " + e);
			}
		}

		state.setAttribute(STATE_MODE, MODE_LIST_CODES);
		
	}//doSave_edited_names
	
	/**
	* Handle a request to Save tool Options setting change.
	*/
	public void doSave_setting (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		try
		{
			//try to get a number from the form field
			state.setAttribute(STATE_MAX_LOAD_MESSAGES_TO_DISPLAY, Integer.valueOf(params.getString ("messages")));
		}
		catch (Exception e)
		{
			//set messages to display to default number
			state.setAttribute(STATE_MAX_LOAD_MESSAGES_TO_DISPLAY, MAX_LOAD_MESSAGES_TO_DISPLAY);
		}
		
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	} //doOptions
	
	/**
	* Utility to check for steps with identical instructions.
	*/
	public void doCheck_duplicate_steps (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		String[] letters = {"A","B","C","D","E","F","G","H","I","J","K",
				"L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
		
		//String[] letters = {"U"};
		String type = null;
		String msg = null;
		CandidatePath path = null;
		User u = null;
		List students = new Vector();
		List bad = new Vector();
		int count = 0;
		
		//check each path for each dissertation type and sort letter
		for(int i = 0; i < letters.length; i++)
		{
			try
			{
				/*keep track of progress */
				if(Log.isInfoEnabled())
					Log.info("chef",letters[i]);
				
				//check Dissertation Steps
				type = DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS;
				
				/*keep track of progress */
				if(Log.isInfoEnabled())
					Log.info("chef",type);
				students.clear();
				students = DissertationService.getSortedUsersOfTypeForLetter(type, letters[i]);
				
				/*keep track of progress */
				if(Log.isInfoEnabled())
					Log.info("chef","number of students to check is " + students.size());
				for(int j = 0; j < students.size(); j++)
				{
					try
					{
						u = (User)students.get(j);
						path = DissertationService.getCandidatePathForCandidate(u.getId());
						
						//check for path with duplicate steps and personal steps
						msg = checkDuplicateSteps(path);
						if(msg != null && !msg.equals(""))
						{
							bad.add(msg);
							count = count + 1;
						}
					}
					catch(Exception e)
					{
						if(Log.isWarnEnabled())
							Log.warn("chef",u.getId() + " " + u.getDisplayName() + " path " + path.getId() + " " + e);
					}
					continue;
				}
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
					Log.warn("chef",".doCheck_dups() " + type + " " + e);
			}
			try
			{
				//check Dissertation Steps: Music Performance
				type = DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE;
				
				/* keep track of progress */
				if(Log.isInfoEnabled())
					Log.info("chef",type);
				students.clear();
				students = DissertationService.getSortedUsersOfTypeForLetter(type, letters[i]);
				
				/*keep track of progress */
				if(Log.isInfoEnabled())
					Log.info("chef","number of students to check is " + students.size());
				for(int j = 0; j < students.size(); j++)
				{
					try
					{
						u = (User)students.get(j);
						path = DissertationService.getCandidatePathForCandidate(u.getId());
						
						//check for path with duplicate steps and personal steps
						msg = checkDuplicateSteps(path);
						if(msg != null && !msg.equals(""))
						{
							bad.add(msg);
							count = count + 1;
						}
					}
					catch(Exception e)
					{
						if(Log.isWarnEnabled())
							Log.warn("chef",u.getId() + " " + u.getDisplayName() + " path " + path.getId() + " " + e);
					}
					continue;
				}
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
					Log.warn("chef",".doCheck_dups() " + type + " " + e);
			}
			continue;
		}
		
		//sort by uniqname
		if(bad != null && !bad.isEmpty())
			Collections.sort(bad);
		
		//save as a ContentResource
		ContentResourceEdit edit = null;
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < bad.size(); i++)
			buf.append(bad.get(i));
		byte[] results = buf.toString().getBytes();
		try
		{
			edit = ContentHostingService.addResource("/group/rackham/duplicates.txt");
			edit.setContent(results);
			edit.setContentType("text/plain");
			edit.setContentLength(results.length);
			ResourcePropertiesEdit props = edit.getPropertiesEdit();
			props.addProperty(ResourceProperties.PROP_DISPLAY_NAME,"Duplicate Steps");
			props.addProperty(ResourceProperties.PROP_DESCRIPTION, "Duplicate Steps");
			ContentHostingService.commitResource(edit);
		}
		catch(Exception e)
		{
			if(edit != null && edit.isActiveEdit())
			{
				try
				{
					ContentHostingService.removeResource(edit);
				}
				catch(Exception ee)
				{
					if(Log.isWarnEnabled())
						Log.warn("chef", this + ".doCheck_dups() removeResource(edit) " + e);
				}
			}
			if(Log.isWarnEnabled())
				Log.warn("chef", this + ".doCheck_dups() commitResource(edit) " + e);
		}
		//saved
		
		if(Log.isInfoEnabled())
			Log.info("chef","count of paths with duplicate steps " + count);
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	} //doCheck_duplicate_steps

	
	/**
	* Handle a request to Cancel tool Options setting change.
	*/
	public void doCancel_setting (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	} //doCancel_settings
	
	/**
	* Handle a request to Cancel new matching code creation or change.
	*/
	public void doCancel_code (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_MODE, MODE_LIST_CODES);
		
		//clear the field to add
		state.removeAttribute(STATE_FIELD_TO_ADD);
		
		//clear the form values
		state.removeAttribute(STATE_FOS_CODE);
		state.removeAttribute(STATE_FOS_NAME);
		state.removeAttribute(STATE_BGG_CODE);
		state.removeAttribute(STATE_BGG_NAME);
		state.removeAttribute(STATE_BGG_GROUP);
		
	} //doCancel_code


	/**
	* Handle a request to Upload OARD/MP extract files
	* Action is to handle the Rackham data extract upload by a Rackham adminstrator
	*/
	public void doUpload (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		
		//if an upload or step change is in progress, don't start another until finished
		if(DissertationService.isLoading())
		{
			addAlert(state, "Data loading is already in progress. Please wait until loading finishes to start loading again.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			return;
		}
		if(DissertationService.isChangingStep())
		{
			addAlert(state, "Checklist steps are being changed. Please try uploading later.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			return;
		}
		ParameterParser params = data.getParameters();
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
		FileItem OARDFileItem = null;
		FileItem MPFileItem = null;
		try
		{
			//get OARD FileItem
			OARDFileItem = params.getFileItem ("OARD");
			
			//get MP FileItem
			MPFileItem = params.getFileItem ("MP");
			
			//if neither file was entered
			if(OARDFileItem.getFileName().equals("") && MPFileItem.getFileName().equals(""))
				addAlert(state, "No file names were entered.");
		}
		catch (Exception e)
		{
			Log.warn("chef", this + " .doUpload: Exception caught checking file items:  " + e);
			addAlert(state, "doUpload: A problem was encountered with the file(s):  " + e.getMessage());
		}
		
		//if there are no alert messages, proceed to asking for confirmation of the load
		if(((String) state.getAttribute(STATE_MESSAGE)) == null)
		{
			//change mode to confirm upload
			state.setAttribute(STATE_MODE, MODE_CONFIRM_UPLOAD);
			
			//put FileItem(s) in state pending confirmation
			state.setAttribute(STATE_OARDFILE, OARDFileItem);
			state.setAttribute(STATE_MPFILE, MPFileItem);
		}
		else
		{
			//deliver alerts to the main panel
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
		}

	} // doUpload
	
	/**
	* Handle a request to display Blook Grant Group(BGG) and Field of Study(FOS) codes and names.
	*/
	public void doList_codes (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		//List codes = DissertationService.getBlockGrantGroups();
		
		//%%% option to sort on other fields could be added
		state.setAttribute (SORTED_BY, SORTED_BY_FIELD_CODE);
		String asc = Boolean.TRUE.toString ();
		state.setAttribute (SORTED_ASC, asc);
		
		//state.setAttribute(STATE_CODES_LIST, codes);
		state.setAttribute(STATE_MODE, MODE_LIST_CODES);
		
	}//doList_codes
	
	/*  
	* Check path for steps with identical instructions (i.e., duplicates).
	*/
	public String checkDuplicateSteps(CandidatePath path)
	{
		String msg = null;
		String ref = null;
		StepStatus status = null;
		boolean personal = false;

		//get the student's name
		String name = "";
		try
		{
			//path.getCandidate() returns User id
			name = ((User)UserDirectoryService.getUser(path.getCandidate())).getDisplayName();
		}
		catch(Exception e){}
		
		//Set has no dups
		Set set = new HashSet();

		//Hashtable might have dups
		Hashtable steps = path.getOrderedStatus();
		
		//add step description to Set
		for (int i = 1; i <= steps.size(); i++)
		{
			ref = (String)steps.get(i + "");
			try
			{
				status = DissertationService.getStepStatus(ref);
				
				//we'll call two steps with the same instructions duplicates
				set.add((String)status.getInstructions());
				if(((String)status.getParentStepReference()).equals("-1"))
				{
					//if there are any personal steps,flag this path
					personal = true;
				}
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
					Log.warn("chef", ".checkDuplicateSteps() getStepStatus(" + ref + ") " + path.getCandidate() + " " + e);
			}
		}
		
		//now see if Set has fewer items than Hashtable and also personal steps
		try
		{
			if((set.size() < steps.size()))
			{
				if(personal)
				{
					//flag as having personal steps
					msg = path.getCandidate() + " " + name + " path id " + path.getId() + " *\r";
				}
				else
				{
					msg = path.getCandidate() + " " + name + " path id " + path.getId() + "\r";
				}
			}
		}
		catch(Exception e)
		{
			if(Log.isWarnEnabled())
				Log.warn("chef", ".checkDuplicateSteps() " + path.getCandidate() + " " + name + " path id " + path.getId() + " " + e);
		}
		return msg;
		
	}//checkDuplicateSteps
	
	/**
	* Handle a request to submit changes to Block Grant Group(BGG) and Field of Study(FOS) codes and names.
	*/
	public void doAdd_code (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		Field field = (Field)state.getAttribute(STATE_FIELD_TO_ADD);
		
		 //check that there is a department site matching the BGG code
		 String id = "diss" + field.getGroupCode();
		 if(existsSite(id))
		 {
			/* if the block grant group doesn't exist, create it and add the field
			 * otherwise, get it and add the field
			*/
			
			String groupReference = null;
			boolean groupExists = false;
			
			//see if it exists
			List codes = (List)state.getAttribute(STATE_CODES_LIST);
			for (ListIterator i = codes.listIterator(); i.hasNext(); )
			{
				BlockGrantGroup tempGroup = (BlockGrantGroup) i.next();
				if(tempGroup.getCode().equals(field.getGroupCode()))
				{
					groupExists = true;
					groupReference = tempGroup.getReference();
				}
			}
			BlockGrantGroupEdit edit = null;
			String currentSite = null;
			
			//if it doesn't exist, add it
			if(!groupExists)
			{
				try
				{
					//currentSite = PortalService.getCurrentSiteId();
					currentSite = ToolManager.getCurrentPlacement().getContext();
					edit = DissertationService.addBlockGrantGroup(currentSite);
					edit.setCode(field.getGroupCode());
					edit.setDescription(field.getGroupName());
					edit.addFieldOfStudy(field.getFieldCode(), field.getFieldName());
					DissertationService.commitEdit(edit);
				}
				catch(Exception e)
				{
					if(Log.isWarnEnabled())
					{
						Log.warn("chef", this + ".doAdd_new_code addBlockGrantGroup(" + currentSite + ")" + e);
					}
				}
			}
			else
			{
				//otherwise, edit it
				try
				{
					edit = DissertationService.editBlockGrantGroup(groupReference);
					edit.setCode(field.getGroupCode());
					edit.setDescription(field.getGroupName());
					edit.addFieldOfStudy(field.getFieldCode(), field.getFieldName());
					DissertationService.commitEdit(edit);
				}
				catch(Exception e)
				{
					if(Log.isWarnEnabled())
					{
						Log.warn("chef", this + ".doAdd_new_code editBlockGrantGroup(" + groupReference + ")" + e);
					}
				}
			}
		 }
		 else
		 {
			addAlert(state, "Cannot add BGG " + field.getGroupCode() + ". There is no department site with id " + id + ".");
		 }

		state.setAttribute(STATE_MODE, MODE_LIST_CODES);
		
		//clear the field to add
		state.removeAttribute(STATE_FIELD_TO_ADD);
		
		//clear the form values
		state.removeAttribute(STATE_FOS_CODE);
		state.removeAttribute(STATE_FOS_NAME);
		state.removeAttribute(STATE_BGG_CODE);
		state.removeAttribute(STATE_BGG_NAME);
		state.removeAttribute(STATE_BGG_GROUP);
		
	}//doAdd_code 
	
	/**
	* Handle a request to revise changes to Block Grant Group(BGG) and Field of Study(FOS) codes and names.
	*/
	public void doRevise_code (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_REVISE_CODE);
		
	}//doRevise_new_code 
	
	/**
	* Handle a request to confirm removal of FOS code(s).
	*/
	public void doConfirm_remove_codes (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_CONFIRM_REMOVE_CODES);
		
	}//doConfirm_remove_codes
	
	/**
	* Handle a request to confirm removal of student(s)
	*/
	public void doConfirm_remove_students(RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		Vector studentsToRemove = new Vector();
		Vector studentsCannotRemove = new Vector();
		String name = null;
		String msg = null;
		String uniqnames = null;
		CandidatePath path = null;
		
		//get student uniqname(s) that were input
		uniqnames = StringUtil.trimToNull((params.getString("uniqnames")).toLowerCase());
		
		//check that there is something with which to work
		if (uniqnames == null)
		{
			addAlert(state, "Please enter student(s)' uniqnames.");
			state.setAttribute(STATE_MODE, MODE_REMOVE_STUDENTS);
			return;
		}
		
		//parse it
		String[] names = uniqnames.replaceAll(",","\r\n").split("\r\n");
		
		//check that uniqname fetches path
		if(names != null && names.length != 0)
		{
			for (int i = 0; i < names.length; i++)
			{
				try
				{
					name = names[i];
		
					//good uniqnames are 3-8 characters long
					if(name.length() > 2 && name.length() < 9)
					{
						path = DissertationService.getCandidatePathForCandidate(name);
						if(path != null)
							studentsToRemove.add(name);
						else
							studentsCannotRemove.add(name + ": has no or multiple path(s) ");
					}
					else
					{
						studentsCannotRemove.add(name + ": uniqname is not between 3 and 8 characters ");
					}
				}
				catch(Exception e)
				{
					msg = msg + "," + name;
				}
			}
			if(msg != null)
				addAlert(state, "Validation error for name(s): " + msg);
		}
		
		//put both lists in state
		state.setAttribute(STATE_STUDENTS_TO_REMOVE,studentsToRemove);
		state.setAttribute(STATE_STUDENTS_CANNOT_REMOVE,studentsCannotRemove);
		
		//get confirmation
		state.setAttribute(STATE_MODE, MODE_CONFIRM_REMOVE_STUDENTS);
		
	}//doConfirm_remove_students
	
	/**
	* Handle a confirmation to remove student(s).
	*/
	public void doRemove_students_confirmed (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		
		// get parameters
		ParameterParser params = data.getParameters();
		String option = params.getString("option");
		
		//if Cancel
		if (option.equalsIgnoreCase("cancel"))
		{
			doCancel_remove_students(data);
			return;
		}
		
		String msg = "";
		List paths = new Vector();
		List candidates = new Vector();
		
		//TODO the template to use for static checklist
		//String template = getStaticTemplate();
		String template = "";
		
		//paths for which a snapshot was made or no site exists
		paths = getPathsToRemove(state, template);
		
		//get candidates associates with paths
		candidates = getCandidatesToRemove(paths);
		
		//remove paths, step status, and candidate info for these students
		removeStudents(state, paths, candidates);
		
		//get messages from state
		if(state.getAttribute(STATE_STUDENT_REMOVAL_MESSAGES)!=null)
			msg = (String)state.getAttribute(STATE_STUDENT_REMOVAL_MESSAGES);
		if(!msg.equals(""))
			addAlert(state, "There was a problem removing " + msg);
		else
			addAlert(state, "Selected student(s) have been removed.");
		
		state.removeAttribute(STATE_STUDENT_REMOVAL_MESSAGES);
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	}//doRemove_students_confirmed
	
	/**
	* Handle a request to remove FOS code(s) and associated data.
	*/
	public void doRemove_codes (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		String[] selectedFields = params.getStrings("selectedfields");
		
		//make sure that we have at least one checkbox checked
		if(selectedFields==null || selectedFields.length == 0)
		{
			addAlert(state, "Please check the item to remove.");
			state.setAttribute(STATE_MODE, MODE_LIST_CODES);
			return;
		}
		else
		{
			state.setAttribute(STATE_FIELDS_TO_REMOVE, selectedFields);
			state.setAttribute(STATE_MODE, MODE_CONFIRM_REMOVE_CODES);
		}
	
	}//doRemove_codes
	
	/**
	* Handle a Rackham administrator's request to remove student(s)' data
	*/
	public void doRemove_students (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_REMOVE_STUDENTS);
		
	}//doRemove_students
	
	
	/**
	* Handle a request to remove FOS code(s) and associated data.
	*/
	public void doRemove_codes_confirmed (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		// %%% need to remove any CandidateInfo's and CandidatePath's for students in removed field
		
		String[] fields = (String[])state.getAttribute(STATE_FIELDS_TO_REMOVE);
		BlockGrantGroup group = null;
		
		// for each field in fields
		for(int i = 0; i < fields.length; i++)
		{
			//    get BlockGrantGroup for field
			try
			{
				group = DissertationService.getBlockGrantGroupForFieldOfStudy(fields[i]);
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
				{
					Log.warn("chef", this + ".doRemove_codes_confirmed getBlockGrantGroupForFieldOfStudy(" + fields[i] + ")" + e);
				}
			}
			//		get BlockGrantGroupEdit edit
			try
			{
				//	remove field from fields of study
				BlockGrantGroupEdit edit = DissertationService.editBlockGrantGroup(group.getReference());
				edit.removeFieldOfStudy(fields[i]);
				
				// if the last FOS is removed, remove the BGG too
				Hashtable remaining = edit.getFieldsOfStudy();
				if(remaining.isEmpty())
				{
					DissertationService.removeBlockGrantGroup(edit);
				}
				else
				{
					DissertationService.commitEdit(edit);
				}
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
				{
					Log.warn("chef", this + ".doRemove_codes_confirmed editBlockGrantGroup(" + group.getReference() + ")" + e);
				}
			}
		}
		state.setAttribute(STATE_MODE, MODE_LIST_CODES);
		
	}//doRemove_codes_confirmed
	
	/**
	* Handle a request to return to Upload mode.
	*/
	public void doDone_edit_codes (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.removeAttribute(STATE_CODES_LIST);
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	}//doDone_edit_codes
	
	/**
	* Handle a request to edit FOS or BGG name(s).
	*/
	public void doEdit_names (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String selected = null;
		String[] selectedFields = params.getStrings("selectedfields");
		
		//make sure that we have one and only one checkbox checked
		if(selectedFields==null || selectedFields.length == 0)
		{
			addAlert(state, "Please check the item to edit.");
			state.setAttribute(STATE_MODE, MODE_LIST_CODES);
			return;
		}
		else if(selectedFields.length != 1)
		{
			addAlert(state, "Please check one item to edit.");
			state.setAttribute(STATE_MODE, MODE_LIST_CODES);
			return;
		}
		else if(selectedFields.length == 1)
		{
			selected = selectedFields[0];
			state.setAttribute(STATE_FIELD_TO_EDIT, selected);
			state.setAttribute(STATE_MODE, MODE_EDIT_NAMES);
		}

	}//doEdit_names
	
	/**
	* Handle a request to add a BGG or FOS code.
	*/
	public void doNew_code (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_NEW_CODE);
		
	}//doNew_code
	
	/**
	* Handle a request to preview added BGG or FOS code.
	*/
	public void doPreview_code (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String FOS_code = params.getString ("FOS_code");
		String FOS_name = params.getString ("FOS_name");
		String BGG_code = params.getString ("BGG_code");
		String BGG_name = params.getString ("BGG_name");
		String msg = null;
		
		if(FOS_code==null || FOS_name==null || BGG_code==null || BGG_name==null 
			|| FOS_code.equals("") || FOS_name.equals("") || BGG_code.equals("") || BGG_name.equals(""))
		{
			msg = "Please provide missing field(s): ";
		}
		
		if(FOS_code==null || FOS_code.equals(""))
		{
			msg = msg + " FOS code ";
		}
		if(FOS_name==null || FOS_name.equals(""))
		{
			msg = msg + " FOS name ";
		}
		if(BGG_code==null || BGG_code.equals(""))
		{
			msg = msg + " BGG code ";
		}
		if(BGG_name==null || BGG_name.equals(""))
		{
			msg = msg + " BGG name ";
		}
		if(msg == null)
		{
			Field field = new Field();
			field.setFieldCode(FOS_code);
			field.setFieldName(FOS_name);
			field.setGroupCode(BGG_code);
			field.setGroupName(BGG_name);
			state.setAttribute(STATE_FIELD_TO_ADD, field);
			state.setAttribute(STATE_MODE, MODE_PREVIEW_CODE);
		}
		else
		{
			addAlert(state, msg);
			state.setAttribute(STATE_MODE, MODE_NEW_CODE);
		}
		
	}//doPreview_code
	
	
	/**
	* Handle a request to Continue to load with the extract file(s).
	*/
	public void doContinue_load (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		
		// get parameters
		ParameterParser params = data.getParameters();
		String option = params.getString("option");
		
		//if Cancel
		if (option.equalsIgnoreCase("cancel"))
		{
			doCancel_load(data);
			return;
		}

		//don't start an upload if upload or step change one is in progress
		if(DissertationService.isLoading())
		{
			addAlert(state, "Data is being loaded. Please wait until loading finishes to start loading again.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			return;
		}
		if(DissertationService.isChangingStep())
		{
			addAlert(state, "Checklist steps are being changed. Please try uploading later.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			return;
		}
		state.setAttribute(STATE_MODE, MODE_CONFIRM_UPLOAD);
		
		//a dummy byte[] to stand in for a missing file
		byte[] missing = new byte[1];
		missing[0] = 1;
		
		boolean MP = false;
		boolean OARD = false;
		String oardContent = null;
		String mpContent = null;
		String msg = null;
		//String currentSite = PortalService.getCurrentSiteId();
		String currentSite = ToolManager.getCurrentPlacement().getContext();
		
		//get the content of the files uploaded
		if(state.getAttribute(STATE_OARD_CONTENT_STRING)!= null)
			oardContent = (String)state.getAttribute(STATE_OARD_CONTENT_STRING);
		if(state.getAttribute(STATE_MP_CONTENT_STRING)!= null)
			mpContent = (String)state.getAttribute(STATE_MP_CONTENT_STRING);
		
		//set flags based on content in files
		MP = (mpContent != null && mpContent.length()!=0) ? true:false;
		OARD = (oardContent != null && oardContent.length()!=0) ? true:false;
		
		//upload the data
		try
		{
			if(MP && OARD)
				msg = DissertationService.executeUploadExtractsJob(currentSite, oardContent.getBytes(), mpContent.getBytes());
			else if (MP)
				msg = DissertationService.executeUploadExtractsJob(currentSite, missing, mpContent.getBytes());
			else if (OARD)
				msg = DissertationService.executeUploadExtractsJob(currentSite, oardContent.getBytes(), missing);
			else
			{
				//neither file had content
				addAlert(state, "Both files are missing or missing content.");
				state.setAttribute(STATE_MODE, MODE_UPLOAD);
			}
		}
		catch(Exception e)
		{
			//advertise exception kicking off job
			addAlert(state, "Problem starting upload job " + e.toString());
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
		}
		finally
		{
			//instruct user where to look for job execution report
			if(msg != null)
				addAlert(state, msg);
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			
			//remove content from state
			state.removeAttribute(STATE_OARD_CONTENT_STRING);
			state.removeAttribute(STATE_MP_CONTENT_STRING);
		}
	
	} // doContinue_load

	/**
	* Handle a request to Cancel loading these OARD/MP extract files. Called from Confirm Uploads page.
	*/
	public void doCancel_load (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.removeAttribute(STATE_OARDFILE);
		state.removeAttribute(STATE_MPFILE);
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	}	// doCancel_load
	
	/**
	* Handle a request to Cancel removing student(s). Called from Confirm Removal page.
	*/
	public void doCancel_remove_students (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		state.removeAttribute(STATE_STUDENTS_TO_REMOVE);
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
	}	// doCancel_remove_students
	
	/**
	* Get the collection of Checklist Section Headings for display.
	* @return Vector of ordered String objects, one for each section head.
	*/
	private Vector getSectionHeads()
	{
		//TODO move to service so both tools may use
		Vector headers = new Vector();
		headers.add("None");
		headers.add(DissertationService.CHECKLIST_SECTION_HEADING1);
		headers.add(DissertationService.CHECKLIST_SECTION_HEADING2);
		headers.add(DissertationService.CHECKLIST_SECTION_HEADING3);
		headers.add(DissertationService.CHECKLIST_SECTION_HEADING4);
		return headers;
		
	}//getSectionHeadings
	
	/**
	* Get the collection of display objects corresponding to a CandidatePath for inserting into a velocity template.
	* @param path The CandidatePath.
	* @param state The SessionState.
	* @param committee Is this for a committee member's page?
	* @return The collection of Template Steps.
	*/
	private TemplateStep[] getTemplateSteps(CandidatePath path, SessionState state, boolean committee)
	{
		Hashtable order = path.getOrderedStatus();
		String ref = null;
		int numberOfSteps = order.size();
		StepStatus status = null;
		Time timeCompleted = null;
		String timeCompletedText;
		String keyString = null;
		TemplateStep[] templateSteps = new TemplateStep[numberOfSteps];
		for(int x = 1; x < (numberOfSteps+1); x++)
		{
			try
			{
				keyString = "" + x;
				ref = (String)order.get(keyString);
				status = DissertationService.getStepStatus(ref);
				DissertationStep parent = null;
				
				//If not a personal step, get section id of the parent DissertationStep for StepStatus TemplateStep
				if(!((String)status.getParentStepReference()).equals("-1"))
				{
					try
					{
						parent = DissertationService.getDissertationStep(status.getParentStepReference());
					}
					catch(IdUnusedException e)
					{
						if(Log.isWarnEnabled())
							Log.warn("chef", this + "getTemplateSteps get section id of the parent for path " + path.getId() + " " + e);
					}
				}
				
				templateSteps[x-1] = new TemplateStep();

				//in DissertationAction value depends on whether student or committee
				templateSteps[x-1].setShowCheckbox(true);
				
				templateSteps[x-1].setStatusReference(status.getReference());
				templateSteps[x-1].setInstructions(status.getInstructions());
				templateSteps[x-1].setValidationImage(status.getValidationType());
				
				// FIND THE STEP STATUS
				templateSteps[x-1].setStatus(path.getStatusStatus(status));
				templateSteps[x-1].setPrereqs(path.getPrerequisiteStepsDisplayString(status));
				timeCompleted = status.getTimeCompleted();
				timeCompletedText = status.getTimeCompletedText();
				if(timeCompleted != null)
				{
					if((timeCompletedText==null) || (timeCompletedText.equals("")))
					{
						timeCompletedText = timeCompleted.toStringLocalDate();
					}
					else
					{
						//add supplemental text (e.g., term) to completion date
						timeCompletedText = timeCompletedText + " : " + timeCompleted.toStringLocalDate();
					}
					templateSteps[x-1].setTimeCompleted(timeCompletedText);
				}
				//set additional auxiliary text if any
				if(status.getAuxiliaryText() != null && status.getAuxiliaryText().size() != 0)
				{
					templateSteps[x-1].setAuxiliaryText(status.getAuxiliaryText());
				}

				//DissertationStep section is available through StepStatus parentstepreference
				if(parent!=null)
				{	
					try
					{
						templateSteps[x-1].setSection(Integer.parseInt(parent.getSection()));
					}
					catch(NumberFormatException e)
					{
						Log.warn("chef", this + "getTemplateSteps setSection() for path " + path.getId() + " " + e);
					}
					catch(Exception e)
					{
						Log.warn("chef", this + "getTemplateSteps setSection() for path " + path.getId() + " " + e);
					}
					//earlier steps did not have a section, so deal with missing section
				}
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
					Log.warn("chef", this + ".getTemplateSteps " + "step " + keyString + " " + e);
			}
		}
		return templateSteps;
		
	}//getTemplateSteps
	
	/**
	* the GroupComparator class
	*/
	private class GroupComparator
		implements Comparator
	{
		/**
		 * the criteria
		 */
		String m_criterion = null;
		String m_asc = null;
		
		/**
		 * constructor
		 * @param criteria The sort criteria string
		 * @param asc The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 */
		public GroupComparator (String criterion, String asc)
		{
			m_criterion = criterion;
			m_asc = asc;
			
		}	// constructor
		
		/**
		* implementing the Comparator compare function
		* @param o1 The first object
		* @param o2 The second object
		* @return The compare result. 1 is o1 < o2; 0 is o1.equals(o2); -1 otherwise
		*/
		public int compare ( Object o1, Object o2)
		{
			int result = -1;
			
			if(m_criterion==null) m_criterion = SORTED_BY_GROUP_NAME;
			
			/************* for sorting group list *******************/
			if (m_criterion.equals (SORTED_BY_GROUP_NAME))
			{
				// sorted by the String Block Grant Group Description
				String f1 = ((BlockGrantGroup) o1).getDescription();
				String f2 = ((BlockGrantGroup) o2).getDescription();
				
				if (f1==null && f2==null)
					result = 0;
				else if (f2==null)
					result = 1;
				else if (f1==null)
					result = -1;
				else
					result =  f1.compareToIgnoreCase (f2);
			}
			else if (m_criterion.equals (SORTED_BY_GROUP_CODE))
			{
				// sorted by the String student uniqname
				String f1 = ((BlockGrantGroup) o1).getCode();
				String f2 = ((BlockGrantGroup) o2).getCode();
				if (f1==null && f2==null)
					result = 0;
				else if (f2==null)
					result = 1;
				else if (f1==null)
					result = -1;
				else
					result = f1.compareToIgnoreCase (f2);
			}
			if(m_asc == null) m_asc = Boolean.TRUE.toString ();
			
			// sort ascending or descending
			if (m_asc.equals (Boolean.FALSE.toString ()))
				result = -result;
			return result;
			
		}	// compare
		
	} //GroupComparator
	
	/**
	* the FieldComparator class
	*/
	private class FieldComparator
		implements Comparator
	{
		/**
		 * the criteria
		 */
		String m_criterion = null;
		String m_asc = null;
		
		/**
		 * constructor
		 * @param criteria The sort criteria string
		 * @param asc The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 */
		public FieldComparator (String criterion, String asc)
		{
			m_criterion = criterion;
			m_asc = asc;
			
		}	// constructor
		
		/**
		* implementing the Comparator compare function
		* @param o1 The first object
		* @param o2 The second object
		* @return The compare result. 1 is o1 < o2; 0 is o1.equals(o2); -1 otherwise
		*/
		public int compare ( Object o1, Object o2)
		{
			int result = -1;
			
			if(m_criterion==null) m_criterion = SORTED_BY_FIELD_CODE;
			
			/************* for sorting site list *******************/
			if (m_criterion.equals (SORTED_BY_FIELD_CODE))
			{
				// sorted by the String student name
				String f1 = ((Field) o1).getFieldCode();
				String f2 = ((Field) o2).getFieldCode();
				if (f1==null && f2==null)
					result = 0;
				else if (f2==null)
					result = 1;
				else if (f1==null)
					result = -1;
				else
					result =  f1.compareToIgnoreCase (f2);
			}
			else if (m_criterion.equals (SORTED_BY_GROUP_CODE))
			{
				// sorted by the String student uniqname
				String f1 = ((Field) o1).getGroupCode();
				String f2 = ((Field) o2).getGroupCode();
				if (f1==null && f2==null)
					result = 0;
				else if (f2==null)
					result = 1;
				else if (f1==null)
					result = -1;
				else
					result = f1.compareToIgnoreCase (f2);
			}
			else if (m_criterion.equals (SORTED_BY_FIELD_NAME))
			{
				// sorted by the String U-M id
				String f1 = ((Field) o1).getFieldName();
				String f2 = ((Field) o2).getFieldName();
				if (f1==null && f2==null)
					result = 0;
				else if (f2==null)
					result = 1;
				else if (f1==null)
					result = -1;
				else
					result = f1.compareToIgnoreCase (f2);
			}
			else if (m_criterion.equals (SORTED_BY_GROUP_NAME))
			{
				// sorted by the String course level
				String f1 = ((Field) o1).getGroupName();
				String f2 = ((Field) o2).getGroupName();
				if (f1==null && f2==null)
					result = 0;
				else if (f2==null)
					result = 1;
				else if (f1==null)
					result = -1;
				else
					result = f1.compareToIgnoreCase (f2);
			}
			
			if(m_asc == null) m_asc = Boolean.TRUE.toString ();
			
			// sort ascending or descending
			if (m_asc.equals (Boolean.FALSE.toString ()))
				result = -result;
			return result;
			
		}	// compare
		
	} //FieldComparator
	
	/** Class that holds all the information for display in a velocity template. */
	public class Field
	{
		private String m_fieldCode;
		private String m_fieldName;
		private String m_groupCode;
		private String m_groupName;
		
		//construct
		public Field()
		{
			m_fieldCode = "";
			m_fieldName = "";
			m_groupCode = "";
			m_groupName = "";
		}
		
		//getters
		public String getFieldCode()
		{
			return m_fieldCode;
		}
		public String getFieldName()
		{
			return m_fieldName;
		}
		public String getGroupCode()
		{
			return m_groupCode;
		}
		public String getGroupName()
		{
			return m_groupName;
		}
		
		//setters
		public void setFieldCode(String fieldCode)
		{
			m_fieldCode = fieldCode;
		}
		public void setFieldName(String fieldName)
		{
			m_fieldName = fieldName;
		}
		public void setGroupCode(String groupCode)
		{
			m_groupCode = groupCode;
		}
		public void setGroupName(String groupName)
		{
			m_groupName = groupName;
		}
		
	}//Field
	
	/** Class that holds all the information for display in a velocity template. */
	public class TemplateStep
	{
		private String stepId;
		private String stepReference;
		private String statusReference;
		private String instructions;
		private String validationImage;
		private String status;
		private String prereqs;
		private String timeCompleted;
		private boolean showCheckbox;
		private String links;
		private int section;
		private List auxiliaryText;
		
		public TemplateStep()
		{
			stepId = "";
			stepReference = "";
			statusReference = "";
			instructions = "";
			validationImage = "";
			status = "";
			prereqs = "";
			timeCompleted = "";
			auxiliaryText = null;
			links = "";
			showCheckbox = true;
			section = 0;
		}
		
		public List getAuxiliaryText()
		{
			return auxiliaryText;
		}
		
		public void setAuxiliaryText(List text)
		{
			auxiliaryText = text;
		}

		public int getSection()
		{
			return section;
		}
		
		public void setSection(int s)
		{
			section = s;
		}
		
		public String getStepId()
		{
			return stepId;
		}
		
		public void setStepId(String id)
		{
			stepId = id;
		}

		public String getStepReference()
		{
			return stepReference;
		}
		
		public void setStepReference(String ref)
		{
			stepReference = ref;
		}

		public String getStatusReference()
		{
			return statusReference;
		}
		
		public void setStatusReference(String ref)
		{
			statusReference = ref;
		}

		public String getInstructions()
		{
			String retVal = instructions;
			if(instructions.indexOf("%links%") != -1)
			{
				retVal = retVal.replaceFirst("%links%", getLinks());
			}
			return retVal;
		}

		public void setInstructions(String i)
		{
			if(i != null)
				instructions = i;
		}

		public String getValidationImage()
		{
			return validationImage;
		}

		public void setValidationImage(String type)
		{
			if(type != null)
				validationImage = "sakai/diss_validate" + type + ".gif";
		}

		public String getStatus()
		{
			return status;
		}
		
		public void setStatus(String s)
		{
			if(s != null)
				status = s;
		}
		
		public String getPrereqs()
		{
			return prereqs;
		}
		
		public void setPrereqs(String p)
		{
			if(p != null)
				prereqs = p;
		}
		
		public String getTimeCompleted()
		{
			return timeCompleted;
		}
		
		public void setTimeCompleted(String time)
		{
			if(time != null)
				timeCompleted = time;
		}
		
		public boolean showCheckbox()
		{
			return showCheckbox;
		}
		
		public void setShowCheckbox(boolean show)
		{
			showCheckbox = show;
		}
		
		public void setLinks(String newLinks)
		{
			if(newLinks != null)
				links = newLinks;
		}
		
		public String getLinks()
		{
			return links;
		}
		
	}//TemplateStep
	
	/** Class that starts a session to create content on another site. */
	protected class Snapshot implements Runnable
	{
		String m_id = null;
		String m_checklist = null;
	
		public void init(){}
		public void start(){}
		
		//constructor
		Snapshot(String id, String s)
		{
			//student's site
			m_id = id;
			
			//student's static checklist
			m_checklist = s;
		}

		public void run()
		{
		    try
			{
				// set the current user to admin
				Session s = SessionManager.getCurrentSession();
				if (s != null)
				{
					s.setUserId(UserDirectoryService.ADMIN_ID);
				}
				else
				{
					Log.warn("chef", this + ".run() - Session is null, cannot set user id to ADMIN_ID user");
				}

				if(m_id != null)
				{
					ContentResourceEdit edit = null;
					String collectionId = null;
					if(m_checklist != null)
					{
						//check student's Resources collection is accessible
						collectionId = ContentHostingService.getSiteCollection(m_id);
						try
						{
							ContentHostingService.checkCollection(collectionId);
							
							//save the student's checklist in Resources
							try
							{
								edit = ContentHostingService.addResource(collectionId + DissertationService.STATIC_CHECKLIST_NAME);
								edit.setContent(m_checklist.getBytes());
								edit.setContentType("text/html");
								edit.setContentLength(m_checklist.length());
								ResourcePropertiesEdit props = edit.getPropertiesEdit();
								props.addProperty(ResourceProperties.PROP_DISPLAY_NAME,DissertationService.STATIC_CHECKLIST_DISPLAY_NAME);
								props.addProperty(ResourceProperties.PROP_DESCRIPTION, DissertationService.STATIC_CHECKLIST_DESCRIPTION);
								ContentHostingService.commitResource(edit);
							}
							catch(Exception e)
							{
								if(edit != null && edit.isActiveEdit())
								{
									try
									{
										ContentHostingService.removeResource(edit);
									}
									catch(Exception ee)
									{
										if(Log.isWarnEnabled())
											Log.warn("chef", this + ".run removeResource " + e);
									}
								}
								if(Log.isWarnEnabled())
									Log.warn("chef", this + ".run addResource " + collectionId + DissertationService.STATIC_CHECKLIST_NAME + " " + e);
							}
						}
						catch (Exception e)
						{
							if(Log.isWarnEnabled())
								Log.warn("chef", this + ".run checkCollection " + collectionId + " " + e);
						}
					}
				}
		    }
		    finally
			{
				//clear any current bindings
				ThreadLocalManager.clear();
			}
		}
		
	}//Snapshot

}	// DissertationUploadAction
/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/tool/src/java/org/sakaiproject/tool/dissertation/DissertationUploadAction.java,v 1.2 2005/05/12 23:49:05 ggolden.umich.edu Exp $
*
**********************************************************************************/
