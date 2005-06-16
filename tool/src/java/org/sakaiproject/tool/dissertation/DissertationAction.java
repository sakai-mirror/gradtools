/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/tool/src/java/org/sakaiproject/tool/dissertation/DissertationAction.java,v 1.2 2005/05/28 03:05:07 ggolden.umich.edu Exp $
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

// import
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.sakaiproject.api.app.dissertation.CandidateInfo;
import org.sakaiproject.api.app.dissertation.CandidatePath;
import org.sakaiproject.api.app.dissertation.CandidatePathEdit;
import org.sakaiproject.api.app.dissertation.Dissertation;
import org.sakaiproject.api.app.dissertation.DissertationEdit;
import org.sakaiproject.api.app.dissertation.DissertationStep;
import org.sakaiproject.api.app.dissertation.DissertationStepEdit;
import org.sakaiproject.api.app.dissertation.StepStatus;
import org.sakaiproject.api.app.dissertation.StepStatusEdit;
import org.sakaiproject.api.app.dissertation.cover.DissertationService;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.PortletConfig;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
import org.sakaiproject.cheftool.menu.Menu;
import org.sakaiproject.cheftool.menu.MenuEntry;
import org.sakaiproject.cheftool.menu.MenuItem;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.service.framework.portal.cover.PortalService;
import org.sakaiproject.service.framework.session.SessionState;
import org.sakaiproject.service.framework.session.UsageSession;
import org.sakaiproject.service.framework.session.cover.UsageSessionService;
import org.sakaiproject.service.legacy.site.Site;
import org.sakaiproject.service.legacy.site.cover.SiteService;
import org.sakaiproject.service.legacy.time.Time;
import org.sakaiproject.service.legacy.time.cover.TimeService;
import org.sakaiproject.service.legacy.user.User;
import org.sakaiproject.service.legacy.user.cover.UserDirectoryService;
import org.sakaiproject.util.ParameterParser;


/**
* <p>DissertationAction is the administration action class for the Grad tool.</p>
*
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/

public class DissertationAction
	extends VelocityPortletPaneledAction
{
	/** The current session	*/
	private UsageSession session = null;
	
	/** The state mode	*/
	private static final String STATE_MODE = "Dissertation.mode";
	
	//** One time initialization of departmental Dissertation type attribute */
	private static final String STATE_INITIALIZE_DEPT_DISSERTATION_TYPE = "Dissertation.initialize.dept_dissertation_type";
	
