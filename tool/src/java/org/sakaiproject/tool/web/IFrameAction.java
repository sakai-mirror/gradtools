/**********************************************************************************
*
* $Header: /cvs/sakai2/legacy/tools/src/java/org/sakaiproject/tool/web/IFrameAction.java,v 1.9 2005/06/06 20:29:08 ggolden.umich.edu Exp $
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
package org.sakaiproject.tool.web;

// imports
import java.util.ResourceBundle;

import org.sakaiproject.api.kernel.tool.Placement;
import org.sakaiproject.api.kernel.tool.cover.ToolManager;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.PortletConfig;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
import org.sakaiproject.cheftool.menu.Menu;
import org.sakaiproject.service.framework.config.cover.ServerConfigurationService;
import org.sakaiproject.service.framework.portal.cover.PortalService;
import org.sakaiproject.service.framework.session.SessionState;
import org.sakaiproject.service.legacy.resource.Reference;
import org.sakaiproject.service.legacy.site.Site;
import org.sakaiproject.service.legacy.site.cover.SiteService;
import org.sakaiproject.util.java.StringUtil;


/**
* <p>IFrameAction is the CHEF tool to place any web content in an IFrame on the page.</p>
* 
* @author University of Michigan, CHEF Software Development Team
* @version $Revision$
*/
public class IFrameAction
	extends VelocityPortletPaneledAction
{
	/** Resource bundle using current language locale */
    protected static ResourceBundle rb = ResourceBundle.getBundle("iframe");
	
	/** The source URL, in state, config and context. */
	protected final static String SOURCE = "source";

	/** The height, in state, config and context. */
	protected final static String HEIGHT = "height";

	/** The special attribute, in state, config and context. */
	protected final static String SPECIAL = "special";

	/** The title, in state and context. */
	protected final static String TITLE = "title";

	/** The flag name and value in state to indicate an update to the portlet is needed. */
	private final static String UPDATE = "update";
	
	/** The type of source: URL, Worksite Info, Workspace Info or etc. **/
	private final static String SOURCE_TYPE = "source_type";
	
	/** Whether to pass through the PID to the URL displayed in the IFRAME.  This
	 * enables integration in that the application in the IFRAME will know what
	 * site and tool it is part of.
	 */
	private final static String PASS_PID = "passthroughPID";

	/**
	* Populate the state object, if needed - override to do something!
	*/
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData rundata)
	{
		super.initState(state, portlet, rundata);

		PortletConfig config = portlet.getPortletConfig();
		
		state.setAttribute(PASS_PID, config.getInitParameter(PASS_PID));

		// parameter "special" - won't change 
		String special = StringUtil.trimToNull(config.getInitParameter(SPECIAL));

		if (special == null)
		{
			// this was how the ChefWebPagePortlet used to work.  We handle those now.
			if ("true".equals(config.getInitParameter("site")))
			{
				special = "site";
			}
			else if ("true".equals(config.getInitParameter("workspace")))
			{
				special = "workspace";
			}
			else if ("true".equals(config.getInitParameter("worksite")))
			{
				special = "worksite";
			}
		}

		if (special == null) special = "";

		state.setAttribute(SPECIAL, special);

		String url = StringUtil.trimToNull(config.getInitParameter(SOURCE));
		if (url == null)
		{
			// this was how the ChefWebPagePortlet used to work.  We handle those now.
			url = StringUtil.trimToNull(config.getInitParameter("url"));
		}

		// store the raw as-configured source
		state.setAttribute("source-config", url);

		// compute working source
		String source_type = (String)state.getAttribute(SOURCE_TYPE);
		if (source_type != null)
		{
			url = sourceUrl(source_type, url, state, rundata);
		}
		else
		{
			url = sourceUrl(special, url, state, rundata);
		}
		state.setAttribute(SOURCE, url);

		if (state.getAttribute(HEIGHT) == null)
		{
			state.setAttribute(HEIGHT, config.getInitParameter(HEIGHT));
		}

		if (state.getAttribute(TITLE) == null)
		{
			state.setAttribute(TITLE, portlet.getPortletConfig().getTitle());
		}

	}   // initState

	/** 
	* build the context
	*/
	public String buildMainPanelContext(VelocityPortlet portlet, 
										Context context,
										RunData rundata,
										SessionState state)
	{
		if (MODE_OPTIONS.equals(state.getAttribute(STATE_MODE)))
		{
			return buildOptionsPanelContext(portlet, context, rundata, state);
		}

		// if we have a source url, use it
		String sourceUrl = StringUtil.trimToNull((String) state.getAttribute(SOURCE));
		if (sourceUrl != null)
		{
			context.put(SOURCE,(String) state.getAttribute(SOURCE));
			context.put(HEIGHT, state.getAttribute(HEIGHT));
			context.put("tlang",rb);
		}

		// otherwise provide the site's description text
		else
		{		
			//set the url to the site of this request's config'ed url
			String siteId = PortalService.getCurrentSiteId();
			try
			{
				Site s = SiteService.getSite(siteId);
			
				String description = s.getDescription();
				if (description != null)
				{
					context.put("text", description);
					context.put("tlang",rb);
				}
			}
			catch (Exception any) {}
		}

		// put $action into context for menus, forms and links
		context.put(Menu.CONTEXT_ACTION, state.getAttribute(STATE_ACTION));
		context.put("tlang",rb);
		// add options if allowed
		Menu bar = new Menu(portlet, rundata, "IFrameAction");
		addOptionsMenu(bar, (JetspeedRunData) rundata);
		// if we there's anything in the menu, use it
		if (!bar.getItems().isEmpty())
		{
			context.put(Menu.CONTEXT_MENU, bar);
			context.put("tlang",rb);
		}
		
		context.put(SPECIAL, state.getAttribute(SPECIAL));
		
		String source_type = (String)state.getAttribute(SOURCE_TYPE);
		if (source_type == null)
		{
			source_type = (String)state.getAttribute(SPECIAL);
			state.setAttribute(SOURCE_TYPE, source_type);
		}
		context.put(SOURCE_TYPE, state.getAttribute(SOURCE_TYPE));
		
		String title = portlet.getPortletConfig().getTitle() + " " + rb.getString("gen.options");
		context.put("title", title);

		return (String)getContext(rundata).get("template");

	}	// buildMainPanelContext

	/**
	*  Setup for options.
	**/
	public String buildOptionsPanelContext( VelocityPortlet portlet,
											Context context,
											RunData data,
											SessionState state) 
	{
		context.put(SOURCE, state.getAttribute("source-config"));
		context.put(HEIGHT, state.getAttribute(HEIGHT));
		context.put(TITLE, state.getAttribute(TITLE));
		context.put(SPECIAL, state.getAttribute(SPECIAL));
		context.put(SOURCE_TYPE, state.getAttribute(SOURCE_TYPE));
		context.put("tlang",rb);
		context.put(Menu.CONTEXT_ACTION, "IFrameAction");

		context.put("doUpdate", BUTTON + "doConfigure_update");
		context.put("doCancel", BUTTON + "doCancel");

		// pick the "-customize" template based on the standard template name
		String template = (String)getContext(data).get("template");
		return template + "-customize";

	}	// buildOptionsPanelContext

	/**
	* Handle the configure context's update button
	*/
	public void doConfigure_update(RunData data, Context context)
	{
		// access the portlet element id to find our state
		String peid = ((JetspeedRunData)data).getJs_peid();
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(peid);

		String source_type = data.getParameters().getString(SOURCE_TYPE);
		if (!source_type.equals(state.getAttribute(SOURCE_TYPE)))
		{
			// update the source in state
			state.setAttribute(SOURCE_TYPE, source_type);
		}

		// mark for update w/o a test against state's current value - state may be showing
		// a special source value (worksite, site, workspace).
		String source = data.getParameters().getString(SOURCE);

		// if it's missing the transport, add http://
		source = source.trim();
		if ((source.length() > 0) && (!source.startsWith("/")) && (source.indexOf("://") == -1))
		{
			source = "http://" + source;
		}

		// update configed source
		state.setAttribute("source-config", source);

		// update the working source
		source = sourceUrl(source_type, source, state, (JetspeedRunData) data);
		state.setAttribute(SOURCE, source);

		// height
		String height = data.getParameters().getString(HEIGHT);
		if (!height.equals(state.getAttribute(HEIGHT)))
		{
			// update the source in state
			state.setAttribute(HEIGHT, height);
		}

		// title
		String title = data.getParameters().getString(TITLE);
		if (!title.equals(state.getAttribute(TITLE)))
		{
			// update the source in state
			state.setAttribute(TITLE, title);
		}

		// update the tool placement config
		Placement placement = ToolManager.getCurrentPlacement();
		placement.getPlacementConfig().setProperty(HEIGHT, (String)state.getAttribute(HEIGHT));
		placement.getPlacementConfig().setProperty(SOURCE, (String)state.getAttribute("source-config"));
		placement.setTitle((String)state.getAttribute(TITLE));

		// we are done with customization... back to the main mode
		state.removeAttribute(STATE_MODE);

		// deliver an update to the title panel (to show the new channel name)
		String titleId = titlePanelUpdateId(peid);
		schedulePeerFrameRefresh(titleId);

		// commit the change
		saveOptions();

	}   // doConfigure_update

	/**
	* doCancel called for form input tags type="submit" named="eventSubmit_doCancel"
	* cancel the options process
	*/
	public void doCancel(RunData data, Context context)
	{
		// access the portlet element id to find our state
		String peid = ((JetspeedRunData)data).getJs_peid();
		SessionState state = ((JetspeedRunData)data).getPortletSessionState(peid);

		// cancel the options
		cancelOptions();

		// we are done with customization... back to the main mode
		state.removeAttribute(STATE_MODE);

	}   // doCancel

	/**
	* Return the url unchanged, unless it's a reference, then return the reference url
	*/
	private String convertReferenceUrl(String url)
	{
		// make a reference
		Reference ref = new Reference(url);

		// if it didn't recognize this, return it unchanged
		if (ref.getType() == null) return url;

		// return the reference's url
		return ref.getUrl();

	}	// convertReferenceUrl

	/**
	* Make a source URL based on the special setting, the url already set to "url" or "source", ...
	*/
	protected String sourceUrl(String source_type, String url, SessionState state, JetspeedRunData rundata)
	{	
		// if marked for "site", use the site intro from the properties
		if ("site".equals(source_type))
		{
			// set the url to the site config'ed url
			url = ServerConfigurationService.getString("server.info.url","");
		}
			
		// if marked for "workspace", use the "user" site intro from the properties
		else if ("workspace".equals(source_type))
		{
			// set the url to the site config'ed url
			url = ServerConfigurationService.getString("myworkspace.info.url","");
		}
	
		// if marked for "worksite", use the setting from the site's definition
		else if ("worksite".equals(source_type))
		{
			// set the url to the site of this request's config'ed url
			String siteId = PortalService.getCurrentSiteId();
			try
			{
				// use the site's info url, if any, else the site description will be used
				Site s = SiteService.getSite(siteId);
				url = StringUtil.trimToZero(s.getInfoUrlFull());
			}
			catch (Exception any) {}
		}
			
		// otherwise, if it's not set, set it to a sample page in public
		else if ((url == null) || (url.trim().length() == 0))
		{
			url = ServerConfigurationService.getString("webcontent.instructions.url", null);
		}

		// accept a partial reference url (i.e. "/content/group/sakai/test.gif"), convert to full url
		if (url != null)
		{
			url = url.trim();
			url = convertReferenceUrl(url);
		}
		
		// pass the PID through on the URL, IF configured to do so
		if ("true".equals(state.getAttribute(PASS_PID)))
		{
		    if (url.indexOf("?") < 0)
		    {
		        url = url + "?";
		    }
		    else
		    {
		        url = url + "&";
		    }
		    
		    url = url + "pid=" + PortalService.getCurrentToolId();  
		}

		return url;

	}	// sourceUrl

	/**
	* Say if the tool will require the "reset tool" button on the title bar.
	* @return true to have the "reset tool" button, false if not.
	* The default is to have it.
	*/
	protected boolean requireResetToolLink()
	{
		return false;

	}	// requireResetToolLink

}	// IFrameAction

/**********************************************************************************
*
* $Header: /cvs/sakai2/legacy/tools/src/java/org/sakaiproject/tool/web/IFrameAction.java,v 1.9 2005/06/06 20:29:08 ggolden.umich.edu Exp $
*
**********************************************************************************/
