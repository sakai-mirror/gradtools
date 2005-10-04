/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005 The Regents of the University of Michigan, Trustees of Indiana University,
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
import java.util.Properties;
import java.util.ResourceBundle;

import org.sakaiproject.api.kernel.tool.Placement;
import org.sakaiproject.api.kernel.tool.cover.ToolManager;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.cheftool.VelocityPortletPaneledAction;
import org.sakaiproject.service.framework.config.cover.ServerConfigurationService;
import org.sakaiproject.service.framework.portal.cover.PortalService;
import org.sakaiproject.service.framework.session.SessionState;
import org.sakaiproject.service.legacy.resource.Reference;
import org.sakaiproject.service.legacy.resource.cover.EntityManager;
import org.sakaiproject.service.legacy.site.Site;
import org.sakaiproject.service.legacy.site.SitePage;
import org.sakaiproject.service.legacy.site.cover.SiteService;
import org.sakaiproject.util.java.StringUtil;

/**
 * <p>
 * IFrameAction is the Sakai tool to place any web content in an IFrame on the page.
 * </p>
 * <p>
 * Three special modes are supported - these pick the URL content from special places:
 * </p>
 * <ul>
 * <li>"site" - to show the services "server.info.url" configuration URL setting</li>
 * <li>"workspace" - to show the configured "myworkspace.info.url" URL, introducing a my workspace to users</li>
 * <li>"worksite" - to show the current site's "getInfoUrlFull()" setting</li>
 * </ul>
 * 
 * @author University of Michigan, Sakai Software Development Team
 */
public class IFrameAction extends VelocityPortletPaneledAction
{
	/** Resource bundle using current language locale */
	protected static ResourceBundle rb = ResourceBundle.getBundle("iframe");

	/** The source URL, in state, config and context. */
	protected final static String SOURCE = "source";

	/** The value in state and context for the source URL to actually used, as computed from special and URL. */
	protected final static String URL = "url";

	/** The height, in state, config and context. */
	protected final static String HEIGHT = "height";

	/** The special attribute, in state, config and context. */
	protected final static String SPECIAL = "special";

	/** Special value for site. */
	protected final static String SPECIAL_SITE = "site";

	/** Special value for myworkspace. */
	protected final static String SPECIAL_WORKSPACE = "workspace";

	/** Special value for worksite. */
	protected final static String SPECIAL_WORKSITE = "worksite";

	/** The title, in state and context. */
	protected final static String TITLE = "title";

	/**
	 * Whether to pass through the PID to the URL displayed in the IFRAME. This enables integration in that the application in the IFRAME will know what site and tool it is part of.
	 */
	private final static String PASS_PID = "passthroughPID";