//	** One time initialization of CandidatePath type attribute */
	private static final String STATE_INITIALIZE_CANDIDATE_PATH_TYPE = "Dissertation.initialize.candidate_path_type";
	
	/** One time initialization of DissertationStep section attribute */
	private static final String STATE_INITIALIZE_STEP_SECTION = "Dissertation.initialize.step.section";
	
	/** The site string */
	private static final String STATE_CURRENT_SITE = "Dissertation.current_site";

	/** The main school group id */
	private static final String STATE_SCHOOL_SITE = "Dissertation.school_site";
	
	/** The Music Performance group id */
	private static final String STATE_MUSIC_PERFORMANCE_SITE = "Dissertation.music_performance_site";

	/** The user */
	private static final String STATE_USER = "Dissertation.user";
	
	/** The user's role */
	private static final String STATE_USER_ROLE = "Dissertation.user.role";
	
	/** The current users display name */
	private static final String STATE_USER_DISPLAY_NAME = "Dissertation.users_display_name";

	/** The user's department full name for display */
	private static final String STATE_USERS_DEPT_FULL_NAME = "Dissertation.users_dept_full_name";

	/** Show Completed Steps in Checklist. */
	private static final String STATE_SHOW_COMPLETED_STEPS = "Dissertation.show_completed_steps";

	/** The current dissertation. */
	private static final String STATE_CURRENT_DISSERTATION_REFERENCE = "Dissertation.current_dissertation_reference";

	/** The dissertation step to be modified. */
	private static final String STATE_DISSERTATION_STEP = "Dissertation.dissertation_step";

	/** The current dissertation steps vector. */
	private static final String STATE_CURRENT_DISSERTATION_STEPS = "Dissertation.current_dissertation_steps";

	/** The array user-selected template steps */
	private static final String STATE_SELECTED_STEPS = "Dissertation.selected_steps";
	
	/** The currently selected candidate */
	private static final String STATE_SELECTED_CANDIDATE = "Dissertation.selected_candidate";

	/** The currently selected candidate */
	private static final String STATE_SELECTED_CANDIDATE_DISPLAY_NAME = "Dissertation.selected_candidate_display_name";

	/** The display name of the group of the currently selected candidate. */
	private static final String STATE_SELECTED_CANDIDATE_GROUP_DISPLAY_NAME = "Dissertation.selected_candidate_group_display_name";
	
	/** The ordered steps hashtable for a dissertation. */
	private static final String STATE_ORDERED_STEPS = "Dissertation.ordered_steps";
	
	/** The template to use when editing a step. */
	private static final String STATE_CURRENT_TEMPLATE = "Dissertation.current_template";	
	
	/** The type of dissertation steps, such as Dissertation Steps or Dissertation Steps: Music Performance. */
	private static final String STATE_DISSERTATION_TYPE = "Dissertation.dissertation_type";

	/** The valid prerequisites for a step. */
	private static final String STATE_PREREQUISITES = "Dissertation.prerequisites";

	/** The current prerequisites for a step. */
	private static final String STATE_CURRENT_PREREQUISITES = "Dissertation.current_prerequisites";

	/** The step instructions. */
	private static final String STATE_STEP_DESC = "Dissertation.step_desc";

	/** The step validation type. */
	private static final String STATE_STEP_VALIDATION_TYPE = "Dissertation.step_validation_type";

	/** The step's selected potential prereqs. */
	private static final String STATE_STEP_POTENTIAL_PREREQS = "Dissertation.step_potential_prereqs";

	/** The step's selected current prereqs. */
	private static final String STATE_STEP_CURRENT_PREREQS = "Dissertation.step_current_prereqs";	
	
	/** Are there active candidates? */
	private static final String STATE_ACTIVE_PATHS = "Dissertation.active_candidates";

	/** Should the change to a administrator's checklist be applied retroactively? */
	private static final String STATE_RETROACTIVE_CHANGE = "Dissertation.retroactive_change";
	
	/** The list of template steps for a candidate path. */
	private static final String STATE_CANDIDATE_PATH_TEMPLATE_STEPS = "Dissertation.candidate_path_template_steps";
	
	/** Candidate ID selected by administrator. */
	private static final String STATE_SELECTED_CANDIDATE_ID = "Dissertation.selected_candidate_id";

	/** Name of candidate selected by administrator. */
	private static final String STATE_SELECTED_CANDIDATE_NAME = "Dissertation.selected_candidate_name";

	/** Name of candidate selected by administrator. */
	private static final String STATE_SELECTED_CANDIDATE_EMPLID = "Dissertation.selected_candidate_emplid";

	/** The step status id of the step to validate. */
	private static final String STATE_STEP_STATUS_TO_VALIDATE = "Dissertation.step_to_validate";
	
	/** The array of users to select from. */
	private static final String STATE_USERS_LIST = "Dissertation.users_list";
	
	/** The chefid (key) emplid (value) map. */
	private static final String STATE_USERS_EMPLID_MAP = "Dissertation.users_emplid_map";
	
	/** The current candidate path. */
	private static final String STATE_CURRENT_CANDIDATE_PATH_REFERENCE = "Dissertation.current_candidate_path_reference";
	
	/** The description of a step. */
	private static final String STATE_STEP_DESCRIPTION = "Dissertation.step_description";
	
	/** Vector of LetterCarrier objects */
	private static final String STATE_LETTERS = "Dissertation.letters";
	
	private static final String STATE_LETTERS_DISSERTATION_STEPS = "Dissertation.letters.dissertation.steps";
	
	private static final String STATE_LETTERS_MUSIC_PERFORMANCE = "Dissertation.letters.music.performance";

	/** Vector of LetterCarrier objects */
	private static final String STATE_USER_ID = "Dissertation.current_candidate_id";
	
	/** Validating more than one step? */
	private static final String STATE_MULTIPLE_VALIDATION = "Dissertation.multiple_validation";

	/** Validating more than one step? */
	private static final String STATE_VALIDATION_TABLE = "Dissertation.validation_table";

	/** Validating more than one step? */
	private static final String STATE_VALIDATION_TYPES = "Dissertation.validation_types";

	private static final String STATE_MOVE_STEP_REFERENCE = "dissertation.move_step_reference";
	
	private static final String STATE_EDIT_STEP_REFERENCE = "dissertation.edit_step_reference";
	
	private static final String STATE_EDIT_STEPSTATUS = "dissertation.edit.stepstatus";
	
	private static final String STATE_DISSERTATION_OBJECTS_SELECTED = "dissertation.objects_selected";

	private static final String STATE_AUTO_VALIDATION_IDS = "dissertation.auto_valid_ids";
	
	/****************************** modes ****************************/

	/** The admin current dissertation view	 */
	private static final String MODE_ADMIN_VIEW_CURRENT_DISSERTATION = "Dissertation.mode_admin_view_current_dissertation";
	
	/** The admin view of add a dissertation step	 */
	private static final String MODE_ADMIN_ADD_DISSERTATION_STEP = "Dissertation.mode_admin_add_dissertation_step";

	/** The admin view to edit a dissertation step	 */
	private static final String MODE_ADMIN_EDIT_DISSERTATION_STEP = "Dissertation.mode_admin_edit_dissertation_step";

	/** The admin view to delete a dissertation step	 */
	private static final String MODE_CONFIRM_DELETE_DISSERTATION_STEP = "Dissertation.mode_confirm_delete_dissertation_step";

	/** The candidate's path view	 */
	private static final String MODE_CANDIDATE_VIEW_PATH = "Dissertation.mode_candidate_view_path";
	
	/** The candidate's no dissertation view	 */
	private static final String MODE_CANDIDATE_NO_DISSERTATION = "Dissertation.mode_no_dissertation";
	
	/** The admin's view of a candidate's path	 */
	private static final String MODE_ADMIN_VIEW_CANDIDATE_PATH = "Dissertation.mode_admin_view_candidate_path";
	
	/** The admin's alphabetical candidate chooser	 */
	private static final String MODE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER = "Dissertation.mode_admin_alphabetical_candidate_chooser";

	/** The admin view of add a dissertation step	 */
	private static final String MODE_CANDIDATE_ADD_STEPSTATUS = "Dissertation.mode_candidate_add_stepstatus";

	/** The admin view to edit a dissertation step	 */
	private static final String MODE_CANDIDATE_EDIT_STEPSTATUS = "Dissertation.mode_candidate_edit_stepstatus";
	
	/** The view if the system is not yet initialized by the first influx of school data.	 */
	private static final String MODE_SYSTEM_NOT_INITIALIZED = "Dissertation.mode_system_not_initialized";

	/** The view if a committee member tries to view a non-existent candidate path.	 */
	private static final String MODE_NO_CANDIDATE_PATH = "Dissertation.no_candidate_path";

	/** The committee's path view	 */
	private static final String MODE_COMMITTEE_VIEW_CANDIDATE_PATH = "Dissertation.mode_committee_view_candidate_path";

	/** The committee view of add a dissertation step	 */
	private static final String MODE_COMMITTEE_ADD_STEPSTATUS = "Dissertation.mode_committee_add_stepstatus";

	/** The committee view to edit a dissertation step	 */
	private static final String MODE_COMMITTEE_EDIT_STEPSTATUS = "Dissertation.mode_committee_edit_stepstatus";
	
	/** The view to move a dissertation step	 */
	private static final String MODE_MOVE_STEP = "Dissertation.mode_move_step";

	/** The view to confirm validate of a dissertation step	 */
	private static final String MODE_CONFIRM_VALIDATE_STEP = "Dissertation.mode_confirm_validate_step";
	
	/*************************** vm names ***************************/
	
	/** The admin welcome view */
	private static final String TEMPLATE_ADMIN_VIEW_CURRENT_DISSERTATION = "_admin_view_current_dissertation";

	/** The admin alphabetical candidate chooser view	*/
	private static final String TEMPLATE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER = "_admin_alphabetical_candidate_chooser";
	
	/** The admin view to add a dissertation step */
	private static final String TEMPLATE_ADMIN_ADD_DISSERTATION_STEP = "_admin_add_dissertation_step";

	/** The admin view to edit a dissertation step */
	private static final String TEMPLATE_ADMIN_EDIT_DISSERTATION_STEP = "_admin_edit_dissertation_step";

	/** The admin view to edit a dissertation step's prerequisites */
	private static final String TEMPLATE_ADMIN_EDIT_DISSERTATION_STEP_PREREQS = "_admin_edit_dissertation_step_prereqs";

	/** The admin view to choose a candidate */
	private static final String TEMPLATE_ADMIN_CHOOSE_CANDIDATE = "_admin_choose_candidate";

	/** The admin view of a candidate path */
	private static final String TEMPLATE_ADMIN_VIEW_CANDIDATE_PATH = "_admin_view_candidate_path";
	
	/** The candidate path view */
	private static final String TEMPLATE_CANDIDATE_VIEW_PATH = "_candidate_view_path";

	/** The committee member's no candidate path view. */
	private static final String TEMPLATE_NO_CANDIDATE_PATH = "_no_candidate_path";
	
	/** The candidate view to add a stepstatus */
	private static final String TEMPLATE_CANDIDATE_ADD_STEPSTATUS = "_candidate_add_stepstatus";

	/** The candidate view to edit a dissertation step */
	private static final String TEMPLATE_CANDIDATE_EDIT_STEPSTATUS = "_candidate_edit_stepstatus";
	
	/** The view if the system is not yet initialized by the first influx of school data.	 */
	private static final String TEMPLATE_SYSTEM_NOT_INITIALIZED = "_system_not_initialized";

	/** The committee view of a candidate path */
	private static final String TEMPLATE_COMMITTEE_VIEW_CANDIDATE_PATH = "_committee_view_candidate_path";

	/** The committee view to add a stepstatus */
	private static final String TEMPLATE_COMMITTEE_ADD_STEPSTATUS = "_committee_add_stepstatus";
	
	/** The committee view to edit a stepstatus */
	private static final String TEMPLATE_COMMITTEE_EDIT_STEPSTATUS = "_committee_edit_stepstatus";



	/** CONSOLIDATED VM VILES **/
	
	/** The confirm validate step view. */
	private static final String TEMPLATE_CONFIRM_VALIDATE_STEP = "_confirm_validate_step";
	
	/** The confirm delete dissertation step view */
	private static final String TEMPLATE_CONFIRM_DELETE_STEP = "_confirm_delete_step";

	/** The view to move a dissertation step */
	private static final String TEMPLATE_MOVE_STEP = "_move_step";




	/** *******************************    CONTEXT STRINGS     *****************************************/
	
	private static final String CONTEXT_ALERT_MESSAGE = "alert";
	private static final String CONTEXT_CANDIDATES_LIST = "candidates";
	private static final String CONTEXT_SELECTED_CANDIDATE = "selected_candidate";
	private static final String CONTEXT_STEP_DESCRIPTION = "stepdesc";
	private static final String CONTEXT_USER_DISPLAY_NAME = "username";
	private static final String CONTEXT_SELECTED_CANDIDATE_DISPLAY_NAME = "candidatename";
	private static final String CONTEXT_SELECTED_CANDIDATE_EMPLID = "emplid";
	private static final String CONTEXT_MULTIPLE_VALIDATION = "multivalid";
	
	private static final String CONTEXT_USER_ROLE = "userrole";
	private static final String SCHOOL_ROLE = "schoolrole";
	private static final String ADMIN_ROLE = "adminrole";
	private static final String COMMITTEE_ROLE = "committeerole";
	private static final String CANDIDATE_ROLE = "candidaterole";
	private static final String DEAN_ROLE = "deanrole";
	
	
	
	/**
	 * Central place for dispatching the build routines based on the state name.
	 */
	public String buildMainPanelContext(	VelocityPortlet portlet,
											Context context,
											RunData data,
											SessionState state)
	{
		String mode = (String)state.getAttribute (STATE_MODE);
		mode = (String)state.getAttribute (STATE_MODE);
		if(mode == null)
		{
			mode = MODE_SYSTEM_NOT_INITIALIZED;
		}
		
		// exception Message
		StringBuffer exceptionMessage = new StringBuffer();

		String retVal = "";
		if(mode.equals(MODE_ADMIN_VIEW_CURRENT_DISSERTATION))
		{
			// build the context for an administrator to view the current dissertation for their department
			retVal = build_admin_view_current_dissertation_context(portlet, context, data, state);
		}
		else if (mode.equals (MODE_ADMIN_EDIT_DISSERTATION_STEP))
		{
			// build the context for an adminstrator to edit a dissertation step
			retVal = build_admin_edit_dissertation_step_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_MOVE_STEP))
		{
			// build the context for moving a dissertation step
			retVal = build_move_step_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_ADMIN_ADD_DISSERTATION_STEP))
		{
			// build the context for an administrator to add a dissertation step
			retVal = build_admin_add_dissertation_step_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_CONFIRM_DELETE_DISSERTATION_STEP))
		{
			// build the context to confirm the deletion a dissertation step
			retVal = build_confirm_delete_dissertation_step_context (portlet, context, data, state);
		}
		else if(mode.equals(MODE_CANDIDATE_VIEW_PATH))
		{
			// build the context for a candidate to view their CandidatePath
			retVal = build_candidate_view_path_context(portlet, context, data, state);
		}
		else if (mode.equals (MODE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER))
		{
			// build the context for the admin to choose a candidate from an alphabetical selector
			retVal = build_admin_alphabetical_candidate_chooser_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_ADMIN_VIEW_CANDIDATE_PATH))
		{
			// build the context for the administrator viewing a candidate path
			retVal = build_admin_view_candidate_path_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_CANDIDATE_NO_DISSERTATION))
		{
			// build the context for if there is no parent dissertation for the candidate at time of initialization
			retVal = build_candidate_no_dissertation_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_NO_CANDIDATE_PATH))
		{
			// build the context for a warning message if no candidate path exists
			retVal = build_no_candidate_path_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_CANDIDATE_EDIT_STEPSTATUS))
		{
			// build the context for a candidate to edit a stepstatus
			retVal = build_candidate_edit_stepstatus_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_CANDIDATE_ADD_STEPSTATUS))
		{
			// build the context for candidate add a stepstatus
			retVal = build_candidate_add_stepstatus_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_SYSTEM_NOT_INITIALIZED))
		{
			// build the context for an uninitialized system
			retVal = build_system_not_initialized_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_COMMITTEE_VIEW_CANDIDATE_PATH))
		{
			// build the context for a committee member to view a candidate path
			retVal = build_committee_view_candidate_path_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_COMMITTEE_EDIT_STEPSTATUS))
		{
			// build the context for a committee member edit a stepstatus
			retVal = build_committee_edit_stepstatus_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_COMMITTEE_ADD_STEPSTATUS))
		{
			// build the context for committee member to add a stepstatus
			retVal = build_committee_add_stepstatus_context (portlet, context, data, state);
		}
		else if (mode.equals (MODE_CONFIRM_VALIDATE_STEP))
		{
			// build the context to confirm validation of a step
			retVal = build_confirm_validate_step_context (portlet, context, data, state);
		}

		String templateRoot = (String) getContext(data).get("template");
		return templateRoot + retVal;

	}	// buildMainPanelContext

	
	/**
	* Build the context to confirm validation of a step.
	*/
	protected String build_confirm_validate_step_context (	VelocityPortlet portlet,
								Context context,
								RunData data,
								SessionState state)
	{
		String userRole = (String)state.getAttribute(STATE_USER_ROLE);

		context.put (Menu.CONTEXT_ACTION, "DissertationAction");
		context.put(CONTEXT_USER_ROLE, userRole);
		if((COMMITTEE_ROLE.equals(userRole)) || (ADMIN_ROLE.equals(userRole)) || (SCHOOL_ROLE.equals(userRole)))
			context.put(CONTEXT_SELECTED_CANDIDATE_DISPLAY_NAME, (String)state.getAttribute(STATE_SELECTED_CANDIDATE_NAME));
		
		context.put(CONTEXT_STEP_DESCRIPTION, (String)state.getAttribute(STATE_STEP_DESCRIPTION));
		context.put(CONTEXT_MULTIPLE_VALIDATION, (Boolean)state.getAttribute(STATE_MULTIPLE_VALIDATION));
		return TEMPLATE_CONFIRM_VALIDATE_STEP;

	}//build_confirm_validate_step_context


	/**
	* Build the context to confirm the deletion a dissertation step.
	*/
	protected String build_confirm_delete_dissertation_step_context (	VelocityPortlet portlet,
										Context context,
										RunData data,
										SessionState state)
	{
		context.put(CONTEXT_USER_ROLE, (String)state.getAttribute(STATE_USER_ROLE));
		context.put("steps", (TemplateStep[])state.getAttribute(STATE_SELECTED_STEPS));
		context.put("multidelete", (Boolean)state.getAttribute(STATE_MULTIPLE_VALIDATION));
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		context.put("activepaths", (Boolean)state.getAttribute(STATE_ACTIVE_PATHS));
		
		return TEMPLATE_CONFIRM_DELETE_STEP;

	}//build_confirm_delete_dissertation_step_context


	/**
	* Build the context for moving a dissertation step.
	*/	
	protected String build_move_step_context (VelocityPortlet portlet,
						Context context,
						RunData data,
						SessionState state)
	{
		String userRole = (String)state.getAttribute(STATE_USER_ROLE);
		String stepRef = (String)state.getAttribute(STATE_MOVE_STEP_REFERENCE);
		
		//Need step's section header when moving a Rackham dissertation step 
		if(userRole!=null && userRole.equals("schoolrole"))
		{
			try
			{
				DissertationStep stepToMove = null;
				stepToMove = DissertationService.getDissertationStep(stepRef);
				int header = Integer.parseInt(stepToMove.getSection());
				context.put("sections", (Vector)getSectionHeads());
				context.put("stepToMoveHeader", (String)(((Vector)getSectionHeads()).get(header)));
			}
			catch(Exception e) 
			{
				Log.warn("chef", this + ".build_move_step_context " + userRole + " " + stepRef + " " + e);
			}
		}
		context.put(CONTEXT_USER_ROLE, (String)state.getAttribute(STATE_USER_ROLE));
		context.put("stepdesc", (String)state.getAttribute(STATE_STEP_DESC));
		context.put("orderedsteps", (Vector)state.getAttribute(STATE_ORDERED_STEPS));
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		context.put("activepaths", (Boolean)state.getAttribute(STATE_ACTIVE_PATHS));
		
		return TEMPLATE_MOVE_STEP;

	}//build_move_step_context
	
	/**
	* Build the context for a committee member to view a candidate path.
	*/
	protected String build_committee_view_candidate_path_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		Boolean hasSteps = null;
		TemplateStep[] templateSteps = (TemplateStep[])state.getAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS);
		if(templateSteps.length > 0)
			hasSteps = new Boolean(true);
		else
			hasSteps = new Boolean(false);
		context.put("showcompleted", (Boolean)state.getAttribute(STATE_SHOW_COMPLETED_STEPS));
		context.put("displayname", (String)state.getAttribute(STATE_USER_DISPLAY_NAME));
		context.put("steps", templateSteps);
		context.put("hassteps", hasSteps);
		context.put("sections",(Vector)getSectionHeads());
		
		Boolean showdone = (Boolean)state.getAttribute(STATE_SHOW_COMPLETED_STEPS);
		
		Menu menu = new Menu (portlet, data, "DissertationAction");
		menu.add ( new MenuEntry ("New", null, true, MenuItem.CHECKED_NA, "doAdd_stepstatus_comm", "commViewPath") );
		menu.add ( new MenuEntry ("Delete", null, true, MenuItem.CHECKED_NA, "doConfirm_delete_stepstatus_comm", "commViewPath") );
		menu.add ( new MenuEntry ("Revise", null, true, MenuItem.CHECKED_NA, "doEdit_stepstatus_comm", "commViewPath") );
		menu.add ( new MenuEntry ("Move", null, true, MenuItem.CHECKED_NA, "doMove_stepstatus_comm", "commViewPath") );
		menu.add ( new MenuEntry ("Mark as Done", null, true, MenuItem.CHECKED_NA, "doConfirm_committee_update_candidate_path", "commViewPath") );
		if(showdone.booleanValue())
			menu.add ( new MenuEntry ("Hide Done", null, true, MenuItem.CHECKED_NA, "doToggle_candidate_path_display_status_comm", "commViewPath") );
		else
			menu.add ( new MenuEntry ("Show Done", null, true, MenuItem.CHECKED_NA, "doToggle_candidate_path_display_status_comm", "commViewPath") );
		
		context.put(Menu.CONTEXT_MENU, menu);
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		return TEMPLATE_COMMITTEE_VIEW_CANDIDATE_PATH;

	}//build_committee_view_path_context


	/**
	* Build the context for committee member to add a stepstatus.
	*/
	protected String build_committee_add_stepstatus_context (VelocityPortlet portlet,
								Context context,
								RunData data,
								SessionState state)
	{
		context.put("sections", (Vector)getSectionHeads());
		context.put("orderedsteps", (Vector)state.getAttribute(STATE_ORDERED_STEPS));
		context.put("validationTypeTable", commValidationTypeTable());
		context.put("validationtypes", commValidationTypes());
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		return TEMPLATE_COMMITTEE_ADD_STEPSTATUS;

	}//build_committee_add_stepstatus_context


	/**
	* Build the context for a committee member edit a stepstatus.
	*/
	protected String build_committee_edit_stepstatus_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		context.put("sections", (Vector)getSectionHeads());
		context.put("prerequisites", (Iterator)state.getAttribute(STATE_PREREQUISITES));
		context.put("currentprerequisites", (Iterator)state.getAttribute(STATE_CURRENT_PREREQUISITES));
		context.put("step", (StepStatus)state.getAttribute(STATE_EDIT_STEPSTATUS));
		context.put("validationTypeTable", commValidationTypeTable());
		context.put("validationtypes", commValidationTypes());
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		return ((String)state.getAttribute(STATE_CURRENT_TEMPLATE));

	}//build_committee_edit_stepstatus_context
	
	
	
	//*********************************************************************************************************************************************************
	
	
	/**
	* Build the context for an uninitialized system.
	*/
	protected String build_system_not_initialized_context (	VelocityPortlet portlet,
								Context context,
								RunData data,
								SessionState state)
	{
		return TEMPLATE_SYSTEM_NOT_INITIALIZED;

	}//build_system_not_initialized_context
	
	
	/**
	* Build the context for candidate add a stepstatus.
	*/
	protected String build_candidate_add_stepstatus_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		context.put("sections", (Vector)getSectionHeads());
		context.put("orderedsteps", (Vector)state.getAttribute(STATE_ORDERED_STEPS));
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		return TEMPLATE_CANDIDATE_ADD_STEPSTATUS;

	}//build_candidate_add_stepstatus_context


	/**
	* Build the context for a candidate to edit a stepstatus.
	*/
	protected String build_candidate_edit_stepstatus_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		context.put("sections", (Vector)getSectionHeads());
		context.put("prerequisites", (Iterator)state.getAttribute(STATE_PREREQUISITES));
		context.put("currentprerequisites", (Iterator)state.getAttribute(STATE_CURRENT_PREREQUISITES));
		context.put("step", (StepStatus)state.getAttribute(STATE_EDIT_STEPSTATUS));
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");

		return ((String)state.getAttribute(STATE_CURRENT_TEMPLATE));

	}//build_candidate_edit_stepstatus_context
	
	
	//******************************************************************************************************************************************
	
	
	/**
	* Build the context for an administrator to add a dissertation step.
	*/
	protected String build_admin_add_dissertation_step_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		context.put("sections", (Vector)getSectionHeads());
		context.put("orderedsteps", (Vector)state.getAttribute(STATE_ORDERED_STEPS));
		context.put(CONTEXT_USER_ROLE, (String)state.getAttribute(STATE_USER_ROLE));
		context.put("validationtypes", (String[])state.getAttribute(STATE_VALIDATION_TYPES));
		context.put("validationTypeTable", (Hashtable)state.getAttribute(STATE_VALIDATION_TABLE));
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		context.put("activepaths", (Boolean)state.getAttribute(STATE_ACTIVE_PATHS));
		
		return TEMPLATE_ADMIN_ADD_DISSERTATION_STEP;

	}//build_admin_add_dissertation_step_context
	
	
	/**
	* Build the context for an administrator to view the current dissertation for their department.
	*/
	protected String build_admin_view_current_dissertation_context (VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		Boolean hasSteps = null;
		TemplateStep[] steps = (TemplateStep[])state.getAttribute(STATE_CURRENT_DISSERTATION_STEPS);
		Menu menu = new Menu (portlet, data, "DissertationAction");
		String department = (String)state.getAttribute(STATE_USERS_DEPT_FULL_NAME);
		
		//DEANS MAY LOOK BUT NOT TOUCH
		boolean dean = ((String)state.getAttribute(STATE_USER_ROLE)).equals(DEAN_ROLE) ? true : false ;
		if(!dean)
		{
			menu.add ( new MenuEntry ("New", null, true, MenuItem.CHECKED_NA, "doAdd_dissertation_step", "listSteps") );
		}
		
		if(steps.length > 0)
		{
			hasSteps = new Boolean(true);
			if(!dean)
			{
				menu.add ( new MenuEntry ("Delete", null, true, MenuItem.CHECKED_NA, "doConfirm_delete_step", "listSteps") );
				menu.add ( new MenuEntry ("Revise", null, true, MenuItem.CHECKED_NA, "doEdit_dissertation_step", "listSteps") );
				menu.add ( new MenuEntry ("Move", null, true, MenuItem.CHECKED_NA, "doMove_dissertation_step", "listSteps") );
			}
		}
		else
		{
			hasSteps = new Boolean(false);
		}
		menu.add ( new MenuEntry ("View Student's Progress", null, true, MenuItem.CHECKED_NA, "doChecklist", "listSteps") );
		
		//IF THIS IS THE SCHOOL SITE, PUT UP A BUTTON TO SWITCH CURRENT DISSERTATION STEPS 
		if(((String)state.getAttribute(STATE_CURRENT_SITE)).equals((String)state.getAttribute(STATE_SCHOOL_SITE)))
		{
			if(((String)state.getAttribute(STATE_DISSERTATION_TYPE)).equals(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE))
			{
				department = department + " - Music Performance";
				menu.add ( new MenuEntry ("Go to Dissertation Steps", null, true, MenuItem.CHECKED_NA, "doToggle_current_dissertation_steps", "listSteps") );
			}
			else
			{
				department = department + " - Dissertation Steps";
				menu.add ( new MenuEntry ("Go to Music Performance", null, true, MenuItem.CHECKED_NA, "doToggle_current_dissertation_steps", "listSteps") );
			}
		}
		
		context.put(Menu.CONTEXT_MENU, menu);
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		context.put("hassteps", hasSteps);
		context.put("steps", (steps));
		context.put("department", department);
		context.put("sections",(Vector)getSectionHeads());
		context.put("dean", new Boolean(dean));
				
		return TEMPLATE_ADMIN_VIEW_CURRENT_DISSERTATION;

	}// build_admin_view_current_dissertation_context


	/**
	* Build the context for an adminstrator to edit a dissertation step.
	*/
	protected String build_admin_edit_dissertation_step_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		int header = 1;
		try
		{
			header = Integer.parseInt((String) ((DissertationStep)state.getAttribute(STATE_DISSERTATION_STEP)).getSection());
		}
		catch(NumberFormatException ignore){}
		context.put("stepToMoveHeader", (String)(((Vector)getSectionHeads()).get(header)));
		context.put("sections", (Vector)getSectionHeads());
		context.put("prerequisites", (Iterator)state.getAttribute(STATE_PREREQUISITES));
		context.put("currentprerequisites", (Iterator)state.getAttribute(STATE_CURRENT_PREREQUISITES));
		context.put("step", (DissertationStep)state.getAttribute(STATE_DISSERTATION_STEP));
		context.put("validationtypes", (String[])state.getAttribute(STATE_VALIDATION_TYPES));
		context.put("validationTypeTable", (Hashtable)state.getAttribute(STATE_VALIDATION_TABLE));
		context.put(CONTEXT_USER_ROLE, (String)state.getAttribute(STATE_USER_ROLE));
		context.put("autovalidids", (String[])state.getAttribute(STATE_AUTO_VALIDATION_IDS));
		String template = (String)state.getAttribute(STATE_CURRENT_TEMPLATE);
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		context.put("activepaths", (Boolean)state.getAttribute(STATE_ACTIVE_PATHS));
		
		return ((String)state.getAttribute(STATE_CURRENT_TEMPLATE));
		
	}//build_admin_edit_dissertation_step_context


	/**
	* Build the context for a warning message if no candidate path exists.
	*/
	protected String build_no_candidate_path_context (	VelocityPortlet portlet,
								Context context,
								RunData data,
								SessionState state)
	{
		context.put("candidatename", (String)state.getAttribute(STATE_SELECTED_CANDIDATE_ID));
		return TEMPLATE_NO_CANDIDATE_PATH;

	}//build_no_candidate_path_context


	/**
	* Build the context for if there is no parent dissertation for the candidate at time of initialization.
	*/
	protected String build_candidate_no_dissertation_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		context.put("candidatename", (String)state.getAttribute(STATE_SELECTED_CANDIDATE_ID));
		return TEMPLATE_NO_CANDIDATE_PATH;

	}//build_candidate_no_dissertation_context
	

	/**
	* Build the context for the administrator viewing a candidate path.
	*/
	protected String build_admin_view_candidate_path_context (	VelocityPortlet portlet,
									Context context,
									RunData data,
									SessionState state)
	{
		boolean dean = ((String)state.getAttribute(STATE_USER_ROLE)).equals(DEAN_ROLE) ? true : false ;
		
		Boolean hasSteps = null;
		TemplateStep[] steps = (TemplateStep[])state.getAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS);
		if(steps.length > 0)
			hasSteps = new Boolean(true);
		else
			hasSteps = new Boolean(false);
		
		context.put(CONTEXT_SELECTED_CANDIDATE_DISPLAY_NAME, (String)state.getAttribute(STATE_SELECTED_CANDIDATE_NAME));
		context.put(CONTEXT_SELECTED_CANDIDATE_EMPLID, (String)state.getAttribute(STATE_SELECTED_CANDIDATE_EMPLID));
		context.put("program", (String)state.getAttribute(STATE_SELECTED_CANDIDATE_GROUP_DISPLAY_NAME));
		context.put("hassteps", hasSteps);
		context.put("steps", steps);
		context.put("sections",(Vector)getSectionHeads());
		
		//DEANS MAY LOOK BUT NOT TOUCH
		Menu menu = new Menu (portlet, data, "DissertationAction");
		if(!dean)
		{
			menu.add ( new MenuEntry ("Mark as Done", null, true, MenuItem.CHECKED_NA, "doConfirm_admin_update_candidate_path", "adminViewPath") );
		}
		menu.add ( new MenuEntry ("Back to List of Students", null, true, MenuItem.CHECKED_NA, "doView_candidates_list", "adminViewPath") );
		
		context.put("dean", new Boolean(dean));
		context.put(Menu.CONTEXT_MENU, menu);
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		return TEMPLATE_ADMIN_VIEW_CANDIDATE_PATH;

	}//build_admin_view_candidate_path_context	

	
	/**
	* Build the context for the admin to choose a candidate from an alphabetical selector.
	*/
	protected String build_admin_alphabetical_candidate_chooser_context (	VelocityPortlet portlet,
										Context context,
										RunData data,
										SessionState state)
	{
		Vector letters = (Vector)state.getAttribute(STATE_LETTERS);
		User[] users = (User[])state.getAttribute(STATE_USERS_LIST);
		Hashtable emplids = (Hashtable)state.getAttribute(STATE_USERS_EMPLID_MAP);
		
		if(letters == null)
		{
			Log.warn("chef", this + "build_admin_alphabetical_candidate_chooser_context, letters == null");
		}
		if(users == null)
		{
			Log.warn("chef", this + "build_admin_alphabetical_candidate_chooser_context, users == null");
		}
		
		Boolean hasUsers = null;
		if(users.length > 0)
		{
			hasUsers = new Boolean(true);
		}
		else
		{
			hasUsers = new Boolean(false);
		}

		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		context.put("users", users);
		context.put("emplids", emplids);
		context.put("letters", letters);
		context.put("hasusers", hasUsers);
		return TEMPLATE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER;

	}//build_admin_alphabetical_candidate_chooser_context


	/**
	* Build the context for a candidate to view their CandidatePath.
	*/
	protected String build_candidate_view_path_context (	VelocityPortlet portlet,
								Context context,
								RunData data,
								SessionState state)
	{
		Boolean hasSteps = null;
		TemplateStep[] templateSteps = (TemplateStep[])state.getAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS);
		if(templateSteps.length > 0)
			hasSteps = new Boolean(true);
		else
			hasSteps = new Boolean(false);

		context.put("showcompleted", (Boolean)state.getAttribute(STATE_SHOW_COMPLETED_STEPS));
		context.put("displayname", (String)state.getAttribute(STATE_USER_DISPLAY_NAME));
		context.put("steps", templateSteps);
		context.put("hassteps", hasSteps);
		context.put("sections",(Vector)getSectionHeads());

		Menu menu = new Menu (portlet, data, "DissertationAction");
		menu.add ( new MenuEntry ("New", null, true, MenuItem.CHECKED_NA, "doAdd_stepstatus", "candidateViewPath") );
		menu.add ( new MenuEntry ("Delete", null, true, MenuItem.CHECKED_NA, "doConfirm_delete_stepstatus", "candidateViewPath") );
		menu.add ( new MenuEntry ("Revise", null, true, MenuItem.CHECKED_NA, "doEdit_stepstatus", "candidateViewPath") );
		menu.add ( new MenuEntry ("Move", null, true, MenuItem.CHECKED_NA, "doMove_stepstatus", "candidateViewPath") );
		menu.add ( new MenuEntry ("Mark as Done", null, true, MenuItem.CHECKED_NA, "doConfirm_candidate_update_candidate_path", "candidateViewPath") );
	
		Boolean showdone = (Boolean)state.getAttribute(STATE_SHOW_COMPLETED_STEPS);
		if(showdone.booleanValue())
			menu.add ( new MenuEntry ("Hide Done", null, true, MenuItem.CHECKED_NA, "doToggle_candidate_path_display_status", "candidateViewPath") );
		else
			menu.add ( new MenuEntry ("Show Done", null, true, MenuItem.CHECKED_NA, "doToggle_candidate_path_display_status", "candidateViewPath") );

		context.put(Menu.CONTEXT_MENU, menu);
		context.put(Menu.CONTEXT_ACTION, "DissertationAction");
		return TEMPLATE_CANDIDATE_VIEW_PATH;

	}//build_candidate_view_path_context




	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	// ****************************************************       DO ROUTINES           *********************************************************************	
	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************

	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	// **********************************************       DO ROUTINES - COMMITTEE           ***************************************************************	
	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	
	/**
	* Action is to present the create a new dissertation step interface to a committee member
	**/
	public void doAdd_stepstatus_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();

		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String candidatePathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		
		Vector prereqsVector = new Vector();
		Iterator prerequisites = null;
		StepStatus tempStatus = null;
		CandidatePath path = null;
		Vector orderedStatus = new Vector();
		Hashtable statusHash = null;
		String statusRef = null;
		String keyString = null;
		try
		{
			if(DissertationService.allowUpdateCandidatePathComm(candidatePathRef))
			{
				path = DissertationService.getCandidatePath(candidatePathRef);
				statusHash = path.getOrderedStatus();

				for(int x = 1; x < (statusHash.size()+1); x++)
				{
					keyString = "" + x;
					statusRef = (String)statusHash.get(keyString);
					tempStatus = DissertationService.getStepStatus(statusRef);
					orderedStatus.add(tempStatus);
				}
			}
			else
			{
				addAlert(state, "You do not have permission to add a step to this checklist.");
			}
		}
		catch(Exception e){}

		if(state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_ORDERED_STEPS, orderedStatus);
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_ADD_STEPSTATUS);
		}
		else
		{
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		}

	}//doAdd_stepstatus_comm
	
	/**
	* Action is for a committee member to create a new stepstatus and add it to the service
	**/
	public void doAddnew_stepstatus_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		CandidatePathEdit path = null;
		String pathRef = null;
		if (params.getString("cancel")!=null)
		{
			// cancel button been clicked
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		}
		else
		{
			//CHECK STEP COMES AFTER ITS PREREQS
			String[] prereqs = params.getStrings("prereq");
			String location = params.getString("location");	
			String dummy = ""; //just to change the signature
			if(!checkFollowsPrereqs(state, location, prereqs, dummy))
			{
				addAlert(state, "The location of the step would have come before its prerequisite(s).");
				state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
				try
				{
					CandidatePath cp = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
					state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(cp, state, true));
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".doAddnew_stepstatus_comm Exception " + e);
				}
				state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
				return;
			}
			//END CHECK STEP COMES AFTER ITS PREREQS
			
			StepStatusEdit statusEdit = null;
			String instructionsText = params.getString("desc");
			String validType = params.getString("vtype");
				
			try
			{
				// CREATE THE NEW STATUS
				statusEdit = DissertationService.addStepStatus(currentSite);
				statusEdit.setInstructions(instructionsText);
				statusEdit.setValidationType(validType);
				if(prereqs != null)
				{
					for(int y = 0; y < prereqs.length; y++)
					{
						statusEdit.addPrerequisiteStatus(prereqs[y]);
					}
				}
				DissertationService.commitEdit(statusEdit);
				
					// APPLY THE CHANGE TO THE CANDIDATEPATH
				pathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
				path = DissertationService.editCandidatePath(pathRef);
				int loc = path.getOrderForStatus(location);
				path.addToOrderedStatus(statusEdit, ++loc);
				DissertationService.commitEdit(path);
			}
			catch(PermissionException pe)
			{
				addAlert(state, "You do not have permission to modify this checklist.");
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doAddnew_stepstatus_comm : EXCEPTION : " + e);
			}
			state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(path, state, true));
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		}

	}//doAddnew_stepstatus_comm
	
	
	/**
	* Action : For a committee member to edit the selected stepstatus.
	**/
	public void doEdit_stepstatus_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String selectedstatus = params.getString ("selectedstatus");
		
		if(selectedstatus == null)
		{
			addAlert(state, "You did not select a step to revise.");
		}
		else
		{
			String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
			User currentUser = (User)state.getAttribute(STATE_USER);
			String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
			String candidatePathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
			Vector prereqsVector = new Vector();
			Vector currentPrereqsVector = new Vector();
			Hashtable orderedStatus = null;
			String keyString = null;
			String idString = null;
			StepStatus tempStatus = null;
			String template = TEMPLATE_COMMITTEE_EDIT_STEPSTATUS;
			List prereqs = null;
			StepStatus status = null;
			CandidatePath path = null;

			try
			{
				status = DissertationService.getStepStatus(selectedstatus);
				
				//if step status lacks CHEF:creator property
				if((status.getCreator()!=null) && (status.getCreator().equals(currentUser.getId())))
				{
					state.setAttribute(STATE_EDIT_STEPSTATUS, status);
					state.setAttribute(STATE_EDIT_STEP_REFERENCE, status.getReference());
					path = DissertationService.getCandidatePath(candidatePathRef);
					orderedStatus = path.getOrderedStatus();
					for(int y = 1; y < (orderedStatus.size()+1); y++)
					{
						keyString = "" + y;
						idString = (String)orderedStatus.get(keyString);
						tempStatus = (StepStatus)DissertationService.getStepStatus(idString);
						if((!tempStatus.getId().equals(status.getId())) && (!status.hasPrerequisite(tempStatus.getReference())))
						{
							prereqsVector.add(tempStatus);
						}
					}
					prereqs = status.getPrereqs();
					for(int x = 0; x < prereqs.size(); x++)
					{
						tempStatus = DissertationService.getStepStatus((String) prereqs.get(x));
						if(tempStatus != null)
							currentPrereqsVector.add(tempStatus);
					}
				}
				else
				{
					addAlert(state, "You do not have permission to revise this step : " + status.getShortInstructionsText());
				}
			}
			catch(Exception e){}
			
			if(state.getAttribute(STATE_MESSAGE) != null)
			{
				state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
			}
			else
			{
				state.setAttribute(STATE_CURRENT_PREREQUISITES, currentPrereqsVector.iterator());
				state.setAttribute(STATE_PREREQUISITES, prereqsVector.iterator());
				state.setAttribute(STATE_CURRENT_TEMPLATE, template);
				state.setAttribute(STATE_MODE, MODE_COMMITTEE_EDIT_STEPSTATUS);
			}
		}
	
	}	//doEdit_stepstatus_comm



	/**
	* Action is to present the confirm delete stepstatus interface to a committee member
	**/
	public void doConfirm_delete_stepstatus_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String currentUserId = (String)state.getAttribute(STATE_USER_ID);
		ParameterParser params = data.getParameters();
		String[] selectedStatus = params.getStrings("selectedstatus");
		StepStatus status = null;
		String instructionsText = null;
		if(selectedStatus == null)
		{
			addAlert(state, "You did not select a step to delete.");
		}
		else
		{
			if(selectedStatus.length > 1)
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(true));
			else
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(false));
			for(int x = 0; x < selectedStatus.length; x++)
			{
				try
				{
					status = DissertationService.getStepStatus(selectedStatus[x]);
					
					//if step status doesn't have CHEF:creator property
					if((status.getCreator()==null) || (!status.getCreator().equals(currentUserId)))
					{
						instructionsText = status.getShortInstructionsText();
						addAlert(state, "You do not have permission to remove this step : " + instructionsText + " <br> ");
					}
					else
					{
						//SECURITY CHECK SUCCEEDED FOR ID
					}
				}
				catch(Exception e)
				{
					addAlert(state, "You do not have permission to remove this step.");
				}
			}

				// Did they pass the security check ?			
			if(state.getAttribute(STATE_MESSAGE) == null)
			{
				TemplateStep[] templateSteps = new TemplateStep[selectedStatus.length];
				StepStatus aStatus = null;
				for(int x = 0; x < selectedStatus.length; x++)
				{
					try
					{
						aStatus = DissertationService.getStepStatus(selectedStatus[x]);
						templateSteps[x] = new TemplateStep();
						templateSteps[x].setInstructions(aStatus.getShortInstructionsText());
					}
					catch(Exception e){}
				}
				state.setAttribute(STATE_SELECTED_STEPS, templateSteps);
			}
		}

		if(state.getAttribute(STATE_MESSAGE) == null)
		{
			//NO ALERT STATUS - DIRECTING TO CONFIRM DELETE
			state.setAttribute(STATE_DISSERTATION_OBJECTS_SELECTED, selectedStatus);
			state.setAttribute(STATE_MODE, MODE_CONFIRM_DELETE_DISSERTATION_STEP);
		}
		else
		{
			//ALERT ALERT ALERT - DIRECTING BACK TO VIEW CURRENT DISSERTATION
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		}

	}//doConfirm_delete_stepstatus_comm
	
	
	/**
	* Action is to delete a committee-added stepstatus
	**/
	public void doDelete_step_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		if(params.getString("cancel") != null)
		{
			//ALERT ALERT ALERT - DIRECTING BACK TO VIEW CURRENT DISSERTATION
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
			return;
		}

		String[] selectedStatus = (String[])state.getAttribute(STATE_DISSERTATION_OBJECTS_SELECTED);
				
			// FIRST CYCLE THROUGH ALL STATUS AND REMOVE PREREQUISITE REFERENCES TO THESE STATUS
		String pathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
		CandidatePathEdit pathEdit = null;
		StepStatus aStatus = null;
		StepStatusEdit statusEdit = null;
		List prereqs = null;
		String keyString = null;
		String valueString = null;
		
		try
		{
			pathEdit = DissertationService.editCandidatePath(pathRef);
			Hashtable orderedStatus = pathEdit.getOrderedStatus();
			
			for(int counter = 0; counter < selectedStatus.length; counter++)
			{
				for(int x = 1; x < (orderedStatus.size()+1); x++)
				{
					keyString = "" + x;
					valueString = (String)orderedStatus.get(keyString);
					//COMMENCING PREREQUISITE REFERENCE DELETION FOR STATUS
					aStatus = DissertationService.getStepStatus(valueString);
					//ANALYZING STATUS
					prereqs = aStatus.getPrereqs();
					for(int y = 0; y < prereqs.size(); y++)
					{
						if(prereqs.get(y).equals(selectedStatus[counter]))
						{
							//PREREQ ID MATCHES STATUS ID - REMOVING
							statusEdit = DissertationService.editStepStatus(aStatus.getReference());
							statusEdit.removePrerequisiteStatus(selectedStatus[counter]);
							DissertationService.commitEdit(statusEdit);
						}
					}
				}
			}
			
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doDelete_step_comm : EXCEPTION REMOVE PREREQS : " + e);
		}

		
		for(int z = 0; z < selectedStatus.length; z++)
		{
			try
			{
				pathEdit.removeFromOrderedStatus(selectedStatus[z]);
				//REMOVED STATUS FROM CANDIDATE PATH
				statusEdit = DissertationService.editStepStatus(selectedStatus[z]);
				DissertationService.removeStepStatus(statusEdit);
				//REMOVED STATUS FROM SERVICE
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : EXCEPTION REMOVING STATUS : " + e);
			}
		}
		DissertationService.commitEdit(pathEdit);

		state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(pathEdit, state, true));
		state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);

	}//doDelete_step_comm
	
	
	
	/**
	* Action is for a committee member to move a step
	**/
	public void doMove_stepstatus_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String currentUserId = (String)state.getAttribute(STATE_USER_ID);
		String statusToMoveRef = params.getString("selectedstatus");

		if(statusToMoveRef == null)
		{
			addAlert(state, "You did not select a step to move.");
		}
		else
		{
			String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
			StepStatus statusToMove = null;
			StepStatus tempStatus = null;
			String pathId = null;
			String statusToMoveDesc = "";
			CandidatePath path = null;
			Vector orderedStatus = new Vector();
			Hashtable statusHash = null;
			String statusRef = null;
			String keyString = null;
			String instructionsText = null;

			try
			{
				statusToMove = DissertationService.getStepStatus(statusToMoveRef);
				//GOT STATUS TO MOVE
				statusToMoveDesc = statusToMove.getShortInstructionsText();
				
				//if step status is missing CHEF:creator property
				if((statusToMove.getCreator()!=null) && (statusToMove.getCreator().equals(currentUserId)))
				{
					//PASSED SECURITY TEST - FILLING LOCATION VECTOR
					path = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
					//GOT CANDIDATE PATH
					statusHash = path.getOrderedStatus();
					//GOT STATUS HASH
					for(int x = 1; x < (statusHash.size()+1); x++)
					{
						keyString = "" + x;
						statusRef = (String)statusHash.get(keyString);
						tempStatus = DissertationService.getStepStatus(statusRef);
						if(!tempStatus.getId().equals(statusToMoveRef))
						{
							orderedStatus.add(tempStatus);
						}
					}
				}
				else
				{
					addAlert(state, "You do not have permission to move this step : " + statusToMoveDesc);
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doMove_stepstatus_comm : EXCEPTION : " + e);
			}

			if(state.getAttribute(STATE_MESSAGE) == null)
			{
				state.setAttribute(STATE_MOVE_STEP_REFERENCE, statusToMoveRef);
				state.setAttribute(STATE_MODE, MODE_MOVE_STEP);
				state.setAttribute(STATE_STEP_DESC, statusToMoveDesc);
				state.setAttribute(STATE_ORDERED_STEPS, orderedStatus);
			}
		}

	}	//doMove_stepstatus_comm

	
	/**
	* Action is for a committee member to update the selected stepstatus's location
	**/
	public void doUpdate_stepstatus_location_comm (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		if(params.getString("cancel") == null)
		{
			try
			{
				String currentUserId = (String)state.getAttribute(STATE_USER_ID);
				StepStatus statusToMove = null;
				String statusToMoveRef = (String)state.getAttribute(STATE_MOVE_STEP_REFERENCE);
				String location = params.getString("location");
				statusToMove = DissertationService.getStepStatus(statusToMoveRef);
				
				if((statusToMove.getCreator()!=null) && (statusToMove.getCreator().equals(currentUserId)))
				{
					CandidatePathEdit pathEdit = DissertationService.editCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
					
					//CHECK STEP'S LOCATION IS AFTER ITS PREREQUISITES
					if(checkFollowsPrereqs(state, location, statusToMove))
					{
						//UPDATING LOCATION
						pathEdit.moveStatus(statusToMoveRef, location);
						DissertationService.commitEdit(pathEdit);
					}
					else
					{
						addAlert(state, "The location of the step would have been before its prerequisite(s).");
						DissertationService.cancelEdit(pathEdit);
					}
				}
				else
					addAlert(state, "You do not have permission to move this step.");

				CandidatePath path = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
				state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(path, state, true));
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doUpdate_stepstatus_location_comm : EXCEPTION MOVING STEPS : " + e);
			}
		}
		state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);

	}	//doUpdate_stepstatus_location_comm


	/**
	* Action is present the confirm update page to a committee member
	**/
	public void doConfirm_committee_update_candidate_path(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		StringBuffer statusDesc = new StringBuffer();
		boolean alert = false;
		String[] stepStatusRefs = params.getStrings("selectedstatus");
		String candidatePathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
		if(stepStatusRefs == null)
		{
			//selected step status is null !!!!!!!!!
			addAlert(state, "You did not select a step to mark as completed.");
			alert = true;
		}
		else
		{
			String statusStatus = null;
			String stepToValidateDesc = null;
			String parentstepRef = null;
			CandidatePath path = null;
			StepStatus stepStatus = null;

			if(stepStatusRefs.length > 1)
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(true));
			else
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(false));

			state.setAttribute(STATE_STEP_STATUS_TO_VALIDATE, stepStatusRefs);
			for(int x = 0; x < stepStatusRefs.length; x++)
			{
				try
				{
					stepStatus = null;
					parentstepRef = null;
					stepToValidateDesc = "";
					stepStatus = DissertationService.getStepStatus(stepStatusRefs[x]);
					parentstepRef = stepStatus.getParentStepReference();
					//PARENT STEP REF			
					stepToValidateDesc = stepStatus.getShortInstructionsText();
					//CHECKING PERMISSIONS TO VALIDATE STEP
					if(((stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE)) 
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR))
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE)) 
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_CHAIR)))
						//&& (!DissertationService.isOardTrackedStepId(parentStepId)))
						&& (stepStatus.getAutoValidationId().equals("") || stepStatus.getAutoValidationId().equals("None")))
					{
						//VALIDATION TYPE IS COMMITTEE - CHECKING PERERQUISITES
						path = DissertationService.getCandidatePath(candidatePathRef);
						//GOT CANDIDATE'S PATH
						statusStatus = path.getStatusStatus(stepStatus);
						if(statusStatus.equals("Prerequisites not completed."))
						{
							addAlert(state, "You cannot mark this step as completed because you have not completed its prerequisites : " + stepToValidateDesc);
							alert = true;
						}
						else
						{
							//PREREQS COMPLETED - WILL VALIDATE STEP !!!!!
							statusDesc.append(stepToValidateDesc + "<br>");
						}
					}
					else
					{
						//VALIDATION TYPE IS NOT CANDIDATE - PERMISSION DENIED
						addAlert(state, "You are not authorized to mark this step as completed : " + stepToValidateDesc);
						alert = true;
					}
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : doConfirm_committee_update_candidate_path : EXCEPTION : " + e);
				}
			}
		}
		
		if(alert)
		{
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		}
		else
		{
			state.setAttribute(STATE_STEP_DESCRIPTION, statusDesc.toString());
			state.setAttribute(STATE_MODE, MODE_CONFIRM_VALIDATE_STEP);
		}

	}//doConfirm_committee_update_candidate_path
	
	/**
	* Action is for a committee member to update the selected stepstatus
	**/
	public void doUpdate_stepstatus_comm(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		if(params.getString("cancel") != null)
		{
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
			return;
		}

		//CHECK STEP COMES AFTER ITS NEW PREREQS
		String statusRef = params.getString("statusreference");
		String[] addprereqs = params.getStrings("addprereqs");
		String dummy = ""; //just to change the signature
		if(!checkFollowsPrereqs(state, statusRef, addprereqs, dummy))
		{
			addAlert(state, "The location of the step would have come before its prerequisite(s).");
			try
			{
				CandidatePath cp = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
				state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(cp, state, true));
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doUpdate_stepstatus Exception " + e);
			}
			state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
			return;
		}
		
		String instructionsText = params.getString("desc");
		String validType = params.getString("vtype");
		String[] removeprereqs = params.getStrings("removeprereqs");
		statusRef = (String)state.getAttribute(STATE_EDIT_STEP_REFERENCE);
		
		//UPDATING STATUS WITH ID
		CandidatePath path = null;
		try
		{
			path = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
			StepStatusEdit statusEdit = DissertationService.editStepStatus(statusRef);
			statusEdit.setInstructions(instructionsText);
			statusEdit.setValidationType(validType);

			if(removeprereqs != null)
			{
				for(int z = 0; z < removeprereqs.length; z++)
				{
					 statusEdit.removePrerequisiteStatus(removeprereqs[z]);
				}
			}
			
			if(addprereqs != null)
			{
				for(int z = 0; z < addprereqs.length; z++)
				{
					 statusEdit.addPrerequisiteStatus(addprereqs[z]);
				}
			}
			DissertationService.commitEdit(statusEdit);
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doUpdate_stepstatus_comm : EXCEPTION UPDATING STEP WITH ID : " + e);
		}

		state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(path, state, true));
		state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		
	}//doUpdate_stepstatus_comm
	
	/**
	* Action is for a committee member to update the candidate path
	**/
	public void doUpdate_candidate_path_comm(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String[] stepStatusRefs = (String[])state.getAttribute(STATE_STEP_STATUS_TO_VALIDATE);
		StepStatusEdit statusEdit = null;

		if(stepStatusRefs == null)
		{
			addAlert(state, "You did not select a step to mark as completed.");
		}
		else
		{
			for(int x = 0; x < stepStatusRefs.length; x++)
			{
				try
				{
					statusEdit = null;
					CandidatePath candidatePath = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
					statusEdit = DissertationService.editStepStatus(stepStatusRefs[x]);
					statusEdit.setTimeCompleted(TimeService.newTime());
					statusEdit.setCompleted(true);
					DissertationService.commitEdit(statusEdit);
					state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(candidatePath, state, true));
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : doUpdate_candidate_path_comm : status ref " + stepStatusRefs[x] + " : EXCEPTION : " + e);
				}
			}
		}
		
		state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);

	}//doUpdate_candidate_path_comm
	

	/**
	* Action is for a committee member to toggle the show complted steps status
	**/
	public void doToggle_candidate_path_display_status_comm(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		Boolean showCompleted = (Boolean)state.getAttribute(STATE_SHOW_COMPLETED_STEPS);

		if(showCompleted.booleanValue())
			state.setAttribute(STATE_SHOW_COMPLETED_STEPS, new Boolean("false"));
		else
			state.setAttribute(STATE_SHOW_COMPLETED_STEPS, new Boolean("true"));
		
		state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		
	}//doToggle_candidate_path_display_status_comm
	
	/**
	* Action is for Rackham staff to change current dissertation steps
	* to Dissertation Steps or Dissertation Steps: Music Performance
	**/
	public void doToggle_current_dissertation_steps(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		Vector orderedSteps = new Vector();
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		String dissType = (String)state.getAttribute(STATE_DISSERTATION_TYPE);
		if(dissType.equals(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE))
		{
			//toggle dissertation type
			state.setAttribute(STATE_DISSERTATION_TYPE, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
			try
			{
				state.setAttribute(STATE_CURRENT_DISSERTATION_REFERENCE,
					(String)DissertationService.getDissertationForSite(schoolSite,DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS).getReference());
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
				{
					Log.warn("chef", this + "doToggle_current_dissertation_steps " + e);
				}
			}
		}
		else
		{
			//toggle dissertation type
			state.setAttribute(STATE_DISSERTATION_TYPE, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
			String musicPerformanceRef = null;
			
			//if there is no Music Performance dissertation, create one
			try
			{
				//try to get the reference
				Dissertation tempDiss = DissertationService.getDissertationForSite(schoolSite,DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
				if(tempDiss!=null)
				{
					musicPerformanceRef = tempDiss.getReference();
				}
			}
			catch(Exception e)
			{
				if(Log.isWarnEnabled())
				{
					Log.warn("chef", this + "doToggle_current_dissertation_steps  DissertationService.getDissertationForSite " + e);
				}
			}
			
			//if there is no reference
			if(musicPerformanceRef == null)
			{
				DissertationEdit dissertationEdit = null;
				try
				{
					dissertationEdit = DissertationService.addDissertation((String)state.getAttribute(STATE_CURRENT_SITE));
					dissertationEdit.setType(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
					DissertationService.commitEdit(dissertationEdit);
					musicPerformanceRef = ((String)DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE).getReference());
				}
				catch(Exception e)
				{
					if(Log.isWarnEnabled())
					{
						Log.warn("chef", this + "doToggle_current_dissertation_steps " + e);
					}
				}
			}
			state.setAttribute(STATE_CURRENT_DISSERTATION_REFERENCE, musicPerformanceRef);
		}
		
		try
		{
			//get the correct ordered steps
			Dissertation diss = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
			state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(diss, state));
			String keyString = null;
			String stepRef = null;
			DissertationStep tempStep = null;
			Hashtable stepsHash = diss.getOrderedSteps();
			for(int x = 1; x < (stepsHash.size()+1); x++)
			{
				keyString = "" + x;
				stepRef = (String)stepsHash.get(keyString);
				tempStep = DissertationService.getDissertationStep(stepRef);
				orderedSteps.add(tempStep);
			}	
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".doToggle_current_dissertation_steps Exception " + e);
		}
		state.setAttribute(STATE_ORDERED_STEPS, orderedSteps);
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
		
	}//doToggle_current_dissertation_steps

	/**
	* Action is to cancel a committee action
	**/
	public void doCancel_committee (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
	}


	
	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	//**********************************************       DO ROUTINES - CANDIDATE          *****************************************************************
	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	
	/**
	* Action is to present the create a new dissertation step interface to a candidate
	**/
	public void doAdd_stepstatus (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();

		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String candidatePathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		Vector prereqsVector = new Vector();
		Iterator prerequisites = null;
		StepStatus tempStatus = null;
		CandidatePath path = null;
		Vector orderedStatus = new Vector();
		Hashtable statusHash = null;
		String statusRef = null;
		String keyString = null;
		try
		{
			if(DissertationService.allowAddCandidatePath(currentSite))
			{
				//ALLOW ADD PATH RETURNED TRUE : WILL ALLOW ADDING STEPSTATUS
				path = DissertationService.getCandidatePath(candidatePathRef);
				statusHash = path.getOrderedStatus();
				for(int x = 1; x < (statusHash.size()+1); x++)
				{
					keyString = "" + x;
					statusRef = (String)statusHash.get(keyString);
					tempStatus = DissertationService.getStepStatus(statusRef);
					orderedStatus.add(tempStatus);
				}
			}
			else
			{
				//ALLOW ADD PATH RETURNED FALSE : WILL NOT ALLOW ADDING STEPSTATUS
				addAlert(state, "You do not have permission to add a step to this checklist.");
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".doAdd_stepstatus " + e);
		}

		if(state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_ORDERED_STEPS, orderedStatus);
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_ADD_STEPSTATUS);
		}
		else
		{
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		}

	}//doAdd_stepstatus
	
	/**
	* Action is to create a new stepstatus and add it to the service for a candidate
	**/
	public void doAddnew_stepstatus (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		CandidatePathEdit pathEdit = null;
		String pathRef = null;
		if (params.getString("cancel")!=null)
		{
			// cancel button been clicked
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		}
		else
		{
			//CHECK STEP COMES AFTER ITS PREREQS
			String location = params.getString("location");
			String[] prereqs = params.getStrings("prereq");
			String dummy = ""; //just to change the signature
			if(!checkFollowsPrereqs(state, location, prereqs, dummy))
			{
				addAlert(state, "The location of the step would have come before its prerequisite(s).");
				state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
				try
				{
					CandidatePath path = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
					state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(pathEdit, state, false));
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".doAddnew_stepstatus Exception " + e);
				}
				state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
				return;
			}
			//END CHECK STEP COMES AFTER ITS PREREQS
			
			StepStatusEdit newStatusEdit = null;
			String instructionsText = params.getString("desc");
			try
			{
				// CREATE THE NEW STATUS
				String vType = DissertationStep.STEP_VALIDATION_TYPE_STUDENT;	// AUTOMATIC FOR PERSONAL STEPS
				
				//CURRENT SITE
				newStatusEdit = DissertationService.addStepStatus(currentSite);
				
				//NEW STATUS REF
				newStatusEdit.setInstructions(instructionsText);
				newStatusEdit.setValidationType(vType);

				if(prereqs != null)
				{
					for(int y = 0; y < prereqs.length; y++)
					{
						newStatusEdit.addPrerequisiteStatus(prereqs[y]);
					}
				}
				DissertationService.commitEdit(newStatusEdit);
				
				// APPLY THE CHANGE TO THE CANDIDATE PATH
				pathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
				
				//ADDING STATUS TO CANDIDATE PATH
				pathEdit = DissertationService.editCandidatePath(pathRef);
				int loc = pathEdit.getOrderForStatus(location);
				pathEdit.addToOrderedStatus(newStatusEdit, ++loc);
				DissertationService.commitEdit(pathEdit);
			}
			catch(PermissionException pe)
			{
				addAlert(state, "You do not have permission to modify this checklist.");
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doAddnew_stepstatus " + e);
			}
			state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(pathEdit, state, false));
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		}

	}//doAddnew_stepstatus
	
	
	
	/**
	* Action : For the candidate to edit the selected stepstatus.
	**/
	public void doEdit_stepstatus (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String selectedstatus = params.getString ("selectedstatus");
		
		if(selectedstatus == null)
		{
			addAlert(state, "You did not select a step to revise.");
		}
		else
		{
			String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
			User currentUser = (User)state.getAttribute(STATE_USER);
			String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
			Vector prereqsVector = new Vector();
			Vector currentPrereqsVector = new Vector();
			Hashtable orderedStatus = null;
			String keyString = null;
			String idString = null;
			StepStatus tempStatus = null;
			String template = TEMPLATE_CANDIDATE_EDIT_STEPSTATUS;
			List prereqs = null;
			StepStatus status = null;
			CandidatePath path = null;

			try
			{
				status = DissertationService.getStepStatus(selectedstatus);
				if(status.getParentStepReference().equals("-1") && DissertationService.allowAddCandidatePath(currentSite))
				{
					state.setAttribute(STATE_EDIT_STEPSTATUS, status);
					state.setAttribute(STATE_EDIT_STEP_REFERENCE, status.getReference());
					path = DissertationService.getCandidatePathForCandidate(currentUser.getId());
					orderedStatus = path.getOrderedStatus();
					for(int y = 1; y < (orderedStatus.size()+1); y++)
					{
						keyString = "" + y;
						idString = (String)orderedStatus.get(keyString);
						tempStatus = (StepStatus)DissertationService.getStepStatus(idString);
						if((!tempStatus.getId().equals(status.getId())) && (!status.hasPrerequisite(tempStatus.getReference())))
						{
							prereqsVector.add(tempStatus);
						}
					}
					prereqs = status.getPrereqs();
					for(int x = 0; x < prereqs.size(); x++)
					{
						tempStatus = DissertationService.getStepStatus((String) prereqs.get(x));
						if(tempStatus != null)
							currentPrereqsVector.add(tempStatus);
					}
				}
				else
				{
					addAlert(state, "You do not have permission to revise this step : " + status.getShortInstructionsText());
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : DO EDIT STEPSTATUS : EXCEPTION : " + e);
			}
			
			if(state.getAttribute(STATE_MESSAGE) != null)
			{
				state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
			}
			else
			{
				state.setAttribute(STATE_CURRENT_PREREQUISITES, currentPrereqsVector.iterator());
				state.setAttribute(STATE_PREREQUISITES, prereqsVector.iterator());
				state.setAttribute(STATE_CURRENT_TEMPLATE, template);
				state.setAttribute(STATE_MODE, MODE_CANDIDATE_EDIT_STEPSTATUS);
			}
		}
	
	}	//doEdit_stepstatus
	
	
	
	/**
	* Action is to present the confirm delete stepstatus interface to a candidate
	**/
	public void doConfirm_delete_stepstatus (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		ParameterParser params = data.getParameters();
		String[] selectedStatus = params.getStrings("selectedstatus");
		StepStatus status = null;
		String instructionsText = null;
		boolean alert = false;
		
		if(selectedStatus == null)
		{
			addAlert(state, "You did not select a step to delete.");
			alert = true;
		}
		else
		{
			if(selectedStatus.length > 1)
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(true));
			else
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(false));
			for(int x = 0; x < selectedStatus.length; x++)
			{
				try
				{
					status = DissertationService.getStepStatus(selectedStatus[x]);
					if(!status.getParentStepReference().equals("-1") || (!DissertationService.allowAddCandidatePath(currentSite)))
					{
						//SECURITY CHECK FAILED FOR ID
						instructionsText = status.getShortInstructionsText();
						addAlert(state, "You do not have permission to remove this step : " + instructionsText);
						alert = true;
					}
					else
					{
						//SECURITY CHECK SUCCEEDED FOR ID
					}
				}
				catch(Exception e)
				{
					addAlert(state, "You do not have permission to remove this step.");
					alert = true;
				}
			}

				// Did they pass the security check ?			
			if(state.getAttribute(STATE_MESSAGE) == null)
			{
				TemplateStep[] templateSteps = new TemplateStep[selectedStatus.length];
				StepStatus aStatus = null;
				for(int x = 0; x < selectedStatus.length; x++)
				{
					try
					{
						aStatus = DissertationService.getStepStatus(selectedStatus[x]);
						templateSteps[x] = new TemplateStep();
						templateSteps[x].setInstructions(aStatus.getShortInstructionsText());
					}
					catch(Exception e){}
				}
				state.setAttribute(STATE_SELECTED_STEPS, templateSteps);
			}
		}

		if(!alert)
		{
			//NO ALERT STATUS - DIRECTING TO CONFIRM DELETE
			state.setAttribute(STATE_DISSERTATION_OBJECTS_SELECTED, selectedStatus);
			state.setAttribute(STATE_MODE, MODE_CONFIRM_DELETE_DISSERTATION_STEP);
		}
		else
		{
			//ALERT ALERT ALERT - DIRECTING BACK TO VIEW CURRENT DISSERTATION
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		}

	}//doConfirm_delete_stepstatus
	
	
	/**
	* Action is to delete a candidate's personal stepstatus
	**/
	public void doDelete_step_candidate (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		if(params.getString("cancel") != null)
		{
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
			return;
		}

		String[] selectedStatus = (String[])state.getAttribute(STATE_DISSERTATION_OBJECTS_SELECTED);
				
			// FIRST CYCLE THROUGH ALL STATUS AND REMOVE PREREQUISITE REFERENCES TO THESE STATUS
		CandidatePathEdit pathEdit = null;
		StepStatus aStatus = null;
		StepStatusEdit statusEdit = null;
		List prereqs = null;
		String keyString = null;
		String valueString = null;
		
		try
		{
			pathEdit = DissertationService.editCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
			Hashtable orderedStatus = pathEdit.getOrderedStatus();
			
			for(int counter = 0; counter < selectedStatus.length; counter++)
			{
				for(int x = 1; x < (orderedStatus.size()+1); x++)
				{
					keyString = "" + x;
					valueString = (String)orderedStatus.get(keyString);
					//COMMENCING PREREQUISITE REFERENCE DELETION FOR STATUS
					aStatus = DissertationService.getStepStatus(valueString);
					//ANALYZING STATUS
					prereqs = aStatus.getPrereqs();
					for(int y = 0; y < prereqs.size(); y++)
					{
						if(prereqs.get(y).equals(selectedStatus[counter]))
						{
							statusEdit = DissertationService.editStepStatus(aStatus.getReference());
							//PREREQ ID MATCHES STATUS ID - REMOVING
							statusEdit.removePrerequisiteStatus(selectedStatus[counter]);
							DissertationService.commitEdit(statusEdit);
						}
					}
				}
			}
			
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doDelete_step_candidate : EXCEPTION REMOVE PREREQS : " + e);
		}

		
		for(int z = 0; z < selectedStatus.length; z++)
		{
			try
			{
				pathEdit.removeFromOrderedStatus(selectedStatus[z]);
				statusEdit = DissertationService.editStepStatus(selectedStatus[z]);
				//REMOVED STATUS FROM CANDIDATE PATH
				DissertationService.removeStepStatus(statusEdit);
				//REMOVED STATUS FROM SERVICE
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : EXCEPTION REMOVING STATUS : " + e);
			}
		}
		DissertationService.commitEdit(pathEdit);

		state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(pathEdit, state, false));
		state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
	}
	
	
	/**
	* Action is to present the move step page to a candidate
	**/
	public void doMove_stepstatus (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String statusToMoveRef = params.getString("selectedstatus");
		boolean alert = false;

		if(statusToMoveRef == null)
		{
			addAlert(state, "You did not select a step to move.");
			alert = true;
		}
		else
		{
			String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
			StepStatus statusToMove = null;
			StepStatus tempStatus = null;
			String pathRef = null;
			String statusToMoveDesc = "";
			CandidatePath path = null;
			Vector orderedStatus = new Vector();
			Hashtable statusHash = null;
			String statusRef = null;
			String keyString = null;
			String instructionsText = null;

			try
			{
				statusToMove = DissertationService.getStepStatus(statusToMoveRef);
				//GOT STATUS TO MOVE
				statusToMoveDesc = statusToMove.getShortInstructionsText();
				if(statusToMove.getParentStepReference().equals("-1") && DissertationService.allowAddCandidatePath(currentSite))
				{
					//PASSED SECURITY TEST - FILLING LOCATION VECTOR
					path = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
					//GOT CANDIDATE PATH
					statusHash = path.getOrderedStatus();
					//GOT STATUS HASH
					for(int x = 1; x < (statusHash.size()+1); x++)
					{
						keyString = "" + x;
						statusRef = (String)statusHash.get(keyString);
						tempStatus = DissertationService.getStepStatus(statusRef);
						if(!tempStatus.getReference().equals(statusToMoveRef))
						{
							orderedStatus.add(tempStatus);
						}
					}
				}
				else
				{
					addAlert(state, "You do not have permission to move this step : " + statusToMoveDesc);
					alert = true;
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doMove_stepstatus : EXCEPTION : " + e);
			}

			if(!alert)
			{
				state.setAttribute(STATE_MOVE_STEP_REFERENCE, statusToMoveRef);
				state.setAttribute(STATE_MODE, MODE_MOVE_STEP);
				state.setAttribute(STATE_STEP_DESC, statusToMoveDesc);
				state.setAttribute(STATE_ORDERED_STEPS, orderedStatus);
			}
		}

	}	//doMove_stepstatus

	
	/**
	* Action is to update the selected stepstatus's location
	**/
	public void doUpdate_stepstatus_location (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		if(params.getString("cancel") == null)
		{
			try
			{
				CandidatePathEdit pathEdit = DissertationService.editCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE));
				StepStatus statusToMove = null;
				String statusToMoveRef = (String)state.getAttribute(STATE_MOVE_STEP_REFERENCE);
				String location = params.getString("location");
				statusToMove = DissertationService.getStepStatus(statusToMoveRef);
				if(statusToMove.getParentStepReference().equals("-1"))
				{
					//CHECK STEP'S LOCATION IS AFTER ITS PREREQUISITES
					if(checkFollowsPrereqs(state, location, statusToMove))
					{
						//UPDATING LOCATION
						pathEdit.moveStatus(statusToMoveRef, location);
						DissertationService.commitEdit(pathEdit);
					}
					else
					{
						addAlert(state, "The location of the step would have been before its prerequisite(s).");
						DissertationService.cancelEdit(pathEdit);
					}
				}
				else
				{
					addAlert(state, "You do not have permission to move this step.");
					DissertationService.cancelEdit(pathEdit);
				}

				state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(pathEdit, state, false));
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doUpdate_stepstatus_location : EXCEPTION MOVING STEPS : " + e);
			}
		}
		state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);

	}	//doUpdate_stepstatus_location
	
	
	/**
	* Action is for a candidate to update the selected stepstatus
	**/
	public void doUpdate_stepstatus(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		if(params.getString("cancel") != null)
		{
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
			return;
		}
		
		//CHECK STEP COMES AFTER ITS NEW PREREQS
		String statusRef = params.getString("statusreference");
		String[] addprereqs = params.getStrings("addprereqs");
		String dummy = ""; //just to change the signature
		if(!checkFollowsPrereqs(state, statusRef, addprereqs, dummy))
		{
			addAlert(state, "The location of the step would have come before its prerequisite(s).");
			try
			{
				CandidatePath cp = DissertationService.getCandidatePathForCandidate((String)state.getAttribute(STATE_USER_ID));
				state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(cp, state, false));
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doUpdate_stepstatus Exception " + e);
			}
			
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
			return;
		}
		
		String instructionsText = params.getString("desc");
		String[] removeprereqs = params.getStrings("removeprereqs");
		//if(removeprereqs != null)
		//{
		//	for(int y = 0; y < removeprereqs.length; y++)
		//	{
		//		Log.info("chef", "DISSERTATION : ACTION : doUpdate_stepstatus : prerequisite to remove    : " + removeprereqs[y]);
		//	}
		//}
		//String[] addprereqs = params.getStrings("addprereqs");
		//if(addprereqs != null)
		//{
		//	for(int y = 0; y < addprereqs.length; y++)
		//	{
		//		Log.info("chef", "DISSERTATION : ACTION : doUpdate_stepstatus : prerequisiste to add      : " + addprereqs[y]);
		//	}
		//}

		statusRef = (String)state.getAttribute(STATE_EDIT_STEP_REFERENCE);
		//Log.info("chef", "DISSERTATION : ACTION : doUpdate_stepstatus : ******************************************************");
		//Log.info("chef", "DISSERTATION : ACTION : doUpdate_stepstatus : UPDATING STATUS WITH REF : " + statusRef);
		//Log.info("chef", "DISSERTATION : ACTION : doUpdate_stepstatus : ******************************************************");
		String validType = DissertationStep.STEP_VALIDATION_TYPE_STUDENT;


		CandidatePath path = null;
		try
		{
			path = DissertationService.getCandidatePathForCandidate((String)state.getAttribute(STATE_USER_ID));
			StepStatusEdit statusEdit = DissertationService.editStepStatus(statusRef);
			statusEdit.setInstructions(instructionsText);
			statusEdit.setValidationType(validType);

			if(removeprereqs != null)
			{
				for(int z = 0; z < removeprereqs.length; z++)
				{
					 statusEdit.removePrerequisiteStatus(removeprereqs[z]);
				}
			}
			
			if(addprereqs != null)
			{
				for(int z = 0; z < addprereqs.length; z++)
				{
					 statusEdit.addPrerequisiteStatus(addprereqs[z]);
				}
			}
			DissertationService.commitEdit(statusEdit);
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doUpdate_stepstatus : EXCEPTION UPDATING STEP WITH ID : " + e);
		}

		state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(path, state, false));
		state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		
	}//doUpdate_stepstatus

	
	
	
	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	//**********************************************       DO ROUTINES - ADMINISTRATION           ***********************************************************
	//*******************************************************************************************************************************************************
	//*******************************************************************************************************************************************************
	
	
	/**
	* Action is to present the create a new dissertation step interface to an adminstrator
	**/
	public void doAdd_dissertation_step (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();

		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		User currentUser = (User)state.getAttribute(STATE_USER);
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		
		Hashtable validationtable = null;
		String[] validationtypes = null;
		Vector prereqsVector = new Vector();
		Iterator prerequisites = null;
		DissertationStep tempStep = null;
		Dissertation dissertation = null;
		Vector orderedSteps = new Vector();
		Hashtable stepsHash = null;
		String stepRef = null;
		String keyString = null;
		try
		{
			//dissertation = DissertationService.getDissertationForSite(currentSite);
			dissertation = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
			stepsHash = dissertation.getOrderedSteps();

			for(int x = 1; x < (stepsHash.size()+1); x++)
			{
				keyString = "" + x;
				stepRef = (String)stepsHash.get(keyString);
				tempStep = DissertationService.getDissertationStep(stepRef);
				orderedSteps.add(tempStep);
			}
			
			if(dissertation.getSite().equals(schoolSite))
			{
				//USING SCHOOL VALIDATION TYPE TABLE
				validationtable = validationTypeTable();
				validationtypes = validationTypes();
			}
			else
			{
				//USING DEPARTMENT VALIDATION TYPE TABLE
				validationtable = deptValidationTypeTable();
				validationtypes = deptValidationTypes();
			}
			state.setAttribute(STATE_VALIDATION_TABLE, validationtable);
			state.setAttribute(STATE_VALIDATION_TYPES, validationtypes);
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : DO ADD DISSERTATION STEP : EXCEPTION : " + e);
		}

		state.setAttribute(STATE_ORDERED_STEPS, orderedSteps);
		state.setAttribute(STATE_MODE, MODE_ADMIN_ADD_DISSERTATION_STEP);
		
	}//doAdd_dissertation_step
	
	
	/**
	* Action is to create a new dissertation step and add it to the service
	**/
	public void doAddnew_dissertation_step (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String retroactiveChange = params.getString("retroactive");
		int count = 0;
		
		//RETROACTIVE
		if(retroactiveChange != null)
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(true));
		
		String location = params.getString("location");
		String section = params.getString("section");
		if(section!=null)
		{
			//for one-time conversion, add step section without validation check for steps that don't have section attribute
			Boolean disable_checking = (Boolean)state.getAttribute(STATE_INITIALIZE_STEP_SECTION);
			if((disable_checking != null) && (!disable_checking.booleanValue()))
			{
				//check section is consistent with location
				if(!checkSection(state, location, section))
				{
					addAlert(state, "The step's section was incorrect in that location.");
					return;
				}
			}
		}
		
		//CHECK STEP COMES AFTER ITS PREREQS
		String[] prereqs = params.getStrings("prereq");
		if(!checkFollowsPrereqs(state, location, prereqs))
		{
			addAlert(state, "The location of the step would have come before its prerequisite(s).");
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
			try
			{
				Dissertation diss = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
				state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(diss, state));	
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doAddnew_dissertation_step Exception " + e);
			}
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
			return;
		}
		
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		Boolean retro = (Boolean)state.getAttribute(STATE_RETROACTIVE_CHANGE);
		Dissertation dissertation = null;
		DissertationEdit dissEdit = null;
		CandidatePathEdit pathEdit = null;
	
		DissertationStepEdit stepEdit = null;
		String instructionsText = params.getString("desc");
		String validType = params.getString("vtype");
		String autoValid = params.getString("autovalid");
		
		//CREATE THE NEW STEP
		try
		{
			stepEdit = DissertationService.addDissertationStep(currentSite);
			stepEdit.setInstructionsText(instructionsText);
			stepEdit.setValidationType(validType);
			stepEdit.setAutoValidationId(autoValid);

			//Rackham sets step section, Department doesn't
			if(section!=null)
			{
				stepEdit.setSection(getSectionId(section));
			}
			
			if(prereqs != null)
			{
				for(int y = 0; y < prereqs.length; y++)
				{
					if(!("".equals(prereqs[y])))
					{
						stepEdit.addPrerequisiteStep(prereqs[y]);
					}
				}
			}
			DissertationService.commitEdit(stepEdit);
				
			// APPLY THE CHANGE TO THE APPROPRIATE DISSERTATION / CANDIDATEPATH OBJECTS
			String keystring = null;
			String refstring = null;
			boolean notdone = true;
			Dissertation tempDissertation = null;
			CandidatePath tempPath = null;
			List previousStepRefs = new Vector();
			previousStepRefs.add("start");
			
			//the current dissertation
			dissertation = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
			Hashtable orderedsteps = dissertation.getOrderedSteps();
			
			if("start".equals(location))
			{
				notdone = false;
			}
			for(int z = 1; z < (orderedsteps.size()+1); z++)
			{
				if(notdone)
				{
					keystring = "" + z;
					refstring = (String)orderedsteps.get(keystring);
					if(refstring != null)
					{
						previousStepRefs.add(refstring);
						if(refstring.equals(location))
						{
							notdone = false;
						}
					}
				}
			}

			// IF THIS IS A SCHOOL GROUP, ADD TO ALL DISSERTATIONS
			if(dissertation.getSite().equals(DissertationService.getSchoolSite()))
			{
				//CURRENT DISSERTATION IS SCHOOL'S - ADDING TO ALL DISSERTATIONS OF THIS TYPE
				//List allDissertations = DissertationService.getDissertations();
				List allDissertations = DissertationService.getDissertationsOfType(dissertation.getType());
				for(int x = 0; x < allDissertations.size(); x++)
				{
					tempDissertation = (Dissertation)allDissertations.get(x);
					dissEdit = DissertationService.editDissertation(tempDissertation.getReference());
					dissEdit.addStep(stepEdit, location);
					DissertationService.commitEdit(dissEdit);
				}  
				if(retro.booleanValue())
				{
					//RETRO IS ON !!!!! - ADDING TO ALL CANDIDATE PATHS OF THIS TYPE
					//List allPaths = DissertationService.getCandidatePaths();
					List allPaths = DissertationService.getCandidatePathsOfType(dissertation.getType());
					if(Log.isInfoEnabled())
						Log.info("chef", this + ".doAddnew_dissertation_step applying change retroactivley to " + allPaths.size() + " paths");
					for(int y = 0; y < allPaths.size(); y++)
					{
						//apply to all paths of the same type as current dissertation
						tempPath = (CandidatePath)allPaths.get(y);
						try
						{
							pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
							pathEdit.liveAddStep(stepEdit, previousStepRefs, currentSite);
							DissertationService.commitEdit(pathEdit);
							count++;
							
							//reset the current session.inactive time periodically
							if((count % 100) == 0)
								session = UsageSessionService.setSessionActive(false);
							
							//log progress periodically
							if((count % 500) == 0)
								if(Log.isInfoEnabled())
									Log.info("chef", this + ".doAddnew_dissertation_step " + count + " paths retroactively updated");
						}
						catch (Exception e)
						{	
							if(Log.isWarnEnabled())
							{
								Log.warn("chef", this + ".doAddnew_dissertation_step " + e);
							}
						}
					}
				}
			}
			else
			{
				//CURRENT DISSERTATION IS FOR A DEPARTMENT - ADDING STEP
				dissEdit = DissertationService.editDissertation(dissertation.getReference());
				dissEdit.addStep(stepEdit, location);
				DissertationService.commitEdit(dissEdit);
				if(retro.booleanValue())
				{
					//RETRO IS ON !!! - ADDING TO DEPT CANDIDATE PATHS
					List deptPaths = DissertationService.getCandidatePathsForParentSite(dissertation.getSite());
					for(int z = 0; z < deptPaths.size(); z++)
					{
						tempPath = (CandidatePath)deptPaths.get(z);
						pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
						pathEdit.liveAddStep(stepEdit, previousStepRefs, currentSite);
						DissertationService.commitEdit(pathEdit);
						count++;
						
						//reset the current session.inactive time periodically
						if((count % 100) == 0)
							session = UsageSessionService.setSessionActive(false);
						
						//log progress periodically
						if((count % 500) == 0)
							if(Log.isInfoEnabled())
								Log.info("chef", this + ".doAddnew_dissertation_step " + count + " paths retroactively updated");
					}
				}
			}
		}
		catch(PermissionException pe)
		{
			addAlert(state, "You do not have permission to modify this checklist.");
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doAddNew_dissertation_step : EXCEPTION : " + e);
		}

		if(Log.isInfoEnabled())
			Log.info("chef", this + ".doAddnew_dissertation_step at finish " + count + " paths retroactively updated");
		
		// Get the current dissertation with the latest changes.
		try
		{
			dissertation = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
		}
		catch(Exception e){}
		
		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(dissertation, state));
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);

	}//doAddnew_dissertation_step


	/**
	* Action is to present the move step interface to an adminstrator
	**/
	public void doMove_dissertation_step (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String stepToMoveRef = params.getString("selectedsteps");
		boolean alert = false;
		if(stepToMoveRef == null)
		{
			addAlert(state, "You did not select a step to move.");
			alert = true;
		}
		else
		{
			String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
			String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
			String type = (String)state.getAttribute(STATE_DISSERTATION_TYPE);
			DissertationStep stepToMove = null;
			String stepToMoveDesc = "";
			DissertationStep tempStep = null;
			Dissertation dissertation = null;
			Vector orderedSteps = new Vector();
			Hashtable stepsHash = null;
			String stepRef = null;
			String keyString = null;
			String instructionsText = null;

			try
			{
				stepToMove = DissertationService.getDissertationStep(stepToMoveRef);
				stepToMoveDesc = stepToMove.getShortInstructionsText();
				if(!DissertationService.allowUpdateDissertationStep(stepToMoveRef))
				{
					addAlert(state, "You do not have permission to move this step : " + stepToMoveDesc);
					alert = true;
				}
				else
				{
					// If not school site, perform additional department security check
					if(!schoolSite.equals(currentSite))
					{
						//DO CONFIRM DELETE STEP : NON-SCHOOL SITE : CHECKING DEPT AUTH
						if(!stepToMove.getSite().equals(currentSite))
						{
							addAlert(state, "You do not have permission to move this step : " + stepToMoveDesc);
							alert = true;
						}
					}
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doMove_dissertation_step : EXCEPTION : " + e);
			}

			if(!alert)
			{
				try
				{
					if(currentSite.equals(schoolSite))
					{
						dissertation = DissertationService.getDissertationForSite(schoolSite, type);
					}
					else
					{
						dissertation = DissertationService.getDissertationForSite(currentSite);
					}
					stepsHash = dissertation.getOrderedSteps();
					for(int x = 1; x < (stepsHash.size()+1); x++)
					{
						keyString = "" + x;
						stepRef = (String)stepsHash.get(keyString);
						tempStep = DissertationService.getDissertationStep(stepRef);
						if(!tempStep.getReference().equals(stepToMoveRef))
						{
							orderedSteps.add(tempStep);
						}
					}
				}
				catch(Exception e){}
						
				state.setAttribute(STATE_MOVE_STEP_REFERENCE, stepToMoveRef);
				state.setAttribute(STATE_CURRENT_DISSERTATION_REFERENCE, dissertation.getReference());
				state.setAttribute(STATE_MODE, MODE_MOVE_STEP);
				state.setAttribute(STATE_STEP_DESC, stepToMoveDesc);
				state.setAttribute(STATE_ORDERED_STEPS, orderedSteps);
			}
		}

	}	//doMove_Dissertation_Step


	/**
	* Action is to update the selected dissertation step's location
	**/
	public void doUpdate_step_location (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String retroactiveChange = params.getString("retroactive");
		
		//RETROACTIVE
		if(retroactiveChange == null)
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		else
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(true));
				
		//check Rackham step section is consistent with location
		String location = params.getString("location");
		String section = params.getString("section");
		if(section!=null)
		{
			if(!checkSection(state, location, section))
			{
				addAlert(state, "The step's section was incorrect in that location.");
				return;
			}
		}
		Boolean retro = (Boolean)state.getAttribute(STATE_RETROACTIVE_CHANGE);
		int count = 0;
		if(params.getString("cancel") == null)
		{
			CandidatePath tempPath = null;
			CandidatePathEdit pathEdit = null;
			Dissertation dissertation = null;
			DissertationEdit dissEdit = null;
			DissertationStep stepToMove = null;
			String currentDissRef = (String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE);
			String stepToMoveRef = (String)state.getAttribute(STATE_MOVE_STEP_REFERENCE);
			try
			{
				String previousStepPosition = null;
				Dissertation tempDissertation = null;
				dissertation = DissertationService.getDissertation(currentDissRef);
				stepToMove = DissertationService.getDissertationStep(stepToMoveRef);
				if(DissertationService.allowUpdateDissertation(dissertation.getReference()))
				{
					//CHECK STEP'S LOCATION IS AFTER ITS PREREQUISITES
					if(checkFollowsPrereqs(state, location, stepToMove))
					{
						if(dissertation.getSite().equals(DissertationService.getSchoolSite()))
						{
							DissertationStepEdit stepEdit = DissertationService.editDissertationStep(stepToMoveRef);
							stepEdit.setSection((String)getSectionId(section));
							DissertationService.commitEdit(stepEdit);
						
							//SCHOOL GROUP - WILL UPDATE ALL DISSERTATIONS OF THIS TYPE
							//List allDissertations = DissertationService.getDissertations();
							List allDissertations = DissertationService.getDissertationsOfType(dissertation.getType());
							for(int x = 0; x < allDissertations.size(); x++)
							{
								tempDissertation = (Dissertation)allDissertations.get(x);
								try
								{
									dissEdit = DissertationService.editDissertation(tempDissertation.getReference());
									dissEdit.moveStep(stepToMoveRef, location);
									DissertationService.commitEdit(dissEdit);
								}
								catch (Exception e)
								{
									if(Log.isWarnEnabled())
									{
										Log.warn("chef", this + ".doUpdate_step_location updating dissertations " + e);
									}
								}
							}
							if(retro.booleanValue())
							{
								//RETRO IS ON !!!!! - MOVING IN ALL CANDIDATE PATHS OF THIS TYPE
								//List allPaths = DissertationService.getCandidatePaths();
								List allPaths = DissertationService.getCandidatePathsOfType(dissertation.getType());
								if(Log.isInfoEnabled())
									Log.info("chef", this + ".doUpdate_step_location applying change retroactively to " + allPaths.size() + " paths");
								for(int y = 0; y < allPaths.size(); y++)
								{
									tempPath = (CandidatePath)allPaths.get(y);
									dissertation = DissertationService.getDissertation(currentDissRef);  // GET UPDATED VERSION
									try
									{
										previousStepPosition = dissertation.getOrderForStep(location);
										pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
										pathEdit.liveMoveStep(stepToMove, location, previousStepPosition);
										DissertationService.commitEdit(pathEdit);
										count++;
										
										//reset the current session.inactive time periodically
										if((count % 100) == 0)
											session = UsageSessionService.setSessionActive(false);
										
										//log progress periodically
										if((count % 500) == 0)
											if(Log.isInfoEnabled())
												Log.info("chef", this + ".doUpdate_step_location " + count + " paths retroactively updated");
									}
									catch (Exception e)
									{
										if(Log.isWarnEnabled())
										{
											Log.warn("chef", this + ".doUpdate_step_location updating paths " + e);
										}
									}				
								}
							}
						}
						else
						{
							//DEPARTMENT - WILL UPDATE THIS DISSERTATION
							dissEdit = DissertationService.editDissertation(currentDissRef);
							dissEdit.moveStep(stepToMoveRef, location);
							DissertationService.commitEdit(dissEdit);

							if(retro.booleanValue())
							{
								//RETRO IS ON !!! - ADDING TO DEPT CANDIDATE PATHS
								List deptPaths = DissertationService.getCandidatePathsForParentSite(dissertation.getSite());
								if(Log.isInfoEnabled())
									Log.info("chef", this + ".doUpdate_step_location applying change retroactively to " + deptPaths.size() + " paths");
								for(int z = 0; z < deptPaths.size(); z++)
								{
									tempPath = (CandidatePath)deptPaths.get(z);
									previousStepPosition = dissertation.getOrderForStep(stepToMoveRef);
									pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
									pathEdit.liveMoveStep(stepToMove, location, previousStepPosition);
									DissertationService.commitEdit(pathEdit);
									count++;
									
									//reset the current session.inactive time periodically
									if((count % 100) == 0)
										session = UsageSessionService.setSessionActive(false);
									
									//log progress periodically
									if((count % 500) == 0)
										if(Log.isInfoEnabled())
											Log.info("chef", this + ".doUpdate_step_location " + count + " paths retroactively updated");
								}
							}
						}
					}
					else
					{
						addAlert(state, "The location of the step would have been before its prerequisite(s).");
					}
				}
				else
				{
					addAlert(state, "You do not have permission to revise this checklist.");
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doUpdate_step_location : EXCEPTION MOVING STEPS : " + e);
			}
			
			// Reload the dissertation with changes.
			try
			{
				dissertation = DissertationService.getDissertation(currentDissRef);
			}
			catch(Exception e){}
			
			state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(dissertation, state));
		}

		if(Log.isInfoEnabled())
			Log.info("chef", this + ".doUpdate_step_location at finish " + count + " paths retroactively updated");
		
		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);

	}	//doUpdate_step_location



	/**
	* Action is for an adminstrator to edit the selected dissertation step.
	**/
	public void doEdit_dissertation_step (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String selectedstep = params.getString ("selectedsteps");
		state.setAttribute(STATE_EDIT_STEP_REFERENCE, selectedstep);
		if(selectedstep == null)
		{
			addAlert(state, "You did not select a step to revise.");
		}
		else
		{
			String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
			User currentUser = (User)state.getAttribute(STATE_USER);
			String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
			Vector prereqsVector = new Vector();
			Vector currentPrereqsVector = new Vector();
			Iterator prerequisites = null;
			DissertationStep tempStep = null;
			String template = TEMPLATE_ADMIN_EDIT_DISSERTATION_STEP;
			Hashtable orderedSteps = null;
			String keyString = null;
			String valueString = null;
			String type = null;
			
			DissertationStep step = null;
			Dissertation dissertation = null;
			try
			{	
				step = DissertationService.getDissertationStep(selectedstep);
				if(schoolSite.equals(currentSite))
				{
					type = (String)state.getAttribute(STATE_DISSERTATION_TYPE);
					dissertation = DissertationService.getDissertationForSite(schoolSite, type);
				}
				else
				{
					dissertation = DissertationService.getDissertationForSite(currentSite);
				}

			}
			catch(Exception e){}

				// IF THE CURRENT GROUP IS THE SCHOOL - ONLY SCHOOL STEPS CAN BE PREREQUISITES
			if(schoolSite.equals(currentSite))
			{
				try
				{
					orderedSteps = dissertation.getOrderedSteps();
					for(int z = 1; z < (orderedSteps.size()+1); z++)
					{
						keyString = "" + z;
						valueString = (String)orderedSteps.get(keyString);
						tempStep = DissertationService.getDissertationStep(valueString);
						if((!tempStep.getReference().equals(step.getReference())) && (!step.hasPrerequisiteStep(tempStep.getReference())))
						{
							prereqsVector.add(tempStep);
						}
						else
						{
							//Log.info("chef", "DISSERTATION : ACTION : doEdit_dissertation_step : did not add temp step");
						}
					}
				}
				catch(Exception e){}

				state.setAttribute(STATE_PREREQUISITES, prereqsVector.iterator());
				state.setAttribute(STATE_VALIDATION_TABLE, validationTypeTable());
				state.setAttribute(STATE_VALIDATION_TYPES, validationTypes());
			}
				// THE CURRENT GROUP IS A DEPARTMENT
			else
			{
					// THE STEP TO BE EDITED IS A SCHOOL STEP - ONLY PREREQS FROM THE DEPARTMENT CAN BE SELECTED
				if(step.getSite().equals(((String)state.getAttribute(STATE_SCHOOL_SITE))))
				{
					List currentPrereqs = null;
					try
					{
						currentPrereqs = dissertation.getSchoolPrereqs(step.getReference());
						if(currentPrereqs == null)
							currentPrereqs = new Vector();
					
						for(int x = 0; x < currentPrereqs.size(); x++)
						{
							tempStep = DissertationService.getDissertationStep((String) currentPrereqs.get(x));
							if(tempStep != null)
								currentPrereqsVector.add(tempStep);
						}
					}
					catch(Exception e){}

					template = TEMPLATE_ADMIN_EDIT_DISSERTATION_STEP_PREREQS;
					try
					{
						orderedSteps = dissertation.getOrderedSteps();
						for(int x = 1; x < (orderedSteps.size()+1); x++)
						{
							keyString = "" + x;
							valueString = (String)orderedSteps.get(keyString);
							tempStep = DissertationService.getDissertationStep(valueString);
							if(!currentPrereqs.contains(tempStep.getReference()) && (!tempStep.getSite().equals(schoolSite)))
								prereqsVector.add(tempStep);
						}
					}
					catch(Exception e){}
					state.setAttribute(STATE_CURRENT_PREREQUISITES, currentPrereqsVector.iterator());
					state.setAttribute(STATE_VALIDATION_TABLE, validationTypeTable());
					state.setAttribute(STATE_VALIDATION_TYPES, validationTypes());
				}
				else
				{
						// THE STEP TO BE EDITED IS A DEPARTMENT STEP, DEPARTMENT'S STEPS AND THE SCHOOL'S STEPS CAN BE PREREQUISITES
					try
					{
						orderedSteps = dissertation.getOrderedSteps();
						for(int y = 1; y < (orderedSteps.size()+1); y++)
						{
							keyString = "" + y;
							valueString = (String)orderedSteps.get(keyString);
							tempStep = DissertationService.getDissertationStep(valueString);
							if((!tempStep.getReference().equals(step.getReference())) && (!step.hasPrerequisiteStep(tempStep.getReference())))
								prereqsVector.add(tempStep);
						}
					}
					catch(Exception e){}
					state.setAttribute(STATE_VALIDATION_TABLE, deptValidationTypeTable());
					state.setAttribute(STATE_VALIDATION_TYPES, deptValidationTypes());

				}

				state.setAttribute(STATE_PREREQUISITES, prereqsVector.iterator());
			}

			state.setAttribute(STATE_CURRENT_TEMPLATE, template);
			state.setAttribute(STATE_DISSERTATION_STEP, step);
			state.setAttribute(STATE_MODE, MODE_ADMIN_EDIT_DISSERTATION_STEP);
		}
	
	}	//doEdit_Dissertation_Step


	/**
	* Action is for an adminstrator to update the selected step
	**/
	public void doUpdate_step(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		String retroactiveChange = params.getString("retroactive");
		if(retroactiveChange == null)
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		else
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(true));
		Boolean retro = (Boolean)state.getAttribute(STATE_RETROACTIVE_CHANGE);
		String section = null;
		if(params.getString("cancel") != null)
		{
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
			return;
		}
		String stepReference = params.getString("stepreference");

		//Rackham can change the section of a step, so check validity of this section at this location
		if(((String)state.getAttribute(STATE_USER_ROLE)).equals(SCHOOL_ROLE))
		{
			section = params.getString("section");

			//one time conversion - add step section without validation check for steps that don't have section attribute
			Boolean disable_checking = (Boolean)state.getAttribute(STATE_INITIALIZE_STEP_SECTION);
			if((disable_checking != null) && (!disable_checking.booleanValue()))
			{
				//check section is consistent with location
				if(!checkSectionEdit(state, stepReference, section))
				{
					addAlert(state, "The step's section is incorrect.");
					return;
				}
			}
		}
		
		//CHECK STEP COMES AFTER ITS NEW PREREQS
		String[] addprereqs = params.getStrings("addprereqs");
		if(!checkFollowsPrereqs(state, stepReference, addprereqs))
		{
			addAlert(state, "The location of the step would have come before its prerequisite(s).");
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
			try
			{
				Dissertation diss = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
				state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(diss, state));
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".doAddnew_dissertation_step Exception " + e);
			}
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
			return;
		}
		
		String instructionsText = params.getString("desc");
		String validType = params.getString("vtype");
		String[] removeprereqs = params.getStrings("removeprereqs");
		
		String autoValid = params.getString("autovalid");
		stepReference = (String)state.getAttribute(STATE_EDIT_STEP_REFERENCE);
		
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String type = (String)state.getAttribute(STATE_DISSERTATION_TYPE);

		Dissertation dissertation = null;
		CandidatePath tempPath = null;
		CandidatePathEdit pathEdit = null;
		int count = 0;
		try
		{
			if(currentSite.equals(schoolSite))
			{
				dissertation = DissertationService.getDissertationForSite(schoolSite, type);
			}
			else
			{
				dissertation = DissertationService.getDissertationForSite(currentSite);
			}
			DissertationStep before = DissertationService.getDissertationStep(stepReference);
			DissertationStepEdit stepEdit = DissertationService.editDissertationStep(stepReference);
			stepEdit.setInstructionsText(instructionsText);
			stepEdit.setValidationType(validType);
			stepEdit.setSection((String)getSectionId(section));
			if(autoValid != null)
			{
				stepEdit.setAutoValidationId(autoValid);
				//AUTO VALID ID IS NOT NULL : WILL APPLY
			}

			if(removeprereqs != null)
			{
				for(int z = 0; z < removeprereqs.length; z++)
				{
					 stepEdit.removePrerequisiteStep(removeprereqs[z]);
				}
			}
			
			if(addprereqs != null)
			{
				for(int z = 0; z < addprereqs.length; z++)
				{
					 stepEdit.addPrerequisiteStep(addprereqs[z]);
				}
			}
			DissertationService.commitEdit(stepEdit);
			if(retro.booleanValue())
			{
				//RETRO IS ON !!!
				if(dissertation.getSite().equals(DissertationService.getSchoolSite()))
				{
					// UPDATE ALL CANDIDATE PATHS OF THIS TYPE
					
					//SCHOOL GROUP - ADDING TO ALL CANDIDATE PATHS OF THIS TYPE
					//List deptPaths = DissertationService.getCandidatePaths();
					List deptPaths = DissertationService.getCandidatePathsOfType(dissertation.getType());
					if(Log.isInfoEnabled())
						Log.info("chef", this + ".doUpdate_step applying change retroactively to " + deptPaths.size() + " paths");
					for(int y =  0; y < deptPaths.size(); y++)
					{
						tempPath = (CandidatePath)deptPaths.get(y);
						try
						{
							pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
							pathEdit.liveUpdateStep(before, stepEdit);
							DissertationService.commitEdit(pathEdit);
							count++;
							
							//reset the current session.inactive time periodically
							if((count % 100) == 0)
								session = UsageSessionService.setSessionActive(false);
							
							//log progress periodically
							if((count % 500) == 0)
								if(Log.isInfoEnabled())
									Log.info("chef", this + ".doUpdate_step " + count + " paths retroactively updated");
						}
						catch (Exception e)
						{
							if(Log.isWarnEnabled())
							{
								Log.warn("chef", this + ".doUpdate_step getCandidatePaths " + e);
							}
						}
					}
				}
				else
				{
					// UPDATE DEPT'S CANDIDATE PATHS
					
					//DEPT - ADDING TO DEPT CANDIDATE PATHS OF THIS TYPE
					List deptPaths = DissertationService.getCandidatePathsForParentSite(dissertation.getSite());
					if(Log.isInfoEnabled())
						Log.info("chef", this + ".doUpdate_step applying change retroactively to " + deptPaths.size() + " paths");
					for(int z = 0; z < deptPaths.size(); z++)
					{
						tempPath = (CandidatePath)deptPaths.get(z);
						try
						{
							pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
							pathEdit.liveUpdateStep(before, stepEdit);
							DissertationService.commitEdit(pathEdit);
							count++;
							
							//reset the current session.inactive time periodically
							if((count % 100) == 0)
								session = UsageSessionService.setSessionActive(false);
							
							//log progress periodically
							if((count % 500) == 0)
								if(Log.isInfoEnabled())
									Log.info("chef", this + ".doUpdate_step " + count + " paths retroactively updated");
						}
						catch (Exception e)
						{
							if(Log.isWarnEnabled())
							{
								Log.warn("chef", this + ".doUpdate_step getCandidatePathsForParentSite " + e);
							}
						}
					}
				}
				
				//DissertationService.commitEdit(pathEdit);
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doUpdate_step : EXCEPTION UPDATING STEP : " + e);
		}

		if(Log.isInfoEnabled())
			Log.info("chef", this + ".doUpdate_step at finish " + count + " paths retroactively updated");
		
		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(dissertation, state));
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
		
	}//doUpdate_step


	/**
	* Action is for a adminstrator to update the selected step's prerequisites
	**/
	public void doUpdate_step_prereqs(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(((JetspeedRunData)data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		if(params.getString("cancel") != null)
		{
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
			return;
		}

		if(Log.isInfoEnabled())
			Log.info("chef", this + ".doUpdate_step_prereqs called in retroactive update");
		
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String schoolstepRef = (String)state.getAttribute(STATE_EDIT_STEP_REFERENCE);
		String retroactiveChange = params.getString("retroactive");
		if(retroactiveChange == null)
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		else
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(true));
		Boolean retro = (Boolean)state.getAttribute(STATE_RETROACTIVE_CHANGE);
		Dissertation dissertation = null;
		CandidatePathEdit pathEdit = null;
		CandidatePath tempPath = null;
		
		try
		{
			dissertation = DissertationService.getDissertationForSite(currentSite);
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doUpdate_step_prereqs : * EXCEPTION * : " + e);
		}

		String[] removeprereqs = params.getStrings("removeprereqs");
		DissertationEdit dissEdit = null;
		try
		{
			dissEdit = DissertationService.editDissertation(dissertation.getReference());
			//GOT DISSERTATION EDIT CLONE
			if(removeprereqs != null)
			{
				for(int y = 0; y < removeprereqs.length; y++)
				{
					dissEdit.removeSchoolPrereq(schoolstepRef, removeprereqs[y]);
				}
			}
			String[] addprereqs = params.getStrings("addprereqs");
			if(addprereqs != null)
			{
				for(int y = 0; y < addprereqs.length; y++)
				{
					dissEdit.addSchoolPrereq(schoolstepRef, addprereqs[y]);
				}
			}
			DissertationService.commitEdit(dissEdit);
			
			if(retro.booleanValue())
			{
				try
				{
					//RETRO IS ON !!!!!!!!!!!!!!!!!
					List deptPaths = DissertationService.getCandidatePathsForParentSite(currentSite);
					for(int z = 0; z < deptPaths.size(); z++)
					{
						tempPath = (CandidatePath)deptPaths.get(z);
						if(removeprereqs != null)
						{
							//THERE ARE PREREQS TO REMOVE
							for(int x = 0; x < removeprereqs.length; x++)
							{
								pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
								pathEdit.liveRemoveSchoolPrereq(schoolstepRef, removeprereqs[x]);
								DissertationService.commitEdit(pathEdit);
							}
						}
					
						if(addprereqs != null)
						{
							//THERE ARE PREREQS TO REMOVE
							for(int v = 0; v < addprereqs.length; v++)
							{
								pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
								pathEdit.liveAddSchoolPrereq(schoolstepRef, addprereqs[v]);
								DissertationService.commitEdit(pathEdit);
							}
						}
					}
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : doUpdate_step_prereqs : EXCEPTION : " + e);
				}
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doUpdate_step_prereqs :  EXCEPTION  : " + e);
		}

		//Reload the dissertation with the changes
		try
		{
			dissertation = DissertationService.getDissertationForSite(currentSite);
		}
		catch(Exception e)
		{
			Log.warn("chef", "DISSERTATION : ACTION : doUpdate_step_prereqs : EXCEPTION  : " + e);
		}

		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(dissertation, state));
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);

	}//doUpdate_step_prereqs


	/**
	* Action is to present the confirm delete dissertation step interface to an adminstrator
	**/
	public void doConfirm_delete_step (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		ParameterParser params = data.getParameters();
		String[] selectedSteps = params.getStrings("selectedsteps");
		boolean alert = false;

		DissertationStep step = null;
		String instructionsText = null;
		if(selectedSteps == null)
		{
			addAlert(state, "You did not select a step to delete.");
			alert = true;
		}
		else
		{
			if(selectedSteps.length > 1)
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(true));
			else
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(false));
			for(int x = 0; x < selectedSteps.length; x++)
			{
				
				if(!DissertationService.allowRemoveDissertationStep(selectedSteps[x]))
				{
					//SECURITY CHECK FAILED FOR ID
					try
					{
						step = DissertationService.getDissertationStep(selectedSteps[x]);
						instructionsText = step.getShortInstructionsText();
						addAlert(state, "You do not have permission to remove this step : " + instructionsText);
						alert = true;
					}
					catch(Exception e)
					{
						addAlert(state, "You do not have permission to remove this step.");
						alert = true;
					}
				}
				else
				{
					// If not school site, perform additional deparment security check
					if(!schoolSite.equals(currentSite))
					{
						//DO CONFIRM DELETE STEP : NON-SCHOOL SITE : CHECKING DEPT AUTH
						try
						{
							step = DissertationService.getDissertationStep(selectedSteps[x]);
							if(step.getSite().equals(currentSite))
							{
								//DO CONFIRM DELETE STEP : NON-SCHOOL SITE : STEP SITE MATCHES CURRENT SITE
							}
							else
							{
								//DO CONFIRM DELETE STEP : NON-SCHOOL SITE : STEP SITE DOES NOT MATCH CURRENT SITE
								instructionsText = step.getShortInstructionsText();
								addAlert(state, "You do not have permission to remove this step : " + instructionsText);
								alert = true;
							}
						}
						catch(Exception e)
						{
							addAlert(state, "You do not have permission to remove this step.");
							alert = true;
						}
					}
					else
					{
						//DO CONFIRM DELETE STEP : SCHOOL SITE : DEPT AUTH = TRUE
					}
										
					//SECURITY CHECK SUCCEEDED FOR ID
				}
			}

				// Did they pass the security check ?			
			if(state.getAttribute(STATE_MESSAGE) == null)
			{
				TemplateStep[] templateSteps = new TemplateStep[selectedSteps.length];
				DissertationStep aStep = null;
				for(int x = 0; x < selectedSteps.length; x++)
				{
					try
					{
						aStep = DissertationService.getDissertationStep(selectedSteps[x]);
						templateSteps[x] = new TemplateStep();
						templateSteps[x].setInstructions(aStep.getInstructionsText());
					}
					catch(Exception e){}
				}
				state.setAttribute(STATE_SELECTED_STEPS, templateSteps);
			}
		}

		if(!alert)
		{
			//NO ALERT STATUS - DIRECTING TO CONFIRM DELETE
			state.setAttribute(STATE_DISSERTATION_OBJECTS_SELECTED, selectedSteps);
			state.setAttribute(STATE_MODE, MODE_CONFIRM_DELETE_DISSERTATION_STEP);
		}
		else
		{
			//ALERT ALERT ALERT - DIRECTING BACK TO VIEW CURRENT DISSERTATION
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
		}

	}//doConfirm_delete_step


	/**
	* Action is for an adminstrator to delete a dissertation step
	**/
	public void doDelete_step_admin (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		if(params.getString("cancel") != null)
		{
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
			return;
		}
		String[] selectedStepRefs = (String[])state.getAttribute(STATE_DISSERTATION_OBJECTS_SELECTED);
		String retroactiveChange = params.getString("retroactive");
		if(retroactiveChange == null)
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		else
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(true));
		Boolean retro = (Boolean)state.getAttribute(STATE_RETROACTIVE_CHANGE);
		
			// FIRST CYCLE THROUGH ALL STEPS AND REMOVE PREREQUISITE REFERENCES TO THESE STEPS
		Dissertation currentDissertation = null;
		List allSteps = DissertationService.getDissertationSteps();
		DissertationStep aStep = null;
		DissertationStepEdit stepEdit = null;
		CandidatePathEdit pathEdit = null;
		List prereqs = null;
		int count = 0;
		
		for(int counter = 0; counter < selectedStepRefs.length; counter++)
		{
			//COMMENCING PREREQUISITE REFERENCE DELETION FOR STEP
			for(int z = 0; z < allSteps.size(); z++)
			{
				aStep = (DissertationStep)allSteps.get(z);
				//ANALYZING STEP
				prereqs = aStep.getPrerequisiteStepReferences();
				for(int x = 0; x < prereqs.size(); x++)
				{
					if(prereqs.get(x).equals(selectedStepRefs[counter]))
					{
						try
						{
							stepEdit = DissertationService.editDissertationStep(selectedStepRefs[counter]);
							//PREREQ REF MATCHES STEP REF - REMOVING
							stepEdit.removePrerequisiteStep(selectedStepRefs[counter]);
							DissertationService.commitEdit(stepEdit);
						}
						catch(Exception e)
						{
							Log.warn("chef", "DISSERTATION : ACTION : doDelete_step_admin : EXCEPTION : " + e);
						}
					}
				}
			}
		}
		for(int x = 0; x < selectedStepRefs.length; x++)
		{
			try
			{
				if(Log.isInfoEnabled())
					Log.info("chef", this + ".doDelete_step_admin deleting step " + x);
				
				// GET THE CURRENT DISSERTATION OBJECT AND UPDATE
				List candidatePathsToUpdate = null;
				CandidatePath tempPath = null;
				DissertationStep tempStep = null;
				currentDissertation = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
				
				// IF THIS IS THE SCHOOL DEPARTMENT, DELETE STEP FROM ALL DEPARTMENT CHECKLIST-DEFINITIONS
				Dissertation tempDiss = null;
				DissertationEdit dissEdit = null;
				if(currentDissertation.getSite().equals(DissertationService.getSchoolSite()))
				{
					//SCHOOL DEPARTMENT : REMOVING FROM ALL CHECKLIST-DEFS OF THIS TYPE!
					//List allChecklistDefs = DissertationService.getDissertations();
					List allChecklistDefs = DissertationService.getDissertationsOfType(currentDissertation.getType());
					for(int y = 0; y < allChecklistDefs.size(); y++)
					{
						tempDiss = (Dissertation)allChecklistDefs.get(y);
						dissEdit = DissertationService.editDissertation(tempDiss.getReference());
						dissEdit.removeStep(selectedStepRefs[x]);
						DissertationService.commitEdit(dissEdit);
						
						//REMOVED STEP FROM CHECKLIST-DEF
					}
					
					// IF RETRO, UPDATE ALL CANDIDATE PATHS OF THIS TYPE
					if(retro.booleanValue())
					{
						//candidatePathsToUpdate = DissertationService.getCandidatePaths();
						candidatePathsToUpdate = DissertationService.getCandidatePathsOfType(currentDissertation.getType());
					}
				}
				else
				{
					//  NON-SCHOOL DEPT : REMOVE FROM THIS DISSERTATION ONLY
					dissEdit = DissertationService.editDissertation(currentDissertation.getReference());
					dissEdit.removeStep(selectedStepRefs[x]);
					DissertationService.commitEdit(dissEdit);
					
					//  IF RETRO, UPDATE ALL CANDIDATE PATHS FOR DEPARTMENT
					if(retro.booleanValue())
						candidatePathsToUpdate = DissertationService.getCandidatePathsForParentSite(currentDissertation.getSite());
				}
				
				// IF THIS IS A RETROACTIVE CHANGE, REMOVE FROM ALL CANDIDATE PATHS OF THIS TYPE
				if(retro.booleanValue())
				{
					if(Log.isInfoEnabled())
						Log.info("chef", this + ".doDelete_step_admin deleting step from " + candidatePathsToUpdate.size() + " paths");
					
					for(int v = 0; v < candidatePathsToUpdate.size(); v++)
					{
						tempPath = (CandidatePath)candidatePathsToUpdate.get(v);
						tempStep = DissertationService.getDissertationStep(selectedStepRefs[x]);
						pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
						pathEdit.liveRemoveStep(tempStep);
						DissertationService.commitEdit(pathEdit);
						count++;
						
						//reset the current session.inactive time periodically
						if((count % 100) == 0)
							session = UsageSessionService.setSessionActive(false);
						
						//log progress periodically
						if((count % 500) == 0)
							if(Log.isInfoEnabled())
								Log.info("chef", this + ".doDelete_step_admin " + count + " paths retroactively updated");
					}
				}
				stepEdit = DissertationService.editDissertationStep(selectedStepRefs[x]);
				DissertationService.removeDissertationStep(stepEdit);
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : EXCEPTION REMOVING STEP : " + e);
			}
		}

		// Reload the dissertation with the changes.
		try
		{
			currentDissertation = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE));
		}
		catch(Exception e){}
		
		if(Log.isInfoEnabled())
			Log.info("chef", this + ".doDelete_step_admin at finish " + count + " paths retroactively updated");
		
		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, getTemplateSteps(currentDissertation, state));
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
		
	}//doDelete_step_admin


	/**
	* Action is to cancel an admin action
	**/
	public void doCancel_admin (RunData data, Context context)
	{
		// cancel button been clicked
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
	}

	/**
	* Action is to cancel an admin validation step action
	**/
	public void doCancel_admin_validate_step (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CANDIDATE_PATH);
	}
	
	//***************************************************************************************************************************************************
	//**********************************************************  CANDIDATE PATH DO ROUTINES ************************************************************
	//***************************************************************************************************************************************************
	
	
	/**
	* Action is to toggle the show complted steps status
	**/
	public void doToggle_candidate_path_display_status(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		Boolean showCompleted = (Boolean)state.getAttribute(STATE_SHOW_COMPLETED_STEPS);

		if(showCompleted.booleanValue())
			state.setAttribute(STATE_SHOW_COMPLETED_STEPS, new Boolean("false"));
		else
			state.setAttribute(STATE_SHOW_COMPLETED_STEPS, new Boolean("true"));
		
		state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		
	}//doToggle_candidate_path_display_status
	

