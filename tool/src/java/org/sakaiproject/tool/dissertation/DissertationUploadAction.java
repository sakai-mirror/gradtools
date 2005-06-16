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

// imports
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import org.sakaiproject.api.app.dissertation.BlockGrantGroup;
import org.sakaiproject.api.app.dissertation.BlockGrantGroupEdit;
import org.sakaiproject.api.app.dissertation.cover.DissertationService;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
import org.sakaiproject.cheftool.menu.Menu;
import org.sakaiproject.cheftool.menu.MenuEntry;
import org.sakaiproject.cheftool.menu.MenuItem;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.service.framework.portal.cover.PortalService;
import org.sakaiproject.service.framework.session.SessionState;
import org.sakaiproject.service.legacy.site.Site;
import org.sakaiproject.service.legacy.site.cover.SiteService;
import org.sakaiproject.util.FileItem;
import org.sakaiproject.util.ParameterParser;
import org.sakaiproject.util.SortedIterator;

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
	private final static String  MODE_REMOVE_CODES = "remove_codes";
	private final static String  MODE_REMOVE_CODES_CONFIRMED = "remove_codes_confirmed";
	private final static String  MODE_CONFIRM_REMOVE_CODES = "confirm_remove_codes";
	
	/** The templates */
	private final static String  TEMPLATE_UPLOAD = "_upload";
	private final static String  TEMPLATE_CONFIRM_UPLOAD = "_confirm_upload";
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
		if(!DissertationService.getSchoolSite().equals(PortalService.getCurrentSiteId()))
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
		Menu bar = new Menu(portlet, rundata, (String) state.getAttribute(STATE_ACTION));
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
		
	}//buildPreviewNewCodeContext
	
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
		
	}//buildEditNamesContext
	
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
		
	}//getFieldName
	
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
	* Build the context for the file upload form.
	*/
	private String buildUploadContext(VelocityPortlet portlet, RunData rundata, SessionState state, Context context)
	{
		// menu bar
		Menu bar = new Menu(portlet, rundata, (String) state.getAttribute(STATE_ACTION));
		bar.add( new MenuEntry("Show...", "doShow_setting"));
		bar.add( new MenuEntry("Edit Codes", "doList_codes"));
		context.put("menu", bar);
		return TEMPLATE_UPLOAD;

	}	// buildUpLoadContext


	/**
	* Build the context for file upload confirmation.
	*/
	private String buildConfirmUploadContext(SessionState state, Context context)
	{
		FileItem OARDFileItem = null;
		FileItem MPFileItem = null;
		
		//get the FileItems from state
		if(state.getAttribute(STATE_OARDFILE)!=null)
		{
			OARDFileItem = (FileItem)state.getAttribute(STATE_OARDFILE);
		}
		if(state.getAttribute(STATE_MPFILE)!=null)
		{
			MPFileItem = (FileItem)state.getAttribute(STATE_MPFILE);
		}
		
		//check that FileItems aren't null
		if(OARDFileItem == null && MPFileItem == null)
		{
			addAlert(state, "OARD and MP extract files are null. Please contact GradTools Support if you have questions.");
			return TEMPLATE_UPLOAD;
		}
		else if(OARDFileItem == null)
		{
			addAlert(state, "Did you mean to omit the OARD extract file?");
		}
		else if (MPFileItem == null)
		{
			addAlert(state, "Did you mean to omit the MP extract file?");
		}
		
		//display properties of the data files for confirmation
		try
		{
			if(OARDFileItem != null)
			{
				String OARDContentString = OARDFileItem.getString();
				state.setAttribute(STATE_OARD_CONTENT_STRING, OARDContentString);
				
				Hashtable OARDProps = DissertationService.getDataFileProperties(OARDContentString.getBytes());
				context.put("OARDLines", ((Integer)OARDProps.get(DissertationService.DATAFILE_LINES)).toString());
				context.put("OARDFileName", (String)OARDFileItem.getFileName());
				context.put("OARDContentType", (String)OARDFileItem.getContentType());
			}
			if(MPFileItem != null)
			{
				String MPContentString = MPFileItem.getString();
				state.setAttribute(STATE_MP_CONTENT_STRING, MPContentString);
				
				Hashtable MPProps = DissertationService.getDataFileProperties(MPContentString.getBytes());
				context.put("MPLines", ((Integer)MPProps.get(DissertationService.DATAFILE_LINES)).toString());
				context.put("MPFileName", (String)MPFileItem.getFileName());
				context.put("MPContentType", (String)MPFileItem.getContentType());
			}
		}
		catch (Exception e)
		{
			Log.warn("chef", this + ".buildConfirmUpLoadContext Exception caught displaying properties of the data files: " + e);
		}
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
		
		//if an upload is in progress, don't start another until it's finished
		if(DissertationService.isLoading())
		{
			addAlert(state, "Data loading is already in progress. Please wait until loading finishes to start loading again.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			return;
		}
		ParameterParser params = data.getParameters();
		state.setAttribute(STATE_MODE, MODE_UPLOAD);
		
		FileItem OARDFileItem = null;
		FileItem MPFileItem = null;

		//check for file items
		try
		{
			OARDFileItem = params.getFileItem ("OARD");
			MPFileItem = params.getFileItem ("MP");
		
			//if we don't have any files
			if(OARDFileItem == null && MPFileItem == null)
			{
				addAlert(state, "Both files are null.");
			}
			
			//if the file(s) don't contain plain text
			if((OARDFileItem != null) && !(OARDFileItem.getContentType().equals("text/plain")))
			{
				addAlert(state, OARDFileItem.getFileName() + " content type is not text/plain:  " + OARDFileItem.getContentType());	
			}
			if((MPFileItem != null) && !(MPFileItem.getContentType().equals("text/plain")))
			{
				addAlert(state, MPFileItem.getFileName() + " content type is not text/plain:  " + MPFileItem.getContentType());
			}
			
		}
		catch (Exception e)
		{
			Log.warn("chef", this + " .doUpload: Exception caught checking file items:  " + e);
			addAlert(state, "doUpload: A problem was encountered with the file(s):  " + e.getMessage());
		}
		
		//if no there are no alert messages, proceed to asking for confirmation of the load
		if(((String) state.getAttribute(STATE_MESSAGE)) == null)
		{
			//change mode to confirm upload
			state.setAttribute(STATE_MODE, MODE_CONFIRM_UPLOAD);
			
			//put files in state awaiting confirmation
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
	
	/**
	* Handle a request to submit changes to Block Grant Group(BGG) and Field of Study(FOS) codes and names.
	*/
	public void doAdd_code (RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
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
					currentSite = PortalService.getCurrentSiteId();
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
		
		//if an upload is in progress, don't start another until it's finished
		if(DissertationService.isLoading())
		{
			addAlert(state, "Data is being loaded. Please wait until loading finishes to start loading again.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
			return;
		}
		
		state.setAttribute(STATE_MODE, MODE_CONFIRM_UPLOAD);
		
		String OARDContentString = null;
		String MPContentString = null;
		
		//get file content from state
		if(state.getAttribute(STATE_OARD_CONTENT_STRING)!=null)
		{
			try
			{
				OARDContentString = (String)state.getAttribute(STATE_OARD_CONTENT_STRING);
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doContinue_load Exception caught getting OARDBytes: " + e);
			}
		}
		if(state.getAttribute(STATE_MP_CONTENT_STRING)!=null)
		{
			try
			{
				MPContentString = (String)state.getAttribute(STATE_MP_CONTENT_STRING);
				
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doContinue_load Exception caught getting MPBytes: " + e);
			}
		}
		if((OARDContentString == null || OARDContentString.length() == 0) && (MPContentString == null || MPContentString.length() == 0))
		{
			
			//put up an alert if both files are missing content
			addAlert(state, "Both files are missing or missing content.");
			state.setAttribute(STATE_MODE, MODE_UPLOAD);
		}
		else
		{
			byte[] missing = new byte[1];
			missing[0] = 1;
			List loadMessages = new Vector();
			
			//load the data, returning all error messages
			if(MPContentString == null || MPContentString.length() == 0)
			{
				//if MPathways data are missing
				try
				{
					loadMessages = DissertationService.loadData(OARDContentString.getBytes(), missing);
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".doContinue_load Exception caught loading OARDBytes: " + e);
				}
			}
			else if(OARDContentString == null || OARDContentString.length() == 0)
			{
				
				//if OARD data are missing
				try
				{
					loadMessages = DissertationService.loadData(missing, MPContentString.getBytes());
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".doContinue_load Exception caught loading MPBytes: " + e);
				}
			}
			else
			{
				//neither MPathways nor OARD data are missing
				try
				{
					loadMessages = DissertationService.loadData(OARDContentString.getBytes(), MPContentString.getBytes());
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".doContinue_load Exception caught loading OARDContentString.getBytes(), MPContentString.getBytes(): " + e);
				}
			}

			//the load was successful
			if(loadMessages == null || loadMessages.size() == 0)
			{
				addAlert(state, "The extract files have been loaded.");
				state.setAttribute(STATE_MODE, MODE_UPLOAD);
			}
			else
			{
				//the load was not successful
				boolean validationErrors = false;
				boolean loadingErrors = false;
				for (Iterator i = loadMessages.iterator(); i.hasNext(); )
				{
					//get the phase in which the error occurred
					String msg = (String) i.next();
					if(msg.equals("During validation of data:")) validationErrors = true;
					if(msg.equals("During loading of data:")) loadingErrors = true;
				}
				if(validationErrors)
				{
					addAlert(state, "Because of errors, no data were loaded.");
				}
				else if (loadingErrors) 
				{
					addAlert(state, "There were errors during loading of data. Please correct the data and load again.");
				}
				else
				{
					addAlert(state, "There were problems during validation and loading of data.");
				}
				state.setAttribute(STATE_LOAD_ERRORS, loadMessages);
				state.setAttribute(STATE_MODE, MODE_LOAD_ERRORS);
			}
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
				{
					result = 0;
				}
				else if (f2==null)
				{
					result = 1;
				}
				else if (f1==null)
				{
					result = -1;
				}
				else
				{
					result =  f1.compareToIgnoreCase (f2);
				}
			}
			else if (m_criterion.equals (SORTED_BY_GROUP_CODE))
			{
				// sorted by the String student uniqname
				String f1 = ((BlockGrantGroup) o1).getCode();
				String f2 = ((BlockGrantGroup) o2).getCode();
				if (f1==null && f2==null)
				{
					result = 0;
				}
				else if (f2==null)
				{
					result = 1;
				}
				else if (f1==null)
				{
					result = -1;
				}
				else
				{
					result = f1.compareToIgnoreCase (f2);
				}
			}
			if(m_asc == null) m_asc = Boolean.TRUE.toString ();
			
			// sort ascending or descending
			if (m_asc.equals (Boolean.FALSE.toString ()))
			{
				result = -result;
			}
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
				{
					result = 0;
				}
				else if (f2==null)
				{
					result = 1;
				}
				else if (f1==null)
				{
					result = -1;
				}
				else
				{
					result =  f1.compareToIgnoreCase (f2);
				}
			}
			else if (m_criterion.equals (SORTED_BY_GROUP_CODE))
			{
				// sorted by the String student uniqname
				String f1 = ((Field) o1).getGroupCode();
				String f2 = ((Field) o2).getGroupCode();
				if (f1==null && f2==null)
				{
					result = 0;
				}
				else if (f2==null)
				{
					result = 1;
				}
				else if (f1==null)
				{
					result = -1;
				}
				else
				{
					result = f1.compareToIgnoreCase (f2);
				}
			}
			else if (m_criterion.equals (SORTED_BY_FIELD_NAME))
			{
				// sorted by the String U-M id
				String f1 = ((Field) o1).getFieldName();
				String f2 = ((Field) o2).getFieldName();
				if (f1==null && f2==null)
				{
					result = 0;
				}
				else if (f2==null)
				{
					result = 1;
				}
				else if (f1==null)
				{
					result = -1;
				}
				else
				{
					result = f1.compareToIgnoreCase (f2);
				}
			}
			else if (m_criterion.equals (SORTED_BY_GROUP_NAME))
			{
				// sorted by the String course level
				String f1 = ((Field) o1).getGroupName();
				String f2 = ((Field) o2).getGroupName();
				if (f1==null && f2==null)
				{
					result = 0;
				}
				else if (f2==null)
				{
					result = 1;
				}
				else if (f1==null)
				{
					result = -1;
				}
				else
				{
					result = f1.compareToIgnoreCase (f2);
				}
			}
			
			if(m_asc == null) m_asc = Boolean.TRUE.toString ();
			
			// sort ascending or descending
			if (m_asc.equals (Boolean.FALSE.toString ()))
			{
				result = -result;
			}
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

}	// DissertationUploadAction
/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/tool/src/java/org/sakaiproject/tool/dissertation/DissertationUploadAction.java,v 1.2 2005/05/12 23:49:05 ggolden.umich.edu Exp $
*
**********************************************************************************/