	/**
	 * Populate the state with configuration settings
	 */
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData rundata)
	{
		// TODO: we might want to keep this from running for each request - but by letting it we get fresh info each time... -ggolden
		super.initState(state, portlet, rundata);

		Placement placement = ToolManager.getCurrentPlacement();
		Properties config = placement.getConfig();

		// set the pass_pid parameter
		boolean passPid = false;
		String passPidStr = config.getProperty(PASS_PID, "false");
		state.removeAttribute(PASS_PID);
		if ("true".equalsIgnoreCase(passPidStr))
		{
			state.setAttribute(PASS_PID, Boolean.TRUE);
			passPid = true;
		}

		// set the special setting
		String special = config.getProperty(SPECIAL);

		// check for an older way the ChefWebPagePortlet took parameters, converting to our "special" values
		if (special == null)
		{
			if ("true".equals(config.getProperty("site")))
			{
				special = SPECIAL_SITE;
			}
			else if ("true".equals(config.getProperty("workspace")))
			{
				special = SPECIAL_WORKSPACE;
			}
			else if ("true".equals(config.getProperty("worksite")))
			{
				special = SPECIAL_WORKSITE;
			}
		}

		state.removeAttribute(SPECIAL);
		if ((special != null) && (special.trim().length() > 0))
		{
			state.setAttribute(SPECIAL, special);
		}

		// set the source url setting
		String source = StringUtil.trimToNull(config.getProperty(SOURCE));

		// check for an older way the ChefWebPagePortlet took parameters, converting to our "source" value
		if (source == null)
		{
			source = StringUtil.trimToNull(config.getProperty("url"));
		}

		// store the raw as-configured source url
		state.removeAttribute(SOURCE);
		if (source != null)
		{
			state.setAttribute(SOURCE, source);
		}

		// compute working URL, modified from the configuration URL if special
		String url = sourceUrl(special, source, placement.getContext(), passPid, placement.getId());
		state.setAttribute(URL, url);

		// set the height
		state.setAttribute(HEIGHT, config.getProperty(HEIGHT, "600px"));

		// set the title
		state.setAttribute(TITLE, placement.getTitle());
	}

	/**
	 * Compute the actual URL we will used, based on the configuration special and source URLs
	 */
	protected String sourceUrl(String special, String source, String context, boolean passPid, String pid)
	{
		String rv = StringUtil.trimToNull(source);

		// if marked for "site", use the site intro from the properties
		if (SPECIAL_SITE.equals(special))
		{
			// set the url to the site config'ed url
			rv = StringUtil.trimToNull(ServerConfigurationService.getString("server.info.url"));
		}

		// if marked for "workspace", use the "user" site info from the properties
		else if (SPECIAL_WORKSPACE.equals(special))
		{
			// set the url to the site config'ed url
			rv = StringUtil.trimToNull(ServerConfigurationService.getString("myworkspace.info.url"));
		}

		// if marked for "worksite", use the setting from the site's definition
		else if (SPECIAL_WORKSITE.equals(special))
		{
			// set the url to the site of this request's config'ed url
			String siteId = PortalService.getCurrentSiteId();
			try
			{
				// get the site's info URL, if defined
				Site s = SiteService.getSite(context);
				rv = StringUtil.trimToNull(s.getInfoUrlFull());

				// compute the info url for the site if it has no specific InfoUrl
				if (rv == null)
				{
					// access will show the site description or title...
					rv = ServerConfigurationService.getAccessUrl() + s.getReference();
				}
			}
			catch (Exception any)
			{
			}
		}

		// if it's not special, and we have no value yet, set it to the webcontent instruction page, as configured
		if (rv == null)
		{
			rv = StringUtil.trimToNull(ServerConfigurationService.getString("webcontent.instructions.url"));
		}

		if (rv != null)
		{
			// accept a partial reference url (i.e. "/content/group/sakai/test.gif"), convert to full url
			rv = convertReferenceUrl(rv);

			// pass the PID through on the URL, IF configured to do so
			if (passPid)
			{
				if (rv.indexOf("?") < 0)
				{
					rv = rv + "?";
				}
				else
				{
					rv = rv + "&";
				}

				rv = rv + "pid=" + pid;
			}
		}

		return rv;
	}

	/**
	 * If the url is a valid reference, convert it to a URL, else return it unchanged.
	 */
	protected String convertReferenceUrl(String url)
	{
		// make a reference
		Reference ref = EntityManager.newReference(url);

		// if it didn't recognize this, return it unchanged
		if (!ref.isKnownType()) return url;

		// return the reference's url
		return ref.getUrl();
	}

	/**
	 * Setup the velocity context and choose the template for the response.
	 */
	public String buildMainPanelContext(VelocityPortlet portlet, Context context, RunData rundata, SessionState state)
	{
		// do options if we are in options mode
		if (MODE_OPTIONS.equals(state.getAttribute(STATE_MODE)))
		{
			return buildOptionsPanelContext(portlet, context, rundata, state);
		}

		// if we rely on state (like all the other tools), we won't pick up any changes others make to the configuration till we are refreshed... -ggolden

		// set our configuration into the context for the vm
		context.put(URL, (String) state.getAttribute(URL));
		context.put(HEIGHT, state.getAttribute(HEIGHT));

		// set the resource bundle with our strings
		context.put("tlang", rb);

		// setup for the options menu if needed
		if (SiteService.allowUpdateSite(ToolManager.getCurrentPlacement().getContext()))
		{
			context.put("options_title", ToolManager.getCurrentPlacement().getTitle() + " " + rb.getString("gen.options"));
		}

		return (String) getContext(rundata).get("template");
	}

	/**
	 * Setup the velocity context and choose the template for options.
	 */
	public String buildOptionsPanelContext(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		// provide the source, and let the user edit, if not special
		String special = (String) state.getAttribute(SPECIAL);
		if (special == null)
		{
			String source = (String) state.getAttribute(SOURCE);
			if (source == null) source = "";
			context.put(SOURCE, source);
			context.put("heading", rb.getString("gen.custom"));
		}
		
		// set the heading based on special
		else
		{
			if (SPECIAL_SITE.equals(special))
			{
				context.put("heading", rb.getString("gen.custom.site"));				
			}

			else if (SPECIAL_WORKSPACE.equals(special))
			{
				context.put("heading", rb.getString("gen.custom.workspace"));
			}

			else if (SPECIAL_WORKSITE.equals(special))
			{
				context.put("heading", rb.getString("gen.custom.worksite"));

				// for worksite, also include the Site's infourl and description
				try
				{
					Site s = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());

					String infoUrl = StringUtil.trimToNull(s.getInfoUrl());
					if (infoUrl != null)
					{
						context.put("info_url", infoUrl);
					}

					String description = StringUtil.trimToNull(s.getDescription());
					if (description != null)
					{
						context.put("description", description);
					}
				}
				catch (Throwable e) {}
			}

			else
			{
				context.put("heading", rb.getString("gen.custom"));
			}
		}

		context.put(HEIGHT, state.getAttribute(HEIGHT));
		context.put(TITLE, state.getAttribute(TITLE));
		context.put("tlang", rb);

		context.put("doUpdate", BUTTON + "doConfigure_update");
		context.put("doCancel", BUTTON + "doCancel");

		// pick the "-customize" template based on the standard template name
		String template = (String) getContext(data).get("template");
		
		// pick the site customize template if we are in that mode
		if (SPECIAL_WORKSITE.equals(special))
		{
			template = template + "-site-customize";
		}
		else
		{
			template = template + "-customize";
		}

		return template;
	}

	/**
	 * Handle the configure context's update button
	 */
	public void doConfigure_update(RunData data, Context context)
	{
		// TODO: if we do limit the initState() calls, we need to make sure we get a new one after this call -ggolden

		String peid = ((JetspeedRunData) data).getJs_peid();
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);

		Placement placement = ToolManager.getCurrentPlacement();

		// read source if we are not special
		if (state.getAttribute(SPECIAL) == null)
		{
			String source = StringUtil.trimToZero(data.getParameters().getString(SOURCE));
			if ((source != null) && (source.length() > 0) && (!source.startsWith("/")) && (source.indexOf("://") == -1))
			{
				source = "http://" + source;
			}

			// update state
			//state.setAttribute(SOURCE, source);
			placement.getPlacementConfig().setProperty(SOURCE, source);
		}
		
		else if (SPECIAL_WORKSITE.equals(state.getAttribute(SPECIAL)))
		{
			String infoUrl = StringUtil.trimToNull(data.getParameters().getString("infourl"));
			if ((infoUrl != null) && (infoUrl.length() > 0) && (!infoUrl.startsWith("/")) && (infoUrl.indexOf("://") == -1))
			{
				infoUrl = "http://" + infoUrl;
			}
			String description = StringUtil.trimToNull(data.getParameters().getString("description"));
			
			// update the site info
			try
			{
				SiteService.saveSiteInfo(ToolManager.getCurrentPlacement().getContext(), description, infoUrl);
			}
			catch (Throwable e) {}
		}

		// height
		String height = data.getParameters().getString(HEIGHT);
		//state.setAttribute(HEIGHT, height);
		placement.getPlacementConfig().setProperty(HEIGHT, height);

		// title
		String title = data.getParameters().getString(TITLE);
		//state.setAttribute(TITLE, title);
		placement.setTitle(title);

		if (state.getAttribute(SPECIAL) == null)
		{
			// for web content tool, if it is the only tool on the page, update the page title also.
			SitePage p = SiteService.findPage(PortalService.getCurrentSitePageId());
			if (p.getTools() != null && p.getTools().size() == 1)
			{
				// if this is the only tool on that page, update the page's title also
				try
				{
					// TODO: save site page title? -ggolden
					Site sEdit = SiteService.getSite(PortalService.getCurrentSiteId());
					SitePage pEdit = sEdit.getPage(p.getId());
					pEdit.setTitle(title);
					SiteService.save(sEdit);
				}
				catch (Exception ignore)
				{
				}
			}
		
		}

		// save
		placement.save();

		// we are done with customization... back to the main mode
		state.removeAttribute(STATE_MODE);

		// deliver an update to the title panel (to show the new title)
		String titleId = titlePanelUpdateId(peid);
		schedulePeerFrameRefresh(titleId);
	}

	/**
	 * doCancel called for form input tags type="submit" named="eventSubmit_doCancel" cancel the options process
	 */
	public void doCancel(RunData data, Context context)
	{
		// access the portlet element id to find our state
		String peid = ((JetspeedRunData) data).getJs_peid();
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(peid);

		// we are done with customization... back to the main mode
		state.removeAttribute(STATE_MODE);
	}
}