// ******************************************************************************************************************************************************************
	
	/**
	* Action is to present the confirm mark as completed page to a candidate.
	**/
	public void doConfirm_candidate_update_candidate_path(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		boolean alert = false;
		StringBuffer statusDesc = new StringBuffer();
		String[] stepStatusRefs = params.getStrings("selectedstatus");
		
		if(stepStatusRefs == null)
		{
			addAlert(state, "You did not select a step to mark as completed.");
			alert = true;
		}
		else
		{
			String statusStatus = null;
			String stepToValidateDesc = null;
			String parentStepRef = null;
			User currentUser = null;
			CandidatePath path = null;
			StepStatus stepStatus = null;
			if(stepStatusRefs.length > 1)
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(true));
			else
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(false));

			state.setAttribute(STATE_STEP_STATUS_TO_VALIDATE, stepStatusRefs);
			for(int x = 0; x < stepStatusRefs.length; x++)
			{
				try
				{
					stepStatus = null;
					parentStepRef = null;
					stepToValidateDesc = "";
					stepStatus = DissertationService.getStepStatus(stepStatusRefs[x]);
					
					//PARENT STEP REF
					parentStepRef = stepStatus.getParentStepReference();
					stepToValidateDesc = stepStatus.getShortInstructionsText();
					
					//CHECKING PERMISSIONS TO VALIDATE STEP
					if(((stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT))
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR)) 
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE)) 
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_SCHOOL))
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT))
						|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT)))
						&& (stepStatus.getAutoValidationId().equals("") || stepStatus.getAutoValidationId().equals("None")))
					{
						//VALIDATION TYPE IS CANDIDATE - CHECKING PERERQUISITES
						currentUser = (User)state.getAttribute(STATE_USER);
						path = DissertationService.getCandidatePathForCandidate(currentUser.getId());
						statusStatus = path.getStatusStatus(stepStatus);
						if(statusStatus.equals("Prerequisites not completed."))
						{
							addAlert(state, "You cannot mark this step as completed because you have not completed its prerequisites : " + stepToValidateDesc);
							alert = true;
						}
						else
						{
							//PREREQS COMPLETED - WILL VALIDATE STEP !!!!!
							statusDesc.append(stepToValidateDesc + "<br>");
						}
					}
					else
					{
						//VALIDATION TYPE IS NOT CANDIDATE - PERMISSION DENIED
						addAlert(state, "You are not authorized to mark this step as completed : " + stepToValidateDesc);
						alert = true;
					}
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : BUILD_CANDIDATE_UPDATE_CANDIDATE_PATH : EXCEPTION : " + e);
				}
			}
		}
		
		if(alert)
		{
			state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
		}
		else
		{
			state.setAttribute(STATE_STEP_DESCRIPTION, statusDesc.toString());
			state.setAttribute(STATE_MODE, MODE_CONFIRM_VALIDATE_STEP);
		}

	}//doConfirm_candidate_update_candidate_path
	
	/**
	* Action is to present the confirm mark as completed page to an adminstrator.
	**/	
	public void doConfirm_admin_update_candidate_path(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		String currentGroup = (String)state.getAttribute(STATE_CURRENT_SITE);
		ParameterParser params = data.getParameters();
		String[] stepStatusRefs = params.getStrings("selectedstatus");
		boolean alert = false;
		StringBuffer stepDesc = new StringBuffer();
		
		if(stepStatusRefs == null)
		{
			//step status is null !!!!!!!!!
			addAlert(state, "You did not select a step to mark as completed.");
			alert = true;
		}
		else
		{
			String statusStatus = null;
			String stepToValidateDesc = null;
			String selectedCandidate = null;
			CandidatePath path = null;
			StepStatus stepStatus = null;
			String parentstepRef = null;
			boolean authorizedValidator = false;
			if(stepStatusRefs.length > 1)
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(true));
			else
				state.setAttribute(STATE_MULTIPLE_VALIDATION, new Boolean(false));

			state.setAttribute(STATE_STEP_STATUS_TO_VALIDATE, stepStatusRefs);
			
			for(int x = 0; x < stepStatusRefs.length; x++)
			{
				try
				{
					stepStatus = DissertationService.getStepStatus(stepStatusRefs[x]);
					parentstepRef = stepStatus.getParentStepReference();
					stepToValidateDesc = stepStatus.getShortInstructionsText();
					selectedCandidate = (String)state.getAttribute(STATE_SELECTED_CANDIDATE_ID);
					path = DissertationService.getCandidatePathForCandidate(selectedCandidate);
					statusStatus = path.getStatusStatus(stepStatus);

					if((!stepStatus.getAutoValidationId().equals("")) && (!stepStatus.getAutoValidationId().equals("None")))
					{
						if(currentGroup.equals(DissertationService.getSchoolSite()))
						{
							//OARD-TRACKED STEP - SCHOOL ADMIN - PERMISSION GRANTED
							if(statusStatus.equals("Prerequisites not completed."))
							{
								
								//prereqs not completed - directing to view_path
								addAlert(state, "You cannot mark this step as completed because its prerequisites are not completed : " + stepToValidateDesc);
								alert = true;
							}
							else
							{
								//PREREQS COMPLETED - MOVING TO VALIDATION CONFIRMATION SCREEN !!!
								stepDesc.append(stepToValidateDesc + "<br>");
							}
						}
						else
						{
							//OARD-TRACKED STEP - NON SCHOOL ADMIN - PERMISSION DENIED
							authorizedValidator = false;
							addAlert(state, "You are not authorized to mark this step as completed : " + stepToValidateDesc);
							alert = true;
						}
					}
					else
					{
						//NON-OARD-TRACKED STEP !!!
						if(parentstepRef.equals("-1"))
						{
							//PERSONAL STEP - PERMISSION DENIED
							authorizedValidator = false;
							addAlert(state, "You cannot mark as completed candidate's personal steps : " + stepToValidateDesc);
							alert = true;
						}
						else
						{
							
							//NOT A PERSONAL STEP - CHECKING PERMISSION
							if((stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_SCHOOL)) || (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_SCHOOL)))
							{
								//VALIDATION TYPE SCHOOL OR SCHOOL/STUDENT
								if(currentGroup.equals(DissertationService.getSchoolSite()))
								{
									//CURRENT GROUP IS SCHOOL - AUTHORIZED VALIDATOR
									authorizedValidator = true;
								}
								else
								{
									//CURRENT GROUP IS NOT SCHOOL - NOT AUTHORIZED VALIDATOR !
									addAlert(state, "You are not authorized to mark this step as completed : " + stepToValidateDesc);
									alert = true;
								}
							}
							else
							{
								//VALIDATION TYPE OTHER
								if(currentGroup.equals(stepStatus.getSite()) 
									|| ((!currentGroup.equals(DissertationService.getSchoolSite())) 
									&& ((stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT)) 
									|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT)))))
								{
									//(CURRENT GROUP == STATUS DEPT) || (CURRENT GROUP != SCHOOL && STATUS DEPT = CURRENT GROUP) - AUTHORIZED VALIDATOR
									authorizedValidator = true;
								}
								else
								{
									//(CURRENT GROUP != SCHOOL) && (CURRENT GROUP != STATUS DEPT) - NOT AUTHORIZED VALIDATOR !
									addAlert(state, "You are not authorized to mark this step as completed : " + stepToValidateDesc);
									alert = true;
								}
							}
						}
					
						if(authorizedValidator)
						{	
							if(statusStatus.equals("Prerequisites not completed."))
							{	
								//prereqs not completed - directing to view_path
								addAlert(state, "You cannot mark this step as completed because its prerequisites are not completed : " + stepToValidateDesc);
								alert = true;
							}
							else
							{	
								//PREREQS COMPLETED - MOVING TO VALIDATION CONFIRMATION SCREEN !!!
								stepDesc.append(stepToValidateDesc + "<br>");
							}
						}
					}
				
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : doConfirm_admin_update_candidate_path : status id : " + stepStatusRefs[x] + " : EXCEPTION : " + e);
				}
			}
		}
		
		if(alert)
		{
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CANDIDATE_PATH);
		}
		else
		{
			state.setAttribute(STATE_STEP_DESCRIPTION, stepDesc.toString());
			state.setAttribute(STATE_MODE, MODE_CONFIRM_VALIDATE_STEP);
		}

	}//doConfirm_admin_update_candidate_path

	
	/**
	* Action is for a candidate to update a candidate path.
	**/
	public void doUpdate_candidate_path(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		String[] stepStatusRefs = (String[])state.getAttribute(STATE_STEP_STATUS_TO_VALIDATE);
		boolean alert = false;
		
		if(stepStatusRefs == null)
		{
			addAlert(state, "You did not select a step to mark as completed.");
			alert = true;
		}
		else
		{
			String pathRef = (String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE);
			String stepRef = null;
			DissertationStep parentStep = null;
			String[] prereqs = null;
			StepStatus tempStatus = null;
			CandidatePath candidatePath = null;

			StepStatusEdit statusEdit = null;
			for(int x = 0; x < stepStatusRefs.length; x++)
			{
				try
				{
					candidatePath = DissertationService.getCandidatePath(pathRef);
					statusEdit = DissertationService.editStepStatus(stepStatusRefs[x]);
					statusEdit.setTimeCompleted(TimeService.newTime());
					statusEdit.setCompleted(true);
					DissertationService.commitEdit(statusEdit);
					state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(candidatePath, state, false));
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : doUpdate_candidate_path : validating id : " + stepStatusRefs[x] + " : EXCEPTION :  " + e);
				}
			}
		}

		state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);

	}//doUpdate_candidate_path


	/**
	* Action is for an adminstrator to update a candidate path.
	**/
	public void doUpdate_candidate_path_admin(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String[] stepStatusRefs = (String[])state.getAttribute(STATE_STEP_STATUS_TO_VALIDATE);
		StepStatusEdit statusEdit = null;

		if(stepStatusRefs == null)
		{
			addAlert(state, "You did not select a step to mark as completed.");
		}
		else
		{
			for(int x = 0; x < stepStatusRefs.length; x++)
			{
				try
				{
					statusEdit = null;
					CandidatePath candidatePath = DissertationService.getCandidatePathForCandidate((String)state.getAttribute(STATE_SELECTED_CANDIDATE_ID));
					statusEdit = DissertationService.editStepStatus(stepStatusRefs[x]);
					statusEdit.setTimeCompleted(TimeService.newTime());
					statusEdit.setCompleted(true);
					DissertationService.commitEdit(statusEdit);
					state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(candidatePath, state, false));
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : doUpdate_candidate_path_admin : status id " + stepStatusRefs[x] + " : EXCEPTION : " + e);
				}
			}
		}
		
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CANDIDATE_PATH);
	}
	
	
	/**
	* Action is to present the alphabetical candidate chooser to an adminstrator.
	**/	
	public void doView_candidates_list(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		
		state.setAttribute(STATE_MODE, MODE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER);
	}

	/**
	* Action is to cancel the action of a candidate.
	**/
	public void doCancel_candidate (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.setAttribute(STATE_MODE, MODE_CANDIDATE_VIEW_PATH);
	}
	
	//***************************************************************************************************************************************************
	//**********************************************************  ADMINISTRATIVE FUNCTIONS **************************************************************
	//***************************************************************************************************************************************************

	/**
	* Action is to present the alphabetical candidate chooser to an adminstrator.
	**/
	public void doChecklist(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		
		//get the current site
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		
		if (currentSite==null)
		{
			currentSite = PortalService.getCurrentSiteId();
			state.setAttribute(STATE_CURRENT_SITE, currentSite);
		}
		
		//get the dissertation type
		String stepsType = (String)state.getAttribute(STATE_DISSERTATION_TYPE);
		
		if(stepsType == null)
		{
			Log.warn("chef", this + "doChecklist() STATE_DISSERTATION_TYPE == null");
			return;
		}
		
		/** SET UP THE CANDIDATE-CHOOSER LETTERS FOR THE CURRENT DISSERTATION TYPE
		 *  If Rackham site or STATE_LETTERS == null, get the letters for the
		 *  currently displayed dissertation
		 */
		if (currentSite.equals(schoolSite) || state.getAttribute(STATE_LETTERS) == null)
		{
			//set STATE_LETTERS
			if(currentSite.equals(schoolSite))
			{
				if(stepsType.equals(DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS))
				{
					//to Dissertation Steps
					if(state.getAttribute(STATE_LETTERS_DISSERTATION_STEPS)!= null)
					{
						state.setAttribute(STATE_LETTERS, (Vector)state.getAttribute(STATE_LETTERS_DISSERTATION_STEPS));
					}
					else
					{
						state.setAttribute(STATE_LETTERS, getLetters(schoolSite, currentSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS));
					}
				}
				else if (stepsType.equals(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE))
				{
					//to Music Performance
					if(state.getAttribute(STATE_LETTERS_MUSIC_PERFORMANCE) != null)
					{
						state.setAttribute(STATE_LETTERS, (Vector)state.getAttribute(STATE_LETTERS_MUSIC_PERFORMANCE));
					}
					else
					{
						state.setAttribute(STATE_LETTERS, getLetters(schoolSite, currentSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE));
					}
				}
			}
			else
			{
				
				state.setAttribute(STATE_LETTERS, getLetters(schoolSite, currentSite, stepsType));
			}
				
		}
		state.setAttribute(STATE_MODE, MODE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER);
		
	}//doChecklist
	
	/**
	* Action is to present the view current dissertation page to an administrator.
	**/
	public void doAdministration(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		state.removeAttribute(STATE_USERS_LIST);
		state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
	}

	/**
	* Action is to show the list of candidates within an alphabetical choice to an adminstrator.
	**/
	public void doAlphabetical_choice(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String letterChosen = params.getString("selected");
		String department = (String)state.getAttribute(STATE_CURRENT_SITE);
		String stepsType = (String)state.getAttribute(STATE_DISSERTATION_TYPE);
		String currentSite = (String)state.getAttribute(STATE_CURRENT_SITE);
		List sortedUsers = null;
		User[] allUsers = null;
		Hashtable allUsersEmplids = new Hashtable();
		User tmpUser = null;
		String chefid = null;
		String emplid = null;
		
		//IF SCHOOL SITE, GET ALL USERS WITH THE CURRENT DISSERTATION STEPS TYPE
		if(letterChosen == null)
		{
			//%% when would this happen???
			allUsers = DissertationService.getAllUsersForSite(department, stepsType);
		}
		else
		{
			try
			{
				if((currentSite).equals((String)state.getAttribute(STATE_SCHOOL_SITE)))
				{
					//if Rackham, get all users for this type and letter
					sortedUsers = DissertationService.getSortedUsersOfTypeForLetter(stepsType, letterChosen);
				}
				else
				{
					//if department, get all users for this site and letter
					sortedUsers = DissertationService.getSortedUsersOfParentForLetter(currentSite, letterChosen);
				}
				allUsers = (User[])sortedUsers.toArray(new User[sortedUsers.size()]);
			}
			catch(Exception e) 
			{ 
				Log.warn("chef", this + ".doAlphabeticalChoice allUsers " + e);
			}
		}
		
		//get emplids for all users
		for(int i = 0; i< allUsers.length; i++)
		{
			tmpUser = allUsers[i];
			chefid = tmpUser.getId();
			try
			{
				emplid = ((CandidateInfo)DissertationService.getInfoForCandidate(chefid)).getEmplid();
			}
			catch(IdUnusedException e)
			{
				Log.warn("chef", this + ".doAlphabeticalChoice map emplid " + e);
			}
			catch(PermissionException e)
			{
				Log.warn("chef", this + ".doAlphabeticalChoice map emplid " + e);
			}
			allUsersEmplids.put(chefid,emplid);
		}
		state.setAttribute(STATE_USERS_LIST, allUsers);
		state.setAttribute(STATE_USERS_EMPLID_MAP, allUsersEmplids);
		state.setAttribute(STATE_MODE, MODE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER);
		
	}//doAlphabetical_choice
	
	/**
	* Action is to show a candidate's checklist to an adminstrator.
	**/
	public void doCandidate_chosen (RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ());
		ParameterParser params = data.getParameters();
		String selectedCandidate = params.getString("selectedcandidate");
		
		if((selectedCandidate == null) || selectedCandidate.equals(""))
		{
			addAlert(state, "No candidate was chosen.");
			state.setAttribute(STATE_MODE, MODE_ADMIN_ALPHABETICAL_CANDIDATE_CHOOSER);
		}
		else
		{
			//MOVING TO ADMIN VIEW CANDIDATE'S PATH MODE
			try
			{
				User selectedUser = UserDirectoryService.getUser(selectedCandidate);
				
				String displayName = selectedUser.getDisplayName();
				if(displayName == null)
					displayName = "The candidate ";
				state.setAttribute(STATE_SELECTED_CANDIDATE_NAME, displayName);

				
				String emplid = DissertationService.getEmplidForUser(selectedCandidate);
				if(emplid == null)
					emplid = "";
				state.setAttribute(STATE_SELECTED_CANDIDATE_EMPLID, emplid);
				
				String siteId = DissertationService.getParentSiteForUser(selectedUser.getId());
				Site site = SiteService.getSite(siteId);
				String groupDisplayName = site.getTitle();
				if(groupDisplayName == null)
					groupDisplayName = "";
				
				state.setAttribute(STATE_SELECTED_CANDIDATE_GROUP_DISPLAY_NAME, groupDisplayName);
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doCandidate_chosen : EXCEPTION : " + e);
			}
			
			CandidatePath candidatePath = null;
			try
			{
				candidatePath = DissertationService.getCandidatePathForCandidate(selectedCandidate);
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : doCandidate_chosen : EXCEPTION : " + e);
			}
			TemplateStep[] templateSteps = getTemplateSteps(candidatePath, state, false);
			state.setAttribute(STATE_SELECTED_CANDIDATE_ID, selectedCandidate);
			state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, templateSteps);
			state.setAttribute(STATE_MODE, MODE_ADMIN_VIEW_CANDIDATE_PATH);
		}
	}
	

	/**
	* Populate the state object, if needed - override to do something!
	*/
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData rundata)
	{
		super.initState(state, portlet, rundata);

		PortletConfig config = portlet.getPortletConfig();
		String elementId = portlet.getID();
		
		//current site
		String currentSite = null;
		if (state.getAttribute(STATE_CURRENT_SITE)==null)
		{
			currentSite = PortalService.getCurrentSiteId();
			state.setAttribute(STATE_CURRENT_SITE, currentSite);
		}

		//current user
		User currentUser = null;
		if (state.getAttribute (STATE_USER)==null)
		{
			currentUser = UserDirectoryService.getCurrentUser();
			state.setAttribute(STATE_USER, currentUser);
		}
		else
		{
			currentUser = (User)state.getAttribute(STATE_USER);
		}
		if(currentUser == null)
		{
			// EVERYTHING DEPENDS ON THIS BEING SET, SO IF IT IS NOT, BAIL . . .
			return;
		}
		else
		{
			if (state.getAttribute (STATE_USER_ID)==null)
			{
				state.setAttribute(STATE_USER_ID, currentUser.getId());
			}
			if (state.getAttribute (STATE_USER_DISPLAY_NAME)==null)
			{
				String displayName = currentUser.getDisplayName();
				if(displayName == null)
				{
					displayName = "";
				}
				state.setAttribute(STATE_USER_DISPLAY_NAME, displayName);
			}
		}

		//Rackham site
		String schoolSite = null;
		if (state.getAttribute(STATE_SCHOOL_SITE)==null)
		{
			schoolSite = DissertationService.getSchoolSite();
			state.setAttribute(STATE_SCHOOL_SITE, schoolSite);
		}
		
		//Music Performance site
		String musicPerformanceSite = null;
		if (state.getAttribute(STATE_MUSIC_PERFORMANCE_SITE)==null)
		{
			musicPerformanceSite = DissertationService.getMusicPerformanceSite();
			state.setAttribute(STATE_MUSIC_PERFORMANCE_SITE, musicPerformanceSite);
		}
		
		/**
		if (state.getAttribute(STATE_INITIALIZE_DEPT_DISSERTATION_TYPE)==null)
		{
			initDeptDissertationType(state);
			state.setAttribute(STATE_INITIALIZE_DEPT_DISSERTATION_TYPE, new Boolean("true"));
		}
		
		if (state.getAttribute(STATE_INITIALIZE_CANDIDATE_PATH_TYPE)==null)
		{
			initCandidatePathType(state);
			state.setAttribute(STATE_INITIALIZE_CANDIDATE_PATH_TYPE, new Boolean("true"));
		}
		*/

		if (state.getAttribute(STATE_RETROACTIVE_CHANGE)==null)
		{
			state.setAttribute(STATE_RETROACTIVE_CHANGE, new Boolean(false));
		}

		if (state.getAttribute(STATE_USERS_LIST) == null)
		{
			state.setAttribute(STATE_USERS_LIST, new User[0]);
		}
		
		Boolean showCompleted = new Boolean(true);
		if (state.getAttribute(STATE_SHOW_COMPLETED_STEPS) == null)
		{
			state.setAttribute(STATE_SHOW_COMPLETED_STEPS, showCompleted);
		}

		//see CandidateInfo.getExternalValidation()
		if (state.getAttribute(STATE_AUTO_VALIDATION_IDS) == null)
		{
			String[] ids = new String[13];
			ids[0] = "None";
			ids[1] = "1";
			ids[2] = "2";
			ids[3] = "3";
			ids[4] = "4";
			ids[5] = "5";
			ids[6] = "6";
			ids[7] = "7";
			ids[8] = "8";
			ids[9] = "9";
			ids[10] = "10";
			ids[11] = "11";
			ids[12] = "12";
			state.setAttribute(STATE_AUTO_VALIDATION_IDS, ids);
		}
		
		// THE MEAT - CANDIDATE, COMMITTEE, ADMINISTRATOR, STUDENT OR DEAN ROLE DECISION TREE
		if(state.getAttribute(STATE_USER_ROLE) == null)
		{	
			// GET THE EMPLID FROM THE SERVICE OR FROM UMIAC IF IT DOES NOT EXIST
			String emplid = DissertationService.getEmplidForUser(currentUser.getId());
			if((emplid.equals("")) || (!DissertationService.isCandidate(currentUser.getId())))
			{
				//CURRENT USER IS NOT A CANDIDATE - MUST BE COMMITTEE, ADMINISTRATOR OR DEAN
				Site deptSite = null;
				TemplateStep[] dissertationSteps = null;
			
				// GET DEPARTMENT AND SITE
				User selectedCandidate = null;
				try
				{
					deptSite = SiteService.getSite(currentSite);
					String fullName = deptSite.getTitle();
					if(fullName == null)
					{
						fullName = "";
					}
					state.setAttribute(STATE_USERS_DEPT_FULL_NAME, fullName);
				}
				catch(IdUnusedException e)
				{
					Log.warn("chef", this + ".initState() SiteService.getSite(" + currentSite + ") " + e);
				}
			
				boolean allowAddDiss = DissertationService.allowAddDissertation(currentSite);
				if(!allowAddDiss)
				{
					if((currentSite.equals(schoolSite)) || (currentSite.matches("^diss[0-9]*$")))
					{
						state.setAttribute(STATE_USER_ROLE, DEAN_ROLE);
						state.setAttribute (STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
						
						// SET UP THE DISSERTATION FOR USER'S SITE
						if(state.getAttribute(STATE_CURRENT_DISSERTATION_STEPS) == null) 
						{
							initDissertation(state, currentSite);
						}
						
					}//DEAN
					else
					{
						String siteId = deptSite.getId();
						state.setAttribute(STATE_USER_ROLE, COMMITTEE_ROLE);
						TemplateStep[] templateSteps = null;
						CandidatePath candidatePath = null;
						String modeString = null;
						try
						{
							candidatePath = DissertationService.getCandidatePathForSite(siteId);
						}
						catch(PermissionException p)
						{
							Log.warn("chef", this + ".initState() DissertationService.getCandidatePathForSite(" + siteId + ") " + p);
						}
						if(candidatePath == null)
						{	
							state.setAttribute(STATE_MODE, MODE_NO_CANDIDATE_PATH);
						}
						else
						{
							String candidId = candidatePath.getCandidate();
							try
							{
								selectedCandidate = UserDirectoryService.getUser(candidId);
							}
							catch(IdUnusedException e)
							{
								Log.warn("chef", this + ".initState() UserDirectoryService.getUser(" + candidId + ") " + e);
							}
							state.setAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE, candidatePath.getReference());
							state.setAttribute(STATE_SELECTED_CANDIDATE_ID, candidId);
							state.setAttribute(STATE_SELECTED_CANDIDATE_DISPLAY_NAME, selectedCandidate.getDisplayName());
							state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, getTemplateSteps(candidatePath, state, true));
							state.setAttribute(STATE_MODE, MODE_COMMITTEE_VIEW_CANDIDATE_PATH);
						}
						
					}//COMMITTEE MEMBER
				}
				else
				{
					//CURRENT USER IS AN ADMINISTRATOR
					state.setAttribute(STATE_USER_ROLE, ADMIN_ROLE);
					if(currentSite.equals(schoolSite))
					{
						state.setAttribute(STATE_USER_ROLE, SCHOOL_ROLE);
					}
					state.setAttribute (STATE_MODE, MODE_ADMIN_VIEW_CURRENT_DISSERTATION);
					
					// SET UP THE DISSERTATION FOR USER'S SITE
					if(state.getAttribute(STATE_CURRENT_DISSERTATION_STEPS) == null) 
					{
						initDissertation(state, currentSite);
					}
				}//ADMINISTRATOR
			}
			else
			{
				//CURRENT USER IS A CANDIDATE
				state.setAttribute(STATE_USER_ROLE, CANDIDATE_ROLE);
				TemplateStep[] templateSteps = null;
				CandidatePath path = null;
				String modeString = null;
				
				//If User has a Candidate Path, check if its site id was set by the upload tool
				try
				{
					path = DissertationService.getCandidatePathForCandidate(currentUser.getId());
					
					//If site id was set by the upload tool, update the student site id now
					if(path!=null && ((String)path.getSite()).equals((String)state.getAttribute(STATE_USER_ID)))
					{
						path = updateCandidatePathSiteId(state, path);
						if(path == null)
						{
							state.setAttribute(STATE_MODE, MODE_CANDIDATE_NO_DISSERTATION);
						    return;
						}
					}
				
				}
				catch(PermissionException p)
				{
					//We don't have a CandidatePath so put up that message
					Log.warn("chef", this + ".initState() DissertationService.getCandidatePathForCandidate(" + currentUser.getId() + ") " + p);
					addAlert(state, "You do not have permission to create a candidate path.");
					state.setAttribute(STATE_MODE, MODE_NO_CANDIDATE_PATH);
					return;
				}
				
				//If User has no CandidatePath, create one using current site id
				if(path == null)
				{
					CandidatePathEdit pathEdit = null;
					String parentSite = DissertationService.getParentSiteForUser(currentUser.getId());
					
					try
					{
						Dissertation dissertation = DissertationService.getDissertationForSite(parentSite);
						if(dissertation == null)
						{
							//DISSERTATION FOR USERS PARENT DEPT IS NULL - USING SCHOOL !!!
							try
							{
								//if Candidate is in Music Performance, use Music Performance type dissertation
								if(DissertationService.isMusicPerformanceCandidate(currentUser.getId()))
								{
									dissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
								}
								else
								{
									dissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
								}
							}
							catch(Exception e1)
							{
								Log.warn("chef", this + ".initState() DissertationService.getDissertationForSite(" + schoolSite + ") " + e1);
							}
							if(dissertation == null)
							{
								//SCHOOL DISSERTATION IS NULL - BAIL OUT  !!!
								state.setAttribute(STATE_MODE, MODE_CANDIDATE_NO_DISSERTATION);
								return;
							}
							try
							{
								//dissertation != null
								pathEdit = DissertationService.addCandidatePath(dissertation, currentSite);
								
								//CANDIDATE PATH CREATED FOR USER
								pathEdit.setSite(currentSite);
								
								//set alphabetical candidate chooser letter
								pathEdit.setSortLetter(getSortLetter(currentUser.getId()));
								
								//set the parent department site id
								pathEdit.setParentSite(DissertationService.getInfoForCandidate(currentUser.getId()).getParentSite());
								
								//set the dissertation type
								pathEdit.setType(dissertation.getType());
								
								templateSteps = getTemplateSteps(pathEdit, state, false);
								DissertationService.commitEdit(pathEdit);
							}
							catch(PermissionException p)
							{
								Log.warn("chef", this + ".initState DissertationService.addCandidatePath(" + currentSite + ") " + "dissertation " + dissertation.getReference() + " " + p);
							}
						}
						else
						{
							try
							{
								// CANDIDATE PATH CREATED FOR USER
								pathEdit = DissertationService.addCandidatePath(dissertation, currentSite);

								//set alphabetical candidate chooser letter
								pathEdit.setSortLetter(getSortLetter(currentUser.getId()));
								
								//set the parent department site id
								pathEdit.setParentSite(DissertationService.getInfoForCandidate(currentUser.getId()).getParentSite());
								
								//set the dissertation type
								pathEdit.setType(dissertation.getType());
								
								templateSteps = getTemplateSteps(pathEdit, state, false);
								DissertationService.commitEdit(pathEdit);
							}
							catch(PermissionException p)
							{
								Log.warn("chef", this + ".initState() DissertationService.addCandidatePath(" + currentSite + ") " + "dissertation " + dissertation.getReference() + " " + p);
							}
						}
						modeString = MODE_CANDIDATE_VIEW_PATH;
						state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, templateSteps);
						state.setAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE, pathEdit.getReference());
					}
					catch(Exception e2)
					{
						Log.warn("chef", this + ".initState() DissertationService.getDissertationForSite(" + parentSite + ") " + e2);
					}
				}
				else
				{
					//path already present and site id updated
					modeString = MODE_CANDIDATE_VIEW_PATH;
					
					// CONSTRUCT THE DISPLAY OBJECTS FROM THE CANDIDATE PATH AND IT'S STEPS
					templateSteps = getTemplateSteps(path, state, false);
					state.setAttribute(STATE_CANDIDATE_PATH_TEMPLATE_STEPS, templateSteps);
					state.setAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE, path.getReference());
				}
				state.setAttribute(STATE_MODE, modeString);
				
			}//CANDIDATE
		}
	}//initState
	

	/**
	* Initialize new CandidatePath attribute Type.
	* @param state SessionState.
	*/
	protected void initCandidatePathType(SessionState state)
	{
		String musicPerformanceSite = DissertationService.getMusicPerformanceSite();
		String parentSite = null;
		List allCandidatePaths = DissertationService.getCandidatePaths();
		CandidatePath tempPath = null;
		for(int x = 0; x < allCandidatePaths.size(); x++)
		{
			tempPath = (CandidatePath)allCandidatePaths.get(x);
			if(tempPath.getType()==null || ((String)tempPath.getType()).equals(""))
			{
				CandidatePathEdit pathEdit = null;
				try
				{
					CandidateInfo info = DissertationService.getInfoForCandidate(tempPath.getCandidate());
					parentSite = info.getParentSite();
					try
					{
						pathEdit = DissertationService.editCandidatePath(tempPath.getReference());
						if(parentSite.equals(musicPerformanceSite))
						{
							pathEdit.setType(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
					
						}
						else
						{
							pathEdit.setType(DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
						}
						DissertationService.commitEdit(pathEdit);
					}
					catch(Exception e)
					{
						if(Log.isWarnEnabled())
						{
							Log.warn("Chef", this + " initCandidatePathType DissertationService.editCandidatePath " + e);
						}
					}
				}
				catch(Exception e)
				{
					if(Log.isWarnEnabled())
					{
						Log.warn("Chef", this + " initCandidatePathType DissertationService.getInfoForCandidate " + e);
					}
				}
			}
		}
		
	}//initCandidatePathType
	
	/**
	* Initialize new Dissertation attribute Type.
	* @param state SessionState.
	*/
	protected void initDeptDissertationType(SessionState state)
	{
		String musicPerformanceSite = DissertationService.getMusicPerformanceSite();
		String schoolSite = DissertationService.getSchoolSite();
		String site = null;
		List allDissertations = DissertationService.getDissertations();
		Dissertation tempDissertation = null;
		for(int x = 0; x < allDissertations.size(); x++)
		{
			tempDissertation = (Dissertation)allDissertations.get(x);
			if(tempDissertation.getType()==null || (tempDissertation.getType().equals("")))
			{
				site = tempDissertation.getSite();
				if(!site.equals(schoolSite))
				{
					try
					{
						DissertationEdit dissEdit = DissertationService.editDissertation(tempDissertation.getReference());
						if (site.equals(musicPerformanceSite))
						{
							dissEdit.setType(DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
						}
						else
						{
							dissEdit.setType(DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
						}
						DissertationService.commitEdit(dissEdit);
					}
					catch(Exception e)
					{
						if(Log.isWarnEnabled())
						{
							Log.warn("chef", this + ".initDissertationType " + e);
						}
					}
				}
			}
		}

	}//initDeptDissertationType

	/**
	* Populate the Rackham/Dept Site DissertationSteps
	* @param currentSite The site id of the current site.
	*/
	protected void initDissertation(SessionState state, String currentSite)
	{
		String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
		TemplateStep[] dissertationSteps = null;
		boolean musicPerformanceSite = (((String)state.getAttribute(STATE_CURRENT_SITE)).equals((String)state.getAttribute(STATE_MUSIC_PERFORMANCE_SITE)))? true : false;
		
		// SET UP THE DISSERTATION STEPS
		Dissertation currentDis = null;
		try
		{
			if(currentSite.equals(schoolSite))
			{
				//Rackham site, initialize to dissertation type Dissertation Steps
				currentDis = DissertationService.getDissertationForSite(currentSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
				
				//school dissertation type needs to be set the first time
				if(currentDis==null)
				{
					currentDis = DissertationService.getDissertationForSite(currentSite);
					if(currentDis!=null)
					{
						DissertationEdit currentDisEdit = DissertationService.editDissertation(currentDis.getReference());
						currentDisEdit.setType(DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
						DissertationService.commitEdit(currentDisEdit);
					}
				}
			}
			else if(musicPerformanceSite)
			{
				//Music Performance site, initialize to dissertation type Dissertation Steps: Music Performance
				currentDis = DissertationService.getDissertationForSite(currentSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
			}
			else
			{
				//any other Department site, initialize to Department dissertation
				currentDis = DissertationService.getDissertationForSite(currentSite);
			}
			
			//disable section checking before the (one-time) conversion of steps to include a section attribute
			boolean disable_section_checking = isNullSection(currentDis);
			state.setAttribute(STATE_INITIALIZE_STEP_SECTION, new Boolean(disable_section_checking));
			
			//DISSERTATION AND DISSERTATION TYPE FOR USER'S SITE
			if(currentDis!=null)
			{
				state.setAttribute(STATE_CURRENT_DISSERTATION_REFERENCE, currentDis.getReference());
				state.setAttribute(STATE_DISSERTATION_TYPE, currentDis.getType());
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".initLetters_Dissertation DissertationService.getDissertationForSite(" + currentSite + ") " + e);
		}
		
		// NO DISSERTATION YET?
		if(currentDis == null)
		{
			//NO DISSERTATION FOR THIS DEPARTMENT - CREATING NEW ONE
			DissertationEdit dissertationEdit = null;
			if(currentSite.equals(schoolSite))
			{
				//school site
				try
				{
					//create the school dissertation that we couldn't find earlier
					dissertationEdit = DissertationService.addDissertation(currentSite);
					
					//initialize as DISSERTATION_TYPE_DISSERTATION_STEPS
					dissertationEdit.setType(DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
					DissertationService.commitEdit(dissertationEdit);
					state.setAttribute(STATE_DISSERTATION_TYPE, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
				}
				catch(PermissionException p)
				{
					Log.warn("chef", "DISSERTATION : ACTION : initLetters_Dissertation DissertationService.addDissertation(" + currentSite + ") " + p);
				}
			}
			else
			{
				//department site
				Dissertation schoolDissertation = null;
				try
				{
					//create the department dissertation that we couldn't find earlier
					if(musicPerformanceSite)
					{
						//Music Performance site, initialize to school dissertation type Dissertation Steps: Music Performance
						schoolDissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
					}
					else
					{
						//other Department site, initialize to school dissertation type Dissertation Steps
						schoolDissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
					}
				}
				catch(Exception e)
				{
					Log.warn("chef", "DISSERTATION : ACTION : initLetters_Dissertation DissertationService.getDissertationForSite(" + schoolSite + ") " + e);
				}
				if(schoolDissertation == null)
				{
					//SCHOOL DISSERTATION IS NULL - CANNOT INITIALIZE - DIRECTING TO WAIT SCREEN
					state.setAttribute(STATE_MODE, MODE_SYSTEM_NOT_INITIALIZED);
				}
				else
				{
					try
					{
						dissertationEdit = DissertationService.addDissertation(currentSite);
						Hashtable schoolOrderedSteps = schoolDissertation.getOrderedSteps();
									
						//CURRENT GROUP IS NOT THE SCHOOL - ADDING SCHOOL'S ORDERED STEPS
						dissertationEdit.setOrderedSteps((Hashtable)schoolOrderedSteps.clone());
						
						//initialize Dissertation Type
						dissertationEdit.setType(schoolDissertation.getType());
						DissertationService.commitEdit(dissertationEdit);
						
						//put Dissertation Type in state
						state.setAttribute(STATE_DISSERTATION_TYPE,schoolDissertation.getType());
					}
					catch(PermissionException p)
					{
						Log.warn("chef", this + ".initDissertation DissertationService.addDissertation(" + currentSite + ") " + p);
					}
					catch(Exception e)
					{
						Log.warn("chef", this + ".initDissertation DissertationService.addDissertation(" + currentSite + ") " + e);
					}
				}
			}
			dissertationSteps = getTemplateSteps(dissertationEdit, state);
			state.setAttribute(STATE_CURRENT_DISSERTATION_REFERENCE, dissertationEdit.getReference());
		}
		else
		{
			dissertationSteps = getTemplateSteps(currentDis, state);
		}
		
		state.setAttribute(STATE_CURRENT_DISSERTATION_STEPS, dissertationSteps);
		
		//Set up letters
		if(currentSite.equals(schoolSite))
		{
			//pre-populate letters based on type
			state.setAttribute(STATE_LETTERS_DISSERTATION_STEPS, getLetters(schoolSite, currentSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS));
			state.setAttribute(STATE_LETTERS_MUSIC_PERFORMANCE, getLetters(schoolSite, currentSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE));
		}

		// initialize STATE_LETTERS
		state.setAttribute(STATE_LETTERS, getLetters(schoolSite, currentSite, (String)state.getAttribute(STATE_DISSERTATION_TYPE)));
		 
	}//initDissertation

	/**
	* Get the collection of display objects corresponding to a Dissertation for inserting into a velocity template.
	* @param dissertation The Dissertation.
	* @param state The SessionState.
	* @return The collection of Template Steps.
	*/
	private TemplateStep[] getTemplateSteps(Dissertation dissertation, SessionState state)
	{
		if(dissertation == null)
		{
			//DISSERTATION IS NULL - RETURNING EMPTY SET OF TEMPLATESTEPS
			return(new TemplateStep[0]);
		}
		// DO WE HAVE CANDIDATES, YET ?
		boolean activeCandidates = false;
		
		//CHECKING FOR ACTIVE CANDIDATES
		if(dissertation.getSite().equals(DissertationService.getSchoolSite()))
		{
			//CURRENTLY SCHOOL DEPARTMENT
			activeCandidates = DissertationService.isCandidatePathOfType(dissertation.getType());
		}
		else
		{
			//CURRENTLY NON-SCHOOL DEPARTMENT
			activeCandidates = DissertationService.getActivePathsForSite(dissertation.getSite());
		}
		state.setAttribute(STATE_ACTIVE_PATHS, new Boolean(activeCandidates));
		Hashtable order = dissertation.getOrderedSteps();
		int numberOfSteps = order.size();
		DissertationStep step = null;
		String keyString = null;
		String ref = null;
		TemplateStep[] tsteps = new TemplateStep[numberOfSteps];
		for(int x = 1; x < (numberOfSteps+1); x++)
		{
			try
			{
				keyString = "" + x;
				ref = (String)order.get(keyString);
				step = DissertationService.getDissertationStep(ref);
				tsteps[x-1] = new TemplateStep();
				tsteps[x-1].setStepReference(step.getReference());
				tsteps[x-1].setInstructions(step.getInstructions());
				tsteps[x-1].setValidationImage(step.getValidationType());
				tsteps[x-1].setPrereqs(dissertation.getPrerequisiteStepsDisplayString(step));
				
				//Unconverted Rackham steps and Departmental steps have no section attribute
				if((String)step.getSection()!=null)
				{
					try
					{
						tsteps[x-1].setSection(Integer.parseInt(step.getSection()));
					}
					catch(NumberFormatException ignore){}
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", "DISSERTATION : ACTION : GET TEMPLATE STEPS : EXCEPTION : " + e);
			}
		}
		return tsteps;

	}//getTemplateSteps

	
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
						Log.warn("chef", this + "getTemplateSteps get section id of the parent for path " + path.getId() + " " + e);
					}
				}
				
				templateSteps[x-1] = new TemplateStep();
	
				if(committee)
				{
					templateSteps[x-1].setShowCheckbox(commManipulableStatus(path, state, status));
				}
				else
				{
					templateSteps[x-1].setShowCheckbox(manipulableStatus(path, state, status));
				}

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
					//earlier steps did not have a section, so deal with missing section
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".getTemplateSteps " + "step " + keyString + " " + e);
			}
		}
		return templateSteps;
		
	}//getTemplateSteps
	
	/**
	* Determine whether there is (an old) DissertationStep without a section attribute so it can be set without validation.
	* @param currentDis The Dissertation.
	* @return True if any of the orderedsteps have a null section attribute.
	*/
	public boolean isNullSection(Dissertation currentDis)
	{
		boolean retVal = false;
		String keystring = null;
		String refstring = null;
		if(currentDis!=null)
		{
			Hashtable orderedsteps = currentDis.getOrderedSteps();
			for(int i = 1; i < (orderedsteps.size()+1); i++)
			{
				keystring = "" + i;
				refstring = (String)orderedsteps.get(keystring);
				try
				{
					DissertationStep step = DissertationService.getDissertationStep(refstring);
					if((step.getSection()==null) || (step.getSection().equals("")))
						retVal = true;
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".isNullSection: Exception " + e);
				}
			}
		}
		return retVal;
		
	}//isNullSection
	
	/**
	* Determine whether the StepStatus is mainipulable by the candidate.  A selection checkbox will be placed by manipulable steps only.
	* @param path The CandidatePath.
	* @param state The SessionState.
	* @param stepStatus The StepStatus in question.
	* @return True if the StepStatus is mainipulable by the candidate, false if it is not.
	*/
	public boolean manipulableStatus(CandidatePath path, SessionState state, StepStatus stepStatus)
	{
		boolean retVal = false;
		try
		{
			if(stepStatus.getParentStepReference().equals("-1"))
			{
				//MAINIPULABLE STATUS : CANDIDATE PERSONAL STEP : AUTOMATICALLY GETS A CHECKBOX
				retVal = true;
			}
			else if(((stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_SCHOOL))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT)))
				&& (stepStatus.getAutoValidationId().equals("") || stepStatus.getAutoValidationId().equals("None")))
			{
				//VALIDATION TYPE IS CANDIDATE - CHECKING PERERQUISITES
				User currentUser = (User)state.getAttribute(STATE_USER);
				String statusStatus = path.getStatusStatus(stepStatus);
				if(statusStatus.equals("Prerequisites not completed."))
				{
					//prereqs not completed - returning false
					retVal = false;
				}
				else
				{
					//prereqs not completed - returning true
					retVal = true;
				}
			}
			else
			{
				//OARD STEP OR NOT STUDENT VALIDATE - returning false
				retVal = false;
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".manipulableStatus: Exception: " + e);
		}
		return retVal;
		
	} //manipulableStatus


	/**
	* Determine whether the StepStatus is mainipulable by the committee member.  A selection checkbox will be placed by manipulable steps only.
	* @param path The CandidatePath.
	* @param state The SessionState.
	* @param stepStatus The StepStatus in question.
	* @return True if the StepStatus is mainipulable by the committee member, false if it is not.
	*/
	public boolean commManipulableStatus(CandidatePath path, SessionState state, StepStatus stepStatus)
	{
		boolean retVal = false;
		try
		{
			User currentUser = (User)state.getAttribute(STATE_USER);
			
			//if step status is missing CHEF:creator property
			if((stepStatus.getCreator()!=null) && (stepStatus.getCreator().equals(currentUser.getId())))
			{
				// CREATED BY CURRENT USER - AUTOMATICALLY GETS A CHECKBOX
				retVal = true;
			}
			else if(stepStatus.getParentStepReference().equals("-1"))
			{
				// CANDIDATE'S PERSONAL STEP - AUTOMATICALLY GETS NO CHECKBOX
				retVal = false;
			}
			else if(((stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE))
				|| (stepStatus.getValidationType().equals(DissertationStep.STEP_VALIDATION_TYPE_CHAIR)))
				&& (stepStatus.getAutoValidationId().equals("") || stepStatus.getAutoValidationId().equals("None")))
			{
				//VALIDATION TYPE HAS COMMITTEE / CHAIR - CHECKING PERERQUISITES
				String statusStatus = path.getStatusStatus(stepStatus);
				
				if(statusStatus.equals("Prerequisites not completed."))
				{
					//prereqs not completed - returning false
					retVal = false;
				}
				else
				{
					//PREREQS COMPLETED - returning true
					retVal = true;
				}
			}
			else
			{
				//OARD STEP OR NOT COMMITTEE/CHAIR VALIDATION - returning false
				retVal = false;
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".commManipulableStatus: Exception: " + e);
		}
		return retVal;
		
	}//commManipulableStatus
	
	/**
	* Check that DissertationStep location is not before any of its prerequisites.
	* @param SessionState state The session state.
	* @param String location The stepReference string of the previous step.
	* @return A boolean true if StepStatus comes after each of its prerequisites, false otherwise.
	* 
	*/
	public boolean checkFollowsPrereqs(SessionState state, String location, DissertationStep stepToMove)
	{	
		//if no prerequisites, there's no need to check
		if(!stepToMove.hasPrerequisites())
		{
			return true;
		}
		else
		{
			Hashtable orderedsteps = null;
			List prereqs = null;
			String lastkey = null;
			String lastref = null;
			String key = null;
			String ref = null;
			try
			{
				prereqs = stepToMove.getPrerequisiteStepReferences();
				orderedsteps = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE)).getOrderedSteps();
				lastkey = "" + orderedsteps.size();
				lastref = (String)orderedsteps.get(lastkey);
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".checkFollowsPrereqs Exception " + e);
			}
			if(location.equals("start"))
			{
				//first step cannot have prerequisites
				return false;
			}
			else if(location.equals(lastref))
			{
				//last step can have any step as a prerequisite
				return true;
			}
			else
			{
				int step = 0;
				int prereq = 0;
				for(int i = 1; i < (orderedsteps.size()+1); i++)
				{
					key = "" + i;
					ref = (String)orderedsteps.get(key);
					
					if(location.equals(ref))
					{
						step = i;
					}
					for(int j = 0; j < prereqs.size(); j++)
					{
						if(((String)prereqs.get(j)).equals(ref))
						{ 
							prereq = i;
						}
					}
				}
				
				if(step < prereq)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
		
	}//checkFollowsPrereqs
	
	/**
	* Check that StepStatus location is not before any of its prerequisites.
	* @param SessionState state The session state.
	* @param String location The stepReference string of the previous step.
	* @return A boolean true if StepStatus comes after each of its prerequisites, false otherwise.
	* 
	*/
	public boolean checkFollowsPrereqs(SessionState state, String location, StepStatus stepToMove)
	{	
		//if no prerequisites, there's no need to check
		List prereqs = null;
		prereqs = stepToMove.getPrereqs();
		if(prereqs == null || prereqs.size()==0)
		{
			return true;
		}
		else
		{
			Hashtable orderedsteps = null;
			String lastkey = null;
			String lastref = null;
			String key = null;
			String ref = null;
			try
			{
				orderedsteps = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE)).getOrderedStatus();
				lastkey = "" + orderedsteps.size();
				lastref = (String)orderedsteps.get(lastkey);
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".checkFollowsPrereqs Exception " + e);
			}
			if(location.equals("start"))
			{
				//first step cannot have prerequisites
				return false;
			}
			else if(location.equals(lastref))
			{
				//last step can have any step as a prerequisite
				return true;
			}
			else
			{
				int step = 0;
				int prereq = 0;
				for(int i = 1; i < (orderedsteps.size()+1); i++)
				{
					key = "" + i;
					ref = (String)orderedsteps.get(key);
					
					if(location.equals(ref))
					{
						step = i;
					}
					for(int j = 0; j < prereqs.size(); j++)
					{
						if(((String)prereqs.get(j)).equals(ref))
						{ 
							prereq = i;
						}
					}
				}
				
				if(step < prereq)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
		
	}//checkFollowsPrereqs
	
	/**
	* Check that new DissertationStep location is not before any of its prerequisites.
	* @param SessionState state The session state.
	* @param String location The stepReference string of the previous step.
	* @return A boolean true if StepStatus comes after each of its prerequisites, false otherwise.
	* 
	*/
	public boolean checkFollowsPrereqs(SessionState state, String location, String[] prereqs)
	{	
		//if no prerequisites, there's no need to check
		if(prereqs==null || prereqs.length==0)
		{
			return true;
		}
		else
		{
			Hashtable orderedsteps = null;
			String lastkey = null;
			String lastref = null;
			String key = null;
			String ref = null;
			try
			{
				orderedsteps = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE)).getOrderedSteps();
				lastkey = "" + orderedsteps.size();
				lastref = (String)orderedsteps.get(lastkey);
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".checkFollowsPrereqs Exception " + e);
			}
			if(location.equals("start"))
			{
				//first step cannot have prerequisites
				return false;
			}
			else if(location.equals(lastref))
			{
				//last step can have any step as a prerequisite
				return true;
			}
			else
			{
				int step = 0;
				int prereq = 0;
				for(int i = 1; i < (orderedsteps.size()+1); i++)
				{
					key = "" + i;
					ref = (String)orderedsteps.get(key);
					
					if(location.equals(ref))
					{
						step = i;
					}
					for(int j = 0; j < prereqs.length; j++)
					{
						if(((String)prereqs[j]).equals(ref))
						{ 
							prereq = i;
						}
					}
				}
				
				if(step < prereq)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
		
	}//checkFollowsPrereqs
	
	/**
	* Check that StepStatus location is not before any of its prerequisites.
	* @param SessionState state The session state.
	* @param String location The stepReference string of the previous step.
	* @return A boolean true if StepStatus comes after each of its prerequisites, false otherwise.
	* 
	*/
	public boolean checkFollowsPrereqs(SessionState state, String location, String[] prereqs, String anything)
	{
		//if no prerequisites, there's no need to check
		if(prereqs==null || prereqs.length==0)
		{
			return true;
		}
		else
		{
			Hashtable orderedsteps = null;
			String lastkey = null;
			String lastref = null;
			String key = null;
			String ref = null;
			try
			{
				orderedsteps = DissertationService.getCandidatePath((String)state.getAttribute(STATE_CURRENT_CANDIDATE_PATH_REFERENCE)).getOrderedStatus();
				lastkey = "" + orderedsteps.size();
				lastref = (String)orderedsteps.get(lastkey);
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".checkFollowsPrereqs Exception " + e);
			}
			if(location.equals("start"))
			{
				//first step cannot have prerequisites
				return false;
			}
			else if(location.equals(lastref))
			{
				//last step can have any step as a prerequisite
				return true;
			}
			else
			{
				int step = 0;
				int prereq = 0;
				for(int i = 1; i < (orderedsteps.size()+1); i++)
				{
					key = "" + i;
					ref = (String)orderedsteps.get(key);
					
					if(location.equals(ref))
					{
						step = i;
					}
					for(int j = 0; j < prereqs.length; j++)
					{
						if(((String)prereqs[j]).equals(ref))
						{ 
							prereq = i;
						}
					}
				}
				
				if(step < prereq)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
		
	}//checkFollowsPrereqs
	
	/**
	* Get validity of inserting step with section header at the selected location.
	* @param SessionState state The session state.
	* @param String location The stepReference string of the previous step.
	* @param String section The section of the step to be added.
	* @return A boolean true if location is consistent with section, false otherwise.
	* 
	*/
	public boolean checkSection(SessionState state, String location, String section)
	{
		String previouskey = null, nextkey = null, lastkey = null;
		String previousref = null, nextref = null, lastref = null;
		boolean comparePreceding = false, compareFollowing = false;
		boolean notdone = true;
		boolean retVal = true;
		int id=0, precedingid=0, followingid=0;
		Hashtable orderedsteps = null;
		try
		{
			//get the sections of the steps surrounding the step to be inserted
			orderedsteps = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE)).getOrderedSteps();
			lastkey = "" + orderedsteps.size();
			lastref = (String)orderedsteps.get(lastkey);
			if(location.equals("start"))
			{
				//just need to check following step
				nextref = (String)orderedsteps.get("1");
				compareFollowing = true;
			}
			else if(location.equals(lastref))
			{
				//just need to check preceding step
				comparePreceding = true;
			}
			else
			{
				for(int i = 1; i < (orderedsteps.size()+1); i++)
				{
					if(notdone)
					{
						previouskey = "" + i;
						previousref = (String)orderedsteps.get(previouskey);
						if(previousref != null)
						{
							if(previousref.equals(location))
								notdone = false;
						}
					}
				}
				int key = Integer.parseInt(previouskey);
				key = key + 1;
				nextkey = Integer.toString(key);
				nextref = (String)orderedsteps.get(nextkey);
			}
			try
			{
				//get integers for comparison
				id = ((Vector)getSectionHeads()).indexOf(section);
				if(nextref!=null)
				{
					followingid = Integer.parseInt((String)((DissertationStep)DissertationService.getDissertationStep(nextref)).getSection());
				}
				if(previousref!=null)
				{
					precedingid = Integer.parseInt((String)((DissertationStep)DissertationService.getDissertationStep(previousref)).getSection());
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".checkSection() section head id(s): " + e);
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".checkSection() orderedsteps key/ref(s): " + e);
		}
		//check that section of step to be inserted is consistent with surrounding step sections
		if(comparePreceding)
		{
			if(id < precedingid)
			{
				retVal = false;
			}
		}
		else if(compareFollowing)
		{
			//if this is the first step, there is nothing to compare it to, so return true for any section assignment
			if(orderedsteps.size() == 0)
			{
				return true;
			}
			if(id > followingid)
			{
				retVal = false;
			}
		}
		else
		{
			if(id < precedingid || id > followingid)
			{
				retVal = false;
			}
		}
		return retVal;
		
	}//checkSection
	
	/**
	* Get validity of step with this section header at this selected location.
	* @param SessionState state The session state.
	* @param String location The stepReference string of the step being edited.
	* @param String section The section of the step being edited.
	* @return A boolean true if location is consistent with section, false otherwise.
	* 
	*/
	public boolean checkSectionEdit(SessionState state, String location, String section)
	{
		String thiskey = null;
		String previouskey = null;
		String nextkey = null;
		String thisref = null;
		String previousref = null;
		String nextref = null;
		String lastkey = null;
		String lastref = null;
		boolean notdone = true;
		boolean comparePreceding = false;
		boolean compareFollowing = false;
		boolean retVal = true;
		int id=0, precedingid=0, followingid=0;
		try
		{
			//get the sections of the steps surrounding the step to be inserted
			Hashtable orderedsteps = DissertationService.getDissertation((String)state.getAttribute(STATE_CURRENT_DISSERTATION_REFERENCE)).getOrderedSteps();
			lastkey = "" + orderedsteps.size();
			lastref = (String)orderedsteps.get(lastkey);
			if(location.equals("start"))
			{
				//just need to check following step
				nextref = (String)orderedsteps.get("1");
				compareFollowing = true;
			}
			else if(location.equals(lastref))
			{
				//just need to check preceding step
				comparePreceding = true;
			}
			else
			{
				for(int i = 1; i < (orderedsteps.size()+1); i++)
				{
					if(notdone)
					{
						thiskey = "" + i;
						thisref = (String)orderedsteps.get(thiskey);
						if(thisref != null)
						{
							if(thisref.equals(location))
								notdone = false;
						}
					}
				}
				int key = Integer.parseInt(thiskey);
				key = key + 1;
				if(key <= orderedsteps.size())
				{
					nextkey = Integer.toString(key);
					nextref = (String)orderedsteps.get(nextkey);
				}
				key = key - 2;
				if(key >= 1)
				{
					previouskey = Integer.toString(key);
					previousref = (String)orderedsteps.get(previouskey);
				}
			}
			try
			{
				//get integers for comparison
				id = ((Vector)getSectionHeads()).indexOf(section);
				if(nextref!=null)
				{
					followingid = Integer.parseInt((String)((DissertationStep)DissertationService.getDissertationStep(nextref)).getSection());
				}
				if(previousref!=null)
				{
					precedingid = Integer.parseInt((String)((DissertationStep)DissertationService.getDissertationStep(previousref)).getSection());
				}
			}
			catch(Exception e)
			{
				Log.warn("chef", this + ".checkSectionEdit() section head id(s): " + e);
			}
		}
		catch(Exception e)
		{
			Log.warn("chef", this + ".checkSectionEdit() orderedsteps key/ref(s): " + e);
		}
		//check that section of step to be inserted is consistent with surrounding step sections
		if(comparePreceding)
		{
			if(id < precedingid)
			{
				retVal = false;
			}
		}
		else if(compareFollowing)
		{
			if(id > followingid)
			{
				retVal = false;
			}
		}
		else
		{
			if(id < precedingid || id > followingid)
			{
				retVal = false;
			}
		}
		return retVal;
		
	}//checkSectionEdit
	
	
	/**
	* Get the collection of validation type strings in a hashtable.
	* @return A hashtable containing the validation type strings appropriate to a Rackham adminstrator,
	* keyed by validation type number as a string.
	*/
	public Hashtable validationTypeTable()
	{
		Hashtable retVal = new Hashtable();
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT, DissertationStep.STUDENT_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_CHAIR, DissertationStep.CHAIR_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE, DissertationStep.COMMITTEE_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT, DissertationStep.DEPARTMENT_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_SCHOOL, DissertationStep.SCHOOL_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR, DissertationStep.STUDENT_CHAIR_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE, DissertationStep.STUDENT_COMMITTEE_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT, DissertationStep.STUDENT_DEPARTMENT_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_SCHOOL, DissertationStep.STUDENT_SCHOOL_VALIDATION_STRING);
		return retVal;
	}

	/**
	* Get the collection of validation types as a string array.
	* @return A hashtable containing the validation type strings appropriate to a Rackham adminstrator.
	*/
	public String[] validationTypes()
	{
		String[] retVal = new String[9];
		retVal[0] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT;
		retVal[1] = DissertationStep.STEP_VALIDATION_TYPE_CHAIR;
		retVal[2] = DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE;
		retVal[3] = DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT;
		retVal[4] = DissertationStep.STEP_VALIDATION_TYPE_SCHOOL;
		retVal[5] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR;
		retVal[6] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE;
		retVal[7] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT;
		retVal[8] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_SCHOOL;
		return retVal;
	}

	/**
	* Get the collection of validation type strings in a hashtable.
	* @return A hashtable containing the validation type strings appropriate to a department adminstrator,
	* keyed by validation type number as a string.
	*/
	public Hashtable deptValidationTypeTable()
	{
		Hashtable retVal = new Hashtable();
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT, DissertationStep.STUDENT_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_CHAIR, DissertationStep.CHAIR_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE, DissertationStep.COMMITTEE_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT, DissertationStep.DEPARTMENT_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR, DissertationStep.STUDENT_CHAIR_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE, DissertationStep.STUDENT_COMMITTEE_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT, DissertationStep.STUDENT_DEPARTMENT_VALIDATION_STRING);
		return retVal;
	}

	/**
	* Get the collection of validation types as a string array.
	* @return A hashtable containing the validation type strings appropriate to a department adminstrator.
	*/
	public String[] deptValidationTypes()
	{
		String[] retVal = new String[7];
		retVal[0] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT;
		retVal[1] = DissertationStep.STEP_VALIDATION_TYPE_CHAIR;
		retVal[2] = DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE;
		retVal[3] = DissertationStep.STEP_VALIDATION_TYPE_DEPARTMENT;
		retVal[4] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR;
		retVal[5] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE;
		retVal[6] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_DEPARTMENT;
		return retVal;
	}

	/**
	* Get the collection of validation type strings in a hashtable.
	* @return A hashtable containing the validation type strings appropriate to a committee member,
	* keyed by validation type number as a string.
	*/
	public Hashtable commValidationTypeTable()
	{
		Hashtable retVal = new Hashtable();
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT, DissertationStep.STUDENT_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_CHAIR, DissertationStep.CHAIR_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE, DissertationStep.COMMITTEE_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR, DissertationStep.STUDENT_CHAIR_VALIDATION_STRING);
		retVal.put(DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE, DissertationStep.STUDENT_COMMITTEE_VALIDATION_STRING);
		return retVal;
	}

	/**
	* Get the collection of validation types as a string array.
	* @return A hashtable containing the validation type strings appropriate to a committee member.
	*/	
	public String[] commValidationTypes()
	{
		String[] retVal = new String[5];
		retVal[0] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT;
		retVal[1] = DissertationStep.STEP_VALIDATION_TYPE_CHAIR;
		retVal[2] = DissertationStep.STEP_VALIDATION_TYPE_COMMITTEE;
		retVal[3] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_CHAIR;
		retVal[4] = DissertationStep.STEP_VALIDATION_TYPE_STUDENT_COMMITTEE;
		return retVal;
	}

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
	}

	/** Utility class used to determine if a letter in the alphabetical candidate chooser is a link. */
	public class LetterCarrier
	{
		private String letter;
		private boolean hasmembers;
		
		private LetterCarrier(){}
		
		public LetterCarrier(String l)
		{
			hasmembers = true;
			if(l != null)
				letter = l;
			else
				letter = "";
		}

		public String getLetter()
		{
			return letter;
		}
		
		public boolean hasMembers()
		{
			return hasmembers;
		}
		
		public void setHasMembers(boolean members)
		{
			hasmembers = members;
		}
	}
	
	/**
	* Get the collection of Checklist Section Headings for display.
	* @return Vector of ordered String objects, one for each section head.
	*/
	private Vector getSectionHeads()
	{
		Vector retVal = new Vector();
		retVal.add("None");
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING1);
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING2);
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING3);
		retVal.add(DissertationService.CHECKLIST_SECTION_HEADING4);
		return retVal;
	}//getSectionHeadings
	
	/**
	* Get the section head identifier for the checklist section heading.
	* @param String head The text of the section heading.
	* @return String containing the section identifier.
	*/
	private String getSectionId(String head)
	{
		String retVal = "";
		if(head!=null && !head.equals(""))
		{
			if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING1))
			{
				retVal = "1";
			}
			else if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING2))
			{
				retVal = "2";
			}
			else if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING3))
			{
				retVal = "3";
			}
			else if(head.equals(DissertationService.CHECKLIST_SECTION_HEADING4))
			{
				retVal = "4";
			}
		}
		return retVal;
	}//getSectionId
	
	/**
	* Access the alphabetical candidate chooser letter for this student. 
	* @param chefid – The user's id.
	* @return The alphabetical candidate chooser letter, A-Z, or "".
	*/
	public String getSortLetter(String chefId)
	{
		String retVal = "";
		if(chefId != null)
		{
			try
			{
				String sortName = UserDirectoryService.getUser(chefId).getSortName();
				if(sortName != null)
				{
					retVal = sortName.substring(0,1).toUpperCase();
				}
			}
			catch(IdUnusedException e) 
			{
				Log.warn("chef", this + ".getSortLetter(String " + chefId + ")");
				
			}
		}
		return retVal;
		
	}//getSortLetter
	
	/**
	* Get the collection of LetterCarrier objects for a site.
	* @param site The site in question.
	* @return Vector of LetterCarrier objects, one for each letter of the alphabet.
	*/
	private Vector getLetters(String schoolSite, String site, String stepsType)
	{
		Vector retVal = new Vector();
		//User[] lettersUsers = null;
		boolean hasMembers = false;
		
		//User[] allusers = DissertationService.getAllUsersForSite(site, stepsType);
		LetterCarrier aCarrier = null;
		
		aCarrier = new LetterCarrier("A");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"A");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"A");
		aCarrier.setHasMembers(hasMembers);
		
		/*
		lettersUsers = getUsersByLetter(allusers, "A");
		if(lettersUsers.length == 0)
			aCarrier.setHasMembers(false);
		*/
		
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("B");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"B");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"B");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("C");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"C");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"C");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("D");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"D");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"D");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("E");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"E");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"E");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("F");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"F");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"F");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("G");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"G");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"G");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("H");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"H");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"H");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("I");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"I");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"I");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		aCarrier = new LetterCarrier("J");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"J");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"J");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("K");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"K");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"K");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("L");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"L");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"L");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("M");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"M");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"M");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("N");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"N");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"N");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("O");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"O");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"O");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("P");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"P");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"P");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("Q");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"Q");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"Q");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("R");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"R");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"R");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("S");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"S");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"S");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("T");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"T");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"T");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("U");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"U");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"U");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("V");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"V");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"V");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("W");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"W");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"W");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("X");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"X");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"X");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("Y");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"Y");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"Y");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);
		
		aCarrier = new LetterCarrier("Z");
		if(site.equals(schoolSite))
			hasMembers = DissertationService.isUserOfTypeForLetter(stepsType,"Z");
		else
			hasMembers = DissertationService.isUserOfParentForLetter(site,"Z");
		aCarrier.setHasMembers(hasMembers);
		retVal.add(aCarrier);

		return retVal;
	}
	
	/**
	* Get all the users whose name starts with a given letter.
	* @param allUsers The collection of user objects for all CandidatePaths.
	* @param letter The starting letter.
	* @return The array of user objects who's display name begins the the specified letter.
	*/
	private User[] getUsersByLetter(User[] allUsers, String letter)
	{
		User[] retVal = null;
		User aUser = null;
		String sortName = null;
		Vector usersByLetter = new Vector();

		for(int x = 0; x < allUsers.length; x++)
		{
			if(allUsers[x]!=null)
			{
				sortName = allUsers[x].getSortName();
				try
				{
					if(sortName.startsWith(letter))
					{
						usersByLetter.add(allUsers[x]);
					}
					else
					{
						//sort name does not begin with letter - will not add
					}
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".getUsersByLetter Exception " + e);
				}
			}
		}
		
		retVal = new User[usersByLetter.size()];
		for(int x = 0; x < usersByLetter.size(); x++)
		{
			retVal[x] = (User)usersByLetter.get(x);
		}
		return retVal;

	}//getUsersByLetter
	
	/**
	* Replace CandidatePath site id set during data load with current site id
	* @return The updated path.
	*/
	public CandidatePath updateCandidatePathSiteId(SessionState state, CandidatePath path)
	{
		//%%% move to service
		CandidatePath updatedPath = null;
		
		//Unless current site id is same as user's uniqname, set site id to current site id
		if(!(((String)state.getAttribute(STATE_CURRENT_SITE)).equals((String)state.getAttribute(STATE_USER_ID))))
		{
			if(DissertationService.allowUpdateCandidatePath(path.getReference()))
			{
				String keyString = null;
				String statusRef = null;
				StepStatusEdit statusEdit = null;
				boolean hasPrerequisites = false;
							
				//This changes the references, so make new references to avoid "edit not in locks"
				String oldPathRef = (String)path.getReference();
				Dissertation parentDissertation = null;
				CandidatePathEdit pathEdit = null;
				try
				{
					//String parentSite = DissertationService.getParentSiteForUser(((User)state.getAttribute(STATE_USER)).getId());
					String parentSite = path.getParentSite();
					try
					{
						//Get a new CandidatePath
						parentDissertation = DissertationService.getDissertationForSite(parentSite);
						if(parentDissertation == null)
						{
							//current user
							User currentUser = (User)state.getAttribute(STATE_USER);
							
							//Rackham site
							String schoolSite = (String)state.getAttribute(STATE_SCHOOL_SITE);
							
							//no dept dissertation, so try to use a Rackham dissertation
							try
							{
								if(DissertationService.isMusicPerformanceCandidate(currentUser.getId()))
								{
									//if Candidate is in Music Performance, use Music Performance type dissertation
									parentDissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_MUSIC_PERFORMANCE);
								}
								else
								{
									//if Candidate is in Music Performance, use Music Performance type dissertation
									parentDissertation = DissertationService.getDissertationForSite(schoolSite, DissertationService.DISSERTATION_TYPE_DISSERTATION_STEPS);
								}
							
							}
							catch(Exception e)
							{
								Log.warn("chef", this + ".initState() DissertationService.getDissertationForSite(" + schoolSite + ") " + e);
							}
							if(parentDissertation == null)
							{
								//no dissertation on which to base path, so bail out
								return null;
							}
							
						}
						pathEdit = DissertationService.addCandidatePath(parentDissertation, (String)state.getAttribute(STATE_CURRENT_SITE));
									
						//setSite
						pathEdit.setSite((String)state.getAttribute(STATE_CURRENT_SITE));
						
						//set alphabetical candidate chooser letter
						pathEdit.setSortLetter(getSortLetter(path.getCandidate()));
						
						//set the parent department site id
						//User currentUser = (User)state.getAttribute(STATE_USER);
						//pathEdit.setParentSite(DissertationService.getCandidateInfo(currentUser.getId()).getParentSiteId());
						pathEdit.setParentSite(path.getParentSite());
						
						//setType
						pathEdit.setType(path.getType());
										
						//setAdvisor
						pathEdit.setAdvisor(path.getAdvisor());
										
						//setCandidate
						pathEdit.setCandidate(path.getCandidate());
										
						//setSchoolPrereqs
						pathEdit.setSchoolPrereqs(path.getSchoolPrereqs());
										
						//Remove new CandidatePath's StepStatus's
						Hashtable removeOrderedStatus = pathEdit.getOrderedStatus();
						statusEdit = null;
						for (int i = 1; i < removeOrderedStatus.size()+1; i++)
						{
							keyString = "" + i;
							statusRef = (String)removeOrderedStatus.get(keyString);
							try
							{
								statusEdit = DissertationService.editStepStatus(statusRef);
								DissertationService.removeStepStatus(statusEdit);
							}
							catch(PermissionException p)
							{
								Log.warn("chef", this + ".initState() DissertationService.removeStepStatus(" + statusEdit.getReference() + ") " + p);
							}
						}
										
						Hashtable orderedStatus = path.getOrderedStatus();
						Hashtable newOrderedStatus = new Hashtable();
						Hashtable prereqOrders = new Hashtable();
						Hashtable newPrereqs = new Hashtable();
										
						String prereqOrder = null;
						String newStatusRef = null;
										
						List statusPrereqs = null;
										
						Vector prereqs = new Vector();
										
						//Add new step status's to new path
						for (int i = 1; i < orderedStatus.size()+1; i++)
						{
							keyString = "" + i;
							statusRef = (String)orderedStatus.get(keyString);
											
							//Get old status
							StepStatus status = DissertationService.getStepStatus(statusRef);
											
							//See if old status had prerequisites
							hasPrerequisites = false;
							prereqs.clear();
							if(((List)status.getPrereqs()).size() != 0)
							{
								//This old status has prerequisites
								hasPrerequisites = true;
												
								//Get those prerequisite references
								statusPrereqs = status.getPrereqs();
												
								//Match preceding step(s) with prerequisite reference(s)
								for (int j = i-1; j > 0; j--)
								{
									prereqOrder = "" + j;
									statusRef = (String)orderedStatus.get(prereqOrder);
									for (int k = 0; k < statusPrereqs.size(); k++)
									{
										if(((String)statusPrereqs.get(k)).equals(statusRef))
										{
											//Store the relative position in the checklist of the prereq for this step
											prereqs.add(prereqOrder);
										}
									}
								}
							}
											
							//Get a new StepStatus
							statusEdit = DissertationService.addStepStatus((String)state.getAttribute(STATE_CURRENT_SITE));
											
							//Set site, parent step, autovalidation status
							DissertationStep step = DissertationService.getDissertationStep(status.getParentStepReference());
							statusEdit.initialize((String)state.getAttribute(STATE_CURRENT_SITE), step, status.getOardValidated());
											
							//Add prereqs to the new step if the old steps had prereqs
							if(hasPrerequisites)
							{
								//New status prerequisite references
								List newStatusPrereqs = new Vector(prereqs.size(), 1);
												
								//For each of the prereqs for the step
								for (int j = 0; j < prereqs.size(); j++)
								{
									//Get the reference of the new step at this position
									try
									{
										newStatusRef = ((String)newOrderedStatus.get((String)prereqs.get(j)));
									}
									catch(NumberFormatException e) 
									{
										Log.warn("chef", this + ".initState() newOrderedStatus.get(Integer.parseInt(prereqs.get(" + j + ") " + e);
									}
													
									//And add it to the List of prereqs for the new step
									newStatusPrereqs.add(newStatusRef);
								}
												
								//Add the prereqs
								statusEdit.setPrereqs(newStatusPrereqs);
							}
											
							//setSite
							statusEdit.setSite((String)state.getAttribute(STATE_CURRENT_SITE));
										
							//setAutoValidationId
							statusEdit.setAutoValidationId(status.getAutoValidationId());
										
							//setCompleted
							statusEdit.setCompleted(status.getCompleted());
										
							//setHardDeadline
							statusEdit.setHardDeadline(status.getHardDeadline());
										
							//setInstructions
							statusEdit.setInstructionsHtml(status.getInstructions());
										
							//setRecommendedDeadline
							statusEdit.setRecommendedDeadline(status.getRecommendedDeadline());
										
							//setTimeCompleted
							statusEdit.setTimeCompleted(status.getTimeCompleted());
							
							//setTimeCompletedText
							statusEdit.setTimeCompletedText(status.getTimeCompletedText());
							
							//setAuxiliaryText
							statusEdit.setAuxiliaryText(status.getAuxiliaryText());
										
							//setValidationType
							statusEdit.setValidationType(status.getValidationType());
										
							//create orderedStatus entry
							newOrderedStatus.put(keyString, statusEdit.getReference());
											
							//commit StepStatus edit
							DissertationService.commitEdit(statusEdit);
						}
										
						//set OrderedStatus and commit CandidatePath edit
						pathEdit.setOrderedStatus(newOrderedStatus);
										
						//commit new path
						DissertationService.commitEdit(pathEdit);
										
						//Finally, remove old path
						CandidatePathEdit oldPathEdit = null;
						try
						{
							oldPathEdit = DissertationService.editCandidatePath(oldPathRef);
							for (int i = 1; i < orderedStatus.size()+1; i++)
							{
								keyString = i + "";
								statusRef = (String)orderedStatus.get(keyString);
								StepStatusEdit oldStatusEdit = DissertationService.editStepStatus(statusRef);
								DissertationService.removeStepStatus(oldStatusEdit);
							}
							DissertationService.removeCandidatePath(oldPathEdit);
						}
						catch(Exception e)
						{
							Log.warn("chef", this + ".initState DissertationService.edit/removeCandidatePath(" + oldPathRef + ") " + e);
						}
					}
					catch (Exception e)
					{
						Log.warn("chef", this + ".initState() DissertationService.getDissertationForSite(" + parentSite + ") " + e);
					}
				}
				catch(Exception e)
				{
					Log.warn("chef", this + ".initState DissertationService.getParentSiteForUser(" + ((User)state.getAttribute(STATE_USER)).getId() + ") " + e);
				}
			}
			else
			{
				//No permission to update CandidatePath site id
				addAlert(state, "You do not have permission to set the site id of the candidate path.");
				state.setAttribute(STATE_MODE, MODE_NO_CANDIDATE_PATH);
				return path;
			}
			
			try
			{
				updatedPath = (CandidatePath)DissertationService.getCandidatePathForCandidate(((User)state.getAttribute(STATE_USER)).getId());
			}
			catch (Exception e)
			{
				Log.warn("chef", this + ".initState() DissertationService.getCandidatePathForCandidate(" + ((User)state.getAttribute(STATE_USER)).getId() + ") " + e);
			}
			

		}
		return updatedPath;
		
	}//updateCandidatePathSiteId


	/**
	* Comparator for alphabetizing User's Display Name
	*/
	private class UserComparator
		implements Comparator
	{
		/**
		* Compare the two objects
		* @return int 
		*/
		public int compare(Object o1, Object o2)
		{
			int result = 0;
			try
			{
				User user1 = (User)o1;
				User user2 = (User)o2;
				String name1 = user1.getSortName();
				String name2 = user2.getSortName();
				result = name1.compareTo(name2);
			}
			catch(Exception e)
			{
				Log.info("chef", "DISSERTATION : ACTION : USER COMPARATOR : COMPARE : EXCEPTION : " + e);
			}
			return result;
		}

		public boolean equals(Object o1, Object o2)
		{
			boolean retVal = false;
			try
			{
				User user1 = (User)o1;
				User user2 = (User)o2;
				String name1 = user1.getSortName();
				String name2 = user2.getSortName();
				if(name1.compareTo(name2) == 0)
					retVal = true;
			}
			catch(Exception e)
			{
				Log.info("chef", "DISSERTATION : ACTION : USER COMPARATOR : EQUALS : EXCEPTION : " + e);
			}
			return retVal;
		}
	}

}//DissertationAction

/**********************************************************************************
*
* $Header: /cvs/ctools/gradtools/tool/src/java/org/sakaiproject/tool/dissertation/DissertationAction.java,v 1.2 2005/05/28 03:05:07 ggolden.umich.edu Exp $
*
**********************************************************************************/
